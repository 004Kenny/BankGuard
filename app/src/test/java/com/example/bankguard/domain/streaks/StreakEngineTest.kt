package com.example.bankguard.domain.streaks

import com.example.bankguard.data.demo.DemoStreakHistory
import com.example.bankguard.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakEngineTest {

    private val safe = Money.of(120.00)
    private val overdrawn = Money.of(-15.00)

    /** Days 1..n from a list of lowest balances; the day after the last is in progress. */
    private fun summary(lows: List<Money?>): StreakSummary =
        StreakEngine.summarize(lows.withIndex().associate { (i, m) -> i + 1 to m }, lows.size + 1)

    private fun safeDays(n: Int): StreakSummary = summary(List(n) { safe })

    // ----- Daily stars -----

    @Test fun oneEligibleDayAwardsOneStar() {
        val s = summary(listOf(safe))
        assertEquals(1, s.totalStars)
        assertEquals(DayStatus.OverdraftFree, s.days.first().status)
    }

    @Test fun aDayCannotAwardMoreThanOneStar() {
        // The history is keyed by day, so re-recording a day replaces it rather than adding a star.
        val history = mutableMapOf<Int, Money?>(1 to safe)
        history[1] = Money.of(500.00)
        history[1] = safe
        assertEquals(1, StreakEngine.summarize(history, 2).totalStars)
    }

    @Test fun overdraftDayAwardsNoStar() {
        val s = summary(listOf(overdrawn))
        assertEquals(0, s.totalStars)
        assertEquals(DayStatus.Overdraft, s.days.first().status)
    }

    @Test fun unknownDayAwardsNoStar() {
        val s = summary(listOf(null))
        assertEquals(0, s.totalStars)
        assertEquals(DayStatus.Unknown, s.days.first().status)
    }

    @Test fun dayInProgressAndFutureDaysAwardNoStar() {
        // Day 3 is in progress and day 4 is in the future: neither counts, whatever their balance.
        val s = StreakEngine.summarize(mapOf(1 to safe, 2 to safe, 3 to safe, 4 to safe), 3)
        assertEquals(2, s.totalStars)
        assertEquals(DaySummary(3, DayStatus.InProgress), s.days.last())
        assertTrue(s.days.none { it.day == 4 })
    }

    @Test fun zeroBalanceIsOverdraftFree() {
        assertEquals(DayStatus.OverdraftFree, StreakEngine.statusOf(Money.Zero))
        assertEquals(DayStatus.Overdraft, StreakEngine.statusOf(Money(-1)))
        assertEquals(1, summary(listOf(Money.Zero)).totalStars)
    }

    @Test fun specExampleWeek() {
        // Mon safe, Tue $0, Wed overdraft, Thu safe, Fri no information.
        val s = summary(listOf(safe, Money.Zero, overdrawn, safe, null))
        assertEquals(3, s.totalStars)
        assertEquals(0, s.currentStreak)
    }

    // ----- Streaks -----

    @Test fun consecutiveSafeDaysIncreaseTheStreak() {
        assertEquals(1, summary(listOf(safe)).currentStreak)
        assertEquals(2, summary(listOf(safe, safe)).currentStreak)
        assertEquals(3, summary(listOf(safe, safe, safe)).currentStreak)
    }

    @Test fun overdraftBreaksTheStreakButKeepsStars() {
        // Spec example: Mon-Wed safe, Thu overdraft, Fri safe.
        val s = summary(listOf(safe, safe, safe, overdrawn, safe))
        assertEquals(1, s.currentStreak)
        assertEquals(4, s.totalStars)
        assertEquals(3, s.longestStreak)
    }

    @Test fun longestStreakIsKept() {
        val s = summary(listOf(safe, safe, safe, safe, overdrawn, safe, safe))
        assertEquals(4, s.longestStreak)
        assertEquals(2, s.currentStreak)
    }

    @Test fun missingDataDoesNotExtendAStreak() {
        // Day 3 has no record at all; it's Unknown and breaks the run.
        val s = StreakEngine.summarize(mapOf(1 to safe, 2 to safe, 4 to safe), 5)
        assertEquals(DayStatus.Unknown, s.days.single { it.day == 3 }.status)
        assertEquals(1, s.currentStreak)
        assertEquals(3, s.totalStars)
        assertEquals(1, summary(listOf(safe, null, safe)).currentStreak)
    }

    @Test fun dayInProgressDoesNotBreakTheStreak() {
        assertEquals(2, summary(listOf(safe, safe)).currentStreak)
    }

    // ----- Achievements -----

    @Test fun milestoneThresholdsAreCorrect() {
        assertEquals(
            listOf(3, 7, 14, 30, 60, 100),
            StreakBadge.entries.map { it.days },
        )
        assertEquals(listOf("🌱", "🥉", "🥈", "🥇", "💎", "👑"), StreakBadge.entries.map { it.emoji })
    }

    @Test fun eachBadgeUnlocksExactlyAtItsThreshold() {
        StreakBadge.entries.forEach { badge ->
            val justBefore = safeDays(badge.days - 1)
            val reached = safeDays(badge.days)
            assertFalse("${badge.name} unlocked early", justBefore.isUnlocked(badge))
            assertTrue("${badge.name} not unlocked", reached.isUnlocked(badge))
            assertEquals(badge.days, reached.unlocked.single { it.badge == badge }.unlockedOnDay)
        }
    }

    @Test fun threeAndSevenDayBadges() {
        assertTrue(summary(listOf(safe, safe, safe)).isUnlocked(StreakBadge.GettingStarted))
        assertFalse(safeDays(6).isUnlocked(StreakBadge.MoneyGuardian))
        assertTrue(safeDays(7).isUnlocked(StreakBadge.MoneyGuardian))
    }

    @Test fun badgesUnlockOnceAndStayAfterAReset() {
        // 3 safe, overdraft, 3 safe again: Getting Started is reached twice but unlocked once, on day 3.
        val s = summary(listOf(safe, safe, safe, overdrawn, safe, safe, safe))
        assertEquals(1, s.unlocked.count { it.badge == StreakBadge.GettingStarted })
        assertEquals(3, s.unlocked.single().unlockedOnDay)

        val afterBreak = summary(listOf(safe, safe, safe, overdrawn))
        assertEquals(0, afterBreak.currentStreak)
        assertTrue(afterBreak.isUnlocked(StreakBadge.GettingStarted))
    }

    @Test fun newlyUnlockedReportsOnlyNewBadges() {
        val before = safeDays(6)
        val after = safeDays(7)
        assertEquals(listOf(StreakBadge.MoneyGuardian), StreakEngine.newlyUnlocked(before, after))
        assertEquals(emptyList<StreakBadge>(), StreakEngine.newlyUnlocked(after, after))
    }

    @Test fun nextBadgeAndAllUnlocked() {
        assertEquals(StreakBadge.GettingStarted, summary(emptyList()).nextBadge)
        assertEquals(StreakBadge.MoneyGuardian, summary(listOf(safe, safe, safe)).nextBadge)
        assertNull(safeDays(100).nextBadge)
    }

    // ----- Demo history -----

    @Test fun demoStartsWithSixSafeDaysAndTheThreeDayBadge() {
        val s = StreakEngine.summarize(DemoStreakHistory.lowestBalances, DemoStreakHistory.START_DAY)
        assertEquals(6, s.totalStars)
        assertEquals(6, s.currentStreak)
        assertEquals(6, s.longestStreak)
        assertEquals(listOf(StreakBadge.GettingStarted), s.unlocked.map { it.badge })
        assertEquals(StreakBadge.MoneyGuardian, s.nextBadge)
        assertEquals(DayStatus.InProgress, s.recent(7).last().status)
        assertEquals(7, s.recent(7).size)
    }

    @Test fun lowerOfKeepsTheLowestObservation() {
        assertEquals(safe, StreakEngine.lowerOf(null, safe))
        assertEquals(overdrawn, StreakEngine.lowerOf(overdrawn, safe))
        assertEquals(overdrawn, StreakEngine.lowerOf(safe, overdrawn))
        assertEquals(overdrawn, StreakEngine.lowerOf(overdrawn, null))
    }
}
