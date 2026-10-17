package app.hideandseek.core

/** Which side of the element the creature peeks out from. */
enum class Edge(val rotationDegrees: Float) {
    TOP(0f), RIGHT(90f), BOTTOM(180f), LEFT(270f);

    companion object {
        fun parse(value: String?): Edge = when (value?.trim()?.lowercase()) {
            "right", "end" -> RIGHT
            "bottom" -> BOTTOM
            "left", "start" -> LEFT
            else -> TOP
        }
    }
}

/** Inclusive date range; a missing end means "open". */
data class DateWindow(val from: Day? = null, val to: Day? = null) {
    operator fun contains(day: Day): Boolean =
        (from == null || day >= from) && (to == null || day <= to)

    companion object {
        val Always = DateWindow()
    }
}

data class CreatureDef(
    val id: String,
    val name: String,
    /** ARGB, e.g. 0xFFFF8A3D. */
    val color: Int,
    /** Optional sprite (https://… or asset:path). Without it the creature is drawn in code. */
    val imageUrl: String? = null,
    val weight: Int = 1,
    /** Rare creatures sparkle; give them a small weight. */
    val rare: Boolean = false,
    val window: DateWindow = DateWindow.Always,
    /** Позы-картинки (лиса Нычка). Если их нет или они не загрузились — работает [imageUrl] или рисунок кодом. */
    val sprites: CreatureSprites? = null,
)

/**
 * Одна поза-картинка.
 * [anchorX]/[anchorY] — точка картинки в долях ширины и высоты, которая совмещается с краем элемента
 * (для поз «выглядывает») или стоит на «земле» (ноги — для поз во весь рост).
 * [heightDp] — высота картинки на экране; ширина берётся из пропорций картинки.
 */
data class PoseDef(
    val image: String,
    /** Тот же кадр с закрытыми глазами, совпадает с [image] до пикселя. */
    val blink: String? = null,
    val heightDp: Int,
    val anchorX: Float,
    val anchorY: Float,
)

/**
 * Набор поз. Любая может отсутствовать:
 * [peek] — выглядывает из-за верхнего края, [side] — из-за правого края (для левого зеркалится),
 * [found] — «ой, нашли!», [celebrate] — радуется, [run] — убегает вправо (влево зеркалится).
 */
data class CreatureSprites(
    val peek: PoseDef? = null,
    val side: PoseDef? = null,
    val found: PoseDef? = null,
    val celebrate: PoseDef? = null,
    val run: PoseDef? = null,
) {
    /** Все адреса картинок (для предзагрузки). */
    val urls: List<String>
        get() = listOfNotNull(peek, side, found, celebrate, run)
            .flatMap { listOfNotNull(it.image, it.blink) }
            .distinct()

    enum class Kind(val key: String, val defaultHeightDp: Int, val defaultAnchorX: Float, val defaultAnchorY: Float) {
        PEEK("peek", 60, 0.5f, 1f),
        SIDE("side", 76, 0f, 0.5f),
        FOUND("found", 110, 0.5f, 1f),
        CELEBRATE("celebrate", 110, 0.5f, 1f),
        RUN("run", 110, 0.5f, 1f),
    }
}

data class SpotDef(
    /** Must match `Modifier.hidingSpot("…")` in the app or `data-hide-spot="…"` on the website. */
    val id: String,
    val edge: Edge = Edge.TOP,
    /** Position along the edge: 0 = start, 1 = end. */
    val align: Float = 0.5f,
    val weight: Int = 1,
    /** Optional CSS selector, so a website element can be a spot without editing the site. */
    val webSelector: String? = null,
    val window: DateWindow = DateWindow.Always,
)

data class TipDef(val text: String, val window: DateWindow = DateWindow.Always)

/** A fixed choice for one date (holidays, a demo), or `skip` to keep the creature home. */
data class SpecialDay(
    val day: Day,
    val creatureId: String? = null,
    val spotId: String? = null,
    val tip: String? = null,
    val skip: Boolean = false,
)

