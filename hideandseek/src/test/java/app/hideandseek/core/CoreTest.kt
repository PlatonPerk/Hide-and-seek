package app.hideandseek.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.time.LocalDate

class CoreTest {

    private val content = ContentParser.parse(
        """
        {
          "version": 7, "salt": "test", "timeZone": "Europe/Moscow",
          "schedule": { "startDate": "2026-01-01", "hours": { "from": 7, "to": 22 } },
          "creatures": [
            { "id": "a", "name": "A", "color": "#FF0000", "weight": 3 },
            { "id": "b", "name": "B", "color": "#00FF00", "weight": 1 },
          ],
          "spots": [
            { "id": "s1", "weight": 3 }, { "id": "s2", "edge": "bottom", "align": 0.2 },
            { "id": "s3", "edge": "Right", "web": ".card" }, { "id": "s4", "weight": 2 }
          ],
          "tips": ["t1", { "text": "t2" }],
          "specialDays": [
            { "date": "2026-12-31", "creature": "b", "spot": "s3", "tip": "NY" },
            { "date": "2026-11-11", "skip": true }
          ]
        }
        """.trimIndent(),
    )

    // ---- Day ----

    @Test
    fun dayMatchesJavaTime() {
        // Years 0001..9999, where both use the same "YYYY-MM-DD" format.
        for (epochDay in -719_162..2_932_896 step 97) {
            val expected = LocalDate.ofEpochDay(epochDay.toLong())
            val day = Day(epochDay)
            assertEquals(expected.toString(), day.toString())
            assertEquals(epochDay, Day.of(expected.year, expected.monthValue, expected.dayOfMonth).epochDay)
            assertEquals(expected.dayOfWeek.value, day.dayOfWeek)
        }
    }

    @Test
    fun dayParsingAndZones() {
        val day = Day.parse("2026-10-17")!!
        assertEquals("2026-10-17", day.toString())
        assertEquals(6, day.dayOfWeek) // Saturday
        assertNull(Day.parse("17.10.2026"))
        assertNull(Day.parse(null))
        // 2026-10-17 21:30 UTC is already 00:30 on the 18th in Moscow (UTC+3).
        val utc = 1792272600000L
        assertEquals("2026-10-17", Day.at(utc, 0).toString())
        assertEquals("2026-10-18", Day.at(utc, 3 * 3_600_000).toString())
        assertEquals(0, Day.hourAt(utc, 3 * 3_600_000))
        assertEquals(21, Day.hourAt(utc, 0))
    }

    // ---- Seeded ----

    @Test
    fun hashIsStableAcrossPlatforms() {
        // Reference values for the iOS port (FNV-1a 32 + fmix32 over UTF-8).
        assertEquals(0xAB3E7C0Bu, Seeded.hash(""))
        assertEquals(0x888D766Eu, Seeded.hash("hello"))
        assertEquals(0x47237649u, Seeded.hash("прятки|2026-10-17|spot"))
    }

    @Test
    fun weightedPickFollowsWeights() {
        val items = listOf("x" to 1, "y" to 3, "z" to 0)
        val counts = HashMap<String, Int>()
        for (i in 0 until 40_000) {
            val pick = Seeded.pickWeighted(items, "seed-$i") { it.second }!!
            counts[pick.first] = (counts[pick.first] ?: 0) + 1
        }
        assertNull(counts["z"])
        val share = counts.getValue("y") / 40_000.0
        assertTrue("share=$share", share in 0.73..0.77)
        assertNull(Seeded.pickWeighted(emptyList<String>(), "s") { 1 })
        assertNull(Seeded.pickWeighted(listOf("a"), "s") { 0 })
    }

    // ---- JSON ----

