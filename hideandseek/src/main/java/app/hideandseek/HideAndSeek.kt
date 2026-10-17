package app.hideandseek

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import app.hideandseek.core.ContentParser
import app.hideandseek.core.DailyPlan
import app.hideandseek.core.DailyPlanner
import app.hideandseek.core.Day
import app.hideandseek.core.HideAndSeekContent
import app.hideandseek.core.Progress
import app.hideandseek.core.SpotDef
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.TimeZone
import kotlin.random.Random

// =====================================================================================
//  Public API — the whole integration is three calls:
//
//    val hideAndSeek = rememberHideAndSeekController(contentUrl = "https://…/hideandseek.json")
//    HideAndSeekHost(hideAndSeek, Modifier.fillMaxSize()) { App() }   // once, at the root
//    Modifier.hidingSpot("home.balance")                               // on any element
//
//  plus HideAndSeekBlocker() on payment / transfer screens.
// =====================================================================================

/** The controller of the nearest [HideAndSeekHost], or null outside of one. */
val LocalHideAndSeek = staticCompositionLocalOf<HideAndSeekController?> { null }

@Composable
fun rememberHideAndSeekController(
    contentUrl: String? = null,
    source: ContentSource? = contentUrl?.let { UrlContentSource(it) },
    bundledAsset: String? = HideAndSeekController.DEFAULT_BUNDLED_ASSET,
): HideAndSeekController {
    val context = LocalContext.current.applicationContext
    return remember { HideAndSeekController(context, source, bundledAsset) }
}

/**
 * Wrap the app content once. Draws the creature in a layer above [content];
 * the layer only takes touches on the creature itself, everything else goes
 * straight to the app underneath.
 */
@Composable
fun HideAndSeekHost(
    controller: HideAndSeekController,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(controller) { controller.start() }
    LaunchedEffect(controller) {
        while (true) {
            delay(30_000L)
            controller.tick() // day rollover at midnight, active hours
        }
    }
    CompositionLocalProvider(LocalHideAndSeek provides controller) {
        Box(modifier) {
            content()
            HideAndSeekOverlay(controller, Modifier.matchParentSize())
        }
    }
}

/**
 * Marks this element as a place where the creature may hide. The [id] must match
 * a spot id in the content JSON. Does nothing outside of a [HideAndSeekHost].
 */
fun Modifier.hidingSpot(id: String): Modifier = composed {
    val controller = LocalHideAndSeek.current ?: return@composed Modifier
    val registry = controller.registry
    val key = remember { Any() }
    DisposableEffect(registry, key) {
        onDispose { registry.remove(key) }
    }
    Modifier.onGloballyPositioned { coordinates ->
        if (!coordinates.isAttached) {
            registry.remove(key)
        } else {
            val size = coordinates.size
            val bounds = Rect(coordinates.positionInRoot(), Size(size.width.toFloat(), size.height.toFloat()))
            registry.put(key, SpotGeometry(id, bounds, coordinates.boundsInRoot()))
        }
    }
}

/**
 * Put this on screens that must stay clean (payments, transfers, card details, limits).
 * While it is on screen the creature is not shown anywhere.
 */
@Composable
fun HideAndSeekBlocker() {
    val controller = LocalHideAndSeek.current ?: return
    DisposableEffect(controller) {
        controller.registry.addBlocker()
        onDispose { controller.registry.removeBlocker() }
    }
}

// =====================================================================================
//  Content sources
// =====================================================================================

/** Where the content JSON comes from. Prototype: a static file; production: a CMS key. */
interface ContentSource {
    suspend fun fetch(): String?
}

/** `ContentSource { cmsClient.getJson("kids_hide_and_seek") }` */
fun ContentSource(block: suspend () -> String?): ContentSource = object : ContentSource {
    override suspend fun fetch(): String? = block()
}

/** Loads the JSON over https (GitHub Pages, any CDN). */
class UrlContentSource(
    private val url: String,
    /** Adds ?t=… so a CDN cache never hides a fresh edit during the demo. Turn off in production. */
    private val bypassCdnCache: Boolean = true,
    private val timeoutMs: Int = 6_000,
) : ContentSource {
    override suspend fun fetch(): String? = withContext(Dispatchers.IO) {
        val target = if (bypassCdnCache) {
            url + (if (url.contains('?')) "&" else "?") + "t=" + System.currentTimeMillis()
        } else {
            url
        }
        val connection = URL(target).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) {
                null
            } else {
                connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            }
        } finally {
            connection.disconnect()
        }
    }
}

// =====================================================================================
//  Controller: content, today's plan, progress
// =====================================================================================