data class Schedule(
    val startDate: Day? = null,
    val endDate: Day? = null,
    /** ISO days 1 = Mon … 7 = Sun; empty = every day. */
    val daysOfWeek: Set<Int> = emptySet(),
    val everyNDays: Int = 1,
    /** Chance (per day, same for everyone) that the creature hides at all. */
    val chancePercent: Int = 100,
    /** Local hours when the creature may show up: [hoursFrom, hoursTo). */
    val hoursFrom: Int = 0,
    val hoursTo: Int = 24,
    /** Never use the same spot two hiding days in a row. */
    val avoidRepeat: Boolean = true,
) {
    fun allowsDay(day: Day, salt: String): Boolean {
        if (startDate != null && day < startDate) return false
        if (endDate != null && day > endDate) return false
        if (daysOfWeek.isNotEmpty() && day.dayOfWeek !in daysOfWeek) return false
        if (everyNDays > 1) {
            val anchor = startDate?.epochDay ?: 0
            if ((day.epochDay - anchor).mod(everyNDays) != 0) return false
        }
        if (chancePercent < 100) {
            if (chancePercent <= 0) return false
            if ((Seeded.hash("$salt|$day|chance") % 100u).toInt() >= chancePercent) return false
        }
        return true
    }

    fun allowsHour(hour: Int): Boolean = when {
        hoursFrom == hoursTo -> true
        hoursFrom < hoursTo -> hour >= hoursFrom && hour < hoursTo
        else -> hour >= hoursFrom || hour < hoursTo // window over midnight
    }
}

data class Look(
    val sizeDp: Int = 56,
    /** How much of the creature shows above the edge while hiding (0..1). */
    val peek: Float = 0.55f,
    /** Delay after the spot appears on screen before the creature peeks out. */
    val appearDelayMs: Long = 1200L,
    /** How long the "found" card stays (it never blocks the screen; a tap closes it). */
    val messageMs: Long = 5000L,
)

/** Texts support {name}, {streak}, {streakDays}, {total}. */
data class Texts(
    val foundTitle: String = "Нашёл!",
    val foundBody: String = "{name} прятался именно здесь. Завтра спрячется в другом месте.",
    val streak: String = "Ты находишь меня {streakDays} подряд!",
    val tipTitle: String = "Совет дня",
    val accessibilityLabel: String = "Здесь кто-то прячется. Нажми, чтобы найти",
)

data class HideAndSeekContent(
    val version: Int = 0,
    /** Kill switch: false hides the feature everywhere without a release. */
    val enabled: Boolean = true,
    /** Day boundaries for everyone are computed in this zone. */
    val timeZone: String = "Europe/Moscow",
    /** Change it to reshuffle all future days. */
    val salt: String = "",
    val schedule: Schedule = Schedule(),
    val look: Look = Look(),
    val creatures: List<CreatureDef> = emptyList(),
    val spots: List<SpotDef> = emptyList(),
    val tips: List<TipDef> = emptyList(),
    val specialDays: List<SpecialDay> = emptyList(),
    val texts: Texts = Texts(),
) {
    fun creature(id: String): CreatureDef? = creatures.firstOrNull { it.id == id }
    fun spot(id: String): SpotDef? = spots.firstOrNull { it.id == id }
    fun specialDay(day: Day): SpecialDay? = specialDays.firstOrNull { it.day == day }
    fun creaturesOn(day: Day): List<CreatureDef> = creatures.filter { it.weight > 0 && day in it.window }
    fun spotsOn(day: Day): List<SpotDef> = spots.filter { it.weight > 0 && day in it.window }
    fun tipsOn(day: Day): List<TipDef> = tips.filter { day in it.window }
}

/** Reads the content JSON (format documented in content/hideandseek.json). */
object ContentParser {

    private const val DEFAULT_COLOR = 0xFFFF8A3D.toInt()

    private val dayNames = mapOf(
        "mon" to 1, "tue" to 2, "wed" to 3, "thu" to 4, "fri" to 5, "sat" to 6, "sun" to 7,
        "пн" to 1, "вт" to 2, "ср" to 3, "чт" to 4, "пт" to 5, "сб" to 6, "вс" to 7,
    )

