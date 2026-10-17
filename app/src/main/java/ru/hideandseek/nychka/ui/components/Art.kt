package ru.hideandseek.nychka.ui.components

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Размеры иллюстраций в dp.
 *
 * Картинки нарезаны из макета в плотности hdpi (1,5 px на dp). На части устройств растровые
 * ресурсы попадают в Compose без пересчёта плотности, и картинка без явного размера рисуется
 * «в пикселях» — вдвое меньше нужного. Поэтому у каждой иллюстрации размер задан явно.
 */
object Art {
    val CardStack = DpSize(296.dp, 146.dp)
    val ActionIcon = DpSize(56.dp, 57.3.dp)
    val HistoryIcon = DpSize(53.3.dp, 53.3.dp)
    val Story = DpSize(80.dp, 113.3.dp)
    val AlgoritmikaMascot = DpSize(142.7.dp, 118.dp)
    val PiggySummary = DpSize(132.dp, 173.3.dp)

    val TabHome = DpSize(32.dp, 32.dp)
    val TabHomeActive = DpSize(54.7.dp, 42.7.dp)
    val TabPiggy = DpSize(33.3.dp, 32.dp)
    val TabPiggyActive = DpSize(41.3.dp, 45.3.dp)
    val TabTasks = DpSize(30.7.dp, 30.7.dp)
    val TabTasksActive = DpSize(42.7.dp, 44.dp)
    val TabContent = DpSize(30.7.dp, 30.7.dp)
    val TabPaw = DpSize(57.3.dp, 60.dp)
}
