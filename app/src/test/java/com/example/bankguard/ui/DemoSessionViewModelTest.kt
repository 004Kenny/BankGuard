package com.example.bankguard.ui

import androidx.lifecycle.SavedStateHandle
import com.example.bankguard.data.demo.DemoAccountOptions
import com.example.bankguard.data.simulated.SimulatedBankMode
import com.example.bankguard.data.simulated.SimulatedPartnerBank
import com.example.bankguard.model.CancellationStatus
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.FeeRecovery
import com.example.bankguard.model.RecoveryStage
import com.example.bankguard.model.WaiverReason
import com.example.bankguard.model.Money
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemoSessionViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    @Before fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(state: SavedStateHandle = SavedStateHandle()) = DemoSessionViewModel(state)

    @Test fun startsWithNoPersona() = runTest(dispatcher) {
        val vm = vm()
        assertNull(vm.persona.value)
        assertNull(vm.profile.value)
    }

    @Test fun selectingPersonaLoadsItsProfile() = runTest(dispatcher) {
        val vm = vm()
        vm.selectPersona(DemoPersona.Unbanked)
        advanceUntilIdle()
        assertEquals(DemoPersona.Unbanked, vm.persona.value)
        assertEquals("Jordan", vm.profile.value?.firstName)
    }

    @Test fun restoresPersonaFromSavedState() = runTest(dispatcher) {
        val vm = vm(SavedStateHandle(mapOf("demo_persona" to "Underbanked")))
        assertEquals(DemoPersona.Underbanked, vm.persona.value)
        assertEquals("Alex", vm.profile.value?.firstName)
    }

    @Test fun approvedCancellationUpdatesBalanceOnlyAfterTheBankAnswers() = runTest(dispatcher) {
        val vm = vm()
        vm.selectPersona(DemoPersona.Underbanked)
        advanceUntilIdle()

        vm.requestCancellation("t-pending-shop")
        dispatcher.scheduler.runCurrent()
        assertEquals(CancellationStatus.Processing, vm.cancellations.value["t-pending-shop"])
        assertEquals(Money.of(142.90), vm.profile.value?.availableBalance)

        advanceUntilIdle()
        assertTrue(vm.cancellations.value["t-pending-shop"] is CancellationStatus.Approved)
        assertEquals(Money.of(201.30), vm.profile.value?.availableBalance)
    }

    @Test fun declinedCancellationLeavesBalanceUnchanged() = runTest(dispatcher) {
        val vm = vm()
        vm.selectPersona(DemoPersona.Underbanked)
        vm.setBankMode(SimulatedBankMode.DeclineAll)
        advanceUntilIdle()

        vm.requestCancellation("t-pending-shop")
        advanceUntilIdle()
        assertEquals(
            CancellationStatus.Declined(SimulatedPartnerBank.REASON_MERCHANT_PROCESSING),
            vm.cancellations.value["t-pending-shop"],
        )
        assertEquals(Money.of(142.90), vm.profile.value?.availableBalance)
    }

    @Test fun ignoresRepeatAndUnknownRequests() = runTest(dispatcher) {
        val vm = vm()
        vm.selectPersona(DemoPersona.Underbanked)
        advanceUntilIdle()
        vm.requestCancellation("does-not-exist")
        vm.requestCancellation("t-grocery") // posted, not pending
        assertTrue(vm.cancellations.value.isEmpty())

        vm.requestCancellation("t-pending-shop")
        advanceUntilIdle()
        val first = vm.cancellations.value["t-pending-shop"]
        vm.requestCancellation("t-pending-shop")
        advanceUntilIdle()
        assertEquals(first, vm.cancellations.value["t-pending-shop"])
    }

    @Test fun resultsSurviveProcessRecreation() = runTest(dispatcher) {
        val state = SavedStateHandle()
        val vm = vm(state)
        vm.selectPersona(DemoPersona.Underbanked)
        advanceUntilIdle()
        vm.requestCancellation("t-pending-shop")
        advanceUntilIdle()

        val restored = vm(state)
        advanceUntilIdle()
        assertTrue(restored.cancellations.value["t-pending-shop"] is CancellationStatus.Approved)
        assertEquals(Money.of(201.30), restored.profile.value?.availableBalance)
    }

    @Test fun resetClearsEverything() = runTest(dispatcher) {
        val vm = vm()
        vm.selectPersona(DemoPersona.Underbanked)
        vm.setBankMode(SimulatedBankMode.DeclineAll)
        advanceUntilIdle()
        vm.requestCancellation("t-pending-shop")
        advanceUntilIdle()

        vm.resetDemo()
        advanceUntilIdle()
        assertNull(vm.persona.value)
        assertTrue(vm.cancellations.value.isEmpty())
        assertEquals(SimulatedBankMode.ApproveEligible, vm.bankMode.value)
    }

    @Test fun recoveryMovesForwardOnlyInOrder() = runTest(dispatcher) {
        val vm = vm()
        // Can't mark sent or record a decision before a letter exists.
        vm.markSent("f-overdraft")
        vm.recordDecision("f-overdraft", refunded = true)
        assertEquals(RecoveryStage.Found, vm.recoveries.value["f-overdraft"]?.stage ?: RecoveryStage.Found)

        vm.saveLetter("f-overdraft", "Dear bank", setOf(WaiverReason.Hardship))
        assertEquals(RecoveryStage.Drafted, vm.recoveries.value.getValue("f-overdraft").stage)

        vm.recordDecision("f-overdraft", refunded = true) // not sent yet: ignored
        assertEquals(RecoveryStage.Drafted, vm.recoveries.value.getValue("f-overdraft").stage)

        vm.markSent("f-overdraft")
        vm.saveLetter("f-overdraft", "Dear bank, edited", emptySet()) // editing doesn't move it back
        assertEquals(RecoveryStage.Sent, vm.recoveries.value.getValue("f-overdraft").stage)

        vm.recordDecision("f-overdraft", refunded = false)
        val r = vm.recoveries.value.getValue("f-overdraft")
        assertEquals(RecoveryStage.NotRefunded, r.stage)
        assertEquals("Dear bank, edited", r.letter)
    }

    @Test fun recoverySurvivesProcessRecreationAndResetClearsIt() = runTest(dispatcher) {
        val state = SavedStateHandle()
        val vm = vm(state)
        vm.saveLetter("f-atm", "Letter text", setOf(WaiverReason.RarelyCharged, WaiverReason.LongTimeCustomer))
        vm.markSent("f-atm")

        val restored = vm(state)
        assertEquals(
            FeeRecovery(RecoveryStage.Sent, "Letter text", setOf(WaiverReason.RarelyCharged, WaiverReason.LongTimeCustomer)),
            restored.recoveries.value["f-atm"],
        )

        restored.resetDemo()
        assertTrue(restored.recoveries.value.isEmpty())
        assertTrue(vm(state).recoveries.value.isEmpty())
    }

    @Test fun savingsInputsDefaultPerPersonaAndPersistUntilReset() = runTest(dispatcher) {
        val state = SavedStateHandle()
        val vm = vm(state)
        assertEquals(DemoAccountOptions.defaultInputs(DemoPersona.Underbanked), vm.inputsFor(DemoPersona.Underbanked))

        val custom = DemoAccountOptions.defaultInputs(DemoPersona.Underbanked).copy(overdraftsPerYear = 9, hasDirectDeposit = false)
        vm.setSavingsInputs(DemoPersona.Underbanked, custom)
        assertEquals(custom, vm(state).inputsFor(DemoPersona.Underbanked))
        assertEquals(DemoAccountOptions.defaultInputs(DemoPersona.Unbanked), vm(state).inputsFor(DemoPersona.Unbanked))

        vm.resetDemo()
        assertEquals(DemoAccountOptions.defaultInputs(DemoPersona.Underbanked), vm(state).inputsFor(DemoPersona.Underbanked))
    }
}
