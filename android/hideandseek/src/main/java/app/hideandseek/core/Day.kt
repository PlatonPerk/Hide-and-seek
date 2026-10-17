package app.hideandseek.core

/**
 * A calendar day counted from 1970-01-01. Pure arithmetic, no java.time,
 * so the same code works on any Android version and ports to iOS as is.
 */
data class Day(val epochDay: Int) : Comparable<Day> {

    operator fun plus(days: Int): Day = Day(epochDay + days)

    operator fun minus(days: Int): Day = Day(epochDay - days)

    override fun compareTo(other: Day): Int = epochDay.compareTo(other.epochDay)

    /** ISO day of week: 1 = Monday … 7 = Sunday (1970-01-01 was a Thursday). */
    val dayOfWeek: Int get() = (epochDay + 3).mod(7) + 1

    /** "YYYY-MM-DD" — also used in seeds, so keep this format stable. */
    override fun toString(): String {
        val (y, m, d) = civilFromDays(epochDay)
        return "${y.toString().padStart(4, '0')}-${m.pad2()}-${d.pad2()}"
    }

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L
        private const val MILLIS_PER_HOUR = 3_600_000L

        fun of(year: Int, month: Int, day: Int): Day = Day(daysFromCivil(year, month, day))

        /** Parses "YYYY-MM-DD"; returns null for anything else. */
        fun parse(text: String?): Day? {
            val parts = text?.trim()?.split('-') ?: return null
            if (parts.size != 3) return null
            val y = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val d = parts[2].toIntOrNull() ?: return null
            if (m !in 1..12 || d !in 1..31) return null
            return of(y, m, d)
        }

        /** The calendar day at [utcMillis] in a zone that is [offsetMillis] ahead of UTC. */
        fun at(utcMillis: Long, offsetMillis: Int): Day =
            Day((utcMillis + offsetMillis).floorDiv(MILLIS_PER_DAY).toInt())

        /** Hour of day (0..23) at [utcMillis] in that zone. */
        fun hourAt(utcMillis: Long, offsetMillis: Int): Int =
            ((utcMillis + offsetMillis).mod(MILLIS_PER_DAY) / MILLIS_PER_HOUR).toInt()
    }
}

// Howard Hinnant's civil-date algorithms (public domain).
private fun daysFromCivil(year: Int, month: Int, day: Int): Int {
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val mp = if (month > 2) month - 3 else month + 9
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

private fun civilFromDays(epochDay: Int): Triple<Int, Int, Int> {
    val z = epochDay + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    return Triple(if (m <= 2) y + 1 else y, m, d)
}

private fun Int.pad2(): String = toString().padStart(2, '0')
