package com.example.bankguard.domain.recovery

import com.example.bankguard.data.demo.AccountScenario
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.Money
import com.example.bankguard.model.Transaction
import com.example.bankguard.model.TransactionCategory
import com.example.bankguard.model.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverdraftDetectorTest {

    private fun scenario(s: AccountScenario) = DemoProfiles.forScenario(DemoPersona.Underbanked, s)

    @Test fun projectedDipIsNotAnOverdraft() {
        // The default account has a forecast dip tomorrow, but nothing has gone wrong yet: Prevent's job.
        assertNull(OverdraftDetector.find(DemoProfiles.underbanked))
    }

    @Test fun unbankedHasNoOverdraft() {
        assertNull(OverdraftDetector.find(DemoProfiles.unbanked))
    }

    @Test fun everyOverdraftScenarioIsHundredToMinusFifty() {
        AccountScenario.entries.filter { it.isOverdraft }.forEach { s ->
            val cause = OverdraftDetector.find(scenario(s))!!
            assertEquals(s.name, Money.of(100.00), cause.balanceBefore)
            assertEquals(s.name, Money.of(-50.00), cause.balanceAfter)
            assertEquals(s.name, Money.of(-50.00), cause.currentAvailable)
            assertEquals(s.name, Money.of(-150.00), cause.transaction.amount)
            assertEquals(s.name, Money.of(50.00), cause.overdraftAmount)
        }
    }

    @Test fun findsPendingAndPostedCauses() {
        assertEquals(TransactionStatus.Pending, OverdraftDetector.find(scenario(AccountScenario.PendingRelease))!!.transaction.status)
        assertEquals(TransactionStatus.Posted, OverdraftDetector.find(scenario(AccountScenario.PostedReversal))!!.transaction.status)
    }

    @Test fun causeIsNotSimplyTheNewestCharge() {
        // A small coffee after the big purchase keeps the balance negative; the purchase is still the cause.
        val base = scenario(AccountScenario.PostedReversal)
        val coffee = Transaction("t-coffee-later", "Coffee (sample)", TransactionCategory.Dining, Money.of(-4.00), 0, TransactionStatus.Posted)
        val profile = base.copy(
            transactions = listOf(coffee) + base.transactions,
            account = base.account!!.copy(currentBalance = base.account!!.currentBalance - Money.of(4.00)),
        )
        val cause = OverdraftDetector.find(profile)!!
        assertEquals("t-ovd-b", cause.transaction.id)
        assertEquals(Money.of(100.00), cause.balanceBefore)
        assertEquals(Money.of(-54.00), cause.currentAvailable)
    }

    @Test fun laterChargeIsTheCauseIfTheBalanceRecoveredInBetween() {
        val base = scenario(AccountScenario.PostedReversal)
        val deposit = Transaction("t-dep", "Deposit (sample)", TransactionCategory.Income, Money.of(60.00), 0, TransactionStatus.Posted)
        val snack = Transaction("t-snack", "Snack (sample)", TransactionCategory.Dining, Money.of(-15.00), 0, TransactionStatus.Posted)
        // Newest first: snack after deposit after the $150 charge. -50 -> +10 -> -5.
        val profile = base.copy(
            transactions = listOf(snack, deposit) + base.transactions,
            account = base.account!!.copy(currentBalance = Money.of(-5.00)),
        )
        val cause = OverdraftDetector.find(profile)!!
        assertEquals("t-snack", cause.transaction.id)
        assertEquals(Money.of(10.00), cause.balanceBefore)
    }
}
