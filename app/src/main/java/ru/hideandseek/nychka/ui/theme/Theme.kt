package ru.hideandseek.nychka.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Палитра снята с макетов СберKids в Figma (раздел «Главный»). */
object KidsColors {
    val Page = Color(0xFFF3F4F6)
    val Card = Color.White
    val TextPrimary = Color(0xFF0B0B0C)
    val TextSecondary = Color(0xFF8A8F98)
    val TextTertiary = Color(0xFFB0B5BC)
    val TextSubtle = Color(0xFFA8ABB1)
    val Kopecks = Color(0xFFA3A9B0)
    val Green = Color(0xFF45C255)
    val GreenBadge = Color(0xFF58D647)
    val TabInactive = Color(0xFFA9B4C2)
    val Purple = Color(0xFFA887FC)
    val Track = Color(0xFFE9E9EE)
    val GreyButton = Color(0xFFE6EBEF)
    val ItemGrey = Color(0xFFF4F5F7)
    val Spasibo = Color(0xFFEFFDC4)
    val ArrowBlue = Color(0xFF66B3F5)
    val Numeral = Color(0xFFB2A3FE)
    val Algoritmika = Color(0xFFE2EAFE)
    val PlaceholderBlue = Color(0xFFD6E8FF)
    val PlaceholderIcon = Color(0xFF5AA9F0)
    val FlamePurple = Color(0xFFA48BF2)
    val FlameOrange = Color(0xFFF39A3D)
}

/**
 * Типографика. В приложении используется фирменный SB Sans — в прототипе его нет,
 * поэтому системный шрифт с теми же размерами и начертаниями.
 */
object KidsType {
    val Display = TextStyle(
        fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.2).sp, color = KidsColors.TextPrimary,
    )
    val Balance = TextStyle(
        fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.3).sp, color = KidsColors.TextPrimary,
    )
    val TileTitle = TextStyle(
        fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, color = KidsColors.TextPrimary,
    )
    val CardTitle = TextStyle(
        fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold, color = KidsColors.TextPrimary,
    )
    val Body = TextStyle(
        fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium, color = KidsColors.TextPrimary,
    )
    val Caption = TextStyle(
        fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, color = KidsColors.TextSecondary,
    )
    val Button = TextStyle(
        fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = KidsColors.TextPrimary,
    )
    val Tab = TextStyle(
        fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium,
    )
}

@Composable
fun NychkaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = KidsColors.Green,
            background = KidsColors.Page,
            surface = KidsColors.Card,
        ),
        content = content,
    )
}