    @Test
    fun miniJsonParsesValues() {
        val parsed = MiniJson.parse(
            """{"a": [1, 2.5, -3e2, true, false, null], "s": "q\"\\\/\nЖ😀", "o": {}, "e": [],}""",
        ).asObject()!!
        assertEquals(listOf(1.0, 2.5, -300.0, true, false, null), parsed["a"])
        assertEquals("q\"\\/\nЖ😀", parsed["s"])
        assertEquals(emptyMap<String, Any?>(), parsed["o"])
        assertEquals(emptyList<Any?>(), parsed["e"])
        for (bad in listOf("", "{", "{\"a\" 1}", "[1 2]", "\"abc", "tru", "{} x", "[,]", "-")) {
            try {
                MiniJson.parse(bad)
                fail("Should not parse: $bad")
            } catch (expected: JsonException) {
                // ok
            }
        }
        assertEquals("\"a\\\"b\\n\"", MiniJson.quote("a\"b\n"))
    }

    @Test
    fun contentParsesWithDefaults() {
        assertEquals(7, content.version)
        assertTrue(content.enabled)
        assertEquals(2, content.creatures.size)
        assertEquals(0xFFFF0000.toInt(), content.creatures[0].color)
        assertEquals(Edge.BOTTOM, content.spot("s2")!!.edge)
        assertEquals(Edge.RIGHT, content.spot("s3")!!.edge)
        assertEquals(".card", content.spot("s3")!!.webSelector)
        assertEquals(0.2f, content.spot("s2")!!.align)
        assertEquals(listOf("t1", "t2"), content.tips.map { it.text })
        assertEquals(56, content.look.sizeDp)
        assertEquals("Нашёл!", content.texts.foundTitle)
        assertEquals(0x80112233.toInt(), ContentParser.parseColor("#80112233"))
        assertNull(ContentParser.parseColor("red"))
    }

    @Test
    fun sampleContentFileParses() {
        val file = listOf(File("../content/hideandseek.json"), File("content/hideandseek.json"))
            .firstOrNull { it.exists() } ?: return
        val sample = ContentParser.parse(file.readText())
        assertTrue(sample.creatures.isNotEmpty())
        assertTrue(sample.spots.size >= 4)
        assertTrue(sample.tips.isNotEmpty())
        val start = Day.parse("2026-10-17")!!
        for (i in 0 until 300) assertNotNull(DailyPlanner.plan(sample, start + i)) // endDate is 2027-08-31
        val newYear = DailyPlanner.plan(sample, Day.parse("2026-12-31")!!)!!
        assertEquals("snezhok", newYear.creature.id)
    }

    // ---- Planner ----

    @Test
    fun planIsDeterministic() {
        val day = Day.parse("2026-10-17")!!
        val first = DailyPlanner.plan(content, day)
        val again = DailyPlanner.plan(ContentParser.parse(contentJsonCopy()), day)
        assertNotNull(first)
        assertEquals(first, again)
    }

    @Test
    fun neverSameSpotTwoDaysInARow() {
        var previous: DailyPlan? = null
        val start = Day.parse("2026-01-01")!!
        val spotCounts = HashMap<String, Int>()
        for (i in 0 until 730) {
            val plan = DailyPlanner.plan(content, start + i)
            if (plan != null && previous != null && previous.day == plan.day - 1) {
                assertNotEquals("day ${plan.day}", previous.spot.id, plan.spot.id)
            }
            plan?.let { spotCounts[it.spot.id] = (spotCounts[it.spot.id] ?: 0) + 1 }
            previous = plan
        }
        assertEquals(4, spotCounts.size)
    }

