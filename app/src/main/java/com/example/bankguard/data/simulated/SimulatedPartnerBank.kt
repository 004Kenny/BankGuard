package com.example.bankguard.data.simulated

import com.example.bankguard.model.CancellationDecision
import com.example.bankguard.model.CancellationEligibility
import com.example.bankguard.model.FeeCollectionResult
import com.example.bankguard.model.Money
import com.example.bankguard.model.RecoveryEligibility
import com.example.bankguard.model.RecoveryMechanism
import com.example.bankguard.model.RecoveryStatusUpdate
import com.example.bankguard.model.RecoverySubmission
import com.example.bankguard.model.SimulatedRecoveryRule
import com.example.bankguard.model.SimulatedSubmitOutcome
import com.example.bankguard.model.Transaction
import com.example.bankguard.model.TransactionStatus
import kotlinx.coroutines.delay

/**
 * The proposed partner-bank integration: cancelling pending charges (Prevent) and recovering
 * charges that already caused an overdraft (Recover).
 * BankGuard has NO real bank integration; the only implementation is [SimulatedPartnerBank].
 */
interface PartnerBankGateway {
    suspend fun requestCancellation(transaction: Transaction): CancellationDecision

    /** Whether this charge qualifies for recovery, and how. Throws if the bank can't be reached. */
    suspend fun checkRecoveryEligibility(transaction: Transaction): RecoveryEligibility

    /** Submits a recovery request the user has explicitly confirmed. Acceptance is not confirmation. */
    suspend fun submitRecovery(transaction: Transaction): RecoverySubmission

    /** The latest status of an accepted request. Only [RecoveryStatusUpdate.Confirmed] means money moved. */
    suspend fun recoveryStatus(transaction: Transaction, requestReference: String): RecoveryStatusUpdate
}

/**
 * Collects BankGuard's proposed recovery fee as its own event, separate from the bank's
 * reversal. Nothing real is ever debited.
 */
fun interface RecoveryFeeCollector {
    suspend fun collect(transactionId: String, fee: Money): FeeCollectionResult
}

/** Demo stand-in: records the fee as collected. BankGuard has no authority to debit a real account. */
class SimulatedFeeCollector : RecoveryFeeCollector {
    override suspend fun collect(transactionId: String, fee: Money): FeeCollectionResult = FeeCollectionResult.Collected
}

/** How the simulated bank answers, so a presenter can demo every outcome. */
enum class SimulatedBankMode {
    /** Follow each transaction's demo rule. */
    ApproveEligible,

    /** Decline every cancellation and reject every recovery request. */
    DeclineAll,

    /** The bank can't be reached for recovery eligibility checks. */
    Unavailable,
}

/**
 * Stand-in for a partner bank. It never contacts anything: after a short pause it answers by
 * fixed rules, so demos and tests are repeatable. There are no random decisions.
 *
 * Cancellation rules:
 *  - Only pending charges marked eligible can be approved; anything else is declined.
 *  - In [SimulatedBankMode.DeclineAll], every request is declined.
 *
 * Recovery rules (from each transaction's [SimulatedRecoveryRule]):
 *  - No rule, or [SimulatedRecoveryRule.Ineligible]: not eligible, with a reason.
 *  - Eligible: names the mechanism (hold release or posted reversal). On submit the request is
 *    confirmed, rejected or left awaiting confirmation, as the rule says.
 *  - [SimulatedBankMode.DeclineAll] rejects every submission;
 *    [SimulatedBankMode.Unavailable] makes eligibility checks fail.
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
            SimulatedBankMode.DeclineAll -> CancellationDecision.Declined(REASON_MERCHANT_PROCESSING)
            else -> CancellationDecision.Approved(referenceFor(transaction))
        }
    }

    override suspend fun checkRecoveryEligibility(transaction: Transaction): RecoveryEligibility {
        delay(responseDelayMillis / 2)
        if (mode() == SimulatedBankMode.Unavailable) throw BankUnavailableException()
        return when (val rule = transaction.recoveryRule) {
            null -> RecoveryEligibility.Ineligible(REASON_NO_RECOVERY_PATH)
            is SimulatedRecoveryRule.Ineligible -> RecoveryEligibility.Ineligible(rule.reason)
            is SimulatedRecoveryRule.Eligible -> when {
                rule.mechanism == RecoveryMechanism.AuthorizationHoldRelease &&
                    transaction.status != TransactionStatus.Pending ->
                    RecoveryEligibility.Ineligible(REASON_HOLD_ALREADY_POSTED)
                else -> RecoveryEligibility.Eligible(rule.mechanism, rule.explanation)
            }
        }
    }

    override suspend fun submitRecovery(transaction: Transaction): RecoverySubmission {
        delay(responseDelayMillis)
        val rule = transaction.recoveryRule as? SimulatedRecoveryRule.Eligible
            ?: return RecoverySubmission.Rejected(REASON_NO_RECOVERY_PATH)
        if (mode() == SimulatedBankMode.DeclineAll) return RecoverySubmission.Rejected(REASON_RECOVERY_DECLINED)
        return when (rule.onSubmit) {
            SimulatedSubmitOutcome.Reject -> RecoverySubmission.Rejected(rule.rejectionReason ?: REASON_RECOVERY_DECLINED)
            SimulatedSubmitOutcome.Confirm, SimulatedSubmitOutcome.StayPending ->
                RecoverySubmission.Accepted(requestReferenceFor(transaction))
        }
    }

    override suspend fun recoveryStatus(transaction: Transaction, requestReference: String): RecoveryStatusUpdate {
        delay(responseDelayMillis / 2)
        val rule = transaction.recoveryRule as? SimulatedRecoveryRule.Eligible
            ?: return RecoveryStatusUpdate.Rejected(REASON_NO_RECOVERY_PATH)
        return when (rule.onSubmit) {
            SimulatedSubmitOutcome.Confirm -> RecoveryStatusUpdate.Confirmed(referenceFor(transaction), transaction.amount.absolute)
            SimulatedSubmitOutcome.StayPending -> RecoveryStatusUpdate.AwaitingConfirmation
            SimulatedSubmitOutcome.Reject -> RecoveryStatusUpdate.Rejected(rule.rejectionReason ?: REASON_RECOVERY_DECLINED)
        }
    }

    /** Thrown when the simulated bank is set to be unreachable. */
    class BankUnavailableException : Exception("The simulated partner bank didn't respond.")

    companion object {
        const val REASON_NOT_PENDING = "This charge has already posted, so it can't be cancelled."
        const val REASON_NOT_ELIGIBLE = "This charge isn't eligible for cancellation."
        const val REASON_MERCHANT_PROCESSING = "The merchant has already started processing this charge."
        const val REASON_NO_RECOVERY_PATH = "The simulated bank has no supported way to reverse this charge."
        const val REASON_HOLD_ALREADY_POSTED = "This charge has already posted, so there's no hold left to release."
        const val REASON_RECOVERY_DECLINED = "The simulated bank declined this recovery request."

        /** Stable, obviously simulated confirmation number, e.g. "SIM-4821". */
        fun referenceFor(transaction: Transaction): String =
            "SIM-" + (Math.floorMod(transaction.id.hashCode(), 9000) + 1000)

        /** Stable, obviously simulated request number, e.g. "REQ-SIM-4821". */
        fun requestReferenceFor(transaction: Transaction): String = "REQ-" + referenceFor(transaction)
    }
}
