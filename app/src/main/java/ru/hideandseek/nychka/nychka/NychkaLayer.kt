package ru.hideandseek.nychka.nychka

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.hideandseek.nychka.R
import ru.hideandseek.nychka.hidespots.HideSpotRegistry
import ru.hideandseek.nychka.hidespots.SpotKind
import ru.hideandseek.nychka.ui.components.GradientButton
import ru.hideandseek.nychka.ui.components.WideButton
import ru.hideandseek.nychka.ui.theme.KidsColors
import ru.hideandseek.nychka.ui.theme.KidsType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// Пропорции поз (по нарезанным картинкам в res/drawable-nodpi)
private const val PEEK_ASPECT = 360f / 264f // ширина / высота
private const val PEEK_EDGE = 667f / 709f // где на картинке «край укрытия» (лапы лежат на нём)
private const val SURPRISED_ASPECT = 270f / 420f
private const val CELEBRATE_ASPECT = 426f / 540f
private const val RUN_ASPECT = 453f / 420f

/** Куда и какого размера поставить выглядывающую Нычку (координаты корня, px). */
private class PeekAnchor(val key: String, val left: Float, val top: Float, val width: Float, val height: Float)

/**
 * Слой Нычки поверх экранов. Касания перехватывает только сама Нычка и карточка награды,
 * всё остальное проходит к экрану под ней — прикол не блокирует сценарии приложения.
 */
@Composable
fun NychkaLayer(
    controller: NychkaController,
    registry: HideSpotRegistry,
    debug: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val spot = controller.spot
        val anchor = if (spot != null) peekAnchor(spot, registry, screenW) else null

        when (controller.phase) {
            NychkaPhase.Peeking -> if (anchor != null) PeekingNychka(anchor, controller)
            NychkaPhase.Caught -> if (anchor != null) {
                CaughtNychka(anchor, controller)
            } else {
                LaunchedEffect(Unit) { controller.onCaughtShown() }
            }
            NychkaPhase.Celebrate -> CelebrateCard(controller)
            NychkaPhase.Running -> RunningNychka(controller, screenW, screenH)
            NychkaPhase.Done -> Unit
        }
        if (debug) DebugBadge(controller, visible = anchor != null)
    }
}

/**
 * Считает позицию Нычки над местом дня: линия её лап совпадает с «краем укрытия»,
 * а сама она уменьшается, если сверху мало места до запретной зоны или статус-бара.
 * null — место сейчас не на экране (другая вкладка, проскроллено, под таб-баром).
 */
@Composable
private fun peekAnchor(cfg: SpotConfig, registry: HideSpotRegistry, screenW: Float): PeekAnchor? {
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.getTop(density).toFloat()
    val spot = registry.spots[cfg.id] ?: return null
    if (registry.visibleAllowed().none { it.id == cfg.id }) return null
    val raw = spot.raw
    if (raw.top < spot.bounds.top - 1f) return null // верх укрытия уехал за край экрана
    with(density) {
        val margin = 8.dp.toPx()
        val edgeY = raw.top + raw.height * cfg.edge
        val blockers = registry.spots.values
            .filter { it.kind != SpotKind.Allowed && !it.bounds.isEmpty }
            .map { it.bounds }

        fun leftFor(width: Float) =
            (raw.left + raw.width * cfg.align - width / 2).coerceIn(margin, max(margin, screenW - width - margin))

        fun topLimit(left: Float, width: Float): Float {
            val above = blockers.filter { it.bottom <= edgeY + 1f && it.right > left && it.left < left + width }
            return max(statusTop, above.maxOfOrNull { it.bottom } ?: 0f)
        }

        var width = min(120.dp.toPx(), raw.width * 0.8f).coerceAtLeast(64.dp.toPx())
        var left = leftFor(width)
        val available = edgeY - topLimit(left, width) - 4.dp.toPx()
        if (width / PEEK_ASPECT * PEEK_EDGE > available) {
            width = max(56.dp.toPx(), available / PEEK_EDGE * PEEK_ASPECT)
            left = leftFor(width)
        }
        val height = width / PEEK_ASPECT
        return PeekAnchor(cfg.id, left, edgeY - height * PEEK_EDGE, width, height)
    }
}

