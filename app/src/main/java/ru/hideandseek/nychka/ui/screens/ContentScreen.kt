package ru.hideandseek.nychka.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.hideandseek.nychka.R
import ru.hideandseek.nychka.hidespots.hideSpot
import ru.hideandseek.nychka.hidespots.noHideZone
import ru.hideandseek.nychka.ui.components.Art
import ru.hideandseek.nychka.ui.components.CardShape
import ru.hideandseek.nychka.ui.components.CheckBadge
import ru.hideandseek.nychka.ui.components.CourseIcon
import ru.hideandseek.nychka.ui.components.HeartIcon
import ru.hideandseek.nychka.ui.components.ImagePlaceholder
import ru.hideandseek.nychka.ui.components.KidsAvatar
import ru.hideandseek.nychka.ui.components.PauseIcon
import ru.hideandseek.nychka.ui.components.PlayIcon
import ru.hideandseek.nychka.ui.components.ProgressBar
import ru.hideandseek.nychka.ui.components.SectionTitle
import ru.hideandseek.nychka.ui.components.kidsCard
import ru.hideandseek.nychka.ui.components.nb
import ru.hideandseek.nychka.ui.components.tappable
import ru.hideandseek.nychka.ui.theme.KidsColors
import ru.hideandseek.nychka.ui.theme.KidsType

private val LikedColor = Color(0xFFFF5A7E)

/**
 * Вкладка «Контент» по макету Figma (третий экран в разделе «Главный»).
 * Лента длинная, поэтому это LazyColumn: в каждом кадре компонуется только видимая часть.
 *
 * Места для пряток: content.avatar, content.hero, content.story_1..4, content.audio, content.chip_*,
 * content.card_1..5, content.top5_title, content.top5_1..5, content.algoritmika_mascot, content.alg_1..3.
 * Запретные зоны: content.ad (рекламный баннер).
 */
@Composable
fun ContentScreen(modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize().background(KidsColors.Page)) {
        item(key = "header") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .drawWithCache {
                        val brush = Brush.verticalGradient(
                            listOf(Color(0xFFE4DBFA), Color(0xFFEDEBF8), KidsColors.Page),
                            endY = size.height,
                        )
                        onDrawBehind { drawRect(brush) }
                    }
                    .statusBarsPadding(),
            ) {
                KidsAvatar("content.avatar", Modifier.padding(start = 24.dp, top = 16.dp))
                Spacer(Modifier.height(37.dp))
                HeroCarousel()
                Spacer(Modifier.height(36.dp))
            }
        }
        item(key = "stories") { Stories() }
        item(key = "audio") { AudioCard(Modifier.padding(top = 33.dp)) }
        item(key = "chips") { FilterChips(Modifier.padding(top = 43.dp)) }
        item(key = "card_1") {
            ContentCard(
                image = R.drawable.img_sharkcat, kind = Kind.Course,
                title = "Короткое название в_одну или даже в_две строки максимум",
                progress = 0.82f, spotId = "content.card_1", modifier = Modifier.padding(top = 33.dp),
            )
        }
        item(key = "card_2") {
            ContentCard(
                image = R.drawable.img_piggy, kind = Kind.Video,
                title = "Короткое название в_одну строку", done = true, spotId = "content.card_2",
                modifier = Modifier.padding(top = 20.dp),
            )
        }
        item(key = "ad") {
            Image(
                painter = painterResource(R.drawable.banner_dream),
                contentDescription = "Исполни мечту",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 25.dp)
                    .fillMaxWidth()
                    .tappable(RoundedCornerShape(17.dp))
                    .noHideZone("content.ad"),
            )
        }
        item(key = "top5") {
            Column(Modifier.padding(top = 46.dp)) {
                SectionTitle("Топ 5 за_неделю", Modifier.padding(start = 16.dp).hideSpot("content.top5_title"))
                Spacer(Modifier.height(40.dp))
                TopFive()
            }
        }
        item(key = "card_3") {
            ContentCard(
                image = R.drawable.img_kids, kind = Kind.Video,
                title = "Короткое название в_одну или даже в_две строки максимум",
                done = true, spotId = "content.card_3", modifier = Modifier.padding(top = 46.dp),
            )
        }
        item(key = "algoritmika") { Algoritmika(Modifier.padding(top = 28.dp)) }
        item(key = "card_4") {
            ContentCard(
                image = R.drawable.img_girl_dog, kind = Kind.Video,
                title = "Короткое название в_одну строку", spotId = "content.card_4",
                modifier = Modifier.padding(top = 33.dp),
            )
        }
        item(key = "card_5") {
            ContentCard(
                image = null, kind = Kind.Video,
                title = "Короткое название в_одну или даже в_две строки максимум а_вдруг три строки",
                spotId = "content.card_5", modifier = Modifier.padding(top = 20.dp),
            )
        }
        // место под плавающий таб-бар
        item(key = "bottom") { Spacer(Modifier.navigationBarsPadding().height(124.dp)) }
    }
}

private enum class Kind(val label: String) { Video("видео"), Course("курс") }

