package ru.hideandseek.nychka.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
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
                icon = R.drawable.tab_home, activeIcon = R.drawable.tab_home_active,
                activeIconOffsetY = (-5).dp,
                onClick = { onSelect(Tab.Main) },
            )
            TabItem(
                label = "Копилки", selected = selected == Tab.Piggy, spotId = "tab.piggy",
                icon = R.drawable.tab_piggy, activeIcon = R.drawable.tab_piggy,
                onClick = { onSelect(Tab.Piggy) },
            )
            PawButton(onLongPress = onPawLongPress)
            TabItem(
                label = "Дела", selected = selected == Tab.Tasks, spotId = "tab.tasks",
                icon = R.drawable.tab_tasks, activeIcon = R.drawable.tab_tasks_active,
                activeIconOffsetX = 3.dp, activeIconOffsetY = (-5).dp,
                onClick = { onSelect(Tab.Tasks) },
            )
            TabItem(
                label = "Контент", selected = selected == Tab.Content, spotId = "tab.content",
                icon = R.drawable.tab_content, activeIcon = R.drawable.tab_content_active,
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
    @DrawableRes activeIcon: Int,
    onClick: () -> Unit,
    activeIconOffsetX: Dp = 0.dp,
    activeIconOffsetY: Dp = 0.dp,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(34.dp).hideSpot(spotId), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(if (selected) activeIcon else icon),
                contentDescription = null,
                modifier = Modifier
                    .wrapContentSize(unbounded = true)
                    .offset(
                        x = if (selected) activeIconOffsetX else 0.dp,
                        y = if (selected) activeIconOffsetY else 0.dp,
                    ),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = KidsType.Tab,
            color = if (selected) KidsColors.Green else KidsColors.TabInactive,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.PawButton(onLongPress: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onLongPress,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(56.dp).offset(y = (-10).dp).hideSpot("tab.paw"), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.tab_paw),
                contentDescription = "Лапка",
                modifier = Modifier.wrapContentSize(unbounded = true),
            )
        }
    }
}
