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
import androidx.compose.ui.platform.LocalContext
import ru.hideandseek.nychka.hidespots.HideSpotDebugOverlay
import ru.hideandseek.nychka.hidespots.HideSpotRegistry
import ru.hideandseek.nychka.hidespots.LocalHideSpotRegistry
import ru.hideandseek.nychka.nychka.NychkaController
import ru.hideandseek.nychka.nychka.NychkaLayer
import ru.hideandseek.nychka.ui.components.KidsTabBar
import ru.hideandseek.nychka.ui.components.Tab
import ru.hideandseek.nychka.ui.screens.ContentScreen
import ru.hideandseek.nychka.ui.screens.MainScreen
import ru.hideandseek.nychka.ui.screens.PiggyScreen
import ru.hideandseek.nychka.ui.screens.TasksScreen
import ru.hideandseek.nychka.ui.theme.KidsColors

/**
 * Корень прототипа: имитация вкладок СберKids, реестр мест для пряток и слой Нычки поверх.
 *
 * Отладка для демо: долгое нажатие на лапку — рамки мест и плашка с местом дня;
 * в этом режиме обычный тап по лапке переключает на следующий день.
 */
@Composable
fun AppRoot(
    initialTab: Tab = Tab.Main,
    initialDebug: Boolean = false,
    nychka: NychkaController? = null,
) {
    val context = LocalContext.current
    val controller = nychka ?: remember { NychkaController.create(context) }
    val registry = remember { HideSpotRegistry() }
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var debug by rememberSaveable { mutableStateOf(initialDebug) }

    CompositionLocalProvider(LocalHideSpotRegistry provides registry) {
        Box(Modifier.fillMaxSize().background(KidsColors.Page)) {
            when (tab) {
                Tab.Main -> MainScreen()
                Tab.Tasks -> TasksScreen()
                Tab.Content -> ContentScreen()
                Tab.Piggy -> PiggyScreen()
            }
            KidsTabBar(
                selected = tab,
                onSelect = { tab = it },
                onPawLongPress = { debug = !debug },
                onPawClick = { if (debug) controller.nextDay() },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            NychkaLayer(controller, registry, debug)
            if (debug) HideSpotDebugOverlay(registry)
        }
    }
}