@Stable
class HideAndSeekController(
    context: Context,
    private val source: ContentSource?,
    private val bundledAsset: String? = DEFAULT_BUNDLED_ASSET,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    companion object {
        const val DEFAULT_BUNDLED_ASSET = "hideandseek/content.json"
        private const val DEFAULT_ZONE = "Europe/Moscow"
    }

    private val appContext = context.applicationContext
    private val store = ProgressStore(appContext)
    private val cache = ContentCache(appContext)
    private var started = false

    internal val registry = SpotRegistry()

    /** Current content, or null until the first load finishes. */
    var content: HideAndSeekContent? by mutableStateOf(null)
        private set

    /** Where the current content came from: "сеть", "кеш" or "встроенный" (for the demo panel). */
    var contentOrigin: String by mutableStateOf("—")
        private set

    /** Last load / parse problem, for the demo panel. */
    var lastError: String? by mutableStateOf(null)
        private set

    var progress: Progress by mutableStateOf(Progress())
        private set

    private var dayShiftState: Int by mutableIntStateOf(0)

    /** Demo time travel: shifts "today" by this many days. */
    var dayShift: Int
        get() = dayShiftState
        set(value) {
            dayShiftState = value
            demoSpot = null
        }

    /**
     * Режим демо (только для показа прототипа): после находки и закрытия карточки «Нашёл!»
     * персонаж сразу прячется снова в другом месте, а не ждёт следующего дня.
     * Предпочитает места, которые сейчас видны на экране.
     */
    var demoRehide: Boolean by mutableStateOf(false)

    /** Режим демо: место, куда персонаж перепрятался после последней находки (null — место дня). */
    private var demoSpot: SpotDef? by mutableStateOf(null)

    private val demoRandom = Random(System.nanoTime())

    /** Called on the main thread when the kid finds today's creature (analytics hook). */
    var onFound: ((DailyPlan) -> Unit)? = null

    private var nowMillis: Long by mutableLongStateOf(clock())

    private val offsetMillis: Int
        get() = TimeZone.getTimeZone(content?.timeZone ?: DEFAULT_ZONE).getOffset(nowMillis)

    /** Today in the content's time zone (Moscow by default), plus [dayShift]. */
    val today: Day get() = Day.at(nowMillis, offsetMillis) + dayShift

    internal val hour: Int get() = Day.hourAt(nowMillis, offsetMillis)

    private val planState = derivedStateOf {
        val base = content?.let { DailyPlanner.plan(it, today) }
        val moved = demoSpot
        if (base != null && moved != null) base.copy(spot = moved) else base
    }

    /** Who hides today and where; null when nobody hides today. */
    val plan: DailyPlan? get() = planState.value

    val isFoundToday: Boolean get() = progress.isFound(today)

    internal val isAwakeHour: Boolean get() = content?.schedule?.allowsHour(hour) ?: false

    /** Loads saved progress and cached (or bundled) content, then refreshes from the source. */
    internal suspend fun start() {
        if (started) return
        started = true
        val local = withContext(Dispatchers.IO) {
            val saved = store.load()
            val cachedJson = cache.read()
            val cached = cachedJson?.let { parseOrNull(it) }
            val bundled = if (cached == null) readBundled()?.let { parseOrNull(it) } else null
            Triple(saved, cached, bundled)
        }
        progress = local.first
        if (content == null) {
            val cached = local.second
            val bundled = local.third
            if (cached != null) {
                content = cached
                contentOrigin = "кеш"
            } else if (bundled != null) {
                content = bundled
                contentOrigin = "встроенный"
            }
        }
        refresh()
    }

    /** Fetches fresh content from the source. Returns true when new content was applied. */
    suspend fun refresh(): Boolean {
        val src = source ?: return false
        val json = try {
            src.fetch()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            lastError = "Не удалось скачать: ${e.message}"
            null
        } ?: return false
        val parsed = parseOrNull(json)
        if (parsed == null) {
            lastError = "JSON с ошибкой — оставлена прошлая версия"
            return false
        }
        withContext(Dispatchers.IO) { cache.write(json) }
        content = parsed
        demoSpot = null
        contentOrigin = "сеть"
        lastError = null
        nowMillis = clock()
        return true
    }

    internal fun tick() {
        nowMillis = clock()
    }

    internal fun markFound(plan: DailyPlan) {
        val current = content ?: return
        if (progress.isFound(plan.day)) return
        progress = progress.found(plan.day, plan.creature.id, DailyPlanner.previousHidingDay(current, plan.day))
        store.save(progress)
        onFound?.invoke(plan)
    }

    /**
     * Вызывается слоем, когда карточка «Нашёл!» закрылась. В режиме демо персонаж
     * перепрятывается в другое место: сначала среди мест, видимых на экране, иначе в любое.
     */
    internal fun afterFound() {
        if (!demoRehide) return
        val current = content ?: return
        val now = plan ?: return
        if (!progress.isFound(now.day)) return
        val others = current.spotsOn(now.day).filter { it.id != now.spot.id }
        if (others.isEmpty()) return
        val onScreen = others.filter { registry.find(it.id)?.visibleBounds?.isEmpty == false }
        val pool = onScreen.ifEmpty { others }
        demoSpot = pool[demoRandom.nextInt(pool.size)]
        progress = progress.copy(lastFoundDay = null)
        store.save(progress)
    }

    /** Demo: let today's creature hide again. */
    fun resetToday() {
        if (progress.lastFoundDay == today) {
            progress = progress.copy(lastFoundDay = null)
            store.save(progress)
        }
    }

    /** Demo: forget all progress. */
    fun resetAll() {
        progress = Progress()
        store.save(progress)
    }

    private fun parseOrNull(json: String): HideAndSeekContent? = try {
        ContentParser.parse(json)
    } catch (e: Exception) {
        null
    }

    private fun readBundled(): String? {
        val path = bundledAsset ?: return null
        return try {
            appContext.assets.open(path).use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (e: Exception) {
            null
        }
    }
}

// =====================================================================================
//  Internals: spot registry, storage
// =====================================================================================

/** Where a spot is on screen, in root (window) pixels. */
@Immutable
internal data class SpotGeometry(
    val id: String,
    /** Full bounds of the element. */
    val bounds: Rect,
    /** The part not clipped by scrolling containers; empty when scrolled away. */
    val visibleBounds: Rect,
)

@Stable
internal class SpotRegistry {
    private val spots = mutableStateMapOf<Any, SpotGeometry>()
    private var blockers by mutableIntStateOf(0)

    /** Set by the website bridge when the page has `data-hide-seek-block`. */
    var webBlocked by mutableStateOf(false)

    val isBlocked: Boolean get() = blockers > 0 || webBlocked

    fun put(key: Any, geometry: SpotGeometry) {
        if (spots[key] != geometry) spots[key] = geometry
    }

    fun remove(key: Any) {
        spots.remove(key)
    }

    /** The on-screen element with this id (a visible one wins if there are several). */
    fun find(id: String): SpotGeometry? {
        var fallback: SpotGeometry? = null
        for (geometry in spots.values) {
            if (geometry.id != id) continue
            if (!geometry.visibleBounds.isEmpty) return geometry
            if (fallback == null) fallback = geometry
        }
        return fallback
    }

    fun addBlocker() {
        blockers += 1
    }

    fun removeBlocker() {
        blockers -= 1
    }
}

internal class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("hideandseek_progress", Context.MODE_PRIVATE)

    fun load(): Progress = Progress(
        lastFoundDay = if (prefs.contains(KEY_LAST)) Day(prefs.getInt(KEY_LAST, 0)) else null,
        streak = prefs.getInt(KEY_STREAK, 0),
        totalFound = prefs.getInt(KEY_TOTAL, 0),
        collection = prefs.getStringSet(KEY_COLLECTION, null)?.toSet() ?: emptySet(),
    )

    fun save(progress: Progress) {
        val editor = prefs.edit()
        val last = progress.lastFoundDay
        if (last != null) editor.putInt(KEY_LAST, last.epochDay) else editor.remove(KEY_LAST)
        editor.putInt(KEY_STREAK, progress.streak)
        editor.putInt(KEY_TOTAL, progress.totalFound)
        editor.putStringSet(KEY_COLLECTION, HashSet(progress.collection))
        editor.apply()
    }

    private companion object {
        const val KEY_LAST = "lastFoundDay"
        const val KEY_STREAK = "streak"
        const val KEY_TOTAL = "total"
        const val KEY_COLLECTION = "collection"
    }
}

internal class ContentCache(context: Context) {
    private val file = File(context.filesDir, "hideandseek/content.json")

    fun read(): String? = try {
        if (file.exists()) file.readText() else null
    } catch (e: Exception) {
        null
    }

    fun write(json: String) {
        try {
            file.parentFile?.mkdirs()
            val tmp = File(file.path + ".tmp")
            tmp.writeText(json)
            if (!tmp.renameTo(file)) {
                file.writeText(json)
                tmp.delete()
            }
        } catch (e: Exception) {
            // Cache is best-effort.
        }
    }
}
