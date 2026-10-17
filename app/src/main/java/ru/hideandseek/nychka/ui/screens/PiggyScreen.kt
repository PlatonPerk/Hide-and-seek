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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.hideandseek.nychka.R
import ru.hideandseek.nychka.hidespots.hideSpot
import ru.hideandseek.nychka.hidespots.noHideZone
import ru.hideandseek.nychka.ui.components.Art
import ru.hideandseek.nychka.ui.components.CardShape
import ru.hideandseek.nychka.ui.components.KidsAvatar
import ru.hideandseek.nychka.ui.components.PlusButton
import ru.hideandseek.nychka.ui.components.SectionTitle
import ru.hideandseek.nychka.ui.components.kidsCard
import ru.hideandseek.nychka.ui.components.nb
import ru.hideandseek.nychka.ui.components.tappable
import ru.hideandseek.nychka.ui.theme.KidsColors
import ru.hideandseek.nychka.ui.theme.KidsType

/** Копилка из списка. [goal] = null — копилка без цели (без полоски прогресса). */
private data class PiggyBank(
    val id: String,
    @param:DrawableRes val tile: Int,
    val rubles: String,
    val kopecks: String?,
    val name: String,
    val amount: Double,
    val goal: Double? = null,
    val goalText: String? = null,
)

private val Banks = listOf(
    PiggyBank("favorite", R.drawable.piggy_tile_favorite, "30", null, "Копилка фаворит", 30.0),
    PiggyBank("default", R.drawable.piggy_tile_default, "0", null, "Копилка", 0.0),
    PiggyBank("slides", R.drawable.piggy_tile_slides, "1 675", ",50", "Yeezy Slide", 1675.5, 3000.0, "из 3 000 ₽"),
)

private val BanksAfterGoal = listOf(
    PiggyBank("boots", R.drawable.piggy_tile_boots, "450", ",50", "Ботинки", 450.5, 500.0, "из 500 ₽"),
    PiggyBank("slime", R.drawable.piggy_tile_default, "580", null, "Слайм розовый", 580.0, 700.0, "из 700 ₽"),
    PiggyBank("markers", R.drawable.piggy_tile_default, "100", ",32", "Фломастеры от А4", 100.32, 200.0, "из 200 ₽"),
    PiggyBank("band", R.drawable.piggy_tile_band, "1 800", null, "Фитнес-браслет Xiaomi mi Band 7", 1800.0, 2000.0, "из 2 000 ₽"),
    PiggyBank("robot", R.drawable.piggy_tile_robot, "3 950", null, "Робот Макс", 3950.0, 12000.0, "из 12 000 ₽"),
)

private val GoalGreen = Color(0xFF62CE54)
private val ProgressGreen = Color(0xFF68CA5B)
private val ProductGrey = Color(0xFFE6EBEF)

/**
 * Вкладка «Копилки» по макету Figma (четвёртый экран в разделе «Главный»).
 *
 * Места для пряток: piggy.avatar, piggy.add, piggy.pig (свинья на сводке), piggy.bank_<id>, piggy.goal,
 * piggy.create, piggy.others_title, piggy.product_1..2, piggy.see_all, piggy.archive.
 * Запретные зоны: piggy.total (сумма на копилках), piggy.withdraw (кнопка «Снять»).
 */
@Composable
fun PiggyScreen(modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize().background(KidsColors.Page)) {
        item(key = "header") { Header() }
        items(Banks, key = { it.id }) { bank ->
            BankCard(bank, Modifier.padding(top = 16.dp))
        }
        item(key = "goal") { GoalReachedCard(Modifier.padding(top = 16.dp)) }
        items(BanksAfterGoal, key = { it.id }) { bank ->
            BankCard(bank, Modifier.padding(top = 16.dp))
        }
        item(key = "create") { CreateButton(Modifier.padding(top = 32.dp)) }
        item(key = "others") { WhatOthersSave(Modifier.padding(top = 32.dp)) }
        item(key = "archive") { ArchiveCard(Modifier.padding(top = 22.dp)) }
        // место под плавающий таб-бар
        item(key = "bottom") { Spacer(Modifier.navigationBarsPadding().height(124.dp)) }
    }
}

@Composable
private fun Header() {
    Column(
        Modifier
            .fillMaxWidth()
            .drawWithCache {
                // жёлто-розовый верх, как в макете, плавно уходящий в фон страницы
                val band = 300.dp.toPx()
                val tint = Brush.horizontalGradient(listOf(Color(0xFFF2EAAB), Color(0xFFEFD8D2), Color(0xFFEAB9CF)))
                val fade = Brush.verticalGradient(listOf(Color.Transparent, KidsColors.Page), startY = 0f, endY = band)
                onDrawBehind {
                    drawRect(KidsColors.Page)
                    drawRect(tint, size = Size(size.width, band))
                    drawRect(fade, size = Size(size.width, band))
                }
            }
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            KidsAvatar("piggy.avatar")
            PlusButton(Modifier.hideSpot("piggy.add"))
        }
        Spacer(Modifier.height(48.dp))
        Summary()
        Spacer(Modifier.height(23.dp))
    }
}

