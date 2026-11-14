package com.example.bankguard.data.simulated

import com.example.bankguard.data.demo.AccountScenario
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.domain.recovery.OverdraftDetector
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.Money
import com.example.bankguard.model.RecoveryEligibility
import com.example.bankguard.model.RecoveryMechanism
import com.example.bankguard.model.RecoveryStatusUpdate
import com.example.bankguard.model.RecoverySubmission
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulatedPartnerBankRecoveryTest {

    private val bank = SimulatedPartnerBank(responseDelayMillis = 0)
    private fun charge(s: AccountScenario) =
        OverdraftDetector.find(DemoProfiles.forScenario(DemoPersona.Underbanked, s))!!.transaction

    @Test fun pendingReleaseIsEligibleAndConfirms() = runTest {
        val t = charge(AccountScenario.PendingRelease)
        val e = bank.checkRecoveryEligibility(t) as RecoveryEligibility.Eligible
        assertEquals(RecoveryMechanism.AuthorizationHoldRelease, e.mechanism)
        val accepted = bank.submitRecovery(t) as RecoverySubmission.Accepted
        val status = bank.recoveryStatus(t, accepted.requestReference) as RecoveryStatusUpdate.Confirmed
        assertEquals(Money.of(150.00), status.amountRestored)
        assertTrue(status.confirmationReference.startsWith("SIM-"))
    }

    @Test fun postedReversalNamesItsMechanism() = runTest {
        val t = charge(AccountScenario.PostedReversal)
        val e = bank.checkRecoveryEligibility(t) as RecoveryEligibility.Eligible
        assertEquals(RecoveryMechanism.PostedTransactionReversal, e.mechanism)
        assertTrue(bank.submitRecovery(t) is RecoverySubmission.Accepted)
    }

    @Test fun outsideWindowIsIneligibleWithAReason() = runTest {
        val e = bank.checkRecoveryEligibility(charge(AccountScenario.OutsideWindow))
        assertTrue(e is RecoveryEligibility.Ineligible && e.reason.isNotBlank())
    }

    @Test fun rejectedScenarioIsEligibleButRejectedOnSubmit() = runTest {
        val t = charge(AccountScenario.BankRejects)
        assertTrue(bank.checkRecoveryEligibility(t) is RecoveryEligibility.Eligible)
        assertTrue(bank.submitRecovery(t) is RecoverySubmission.Rejected)
    }

    @Test fun awaitingScenarioStaysPending() = runTest {
        val t = charge(AccountScenario.AwaitingConfirmation)
        val accepted = bank.submitRecovery(t) as RecoverySubmission.Accepted
        repeat(3) { assertEquals(RecoveryStatusUpdate.AwaitingConfirmation, bank.recoveryStatus(t, accepted.requestReference)) }
    }

    @Test fun chargeWithoutARuleIsIneligible() = runTest {
        val grocery = DemoProfiles.underbanked.transactions.first { it.id == "t-grocery" }
        assertEquals(
            RecoveryEligibility.Ineligible(SimulatedPartnerBank.REASON_NO_RECOVERY_PATH),
            bank.checkRecoveryEligibility(grocery),
        )
    }

    @Test fun declineAllRejectsEverySubmission() = runTest {
        val declining = SimulatedPartnerBank(mode = { SimulatedBankMode.DeclineAll }, responseDelayMillis = 0)
        assertTrue(declining.submitRecovery(charge(AccountScenario.PendingRelease)) is RecoverySubmission.Rejected)
    }

    @Test(expected = SimulatedPartnerBank.BankUnavailableException::class)
    fun unavailableBankFailsEligibilityChecks() = runTest {
        SimulatedPartnerBank(mode = { SimulatedBankMode.Unavailable }, responseDelayMillis = 0)
            .checkRecoveryEligibility(charge(AccountScenario.PendingRelease))
    }

    @Test fun answersAreDeterministic() = runTest {
        val t = charge(AccountScenario.PendingRelease)
        val a = bank.submitRecovery(t)
        val b = bank.submitRecovery(t)
        assertEquals(a, b)
    }
}
