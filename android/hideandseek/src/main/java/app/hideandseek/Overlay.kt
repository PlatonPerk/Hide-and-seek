package app.hideandseek

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hideandseek.core.CreatureDef
import app.hideandseek.core.DailyPlan
import app.hideandseek.core.Edge
import app.hideandseek.core.HideAndSeekContent
import app.hideandseek.core.SpotDef
import app.hideandseek.core.TextTemplate
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val CardInk = Color(0xFF1F2430)
private val CardInkSoft = Color(0xFF5B6475)

/**
 * The layer above the app. It has no touch handling of its own: only the small
 * box over the visible part of the creature takes taps, everything else falls
 * through to the screen underneath.
 */
@Composable
internal fun HideAndSeekOverlay(controller: HideAndSeekController, modifier: Modifier) {
    val content = controller.content
    val plan = controller.plan
    val frame = remember { OverlayFrame() }
    var celebration by remember { mutableStateOf<Celebration?>(null) }
    val blocked = controller.registry.isBlocked
    // Starts loading today's sprite (if the content has one) before the creature shows up.
    val sprite = rememberSprite(plan?.creature?.imageUrl)

    LaunchedEffect(blocked) {
        if (blocked) celebration = null // never cover payment screens, not even with the "found" card
    }

    Box(modifier.onGloballyPositioned { frame.update(it.positionInRoot(), it.size) }) {
        if (content != null && plan != null && !controller.progress.isFound(plan.day)) {
            key(plan.day, plan.spot.id, plan.creature.id) {
                PeekingCreature(controller, content, plan, frame, sprite) { celebration = it }
            }
        }
        val current = celebration
        if (current != null && !blocked) {
            key(current.id) {
                CelebrationLayer(current) {
                    if (celebration?.id == current.id) celebration = null
                }
            }
        }
    }
}

@Stable
internal class OverlayFrame {
    var origin by mutableStateOf(Offset.Zero)
    var size by mutableStateOf(IntSize.Zero)

    fun update(newOrigin: Offset, newSize: IntSize) {
        if (newOrigin != origin) origin = newOrigin
        if (newSize != size) size = newSize
    }
}

private class LastAnchor {
    var value: Offset? = null
}

