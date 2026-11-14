package com.example.bankguard.domain.recovery

import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.model.FeeCollectionStatus
import com.example.bankguard.model.Money
import com.example.bankguard.model.RecoveryMechanism
import com.example.bankguard.model.RecoveryRecord
import com.example.bankguard.model.Transaction
import com.example.bankguard.model.TransactionCategory
import com.example.bankguard.model.TransactionStatus

/** Ids of the ledger entries a confirmed recovery adds, so they are easy to find and never duplicated. */
fun reversalCreditId(transactionId: String) = "recovery-credit:$transactionId"
fun recoveryFeeId(transactionId: String) = "recovery-fee:$transactionId"

/**
 * The profile as it looks after the user's recovery requests. A pure function of the records,
 * so applying it again never double-counts anything.
 *
 * Only CONFIRMED records change money:
 *  - Hold release: the pending charge becomes [TransactionStatus.Released] (kept in history,
 *    no longer reduces the available balance). The current balance doesn't change, because a
 *    hold was never paid.
 *  - Posted reversal: the original charge stays as it was; a separate credit is added and the
 *    current balance goes up by the same amount.
 *  - BankGuard's fee: a separate debit, added only if the simulated fee event succeeded.
 *
 * Submitted, awaiting-confirmation and rejected records change nothing. Records for
 * transactions that aren't in this profile (another scenario or persona) are ignored.
 */
fun DemoProfile.withRecoveries(records: Collection<RecoveryRecord>): DemoProfile {
    val account = account ?: return this
    val ids = transactions.map(Transaction::id).toSet()
    val confirmed = records
        .filter { it.isConfirmed && it.transactionId in ids }
        .distinctBy(RecoveryRecord::transactionId)
    if (confirmed.isEmpty()) return this

    var balance = account.currentBalance
    val added = mutableListOf<Transaction>()
    val released = mutableSetOf<String>()
    for (r in confirmed) {
        val original = transactions.first { it.id == r.transactionId }
        when (r.mechanism) {
            RecoveryMechanism.AuthorizationHoldRelease ->
                if (original.status == TransactionStatus.Pending) released += original.id
            RecoveryMechanism.PostedTransactionReversal -> {
                balance += r.amountRestored
                added += Transaction(
                    id = reversalCreditId(r.transactionId),
                    merchant = "Reversal: ${r.merchant}",
                    category = TransactionCategory.Refunds,
                    amount = r.amountRestored,
                    dayOffset = 0,
                    status = TransactionStatus.Posted,
                )
            }
        }
        if (r.feeStatus == FeeCollectionStatus.Collected && r.quotedFee != Money.Zero) {
            balance -= r.quotedFee
            added += Transaction(
                id = recoveryFeeId(r.transactionId),
                merchant = "BankGuard recovery fee (simulated)",
                category = TransactionCategory.Fees,
                amount = -r.quotedFee,
                dayOffset = 0,
                status = TransactionStatus.Posted,
            )
        }
    }
    return copy(
        account = account.copy(currentBalance = balance),
        // Newest first, like the rest of the list: the fee comes after the credit it depends on.
        transactions = added.asReversed() + transactions.map { t ->
            if (t.id in released) t.copy(status = TransactionStatus.Released) else t
        },
    )
}