/** Сводка «Всего на копилках» + «Годовой процент» и свинья с монетками поверх. */
@Composable
private fun Summary() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(119.dp)
                    .clip(RoundedCornerShape(topStart = 21.dp, topEnd = 21.dp))
                    .background(KidsColors.Card)
                    .padding(start = 25.dp)
                    .noHideZone("piggy.total"),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("16 158 ")
                        withStyle(SpanStyle(color = KidsColors.Kopecks)) { append("₽") }
                    },
                    style = KidsType.Balance.copy(fontSize = 30.sp, lineHeight = 34.sp),
                )
                Spacer(Modifier.height(4.dp))
                Text("Всего на копилках", style = KidsType.Caption.copy(fontSize = 14.sp, color = KidsColors.TextTertiary))
            }
            Spacer(Modifier.height(3.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(73.dp)
                    .clip(RoundedCornerShape(bottomStart = 21.dp, bottomEnd = 21.dp))
                    .background(KidsColors.Card)
                    .padding(start = 25.dp, end = 26.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Годовой процент", style = KidsType.Body)
                Spacer(Modifier.weight(1f))
                Text("11%", style = KidsType.Balance.copy(fontSize = 21.sp))
            }
        }
        Image(
            painter = painterResource(R.drawable.piggy_summary),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = (-38).dp)
                .size(Art.PiggySummary)
                .tappable(pressedScale = 0.92f, ripple = false)
                .hideSpot("piggy.pig"),
        )
    }
}

@Composable
private fun Amount(rubles: String, kopecks: String?, color: Color = KidsColors.TextPrimary) {
    Text(
        text = buildAnnotatedString {
            append(rubles)
            withStyle(SpanStyle(color = KidsColors.Kopecks)) {
                if (kopecks != null) append(kopecks)
                append(" ₽")
            }
        },
        style = KidsType.Balance.copy(fontSize = 19.sp, lineHeight = 24.sp, color = color),
    )
}

@Composable
private fun GoalProgress(fraction: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(7.dp)) {
        val r = size.height / 2
        drawLine(ProductGrey, Offset(r, r), Offset(size.width - r, r), size.height, StrokeCap.Round)
        if (fraction > 0f) {
            val end = r + (size.width - 2 * r) * fraction.coerceIn(0f, 1f)
            drawLine(ProgressGreen, Offset(r, r), Offset(end, r), size.height, StrokeCap.Round)
        }
    }
}

