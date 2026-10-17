@file:OptIn(ExperimentalFoundationApi::class)

package ru.hideandseek.nychka.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val PressSpring = spring<Float>(stiffness = Spring.StiffnessHigh)
private val ReleaseSpring = spring<Float>(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium)

/**
 * Единый «живой» отклик на нажатие: элемент проседает под пальцем и пружинит обратно,
 * плюс рябь (ripple) в форме элемента и лёгкая вибрация.
 *
 * Пружинка срабатывает и на быстрый тап: в скроллящихся списках Compose показывает нажатие
 * с задержкой, поэтому без отдельной анимации на клик короткие тапы выглядели бы «мёртвыми».
 *
 * @param shape форма элемента: по ней обрезается рябь. null — без обрезки (для элементов,
 *   у которых декор выходит за границы, например большие цифры «Топ 5»).
 */
fun Modifier.tappable(
    shape: Shape? = null,
    pressedScale: Float = 0.96f,
    ripple: Boolean = true,
    haptic: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit = {},
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val pop = remember { arrayOfNulls<Job>(1) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(pressed) {
        if (pressed) {
            scale.animateTo(pressedScale, PressSpring)
        } else if (pop[0]?.isActive != true) {
            scale.animateTo(1f, ReleaseSpring)
        }
    }

    val base = Modifier.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
    val clipped = if (shape != null) base.clip(shape) else base
    clipped.combinedClickable(
        interactionSource = source,
        indication = if (ripple) LocalIndication.current else null,
        onLongClick = onLongClick,
        onClick = {
            if (haptic) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            pop[0] = scope.launch {
                if (scale.value > pressedScale + 0.005f) scale.animateTo(pressedScale, tween(durationMillis = 70))
                scale.animateTo(1f, ReleaseSpring)
            }
            onClick()
        },
    )
}
