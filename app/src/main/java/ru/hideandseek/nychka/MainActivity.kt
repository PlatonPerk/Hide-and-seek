package ru.hideandseek.nychka

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import app.hideandseek.HideAndSeekHost
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
                val hideAndSeek = rememberHideAndSeekController(contentUrl = NychkaConfig.CONTENT_URL)
                LaunchedEffect(hideAndSeek) { hideAndSeek.demoRehide = NychkaConfig.DEMO_REHIDE }
                HideAndSeekHost(hideAndSeek, Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}
