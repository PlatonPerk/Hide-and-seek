package ru.hideandseek.nychka.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.hideandseek.nychka.R
import ru.hideandseek.nychka.hidespots.hideSpot
import ru.hideandseek.nychka.hidespots.noHideZone
import ru.hideandseek.nychka.ui.components.Art
import ru.hideandseek.nychka.ui.components.KidsAvatar
import ru.hideandseek.nychka.ui.components.SectionTitle
import ru.hideandseek.nychka.ui.components.WideButton
import ru.hideandseek.nychka.ui.components.tappable
import ru.hideandseek.nychka.ui.theme.KidsColors
import ru.hideandseek.nychka.ui.theme.KidsType

/**
 * Вкладка «Главный» по макету Figma «Экраны главного».
 *
 * Места для пряток: main.avatar, main.card, main.banner, main.history_title, main.history_button.
 * Запретные зоны: main.balance, main.action_qr, main.action_transfer, main.action_topup, main.history_1..3.
 */
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .drawWithCache {
                // Кисти создаются один раз на размер, а не на каждый кадр скролла
                val w = size.width
                val glows = listOf(
                    Triple(Offset(w * 0.5f, 330.dp.toPx()), 300.dp.toPx(), Color(0xFFEAFBAD)),
                    Triple(Offset(0f, 250.dp.toPx()), 210.dp.toPx(), Color(0xFFD3F9BC)),
                    Triple(Offset(w, 340.dp.toPx()), 190.dp.toPx(), Color(0xFFD2F7C2)),
                ).map { (center, radius, color) ->
                    Triple(center, radius, Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center, radius))
                }
                onDrawBehind {
                    drawRect(KidsColors.Page)
                    glows.forEach { (center, radius, brush) -> drawCircle(brush, radius, center) }
                }
            }
            .statusBarsPadding(),
    ) {
        KidsAvatar("main.avatar", Modifier.padding(start = 24.dp, top = 16.dp))
        Spacer(Modifier.height(25.dp))
        CardWithBalance()
        Spacer(Modifier.height(16.dp))
        PageDots(Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(38.dp))
        QuickActions()
        Spacer(Modifier.height(42.dp))
        Image(
            painter = painterResource(R.drawable.banner_strengths),
            contentDescription = "Узнай свои сильные стороны",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .tappable(RoundedCornerShape(17.dp))
                .hideSpot("main.banner"),
        )
        Spacer(Modifier.height(18.dp))
        SectionTitle("История", Modifier.padding(start = 16.dp).hideSpot("main.history_title"))
        Spacer(Modifier.height(16.dp))
        HistoryCard()
        Spacer(Modifier.height(24.dp))
        WideButton(
            text = "Посмотреть историю",
            modifier = Modifier.padding(horizontal = 16.dp).hideSpot("main.history_button"),
        )
        // место под плавающий таб-бар
        Spacer(Modifier.navigationBarsPadding().height(124.dp))
    }
}

@Composable
private fun CardWithBalance() {
    val hop = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    Box(Modifier.fillMaxWidth().height(216.dp)) {
        // Передняя карта по центру, задняя выглядывает справа — поэтому картинка смещена.
        // Тап по карте: карта подпрыгивает и пружинит обратно на место.
        Image(
            painter = painterResource(R.drawable.card_stack),
            contentDescription = "Карта",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 26.dp)
                .size(Art.CardStack)
                .graphicsLayer {
                    translationY = hop.value.dp.toPx()
                    rotationZ = hop.value * 0.18f
                }
                .tappable(pressedScale = 0.95f, ripple = false, haptic = false) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        hop.animateTo(-16f, tween(durationMillis = 120, easing = FastOutSlowInEasing))
                        hop.animateTo(0f, spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessLow))
                    }
                }
                .hideSpot("main.card"),
        )
        BalancePlate(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 145.dp)
                .fillMaxWidth()
                .height(71.dp),
        )
    }
}

