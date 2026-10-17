package ru.hideandseek.nychka

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.hideandseek.nychka.hidespots.HideSpotDebugOverlay
import ru.hideandseek.nychka.hidespots.HideSpotRegistry
import ru.hideandseek.nychka.hidespots.LocalHideSpotRegistry
import ru.hideandseek.nychka.ui.components.KidsTabBar
import ru.hideandseek.nychka.ui.components.Tab
import ru.hideandseek.nychka.ui.screens.ContentScreen
import ru.hideandseek.nychka.ui.screens.MainScreen
import ru.hideandseek.nychka.ui.screens.PlaceholderScreen
import ru.hideandseek.nychka.ui.screens.TasksScreen
import ru.hideandseek.nychka.ui.theme.KidsColors

/**
 * Корень прототипа: имитация вкладок СберKids + реестр мест для пряток.
 * Слой с Нычкой будет жить здесь же, поверх экранов (следующий шаг).
 */
@Composable
fun AppRoot(initialTab: Tab = Tab.Main, initialDebug: Boolean = false) {
    val registry = remember { HideSpotRegistry() }
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var debug by rememberSaveable { mutableStateOf(initialDebug) }

    CompositionLocalProvider(LocalHideSpotRegistry provides registry) {
        Box(Modifier.fillMaxSize().background(KidsColors.Page)) {
            when (tab) {
                Tab.Main -> MainScreen()
                Tab.Tasks -> TasksScreen()
                Tab.Content -> ContentScreen()
                Tab.Piggy -> PlaceholderScreen(title = "Копилки")
            }
            KidsTabBar(
                selected = tab,
                onSelect = { tab = it },
                onPawLongPress = { debug = !debug },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            if (debug) HideSpotDebugOverlay(registry)
        }
    }
}
