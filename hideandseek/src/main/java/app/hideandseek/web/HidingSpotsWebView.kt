package app.hideandseek.web

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import app.hideandseek.LocalHideAndSeek
import app.hideandseek.SpotGeometry
import app.hideandseek.SpotRegistry
import app.hideandseek.core.JsonException
import app.hideandseek.core.MiniJson
import app.hideandseek.core.asObject
import app.hideandseek.core.boolean
import app.hideandseek.core.number
import app.hideandseek.core.objects
import app.hideandseek.core.string

/**
 * A WebView whose page elements can be hiding spots for the native creature.
 * Lets the team test the feature on a website that imitates the app; the
 * creature, the logic and the content stay exactly the same as on native screens.
 *
 * In the page, mark elements with `data-hide-spot="home.balance"`, or give a spot
 * a CSS selector in the content JSON ("web": ".balance-card"). Add the attribute
 * `data-hide-seek-block` to any element on payment pages to keep them clean.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HidingSpotsWebView(
    url: String,
    modifier: Modifier = Modifier,
    onCreated: (WebView) -> Unit = {},
) {
    val controller = LocalHideAndSeek.current
    val bridge = remember { WebSpotBridge() }
    bridge.registry = controller?.registry
    val selectorsJson = controller?.content?.spots.orEmpty()
        .mapNotNull { spot -> spot.webSelector?.let { MiniJson.quote(spot.id) + ":" + MiniJson.quote(it) } }
        .joinToString(separator = ",", prefix = "{", postfix = "}")

    DisposableEffect(bridge) {
        onDispose { bridge.detach() }
    }

    AndroidView(
        modifier = modifier.onGloballyPositioned { bridge.onLayout(it.positionInRoot(), it.size) },
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                addJavascriptInterface(bridge, WebSpotBridge.JS_NAME)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (view != null) bridge.inject(view)
                    }
                }
                bridge.webView = this
                bridge.loadedUrl = url
                onCreated(this)
                loadUrl(url)
            }
        },
        update = { webView ->
            bridge.setSelectors(selectorsJson)
            if (bridge.loadedUrl != url) {
                bridge.loadedUrl = url
                webView.loadUrl(url)
            }
        },
    )
}

internal class WebSpotBridge {
    companion object {
        const val JS_NAME = "HideAndSeekBridge"
    }

    /** Registry key for one page element; unique per bridge, so two WebViews never clash. */
    private data class Key(val bridge: WebSpotBridge, val id: String)

    var registry: SpotRegistry? = null
    var webView: WebView? = null
    var loadedUrl: String? = null

    private var selectorsJson = "{}"
    private var origin = Offset.Zero
    private var viewSize = IntSize.Zero
    private val ownedKeys = HashSet<Key>()
    private val main = Handler(Looper.getMainLooper())

    /** Called by the page (on a background thread) with element rectangles in CSS pixels. */
    @JavascriptInterface
    fun report(json: String) {
        main.post { apply(json) }
    }

    fun onLayout(newOrigin: Offset, newSize: IntSize) {
        if (newOrigin == origin && newSize == viewSize) return
        origin = newOrigin
        viewSize = newSize
        webView?.evaluateJavascript("window.__hideSeekReport && window.__hideSeekReport();", null)
    }

    fun setSelectors(json: String) {
        if (json == selectorsJson) return
        selectorsJson = json
        webView?.let { inject(it) }
    }

    fun inject(view: WebView) {
        view.evaluateJavascript(script(selectorsJson), null)
    }

    fun detach() {
        val reg = registry
        if (reg != null) {
            for (key in ownedKeys) reg.remove(key)
            reg.webBlocked = false
        }
        ownedKeys.clear()
        webView = null
    }

    private fun apply(json: String) {
        val reg = registry ?: return
        if (webView == null) return
        val root = try {
            MiniJson.parse(json).asObject()
        } catch (e: JsonException) {
            null
        } ?: return
        val cssWidth = root.number("vw") ?: return
        if (cssWidth <= 0.0 || viewSize.width <= 0) return
        val scale = (viewSize.width / cssWidth).toFloat() // physical px per CSS px
        val viewRect = Rect(origin, Size(viewSize.width.toFloat(), viewSize.height.toFloat()))
        reg.webBlocked = root.boolean("blocked") == true

        val seen = HashSet<Key>()
        for (spot in root.objects("spots")) {
            val id = spot.string("id") ?: continue
            val bounds = spot.rect("x", "y", "w", "h", scale) ?: continue
            val visible = spot.rect("cx", "cy", "cw", "ch", scale)
                ?.intersect(viewRect)
                ?.takeIf { !it.isEmpty }
                ?: Rect.Zero
            val key = Key(this, id)
            reg.put(key, SpotGeometry(id, bounds, visible))
            seen.add(key)
        }
        for (key in ownedKeys) {
            if (key !in seen) reg.remove(key)
        }
        ownedKeys.clear()
        ownedKeys.addAll(seen)
    }

    private fun Map<String, Any?>.rect(x: String, y: String, w: String, h: String, scale: Float): Rect? {
        val left = number(x)?.toFloat() ?: return null
        val top = number(y)?.toFloat() ?: return null
        val width = number(w)?.toFloat() ?: return null
        val height = number(h)?.toFloat() ?: return null
        return Rect(
            origin.x + left * scale,
            origin.y + top * scale,
            origin.x + (left + width) * scale,
            origin.y + (top + height) * scale,
        )
    }

    /** Injected into every page: reports spot rectangles on scroll, resize, DOM changes and navigation. */
    private fun script(selectors: String): String = """
        (function () {
          window.__hideSeekSelectors = $selectors;
          if (window.__hideSeekInstalled) { window.__hideSeekReport(); return; }
          window.__hideSeekInstalled = true;
          var pending = false;

          function visiblePart(el, r) {
            var x1 = r.left, y1 = r.top, x2 = r.right, y2 = r.bottom;
            var p = el.parentElement;
            while (p && p !== document.body && p !== document.documentElement) {
              var cs = window.getComputedStyle(p);
              if (cs.overflowX !== 'visible' || cs.overflowY !== 'visible') {
                var pr = p.getBoundingClientRect();
                x1 = Math.max(x1, pr.left); y1 = Math.max(y1, pr.top);
                x2 = Math.min(x2, pr.right); y2 = Math.min(y2, pr.bottom);
              }
              p = p.parentElement;
            }
            x1 = Math.max(x1, 0); y1 = Math.max(y1, 0);
            x2 = Math.min(x2, window.innerWidth); y2 = Math.min(y2, window.innerHeight);
            return { x: x1, y: y1, w: Math.max(0, x2 - x1), h: Math.max(0, y2 - y1) };
          }

          function collect() {
            pending = false;
            var spots = [];
            function add(el, id) {
              if (!el || !id) return;
              var r = el.getBoundingClientRect();
              var cs = window.getComputedStyle(el);
              var shown = r.width > 0 && r.height > 0 && cs.visibility !== 'hidden' && cs.display !== 'none';
              var v = shown ? visiblePart(el, r) : { x: 0, y: 0, w: 0, h: 0 };
              spots.push({ id: id, x: r.left, y: r.top, w: r.width, h: r.height, cx: v.x, cy: v.y, cw: v.w, ch: v.h });
            }
            var marked = document.querySelectorAll('[data-hide-spot]');
            for (var i = 0; i < marked.length; i++) add(marked[i], marked[i].getAttribute('data-hide-spot'));
            var sel = window.__hideSeekSelectors || {};
            for (var id in sel) {
              try { add(document.querySelector(sel[id]), id); } catch (e) {}
            }
            // Only a visible blocker counts (pages often keep hidden screens in the DOM).
            var blocked = false;
            var blockers = document.querySelectorAll('[data-hide-seek-block]');
            for (var j = 0; j < blockers.length; j++) {
              var br = blockers[j].getBoundingClientRect();
              if (br.width > 0 && br.height > 0) { blocked = true; break; }
            }
            try {
              ${JS_NAME}.report(JSON.stringify({ vw: window.innerWidth, vh: window.innerHeight, blocked: blocked, spots: spots }));
            } catch (e) {}
          }

          function schedule() {
            if (!pending) { pending = true; window.requestAnimationFrame(collect); }
          }
          window.__hideSeekReport = schedule;
          window.addEventListener('scroll', schedule, true);
          window.addEventListener('resize', schedule);
          window.addEventListener('hashchange', schedule);
          window.addEventListener('popstate', schedule);
          document.addEventListener('transitionend', schedule, true);
          document.addEventListener('animationend', schedule, true);
          new MutationObserver(schedule).observe(document.documentElement, { subtree: true, childList: true, attributes: true });
          window.setInterval(schedule, 700);
          schedule();
        })();
    """.trimIndent()
}