    @Test
    fun specialDaysAndSchedule() {
        val ny = DailyPlanner.plan(content, Day.parse("2026-12-31")!!)!!
        assertEquals("b", ny.creature.id)
        assertEquals("s3", ny.spot.id)
        assertEquals("NY", ny.tip)
        assertNull(DailyPlanner.plan(content, Day.parse("2026-11-11")!!))
        assertNull(DailyPlanner.plan(content, Day.parse("2025-12-31")!!)) // before startDate
        assertTrue(content.schedule.allowsHour(7))
        assertFalse(content.schedule.allowsHour(22))
        assertTrue(Schedule(hoursFrom = 20, hoursTo = 2).allowsHour(1))
        assertFalse(Schedule(hoursFrom = 20, hoursTo = 2).allowsHour(12))

        val weekends = content.copy(schedule = Schedule(daysOfWeek = setOf(6, 7)))
        assertNotNull(DailyPlanner.plan(weekends, Day.parse("2026-10-17")!!)) // Saturday
        assertNull(DailyPlanner.plan(weekends, Day.parse("2026-10-19")!!)) // Monday
        assertEquals(
            Day.parse("2026-10-18"),
            DailyPlanner.previousHidingDay(weekends, Day.parse("2026-10-24")!!),
        )

        val everyThird = content.copy(schedule = Schedule(startDate = Day.parse("2026-10-01"), everyNDays = 3))
        assertNotNull(DailyPlanner.plan(everyThird, Day.parse("2026-10-04")!!))
        assertNull(DailyPlanner.plan(everyThird, Day.parse("2026-10-05")!!))

        val half = content.copy(schedule = Schedule(chancePercent = 50))
        val active = (0 until 1000).count { DailyPlanner.plan(half, Day(20_000 + it)) != null }
        assertTrue("active=$active", active in 430..570)

        assertNull(DailyPlanner.plan(content.copy(enabled = false), Day.parse("2026-10-17")!!))
    }

    @Test
    fun streakCountsHidingDays() {
        val weekends = content.copy(schedule = Schedule(daysOfWeek = setOf(6, 7)))
        val sat = Day.parse("2026-10-17")!!
        val sun = sat + 1
        val nextSat = sat + 7
        var p = Progress()
        p = p.found(sat, "a", DailyPlanner.previousHidingDay(weekends, sat))
        p = p.found(sun, "b", DailyPlanner.previousHidingDay(weekends, sun))
        assertEquals(2, p.streak)
        p = p.found(sun, "b", DailyPlanner.previousHidingDay(weekends, sun)) // same day twice
        assertEquals(2, p.totalFound)
        p = p.found(nextSat, "a", DailyPlanner.previousHidingDay(weekends, nextSat))
        assertEquals(3, p.streak) // Mon–Fri had no creature, the streak goes on
        p = p.found(nextSat + 7, "a", DailyPlanner.previousHidingDay(weekends, nextSat + 7))
        assertEquals(1, p.streak) // skipped a hiding day
        assertEquals(setOf("a", "b"), p.collection)
        assertTrue(p.isFound(nextSat + 7))
    }

    @Test
    fun texts() {
        assertEquals("1 день", TextTemplate.ruDays(1))
        assertEquals("3 дня", TextTemplate.ruDays(3))
        assertEquals("5 дней", TextTemplate.ruDays(5))
        assertEquals("12 дней", TextTemplate.ruDays(12))
        assertEquals("21 день", TextTemplate.ruDays(21))
        assertEquals("Hi Рыжик, 2", TextTemplate.fill("Hi {name}, {n}", mapOf("name" to "Рыжик", "n" to "2")))
    }

    private fun contentJsonCopy(): String =
        """
        {
          "version": 7, "salt": "test",
          "schedule": { "startDate": "2026-01-01" },
          "creatures": [
            { "id": "a", "name": "A", "color": "#FF0000", "weight": 3 },
            { "id": "b", "name": "B", "color": "#00FF00", "weight": 1 }
          ],
          "spots": [
            { "id": "s1", "weight": 3 }, { "id": "s2", "edge": "bottom", "align": 0.2 },
            { "id": "s3", "edge": "Right", "web": ".card" }, { "id": "s4", "weight": 2 }
          ],
          "tips": ["t1", { "text": "t2" }],
          "specialDays": [
            { "date": "2026-12-31", "creature": "b", "spot": "s3", "tip": "NY" },
            { "date": "2026-11-11", "skip": true }
          ]
        }
        """.trimIndent()
}