    fun parse(json: String): HideAndSeekContent {
        val root = MiniJson.parse(json).asObject() ?: throw JsonException("Root must be a JSON object")
        val schedule = root.obj("schedule") ?: emptyMap()
        val hours = schedule.obj("hours") ?: emptyMap()
        val look = root.obj("look") ?: emptyMap()
        val texts = root.obj("texts") ?: emptyMap()
        val defaultTexts = Texts()
        val defaultLook = Look()

        return HideAndSeekContent(
            version = root.int("version") ?: 0,
            enabled = root.boolean("enabled") ?: true,
            timeZone = root.string("timeZone") ?: "Europe/Moscow",
            salt = root.string("salt") ?: "",
            schedule = Schedule(
                startDate = Day.parse(schedule.string("startDate")),
                endDate = Day.parse(schedule.string("endDate")),
                daysOfWeek = parseDaysOfWeek(schedule.array("daysOfWeek")),
                everyNDays = (schedule.int("everyNDays") ?: 1).coerceAtLeast(1),
                chancePercent = (schedule.int("chancePercent") ?: 100).coerceIn(0, 100),
                hoursFrom = (hours.int("from") ?: 0).coerceIn(0, 24),
                hoursTo = (hours.int("to") ?: 24).coerceIn(0, 24),
                avoidRepeat = schedule.boolean("avoidRepeat") ?: true,
            ),
            look = Look(
                sizeDp = (look.int("sizeDp") ?: defaultLook.sizeDp).coerceIn(32, 120),
                peek = (look.number("peek")?.toFloat() ?: defaultLook.peek).coerceIn(0.25f, 0.9f),
                appearDelayMs = (look.number("appearDelayMs")?.toLong() ?: defaultLook.appearDelayMs)
                    .coerceIn(0L, 60_000L),
                messageMs = (look.number("messageMs")?.toLong() ?: defaultLook.messageMs)
                    .coerceIn(1_500L, 30_000L),
            ),
            creatures = parseCreatures(root.objects("creatures")),
            spots = root.objects("spots").mapNotNull(::parseSpot),
            tips = root.array("tips").mapNotNull(::parseTip),
            specialDays = root.objects("specialDays").mapNotNull(::parseSpecialDay),
            texts = Texts(
                foundTitle = texts.string("foundTitle") ?: defaultTexts.foundTitle,
                foundBody = texts.string("foundBody") ?: defaultTexts.foundBody,
                streak = texts.string("streak") ?: defaultTexts.streak,
                tipTitle = texts.string("tipTitle") ?: defaultTexts.tipTitle,
                accessibilityLabel = texts.string("accessibilityLabel") ?: defaultTexts.accessibilityLabel,
            ),
        )
    }

    /** "#RRGGBB" or "#AARRGGBB" → ARGB int. */
    fun parseColor(text: String?): Int? {
        val hex = text?.trim()?.removePrefix("#") ?: return null
        val value = hex.toLongOrNull(16) ?: return null
        return when (hex.length) {
            6 -> (0xFF000000L or value).toInt()
            8 -> value.toInt()
            else -> null
        }
    }

    private fun window(o: Map<String, Any?>) = DateWindow(Day.parse(o.string("from")), Day.parse(o.string("to")))

    /** "spritesFrom": "nychka" — взять позы у другого зверька, чтобы не повторять их в JSON. */
    private fun parseCreatures(items: List<Map<String, Any?>>): List<CreatureDef> {
        val parsed = items.mapNotNull { o -> parseCreature(o)?.let { it to o.string("spritesFrom") } }
        val byId = parsed.associate { it.first.id to it.first }
        return parsed.map { (creature, from) ->
            if (creature.sprites == null && from != null) creature.copy(sprites = byId[from]?.sprites) else creature
        }
    }

