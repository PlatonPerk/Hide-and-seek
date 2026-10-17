package app.hideandseek.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.hideandseek.HideAndSeekHost
import app.hideandseek.rememberHideAndSeekController

/** Change these two lines for your setup. */
object DemoConfig {
    /**
     * The content JSON on GitHub Pages (or any https host), e.g.
     * "https://<user>.github.io/<repo>/hideandseek.json". Null = only the copy bundled in assets.
     */
    val CONTENT_URL: String? = null

    /**
     * The website that imitates the app. Default: the sample page in assets/site.
     * For a local dev server use http://<your-PC-IP>:<port>/ (the phone and PC on the same Wi-Fi).
     */
    const val WEBSITE_URL: String = "file:///android_asset/site/index.html"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 1) One controller for the app…
            val hideAndSeek = rememberHideAndSeekController(contentUrl = DemoConfig.CONTENT_URL)
            DemoTheme {
                // 2) …and one host around the whole UI. Screens only add Modifier.hidingSpot("…").
                HideAndSeekHost(hideAndSeek, Modifier.fillMaxSize().background(AppBackground)) {
                    DemoApp(hideAndSeek)
                }
            }
        }
    }
}

val Primary = Color(0xFF3B5BFD)
val Accent = Color(0xFF14B87F)
val Ink = Color(0xFF1F2430)
val InkSoft = Color(0xFF6B7385)
val AppBackground = Color(0xFFF3F5FB)

@Composable
fun DemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Primary,
            secondary = Accent,
            background = AppBackground,
            surface = Color.White,
        ),
        content = content,
    )
}
