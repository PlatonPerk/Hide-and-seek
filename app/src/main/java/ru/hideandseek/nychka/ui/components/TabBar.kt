package ru.hideandseek.nychka.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ru.hideandseek.nychka.R
import ru.hideandseek.nychka.hidespots.hideSpot
import ru.hideandseek.nychka.hidespots.occluder
import ru.hideandseek.nychka.ui.theme.KidsColors
import ru.hideandseek.nychka.ui.theme.KidsType

enum class Tab { Main, Piggy, Tasks, Content }

/** Плавающий таб-бар с лапкой посередине (макет «Главный»). */
@Composable
fun KidsTabBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    onPawLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            .shadow(elevation = 18.dp, shape = shape, ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
            .clip(shape)
            .background(Color.White)
            .height(88.dp)
            .occluder("tabbar"),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem(
                label = "Главный", selected = selected == Tab.Main, spotId = "tab.main",
                icon = R.drawable.tab_home, iconSize = Art.TabHome,
                activeIcon = R.drawable.tab_home_active, activeIconSize = Art.TabHomeActive,
                activeIconOffsetY = (-5).dp,
                onClick = { onSelect(Tab.Main) },
            )
            TabItem(
                label = "Копилки", selected = selected == Tab.Piggy, spotId = "tab.piggy",
                icon = R.drawable.tab_piggy, iconSize = Art.TabPiggy,
                activeIcon = R.drawable.tab_piggy_active, activeIconSize = Art.TabPiggyActive,
                activeIconOffsetX = (-4).dp, activeIconOffsetY = (-9).dp,
                onClick = { onSelect(Tab.Piggy) },
            )
            PawButton(onLongPress = onPawLongPress)
            TabItem(
                label = "Дела", selected = selected == Tab.Tasks, spotId = "tab.tasks",
                icon = R.drawable.tab_tasks, iconSize = Art.TabTasks,
                activeIcon = R.drawable.tab_tasks_active, activeIconSize = Art.TabTasksActive,
                activeIconOffsetX = 3.dp, activeIconOffsetY = (-5).dp,
                onClick = { onSelect(Tab.Tasks) },
            )
            TabItem(
                label = "Контент", selected = selected == Tab.Content, spotId = "tab.content",
                icon = R.drawable.tab_content, iconSize = Art.TabContent,
                activeIcon = R.drawable.tab_content_active, activeIconSize = Art.TabContent,
                onClick = { onSelect(Tab.Content) },
            )
        }
    }
}

@Composable
private fun RowScope.TabItem(
    label: String,
    selected: Boolean,
    spotId: String,
    @DrawableRes icon: Int,
    iconSize: DpSize,
    @DrawableRes activeIcon: Int,
    activeIconSize: DpSize,
    onClick: () -> Unit,
    activeIconOffsetX: Dp = 0.dp,
    activeIconOffsetY: Dp = 0.dp,
) {
    // Иконка «подпрыгивает», когда вкладка становится активной
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            bounce.snapTo(0.8f)
            bounce.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
        }
    }
    val labelColor by animateColorAsState(
        targetValue = if (selected) KidsColors.Green else KidsColors.TabInactive,
        animationSpec = tween(durationMillis = 200),
        label = "tabLabel",
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .tappable(pressedScale = 0.88f, ripple = false, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .graphicsLayer {
                    scaleX = bounce.value
                    scaleY = bounce.value
                }
                .hideSpot(spotId),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = selected, animationSpec = tween(durationMillis = 180), label = "tabIcon") { active ->
                Image(
                    painter = painterResource(if (active) activeIcon else icon),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(
                            x = if (active) activeIconOffsetX else 0.dp,
                            y = if (active) activeIconOffsetY else 0.dp,
                        )
                        .requiredSize(if (active) activeIconSize else iconSize),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = label, style = KidsType.Tab, color = labelColor)
    }
}

@Composable
private fun RowScope.PawButton(onLongPress: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .offset(y = (-10).dp)
                .tappable(pressedScale = 0.82f, ripple = false, onLongClick = onLongPress)
                .hideSpot("tab.paw"),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.tab_paw),
                contentDescription = "Лапка",
                modifier = Modifier.requiredSize(Art.TabPaw),
            )
        }
    }
}