@Composable
private fun PeekingNychka(a: PeekAnchor, controller: NychkaController) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    // поднимается из-за края укрытия
    val rise = remember(a.key) { Animatable(1f) }
    LaunchedEffect(a.key) {
        rise.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
    }
    // и чуть покачивается, пока её не нашли
    val sway: State<Float> = if (controller.liveAnimations) {
        rememberInfiniteTransition(label = "sway").animateFloat(
            initialValue = -3f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "swayAngle",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Box(
        Modifier
            .offset { IntOffset(a.left.roundToInt(), a.top.roundToInt()) }
            .size(with(density) { a.width.toDp() }, with(density) { a.height.toDp() })
            .clipToBounds(), // всё, что ниже лап, — за укрытием
    ) {
        Image(
            painter = painterResource(R.drawable.nychka_peek),
            contentDescription = "Нычка",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = rise.value * size.height
                    rotationZ = sway.value
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    controller.onFound()
                },
        )
    }
}

@Composable
private fun CaughtNychka(a: PeekAnchor, controller: NychkaController) {
    val density = LocalDensity.current
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
        if (controller.liveAnimations) {
            delay(700)
            controller.onCaughtShown()
        }
    }
    val h = a.height * 1.2f
    val w = h * SURPRISED_ASPECT
    val left = a.left + a.width / 2 - w / 2
    val top = a.top + a.height - h
    Image(
        painter = painterResource(R.drawable.nychka_surprised),
        contentDescription = "Нычка: ой, нашли!",
        modifier = Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .size(with(density) { w.toDp() }, with(density) { h.toDp() })
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
    )
}

@Composable
private fun CelebrateCard(controller: NychkaController) {
    val context = LocalContext.current
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
        if (controller.liveAnimations) {
            delay(8000) // карточка не висит вечно и не требует действий
            controller.onCelebrateClosed()
        }
    }
    val texts = controller.config.texts
    val newSeries = controller.lastResult?.newSeries == true
    val cardShape = RoundedCornerShape(28.dp)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    val s = 0.85f + 0.15f * appear.value
                    scaleX = s
                    scaleY = s
                    alpha = appear.value.coerceIn(0f, 1f)
                },
        ) {
            Column(
                Modifier
                    .padding(top = 120.dp)
                    .shadow(24.dp, cardShape, ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
                    .clip(cardShape)
                    .background(Color.White)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(start = 24.dp, end = 24.dp, top = 72.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    controller.foundPhrase,
                    style = KidsType.Display.copy(fontSize = 22.sp),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (newSeries) texts.newSeries else controller.format(texts.streak),
                    style = KidsType.Body.copy(color = KidsColors.Green, fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    controller.format(texts.total) + " · " + controller.format(texts.best),
                    style = KidsType.Caption,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                GradientButton("Поделиться", onClick = { share(context, controller.format(texts.share)) })
                Spacer(Modifier.height(10.dp))
                WideButton(
                    "Ура!",
                    color = KidsColors.GreyButton,
                    radius = 28.dp,
                    height = 52.dp,
                    onClick = { controller.onCelebrateClosed() },
                )
            }
            Image(
                painter = painterResource(R.drawable.nychka_celebrate),
                contentDescription = "Нычка радуется",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = (180 * CELEBRATE_ASPECT).dp, height = 180.dp),
            )
        }
    }
}

@Composable
private fun RunningNychka(controller: NychkaController, screenW: Float, screenH: Float) {
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 750, easing = FastOutLinearInEasing))
        controller.onRunFinished()
    }
    val h = with(density) { 130.dp.toPx() }
    val w = h * RUN_ASPECT
    val startX = screenW / 2 - w / 2
    val endX = screenW + w
    val baseY = screenH / 2 - h / 2
    val bob = with(density) { 14.dp.toPx() }
    Image(
        painter = painterResource(R.drawable.nychka_run),
        contentDescription = "Нычка убегает",
        modifier = Modifier
            .offset {
                val p = progress.value
                IntOffset(
                    (startX + (endX - startX) * p).roundToInt(),
                    (baseY - abs(sin(p * 3 * PI)).toFloat() * bob).roundToInt(),
                )
            }
            .size(with(density) { w.toDp() }, with(density) { h.toDp() }),
    )
}

@Composable
private fun BoxScope.DebugBadge(controller: NychkaController, visible: Boolean) {
    val where = if (visible) "видна на этом экране" else "на этом экране её нет"
    Text(
        text = "Нычка: ${controller.spot?.id ?: "—"} · день +${controller.dayOffset} · ${controller.phase}\n" +
            "$where · серия ${controller.progress.streak}, всего ${controller.progress.total}\n" +
            "тап по лапке — следующий день",
        style = TextStyle(color = Color.White, fontSize = 10.sp, lineHeight = 13.sp),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 4.dp)
            .background(Color(0xCC000000), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

private fun share(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching {
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