@Composable
private fun BankCard(bank: PiggyBank, modifier: Modifier = Modifier) {
    Row(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(128.dp)
            .tappable(CardShape, pressedScale = 0.97f)
            .background(KidsColors.Card)
            .padding(start = 24.dp, end = 25.dp)
            .hideSpot("piggy.bank_${bank.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(bank.tile), contentDescription = null, modifier = Modifier.size(80.dp))
        Spacer(Modifier.width(17.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Amount(bank.rubles, bank.kopecks)
                Spacer(Modifier.weight(1f))
                if (bank.goalText != null) {
                    Text(bank.goalText, style = KidsType.Caption.copy(color = KidsColors.TextTertiary))
                }
            }
            if (bank.goal != null) {
                Spacer(Modifier.height(9.dp))
                GoalProgress((bank.amount / bank.goal).toFloat())
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(10.dp))
            }
            Text(
                bank.name,
                style = KidsType.Body.copy(fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Зелёная карточка «Ура! Ты накопил на мечту» с кнопкой «Снять». */
@Composable
private fun GoalReachedCard(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(128.dp)
            .tappable(CardShape, pressedScale = 0.97f)
            .background(GoalGreen)
            .hideSpot("piggy.goal"),
    ) {
        Image(
            painterResource(R.drawable.piggy_goal_spark_top), null,
            Modifier.offset(x = 10.6.dp, y = 9.3.dp).size(26.5.dp, 22.5.dp),
        )
        Image(
            painterResource(R.drawable.piggy_goal_spark_bottom), null,
            Modifier.offset(x = 76.8.dp, y = 92.7.dp).size(30.5.dp, 27.8.dp),
        )
        Image(
            painterResource(R.drawable.piggy_goal_rocket), null,
            Modifier.align(Alignment.TopEnd).offset(x = (-5).dp, y = 10.6.dp).size(47.7.dp, 104.dp),
        )
        Row(
            Modifier.fillMaxSize().padding(start = 24.dp, end = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.piggy_tile_robot), contentDescription = null, modifier = Modifier.size(80.dp))
            Spacer(Modifier.width(17.dp))
            Column {
                Text(
                    "Ура! Ты_накопил\nна_мечту".nb(),
                    style = KidsType.Display.copy(fontSize = 18.sp, lineHeight = 21.sp, color = Color.White),
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .height(24.dp)
                        .tappable(RoundedCornerShape(12.dp), pressedScale = 0.92f)
                        .background(GradientBrush)
                        .padding(horizontal = 16.dp)
                        .noHideZone("piggy.withdraw"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Снять 7 989,32 ₽", style = KidsType.Caption.copy(fontSize = 14.sp, color = KidsColors.TextPrimary))
                }
            }
        }
    }
}

private val GradientBrush = Brush.horizontalGradient(
    listOf(Color(0xFF7AD8F4), Color(0xFFA5FCDC), Color(0xFFD2FC80), Color(0xFFA5FC55)),
)

@Composable
private fun GradientPill(text: String, spotId: String, modifier: Modifier = Modifier, withPlus: Boolean = false) {
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .tappable(RoundedCornerShape(28.dp))
            .background(GradientBrush)
            .hideSpot(spotId),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (withPlus) {
            Canvas(Modifier.size(18.dp)) {
                val w = size.width
                val stroke = 3.4.dp.toPx()
                drawLine(KidsColors.TextPrimary, Offset(w / 2, 0f), Offset(w / 2, w), stroke, StrokeCap.Round)
                drawLine(KidsColors.TextPrimary, Offset(0f, w / 2), Offset(w, w / 2), stroke, StrokeCap.Round)
            }
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = KidsType.Button.copy(fontSize = 17.sp))
    }
}

@Composable
private fun CreateButton(modifier: Modifier = Modifier) {
    GradientPill("Создать копилку", "piggy.create", modifier.padding(horizontal = 16.dp), withPlus = true)
}

/** «На что копят другие»: лента товаров и кнопка «Посмотреть все». */
@Composable
private fun WhatOthersSave(modifier: Modifier = Modifier) {
    Column(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .kidsCard()
            .padding(top = 22.dp, bottom = 25.dp),
    ) {
        SectionTitle(
            "На_что копят другие",
            Modifier.padding(start = 25.dp).hideSpot("piggy.others_title"),
            style = KidsType.Display.copy(fontSize = 21.sp),
        )
        Spacer(Modifier.height(26.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(listOf(1, 2)) { i -> ProductCard("piggy.product_$i") }
        }
        Spacer(Modifier.height(24.dp))
        GradientPill("Посмотреть все", "piggy.see_all", Modifier.padding(horizontal = 24.dp))
    }
}

@Composable
private fun ProductCard(spotId: String) {
    Column(
        Modifier
            .width(223.dp)
            .tappable(RoundedCornerShape(13.dp), pressedScale = 0.96f)
            .background(ProductGrey)
            .padding(24.dp)
            .hideSpot(spotId),
    ) {
        Image(
            painterResource(R.drawable.piggy_product_markers),
            contentDescription = null,
            modifier = Modifier.size(175.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Фитнес-браслет Xiaomi Mi Band 7",
            style = KidsType.Body.copy(fontSize = 16.sp, lineHeight = 20.sp),
            minLines = 2,
            maxLines = 2,
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildAnnotatedString {
                    append("2 000 000 000")
                    withStyle(SpanStyle(color = KidsColors.Kopecks)) { append(" ₽") }
                },
                style = KidsType.Balance.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Spacer(Modifier.width(8.dp))
            // «+»: добавить товар в копилку
            Box(
                Modifier
                    .size(32.dp)
                    .tappable(RoundedCornerShape(10.dp), pressedScale = 0.85f)
                    .background(Brush.verticalGradient(listOf(Color(0xFF86C3FA), Color(0xFF6EB7FB)))),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(14.dp)) {
                    val w = size.width
                    val stroke = 2.8.dp.toPx()
                    drawLine(Color.White, Offset(w / 2, 0f), Offset(w / 2, w), stroke, StrokeCap.Round)
                    drawLine(Color.White, Offset(0f, w / 2), Offset(w, w / 2), stroke, StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun ArchiveCard(modifier: Modifier = Modifier) {
    Row(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(72.dp)
            .tappable(CardShape, pressedScale = 0.97f)
            .background(KidsColors.Card)
            .padding(start = 25.dp, end = 25.dp)
            .hideSpot("piggy.archive"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_archive), contentDescription = null, modifier = Modifier.size(26.7.dp, 32.dp))
        Spacer(Modifier.width(12.dp))
        Text("Архив", style = KidsType.Body.copy(fontSize = 16.sp))
        Spacer(Modifier.weight(1f))
        Text("6 копилок", style = KidsType.Body.copy(fontSize = 16.sp, color = KidsColors.TextTertiary))
    }
}
