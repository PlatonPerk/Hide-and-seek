package ru.hideandseek.nychka

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ru.hideandseek.nychka.ui.components.Tab
import ru.hideandseek.nychka.ui.theme.NychkaTheme

/**
 * Рендер экранов на JVM (Robolectric + Roborazzi), чтобы сверять вёрстку с макетом без телефона.
 * Запуск: ./gradlew :app:recordRoborazziDebug — картинки появятся в app/build/outputs/roborazzi/.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
class ScreensScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private fun shoot(name: String, tab: Tab, debug: Boolean = false) {
        compose.setContent {
            NychkaTheme { AppRoot(initialTab = tab, initialDebug = debug) }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun main() = shoot("01_main", Tab.Main)

    @Test
    fun tasks() = shoot("02_tasks", Tab.Tasks)

    @Test
    fun content() = shoot("03_content", Tab.Content)

    @Test
    fun piggy() = shoot("06_piggy", Tab.Piggy)

    @Test
    fun mainSpots() = shoot("04_main_spots", Tab.Main, debug = true)

    @Test
    fun tasksSpots() = shoot("05_tasks_spots", Tab.Tasks, debug = true)

    @Test
    @Config(qualifiers = "w360dp-h1250dp-xhdpi")
    fun mainFull() = shoot("11_main_full", Tab.Main)

    @Test
    @Config(qualifiers = "w360dp-h1100dp-xhdpi")
    fun tasksFull() = shoot("12_tasks_full", Tab.Tasks)

    @Test
    @Config(qualifiers = "w360dp-h3400dp-xhdpi")
    fun contentFull() = shoot("13_content_full", Tab.Content)

    @Test
    @Config(qualifiers = "w360dp-h2600dp-xhdpi")
    fun piggyFull() = shoot("14_piggy_full", Tab.Piggy)
}
