package com.example.bankguard.domain.recovery

import com.example.bankguard.data.demo.AccountScenario
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.domain.forecast.CashFlowForecaster
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.FeeCollectionStatus
import com.example.bankguard.model.Money
import com.example.bankguard.model.RecoveryMechanism
import com.example.bankguard.model.RecoveryRecord
import com.example.bankguard.model.RecoveryRequestStatus
import com.example.bankguard.model.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveriesTest {

    private fun scenario(s: AccountScenario) = DemoProfiles.forScenario(DemoPersona.Underbanked, s)

    private fun record(
        id: String,
        mechanism: RecoveryMechanism,
        status: RecoveryRequestStatus = RecoveryRequestStatus.Confirmed,
        fee: FeeCollectionStatus = FeeCollectionStatus.Collected,
    ) = RecoveryRecord(
        transactionId = id, scenarioKey = "x", merchant = "M", transactionAmount = Money.of(-150.00),
        transactionDayOffset = 0, originalStatus = TransactionStatus.Pending, mechanism = mechanism,
        balanceBefore = Money.of(100.00), balanceAfter = Money.of(-50.00), quotedFee = Money.of(5.00),
        status = status, submittedAtMillis = 0,
        amountRestored = if (status == RecoveryRequestStatus.Confirmed) Money.of(150.00) else Money.Zero,
        feeStatus = if (status == RecoveryRequestStatus.Confirmed) fee else FeeCollectionStatus.NotCharged,
    )

    @Test fun holdReleaseGivesNinetyFiveAndKeepsTheCharge() {
        val after = scenario(AccountScenario.PendingRelease)
            .withRecoveries(listOf(record("t-ovd-a", RecoveryMechanism.AuthorizationHoldRelease)))
        assertEquals(Money.of(95.00), after.availableBalance)
        assertEquals(Money.of(95.00), after.account!!.currentBalance)
        val original = after.transactions.first { it.id == "t-ovd-a" }
        assertEquals(TransactionStatus.Released, original.status)
        assertEquals(Money.of(-150.00), original.amount)
        assertEquals(Money.of(-5.00), after.transactions.first { it.id == recoveryFeeId("t-ovd-a") }.amount)
        assertNull(OverdraftDetector.find(after))
    }

    @Test fun postedReversalAddsASeparateCredit() {
        val after = scenario(AccountScenario.PostedReversal)
            .withRecoveries(listOf(record("t-ovd-b", RecoveryMechanism.PostedTransactionReversal)))
        assertEquals(Money.of(95.00), after.availableBalance)
        assertEquals(TransactionStatus.Posted, after.transactions.first { it.id == "t-ovd-b" }.status)
        assertEquals(Money.of(150.00), after.transactions.first { it.id == reversalCreditId("t-ovd-b") }.amount)
    }

    @Test fun unconfirmedRecordsChangeNothing() {
        val base = scenario(AccountScenario.PendingRelease)
        listOf(RecoveryRequestStatus.Submitted, RecoveryRequestStatus.AwaitingConfirmation, RecoveryRequestStatus.Rejected).forEach { s ->
            val after = base.withRecoveries(listOf(record("t-ovd-a", RecoveryMechanism.AuthorizationHoldRelease, status = s)))
            assertSame(s.name, base, after)
            assertEquals(Money.of(-50.00), after.availableBalance)
        }
    }

    @Test fun duplicatesAndReapplicationNeverDoubleCount() {
        val r = record("t-ovd-b", RecoveryMechanism.PostedTransactionReversal)
        val once = scenario(AccountScenario.PostedReversal).withRecoveries(listOf(r))
        val dupes = scenario(AccountScenario.PostedReversal).withRecoveries(listOf(r, r, r))
        assertEquals(once, dupes)
        assertEquals(Money.of(95.00), dupes.availableBalance)
        assertEquals(1, dupes.transactions.count { it.id == reversalCreditId("t-ovd-b") })
        assertEquals(1, dupes.transactions.count { it.id == recoveryFeeId("t-ovd-b") })
    }

    @Test fun failedFeeCollectionTakesNoFee() {
        val after = scenario(AccountScenario.PendingRelease)
            .withRecoveries(listOf(record("t-ovd-a", RecoveryMechanism.AuthorizationHoldRelease, fee = FeeCollectionStatus.CollectionFailed)))
        assertEquals(Money.of(100.00), after.availableBalance)
        assertTrue(after.transactions.none { it.id == recoveryFeeId("t-ovd-a") })
    }

    @Test fun recordsFromAnotherScenarioAreIgnored() {
        val base = scenario(AccountScenario.PostedReversal)
        assertSame(base, base.withRecoveries(listOf(record("t-ovd-a", RecoveryMechanism.AuthorizationHoldRelease))))
    }

    @Test fun forecastRecalculatesFromTheRecoveredBalance() {
        val after = scenario(AccountScenario.PendingRelease)
            .withRecoveries(listOf(record("t-ovd-a", RecoveryMechanism.AuthorizationHoldRelease)))
        val forecast = CashFlowForecaster.forecast(after.availableBalance!!, after.scheduled)
        // $95 today, then the $185 bill tomorrow: Prevent now warns about a NEW, projected dip.
        assertEquals(Money.of(95.00), forecast.days.first().balance)
        assertEquals(Money.of(-90.00), forecast.lowest.balance)
    }
}
