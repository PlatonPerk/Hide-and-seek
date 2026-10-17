package ru.hideandseek.nychka

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.hideandseek.ContentSource
import app.hideandseek.HideAndSeekController
import app.hideandseek.HideAndSeekHost
import com.github.takahirom.roborazzi.captureRoboImage
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private class Spot(val id: String, val edge: String = "top", val align: Double = 0.5)

    private fun bundledContent(): JSONObject {
        val app = RuntimeEnvironment.getApplication()
        return JSONObject(app.assets.open("hideandseek/content.json").bufferedReader().use { it.readText() })
    }

    /** Встроенный контент, но только Нычка, только эти места и (если задано) место дня. */
    private fun contentWith(spots: List<Spot>, spotOfTheDay: String? = null): String {
        val json = bundledContent()
        val creatures = json.getJSONArray("creatures")
        val nychka = (0 until creatures.length()).map { creatures.getJSONObject(it) }.first { it.getString("id") == "nychka" }
        json.put("creatures", JSONArray().put(nychka))
        json.put(
            "spots",
            JSONArray().apply {
                for (spot in spots) put(JSONObject().put("id", spot.id).put("edge", spot.edge).put("align", spot.align))
            },
        )
        json.put(
            "specialDays",
            JSONArray().apply {
                if (spotOfTheDay != null) put(JSONObject().put("date", "2026-11-03").put("spot", spotOfTheDay))
            },
        )
        json.getJSONObject("look").put("appearDelayMs", 0)
        return json.toString()
    }

    private fun contentWith(spotId: String, edge: String, align: Double): String =
        contentWith(listOf(Spot(spotId, edge, align)))

    private fun controllerFor(json: String) = HideAndSeekController(
        RuntimeEnvironment.getApplication(),
        ContentSource { json },
        bundledAsset = null,
        clock = { noon },
    )

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

    private fun show(tab: Tab, spotId: String, edge: String, align: Double): HideAndSeekController =
        show(tab, contentWith(spotId, edge, align))

    private fun show(tab: Tab, json: String): HideAndSeekController {
        val controller = controllerFor(json)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            NychkaTheme {
                HideAndSeekHost(controller, Modifier.fillMaxSize()) { AppRoot(initialTab = tab) }
            }
        }
        advance(2_500) // контент и позы загрузились, лиса выглянула
        return controller
    }

    private fun tabOf(spotId: String): Tab = when (spotId.substringBefore('.')) {
        "piggy" -> Tab.Piggy
        "tasks" -> Tab.Tasks
        "content" -> Tab.Content
        else -> Tab.Main
    }

    private fun foxShown(): Boolean = compose
        .onAllNodesWithContentDescription("Здесь кто-то прячется", substring = true)
        .fetchSemanticsNodes().isNotEmpty()

    private fun capture(name: String) = compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")

    @Test
    fun top() {
        show(Tab.Main, "main.banner", "top", 0.85)
        capture("21_fox_top")
    }

    @Test
    @Config(qualifiers = "w360dp-h3400dp-xhdpi")
    fun leftMascot() {
        show(Tab.Content, "content.algoritmika_mascot", "left", 0.5)
        capture("22_fox_left_mascot")
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
        advance(900) // ~1,6 с плюс время на снимки: убегает (весь бег — 0,6 с)
        capture("26_fox_run")
    }

    private fun find() {
        compose.onNodeWithContentDescription("Здесь кто-то прячется", substring = true).performClick()
    }

    @Test
    fun cardIgnoresTapOnItAndClosesOnTapOutside() {
        show(Tab.Main, "main.banner", "top", 0.85)
        find()
        advance(500)
        compose.onNodeWithText("Нашёл!").performClick()
        advance(400)
        compose.onNodeWithText("Нашёл!").assertExists()
        compose.onNodeWithText("История").performClick()
        advance(400)
        compose.onNodeWithText("Нашёл!").assertDoesNotExist()
    }

    @Test
    fun cardStaysLong() {
        show(Tab.Main, "main.banner", "top", 0.85)
        find()
        advance(10_000)
        compose.onNodeWithText("Нашёл!").assertExists()
    }

    @Test
    fun celebrationScrollsWithScreen() {
        show(Tab.Main, "main.banner", "top", 0.85)
        find()
        advance(500)
        compose.onRoot().performTouchInput {
            swipe(Offset(centerX, height * 0.62f), Offset(centerX, height * 0.42f), durationMillis = 300)
        }
        advance(150)
        capture("27_fox_scrolled")
    }

    /** Каждое место из контента на своей вкладке: лиса там правда появляется (экран высокий, без прокрутки). */
    @Test
    @Config(qualifiers = "w360dp-h3400dp-xhdpi")
    fun everySpotInContentShowsTheFox() {
        val list = bundledContent().getJSONArray("spots")
        val spots = (0 until list.length()).map { list.getJSONObject(it) }
            .map { Spot(it.getString("id"), it.optString("edge", "top"), it.optDouble("align", 0.5)) }
        val controllers = spots.map { controllerFor(contentWith(listOf(it))) }
        var index by mutableIntStateOf(-1)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            NychkaTheme {
                val i = index
                if (i >= 0) {
                    key(i) {
                        HideAndSeekHost(controllers[i], Modifier.fillMaxSize()) { AppRoot(initialTab = tabOf(spots[i].id)) }
                    }
                }
            }
        }
        val missing = mutableListOf<String>()
        for (i in spots.indices) {
            index = i
            advance(2_500)
            if (!foxShown()) missing += spots[i].id
        }
        assertTrue("Лиса не появилась: $missing", missing.isEmpty())
    }

    /** Перепрятка: после находки на «Главном» лиса прячется на другой вкладке. */
    @Test
    fun rehideMovesToAnotherScreen() {
        val json = contentWith(
            listOf(Spot("main.banner", align = 0.85), Spot("main.card", align = 0.75), Spot("piggy.goal", align = 0.85)),
            spotOfTheDay = "main.banner",
        )
        val controller = show(Tab.Main, json)
        compose.runOnIdle { controller.demoRehide = true }
        assertEquals("main.banner", controller.plan?.spot?.id)
        find()
        advance(500)
        compose.onNodeWithText("История").performClick() // закрыть карточку касанием мимо
        advance(3_000)
        assertEquals("piggy.goal", controller.plan?.spot?.id)
    }

    @Test
    fun celebrationDisappearsOnTabSwitch() {
        show(Tab.Main, "main.banner", "top", 0.85)
        find()
        advance(500)
        compose.onNodeWithText("Копилки").performClick()
        advance(400)
        compose.onNodeWithText("Нашёл!").assertDoesNotExist()
        capture("28_fox_tab_switch")
    }
}
