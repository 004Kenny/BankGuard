package com.example.bankguard.ui

import androidx.lifecycle.SavedStateHandle
import com.example.bankguard.data.demo.AccountScenario
import com.example.bankguard.data.simulated.PartnerBankGateway
import com.example.bankguard.data.simulated.SimulatedBankMode
import com.example.bankguard.data.simulated.SimulatedPartnerBank
import com.example.bankguard.domain.forecast.CashFlowForecaster
import com.example.bankguard.domain.recovery.OverdraftDetector
import com.example.bankguard.domain.recovery.recoveryFeeId
import com.example.bankguard.domain.recovery.reversalCreditId
import com.example.bankguard.model.CancellationDecision
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.EligibilityCheck
import com.example.bankguard.model.FeeCollectionResult
import com.example.bankguard.model.FeeCollectionStatus
import com.example.bankguard.model.Money
import com.example.bankguard.model.RecoveryEligibility
import com.example.bankguard.model.RecoveryRequestStatus
import com.example.bankguard.model.RecoveryStatusUpdate
import com.example.bankguard.model.RecoverySubmission
import com.example.bankguard.model.Transaction
import com.example.bankguard.model.TransactionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemoSessionRecoveryTest {

    private lateinit var dispatcher: TestDispatcher

    @Before fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(
        state: SavedStateHandle = SavedStateHandle(),
        gateway: PartnerBankGateway? = null,
        feeResult: FeeCollectionResult = FeeCollectionResult.Collected,
    ) = DemoSessionViewModel(state, gateway, feeCollector = { _, _ -> feeResult }, clock = { 1_700_000_000_000 })

    /** Alex in [scenario], with the overdraft charge's eligibility already checked. */
    private fun kotlinx.coroutines.test.TestScope.ready(vm: DemoSessionViewModel, scenario: AccountScenario): String {
        vm.selectPersona(DemoPersona.Underbanked)
        vm.setScenario(scenario)
        advanceUntilIdle()
        val id = OverdraftDetector.find(vm.profile.value!!)!!.transaction.id
        vm.checkRecoveryEligibility(id)
        advanceUntilIdle()
        return id
    }

    @Test fun definingExampleEndsAtNinetyFive() = runTest(dispatcher) {
        val vm = vm()
        val id = ready(vm, AccountScenario.PendingRelease)
        assertEquals(Money.of(-50.00), vm.profile.value!!.availableBalance)

        vm.requestRecovery(id, feeConsent = true)
        dispatcher.scheduler.runCurrent()
        // Processing: nothing has changed yet.
        assertEquals(RecoveryRequestStatus.Submitted, vm.recoveryRecords.value[id]!!.status)
        assertEquals(Money.of(-50.00), vm.profile.value!!.availableBalance)

        advanceUntilIdle()
        val r = vm.recoveryRecords.value[id]!!
        assertEquals(RecoveryRequestStatus.Confirmed, r.status)
        assertEquals(Money.of(150.00), r.amountRestored)
        assertEquals(Money.of(5.00), r.quotedFee)
        assertEquals(FeeCollectionStatus.Collected, r.feeStatus)
        assertEquals(Money.of(95.00), r.balanceAfterRecovery)
        assertNotNull(r.confirmationReference)
        assertEquals(1_700_000_000_000, r.confirmedAtMillis)
        // Home, Prevent and Recover all read this one profile.
        assertEquals(Money.of(95.00), vm.profile.value!!.availableBalance)
    }

    @Test fun noRequestWithoutConsentOrEligibility() = runTest(dispatcher) {
        val vm = vm()
        vm.selectPersona(DemoPersona.Underbanked)
        vm.setScenario(AccountScenario.PendingRelease)
        advanceUntilIdle()
        vm.requestRecovery("t-ovd-a", feeConsent = true) // not checked yet
        assertTrue(vm.recoveryRecords.value.isEmpty())

        vm.checkRecoveryEligibility("t-ovd-a")
        advanceUntilIdle()
        vm.requestRecovery("t-ovd-a", feeConsent = false)
        assertTrue(vm.recoveryRecords.value.isEmpty())
    }

    @Test fun ineligibleChargeCannotBeSubmitted() = runTest(dispatcher) {
        val vm = vm()
        val id = ready(vm, AccountScenario.OutsideWindow)
        assertTrue((vm.eligibility.value[id] as EligibilityCheck.Done).result is RecoveryEligibility.Ineligible)
        vm.requestRecovery(id, feeConsent = true)
        advanceUntilIdle()
        assertTrue(vm.recoveryRecords.value.isEmpty())
        assertEquals(Money.of(-50.00), vm.profile.value!!.availableBalance)
    }

    @Test fun failedEligibilityCheckIsReported() = runTest(dispatcher) {
        val vm = vm()
        vm.setBankMode(SimulatedBankMode.Unavailable)
        val id = ready(vm, AccountScenario.PendingRelease)
        assertTrue(vm.eligibility.value[id] is EligibilityCheck.Failed)
    }

    @Test fun rejectedRecoveryChangesNothingAndChargesNothing() = runTest(dispatcher) {
        val vm = vm()
        val id = ready(vm, AccountScenario.BankRejects)
        vm.requestRecovery(id, feeConsent = true)
        advanceUntilIdle()
        val r = vm.recoveryRecords.value[id]!!
        assertEquals(RecoveryRequestStatus.Rejected, r.status)
        assertNotNull(r.rejectionReason)
        assertEquals(Money.Zero, r.amountRestored)
        assertEquals(Money.Zero, r.feeAssessed)
        assertEquals(FeeCollectionStatus.NotCharged, r.feeStatus)
        assertEquals(Money.of(-50.00), vm.profile.value!!.availableBalance)
    }

    @Test fun pendingRecoveryChangesNothingAndChargesNothing() = runTest(dispatcher) {
        val vm = vm()
        val id = ready(vm, AccountScenario.AwaitingConfirmation)
        vm.requestRecovery(id, feeConsent = true)
        advanceUntilIdle()
        vm.refreshRecoveryStatus(id)
        advanceUntilIdle()
        val r = vm.recoveryRecords.value[id]!!
        assertEquals(RecoveryRequestStatus.AwaitingConfirmation, r.status)
        assertNotNull(r.requestReference)
        assertEquals(FeeCollectionStatus.NotCharged, r.feeStatus)
        assertEquals(Money.of(-50.00), vm.profile.value!!.availableBalance)
    }

    @Test fun duplicateConfirmationsNeverDoubleCreditOrDoubleCharge() = runTest(dispatcher) {
        var feeCalls = 0
        val bank = ConfirmLaterBank()
        val vm = DemoSessionViewModel(SavedStateHandle(), bank, feeCollector = { _, _ -> feeCalls++; FeeCollectionResult.Collected })
        val id = ready(vm, AccountScenario.PostedReversal)
        vm.requestRecovery(id, feeConsent = true)
        advanceUntilIdle()
        assertEquals(RecoveryRequestStatus.AwaitingConfirmation, vm.recoveryRecords.value[id]!!.status)

        bank.confirm = true
        vm.refreshRecoveryStatus(id)
        vm.refreshRecoveryStatus(id) // ignored: a check is already running
        advanceUntilIdle()
        vm.refreshRecoveryStatus(id) // ignored: already confirmed
        vm.requestRecovery(id, feeConsent = true) // ignored: request exists
        advanceUntilIdle()

        val profile = vm.profile.value!!
        assertEquals(1, feeCalls)
        assertEquals(1, profile.transactions.count { it.id == reversalCreditId(id) })
        assertEquals(1, profile.transactions.count { it.id == recoveryFeeId(id) })
        assertEquals(Money.of(95.00), profile.availableBalance)
    }

    @Test fun uncollectableFeeIsRecordedHonestly() = runTest(dispatcher) {
        val vm = vm(feeResult = FeeCollectionResult.Failed("Simulated collection failure."))
        val id = ready(vm, AccountScenario.PendingRelease)
        vm.requestRecovery(id, feeConsent = true)
        advanceUntilIdle()
        val r = vm.recoveryRecords.value[id]!!
        assertEquals(RecoveryRequestStatus.Confirmed, r.status)
        assertEquals(FeeCollectionStatus.CollectionFailed, r.feeStatus)
        assertEquals(Money.of(5.00), r.feeAssessed)
        assertEquals(Money.Zero, r.feeCollected)
        assertEquals(Money.of(100.00), vm.profile.value!!.availableBalance)
    }

    @Test fun historyKeepsTheOriginalTransaction() = runTest(dispatcher) {
        val vm = vm()
        val id = ready(vm, AccountScenario.PendingRelease)
        vm.requestRecovery(id, feeConsent = true)
        advanceUntilIdle()
        val original = vm.profile.value!!.transactions.first { it.id == id }
        assertEquals(TransactionStatus.Released, original.status)
        assertEquals(Money.of(-150.00), original.amount)
        val r = vm.recoveryRecords.value[id]!!
        assertEquals("ElectroMart Online (sample)", r.merchant)
        assertEquals(TransactionStatus.Pending, r.originalStatus)
        assertEquals(Money.of(-150.00), r.transactionAmount)
    }

    @Test fun forecastAndOverdraftStatusFollowTheRecovery() = runTest(dispatcher) {
        val vm = vm()
        val id = ready(vm, AccountScenario.PendingRelease)
        assertNotNull(OverdraftDetector.find(vm.profile.value!!))
        vm.requestRecovery(id, feeConsent = true)
        advanceUntilIdle()
        val p = vm.profile.value!!
        assertNull(OverdraftDetector.find(p))
        val forecast = CashFlowForecaster.forecast(p.availableBalance!!, p.scheduled)
        assertEquals(Money.of(95.00), forecast.days.first().balance)
        assertEquals(Money.of(-90.00), forecast.lowest.balance) // the $185 bill tomorrow
    }

    @Test fun recordsSurviveProcessRecreation() = runTest(dispatcher) {
        val state = SavedStateHandle()
        val vm = vm(state)
        val id = ready(vm, AccountScenario.PostedReversal)
        vm.requestRecovery(id, feeConsent = true)
        advanceUntilIdle()

        val restored = vm(state)
        advanceUntilIdle()
        assertEquals(vm.recoveryRecords.value, restored.recoveryRecords.value)
        assertEquals(AccountScenario.PostedReversal, restored.scenario.value)
        assertEquals(Money.of(95.00), restored.profile.value!!.availableBalance)
    }

    @Test fun scenariosAndPersonasDoNotMixRecords() = runTest(dispatcher) {
        val vm = vm()
        val id = ready(vm, AccountScenario.PendingRelease)
        vm.requestRecovery(id, feeConsent = true)
        advanceUntilIdle()

        vm.setScenario(AccountScenario.PostedReversal)
        advanceUntilIdle()
        assertEquals(Money.of(-50.00), vm.profile.value!!.availableBalance)

        vm.selectPersona(DemoPersona.Unbanked)
        advanceUntilIdle()
        assertNull(vm.profile.value!!.availableBalance)
        assertTrue(vm.profile.value!!.transactions.isEmpty())

        vm.selectPersona(DemoPersona.Underbanked)
        vm.setScenario(AccountScenario.PendingRelease)
        advanceUntilIdle()
        assertEquals(Money.of(95.00), vm.profile.value!!.availableBalance)
    }

    @Test fun defaultPreventStoryIsUntouched() = runTest(dispatcher) {
        val vm = vm()
        vm.selectPersona(DemoPersona.Underbanked)
        advanceUntilIdle()
        assertEquals(AccountScenario.UpcomingBill, vm.scenario.value)
        assertEquals(Money.of(142.90), vm.profile.value!!.availableBalance)
        assertNull(OverdraftDetector.find(vm.profile.value!!))
    }

    @Test fun resetRestoresTheOriginalDemoState() = runTest(dispatcher) {
        val vm = vm()
        val id = ready(vm, AccountScenario.PendingRelease)
        vm.requestRecovery(id, feeConsent = true)
        advanceUntilIdle()

        vm.resetDemo()
        advanceUntilIdle()
        assertTrue(vm.recoveryRecords.value.isEmpty())
        assertTrue(vm.eligibility.value.isEmpty())
        assertEquals(AccountScenario.UpcomingBill, vm.scenario.value)
        vm.selectPersona(DemoPersona.Underbanked)
        advanceUntilIdle()
        assertEquals(Money.of(142.90), vm.profile.value!!.availableBalance)
    }

    /** Accepts requests, then confirms only once [confirm] is set; answers instantly. */
    private class ConfirmLaterBank : PartnerBankGateway {
        private val real = SimulatedPartnerBank(responseDelayMillis = 0)
        var confirm = false
        override suspend fun requestCancellation(transaction: Transaction): CancellationDecision = real.requestCancellation(transaction)
        override suspend fun checkRecoveryEligibility(transaction: Transaction) = real.checkRecoveryEligibility(transaction)
        override suspend fun submitRecovery(transaction: Transaction): RecoverySubmission = RecoverySubmission.Accepted("REQ-TEST")
        override suspend fun recoveryStatus(transaction: Transaction, requestReference: String): RecoveryStatusUpdate =
            if (confirm) RecoveryStatusUpdate.Confirmed("SIM-TEST", transaction.amount.absolute) else RecoveryStatusUpdate.AwaitingConfirmation
    }
}
