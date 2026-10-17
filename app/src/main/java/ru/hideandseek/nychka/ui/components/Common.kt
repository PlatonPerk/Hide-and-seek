package ru.hideandseek.nychka.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.hideandseek.nychka.R
import ru.hideandseek.nychka.hidespots.hideSpot
import ru.hideandseek.nychka.ui.theme.KidsColors
import ru.hideandseek.nychka.ui.theme.KidsType

/** В макетах «_» обозначает неразрывный пробел («в_школу»). */
fun String.nb(): String = replace('_', ' ')

val CardShape = RoundedCornerShape(21.dp)

/** Белая карточка со скруглением, как в макете. */
fun Modifier.kidsCard(color: Color = KidsColors.Card, radius: Dp = 21.dp): Modifier =
    this.clip(RoundedCornerShape(radius)).background(color)

@Composable
fun KidsAvatar(spotId: String, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.avatar),
        contentDescription = "Профиль",
        modifier = modifier
            .size(56.dp)
            .tappable(CircleShape, pressedScale = 0.9f)
            .hideSpot(spotId),
    )
}

/** Белая квадратная кнопка с зелёным «+» (шапка «Дел» и «Копилок»). */
@Composable
fun PlusButton(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .size(56.dp)
            .tappable(RoundedCornerShape(15.dp), pressedScale = 0.9f, onClick = onClick)
            .background(KidsColors.Card),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp)) {
            val w = size.width
            val stroke = 3.6.dp.toPx()
            drawLine(Color(0xFF5CC84A), Offset(w / 2, 0f), Offset(w / 2, w), stroke, StrokeCap.Round)
            drawLine(Color(0xFF5CC84A), Offset(0f, w / 2), Offset(w, w / 2), stroke, StrokeCap.Round)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, style: TextStyle = KidsType.Display) {
    Text(text = text.nb(), style = style, modifier = modifier)
}

/** Широкая кнопка-«таблетка» на всю ширину («Посмотреть историю», «А чё ещё есть?»). */
@Composable
fun WideButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = KidsColors.Card,
    radius: Dp = 18.dp,
    height: Dp = 56.dp,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .tappable(RoundedCornerShape(radius), onClick = onClick)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text.nb(), style = KidsType.Button)
    }
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .tappable(RoundedCornerShape(28.dp), onClick = onClick)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF90CBFF),
                        Color(0xFF9DF6E0),
                        Color(0xFFDAFD7F),
                        Color(0xFFB7FC63),
                        Color(0xFFA5F65C),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = KidsType.Button)
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.height(5.dp)) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(KidsColors.Track, cornerRadius = r)
        drawRoundRect(
            KidsColors.Purple,
            size = Size(size.width * fraction.coerceIn(0f, 1f), size.height),
            cornerRadius = r,
        )
    }
}

/** Зелёная плашка «пройдено». */
@Composable
fun CheckBadge(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        drawRoundRect(KidsColors.GreenBadge, cornerRadius = CornerRadius(this.size.width * 0.28f))
        drawCheck(Color.White, strokeWidth = this.size.width * 0.12f)
    }
}

/** Голубой квадрат со стрелкой вправо. */
@Composable
fun ArrowTile(modifier: Modifier = Modifier, size: Dp = 32.dp) {
    Canvas(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(Brush.verticalGradient(listOf(Color(0xFF7DC1FA), KidsColors.ArrowBlue))),
    ) {
        val w = this.size.width
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val p = Path().apply {
            moveTo(w * 0.28f, w * 0.5f); lineTo(w * 0.72f, w * 0.5f)
            moveTo(w * 0.53f, w * 0.31f); lineTo(w * 0.72f, w * 0.5f); lineTo(w * 0.53f, w * 0.69f)
        }
        drawPath(p, Color.White, style = stroke)
    }
}