@Composable
private fun PeekingCreature(
    controller: HideAndSeekController,
    content: HideAndSeekContent,
    plan: DailyPlan,
    frame: OverlayFrame,
    sprite: ImageBitmap?,
    onFound: (Celebration) -> Unit,
) {
    val registry = controller.registry
    val look = content.look
    val spot = plan.spot
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val sizePx = with(density) { look.sizeDp.dp.toPx() }
    // The tap target covers only the visible part of the creature (outside the element), at least 48dp.
    val hitAlongPx = with(density) { max(look.sizeDp.toFloat(), 48f).dp.toPx() }
    val hitDepthPx = with(density) { max(look.sizeDp * look.peek + 6f, 48f).dp.toPx() }
    val last = remember { LastAnchor() }

    // The point on the element's edge the creature peeks from, in overlay pixels.
    val anchor: () -> Offset? = {
        registry.find(spot.id)?.let { anchorPoint(it.bounds, spot, sizePx) - frame.origin }
            ?.also { last.value = it }
    }

    val eligible by remember(spot.id, sizePx) {
        derivedStateOf {
            val geometry = registry.find(spot.id)
            geometry != null && !registry.isBlocked && controller.isAwakeHour &&
                isPeekVisible(geometry, spot, sizePx, look.peek, frame.origin, frame.size)
        }
    }

    val peek = remember { Animatable(0f) }
    var appearedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(eligible) {
        if (eligible) {
            delay(if (appearedOnce) 300L else look.appearDelayMs)
            appearedOnce = true
            peek.animateTo(look.peek, spring(dampingRatio = 0.45f, stiffness = 220f))
            // Now and then duck and peek out again, so the creature feels alive.
            while (true) {
                delay(7_000L)
                peek.animateTo(look.peek * 0.3f, tween(durationMillis = 220))
                delay(900L)
                peek.animateTo(look.peek, spring(dampingRatio = 0.45f, stiffness = 260f))
            }
        } else {
            peek.animateTo(0f, tween(durationMillis = 180))
        }
    }

    val showing by remember { derivedStateOf { peek.value > 0.02f } }
    if (!showing) return

    val idle = rememberCreatureIdle()
    val color = Color(plan.creature.color)
    val rare = plan.creature.rare

    // Drawing layer, clipped to the outer side of the edge, so the creature seems to sit behind the element.
    Box(
        Modifier
            .fillMaxSize()
            .drawWithContent {
                val a = anchor() ?: last.value ?: return@drawWithContent
                val w = size.width
                val h = size.height
                when (spot.edge) {
                    Edge.TOP -> clipRect(0f, 0f, w, a.y) { this@drawWithContent.drawContent() }
                    Edge.BOTTOM -> clipRect(0f, a.y, w, h) { this@drawWithContent.drawContent() }
                    Edge.LEFT -> clipRect(0f, 0f, a.x, h) { this@drawWithContent.drawContent() }
                    Edge.RIGHT -> clipRect(a.x, 0f, w, h) { this@drawWithContent.drawContent() }
                }
            },
    ) {
        val creatureModifier = Modifier
            .offset {
                val a = anchor() ?: last.value ?: Offset.Zero
                IntOffset((a.x - sizePx / 2f).roundToInt(), (a.y - peek.value * sizePx).roundToInt())
            }
            .size(look.sizeDp.dp)
            .graphicsLayer {
                rotationZ = spot.edge.rotationDegrees + idle.wiggle.value
                transformOrigin = TransformOrigin(0.5f, peek.value)
            }
        if (sprite != null) {
            Image(bitmap = sprite, contentDescription = null, modifier = creatureModifier)
        } else {
            Canvas(creatureModifier) {
                drawCreature(
                    s = size.width,
                    body = color,
                    eyeOpen = idle.blink.value,
                    lookX = idle.look.value,
                    pawLine = peek.value,
                    sparkle = if (rare) idle.sparkle.value else -1f,
                )
            }
        }
    }

    // Tap target over the visible part only, so the element underneath stays fully usable.
    val alongEdge = spot.edge == Edge.TOP || spot.edge == Edge.BOTTOM
    val hitWidth = with(density) { (if (alongEdge) hitAlongPx else hitDepthPx).toDp() }
    val hitHeight = with(density) { (if (alongEdge) hitDepthPx else hitAlongPx).toDp() }
    Box(
        Modifier
            .offset {
                val a = anchor() ?: last.value ?: Offset.Zero
                val box = peekBox(a, spot.edge, hitAlongPx, hitDepthPx)
                IntOffset(box.left.roundToInt(), box.top.roundToInt())
            }
            .size(hitWidth, hitHeight)
            .semantics { contentDescription = content.texts.accessibilityLabel }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
            ) {
                val a = last.value ?: return@clickable
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                controller.markFound(plan)
                onFound(celebrationFor(controller, content, plan, a, sizePx, peek.value, sprite))
            },
    )
}

// ---- Geometry ----

/** The point on the element's edge where the creature sits, clamped so it fits on that edge. */
internal fun anchorPoint(bounds: Rect, spot: SpotDef, sizePx: Float): Offset {
    val half = sizePx / 2f
    fun along(start: Float, end: Float): Float {
        val lo = start + half
        val hi = end - half
        return if (lo <= hi) lo + (hi - lo) * spot.align else (start + end) / 2f
    }
    return when (spot.edge) {
        Edge.TOP -> Offset(along(bounds.left, bounds.right), bounds.top)
        Edge.BOTTOM -> Offset(along(bounds.left, bounds.right), bounds.bottom)
        Edge.LEFT -> Offset(bounds.left, along(bounds.top, bounds.bottom))
        Edge.RIGHT -> Offset(bounds.right, along(bounds.top, bounds.bottom))
    }
}

/** The rectangle on the outer side of the edge: [along] wide and [depth] deep. */
internal fun peekBox(anchor: Offset, edge: Edge, along: Float, depth: Float): Rect {
    val h = along / 2f
    return when (edge) {
        Edge.TOP -> Rect(anchor.x - h, anchor.y - depth, anchor.x + h, anchor.y)
        Edge.BOTTOM -> Rect(anchor.x - h, anchor.y, anchor.x + h, anchor.y + depth)
        Edge.LEFT -> Rect(anchor.x - depth, anchor.y - h, anchor.x, anchor.y + h)
        Edge.RIGHT -> Rect(anchor.x, anchor.y - h, anchor.x + depth, anchor.y + h)
    }
}