@Composable
private fun HeroCarousel() {
    val pager = rememberPagerState(initialPage = 1) { 3 }
    HorizontalPager(
        state = pager,
        contentPadding = PaddingValues(horizontal = 32.dp),
        pageSpacing = 16.dp,
        modifier = Modifier.fillMaxWidth(),
    ) { page ->
        val pageModifier = Modifier.fillMaxWidth().aspectRatio(448f / 385f)
        when (page) {
            1 -> Image(
                painter = painterResource(R.drawable.content_hero),
                contentDescription = "Агент Kids Луана",
                contentScale = ContentScale.Crop,
                modifier = pageModifier.tappable(RoundedCornerShape(20.dp)).hideSpot("content.hero"),
            )
            0 -> HeroStub(R.drawable.img_kids, "Строим мир", pageModifier)
            else -> HeroStub(R.drawable.img_sharkcat, "Арт-челлендж", pageModifier)
        }
    }
}

@Composable
private fun HeroStub(@DrawableRes image: Int, title: String, modifier: Modifier) {
    Box(modifier.tappable(RoundedCornerShape(20.dp))) {
        Image(painterResource(image), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)), startY = 300f),
            ),
        )
        Text(
            title,
            style = KidsType.Display.copy(color = Color.White, fontSize = 24.sp),
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
        )
    }
}

@Composable
private fun Stories() {
    val stories = listOf(R.drawable.story_quiz, R.drawable.story_cloud, R.drawable.story_green, R.drawable.story_kids)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(stories) { i, res ->
            Image(
                painter = painterResource(res),
                contentDescription = null,
                modifier = Modifier
                    .size(Art.Story)
                    .tappable(RoundedCornerShape(16.dp), pressedScale = 0.92f)
                    .hideSpot("content.story_${i + 1}"),
            )
        }
    }
}

@Composable
private fun AudioCard(modifier: Modifier = Modifier) {
    var playing by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .tappable(CardShape, pressedScale = 0.98f)
            .background(KidsColors.Card)
            .padding(20.dp)
            .hideSpot("content.audio"),
    ) {
        Image(painterResource(R.drawable.audio_cover), contentDescription = null, modifier = Modifier.size(100.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).height(100.dp)) {
            Spacer(Modifier.height(6.dp))
            Text("Эфир Звука", style = KidsType.Caption.copy(color = KidsColors.TextTertiary))
            Spacer(Modifier.height(6.dp))
            Text("Три кота три хвоста", style = KidsType.Body)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressBar(1f, Modifier.weight(1f))
                Spacer(Modifier.width(14.dp))
                // Кнопка из макета: переключается «играть» ↔ «пауза»
                Box(
                    Modifier
                        .size(40.dp)
                        .tappable(RoundedCornerShape(12.dp), pressedScale = 0.85f) { playing = !playing }
                        .background(KidsColors.GreyButton),
                    contentAlignment = Alignment.Center,
                ) {
                    if (playing) PauseIcon(size = 16.dp) else PlayIcon(color = KidsColors.TextPrimary, size = 16.dp)
                }
            }
        }
    }
}

private data class ChipData(val label: String, @param:DrawableRes val icon: Int?, val spotId: String)

@Composable
private fun FilterChips(modifier: Modifier = Modifier) {
    val chips = listOf(
        ChipData("Все", null, "content.chip_all"),
        ChipData("ИИ", R.drawable.ic_chip_ai, "content.chip_ai"),
        ChipData("Деньги", R.drawable.ic_chip_money, "content.chip_money"),
        ChipData("Скетчи", R.drawable.ic_chip_skills, "content.chip_sketch"),
    )
    // Выбранный фильтр обведён «от руки», как «Все» в макете; тап переносит обводку
    var selected by rememberSaveable { mutableIntStateOf(0) }
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(chips) { i, chip ->
            Chip(chip, selected = i == selected) { selected = i }
        }
    }
}

@Composable
private fun Chip(chip: ChipData, selected: Boolean, onClick: () -> Unit) {
    val doodle by animateFloatAsState(if (selected) 1f else 0f, tween(durationMillis = 220), label = "doodle")
    Row(
        modifier = Modifier
            .height(56.dp)
            .drawWithContent {
                drawContent()
                if (doodle > 0f) {
                    val stroke = Stroke(1.8.dp.toPx())
                    val pad = 5.dp.toPx()
                    val ink = Color.Black.copy(alpha = doodle)
                    rotate(-6f) {
                        drawOval(
                            ink, topLeft = Offset(-pad, -pad * 0.6f),
                            size = Size(size.width + 2 * pad, size.height + 1.2f * pad), style = stroke,
                        )
                    }
                    rotate(4f) {
                        drawOval(
                            ink, topLeft = Offset(-pad * 0.4f, -pad),
                            size = Size(size.width + 0.8f * pad, size.height + 1.6f * pad), style = stroke,
                        )
                    }
                }
            }
            .tappable(RoundedCornerShape(28.dp), pressedScale = 0.92f, onClick = onClick)
            .background(KidsColors.Card)
            .padding(horizontal = 20.dp)
            .hideSpot(chip.spotId),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (chip.icon != null) {
            Image(painterResource(chip.icon), contentDescription = null, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(chip.label, style = KidsType.TileTitle)
    }
}

@Composable
private fun KindLabel(kind: Kind) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (kind) {
            Kind.Video -> PlayIcon(size = 13.dp)
            Kind.Course -> CourseIcon(size = 16.dp)
        }
        Spacer(Modifier.width(6.dp))
        Text(kind.label, style = KidsType.Caption.copy(fontSize = 14.sp, color = KidsColors.TextTertiary))
    }
}

