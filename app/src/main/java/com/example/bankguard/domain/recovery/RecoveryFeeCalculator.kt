package com.example.bankguard.domain.recovery

import com.example.bankguard.model.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * BankGuard's PROPOSED success-based recovery fee (a prototype business model, not a
 * bank-approved charge):
 *
 *     feeBase = max(available balance immediately BEFORE the overdraft-causing charge, $0.00)
 *     fee     = 5% x feeBase, rounded half-up to the cent
 *
 * The fee is never calculated from the charge being recovered. Example: $100 balance, $150
 * purchase, -$50 overdraft. If the $150 is recovered the fee is $5.00, not $7.50.
 */
object RecoveryFeeCalculator {

    /** 5%, as a percentage. */
    const val RATE_PERCENT = 5

    private val RATE = BigDecimal("0.05")

    /** The amount the fee is a percentage of. A zero or negative starting balance gives a zero base. */
    fun feeBase(balanceBeforeTransaction: Money): Money =
        if (balanceBeforeTransaction.isNegative) Money.Zero else balanceBeforeTransaction

    /** 5% of [feeBase], in exact decimal arithmetic, rounded half-up to the cent. */
    fun fee(balanceBeforeTransaction: Money): Money {
        val cents = BigDecimal.valueOf(feeBase(balanceBeforeTransaction).cents)
            .multiply(RATE)
            .setScale(0, RoundingMode.HALF_UP)
        return Money(cents.longValueExact())
    }
}

/** What a recovery would mean, shown before the user agrees. */
data class RecoveryQuote(
    /** The original charge, negative. */
    val transactionAmount: Money,
    val balanceBefore: Money,
    /** Available balance now. */
    val currentBalance: Money,
    /** What the bank would restore or release if it confirms. */
    val expectedRestore: Money,
    val fee: Money,
) {
    /** Current balance + restored amount - fee. */
    val estimatedFinalBalance: Money get() = currentBalance + expectedRestore - fee

    companion object {
        fun of(cause: OverdraftCause): RecoveryQuote = RecoveryQuote(
            transactionAmount = cause.transaction.amount,
            balanceBefore = cause.balanceBefore,
            currentBalance = cause.currentAvailable,
            expectedRestore = cause.transaction.amount.absolute,
            fee = RecoveryFeeCalculator.fee(cause.balanceBefore),
        )
    }
}
