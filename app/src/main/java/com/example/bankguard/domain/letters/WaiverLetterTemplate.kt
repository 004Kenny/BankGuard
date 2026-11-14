package com.example.bankguard.domain.letters

import com.example.bankguard.model.BankAccount
import com.example.bankguard.model.BankFee
import com.example.bankguard.model.BankFeeType
import com.example.bankguard.model.WaiverReason

/**
 * Builds a first draft of a fee-waiver letter from a fixed template. No AI is involved yet;
 * a later phase may replace this behind the same inputs, keeping this as the fallback.
 *
 * The draft is only a starting point: the user edits it and sends it themselves.
 * Anything in [square brackets] is a placeholder the user must fill in.
 * (English only for now; the letter is the user's own text, not app UI.)
 */
object WaiverLetterTemplate {

    const val NAME_PLACEHOLDER = "[Your full name]"

    fun feeName(type: BankFeeType): String = when (type) {
        BankFeeType.Overdraft -> "overdraft fee"
        BankFeeType.MonthlyMaintenance -> "monthly maintenance fee"
        BankFeeType.OutOfNetworkAtm -> "out-of-network ATM fee"
    }

    private fun reasonSentence(reason: WaiverReason, institution: String): String = when (reason) {
        WaiverReason.RarelyCharged ->
            "I don't usually have this kind of fee on my account, and I'm taking steps to avoid it in the future."
        WaiverReason.LongTimeCustomer ->
            "I've been a customer for some time and I value my relationship with $institution."
        WaiverReason.Hardship ->
            "Money is tight for me right now, and this fee makes it harder to cover my essential bills."
        WaiverReason.DelayedDeposit ->
            "This happened because a deposit I was expecting arrived later than planned."
    }

    /**
     * @param todayText today's date, already formatted (e.g. "November 13, 2026")
     * @param feeDateText the date the fee was charged, already formatted
     */
    fun build(
        fee: BankFee,
        account: BankAccount,
        reasons: Set<WaiverReason>,
        todayText: String,
        feeDateText: String,
        senderName: String = NAME_PLACEHOLDER,
    ): String {
        val name = feeName(fee.type)
        val reasonParagraph = WaiverReason.entries
            .filter { it in reasons }
            .joinToString(" ") { reasonSentence(it, fee.institution) }

        return buildString {
            appendLine(todayText)
            appendLine()
            appendLine(fee.institution)
            appendLine("Customer Service")
            appendLine()
            appendLine("Re: Request to waive the ${name} on account ending in ${account.last4}")
            appendLine()
            appendLine("Dear ${fee.institution} Customer Service,")
            appendLine()
            appendLine(
                "I'm writing to ask you to refund the ${name} of ${fee.amount.format()} " +
                    "charged to my account ending in ${account.last4} on ${feeDateText}.",
            )
            if (reasonParagraph.isNotEmpty()) {
                appendLine()
                appendLine(reasonParagraph)
            }
            appendLine()
            appendLine("I would appreciate it if you would refund this fee as a courtesy. Thank you for considering my request.")
            appendLine()
            appendLine("Sincerely,")
            append(senderName)
        }
    }
}
