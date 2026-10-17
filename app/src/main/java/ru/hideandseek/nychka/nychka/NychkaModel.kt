package ru.hideandseek.nychka.nychka

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.time.LocalDate

/**
 * Место из контент-конфига.
 * @param weight вес при выборе места дня
 * @param align где по горизонтали выглядывает Нычка: 0 — левый край элемента, 1 — правый
 * @param edge где «край укрытия» по вертикали: 0 — верх элемента, 0.3 — на трети высоты
 */
data class SpotConfig(val id: String, val weight: Int, val align: Float = 0.7f, val edge: Float = 0f)

data class NychkaTexts(
    val found: List<String> = listOf("Ты меня нашёл!"),
    val streak: String = "Серия: {streak} {days} подряд",
    val newSeries: String = "Начинаем новую серию!",
    val best: String = "Рекорд: {best} {bestDays}",
    val total: String = "Всего найдено: {total}",
    val share: String = "Я нашёл Нычку в СберKids! Серия: {streak} {days}. А ты найдёшь?",
)

/**
 * Контент-конфиг Нычки. В приложении — JSON по ключу CMS (новый ключ — это настройка контента,
 * не бэкенд). В прототипе лежит в assets/nychka_config.json.
 * Пустой или битый конфиг не ломает приложение: тогда Нычка просто не появляется.
 */
