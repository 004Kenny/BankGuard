package com.example.bankguard.domain.streaks

import com.example.bankguard.model.Money

// Overdraft-free streaks: one star per completed day whose available balance never went below
// $0.00. Days are numbered on a simulated demo calendar (1, 2, 3, ...); nothing here reads the
// device clock, so every result is deterministic.

/** How one simulated calendar day counts toward the streak. */
enum class DayStatus {
    /** Completed, and the available balance never went below $0.00. Earns one star. */
    OverdraftFree,

    /** Completed, and the available balance went below $0.00 at some point. No star; breaks the streak. */
    Overdraft,

    /** Completed, but there's no verified balance information. No star; breaks the streak. */
    Unknown,

    /** The current day, not finished yet. Neither earns nor breaks anything. */
    InProgress,
}

/**
 * What's known about one completed day: the lowest available balance seen during it,
 * or null when there's no verified balance information.
 */
data class DayBalance(val day: Int, val lowestAvailable: Money?)

/** Streak milestones. Each unlocks once, the first time the current streak reaches [days]. */
enum class StreakBadge(val days: Int, val emoji: String) {
    GettingStarted(3, "🌱"),
    MoneyGuardian(7, "🥉"),
    BalanceProtector(14, "🥈"),
    OverdraftChampion(30, "🥇"),
    FinancialGuardian(60, "💎"),
    BankGuardLegend(100, "👑"),
}

data class UnlockedBadge(val badge: StreakBadge, val unlockedOnDay: Int)

data class DaySummary(val day: Int, val status: DayStatus)

data class StreakSummary(
    /** The simulated day in progress. */
    val currentDay: Int,
    /** Every day from the first recorded one up to and including [currentDay]. */
    val days: List<DaySummary>,
    val totalStars: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    /** In unlock order. Never shrinks as days are added. */
    val unlocked: List<UnlockedBadge>,
) {
    val nextBadge: StreakBadge? get() = StreakBadge.entries.firstOrNull { b -> unlocked.none { it.badge == b } }

    fun isUnlocked(badge: StreakBadge): Boolean = unlocked.any { it.badge == badge }

    /** The last [count] days, ending with the day in progress. */
    fun recent(count: Int = 7): List<DaySummary> = days.takeLast(count)
}

object StreakEngine {

    /** A completed day's status from its lowest available balance. $0.00 counts as overdraft-free. */
    fun statusOf(lowestAvailable: Money?): DayStatus = when {
        lowestAvailable == null -> DayStatus.Unknown
        lowestAvailable.isNegative -> DayStatus.Overdraft
        else -> DayStatus.OverdraftFree
    }

    /**
     * Summarizes a history of completed days. Only days before [currentDay] count: the current
     * day is in progress and future days haven't happened. A day missing from [history] is
     * Unknown. Each day appears once (it's a map), so it can earn at most one star.
     */
    fun summarize(history: Map<Int, Money?>, currentDay: Int): StreakSummary {
        val completed = history.keys.filter { it < currentDay }
        val firstDay = completed.minOrNull() ?: currentDay
        val days = (firstDay until currentDay).map { day ->
            DaySummary(day, if (day in history) statusOf(history[day]) else DayStatus.Unknown)
        } + DaySummary(currentDay, DayStatus.InProgress)

        var run = 0
        var longest = 0
        var stars = 0
        val unlocked = mutableListOf<UnlockedBadge>()
        days.forEach { d ->
            when (d.status) {
                DayStatus.OverdraftFree -> {
                    stars++
                    run++
                    longest = maxOf(longest, run)
                    StreakBadge.entries
                        .filter { it.days <= run && unlocked.none { u -> u.badge == it } }
                        .forEach { unlocked += UnlockedBadge(it, d.day) }
                }
                DayStatus.Overdraft, DayStatus.Unknown -> run = 0
                DayStatus.InProgress -> Unit
            }
        }
        return StreakSummary(
            currentDay = currentDay,
            days = days,
            totalStars = stars,
            currentStreak = run,
            longestStreak = longest,
            unlocked = unlocked,
        )
    }

    /** Badges in [after] that weren't in [before]: what to celebrate after a day completes. */
    fun newlyUnlocked(before: StreakSummary, after: StreakSummary): List<StreakBadge> =
        after.unlocked.map { it.badge }.filterNot(before::isUnlocked)

    /** Running lowest balance for the day in progress: the lower of what was seen before and now. */
    fun lowerOf(previous: Money?, observed: Money?): Money? = when {
        previous == null -> observed
        observed == null -> previous
        else -> minOf(previous, observed)
    }
}
