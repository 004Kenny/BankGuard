package com.example.bankguard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    @Test fun formatsWholeAndFractionalDollars() {
        assertEquals("$1,420.50", Money(142_050).format())
        assertEquals("$0.00", Money.Zero.format())
        assertEquals("$14.99", Money.of(14.99).format())
    }

    @Test fun formatsNegativeWithLeadingMinus() {
        assertEquals("-$42.10", Money(-4_210).format())
    }

    @Test fun signedFormatShowsPlusOnlyForPositive() {
        assertEquals("+$1,650.00", Money.of(1650.0).formatSigned())
        assertEquals("-$185.00", Money.of(-185.0).formatSigned())
        assertEquals("$0.00", Money.Zero.formatSigned())
    }

    @Test fun arithmeticStaysExactInCents() {
        val total = listOf(Money.of(0.10), Money.of(0.20)).sum()
        assertEquals(Money(30), total)
        assertEquals(Money(-4_210), Money.of(142.90) - Money.of(185.0))
        assertTrue(Money(-1).isNegative)
        assertEquals(Money(1_782_00), Money.of(148.50) * 12)
    }

    @Test fun ofRoundsToNearestCent() {
        assertEquals(Money(1_000), Money.of(9.999))
    }
}
