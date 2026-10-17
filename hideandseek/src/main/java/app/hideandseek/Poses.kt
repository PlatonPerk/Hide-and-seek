package app.hideandseek

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.hideandseek.core.CreatureSprites
import app.hideandseek.core.Edge
import app.hideandseek.core.PoseDef
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** Доля высоты боковой позы снизу, которая плавно растворяется: там обрыв худи. */
internal const val SIDE_FADE_BOTTOM = 0.16f

private val TwinkleGold = Color(0xFFFFE066)

/** Загруженная поза: картинка, кадр моргания (того же размера) и её описание из контента. */
internal class LoadedPose(val image: ImageBitmap, val blink: ImageBitmap?, val def: PoseDef) {
    val anchorX: Float get() = def.anchorX
    val anchorY: Float get() = def.anchorY

    /** Размер на экране: высота из контента, ширина — по пропорциям картинки. */
    fun size(density: Density): Size {
        val height = with(density) { def.heightDp.dp.toPx() }
        return Size(height * image.width / image.height.toFloat(), height)
    }
}

/** Позы зверька, которые удалось загрузить. Незагрузившиеся — null. */
internal class LoadedSprites(
    val peek: LoadedPose?,
    val side: LoadedPose?,
    val found: LoadedPose?,
    val celebrate: LoadedPose?,
    val run: LoadedPose?,
) {
    val isEmpty: Boolean get() = peek == null && side == null && found == null && celebrate == null && run == null
    val hasCelebration: Boolean get() = found != null || celebrate != null || run != null

    /** Поза для края: сверху — peek, слева и справа — side. Для нижнего края позы нет. */
    fun forEdge(edge: Edge): LoadedPose? = when (edge) {
        Edge.TOP -> peek
        Edge.LEFT, Edge.RIGHT -> side
        Edge.BOTTOM -> null
    }
}

/** [loading] — позы ещё грузятся (зверёк в это время не выглядывает). */
internal class PosesState(val key: CreatureSprites?, val loading: Boolean, val sprites: LoadedSprites?)

/** Загружает все позы зверька заранее, параллельно. Без поз в контенте сразу возвращает пустое состояние. */
@Composable
internal fun rememberPoses(sprites: CreatureSprites?): PosesState {
    val context = LocalContext.current.applicationContext
    val state = produceState(PosesState(sprites, sprites != null, null), sprites) {
        value = PosesState(sprites, sprites != null, null)
        if (sprites != null) value = PosesState(sprites, false, loadPoses(context, sprites))
    }
    val current = state.value
    return if (current.key == sprites) current else PosesState(sprites, sprites != null, null)
}

private suspend fun loadPoses(context: Context, sprites: CreatureSprites): LoadedSprites? = coroutineScope {
    val bitmaps = sprites.urls
        .map { url -> async { url to SpriteCache.load(context, url) } }
        .awaitAll()
        .toMap()

    fun pose(def: PoseDef?): LoadedPose? {
        if (def == null) return null
        val image = bitmaps[def.image] ?: return null
        // Кадр моргания подменяет позу целиком, поэтому годится только того же размера.
        val blink = def.blink?.let { bitmaps[it] }?.takeIf { it.width == image.width && it.height == image.height }
        return LoadedPose(image, blink, def)
    }

    val loaded = LoadedSprites(
        peek = pose(sprites.peek),
        side = pose(sprites.side),
        found = pose(sprites.found),
        celebrate = pose(sprites.celebrate),
        run = pose(sprites.run),
    )
    if (loaded.isEmpty) null else loaded
}

/**
 * Рисует позу так, что её точка-якорь стоит в [at]. Поворот и масштаб — вокруг якоря.
 * [mirror] отражает картинку по горизонтали (левый край, бег влево).
 * [fadeBottom] — доля высоты снизу, которая плавно растворяется.
 */
internal fun DrawScope.drawPose(
    pose: LoadedPose,
    image: ImageBitmap,
    at: Offset,
    size: Size,
    mirror: Boolean = false,
    rotation: Float = 0f,
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    alpha: Float = 1f,
    fadeBottom: Float = 0f,
) {
    val left = -pose.anchorX * size.width
    val top = -pose.anchorY * size.height
    val dstOffset = IntOffset(left.roundToInt(), top.roundToInt())
    val dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
    withTransform({
        translate(at.x, at.y)
        rotate(rotation, pivot = Offset.Zero)
        scale(if (mirror) -scaleX else scaleX, scaleY, pivot = Offset.Zero)
    }) {
        if (fadeBottom <= 0f) {
            drawImage(image, dstOffset = dstOffset, dstSize = dstSize, alpha = alpha)
        } else {
            val bounds = Rect(
                dstOffset.x.toFloat(),
                dstOffset.y.toFloat(),
                (dstOffset.x + dstSize.width).toFloat(),
                (dstOffset.y + dstSize.height).toFloat(),
            )
            // Отдельный слой только под картинку: низ растворяется маской, фон под лисой не трогаем.
            drawContext.canvas.saveLayer(bounds, Paint())
            drawImage(image, dstOffset = dstOffset, dstSize = dstSize, alpha = alpha)
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black,
                    (1f - fadeBottom) to Color.Black,
                    1f to Color.Transparent,
                    startY = bounds.top,
                    endY = bounds.bottom,
                ),
                topLeft = bounds.topLeft,
                size = bounds.size,
                blendMode = BlendMode.DstIn,
            )
            drawContext.canvas.restore()
        }
    }
}

/**
 * Поза «выглядывает» на краю. [progress]: 0 — целиком за краем, 1 — выглянула,
 * больше 1 — пружинка (поза чуть вытягивается от края, а не отрывается от него).
 */
internal fun DrawScope.drawHidingPose(
    pose: LoadedPose,
    size: Size,
    edge: Edge,
    anchor: Offset,
    progress: Float,
    out: Float,
    blink: Float,
    wiggle: Float,
) {
    val shift = (1f - progress.coerceIn(0f, 1f)) * out
    val stretch = 1f + (progress - 1f).coerceIn(0f, 0.25f) * 0.4f
    val image = if (blink < 0.5f) pose.blink ?: pose.image else pose.image
    when (edge) {
        Edge.TOP -> drawPose(pose, image, Offset(anchor.x, anchor.y + shift), size, rotation = wiggle, scaleY = stretch)
        Edge.RIGHT -> drawPose(
            pose, image, Offset(anchor.x - shift, anchor.y), size,
            rotation = wiggle * 0.5f, scaleX = stretch, fadeBottom = SIDE_FADE_BOTTOM,
        )
        Edge.LEFT -> drawPose(
            pose, image, Offset(anchor.x + shift, anchor.y), size,
            mirror = true, rotation = -wiggle * 0.5f, scaleX = stretch, fadeBottom = SIDE_FADE_BOTTOM,
        )
        Edge.BOTTOM -> Unit // для нижнего края позы нет: там рисуется прежний зверёк
    }
}

/** Искорки вокруг редкого зверька. */
internal fun DrawScope.drawTwinkles(center: Offset, radius: Float, phase: Float) {
    val spots = arrayOf(Offset(-0.95f, -0.75f), Offset(0.9f, -0.9f), Offset(-1.05f, 0.2f), Offset(1.0f, 0.1f))
    val r = 6.dp.toPx()
    for (i in spots.indices) {
        val wave = sin(2.0 * PI * (phase + i / 4.0)).toFloat()
        val a = 0.35f + 0.65f * (0.5f + 0.5f * wave)
        drawPath(star(center + spots[i] * radius, r * (0.7f + 0.3f * a), 0f), TwinkleGold.copy(alpha = a))
    }
}
