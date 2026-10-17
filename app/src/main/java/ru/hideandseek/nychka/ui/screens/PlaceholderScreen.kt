package ru.hideandseek.nychka.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.hideandseek.nychka.ui.components.KidsAvatar
import ru.hideandseek.nychka.ui.theme.KidsType

/** Вкладки, которые не входят в прототип. */
@Composable
fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().statusBarsPadding()) {
        KidsAvatar("placeholder.avatar", Modifier.padding(start = 24.dp, top = 16.dp))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = KidsType.Display)
            Spacer(Modifier.height(8.dp))
            Text("Этот экран не входит в прототип", style = KidsType.Caption)
        }
    }
}
