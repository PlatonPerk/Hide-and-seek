package ru.hideandseek.nychka

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.hideandseek.ContentSource
import app.hideandseek.HideAndSeekController
import app.hideandseek.HideAndSeekHost
import com.github.takahirom.roborazzi.captureRoboImage
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ru.hideandseek.nychka.ui.components.Tab
import ru.hideandseek.nychka.ui.theme.NychkaTheme
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Лиса Нычка на экранах: выглядывает сверху, справа, слева и выходит после находки.
 * Контент — встроенный из assets, но с одним местом и без особых дней, время зафиксировано.
 * Запуск: ./gradlew :app:recordRoborazziDebug — картинки 2x_fox_*.png в app/build/outputs/roborazzi/.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
class FoxScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    /** 3 ноября 2026, полдень по Москве: обычный день. */
    private val noon = ZonedDateTime.of(2026, 11, 3, 12, 0, 0, 0, ZoneId.of("Europe/Moscow"))
        .toInstant().toEpochMilli()

    private fun contentWith(spotId: String, edge: String, align: Double): String {
        val app = RuntimeEnvironment.getApplication()
        val text = app.assets.open("hideandseek/content.json").bufferedReader().use { it.readText() }
        val json = JSONObject(text)
        val creatures = json.getJSONArray("creatures")
        val nychka = (0 until creatures.length()).map { creatures.getJSONObject(it) }.first { it.getString("id") == "nychka" }
        json.put("creatures", JSONArray().put(nychka))
        json.put("spots", JSONArray().put(JSONObject().put("id", spotId).put("edge", edge).put("align", align)))
        json.put("specialDays", JSONArray())
        json.getJSONObject("look").put("appearDelayMs", 0)
        return json.toString()
    }

    /** Время идёт вручную: загрузка контента и поз идёт в фоне, поэтому даём ей и реальное время. */
    private fun advance(ms: Long) {
        var left = ms
        while (left > 0) {
            val step = minOf(left, 50L)
            compose.mainClock.advanceTimeBy(step)
            Thread.sleep(10)
            compose.waitForIdle()
            left -= step
        }
    }

    private fun show(tab: Tab, spotId: String, edge: String, align: Double) {
        val json = contentWith(spotId, edge, align)
        val controller = HideAndSeekController(
            RuntimeEnvironment.getApplication(),
            ContentSource { json },
            bundledAsset = null,
            clock = { noon },
        )
        compose.mainClock.autoAdvance = false
        compose.setContent {
            NychkaTheme {
                HideAndSeekHost(controller, Modifier.fillMaxSize()) { AppRoot(initialTab = tab) }
            }
        }
        advance(2_500) // контент и позы загрузились, лиса выглянула
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")

    @Test
    fun top() {
        show(Tab.Main, "main.banner", "top", 0.85)
        capture("21_fox_top")
    }

    @Test
    fun right() {
        show(Tab.Main, "main.history_title", "right", 0.5)
        capture("22_fox_right")
    }

    @Test
    fun left() {
        show(Tab.Piggy, "piggy.pig", "left", 0.6)
        capture("23_fox_left")
    }

    @Test
    fun foundCelebrateRun() {
        show(Tab.Main, "main.banner", "top", 0.85)
        compose.onNodeWithContentDescription("Здесь кто-то прячется", substring = true).performClick()
        advance(200)
        capture("24_fox_found")
        advance(500) // 0,7 с: первый прыжок
        capture("25_fox_celebrate")
        advance(1_150) // 1,85 с: убегает
        capture("26_fox_run")
    }
}