/** True when the edge is on screen (not scrolled away) and the peeking creature fits in the overlay. */
internal fun isPeekVisible(
    geometry: SpotGeometry,
    spot: SpotDef,
    sizePx: Float,
    peek: Float,
    origin: Offset,
    overlaySize: IntSize,
): Boolean {
    if (geometry.visibleBounds.isEmpty || overlaySize.width <= 0 || overlaySize.height <= 0) return false
    val point = anchorPoint(geometry.bounds, spot, sizePx) // root coordinates
    if (!geometry.visibleBounds.inflate(1f).contains(point)) return false
    val box = peekBox(point, spot.edge, sizePx, sizePx * peek)
    val overlay = Rect(origin, Size(overlaySize.width.toFloat(), overlaySize.height.toFloat())).inflate(1f)
    return box.left >= overlay.left && box.top >= overlay.top &&
        box.right <= overlay.right && box.bottom <= overlay.bottom
}

internal fun Edge.outward(): Offset = when (this) {
    Edge.TOP -> Offset(0f, -1f)
    Edge.RIGHT -> Offset(1f, 0f)
    Edge.BOTTOM -> Offset(0f, 1f)
    Edge.LEFT -> Offset(-1f, 0f)
}

// ---- Found: jump out, burst, card ----

internal class Celebration(
    val id: Long,
    val creature: CreatureDef,
    val anchor: Offset,
    val edge: Edge,
    val sizePx: Float,
    val startPeek: Float,
    val sprite: ImageBitmap?,
    val title: String,
    val body: String,
    val streakLine: String?,
    val tipTitle: String,
    val tip: String?,
    val messageMs: Long,
)

private fun celebrationFor(
    controller: HideAndSeekController,
    content: HideAndSeekContent,
    plan: DailyPlan,
    anchor: Offset,
    sizePx: Float,
    peekNow: Float,
    sprite: ImageBitmap?,
): Celebration {
    val progress = controller.progress
    val values = mapOf(
        "name" to plan.creature.name,
        "streak" to progress.streak.toString(),
        "streakDays" to TextTemplate.ruDays(progress.streak),
        "total" to progress.totalFound.toString(),
    )
    val texts = content.texts
    return Celebration(
        id = System.nanoTime(),
        creature = plan.creature,
        anchor = anchor,
        edge = plan.spot.edge,
        sizePx = sizePx,
        startPeek = peekNow,
        sprite = sprite,
        title = TextTemplate.fill(texts.foundTitle, values),
        body = TextTemplate.fill(texts.foundBody, values),
        streakLine = if (progress.streak >= 2) TextTemplate.fill(texts.streak, values) else null,
        tipTitle = texts.tipTitle,
        tip = plan.tip,
        messageMs = content.look.messageMs,
    )
}

private class Particle(
    val angle: Float,
    val speed: Float,
    val radius: Float,
    val color: Color,
    val star: Boolean,
    val spin: Float,
)

private fun makeParticles(seed: Long, edge: Edge, base: Color, rare: Boolean): List<Particle> {
    val random = Random(seed)
    val gold = Color(0xFFFFD54A)
    val palette = listOf(base, gold, Color.White, Color(0xFFFF7EB6), Color(0xFF6EE7B7), Color(0xFF7CC8FF))
    val normalDegrees = when (edge) {
        Edge.TOP -> -90f
        Edge.RIGHT -> 0f
        Edge.BOTTOM -> 90f
        Edge.LEFT -> 180f
    }
    return List(if (rare) 40 else 28) {
        val degrees = normalDegrees + (random.nextFloat() * 2f - 1f) * 115f
        Particle(
            angle = (degrees * Math.PI / 180.0).toFloat(),
            speed = 0.9f + random.nextFloat() * 1.6f,
            radius = 0.035f + random.nextFloat() * 0.05f,
            color = if (rare && random.nextFloat() < 0.5f) gold else palette[random.nextInt(palette.size)],
            star = random.nextFloat() < 0.45f,
            spin = random.nextFloat() * 360f,
        )
    }
}

@Composable
private fun CelebrationLayer(c: Celebration, onFinished: () -> Unit) {
    val burst = remember { Animatable(0f) }
    val card = remember { Animatable(0f) }
    var cardVisible by remember { mutableStateOf(true) }
    val particles = remember(c.id) { makeParticles(c.id, c.edge, Color(c.creature.color), c.creature.rare) }

    LaunchedEffect(Unit) {
        burst.animateTo(1f, tween(durationMillis = 1300, easing = LinearEasing))
    }
    LaunchedEffect(cardVisible) {
        if (cardVisible) {
            card.animateTo(1f, tween(durationMillis = 280))
            delay(c.messageMs)
            cardVisible = false
        } else {
            card.animateTo(0f, tween(durationMillis = 220))
            while (burst.value < 1f) delay(50L)
            onFinished()
        }
    }

    // Pure drawing, no touch handling.
    Canvas(Modifier.fillMaxSize()) {
        drawCelebration(c, particles, burst.value)
    }

    val cardShown by remember { derivedStateOf { card.value > 0.01f } }
    if (cardShown) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            FoundCard(c, appear = { card.value }, onClick = { cardVisible = false })
        }
    }
}

