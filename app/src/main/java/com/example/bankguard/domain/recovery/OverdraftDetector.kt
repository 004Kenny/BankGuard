package com.example.bankguard.domain.recovery

import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.model.Money
import com.example.bankguard.model.Transaction
import com.example.bankguard.model.TransactionStatus
import com.example.bankguard.model.sum

/** The charge that pushed the available balance below zero, and the balances around it. */
data class OverdraftCause(
    val transaction: Transaction,
    /** Available balance immediately before the charge. */
    val balanceBefore: Money,
    /** Available balance immediately after the charge. */
    val balanceAfter: Money,
    /** Available balance now. Negative, or there'd be no overdraft. */
    val currentAvailable: Money,
) {
    val overdraftAmount: Money get() = currentAvailable.absolute
}

/**
 * Finds an overdraft that has ALREADY happened (Recover's job). A projected future dip is
 * Prevent's job and is never reported here.
 *
 * It replays the account in order instead of assuming the newest charge is to blame:
 * posted transactions oldest first, then pending ones (pending charges reduce the available
 * balance on top of the current balance). The cause is the charge after which the balance
 * went below zero and stayed there.
 *
 * Within one day, transactions are taken in reverse list order, because demo lists are
 * newest first. Released holds count toward nothing.
 */
object OverdraftDetector {

    fun find(profile: DemoProfile): OverdraftCause? {
        val account = profile.account ?: return null
        val available = profile.availableBalance ?: return null
        if (!available.isNegative) return null

        val posted = chronological(profile.transactions.filter { it.status == TransactionStatus.Posted })
        val pending = chronological(profile.transactions.filter { it.status == TransactionStatus.Pending })

        var balance = account.currentBalance - posted.map(Transaction::amount).sum()
        var cause: OverdraftCause? = null
        for (t in posted + pending) {
            val before = balance
            balance += t.amount
            when {
                !before.isNegative && balance.isNegative -> cause = OverdraftCause(t, before, balance, available)
                !balance.isNegative -> cause = null
            }
        }
        return cause
    }

    private fun chronological(transactions: List<Transaction>): List<Transaction> =
        transactions.asReversed().sortedBy { it.dayOffset }
}
