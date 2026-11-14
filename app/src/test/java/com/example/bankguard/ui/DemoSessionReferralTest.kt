package com.example.bankguard.ui

import androidx.lifecycle.SavedStateHandle
import com.example.bankguard.data.demo.AccountScenario
import com.example.bankguard.data.demo.DemoAccountOptions
import com.example.bankguard.domain.savings.SavingsEstimator
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.Money
import com.example.bankguard.model.ReferralSource
import com.example.bankguard.model.ReferralStatus
import com.example.bankguard.ui.navigation.DiscoverRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemoSessionReferralTest {

    private val dispatcher = StandardTestDispatcher()
    private var now = 1_700_000_000_000L

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(state: SavedStateHandle = SavedStateHandle()) =
        DemoSessionViewModel(state, null, clock = { now })

    @Test fun everyDiscoverAccountCanStartAReferralForItsOwnBank() {
        val vm = vm()
        DemoAccountOptions.all.forEach { option ->
            val id = vm.startReferral(option.id)
            assertNotNull("no referral for ${option.id}", id)
            val referral = vm.referrals.value.getValue(id!!)
            assertEquals(option.id, referral.accountId)
            assertEquals(option.bankId, referral.bankId)
        }
        assertEquals(DemoAccountOptions.all.size, vm.referrals.value.size)
    }

    @Test fun everyDiscoverAccountHasItsOwnBankId() {
        val ids = DemoAccountOptions.all.map { it.bankId }
        assertTrue(ids.all { it.isNotBlank() })
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun newReferralIsInitiatedFromBankGuardWithATimestamp() {
        val vm = vm()
        val id = vm.startReferral("basic")!!
        val referral = vm.referrals.value.getValue(id)
        assertEquals("REF-BG-0001", referral.referralId)
        assertEquals(ReferralSource.BankGuard, referral.source)
        assertEquals(ReferralStatus.Initiated, referral.status)
        assertEquals(now, referral.createdAtMillis)
        assertNull(referral.continuedAtMillis)
    }

    @Test fun unknownAccountCreatesNoReferral() {
        val vm = vm()
        assertNull(vm.startReferral("not-a-real-account"))
        assertTrue(vm.referrals.value.isEmpty())
    }

    @Test fun continuingUpdatesOnlyThatReferral() {
        val vm = vm()
        val first = vm.startReferral("starter")!!
        val second = vm.startReferral("second-chance")!!
        assertNotEquals(first, second)
        now += 5_000
        vm.continueReferral(second)

        val referrals = vm.referrals.value
        assertEquals(ReferralStatus.Initiated, referrals.getValue(first).status)
        assertEquals(ReferralStatus.ContinuedToBank, referrals.getValue(second).status)
        assertEquals(now, referrals.getValue(second).continuedAtMillis)
        assertEquals("second-chance", referrals.getValue(second).accountId)
        assertEquals("riverbend-bank", referrals.getValue(second).bankId)
    }

    @Test fun continuingTwiceKeepsTheFirstTime() {
        val vm = vm()
        val id = vm.startReferral("starter")!!
        vm.continueReferral(id)
        val firstTime = vm.referrals.value.getValue(id).continuedAtMillis
        now += 60_000
        vm.continueReferral(id)
        assertEquals(firstTime, vm.referrals.value.getValue(id).continuedAtMillis)
    }

    @Test fun continuingAnUnknownReferralChangesNothing() {
        val vm = vm()
        vm.startReferral("starter")
        val before = vm.referrals.value
        vm.continueReferral("REF-BG-9999")
        assertEquals(before, vm.referrals.value)
    }

    @Test fun referralsSurviveProcessRecreation() {
        val state = SavedStateHandle()
        val vm = vm(state)
        val id = vm.startReferral("basic")!!
        vm.continueReferral(id)
        val restored = vm(state).referrals.value
        assertEquals(vm.referrals.value, restored)
    }

    @Test fun switchingPersonaKeepsReferralsAndResetClearsThem() {
        val vm = vm()
        vm.selectPersona(DemoPersona.Unbanked)
        vm.startReferral("starter")
        vm.selectPersona(DemoPersona.Underbanked)
        assertEquals(1, vm.referrals.value.size)
        vm.resetDemo()
        assertTrue(vm.referrals.value.isEmpty())
        assertEquals("REF-BG-0001", vm.startReferral("starter"))
    }

    @Test fun referralsLeavePreventAndRecoverStateAlone() = runTest(dispatcher) {
        val vm = vm()
        vm.selectPersona(DemoPersona.Underbanked)
        vm.setScenario(AccountScenario.PendingRelease)
        advanceUntilIdle()
        val profileBefore = vm.profile.value
        val cancellationsBefore = vm.cancellations.value
        val recordsBefore = vm.recoveryRecords.value

        val id = vm.startReferral("starter")!!
        vm.continueReferral(id)
        advanceUntilIdle()

        assertEquals(profileBefore, vm.profile.value)
        assertEquals(cancellationsBefore, vm.cancellations.value)
        assertEquals(recordsBefore, vm.recoveryRecords.value)
        assertEquals(AccountScenario.PendingRelease, vm.scenario.value)
    }

    @Test fun savingsEstimatesAreUnchangedByReferrals() {
        val vm = vm()
        val inputs = DemoAccountOptions.defaultInputs(DemoPersona.Underbanked)
        val account = com.example.bankguard.data.demo.DemoProfiles.underbanked.account
        val before = SavingsEstimator.rank(DemoAccountOptions.all, account, inputs)
        DemoAccountOptions.all.forEach { vm.startReferral(it.id)?.let(vm::continueReferral) }
        assertEquals(before, SavingsEstimator.rank(DemoAccountOptions.all, account, inputs))
        assertTrue(before.all { it.yearlyCost >= Money.Zero })
    }

    @Test fun openAccountRouteRoundTrips() {
        val route = DiscoverRoute.OpenAccount("REF-BG-0003")
        assertEquals("open:REF-BG-0003", route.key)
        assertEquals(route, DiscoverRoute.parse(route.key))
        assertEquals(DiscoverRoute.Compare("basic"), DiscoverRoute.parse("compare:basic"))
        assertEquals(DiscoverRoute.Calculator, DiscoverRoute.parse("calculator"))
    }
}
