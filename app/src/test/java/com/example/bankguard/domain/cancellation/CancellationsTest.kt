package com.example.bankguard.domain.cancellation

import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.domain.forecast.CashFlowForecaster
import com.example.bankguard.model.CancellationStatus
import com.example.bankguard.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class CancellationsTest {

    private val alex = DemoProfiles.underbanked

    @Test fun noApprovalsLeavesProfileUnchanged() {
        val statuses = mapOf(
            "t-pending-shop" to CancellationStatus.Processing,
            "t-pending-fuel" to CancellationStatus.Declined("no"),
        )
        assertSame(alex, alex.withCancellations(statuses))
    }

    @Test fun approvedCancellationRaisesBalanceAndClearsTheDip() {
        val updated = alex.withCancellations(mapOf("t-pending-shop" to CancellationStatus.Approved("SIM-1")))
        assertEquals(Money.of(201.30), updated.availableBalance)
        val forecast = CashFlowForecaster.forecast(updated.availableBalance!!, updated.scheduled)
        assertEquals(Money.of(16.30), forecast.lowest.balance)
        assertFalse(forecast.hasOverdraftRisk)
    }
}
