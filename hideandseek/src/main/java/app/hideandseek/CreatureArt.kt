package app.hideandseek

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import app.hideandseek.core.Seeded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Ink = Color(0xFF2A2333)
private val Blush = Color(0xFFFF7EA8)
private val Gold = Color(0xFFFFE066)

/**
 * The built-in creature, drawn in code so it works offline and needs no assets.
 * Drawn in an [s]×[s] box whose "edge line" (where the paws grip) is at [pawLine]·s.
 * Any creature in the content can replace it with a sprite via "image".
 */
internal fun DrawScope.drawCreature(
    s: Float,
    body: Color,
    eyeOpen: Float = 1f,
    lookX: Float = 0f,
    pawLine: Float = 0.97f,
    alpha: Float = 1f,
    sparkle: Float = -1f,
    happy: Boolean = false,
) {
    fun tint(color: Color): Color = if (alpha >= 0.999f) color else color.copy(alpha = color.alpha * alpha)
    val dark = lerp(body, Color.Black, 0.28f)
    val light = lerp(body, Color.White, 0.55f)

    // Ears, behind the head
    drawPath(ear(s, left = true, inner = false), tint(body))
    drawPath(ear(s, left = false, inner = false), tint(body))
    drawPath(ear(s, left = true, inner = true), tint(light))
    drawPath(ear(s, left = false, inner = true), tint(light))

    // A soft bean-shaped body with a slightly darker outline, so it reads on any background
    val bodyTopLeft = Offset(0.13f * s, 0.2f * s)
    val bodySize = Size(0.74f * s, 0.82f * s)
    val bodyRadius = CornerRadius(0.37f * s, 0.37f * s)
    drawRoundRect(tint(body), bodyTopLeft, bodySize, bodyRadius)
    drawRoundRect(tint(dark), bodyTopLeft, bodySize, bodyRadius, style = Stroke(width = 0.025f * s))

    // Tuft of hair
    val tuft = Path().apply {
        moveTo(0.46f * s, 0.22f * s)
        cubicTo(0.44f * s, 0.12f * s, 0.56f * s, 0.08f * s, 0.57f * s, 0.16f * s)
    }
    drawPath(tuft, tint(dark), style = Stroke(width = 0.035f * s, cap = StrokeCap.Round))

    // Belly
    drawOval(tint(light), Offset(0.3f * s, 0.68f * s), Size(0.4f * s, 0.28f * s))

    // Eyes
    val eyeW = 0.17f * s
    val eyeY = 0.45f * s
    for (eyeX in floatArrayOf(0.37f * s, 0.63f * s)) {
        if (happy) {
            drawArc(
                color = tint(Ink),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(eyeX - eyeW / 2f, eyeY - 0.06f * s),
                size = Size(eyeW, 0.14f * s),
                style = Stroke(width = 0.03f * s, cap = StrokeCap.Round),
            )
            continue
        }
        val open = eyeOpen.coerceIn(0.08f, 1f)
        val eyeH = 0.2f * s * open
        drawOval(tint(Color.White), Offset(eyeX - eyeW / 2f, eyeY - eyeH / 2f), Size(eyeW, eyeH))
        if (open > 0.35f) {
            val r = 0.055f * s
            val px = eyeX + lookX * 0.03f * s
            val py = eyeY + 0.012f * s
            drawOval(tint(Ink), Offset(px - r, py - r * open), Size(2f * r, 2f * r * open))
            drawCircle(tint(Color.White), radius = 0.018f * s, center = Offset(px - 0.018f * s, py - 0.02f * s))
        } else {
            drawLine(
                color = tint(Ink),
                start = Offset(eyeX - eyeW * 0.4f, eyeY),
                end = Offset(eyeX + eyeW * 0.4f, eyeY),
                strokeWidth = 0.022f * s,
                cap = StrokeCap.Round,
            )
        }
    }

    // Cheeks and smile
    drawOval(tint(Blush.copy(alpha = 0.45f)), Offset(0.2f * s, 0.56f * s), Size(0.13f * s, 0.07f * s))
    drawOval(tint(Blush.copy(alpha = 0.45f)), Offset(0.67f * s, 0.56f * s), Size(0.13f * s, 0.07f * s))
    drawArc(
        color = tint(Ink),
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(0.445f * s, 0.535f * s),
        size = Size(0.11f * s, 0.07f * s),
        style = Stroke(width = 0.022f * s, cap = StrokeCap.Round),
    )

    // Paws gripping the edge, on the sides of the face (slightly above it, so they show)
    val pawY = pawLine * s - 0.03f * s
    val pawW = 0.18f * s
    val pawH = 0.11f * s
    for (pawX in floatArrayOf(0.22f * s, 0.78f * s)) {
        val topLeft = Offset(pawX - pawW / 2f, pawY - pawH / 2f)
        drawOval(tint(body), topLeft, Size(pawW, pawH))
        drawOval(tint(dark), topLeft, Size(pawW, pawH), style = Stroke(width = 0.02f * s))
    }

    // Twinkles for rare creatures
    if (sparkle >= 0f) {
        val spots = arrayOf(
            Offset(0.06f, 0.16f), Offset(0.94f, 0.12f), Offset(0.02f, 0.44f), Offset(0.98f, 0.4f),
        )
        for (i in spots.indices) {
            val phase = sin(2.0 * PI * (sparkle + i / 4.0)).toFloat()
            val a = 0.35f + 0.65f * (0.5f + 0.5f * phase)
            val center = Offset(spots[i].x * s, spots[i].y * s)
            drawPath(star(center, 0.075f * s * (0.7f + 0.3f * a), 0f), tint(Gold.copy(alpha = a)))
        }
    }
}

