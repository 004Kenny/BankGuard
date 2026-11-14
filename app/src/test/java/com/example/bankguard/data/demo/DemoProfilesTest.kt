package com.example.bankguard.data.demo

import com.example.bankguard.model.CancellationEligibility
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.Money
import com.example.bankguard.model.ScheduledKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the demo story so later edits don't silently break the numbers the UI relies on. */
class DemoProfilesTest {

    private val alex = DemoProfiles.underbanked
    private val jordan = DemoProfiles.unbanked

    @Test fun eachPersonaMapsToItsProfile() {
        DemoPersona.entries.forEach { assertEquals(it, DemoProfiles.forPersona(it).persona) }
    }

    @Test fun underbankedAvailableBalanceSubtractsPendingCharges() {
        assertEquals(Money.of(142.90), alex.availableBalance)
    }

    @Test fun underbankedBillBeforePaydayCausesTheDemoDip() {
        val bill = alex.scheduled.first { it.kind == ScheduledKind.Bill }
        val payday = alex.scheduled.first { it.kind == ScheduledKind.Income }
        assertTrue(bill.dayOffset < payday.dayOffset)
        assertEquals(Money.of(-42.10), alex.availableBalance!! + bill.amount)
    }

    @Test fun cancellingTheEligibleChargeAvoidsTheDip() {
        val eligible = alex.pendingTransactions.single { it.cancellation == CancellationEligibility.Eligible }
        val bill = alex.scheduled.first { it.kind == ScheduledKind.Bill }
        assertEquals(Money.of(16.30), alex.availableBalance!! - eligible.amount + bill.amount)
    }

    @Test fun underbankedFeesTotal() {
        assertEquals(Money.of(50.50), alex.totalBankFees)
    }

    @Test fun unbankedHasNoAccountAndConsistentMonthlyCost() {
        assertNull(jordan.account)
        assertNull(jordan.availableBalance)
        assertEquals(Money.of(148.50), jordan.monthlyAltServiceCost)
        assertEquals(Money.of(1782.00), jordan.monthlyAltServiceCost * 12)
    }

    @Test fun checkCashingCostMatchesTwoPaychecksAtStatedRate() {
        val paycheck = jordan.scheduled.first { it.kind == ScheduledKind.Income }.amount
        val perCheck = Money(Math.round(paycheck.cents * 0.032))
        assertEquals(Money.of(34.00), perCheck)
        assertEquals(Money.of(68.00), perCheck * 2)
    }
}
