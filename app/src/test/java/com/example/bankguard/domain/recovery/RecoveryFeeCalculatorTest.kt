package com.example.bankguard.domain.recovery

import com.example.bankguard.data.demo.AccountScenario
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RecoveryFeeCalculatorTest {

    @Test fun hundredDollarBalanceGivesFiveDollarFee() {
        assertEquals(Money.of(5.00), RecoveryFeeCalculator.fee(Money.of(100.00)))
    }

    @Test fun feeIsNeverBasedOnTheTransactionAmount() {
        // $100 before, $150 charge: 5% of the charge would be $7.50. It must be $5.00.
        val cause = OverdraftDetector.find(DemoProfiles.forScenario(DemoPersona.Underbanked, AccountScenario.PendingRelease))!!
        assertEquals(Money.of(-150.00), cause.transaction.amount)
        val quote = RecoveryQuote.of(cause)
        assertEquals(Money.of(5.00), quote.fee)
        assertNotEquals(Money.of(7.50), quote.fee)
    }

    @Test fun twoHundredDollarBalanceGivesTenDollarFee() {
        assertEquals(Money.of(10.00), RecoveryFeeCalculator.fee(Money.of(200.00)))
    }

    @Test fun zeroBalanceGivesNoFee() {
        assertEquals(Money.Zero, RecoveryFeeCalculator.fee(Money.Zero))
    }

    @Test fun negativeBalanceGivesNoFee() {
        assertEquals(Money.Zero, RecoveryFeeCalculator.feeBase(Money.of(-20.00)))
        assertEquals(Money.Zero, RecoveryFeeCalculator.fee(Money.of(-20.00)))
    }

    @Test fun roundsHalfUpToTheCent() {
        assertEquals(Money.of(1.67), RecoveryFeeCalculator.fee(Money.of(33.33))) // 1.6665
        assertEquals(Money.of(5.01), RecoveryFeeCalculator.fee(Money.of(100.10))) // 5.005
        assertEquals(Money.of(0.01), RecoveryFeeCalculator.fee(Money(10))) // 0.005
        assertEquals(Money.Zero, RecoveryFeeCalculator.fee(Money(9))) // 0.0045
        assertEquals(Money.of(2.47), RecoveryFeeCalculator.fee(Money.of(49.49))) // 2.4745
    }

    @Test fun largeValuesDoNotOverflow() {
        val huge = Money(Long.MAX_VALUE)
        // 5% of Long.MAX_VALUE cents, computed exactly.
        assertEquals(Money(461_168_601_842_738_790L), RecoveryFeeCalculator.fee(huge))
        assertEquals(Money.of(50_000_000.00), RecoveryFeeCalculator.fee(Money.of(1_000_000_000.00)))
    }

    @Test fun quoteShowsTheDefiningExample() {
        val cause = OverdraftDetector.find(DemoProfiles.forScenario(DemoPersona.Underbanked, AccountScenario.PendingRelease))!!
        val quote = RecoveryQuote.of(cause)
        assertEquals(Money.of(100.00), quote.balanceBefore)
        assertEquals(Money.of(-50.00), quote.currentBalance)
        assertEquals(Money.of(150.00), quote.expectedRestore)
        assertEquals(Money.of(5.00), quote.fee)
        assertEquals(Money.of(95.00), quote.estimatedFinalBalance)
    }
}
