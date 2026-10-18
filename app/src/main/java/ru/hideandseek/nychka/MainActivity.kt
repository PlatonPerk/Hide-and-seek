package ru.hideandseek.nychka

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.hideandseek.HideAndSeekHost
import app.hideandseek.TourStop
import app.hideandseek.core.Edge
import app.hideandseek.rememberHideAndSeekController
import ru.hideandseek.nychka.ui.theme.NychkaTheme

/**
 * Откуда берётся контент «Пряток» (места, веса, расписание, тексты).
 * В настоящем СберKids это ключ CMS; в прототипе — файл в этом репозитории на GitHub.
 * Правка файла на GitHub меняет поведение без пересборки (кнопка «Обновить контент»
 * в панели демо или перезапуск). Без сети берётся кеш или копия из assets.
 */
object NychkaConfig {
    const val CONTENT_URL =
        "https://raw.githubusercontent.com/ka12fy/SberHack/main/content/hideandseek.json"

    /**
     * Режим демо для защиты: после находки Нычка сразу прячется в другом месте
     * (а не ждёт завтра). Переключается и в панели демо. В настоящем приложении — false.
     */
    const val DEMO_REHIDE = true

    /**
     * Маршрут Нычки в режиме демо: после каждой находки — следующее место (по кругу).
     * Сверху и сбоку по очереди, на каждой вкладке. Край и положение по умолчанию — из контента;
     * у карты на «Главном» край заменён на левый, чтобы боковую позу было видно на первом же экране.
     */
    val DEMO_TOUR = listOf(
        TourStop("main.banner"),                      // «Главный», сверху
        TourStop("main.card", Edge.LEFT, 0.5f),       // «Главный», сбоку
        TourStop("piggy.pig"),                        // «Копилки», сбоку
        TourStop("piggy.goal"),                       // «Копилки», сверху
        TourStop("tasks.start_card"),                 // «Дела», сверху
        TourStop("content.hero"),                     // «Контент», сверху
        TourStop("content.algoritmika_mascot"),       // «Контент», сбоку (прокрутить вниз)
    )

    /** Серия считается находками (каждая находка +1), а не днями подряд. В настоящем приложении — false. */
    const val STREAK_BY_FINDS = true
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            NychkaTheme {
                // Один контроллер на всё приложение и один слой поверх всех экранов:
                // это и есть встраивание модуля «Прятки».
                val hideAndSeek = rememberHideAndSeekController(contentUrl = NychkaConfig.CONTENT_URL) {
                    demoRehide = NychkaConfig.DEMO_REHIDE
                    demoTour = NychkaConfig.DEMO_TOUR
                    streakByFinds = NychkaConfig.STREAK_BY_FINDS
                }
                HideAndSeekHost(hideAndSeek, Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}
