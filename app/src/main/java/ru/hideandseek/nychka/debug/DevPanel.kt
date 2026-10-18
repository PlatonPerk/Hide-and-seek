package ru.hideandseek.nychka.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hideandseek.HideAndSeekController
import app.hideandseek.core.Edge
import kotlinx.coroutines.launch

/**
 * Панель демо (только для показа на защите): кто и где прячется сегодня,
 * перемотка дней, «спрятать снова» и перезагрузка контента с GitHub.
 * Открывается вместе с отладочными рамками долгим нажатием на лапку в таб-баре.
 */
@Composable
fun DevPanel(hideAndSeek: HideAndSeekController, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val plan = hideAndSeek.plan
    val content = hideAndSeek.content
    val progress = hideAndSeek.progress
    val shift = hideAndSeek.dayShift
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier
            .padding(12.dp)
            .fillMaxWidth()
            .shadow(16.dp, shape)
            .clip(shape)
            .background(Color(0xFF1F2430))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Панель демо",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                "✕",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable(onClick = onClose)
                    .padding(8.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        DevLine(
            "Сегодня",
            hideAndSeek.today.toString() + if (shift != 0) "  (сдвиг ${if (shift > 0) "+" else ""}$shift)" else "",
        )
        DevLine(
            "Прячется",
            plan?.let { "${it.creature.name} → ${screenName(it.spot.id)}, ${edgeName(it.spot.edge)} (${it.spot.id})" }
                ?: "сегодня никто не прячется",
        )
        hideAndSeek.tourPosition?.let { (step, count) -> DevLine("Маршрут", "место $step из $count") }
        DevLine("Найден", if (hideAndSeek.isFoundToday) "да" else "нет")
        DevLine("Контент", content?.let { "v${it.version}, ${hideAndSeek.contentOrigin}" } ?: "загружается…")
        DevLine(
            "Находок",
            "${progress.totalFound}, серия ${progress.streak} " +
                (if (hideAndSeek.streakByFinds) "(по находкам)" else "(по дням)") +
                ", коллекция ${progress.collection.size}",
        )
        hideAndSeek.lastError?.let { DevLine("Ошибка", it) }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DevButton("◀ день", Modifier.weight(1f)) { hideAndSeek.dayShift = shift - 1 }
            DevButton("день ▶", Modifier.weight(1f)) { hideAndSeek.dayShift = shift + 1 }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DevButton("Спрятать снова", Modifier.weight(1f)) { hideAndSeek.resetToday() }
            DevButton("Обновить контент", Modifier.weight(1f)) {
                scope.launch { hideAndSeek.refresh() }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DevButton(
                if (hideAndSeek.demoRehide) "Перепрятка: вкл" else "Перепрятка: выкл",
                Modifier.weight(1f),
            ) { hideAndSeek.demoRehide = !hideAndSeek.demoRehide }
            DevButton("Сбросить находки", Modifier.weight(1f)) { hideAndSeek.resetAll() }
        }
        if (hideAndSeek.demoRehide) {
            Spacer(Modifier.height(8.dp))
            DevButton("Следующее место ▶", Modifier.fillMaxWidth()) { hideAndSeek.demoNextSpot() }
        }
    }
}

private fun edgeName(edge: Edge): String = when (edge) {
    Edge.TOP -> "сверху"
    Edge.BOTTOM -> "снизу"
    Edge.LEFT -> "сбоку слева"
    Edge.RIGHT -> "сбоку справа"
}

/** Вкладка по началу id места (main.banner → «Главный»). */
private fun screenName(spotId: String): String = when (spotId.substringBefore('.')) {
    "main" -> "«Главный»"
    "piggy" -> "«Копилки»"
    "tasks" -> "«Дела»"
    "content" -> "«Контент»"
    else -> spotId.substringBefore('.')
}

@Composable
private fun DevLine(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.width(84.dp))
        Text(value, color = Color.White, fontSize = 13.sp)
    }
}

@Composable
private fun DevButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 13.sp)
    }
}
