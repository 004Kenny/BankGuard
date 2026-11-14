package com.example.bankguard.ui

import androidx.lifecycle.SavedStateHandle
import com.example.bankguard.data.demo.AccountScenario
import com.example.bankguard.data.demo.DemoStreakHistory
import com.example.bankguard.domain.forecast.CashFlowForecaster
import com.example.bankguard.domain.recovery.OverdraftDetector
import com.example.bankguard.domain.streaks.DayStatus
import com.example.bankguard.domain.streaks.StreakBadge
import com.example.bankguard.model.CancellationStatus
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.Money
import com.example.bankguard.model.RecoveryRequestStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemoSessionStreakTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(state: SavedStateHandle = SavedStateHandle()) =
        DemoSessionViewModel(state, null, feeCollector = { _, _ -> com.example.bankguard.model.FeeCollectionResult.Collected }, clock = { 1_700_000_000_000 })

    private fun TestScope.alex(vm: DemoSessionViewModel, scenario: AccountScenario = AccountScenario.UpcomingBill) {
        vm.selectPersona(DemoPersona.Underbanked)
        vm.setScenario(scenario)
        advanceUntilIdle()
    }

    private fun TestScope.completeDay(vm: DemoSessionViewModel) {
        vm.completeDemoDay()
        advanceUntilIdle()
    }

    @Test fun demoStartsAtSixDays() = runTest(dispatcher) {
        val vm = vm()
        alex(vm)
        val s = vm.streak.value
        assertEquals(6, s.currentStreak)
        assertEquals(6, s.totalStars)
        assertEquals(DemoStreakHistory.START_DAY, s.currentDay)
        assertEquals(listOf(StreakBadge.GettingStarted), s.unlocked.map { it.badge })
        assertFalse(s.isUnlocked(StreakBadge.MoneyGuardian))
    }

    @Test fun completingASafeDayReachesSevenAndUnlocksMoneyGuardian() = runTest(dispatcher) {
        val vm = vm()
        alex(vm)
        completeDay(vm)
        val s = vm.streak.value
        assertEquals(7, s.currentStreak)
        assertEquals(7, s.totalStars)
        assertEquals(8, s.currentDay)
        assertTrue(s.isUnlocked(StreakBadge.MoneyGuardian))
        assertEquals(StreakBadge.BalanceProtector, s.nextBadge)

        val result = vm.lastCompletedDay.value!!
        assertEquals(7, result.day)
        assertTrue(result.starAwarded)
        assertEquals(listOf(StreakBadge.MoneyGuardian), result.newBadges)
    }

    @Test fun nextDayDoesNotCelebrateTheSameBadgeAgain() = runTest(dispatcher) {
        val vm = vm()
        alex(vm)
        completeDay(vm)
        completeDay(vm)
        assertEquals(8, vm.streak.value.currentStreak)
        assertEquals(emptyList<StreakBadge>(), vm.lastCompletedDay.value!!.newBadges)
        assertEquals(1, vm.streak.value.unlocked.count { it.badge == StreakBadge.MoneyGuardian })
    }

    @Test fun predictedOverdraftThatPreventStopsDoesNotBreakTheStreak() = runTest(dispatcher) {
        val vm = vm()
        alex(vm)
        val profile = vm.profile.value!!
        // Prevent's story: a projected dip below $0 later this week.
        assertTrue(CashFlowForecaster.forecast(profile.availableBalance!!, profile.scheduled, 7).lowest.balance.isNegative)

        vm.requestCancellation("t-pending-shop")
        advanceUntilIdle()
        assertTrue(vm.cancellations.value["t-pending-shop"] is CancellationStatus.Approved)
        completeDay(vm)
        assertEquals(DayStatus.OverdraftFree, vm.lastCompletedDay.value!!.status)
        assertEquals(7, vm.streak.value.currentStreak)
    }

    @Test fun projectedDipAloneDoesNotBreakTheStreak() = runTest(dispatcher) {
        val vm = vm()
        alex(vm)
        completeDay(vm)
        assertEquals(DayStatus.OverdraftFree, vm.lastCompletedDay.value!!.status)
    }

    @Test fun actualOverdraftBreaksTheStreakButKeepsStarsAndBadges() = runTest(dispatcher) {
        val vm = vm()
        alex(vm, AccountScenario.PendingRelease) // available is -$50.00
        completeDay(vm)
        val s = vm.streak.value
        assertEquals(DayStatus.Overdraft, vm.lastCompletedDay.value!!.status)
        assertFalse(vm.lastCompletedDay.value!!.starAwarded)
        assertEquals(0, s.currentStreak)
        assertEquals(6, s.totalStars)
        assertEquals(6, s.longestStreak)
        assertTrue(s.isUnlocked(StreakBadge.GettingStarted))
    }

    @Test fun recoveringFundsDoesNotRestoreTheStreak() = runTest(dispatcher) {
        val vm = vm()
        alex(vm, AccountScenario.PendingRelease)
        val id = OverdraftDetector.find(vm.profile.value!!)!!.transaction.id
        vm.checkRecoveryEligibility(id)
        advanceUntilIdle()
        vm.requestRecovery(id, feeConsent = true)
        advanceUntilIdle()
        assertEquals(RecoveryRequestStatus.Confirmed, vm.recoveryRecords.value[id]!!.status)
        assertEquals(Money.of(95.00), vm.profile.value!!.availableBalance)

        completeDay(vm)
        assertEquals(DayStatus.Overdraft, vm.lastCompletedDay.value!!.status)
        assertEquals(0, vm.streak.value.currentStreak)

        // The next day starts at $95.00 and is overdraft-free again.
        completeDay(vm)
        assertEquals(DayStatus.OverdraftFree, vm.lastCompletedDay.value!!.status)
        assertEquals(1, vm.streak.value.currentStreak)
        assertEquals(7, vm.streak.value.totalStars)
    }

    @Test fun overdraftSeenEarlierInTheDayStillCountsAfterSwitchingBack() = runTest(dispatcher) {
        val vm = vm()
        alex(vm, AccountScenario.PostedReversal) // -$50.00
        alex(vm, AccountScenario.UpcomingBill)   // back to $142.90 later the same day
        completeDay(vm)
        assertEquals(DayStatus.Overdraft, vm.lastCompletedDay.value!!.status)
    }

    @Test fun unbankedPersonaHasNoStreakAndCannotCompleteADay() = runTest(dispatcher) {
        val vm = vm()
        vm.selectPersona(DemoPersona.Unbanked)
        advanceUntilIdle()
        val before = vm.streak.value
        vm.completeDemoDay()
        advanceUntilIdle()
        assertEquals(before, vm.streak.value)
        assertNull(vm.lastCompletedDay.value)
    }

    @Test fun switchingPersonaDoesNotMixStreakData() = runTest(dispatcher) {
        val vm = vm()
        alex(vm)
        completeDay(vm)
        val alexStreak = vm.streak.value
        vm.selectPersona(DemoPersona.Unbanked)
        advanceUntilIdle()
        vm.completeDemoDay()
        advanceUntilIdle()
        vm.selectPersona(DemoPersona.Underbanked)
        advanceUntilIdle()
        assertEquals(alexStreak, vm.streak.value)
    }

    @Test fun resetRestoresTheOriginalStreak() = runTest(dispatcher) {
        val vm = vm()
        alex(vm, AccountScenario.PendingRelease)
        completeDay(vm)
        completeDay(vm)
        vm.resetDemo()
        advanceUntilIdle()
        alex(vm)
        val s = vm.streak.value
        assertEquals(DemoStreakHistory.START_DAY, s.currentDay)
        assertEquals(6, s.currentStreak)
        assertEquals(6, s.totalStars)
        assertEquals(listOf(StreakBadge.GettingStarted), s.unlocked.map { it.badge })
        assertNull(vm.lastCompletedDay.value)
        // And the demo is repeatable.
        completeDay(vm)
        assertEquals(7, vm.streak.value.currentStreak)
    }

    @Test fun streakSurvivesProcessRecreationWithoutDuplicateStars() = runTest(dispatcher) {
        val state = SavedStateHandle()
        val vm = vm(state)
        alex(vm)
        completeDay(vm)
        val restored = vm(state)
        advanceUntilIdle()
        assertEquals(vm.streak.value, restored.streak.value)
        assertEquals(7, restored.streak.value.totalStars)
    }

    @Test fun overdraftOnTheDayInProgressSurvivesProcessRecreation() = runTest(dispatcher) {
        val state = SavedStateHandle()
        val vm = vm(state)
        alex(vm, AccountScenario.PendingRelease)
        val restored = vm(state)
        alex(restored, AccountScenario.UpcomingBill)
        completeDay(restored)
        assertEquals(DayStatus.Overdraft, restored.lastCompletedDay.value!!.status)
    }

    @Test fun dismissClearsTheResultOnly() = runTest(dispatcher) {
        val vm = vm()
        alex(vm)
        completeDay(vm)
        val s = vm.streak.value
        vm.dismissCompletedDay()
        assertNull(vm.lastCompletedDay.value)
        assertEquals(s, vm.streak.value)
    }
}