/** Лайк из макета («♥ 999 тыс»): тап закрашивает сердечко. */
@Composable
private fun LikeButton() {
    var liked by rememberSaveable { mutableStateOf(false) }
    val color by animateColorAsState(if (liked) LikedColor else KidsColors.TextTertiary, label = "like")
    Row(
        Modifier
            .tappable(CircleShape, pressedScale = 0.8f, ripple = false) { liked = !liked }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeartIcon(color = color)
        Spacer(Modifier.width(6.dp))
        Text("999 тыс", style = KidsType.Caption.copy(fontSize = 14.sp, color = KidsColors.TextTertiary))
    }
}

@Composable
private fun ContentCard(
    @DrawableRes image: Int?,
    kind: Kind,
    title: String,
    spotId: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    done: Boolean = false,
) {
    Column(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .tappable(CardShape, pressedScale = 0.97f)
            .background(KidsColors.Card)
            .padding(16.dp)
            .hideSpot(spotId),
    ) {
        if (image != null) {
            Image(
                painter = painterResource(image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(448f / 248f).clip(RoundedCornerShape(11.dp)),
            )
        } else {
            ImagePlaceholder(Modifier.fillMaxWidth().aspectRatio(448f / 245f))
        }
        Spacer(Modifier.height(12.dp))
        KindLabel(kind)
        Spacer(Modifier.height(8.dp))
        Text(title.nb(), style = KidsType.CardTitle)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LikeButton()
            Spacer(Modifier.weight(1f))
            when {
                done -> CheckBadge()
                progress != null -> ProgressBar(progress, Modifier.width(75.dp))
            }
        }
    }
}

/** Карточка в горизонтальной ленте («Топ 5», «Алгоритмика»). [rank] — большая цифра за карточкой. */
@Composable
private fun SmallCard(
    @DrawableRes image: Int,
    spotId: String,
    rank: Int? = null,
    progress: Float? = null,
    done: Boolean = false,
) {
    // Без обрезки по форме: большая цифра выходит за границы карточки
    Box(Modifier.width(180.dp).tappable(pressedScale = 0.95f, ripple = false).hideSpot(spotId)) {
        Box(Modifier.matchParentSize().kidsCard())
        if (rank != null) {
            Text(
                text = "$rank",
                style = TextStyle(
                    fontSize = 150.sp, lineHeight = 150.sp, fontWeight = FontWeight.Black,
                    color = KidsColors.Numeral.copy(alpha = 0.9f),
                ),
                modifier = Modifier.offset(x = (-20).dp, y = (-46).dp),
            )
        }
        Column(Modifier.padding(16.dp)) {
            Image(
                painter = painterResource(image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(224f / 156f).clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.height(12.dp))
            KindLabel(Kind.Video)
            Spacer(Modifier.height(8.dp))
            Text("Короткое название\nв две строки", style = KidsType.Body.copy(fontWeight = FontWeight.Medium), minLines = 2)
            Spacer(Modifier.height(30.dp))
            Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                when {
                    done -> CheckBadge()
                    progress != null -> ProgressBar(progress, Modifier.width(75.dp))
                }
            }
        }
    }
}

@Composable
private fun TopFive() {
    val images = listOf(
        R.drawable.thumb_monster, R.drawable.thumb_graffiti, R.drawable.thumb_monster,
        R.drawable.thumb_graffiti, R.drawable.thumb_monster,
    )
    LazyRow(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        itemsIndexed(images) { i, img ->
            SmallCard(
                image = img, spotId = "content.top5_${i + 1}", rank = i + 1,
                progress = if (i == 0) 0.6f else null, done = i == 2,
            )
        }
    }
}

@Composable
private fun Algoritmika(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().background(KidsColors.Algoritmika)) {
        Image(
            painter = painterResource(R.drawable.algoritmika_mascot),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(Art.AlgoritmikaMascot)
                .tappable(pressedScale = 0.9f, ripple = false)
                .hideSpot("content.algoritmika_mascot"),
        )
        Column(Modifier.padding(top = 66.dp, bottom = 24.dp)) {
            SectionTitle("Алгоритмика", Modifier.padding(start = 16.dp))
            Spacer(Modifier.height(30.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(listOf(R.drawable.thumb_graffiti, R.drawable.thumb_monster, R.drawable.thumb_graffiti)) { i, img ->
                    SmallCard(
                        image = img, spotId = "content.alg_${i + 1}",
                        done = i == 0, progress = if (i == 1) 0.5f else null,
                    )
                }
            }
        }
    }
}
