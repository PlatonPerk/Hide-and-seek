package ru.hideandseek.nychka.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.hideandseek.nychka.R
import ru.hideandseek.nychka.hidespots.hideSpot
import ru.hideandseek.nychka.ui.components.ArrowTile
import ru.hideandseek.nychka.ui.components.GradientButton
import ru.hideandseek.nychka.ui.components.KidsAvatar
import ru.hideandseek.nychka.ui.components.SectionTitle
import ru.hideandseek.nychka.ui.components.WideButton
import ru.hideandseek.nychka.ui.components.kidsCard
import ru.hideandseek.nychka.ui.components.nb
import ru.hideandseek.nychka.ui.components.tappable
import ru.hideandseek.nychka.ui.theme.KidsColors
import ru.hideandseek.nychka.ui.theme.KidsType

/**
 * Вкладка «Дела» по макету Figma (второй экран в разделе «Главный»).
 *
 * Места для пряток: tasks.avatar, tasks.add, tasks.start_card, tasks.go_button, tasks.section_title,
 * tasks.item_school, tasks.tile_1..3, tasks.more_button.
 */
@Composable
fun TasksScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .drawWithCache {
                // Кисти создаются один раз на размер, а не на каждый кадр скролла
                val band = 380.dp.toPx()
                val tint = Brush.horizontalGradient(listOf(Color(0xFFD2BDF6), Color(0xFFD1DAF7), Color(0xFFBCDEF9)))
                val fade = Brush.verticalGradient(listOf(Color.Transparent, KidsColors.Page), startY = 0f, endY = band)
                onDrawBehind {
                    drawRect(KidsColors.Page)
                    drawRect(brush = tint, size = Size(size.width, band))
                    drawRect(brush = fade, size = Size(size.width, band))
                }
            }
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            KidsAvatar("tasks.avatar")
            PlusButton(Modifier.hideSpot("tasks.add"))
        }
        Spacer(Modifier.height(41.dp))
        StartCard()
        Spacer(Modifier.height(46.dp))
        WhatToDo()
        Spacer(Modifier.navigationBarsPadding().height(124.dp))
    }
}

@Composable
private fun PlusButton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .tappable(RoundedCornerShape(15.dp), pressedScale = 0.9f)
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
private fun StartCard() {
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .kidsCard()
            .hideSpot("tasks.start_card"),
    ) {
        Column(Modifier.padding(start = 17.dp, end = 17.dp, top = 25.dp, bottom = 25.dp)) {
            Text(
                "Начни делать дела",
                style = KidsType.Display.copy(fontSize = 21.sp),
                modifier = Modifier.padding(start = 8.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Выбери или_придумай своё дело и_выполни его за_награду".nb(),
                style = KidsType.Caption.copy(fontSize = 14.sp, lineHeight = 19.sp, color = KidsColors.TextSubtle),
                modifier = Modifier.padding(start = 8.dp, end = 40.dp),
            )
            Spacer(Modifier.height(19.dp))
            GradientButton("За дело!", Modifier.hideSpot("tasks.go_button"))
        }
        // Декоративные «огоньки» из макета
        Flame(Modifier.align(Alignment.TopEnd).padding(top = 53.dp).size(width = 46.dp, height = 86.dp))
        Flame(
            Modifier.align(Alignment.BottomStart).offset(x = (-2).dp, y = 0.dp).size(width = 46.dp, height = 72.dp),
            mirrored = true,
        )
    }
}

@Composable
private fun Flame(modifier: Modifier = Modifier, mirrored: Boolean = false) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        fun x(v: Float) = if (mirrored) w * (1f - v) else w * v
        val outer = Path().apply {
            moveTo(x(0.45f), h)
            cubicTo(x(0.05f), h * 0.95f, x(0.0f), h * 0.6f, x(0.2f), h * 0.32f)
            cubicTo(x(0.22f), h * 0.5f, x(0.3f), h * 0.58f, x(0.38f), h * 0.62f)
            cubicTo(x(0.28f), h * 0.38f, x(0.4f), h * 0.14f, x(0.64f), h * 0.0f)
            cubicTo(x(0.56f), h * 0.24f, x(0.7f), h * 0.4f, x(0.74f), h * 0.5f)
            cubicTo(x(0.8f), h * 0.4f, x(0.86f), h * 0.33f, x(0.94f), h * 0.28f)
            cubicTo(x(1.0f), h * 0.55f, x(0.96f), h * 0.9f, x(0.45f), h)
        }
        drawPath(outer, KidsColors.FlamePurple, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val inner = Path().apply {
            moveTo(x(0.42f), h * 0.96f)
            lineTo(x(0.5f), h * 0.72f)
            lineTo(x(0.6f), h * 0.84f)
            lineTo(x(0.7f), h * 0.6f)
            lineTo(x(0.78f), h * 0.78f)
            lineTo(x(0.86f), h * 0.66f)
        }
        drawPath(inner, KidsColors.FlameOrange, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun WhatToDo() {
    // Белая подложка уже ряда плиток: плитки выезжают за правый край карточки, как в макете
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .padding(horizontal = 16.dp)
                .kidsCard(),
        )
        Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 16.dp)) {
            SectionTitle(
                "Чем заняться",
                Modifier.padding(start = 41.dp).hideSpot("tasks.section_title"),
                style = KidsType.Display.copy(fontSize = 21.sp),
            )
            Spacer(Modifier.height(22.dp))
            SchoolItem(Modifier.padding(start = 40.dp, end = 40.dp).hideSpot("tasks.item_school"))
            Spacer(Modifier.height(13.dp))
            LazyRow(
                contentPadding = PaddingValues(start = 40.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { TaskTile(R.drawable.tile_task_piggy, "Отмыть стену\nот художеств", "tasks.tile_1") }
                item { TaskTile(R.drawable.tile_task_board, "Отмыть стену\nот художеств", "tasks.tile_2") }
                item { TaskTile(R.drawable.tile_task_piggy, "Полить\nцветы", "tasks.tile_3") }
            }
            Spacer(Modifier.height(24.dp))
            WideButton(
                text = "А чё ещё есть?",
                color = KidsColors.GreyButton,
                radius = 28.dp,
                height = 55.dp,
                modifier = Modifier.padding(horizontal = 32.dp).hideSpot("tasks.more_button"),
            )
        }
    }
}

@Composable
private fun SchoolItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .tappable(RoundedCornerShape(20.dp), pressedScale = 0.97f)
            .background(KidsColors.ItemGrey)
            .padding(start = 24.dp, end = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Собраться\nв_школу".nb(), style = KidsType.TileTitle, modifier = Modifier.weight(1f))
        ArrowTile()
    }
}

@Composable
private fun TaskTile(@DrawableRes image: Int, title: String, spotId: String) {
    Box(
        Modifier
            .size(width = 160.dp, height = 211.dp)
            .tappable(RoundedCornerShape(19.dp), pressedScale = 0.95f)
            .background(KidsColors.Card)
            .hideSpot(spotId),
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = title,
            style = KidsType.TileTitle.copy(fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
            modifier = Modifier.padding(start = 16.dp, top = 18.dp),
        )
    }
}
