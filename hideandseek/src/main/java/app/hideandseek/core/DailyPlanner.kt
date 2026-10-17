package app.hideandseek.core

/** Who hides today, where, and which tip they bring. */
data class DailyPlan(
    val day: Day,
    val creature: CreatureDef,
    val spot: SpotDef,
    val tip: String?,
)

/**
 * Picks today's creature and spot from the content alone (no server, no network
 * at decision time). Every device with the same content gets the same answer,
 * so kids can tell each other where the creature is today.
 */
object DailyPlanner {

    /** How many days back the "never the same spot twice in a row" chain starts. */
    private const val CHAIN_DAYS = 30

    fun plan(content: HideAndSeekContent, day: Day): DailyPlan? {
        if (!isHidingDay(content, day)) return null
        val special = content.specialDay(day)
        val creature = special?.creatureId?.let(content::creature)
            ?: Seeded.pickWeighted(content.creaturesOn(day), seed(content, day, "creature")) { it.weight }
            ?: return null
        val spot = spotOn(content, day) ?: return null
        val tip = special?.tip
            ?: Seeded.pickUniform(content.tipsOn(day), seed(content, day, "tip"))?.text
        return DailyPlan(day, creature, spot, tip)
    }

    fun isHidingDay(content: HideAndSeekContent, day: Day): Boolean {
        if (!content.enabled) return false
        val special = content.specialDay(day)
        if (special != null) {
            if (special.skip) return false
        } else if (!content.schedule.allowsDay(day, content.salt)) {
            return false
        }
        val hasCreature = special?.creatureId?.let(content::creature) != null ||
            content.creaturesOn(day).isNotEmpty()
        val hasSpot = special?.spotId?.let(content::spot) != null ||
            content.spotsOn(day).isNotEmpty()
        return hasCreature && hasSpot
    }

    /** The last hiding day before [day], used to keep streaks fair on non-daily schedules. */
    fun previousHidingDay(content: HideAndSeekContent, day: Day, maxDaysBack: Int = 60): Day? {
        for (back in 1..maxDaysBack) {
            val candidate = day - back
            if (isHidingDay(content, candidate)) return candidate
        }
        return null
    }

    private fun spotOn(content: HideAndSeekContent, day: Day): SpotDef? {
        var previous: SpotDef? = null
        var current: SpotDef? = null
        for (back in CHAIN_DAYS downTo 0) {
            val d = day - back
            current = if (isHidingDay(content, d)) spotOfDay(content, d, previous) else null
            previous = current
        }
        return current
    }

    private fun spotOfDay(content: HideAndSeekContent, day: Day, previous: SpotDef?): SpotDef? {
        content.specialDay(day)?.spotId?.let(content::spot)?.let { return it }
        val spots = content.spotsOn(day)
        val pick = Seeded.pickWeighted(spots, seed(content, day, "spot")) { it.weight } ?: return null
        if (!content.schedule.avoidRepeat || previous == null || pick.id != previous.id) return pick
        val others = spots.filter { it.id != previous.id }
        return Seeded.pickWeighted(others, seed(content, day, "spot-again")) { it.weight } ?: pick
    }

    private fun seed(content: HideAndSeekContent, day: Day, what: String): String =
        "${content.salt}|$day|$what"
}

/** What the kid has found so far. Lives on the device only. */
data class Progress(
    val lastFoundDay: Day? = null,
    val streak: Int = 0,
    val totalFound: Int = 0,
    /** Ids of every creature ever found — the collection. */
    val collection: Set<String> = emptySet(),
) {
    fun isFound(day: Day): Boolean = lastFoundDay == day

    /** [previousHidingDay]: the last day before [day] the creature was hiding, if any. */
    fun found(day: Day, creatureId: String, previousHidingDay: Day?): Progress {
        if (lastFoundDay == day) return this
        val continues = lastFoundDay != null && lastFoundDay == previousHidingDay
        return Progress(
            lastFoundDay = day,
            streak = if (continues) streak + 1 else 1,
            totalFound = totalFound + 1,
            collection = collection + creatureId,
        )
    }
}