private fun DrawScope.drawCelebration(c: Celebration, particles: List<Particle>, t: Float) {
    if (t >= 1f) return
    val s = c.sizePx
    val color = Color(c.creature.color)
    val burstCenter = c.anchor + c.edge.outward() * (0.55f * s)
    val eased = 1f - (1f - t) * (1f - t) * (1f - t) // ease-out cubic

    // Ring
    drawCircle(
        color = color.copy(alpha = 0.55f * (1f - t)),
        radius = s * (0.3f + 1.4f * eased),
        center = burstCenter,
        style = Stroke(width = s * 0.07f * (1f - t)),
    )

    // Confetti and stars
    for (p in particles) {
        val travel = p.speed * s * 1.7f * eased
        val x = burstCenter.x + cos(p.angle) * travel
        val y = burstCenter.y + sin(p.angle) * travel + 1.2f * s * t * t
        val alpha = (1f - t * t).coerceIn(0f, 1f)
        val r = p.radius * s * (1f - 0.35f * t)
        if (p.star) {
            drawPath(star(Offset(x, y), r * 1.7f, p.spin + 200f * t), p.color.copy(alpha = alpha))
        } else {
            drawCircle(p.color.copy(alpha = alpha), radius = r, center = Offset(x, y))
        }
    }

    // The creature jumps out of its hiding place, then floats away and fades.
    val popT = (t / 0.3f).coerceIn(0f, 1f)
    val pop = 1f + 2.70158f * (popT - 1f) * (popT - 1f) * (popT - 1f) + 1.70158f * (popT - 1f) * (popT - 1f)
    val peekNow = c.startPeek + (1.05f - c.startPeek) * pop
    val rise = ((t - 0.3f) / 0.7f).coerceIn(0f, 1f)
    val lift = rise * rise * 0.9f * s
    val zoom = 1f + 0.18f * pop - 0.3f * rise
    val fade = 1f - ((t - 0.5f) / 0.5f).coerceIn(0f, 1f)
    if (fade <= 0f) return
    withTransform({
        translate(c.anchor.x, c.anchor.y)
        rotate(c.edge.rotationDegrees, pivot = Offset.Zero)
        translate(-s / 2f, -peekNow * s - lift)
        scale(zoom, zoom, pivot = Offset(s / 2f, s / 2f))
    }) {
        val sprite = c.sprite
        if (sprite != null) {
            drawImage(sprite, dstSize = IntSize(s.roundToInt(), s.roundToInt()), alpha = fade)
        } else {
            drawCreature(
                s = s,
                body = color,
                pawLine = min(peekNow, 0.97f),
                alpha = fade,
                sparkle = if (c.creature.rare) t else -1f,
                happy = true,
            )
        }
    }
}

@Composable
private fun FoundCard(c: Celebration, appear: () -> Float, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val accent = Color(c.creature.color)
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .graphicsLayer {
                val a = appear()
                alpha = a
                translationY = (1f - a) * -32.dp.toPx()
                scaleX = 0.94f + 0.06f * a
                scaleY = 0.94f + 0.06f * a
            }
            .shadow(elevation = 12.dp, shape = shape)
            .background(Color.White, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val sprite = c.sprite
        if (sprite != null) {
            Image(bitmap = sprite, contentDescription = null, modifier = Modifier.size(52.dp))
        } else {
            Canvas(Modifier.size(52.dp)) {
                drawCreature(
                    s = size.width,
                    body = accent,
                    pawLine = 0.97f,
                    sparkle = if (c.creature.rare) 0.25f else -1f,
                    happy = true,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(c.title, style = TextStyle(color = CardInk, fontSize = 18.sp, fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(2.dp))
            BasicText(c.body, style = TextStyle(color = CardInkSoft, fontSize = 14.sp, lineHeight = 19.sp))
            val streakLine = c.streakLine
            if (streakLine != null) {
                Spacer(Modifier.height(4.dp))
                BasicText(
                    "🔥 $streakLine",
                    style = TextStyle(color = CardInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                )
            }
            val tip = c.tip
            if (tip != null) {
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    BasicText(c.tipTitle, style = TextStyle(color = CardInk, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    BasicText(tip, style = TextStyle(color = CardInk, fontSize = 14.sp, lineHeight = 19.sp))
                }
            }
        }
    }
}
