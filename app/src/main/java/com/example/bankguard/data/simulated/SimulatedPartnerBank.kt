package com.example.bankguard.data.simulated

import com.example.bankguard.model.CancellationDecision
import com.example.bankguard.model.CancellationEligibility
import com.example.bankguard.model.Transaction
import com.example.bankguard.model.TransactionStatus
import kotlinx.coroutines.delay

/**
 * The proposed partner-bank integration for cancelling pending charges.
 * BankGuard has NO real bank integration; the only implementation is [SimulatedPartnerBank].
 */
interface PartnerBankGateway {
    suspend fun requestCancellation(transaction: Transaction): CancellationDecision
}

/** How the simulated bank answers, so a presenter can demo both outcomes. */
enum class SimulatedBankMode { ApproveEligible, DeclineAll }

/**
 * Stand-in for a partner bank. It never contacts anything: after a short pause it answers
 * by fixed rules, so demos are repeatable.
 *
 * Rules:
 *  - Only pending charges marked eligible can be approved; anything else is declined.
 *  - In [SimulatedBankMode.DeclineAll], every request is declined.
 */
class SimulatedPartnerBank(
    private val mode: () -> SimulatedBankMode = { SimulatedBankMode.ApproveEligible },
    private val responseDelayMillis: Long = 1_800,
) : PartnerBankGateway {

    override suspend fun requestCancellation(transaction: Transaction): CancellationDecision {
        delay(responseDelayMillis)
        if (transaction.status != TransactionStatus.Pending) {
            return CancellationDecision.Declined(REASON_NOT_PENDING)
        }
        if (transaction.cancellation !is CancellationEligibility.Eligible) {
            return CancellationDecision.Declined(REASON_NOT_ELIGIBLE)
        }
        return when (mode()) {
            SimulatedBankMode.ApproveEligible -> CancellationDecision.Approved(referenceFor(transaction))
            SimulatedBankMode.DeclineAll -> CancellationDecision.Declined(REASON_MERCHANT_PROCESSING)
        }
    }

    companion object {
        const val REASON_NOT_PENDING = "This charge has already posted, so it can't be cancelled."
        const val REASON_NOT_ELIGIBLE = "This charge isn't eligible for cancellation."
        const val REASON_MERCHANT_PROCESSING = "The merchant has already started processing this charge."

        /** Stable, obviously simulated reference number, e.g. "SIM-4821". */
        fun referenceFor(transaction: Transaction): String =
            "SIM-" + (Math.floorMod(transaction.id.hashCode(), 9000) + 1000)
    }
}
