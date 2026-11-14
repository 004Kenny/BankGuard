package com.example.bankguard.domain.letters

import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.model.BankFeeType
import com.example.bankguard.model.WaiverReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaiverLetterTemplateTest {

    private val alex = DemoProfiles.underbanked
    private val account = alex.account!!
    private val overdraft = alex.bankFees.single { it.type == BankFeeType.Overdraft }

    private fun build(reasons: Set<WaiverReason>) =
        WaiverLetterTemplate.build(overdraft, account, reasons, "November 13, 2026", "October 24, 2026")

    @Test fun includesFeeAccountAndDates() {
        val letter = build(emptySet())
        assertTrue(letter.startsWith("November 13, 2026"))
        assertTrue(letter.contains("refund the overdraft fee of $35.00"))
        assertTrue(letter.contains("account ending in 4109 on October 24, 2026"))
        assertTrue(letter.contains("Dear Sample Community Bank (demo) Customer Service,"))
        assertTrue(letter.contains("Re: Request to waive the overdraft fee on account ending in 4109"))
        assertTrue(letter.endsWith(WaiverLetterTemplate.NAME_PLACEHOLDER))
    }

    @Test fun omitsReasonParagraphWhenNoneChosen() {
        val letter = build(emptySet())
        assertFalse(letter.contains("Money is tight"))
        assertFalse(letter.contains("\n\n\n"))
    }

    @Test fun includesOnlyChosenReasonsInFixedOrder() {
        val letter = build(setOf(WaiverReason.Hardship, WaiverReason.RarelyCharged))
        val rarely = letter.indexOf("I don't usually have this kind of fee")
        val hardship = letter.indexOf("Money is tight")
        assertTrue(rarely in 0 until hardship)
        assertFalse(letter.contains("value my relationship"))
        assertFalse(letter.contains("deposit I was expecting"))
    }

    @Test fun neverPromisesAnOutcome() {
        val letter = build(WaiverReason.entries.toSet()).lowercase()
        listOf("guarantee", "must refund", "required by law", "legal").forEach {
            assertFalse("letter should not contain '$it'", letter.contains(it))
        }
    }

    @Test fun everyFeeTypeHasAName() {
        assertEquals(BankFeeType.entries.size, BankFeeType.entries.map(WaiverLetterTemplate::feeName).toSet().size)
    }
}