data class NychkaConfig(
    val salt: String = "",
    val spots: List<SpotConfig> = emptyList(),
    val texts: NychkaTexts = NychkaTexts(),
) {
    companion object {
        fun parse(json: String?): NychkaConfig = try {
            val root = JSONObject(json ?: "")
            val spotsJson = root.optJSONArray("spots")
            val spots = buildList {
                if (spotsJson != null) for (i in 0 until spotsJson.length()) {
                    val o = spotsJson.optJSONObject(i) ?: continue
                    val id = o.optString("id").takeIf { it.isNotBlank() } ?: continue
                    add(
                        SpotConfig(
                            id = id,
                            weight = o.optInt("weight", 1).coerceAtLeast(0),
                            align = o.optDouble("align", 0.7).toFloat().coerceIn(0f, 1f),
                            edge = o.optDouble("edge", 0.0).toFloat().coerceIn(0f, 1f),
                        ),
                    )
                }
            }
            val t = root.optJSONObject("texts")
            val defaults = NychkaTexts()
            val found = t?.optJSONArray("found")?.let { arr ->
                (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
            }.orEmpty()
            NychkaConfig(
                salt = root.optString("salt"),
                spots = spots,
                texts = NychkaTexts(
                    found = found.ifEmpty { defaults.found },
                    streak = t?.optString("streak")?.ifBlank { null } ?: defaults.streak,
                    newSeries = t?.optString("newSeries")?.ifBlank { null } ?: defaults.newSeries,
                    best = t?.optString("best")?.ifBlank { null } ?: defaults.best,
                    total = t?.optString("total")?.ifBlank { null } ?: defaults.total,
                    share = t?.optString("share")?.ifBlank { null } ?: defaults.share,
                ),
            )
        } catch (e: Exception) {
            NychkaConfig()
        }
    }
}

/**
 * Место дня. Сервер не нужен: место вычисляется из даты и «соли» из конфига,
 * поэтому у всех детей с одинаковым конфигом в один день Нычка прячется в одном и том же месте.
 */
object DailySpot {
    fun pick(config: NychkaConfig, date: LocalDate): SpotConfig? {
        val spots = config.spots.filter { it.weight > 0 }
        if (spots.isEmpty()) return null
        val total = spots.sumOf { it.weight }.toLong()
        var r = java.lang.Long.remainderUnsigned(fnv1a64("${config.salt}:$date"), total)
        for (spot in spots) {
            if (r < spot.weight) return spot
            r -= spot.weight
        }
        return spots.last()
    }

    /** FNV-1a: одинаковый результат на Android и iOS, в отличие от платформенных Random/hashCode. */
    internal fun fnv1a64(s: String): Long {
        var h = -0x340d631b7bdddcdbL // 0xcbf29ce484222325
        for (b in s.toByteArray(Charsets.UTF_8)) {
            h = h xor (b.toLong() and 0xff)
            h *= 0x100000001b3L
        }
        return h
    }
}

/**
 * Прогресс ребёнка — только на устройстве. «Мягкая» серия: пропуск одного дня серию не рвёт,
 * при большем перерыве начинается новая, а «всего найдено» и рекорд сохраняются.
 * Никаких таймеров и «серия сгорит» — это тёмные паттерны, запрещённые ТЗ.
 */
data class NychkaProgress(
    val lastFoundDay: Long? = null,
    val streak: Int = 0,
    val best: Int = 0,
    val total: Int = 0,
) {
    fun foundOn(day: Long): FoundResult {
        if (lastFoundDay == day) return FoundResult(this, newSeries = false)
        val continues = lastFoundDay != null && day - lastFoundDay in 1L..2L
        val streakNow = if (continues) streak + 1 else 1
        return FoundResult(
            progress = NychkaProgress(day, streakNow, maxOf(best, streakNow), total + 1),
            newSeries = !continues && streak > 0,
        )
    }
}

data class FoundResult(val progress: NychkaProgress, val newSeries: Boolean)

class NychkaStore(private val prefs: SharedPreferences?) {
    fun load(): NychkaProgress {
        val p = prefs ?: return NychkaProgress()
        return NychkaProgress(
            lastFoundDay = if (p.contains(KEY_DAY)) p.getLong(KEY_DAY, 0) else null,
            streak = p.getInt(KEY_STREAK, 0),
            best = p.getInt(KEY_BEST, 0),
            total = p.getInt(KEY_TOTAL, 0),
        )
    }

    fun save(progress: NychkaProgress) {
        prefs?.edit()?.apply {
            progress.lastFoundDay?.let { putLong(KEY_DAY, it) }
            putInt(KEY_STREAK, progress.streak)
            putInt(KEY_BEST, progress.best)
            putInt(KEY_TOTAL, progress.total)
            apply()
        }
    }

    private companion object {
        const val KEY_DAY = "last_found_day"
        const val KEY_STREAK = "streak"
        const val KEY_BEST = "best"
        const val KEY_TOTAL = "total"
    }
}

enum class NychkaPhase {
    /** Сидит в укрытии и выглядывает (если место видно на экране). */
    Peeking,

    /** Тапнули: «ой, нашли!». */
    Caught,

    /** Карточка с наградой и серией. */
    Celebrate,

    /** Убегает до завтра. */
    Running,

    /** Сегодня уже найдена. */
    Done,
}

@Stable
class NychkaController(
    val config: NychkaConfig,
    private val store: NychkaStore,
    /** false — без бесконечных анимаций и авто-таймеров (для скриншот-тестов). */
    val liveAnimations: Boolean = true,
    private val forcedSpotId: String? = null,
    initialPhase: NychkaPhase? = null,
    private val clock: () -> LocalDate = { LocalDate.now() },
) {
    /** Сдвиг даты для демо на защите (в отладочном режиме: тап по лапке = «следующий день»). */
    var dayOffset by mutableIntStateOf(0)
        private set
    var spot by mutableStateOf<SpotConfig?>(null)
        private set
    var phase by mutableStateOf(NychkaPhase.Peeking)
        private set
    var progress by mutableStateOf(store.load())
        private set
    var lastResult by mutableStateOf<FoundResult?>(null)
        private set

    val today: LocalDate get() = clock().plusDays(dayOffset.toLong())

    init {
        refresh()
        if (initialPhase != null) {
            phase = initialPhase
            if (initialPhase == NychkaPhase.Celebrate) lastResult = progress.foundOn(today.toEpochDay())
        }
    }

    private fun refresh() {
        spot = forcedSpotId?.let { id -> config.spots.firstOrNull { it.id == id } ?: SpotConfig(id, 1) }
            ?: DailySpot.pick(config, today)
        phase = if (progress.lastFoundDay == today.toEpochDay()) NychkaPhase.Done else NychkaPhase.Peeking
    }

    fun nextDay() {
        dayOffset++
        refresh()
    }

    fun onFound() {
        if (phase != NychkaPhase.Peeking) return
        val result = progress.foundOn(today.toEpochDay())
        progress = result.progress
        lastResult = result
        store.save(result.progress)
        phase = NychkaPhase.Caught
    }

    fun onCaughtShown() {
        if (phase == NychkaPhase.Caught) phase = NychkaPhase.Celebrate
    }

    fun onCelebrateClosed() {
        if (phase == NychkaPhase.Celebrate) phase = NychkaPhase.Running
    }

    fun onRunFinished() {
        if (phase == NychkaPhase.Running) phase = NychkaPhase.Done
    }

    /** Фраза дня — тоже детерминированная, без случайности. */
    val foundPhrase: String
        get() = config.texts.found[Math.floorMod(today.toEpochDay(), config.texts.found.size.toLong()).toInt()]

    fun format(template: String): String {
        val p = progress
        return template
            .replace("{streak}", p.streak.toString())
            .replace("{days}", daysWord(p.streak))
            .replace("{best}", p.best.toString())
            .replace("{bestDays}", daysWord(p.best))
            .replace("{total}", p.total.toString())
    }

    companion object {
        fun create(
            context: Context,
            liveAnimations: Boolean = true,
            forcedSpotId: String? = null,
            initialPhase: NychkaPhase? = null,
        ): NychkaController {
            val json = try {
                context.assets.open("nychka_config.json").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                null
            }
            val prefs = context.getSharedPreferences("nychka", Context.MODE_PRIVATE)
            return NychkaController(NychkaConfig.parse(json), NychkaStore(prefs), liveAnimations, forcedSpotId, initialPhase)
        }
    }
}

/** 1 день, 2 дня, 5 дней, 21 день… */
fun daysWord(n: Int): String {
    val m100 = n % 100
    val m10 = n % 10
    return when {
        m100 in 11..14 -> "дней"
        m10 == 1 -> "день"
        m10 in 2..4 -> "дня"
        else -> "дней"
    }
}
