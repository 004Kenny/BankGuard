package com.example.bankguard.domain.forecast

import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.model.Money
import com.example.bankguard.model.ScheduledItem
import com.example.bankguard.model.ScheduledKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CashFlowForecasterTest {

    private fun item(amount: Double, day: Int) =
        ScheduledItem("id$day$amount", "x", if (amount < 0) ScheduledKind.Bill else ScheduledKind.Income, Money.of(amount), day, "")

    @Test fun noScheduledItemsKeepsBalanceFlat() {
        val f = CashFlowForecaster.forecast(Money.of(100.0), emptyList(), horizonDays = 3)
        assertEquals(4, f.days.size)
        assertTrue(f.days.all { it.balance == Money.of(100.0) })
        assertFalse(f.hasOverdraftRisk)
    }

    @Test fun appliesItemsOnTheirDayAndIgnoresOnesPastHorizon() {
        val f = CashFlowForecaster.forecast(Money.of(100.0), listOf(item(-30.0, 1), item(50.0, 2), item(-500.0, 9)), horizonDays = 7)
        assertEquals(Money.of(70.0), f.days[1].balance)
        assertEquals(Money.of(120.0), f.ending.balance)
        assertNull(f.firstNegativeDay)
    }

    @Test fun underbankedDemoDipsTomorrowThenRecovers() {
        val alex = DemoProfiles.underbanked
        val f = CashFlowForecaster.forecast(alex.availableBalance!!, alex.scheduled)
        assertEquals(1, f.lowest.dayOffset)
        assertEquals(Money.of(-42.10), f.lowest.balance)
        assertEquals(1, f.firstNegativeDay?.dayOffset)
        assertEquals(Money.of(1607.90), f.ending.balance)
    }
}
