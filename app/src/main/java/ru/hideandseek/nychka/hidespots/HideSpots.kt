package ru.hideandseek.nychka.hidespots

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Места, где может спрятаться Нычка.
 *
 * Экран помечает свои элементы модификаторами:
 *  - [hideSpot] — сюда прятаться можно (аватар, баннер, карточка контента…);
 *  - [noHideZone] — сюда нельзя (кнопки оплаты и переводов, баланс, суммы, реклама).
 *
 * Модификатор сообщает координаты элемента в [HideSpotRegistry] при каждой раскладке,
 * поэтому слой с Нычкой всегда знает, где элемент сейчас на экране — даже при скролле.
 * Чтобы встроить механику в настоящий экран СберKids, достаточно повесить эти модификаторы
 * на существующие Composable-элементы и положить слой Нычки поверх экрана.
 */
enum class SpotKind {
    /** Сюда можно спрятаться. */
    Allowed,

    /** Сюда нельзя: Нычка не должна перекрывать этот элемент. */
    Forbidden,

    /** Плавающий элемент поверх контента (таб-бар): места под ним считаются невидимыми. */
    Occluder,
}

@Immutable
data class HideSpot(
    val id: String,
    val kind: SpotKind,
    /** Видимая часть элемента в координатах корня Compose. Пустая, если элемент проскроллен за экран. */
    val bounds: Rect,
    /** Полные границы элемента без обрезки скроллом (могут выходить за экран). */
    val raw: Rect = bounds,
)

@Stable
class HideSpotRegistry {
    private val _spots = mutableStateMapOf<String, HideSpot>()

    /** Все зарегистрированные места текущего экрана. */
    val spots: Map<String, HideSpot> get() = _spots

    /**
     * Места, где Нычке разрешено прятаться прямо сейчас: элемент виден на экране
     * и не уехал под плавающий таб-бар (элементы самого таб-бара — можно).
     */
    fun visibleAllowed(): List<HideSpot> {
        val occluders = _spots.values
            .filter { it.kind == SpotKind.Occluder && !it.bounds.isEmpty }
            .map { it.bounds }
        return _spots.values.filter { spot ->
            spot.kind == SpotKind.Allowed && !spot.bounds.isEmpty &&
                occluders.none { o -> o.overlaps(spot.bounds) && !o.contains(spot.bounds) }
        }
    }

    internal fun put(spot: HideSpot) {
        if (_spots[spot.id] != spot) _spots[spot.id] = spot
    }

    internal fun remove(id: String) {
        _spots.remove(id)
    }
}

val LocalHideSpotRegistry = staticCompositionLocalOf<HideSpotRegistry?> { null }

/** Элемент, за которым может спрятаться Нычка. [id] должен совпадать с id в контент-конфиге. */
fun Modifier.hideSpot(id: String): Modifier = spot(id, SpotKind.Allowed)

/** Элемент, который Нычка не должна перекрывать. */
fun Modifier.noHideZone(id: String): Modifier = spot(id, SpotKind.Forbidden)

/** Плавающий элемент поверх экрана (таб-бар): то, что под ним, спрятаться не может. */
fun Modifier.occluder(id: String): Modifier = spot(id, SpotKind.Occluder)

internal fun Rect.contains(other: Rect): Boolean =
    left <= other.left && top <= other.top && right >= other.right && bottom >= other.bottom

private fun Modifier.spot(id: String, kind: SpotKind): Modifier = composed {
    val registry = LocalHideSpotRegistry.current
    if (registry == null) {
        Modifier
    } else {
        DisposableEffect(registry, id) {
            onDispose { registry.remove(id) }
        }
        Modifier.onGloballyPositioned { coordinates ->
            val raw = Rect(coordinates.positionInRoot(), coordinates.size.toSize())
            registry.put(HideSpot(id, kind, coordinates.boundsInRoot(), raw))
        }
    }
}

/**
 * Отладочный слой: зелёные рамки — места для пряток, серые — места, закрытые таб-баром,
 * красные — запретные зоны, синяя — перекрывающий элемент.
 * Включается долгим нажатием на лапку в таб-баре. Касания не перехватывает.
 */
@Composable
fun HideSpotDebugOverlay(registry: HideSpotRegistry, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val spots = registry.spots.values.toList()
    val eligible = registry.visibleAllowed().map { it.id }.toSet()
    Canvas(modifier.fillMaxSize()) {
        val stroke = Stroke(width = 2.dp.toPx())
        for (spot in spots) {
            val b = spot.bounds
            if (b.isEmpty) continue
            val color = when {
                spot.kind == SpotKind.Occluder -> Color(0xFF3B82F6)
                spot.kind == SpotKind.Forbidden -> Color(0xFFE5484D)
                spot.id in eligible -> Color(0xFF1FAE45)
                else -> Color(0xFF9AA0A6)
            }
            if (spot.kind != SpotKind.Occluder) {
                drawRect(color.copy(alpha = 0.12f), topLeft = b.topLeft, size = b.size)
            }
            drawRect(color, topLeft = b.topLeft, size = b.size, style = stroke)
            val labelX = b.left + 4.dp.toPx()
            val labelY = b.top + 2.dp.toPx()
            if (labelX < size.width - 60.dp.toPx() && labelY < size.height - 14.dp.toPx() && labelY >= 0f) {
                drawText(
                    textMeasurer = measurer,
                    text = spot.id,
                    topLeft = Offset(labelX, labelY),
                    style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color),
                )
            }
        }
    }
}