/** A 4-pointed star, used for twinkles and the found burst. */
internal fun star(center: Offset, radius: Float, rotationDegrees: Float): Path {
    val path = Path()
    for (i in 0 until 8) {
        val angle = (rotationDegrees + i * 45f) * PI / 180.0
        val r = if (i % 2 == 0) radius else radius * 0.38f
        val x = center.x + (cos(angle) * r).toFloat()
        val y = center.y + (sin(angle) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun ear(s: Float, left: Boolean, inner: Boolean): Path {
    fun x(v: Float) = (if (left) v else 1f - v) * s
    var a = Offset(x(0.16f), 0.46f * s)
    var t = Offset(x(0.2f), 0.04f * s)
    var b = Offset(x(0.48f), 0.28f * s)
    if (inner) {
        val c = Offset((a.x + t.x + b.x) / 3f, (a.y + t.y + b.y) / 3f + 0.03f * s)
        a = mix(c, a, 0.5f)
        t = mix(c, t, 0.55f)
        b = mix(c, b, 0.5f)
    }
    // Triangle with a rounded tip (quadratic curve written as a cubic, which every Compose version has).
    val p1 = mix(t, a, 0.28f)
    val p2 = mix(t, b, 0.28f)
    return Path().apply {
        moveTo(a.x, a.y)
        lineTo(p1.x, p1.y)
        cubicTo(
            p1.x + (t.x - p1.x) * 2f / 3f, p1.y + (t.y - p1.y) * 2f / 3f,
            p2.x + (t.x - p2.x) * 2f / 3f, p2.y + (t.y - p2.y) * 2f / 3f,
            p2.x, p2.y,
        )
        lineTo(b.x, b.y)
        close()
    }
}

private fun mix(from: Offset, to: Offset, f: Float) = Offset(from.x + (to.x - from.x) * f, from.y + (to.y - from.y) * f)

// ---- Idle animation: blink, wiggle, look around ----

@Stable
internal class CreatureIdle(
    val blink: State<Float>,
    val wiggle: State<Float>,
    val look: State<Float>,
    val sparkle: State<Float>,
)

@Composable
internal fun rememberCreatureIdle(): CreatureIdle {
    val transition = rememberInfiniteTransition(label = "creature")
    val blink = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3800
                1f at 3300
                0.1f at 3420
                1f at 3560
            },
        ),
        label = "blink",
    )
    val wiggle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 5200
                0f at 4000
                -8f at 4150
                8f at 4400
                -5f at 4650
                0f at 4900
            },
        ),
        label = "wiggle",
    )
    val look = transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 6400
                0f at 1500
                -1f at 1800
                -1f at 2900
                1f at 3300
                1f at 4400
                0f at 4700
            },
        ),
        label = "look",
    )
    val sparkle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1600, easing = LinearEasing), RepeatMode.Restart),
        label = "sparkle",
    )
    return remember(transition) { CreatureIdle(blink, wiggle, look, sparkle) }
}

// ---- Optional sprites from the content ("image": "https://…png" or "asset:creatures/fox.png") ----

@Composable
internal fun rememberSprite(url: String?): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val state = produceState<ImageBitmap?>(null, url) {
        // Always reload on a new url (the state itself survives key changes).
        value = if (url == null) null else SpriteCache.peek(url) ?: SpriteCache.load(context, url)
    }
    return if (url == null) null else state.value
}

internal object SpriteCache {
    private val memory = HashMap<String, ImageBitmap>()

    fun peek(url: String): ImageBitmap? = synchronized(memory) { memory[url] }

    suspend fun load(context: Context, url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        peek(url) ?: try {
            val bytes = readBytes(context, url)
            val bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }?.asImageBitmap()
            if (bitmap != null) synchronized(memory) { memory[url] = bitmap }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun readBytes(context: Context, url: String): ByteArray? {
        if (url.startsWith("asset:")) {
            return context.assets.open(url.removePrefix("asset:").trimStart('/')).use { it.readBytes() }
        }
        val file = File(context.cacheDir, "hideandseek/sprites/" + Seeded.hash(url).toString(16))
        if (file.exists()) return file.readBytes()
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            if (connection.responseCode !in 200..299) return null
            val bytes = connection.inputStream.use { it.readBytes() }
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            return bytes
        } finally {
            connection.disconnect()
        }
    }
}
