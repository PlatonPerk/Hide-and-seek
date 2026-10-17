package app.hideandseek.demo

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hideandseek.HideAndSeekBlocker
import app.hideandseek.HideAndSeekController
import app.hideandseek.hidingSpot
import app.hideandseek.web.HidingSpotsWebView

enum class Tab(val title: String, val emoji: String) {
    Home("Главная", "🏠"),
    Lessons("Уроки", "📚"),
    Website("Сайт", "🌐"),
}

/** A tiny mock of a kids' banking app. Spot ids match content/hideandseek.json. */
@Composable
fun DemoApp(hideAndSeek: HideAndSeekController) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var transferOpen by rememberSaveable { mutableStateOf(false) }
    var devOpen by rememberSaveable { mutableStateOf(false) }

    if (transferOpen) {
        TransferScreen(onBack = { transferOpen = false })
        return
    }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            NavigationBar(containerColor = Color.White, modifier = Modifier.hidingSpot("nav.bar")) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Text(item.emoji, fontSize = 20.sp) },
                        label = { Text(item.title) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when (tab) {
                Tab.Home -> HomeScreen(
                    hideAndSeek = hideAndSeek,
                    onTransfer = { transferOpen = true },
                    onLessons = { tab = Tab.Lessons },
                    onDev = { devOpen = !devOpen },
                )
                Tab.Lessons -> LessonsScreen()
                Tab.Website -> WebsiteScreen(onExit = { tab = Tab.Home })
            }
            if (devOpen) {
                DevPanel(hideAndSeek, onClose = { devOpen = false }, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

// ---------------------------------------------------------------- Home

@Composable
fun HomeScreen(
    hideAndSeek: HideAndSeekController,
    onTransfer: () -> Unit,
    onLessons: () -> Unit,
    onDev: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Header(hideAndSeek, onDev)
        Spacer(Modifier.height(30.dp)) // gaps leave room for the creature to peek out
        BalanceCard(Modifier.hidingSpot("home.balance"))
        Spacer(Modifier.height(30.dp))
        QuickActions(onTransfer, Modifier.hidingSpot("home.actions"))
        Spacer(Modifier.height(30.dp))
        GoalCard(Modifier.hidingSpot("home.goal"))
        Spacer(Modifier.height(16.dp))
        TasksCard(Modifier.hidingSpot("home.tasks"))
        Spacer(Modifier.height(36.dp))
        LessonBanner(onLessons)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Header(hideAndSeek: HideAndSeekController, onDev: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFFFB86B), Color(0xFFFF7A59)))),
            contentAlignment = Alignment.Center,
        ) {
            Text("С", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Привет, Саша!", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
            Text("Кто-то спрятался в приложении 👀", fontSize = 13.sp, color = InkSoft)
        }
        // The collection: how many creatures the kid has found.
        Box(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text("🐾 ${hideAndSeek.progress.totalFound}", fontSize = 14.sp, color = Ink, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onDev),
            contentAlignment = Alignment.Center,
        ) {
            Text("⚙", fontSize = 18.sp, color = Ink)
        }
    }
}

@Composable
private fun BalanceCard(modifier: Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Primary, Color(0xFF7C4DFF))))
            .padding(20.dp),
    ) {
        Text("Мой баланс", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text("1 250 ₽", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Карта •• 4417", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Text("Кешбэк 37 ₽", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuickActions(onTransfer: () -> Unit, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ActionButton("💸", "Перевести", onTransfer)
        ActionButton("➕", "Пополнить", onTransfer)
        ActionButton("🐷", "Копилка") {}
        ActionButton("🎁", "Бонусы") {}
    }
}

@Composable
private fun ActionButton(emoji: String, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 24.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, color = Ink)
    }
}

@Composable
private fun WhiteCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun GoalCard(modifier: Modifier) {
    WhiteCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🛴", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Копилка на самокат", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink)
                Text("3 400 из 8 000 ₽", fontSize = 13.sp, color = InkSoft)
            }
            Text("42%", fontWeight = FontWeight.Bold, color = Primary)
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE8ECF8)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.42f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Accent),
            )
        }
    }
}

@Composable
private fun TasksCard(modifier: Modifier) {
    WhiteCard(modifier) {
        Text("Задания от родителей", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink)
        Spacer(Modifier.height(6.dp))
        TaskRow("Убрать в комнате", "+100 ₽", done = true)
        TaskRow("Прочитать 20 страниц", "+50 ₽", done = false)
        TaskRow("Помочь с покупками", "+70 ₽", done = false)
    }
}

@Composable
private fun TaskRow(title: String, reward: String, done: Boolean) {
    var checked by remember { mutableStateOf(done) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { checked = !checked }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (checked) "✅" else "⬜", fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Text(title, Modifier.weight(1f), color = if (checked) InkSoft else Ink, fontSize = 15.sp)
        Text(reward, color = Accent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
private fun LessonBanner(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFF4D6))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🎓", fontSize = 28.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Урок дня", fontSize = 13.sp, color = InkSoft)
            Text("Что такое кешбэк и откуда он берётся?", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink)
        }
        Text("→", fontSize = 20.sp, color = Ink)
    }
}

// ---------------------------------------------------------------- Lessons

@Composable
fun LessonsScreen() {
    val lessons = listOf(
        Triple("💰", "Откуда берутся деньги", "5 минут · 3 вопроса"),
        Triple("💳", "Как работает банковская карта", "7 минут · 4 вопроса"),
        Triple("🛡️", "Как не попасться мошенникам", "6 минут · 5 вопросов"),
        Triple("🎯", "Зачем копить и как ставить цели", "5 минут · 3 вопроса"),
    )
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Уроки", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Пройди урок — получи звёздочку ⭐", fontSize = 14.sp, color = InkSoft)
        Spacer(Modifier.height(34.dp))
        lessons.forEachIndexed { index, (emoji, title, meta) ->
            LessonCard(emoji, title, meta, if (index == 0) Modifier.hidingSpot("lessons.card") else Modifier)
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun LessonCard(emoji: String, title: String, meta: String, modifier: Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable {}
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEFF2FF)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 26.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink)
            Text(meta, fontSize = 13.sp, color = InkSoft)
        }
    }
}

// ---------------------------------------------------------------- Transfer (protected)

@Composable
fun TransferScreen(onBack: () -> Unit) {
    // Payment screens stay clean: while this is on screen the creature never shows.
    HideAndSeekBlocker()
    BackHandler(onBack = onBack)
    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .systemBarsPadding()
            .imePadding()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text("←", fontSize = 20.sp, color = Ink)
            }
            Spacer(Modifier.width(12.dp))
            Text("Перевод", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Экраны оплаты и переводов защищены: здесь прятунчик не появляется.",
            fontSize = 13.sp,
            color = InkSoft,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Кому: номер телефона") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { value -> amount = value.filter { it.isDigit() } },
            label = { Text("Сумма, ₽") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Перевести", fontSize = 16.sp)
        }
    }
}

// ---------------------------------------------------------------- Website mock

@Composable
fun WebsiteScreen(onExit: () -> Unit) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    BackHandler {
        val view = webView
        if (view != null && view.canGoBack()) view.goBack() else onExit()
    }
    HidingSpotsWebView(
        url = DemoConfig.WEBSITE_URL,
        modifier = Modifier.fillMaxSize(),
        onCreated = { webView = it },
    )
}