@Composable
private fun BalancePlate(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .tappable(RoundedCornerShape(22.dp), pressedScale = 0.98f)
            .background(KidsColors.Card)
            .noHideZone("main.balance"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = buildAnnotatedString {
                append("7 932")
                withStyle(SpanStyle(color = KidsColors.Kopecks)) { append(",04 ₽") }
            },
            style = KidsType.Balance,
            modifier = Modifier.padding(start = 25.dp),
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .padding(end = 8.dp)
                .width(101.dp)
                .height(55.dp)
                .tappable(RoundedCornerShape(16.dp), pressedScale = 0.92f)
                .background(KidsColors.Spasibo),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("256", style = KidsType.Balance.copy(fontSize = 18.sp))
            Spacer(Modifier.width(6.dp))
            Image(painterResource(R.drawable.ic_spasibo), contentDescription = "Спасибо", modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun PageDots(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.width(28.dp).height(5.dp).clip(CircleShape).background(Color(0xFFBFC4CB)))
        Box(Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFD3D7DD)))
        Box(Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFD3D7DD)))
    }
}

@Composable
private fun QuickActions() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ActionTile("QR", R.drawable.ic_action_qr, width = 71.dp, centered = true, spotId = "main.action_qr") }
        item { ActionTile("Скинуть\nденег", R.drawable.ic_action_transfer, width = 142.dp, spotId = "main.action_transfer") }
        item { ActionTile("Пополнить\nмобилку", R.drawable.ic_action_topup, width = 142.dp, spotId = "main.action_topup") }
    }
}

/** Кнопки оплаты/переводов — Нычка их никогда не перекрывает (требование ТЗ). */
@Composable
private fun ActionTile(
    title: String,
    @DrawableRes icon: Int,
    width: Dp,
    spotId: String,
    centered: Boolean = false,
) {
    Box(
        Modifier
            .width(width)
            .height(120.dp)
            .tappable(RoundedCornerShape(18.dp), pressedScale = 0.94f)
            .background(KidsColors.Card)
            .noHideZone(spotId),
    ) {
        Text(
            text = title,
            style = KidsType.TileTitle,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = if (centered) {
                Modifier.align(Alignment.TopCenter).padding(top = 20.dp)
            } else {
                Modifier.align(Alignment.TopStart).padding(start = 23.dp, top = 20.dp)
            },
        )
        Box(
            modifier = Modifier
                .align(if (centered) Alignment.BottomCenter else Alignment.BottomEnd)
                .padding(12.dp)
                .size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.requiredSize(Art.ActionIcon))
        }
    }
}

private data class Operation(
    @param:DrawableRes val icon: Int,
    val title: String,
    val subtitle: String,
    val amount: String,
    val bonus: String? = null,
)

@Composable
private fun HistoryCard() {
    val operations = listOf(
        Operation(R.drawable.ic_hist_goods, "Товары для дома", "Маркетплейсы", "−2 292 ₽", bonus = "+28"),
        Operation(R.drawable.ic_hist_kuper, "Купер", "Супермаркет", "−4 686 ₽"),
        Operation(R.drawable.ic_hist_transfer, "Мария Валерьевна Р.", "Клиенту Сбера", "−500 ₽"),
    )
    // Отступы поделены между карточкой и строками, чтобы у каждой строки была своя зона нажатия
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KidsColors.Card)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        operations.forEachIndexed { i, op -> OperationRow(op, Modifier.noHideZone("main.history_${i + 1}")) }
    }
}

@Composable
private fun OperationRow(op: Operation, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .tappable(RoundedCornerShape(16.dp), pressedScale = 0.97f)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Image(painterResource(op.icon), contentDescription = null, modifier = Modifier.requiredSize(Art.HistoryIcon))
        }
        Spacer(Modifier.width(17.dp))
        Column(Modifier.weight(1f).padding(top = 4.dp)) {
            Text(op.title, style = KidsType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(op.subtitle, style = KidsType.Caption)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.padding(top = 4.dp), horizontalAlignment = Alignment.End) {
            Text(op.amount, style = KidsType.Body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp))
            if (op.bonus != null) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(op.bonus, style = KidsType.Caption.copy(color = KidsColors.Green, fontWeight = FontWeight.Medium))
                    Spacer(Modifier.width(3.dp))
                    Image(
                        painter = painterResource(R.drawable.ic_spasibo),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(KidsColors.GreenBadge),
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}
