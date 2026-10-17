package app.hideandseek

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
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
import androidx.compose.ui.unit.Density
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val CardInk = Color(0xFF1F2430)
private val CardInkSoft = Color(0xFF5B6475)

// Выход лисы после находки: «ой, нашли!» → два прыжка → убегает за край экрана.
private const val FOX_FOUND_MS = 420
private const val FOX_JUMP_MS = 600
private const val FOX_RUN_MS = 600
private const val FOX_TOTAL_MS = FOX_FOUND_MS + 2 * FOX_JUMP_MS + FOX_RUN_MS

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
    // Позы лисы грузятся заранее; пока грузятся, зверёк не выглядывает.
    val poses = rememberPoses(plan?.creature?.sprites)

    LaunchedEffect(blocked) {
        if (blocked) celebration = null // never cover payment screens, not even with the "found" card
    }

    Box(modifier.onGloballyPositioned { frame.update(it.positionInRoot(), it.size) }) {
        if (content != null && plan != null && !controller.progress.isFound(plan.day)) {
            key(plan.day, plan.spot.id, plan.creature.id) {
                PeekingCreature(controller, content, plan, frame, sprite, poses) { celebration = it }
            }
        }
        val current = celebration
        if (current != null && !blocked) {
            key(current.id) {
                CelebrationLayer(
                    c = current,
                    // Лиса и звёздочки привязаны к элементу: едут вместе с ним при прокрутке
                    // и пропадают, если элемент ушёл с экрана (переключили вкладку).
                    anchorNow = {
                        val offset = current.elementOffset
                        if (offset == null) {
                            current.anchor
                        } else {
                            controller.registry.find(current.spotId)?.let { it.bounds.topLeft + offset - frame.origin }
                        }
                    },
                ) {
                    if (celebration?.id == current.id) celebration = null
                    controller.afterFound() // режим демо: перепрятаться в другом месте
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
    poses: PosesState,
    onFound: (Celebration) -> Unit,
) {
    val registry = controller.registry
    val look = content.look
    val spot = plan.spot
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val sizePx = with(density) { look.sizeDp.dp.toPx() }
    val loaded = poses.sprites
    // Поза лисы для этого края (сверху — peek, сбоку — side). Нет позы — прежний зверёк: картинка или рисунок кодом.
    val pose = loaded?.forEdge(spot.edge)
    val poseSize = pose?.size(density)
    val shape = if (pose != null && poseSize != null) {
        poseShape(spot.edge, poseSize, pose.anchorX, pose.anchorY)
    } else {
        HideShape(sizePx / 2f, sizePx / 2f, sizePx * look.peek, 0f)
    }
    // Насколько выглядывать: поза — целиком (1), нарисованный зверёк — на look.peek своей высоты.
    val full = if (pose != null) 1f else look.peek
    // The tap target covers only the visible part of the creature (outside the element), at least 48dp.
    val hit = with(density) { hitBox(Offset.Zero, spot.edge, shape, shape.out + 6.dp.toPx(), 48.dp.toPx()) }
    val last = remember { LastAnchor() }

    // The point on the element's edge the creature peeks from, in overlay pixels.
    val anchor: () -> Offset? = {
        registry.find(spot.id)?.let { anchorPoint(it.bounds, spot, shape) - frame.origin }
            ?.also { last.value = it }
    }

    val eligible by remember(spot.id, shape, poses.loading) {
        derivedStateOf {
            val geometry = registry.find(spot.id)
            !poses.loading && geometry != null && !registry.isBlocked && controller.isAwakeHour &&
                isPeekVisible(geometry, spot, shape, frame.origin, frame.size)
        }
    }

    val peek = remember { Animatable(0f) }
    var appearedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(eligible) {
        if (eligible) {
            delay(if (appearedOnce) 300L else look.appearDelayMs)
            appearedOnce = true
            peek.animateTo(full, spring(dampingRatio = 0.45f, stiffness = 220f))
            // Now and then duck and peek out again, so the creature feels alive.
            while (true) {
                delay(7_000L)
                peek.animateTo(full * 0.3f, tween(durationMillis = 220))
                delay(900L)
                peek.animateTo(full, spring(dampingRatio = 0.45f, stiffness = 260f))
            }
        } else if (registry.find(spot.id) == null) {
            // Элемент ушёл с экрана (переключили вкладку): исчезаем сразу,
            // а не прячемся в пустоту там, где только что был элемент.
            peek.snapTo(0f)
        } else {
            // Элемент на месте, но уехал при скролле или экран заблокирован: прячемся за него.
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
                // Нет элемента на экране — не рисуем (иначе персонаж мелькнёт на старом месте).
                val a = anchor() ?: return@drawWithContent
                val w = size.width
                val h = size.height
                // Лапы позы лежат на самом краю элемента: открываем их в конце выглядывания.
                val paws = shape.inward * ((peek.value / full - 0.75f) / 0.25f).coerceIn(0f, 1f)
                // Боковая поза, которая ниже элемента, обрезается по его нижнему краю.
                val bottom = if (pose == null) h else registry.find(spot.id)?.let { it.bounds.bottom - frame.origin.y } ?: h
                when (spot.edge) {
                    Edge.TOP -> clipRect(0f, 0f, w, a.y + paws) { this@drawWithContent.drawContent() }
                    Edge.BOTTOM -> clipRect(0f, a.y - paws, w, h) { this@drawWithContent.drawContent() }
                    Edge.LEFT -> clipRect(0f, 0f, a.x + paws, bottom) { this@drawWithContent.drawContent() }
                    Edge.RIGHT -> clipRect(a.x - paws, 0f, w, bottom) { this@drawWithContent.drawContent() }
                }
            },
    ) {
        if (pose != null && poseSize != null) {
            Canvas(Modifier.fillMaxSize()) {
                val a = anchor() ?: last.value ?: return@Canvas
                drawHidingPose(
                    pose = pose,
                    size = poseSize,
                    edge = spot.edge,
                    anchor = a,
                    progress = peek.value / full,
                    out = shape.out,
                    blink = idle.blink.value,
                    wiggle = idle.wiggle.value,
                )
                if (rare) {
                    val center = a + spot.edge.outward() * (shape.out * 0.5f)
                    drawTwinkles(center, max(shape.before, shape.after) * 0.9f, idle.sparkle.value)
                }
            }
        } else {
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
    }

    // Tap target over the visible part only, so the element underneath stays fully usable.
    Box(
        Modifier
            .offset {
                val a = anchor() ?: last.value ?: Offset.Zero
                IntOffset((a.x + hit.left).roundToInt(), (a.y + hit.top).roundToInt())
            }
            .size(with(density) { hit.width.toDp() }, with(density) { hit.height.toDp() })
            .semantics { contentDescription = content.texts.accessibilityLabel }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
            ) {
                val a = last.value ?: return@clickable
                // Точка относительно элемента: по ней выход лисы следует за элементом при прокрутке.
                val elementOffset = registry.find(spot.id)?.let { a + frame.origin - it.bounds.topLeft }
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                controller.markFound(plan)
                onFound(
                    celebrationFor(
                        controller, content, plan, a, sizePx, peek.value, sprite,
                        poses = loaded,
                        byPose = pose != null,
                        shape = shape,
                        elementOffset = elementOffset,
                    ),
                )
            },
    )
}

// ---- Geometry ----

/**
 * Как зверёк сидит на краю (в px): сколько занимает вдоль края до якоря ([before]: левее или выше)
 * и после него ([after]), насколько торчит наружу ([out]) и насколько заходит на сам элемент ([inward], лапы).
 */
internal data class HideShape(val before: Float, val after: Float, val out: Float, val inward: Float)

/** Форма позы-картинки с якорем ([anchorX], [anchorY]) в долях её размера. */
internal fun poseShape(edge: Edge, size: Size, anchorX: Float, anchorY: Float): HideShape = when (edge) {
    Edge.TOP -> HideShape(anchorX * size.width, (1f - anchorX) * size.width, anchorY * size.height, (1f - anchorY) * size.height)
    Edge.BOTTOM -> HideShape(anchorX * size.width, (1f - anchorX) * size.width, (1f - anchorY) * size.height, anchorY * size.height)
    // Боковая поза нарисована для правого края; для левого она зеркалится, размеры те же.
    Edge.LEFT, Edge.RIGHT ->
        HideShape(anchorY * size.height, (1f - anchorY) * size.height, (1f - anchorX) * size.width, anchorX * size.width)
}

/** The point on the element's edge where the creature sits, clamped so it fits on that edge. */
internal fun anchorPoint(bounds: Rect, spot: SpotDef, shape: HideShape): Offset {
    fun along(start: Float, end: Float): Float {
        val lo = start + shape.before
        val hi = end - shape.after
        if (lo <= hi) return lo + (hi - lo) * spot.align
        // Зверёк длиннее края: ставим его по центру, но якорь не дальше четверти длины от концов края.
        val quarter = (end - start) / 4f
        return ((start + end) / 2f + (shape.before - shape.after) / 2f).coerceIn(start + quarter, end - quarter)
    }
    return when (spot.edge) {
        Edge.TOP -> Offset(along(bounds.left, bounds.right), bounds.top)
        Edge.BOTTOM -> Offset(along(bounds.left, bounds.right), bounds.bottom)
        Edge.LEFT -> Offset(bounds.left, along(bounds.top, bounds.bottom))
        Edge.RIGHT -> Offset(bounds.right, along(bounds.top, bounds.bottom))
    }
}

/** The rectangle on the outer side of the edge: [before] + [after] along it and [depth] deep. */
internal fun peekBox(anchor: Offset, edge: Edge, before: Float, after: Float, depth: Float): Rect = when (edge) {
    Edge.TOP -> Rect(anchor.x - before, anchor.y - depth, anchor.x + after, anchor.y)
    Edge.BOTTOM -> Rect(anchor.x - before, anchor.y, anchor.x + after, anchor.y + depth)
    Edge.LEFT -> Rect(anchor.x - depth, anchor.y - before, anchor.x, anchor.y + after)
    Edge.RIGHT -> Rect(anchor.x, anchor.y - before, anchor.x + depth, anchor.y + after)
}

/** Зона тапа над видимой частью зверька, не меньше [minPx] по обеим сторонам (растёт наружу от края). */
internal fun hitBox(anchor: Offset, edge: Edge, shape: HideShape, depth: Float, minPx: Float): Rect {
    val extra = max(0f, minPx - (shape.before + shape.after)) / 2f
    return peekBox(anchor, edge, shape.before + extra, shape.after + extra, max(depth, minPx))
}

/** True when the edge is on screen (not scrolled away) and the peeking creature fits in the overlay. */
internal fun isPeekVisible(
    geometry: SpotGeometry,
    spot: SpotDef,
    shape: HideShape,
    origin: Offset,
    overlaySize: IntSize,
): Boolean {
    if (geometry.visibleBounds.isEmpty || overlaySize.width <= 0 || overlaySize.height <= 0) return false
    val point = anchorPoint(geometry.bounds, spot, shape) // root coordinates
    if (!geometry.visibleBounds.inflate(1f).contains(point)) return false
    val box = peekBox(point, spot.edge, shape.before, shape.after, shape.out)
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
    /** Точка на краю элемента в момент находки, в координатах слоя. */
    val anchor: Offset,
    val spotId: String,
    /** Та же точка относительно левого верхнего угла элемента; null — не следить за элементом. */
    val elementOffset: Offset?,
    val edge: Edge,
    val sizePx: Float,
    val startPeek: Float,
    val sprite: ImageBitmap?,
    /** Позы для выхода лисы; null — прежний прыжок нарисованного зверька. */
    val poses: LoadedSprites?,
    /** Как зверёк сидел на краю (откуда выпрыгивает лиса). */
    val shape: HideShape,
    /** Картинка в карточке «Нашёл!». */
    val cardImage: ImageBitmap?,
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
    poses: LoadedSprites?,
    byPose: Boolean,
    shape: HideShape,
    elementOffset: Offset?,
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
        spotId = plan.spot.id,
        elementOffset = elementOffset,
        edge = plan.spot.edge,
        sizePx = sizePx,
        startPeek = peekNow,
        sprite = sprite,
        // Лиса выпрыгивает позами, только если и пряталась позой (а не прежним рисунком).
        poses = poses?.takeIf { byPose && it.hasCelebration },
        shape = shape,
        cardImage = poses?.celebrate?.image ?: poses?.found?.image ?: sprite,
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
private fun CelebrationLayer(
    c: Celebration,
    anchorNow: () -> Offset?,
    onFinished: () -> Unit,
) {
    val burst = remember { Animatable(0f) }
    val fox = remember { Animatable(if (c.poses != null) 0f else 1f) }
    val card = remember { Animatable(0f) }
    var cardVisible by remember { mutableStateOf(true) }
    val particles = remember(c.id) { makeParticles(c.id, c.edge, Color(c.creature.color), c.creature.rare) }

    LaunchedEffect(Unit) {
        burst.animateTo(1f, tween(durationMillis = 1300, easing = LinearEasing))
    }
    LaunchedEffect(Unit) {
        fox.animateTo(1f, tween(durationMillis = FOX_TOTAL_MS, easing = LinearEasing))
    }
    // Элемент ушёл с экрана (переключили вкладку): лиса и звёздочки сразу исчезают,
    // а карточка «Нашёл!» уезжает. Касания по карточке и мимо неё её не закрывают —
    // только смахивание вверх, смена вкладки или время из look.messageMs.
    LaunchedEffect(Unit) {
        snapshotFlow { anchorNow() == null }.first { it }
        burst.snapTo(1f)
        fox.snapTo(1f)
        cardVisible = false
    }
    LaunchedEffect(cardVisible) {
        if (cardVisible) {
            card.animateTo(1f, tween(durationMillis = 280))
            delay(c.messageMs)
            cardVisible = false
        } else {
            card.animateTo(0f, tween(durationMillis = 220))
            while (burst.value < 1f || fox.value < 1f) delay(50L)
            onFinished()
        }
    }

    // Pure drawing, no touch handling.
    Canvas(Modifier.fillMaxSize()) {
        val a = anchorNow() ?: return@Canvas
        drawCelebration(c, a, particles, burst.value, fox.value)
    }

    val cardShown by remember { derivedStateOf { card.value > 0.01f } }
    if (cardShown) {
        val statusTop = WindowInsets.statusBars.getTop(LocalDensity.current)
        val navBottom = WindowInsets.navigationBars.getBottom(LocalDensity.current)
        // Карточка сверху экрана, а если там лиса — сразу под ней, чтобы её не закрывать.
        Layout(
            content = { FoundCard(c, appear = { card.value }, onDismiss = { cardVisible = false }) },
            modifier = Modifier.fillMaxSize(),
        ) { measurables, constraints ->
            val placeable = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
            val screen = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
            val y = cardTop(
                cardHeight = placeable.height.toFloat(),
                natural = statusTop + 10.dp.toPx(),
                bottomLimit = screen.height - navBottom - 96.dp.toPx(),
                fox = foxArea(c, this, screen),
                gap = 12.dp.toPx(),
            )
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.place((constraints.maxWidth - placeable.width) / 2, y.roundToInt())
            }
        }
    }
}

/**
 * Верх карточки: обычно [natural] (под статус-баром). Если там лиса ([fox] — где она по высоте
 * во время выхода), то сразу под лисой, если карточка там помещается до [bottomLimit].
 */
internal fun cardTop(
    cardHeight: Float,
    natural: Float,
    bottomLimit: Float,
    fox: ClosedFloatingPointRange<Float>?,
    gap: Float,
): Float {
    if (fox == null) return natural
    val overlaps = natural < fox.endInclusive + gap && natural + cardHeight > fox.start - gap
    if (!overlaps) return natural
    val below = fox.endInclusive + gap
    return if (below + cardHeight <= bottomLimit) below else natural
}

/** Где по высоте лиса во время выхода после находки (в момент тапа), в координатах слоя. */
private fun foxArea(c: Celebration, density: Density, screen: Size): ClosedFloatingPointRange<Float>? {
    val poses = c.poses
    if (poses != null) {
        val stage = foxStage(c, poses, density, screen) ?: return null
        return (stage.fitted.y - stage.tallest - stage.jump)..stage.fitted.y
    }
    // Прежний зверёк выпрыгивает из-за края примерно на два своих роста.
    val s = c.sizePx
    val y = c.anchor.y
    return when (c.edge) {
        Edge.TOP -> (y - 2.3f * s)..(y + 0.3f * s)
        Edge.BOTTOM -> (y - 0.3f * s)..(y + 2.3f * s)
        Edge.LEFT, Edge.RIGHT -> (y - 1.4f * s)..(y + 1.4f * s)
    }
}

/** [a] — где сейчас точка на краю элемента (элемент мог уехать при прокрутке). */
private fun DrawScope.drawCelebration(c: Celebration, a: Offset, particles: List<Particle>, t: Float, foxT: Float) {
    val s = c.sizePx
    val color = Color(c.creature.color)
    val poses = c.poses
    if (t < 1f) {
        val burstCenter = a + c.edge.outward() * (if (poses != null) c.shape.out * 0.5f else 0.55f * s)
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
    }

    // Лиса поверх звёздочек: «ой, нашли!», два прыжка, убегает.
    if (poses != null) {
        drawFoxCelebration(c, a, poses, foxT)
        return
    }
    if (t >= 1f) return

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
        translate(a.x, a.y)
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

/**
 * Выход лисы после тапа, [t] от 0 до 1 за [FOX_TOTAL_MS]:
 * поза found — «поп» 0,8 → 1,1 → 1,0 и маленький прыжок от края;
 * celebrate — два прыжка с приземлением (сплющивание 0,9 по высоте);
 * run — наклон 8° и рывок за ближайший край экрана с покачиванием.
 * Если какой-то позы нет, её место занимает соседняя.
 */
/** Как лиса выходит после находки: позы трёх фаз, их размеры и место, где она стоит. */
private class FoxStage(
    val first: LoadedPose,
    val second: LoadedPose,
    val third: LoadedPose,
    val firstSize: Size,
    val secondSize: Size,
    val thirdSize: Size,
    val tallest: Float,
    val jump: Float,
    /** Где лиса стоит в момент находки, сдвинутая так, чтобы целиком была на экране. */
    val fitted: Offset,
    /** Этот сдвиг; дальше лиса едет вместе с элементом. */
    val shift: Offset,
)

/** Где лиса стоит: на верхнем крае элемента или рядом со стеной, из-за которой выглядывала. */
private fun standAt(c: Celebration, p: Offset): Offset = when (c.edge) {
    Edge.TOP -> p
    Edge.RIGHT -> Offset(p.x + c.shape.out * 0.55f, p.y + c.shape.after)
    Edge.LEFT -> Offset(p.x - c.shape.out * 0.55f, p.y + c.shape.after)
    Edge.BOTTOM -> Offset(p.x, p.y + c.shape.out)
}

private fun foxStage(c: Celebration, poses: LoadedSprites, density: Density, screen: Size): FoxStage? {
    val first = poses.found ?: poses.celebrate ?: poses.run ?: return null
    val second = poses.celebrate ?: poses.found ?: poses.run ?: return null
    val third = poses.run ?: poses.celebrate ?: poses.found ?: return null
    val firstSize = first.size(density)
    val secondSize = second.size(density)
    val thirdSize = third.size(density)
    val tallest = max(firstSize.height, max(secondSize.height, thirdSize.height))
    val widest = max(firstSize.width, max(secondSize.width, thirdSize.width))
    val jump = with(density) { 30.dp.toPx() }
    val margin = with(density) { 8.dp.toPx() }
    // В момент находки лиса целиком на экране, вместе с прыжком. Сдвиг считаем один раз
    // по исходной точке, а дальше лиса едет вместе с элементом.
    val start = standAt(c, c.anchor)
    val minX = widest / 2f + margin
    val minY = tallest + jump + margin
    val fitted = Offset(
        start.x.coerceIn(minX, max(minX, screen.width - widest / 2f - margin)),
        start.y.coerceIn(minY, max(minY, screen.height - margin)),
    )
    return FoxStage(first, second, third, firstSize, secondSize, thirdSize, tallest, jump, fitted, fitted - start)
}

private fun DrawScope.drawFoxCelebration(c: Celebration, a: Offset, poses: LoadedSprites, t: Float) {
    if (t >= 1f) return
    val stage = foxStage(c, poses, this, size) ?: return
    val first = stage.first
    val second = stage.second
    val third = stage.third
    val firstSize = stage.firstSize
    val secondSize = stage.secondSize
    val thirdSize = stage.thirdSize
    val jump = stage.jump
    val fitted = stage.fitted
    val ground = standAt(c, a) + stage.shift

    val ms = t * FOX_TOTAL_MS
    val jumpsEnd = FOX_FOUND_MS + 2f * FOX_JUMP_MS
    when {
        ms < FOX_FOUND_MS -> {
            val p = ms / FOX_FOUND_MS
            val pop = if (p < 0.5f) {
                0.8f + 0.3f * easeOut(p / 0.5f)
            } else {
                1.1f - 0.1f * easeOut((p - 0.5f) / 0.5f)
            }
            val lift = sin(PI.toFloat() * p) * 16.dp.toPx()
            drawPose(first, first.image, Offset(ground.x, ground.y - lift), firstSize, scaleX = pop, scaleY = pop)
        }
        ms < jumpsEnd -> {
            val q = (ms - FOX_FOUND_MS) / FOX_JUMP_MS
            val p = q - floor(q)
            val lift = 4f * p * (1f - p) * jump
            // Сплющивание на приземлении и толчке.
            val squash = when {
                p < 0.12f -> 0.9f + 0.1f * (p / 0.12f)
                p > 0.9f -> 1f - 0.1f * ((p - 0.9f) / 0.1f)
                else -> 1f
            }
            drawPose(
                second, second.image, Offset(ground.x, ground.y - lift), secondSize,
                scaleX = 1f + (1f - squash) * 0.6f, scaleY = squash,
            )
        }
        else -> {
            val p = ((ms - jumpsEnd) / FOX_RUN_MS).coerceIn(0f, 1f)
            val toLeft = fitted.x < size.width / 2f
            val target = if (toLeft) -thirdSize.width else size.width + thirdSize.width
            val x = ground.x + (target - ground.x) * p * p
            val bob = abs(sin(p * 3f * PI.toFloat())) * 8.dp.toPx()
            drawPose(
                third, third.image, Offset(x, ground.y - bob), thirdSize,
                mirror = toLeft, rotation = if (toLeft) -8f else 8f,
            )
        }
    }
}

private fun easeOut(x: Float): Float {
    val v = 1f - x.coerceIn(0f, 1f)
    return 1f - v * v
}

@Composable
private fun FoundCard(c: Celebration, appear: () -> Float, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val accent = Color(c.creature.color)
    val scope = rememberCoroutineScope()
    // Насколько карточку утянули пальцем (вверх — минус).
    val drag = remember { Animatable(0f) }
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .graphicsLayer {
                val a = appear()
                val swiped = (-drag.value / (size.height * 0.8f)).coerceIn(0f, 1f)
                alpha = a * (1f - swiped)
                translationY = (1f - a) * -32.dp.toPx() + drag.value
                scaleX = 0.94f + 0.06f * a
                scaleY = 0.94f + 0.06f * a
            }
            .shadow(elevation = 12.dp, shape = shape)
            .background(Color.White, shape)
            // Смахивание вверх убирает карточку. Касание по ней ничего не делает и не проходит к экрану под ней.
            .pointerInput(Unit) {
                val tracker = VelocityTracker()
                val dismissDistance = 48.dp.toPx()
                val maxDown = 24.dp.toPx()
                var offset = 0f
                detectVerticalDragGestures(
                    onDragStart = {
                        tracker.resetTracking()
                        offset = drag.value
                    },
                    onDragEnd = {
                        val flungUp = tracker.calculateVelocity().y < -1200f
                        if (offset < -dismissDistance || flungUp) {
                            onDismiss()
                            scope.launch { drag.animateTo(-size.height * 1.5f, tween(durationMillis = 200)) }
                        } else {
                            scope.launch { drag.animateTo(0f, spring(dampingRatio = 0.6f)) }
                        }
                    },
                    onDragCancel = { scope.launch { drag.animateTo(0f, spring(dampingRatio = 0.6f)) } },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        // Вверх — свободно, вниз — туго и недалеко.
                        offset = (offset + if (offset + amount > 0f) amount * 0.25f else amount).coerceAtMost(maxDown)
                        tracker.addPosition(change.uptimeMillis, Offset(0f, offset))
                        scope.launch { drag.snapTo(offset) }
                    },
                )
            }
            .padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 8.dp),
    ) {
        FoundCardContent(c, accent)
        // «Ручка» снизу: подсказка, что карточку можно смахнуть.
        Box(
            Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 4.dp)
                .background(CardInkSoft.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
private fun FoundCardContent(c: Celebration, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val image = c.cardImage
        if (image != null) {
            Image(bitmap = image, contentDescription = null, modifier = Modifier.size(52.dp))
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
