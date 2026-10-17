package ru.hideandseek.nychka.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import ru.hideandseek.nychka.ui.components.CheckBadge
import ru.hideandseek.nychka.ui.components.CourseIcon
import ru.hideandseek.nychka.ui.components.HeartIcon
import ru.hideandseek.nychka.ui.components.ImagePlaceholder
import ru.hideandseek.nychka.ui.components.KidsAvatar
import ru.hideandseek.nychka.ui.components.PlayIcon
import ru.hideandseek.nychka.ui.components.ProgressBar
import ru.hideandseek.nychka.ui.components.SectionTitle
import ru.hideandseek.nychka.ui.components.kidsCard
import ru.hideandseek.nychka.ui.components.nb
import ru.hideandseek.nychka.ui.theme.KidsColors
import ru.hideandseek.nychka.ui.theme.KidsType

/**
 * Вкладка «Контент» по макету Figma (третий экран в разделе «Главный»).
 *
 * Места для пряток: content.avatar, content.hero, content.story_1..4, content.audio, content.chip_*,
 * content.card_1..5, content.top5_title, content.top5_1..5, content.algoritmika_mascot, content.alg_1..3.
 * Запретные зоны: content.ad (рекламный баннер).
 */
@Composable
fun ContentScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .drawBehind {
                drawRect(KidsColors.Page)
                val band = 420.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFE4DBFA), Color(0xFFEDEBF8), KidsColors.Page), endY = band),
                    size = Size(size.width, band),
                )
            }
            .statusBarsPadding(),
    ) {
        KidsAvatar("content.avatar", Modifier.padding(start = 24.dp, top = 16.dp))
        Spacer(Modifier.height(37.dp))
        HeroCarousel()
        Spacer(Modifier.height(36.dp))
        Stories()
        Spacer(Modifier.height(33.dp))
        AudioCard()
        Spacer(Modifier.height(43.dp))
        FilterChips()
        Spacer(Modifier.height(33.dp))
        ContentCard(
            image = R.drawable.img_sharkcat, kind = Kind.Course,
            title = "Короткое название в_одну или даже в_две строки максимум",
            progress = 0.82f, spotId = "content.card_1",
        )
        Spacer(Modifier.height(20.dp))
        ContentCard(
            image = R.drawable.img_piggy, kind = Kind.Video,
            title = "Короткое название в_одну строку", done = true, spotId = "content.card_2",
        )
        Spacer(Modifier.height(25.dp))
        Image(
            painter = painterResource(R.drawable.banner_dream),
            contentDescription = "Исполни мечту",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().noHideZone("content.ad"),
        )
        Spacer(Modifier.height(46.dp))
        SectionTitle("Топ 5 за_неделю", Modifier.padding(start = 16.dp).hideSpot("content.top5_title"))
        Spacer(Modifier.height(40.dp))
        TopFive()
        Spacer(Modifier.height(46.dp))
        ContentCard(
            image = R.drawable.img_kids, kind = Kind.Video,
            title = "Короткое название в_одну или даже в_две строки максимум",
            done = true, spotId = "content.card_3",
        )
        Spacer(Modifier.height(28.dp))
        Algoritmika()
        Spacer(Modifier.height(33.dp))
        ContentCard(
            image = R.drawable.img_girl_dog, kind = Kind.Video,
            title = "Короткое название в_одну строку", spotId = "content.card_4",
        )
        Spacer(Modifier.height(20.dp))
        ContentCard(
            image = null, kind = Kind.Video,
            title = "Короткое название в_одну или даже в_две строки максимум а_вдруг три строки",
            spotId = "content.card_5",
        )
        Spacer(Modifier.navigationBarsPadding().height(124.dp))
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
                modifier = pageModifier.clip(RoundedCornerShape(20.dp)).clickable { }.hideSpot("content.hero"),
            )
            0 -> HeroStub(R.drawable.img_kids, "Строим мир", pageModifier)
            else -> HeroStub(R.drawable.img_sharkcat, "Арт-челлендж", pageModifier)
        }
    }
}

@Composable
private fun HeroStub(@DrawableRes image: Int, title: String, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(20.dp))) {
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
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { }.hideSpot("content.story_${i + 1}"),
            )
        }
    }
}

@Composable
private fun AudioCard() {
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .kidsCard()
            .clickable { }
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
                Box(
                    Modifier.size(40.dp).kidsCard(KidsColors.GreyButton, radius = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PlayIcon(color = KidsColors.TextPrimary, size = 16.dp)
                }
            }
        }
    }
}

@Composable
private fun FilterChips() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Chip("Все", icon = null, selected = true, spotId = "content.chip_all") }
        item { Chip("ИИ", R.drawable.ic_chip_ai, spotId = "content.chip_ai") }
        item { Chip("Деньги", R.drawable.ic_chip_money, spotId = "content.chip_money") }
        item { Chip("Скетчи", R.drawable.ic_chip_skills, spotId = "content.chip_sketch") }
    }
}

@Composable
private fun Chip(label: String, @DrawableRes icon: Int?, spotId: String, selected: Boolean = false) {
    Row(
        modifier = Modifier
            .height(56.dp)
            .drawWithContent {
                drawContent()
                if (selected) {
                    // «Обведено от руки», как в макете
                    val stroke = Stroke(1.8.dp.toPx())
                    val pad = 5.dp.toPx()
                    rotate(-6f) {
                        drawOval(
                            Color.Black, topLeft = Offset(-pad, -pad * 0.6f),
                            size = Size(size.width + 2 * pad, size.height + 1.2f * pad), style = stroke,
                        )
                    }
                    rotate(4f) {
                        drawOval(
                            Color.Black, topLeft = Offset(-pad * 0.4f, -pad),
                            size = Size(size.width + 0.8f * pad, size.height + 1.6f * pad), style = stroke,
                        )
                    }
                }
            }
            .kidsCard(radius = 28.dp)
            .clickable { }
            .padding(horizontal = 20.dp)
            .hideSpot(spotId),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = KidsType.TileTitle)
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

@Composable
private fun ContentCard(
    @DrawableRes image: Int?,
    kind: Kind,
    title: String,
    spotId: String,
    progress: Float? = null,
    done: Boolean = false,
) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .kidsCard()
            .clickable { }
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
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeartIcon()
            Spacer(Modifier.width(6.dp))
            Text("999 тыс", style = KidsType.Caption.copy(fontSize = 14.sp, color = KidsColors.TextTertiary))
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
    Box(Modifier.width(180.dp).hideSpot(spotId)) {
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
private fun Algoritmika() {
    Box(Modifier.fillMaxWidth().background(KidsColors.Algoritmika)) {
        Image(
            painter = painterResource(R.drawable.algoritmika_mascot),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd).hideSpot("content.algoritmika_mascot"),
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
