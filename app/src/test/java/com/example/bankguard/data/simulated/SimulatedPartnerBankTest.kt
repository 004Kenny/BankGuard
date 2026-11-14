package com.example.bankguard.data.simulated

import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.model.CancellationDecision
import com.example.bankguard.model.CancellationEligibility
import com.example.bankguard.model.TransactionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimulatedPartnerBankTest {

    private val pending = DemoProfiles.underbanked.pendingTransactions
    private val eligible = pending.single { it.cancellation == CancellationEligibility.Eligible }
    private val notEligible = pending.single { it.cancellation is CancellationEligibility.NotEligible }
    private val posted = DemoProfiles.underbanked.transactions.first { it.status == TransactionStatus.Posted }

    @Test fun approvesEligibleChargeAfterDelay() = runTest {
        val bank = SimulatedPartnerBank(responseDelayMillis = 1_800)
        val decision = bank.requestCancellation(eligible)
        assertTrue(decision is CancellationDecision.Approved)
        assertTrue((decision as CancellationDecision.Approved).referenceId.startsWith("SIM-"))
        assertEquals(1_800, currentTime)
    }

    @Test fun declinesIneligibleAndPostedCharges() = runTest {
        val bank = SimulatedPartnerBank(responseDelayMillis = 0)
        assertEquals(CancellationDecision.Declined(SimulatedPartnerBank.REASON_NOT_ELIGIBLE), bank.requestCancellation(notEligible))
        assertEquals(CancellationDecision.Declined(SimulatedPartnerBank.REASON_NOT_PENDING), bank.requestCancellation(posted))
    }

    @Test fun declineAllModeDeclinesEvenEligibleCharges() = runTest {
        val bank = SimulatedPartnerBank(mode = { SimulatedBankMode.DeclineAll }, responseDelayMillis = 0)
        assertEquals(CancellationDecision.Declined(SimulatedPartnerBank.REASON_MERCHANT_PROCESSING), bank.requestCancellation(eligible))
    }

    @Test fun referenceIsStablePerTransaction() {
        assertEquals(SimulatedPartnerBank.referenceFor(eligible), SimulatedPartnerBank.referenceFor(eligible))
    }
}