@Composable
fun PlayIcon(modifier: Modifier = Modifier, color: Color = KidsColors.TextTertiary, size: Dp = 12.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val p = Path().apply {
            moveTo(w * 0.18f, w * 0.08f)
            lineTo(w * 0.92f, w * 0.5f)
            lineTo(w * 0.18f, w * 0.92f)
            close()
        }
        drawPath(p, color)
    }
}

@Composable
fun PauseIcon(modifier: Modifier = Modifier, color: Color = KidsColors.TextPrimary, size: Dp = 16.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val r = CornerRadius(w * 0.08f)
        drawRoundRect(color, topLeft = Offset(w * 0.2f, w * 0.1f), size = Size(w * 0.2f, w * 0.8f), cornerRadius = r)
        drawRoundRect(color, topLeft = Offset(w * 0.6f, w * 0.1f), size = Size(w * 0.2f, w * 0.8f), cornerRadius = r)
    }
}

@Composable
fun CourseIcon(modifier: Modifier = Modifier, color: Color = KidsColors.TextTertiary, size: Dp = 16.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val cap = Path().apply {
            moveTo(w * 0.5f, w * 0.15f); lineTo(w, w * 0.4f); lineTo(w * 0.5f, w * 0.65f); lineTo(0f, w * 0.4f); close()
        }
        drawPath(cap, color)
        drawRect(color, topLeft = Offset(w * 0.25f, w * 0.48f), size = Size(w * 0.5f, w * 0.32f))
    }
}

@Composable
fun HeartIcon(modifier: Modifier = Modifier, color: Color = KidsColors.TextTertiary, size: Dp = 16.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val p = Path().apply {
            moveTo(w * 0.5f, w * 0.9f)
            cubicTo(w * 0.05f, w * 0.6f, -w * 0.05f, w * 0.15f, w * 0.28f, w * 0.12f)
            cubicTo(w * 0.4f, w * 0.11f, w * 0.47f, w * 0.18f, w * 0.5f, w * 0.26f)
            cubicTo(w * 0.53f, w * 0.18f, w * 0.6f, w * 0.11f, w * 0.72f, w * 0.12f)
            cubicTo(w * 1.05f, w * 0.15f, w * 0.95f, w * 0.6f, w * 0.5f, w * 0.9f)
            close()
        }
        drawPath(p, color)
    }
}

/** Заглушка картинки (голубой прямоугольник с иконкой «изображение»). */
@Composable
fun ImagePlaceholder(modifier: Modifier = Modifier) {
    Canvas(modifier.kidsCard(KidsColors.PlaceholderBlue, radius = 11.dp)) {
        val iw = 34.dp.toPx()
        val ih = 28.dp.toPx()
        val tl = Offset((size.width - iw) / 2, (size.height - ih) / 2)
        drawRoundRect(KidsColors.PlaceholderIcon, topLeft = tl, size = Size(iw, ih), cornerRadius = CornerRadius(4.dp.toPx()))
        val m = Path().apply {
            moveTo(tl.x + iw * 0.12f, tl.y + ih * 0.85f)
            lineTo(tl.x + iw * 0.42f, tl.y + ih * 0.42f)
            lineTo(tl.x + iw * 0.62f, tl.y + ih * 0.7f)
            lineTo(tl.x + iw * 0.74f, tl.y + ih * 0.55f)
            lineTo(tl.x + iw * 0.9f, tl.y + ih * 0.85f)
            close()
        }
        drawPath(m, Color.White)
        drawCircle(Color.White, radius = ih * 0.1f, center = Offset(tl.x + iw * 0.74f, tl.y + ih * 0.3f))
    }
}

fun DrawScope.drawCheck(color: Color, strokeWidth: Float) {
    val w = size.width
    val p = Path().apply {
        moveTo(w * 0.28f, w * 0.52f)
        lineTo(w * 0.44f, w * 0.67f)
        lineTo(w * 0.73f, w * 0.36f)
    }
    drawPath(p, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Мягкое радиальное свечение для фонов экранов. */
fun DrawScope.glow(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center = center, radius = radius),
        radius = radius,
        center = center,
    )
}