    private fun parseCreature(o: Map<String, Any?>): CreatureDef? {
        val id = o.string("id") ?: return null
        return CreatureDef(
            id = id,
            name = o.string("name") ?: id,
            color = parseColor(o.string("color")) ?: DEFAULT_COLOR,
            imageUrl = o.string("image"),
            weight = o.int("weight") ?: 1,
            rare = o.boolean("rare") ?: false,
            window = window(o),
            sprites = parseSprites(o["sprites"]),
        )
    }

    /** Битые или пустые позы не роняют разбор: такая поза просто пропускается. */
    internal fun parseSprites(value: Any?): CreatureSprites? {
        val o = value.asObject() ?: return null
        fun pose(kind: CreatureSprites.Kind) = parsePose(o[kind.key], kind)
        val sprites = CreatureSprites(
            peek = pose(CreatureSprites.Kind.PEEK),
            side = pose(CreatureSprites.Kind.SIDE),
            found = pose(CreatureSprites.Kind.FOUND),
            celebrate = pose(CreatureSprites.Kind.CELEBRATE),
            run = pose(CreatureSprites.Kind.RUN),
        )
        return sprites.takeIf { it.urls.isNotEmpty() }
    }

    /** Поза — строка с адресом картинки или объект { image, blink, heightDp, anchor: [x, y] }. */
    private fun parsePose(value: Any?, kind: CreatureSprites.Kind): PoseDef? {
        if (value is String) {
            val image = value.trim().takeIf { it.isNotEmpty() } ?: return null
            return PoseDef(image, null, kind.defaultHeightDp, kind.defaultAnchorX, kind.defaultAnchorY)
        }
        val o = value.asObject() ?: return null
        val image = o.string("image") ?: return null
        val anchor = o.array("anchor").mapNotNull { (it as? Number)?.toFloat() }
        val valid = anchor.size == 2 && anchor.all { it in 0f..1f }
        return PoseDef(
            image = image,
            blink = o.string("blink"),
            heightDp = (o.int("heightDp") ?: kind.defaultHeightDp).coerceIn(24, 240),
            anchorX = if (valid) anchor[0] else kind.defaultAnchorX,
            anchorY = if (valid) anchor[1] else kind.defaultAnchorY,
        )
    }

    private fun parseSpot(o: Map<String, Any?>): SpotDef? {
        val id = o.string("id") ?: return null
        return SpotDef(
            id = id,
            edge = Edge.parse(o.string("edge")),
            align = (o.number("align")?.toFloat() ?: 0.5f).coerceIn(0f, 1f),
            weight = o.int("weight") ?: 1,
            webSelector = o.string("web"),
            window = window(o),
        )
    }

    private fun parseTip(item: Any?): TipDef? = when (item) {
        is String -> item.trim().takeIf { it.isNotEmpty() }?.let { TipDef(it) }
        else -> item.asObject()?.let { o -> o.string("text")?.let { TipDef(it, window(o)) } }
    }

    private fun parseSpecialDay(o: Map<String, Any?>): SpecialDay? {
        val day = Day.parse(o.string("date")) ?: return null
        return SpecialDay(
            day = day,
            creatureId = o.string("creature"),
            spotId = o.string("spot"),
            tip = o.string("tip"),
            skip = o.boolean("skip") ?: false,
        )
    }

    private fun parseDaysOfWeek(values: List<Any?>): Set<Int> = values.mapNotNull { v ->
        when (v) {
            is Number -> v.toInt().takeIf { it in 1..7 }
            is String -> v.trim().lowercase().let { dayNames[it.take(3)] ?: dayNames[it.take(2)] }
            else -> null
        }
    }.toSet()
}

/** Fills {placeholders} in content texts. */
object TextTemplate {
    fun fill(template: String, values: Map<String, String>): String {
        var result = template
        for ((key, value) in values) result = result.replace("{$key}", value)
        return result
    }

    /** 1 день, 2 дня, 5 дней. */
    fun ruDays(n: Int): String {
        val mod100 = n % 100
        val mod10 = n % 10
        val word = when {
            mod100 in 11..14 -> "дней"
            mod10 == 1 -> "день"
            mod10 in 2..4 -> "дня"
            else -> "дней"
        }
        return "$n $word"
    }
}
