package com.example.bankguard.model

// Post-overdraft transaction recovery (the Recover pillar).
// BankGuard has NO real bank integration: every decision below comes from the simulated
// partner bank, and the 5% recovery fee is a PROPOSED prototype business model.

/** How a confirmed recovery gives money back. The two are different financial outcomes. */
enum class RecoveryMechanism {
    /** A pending charge's authorization hold is released. Nothing was ever paid, so nothing is "refunded". */
    AuthorizationHoldRelease,

    /** A posted charge is reversed by a separate credit. The original charge stays in the history. */
    PostedTransactionReversal,
}

/**
 * What the simulated partner bank's records say about one demo transaction. This is fixture
 * data that only the simulated bank reads; screens learn eligibility by asking the gateway.
 */
sealed interface SimulatedRecoveryRule {
    data class Eligible(
        val mechanism: RecoveryMechanism,
        val explanation: String,
        val onSubmit: SimulatedSubmitOutcome,
        /** Used when [onSubmit] is [SimulatedSubmitOutcome.Reject]. */
        val rejectionReason: String? = null,
    ) : SimulatedRecoveryRule

    data class Ineligible(val reason: String) : SimulatedRecoveryRule
}

/** What the simulated bank does once a request is submitted. Fixed per transaction, never random. */
enum class SimulatedSubmitOutcome { Confirm, Reject, StayPending }

/** The partner bank's answer to "can this charge be recovered?". */
sealed interface RecoveryEligibility {
    data class Eligible(val mechanism: RecoveryMechanism, val explanation: String) : RecoveryEligibility
    data class Ineligible(val reason: String) : RecoveryEligibility
}

/** The bank's answer when a recovery request is submitted. */
sealed interface RecoverySubmission {
    /** Accepted for processing. The money has NOT moved yet. */
    data class Accepted(val requestReference: String) : RecoverySubmission
    data class Rejected(val reason: String) : RecoverySubmission
}

/** The bank's latest word on an accepted request. Only [Confirmed] may change a balance. */
sealed interface RecoveryStatusUpdate {
    data object AwaitingConfirmation : RecoveryStatusUpdate
    data class Confirmed(val confirmationReference: String, val amountRestored: Money) : RecoveryStatusUpdate
    data class Rejected(val reason: String) : RecoveryStatusUpdate
}

/** Where a recovery request stands. */
enum class RecoveryRequestStatus {
    /** Sent to the simulated bank; no answer yet. */
    Submitted,

    /** The bank accepted the request but hasn't confirmed the money moved. Balance unchanged, no fee. */
    AwaitingConfirmation,

    /** The bank confirmed the hold release or reversal. The only status that changes a balance. */
    Confirmed,

    /** The bank said no. Nothing restored, no fee. */
    Rejected,
}

/** BankGuard's proposed recovery fee, tracked separately from the bank's reversal. */
enum class FeeCollectionStatus {
    /** No fee: the recovery hasn't been confirmed (or failed). */
    NotCharged,

    /** Recovery confirmed, so the fee is due; the separate collection event hasn't finished. */
    Assessed,

    /** The separate (simulated) fee event succeeded. */
    Collected,

    /** The fee couldn't be collected. Recorded as such, never shown as paid. */
    CollectionFailed,
}

/** Whether the simulated fee collection worked. */
sealed interface FeeCollectionResult {
    data object Collected : FeeCollectionResult
    data class Failed(val reason: String) : FeeCollectionResult
}

/**
 * One recovery request and everything that happened to it. Kept permanently as history;
 * the original transaction is never deleted.
 */
data class RecoveryRecord(
    val transactionId: String,
    /** The demo account scenario the transaction belongs to, so records never mix between scenarios. */
    val scenarioKey: String,
    val merchant: String,
    /** The original charge, negative. */
    val transactionAmount: Money,
    val transactionDayOffset: Int,
    val originalStatus: TransactionStatus,
    val mechanism: RecoveryMechanism,
    /** Available balance immediately before the charge. The fee base. */
    val balanceBefore: Money,
    /** Available balance immediately after the charge. */
    val balanceAfter: Money,
    /** The exact fee shown to, and agreed by, the user before submitting. */
    val quotedFee: Money,
    val status: RecoveryRequestStatus,
    val requestReference: String? = null,
    val confirmationReference: String? = null,
    val rejectionReason: String? = null,
    val submittedAtMillis: Long,
    val confirmedAtMillis: Long? = null,
    /** Only non-zero once [status] is [RecoveryRequestStatus.Confirmed]. */
    val amountRestored: Money = Money.Zero,
    val feeStatus: FeeCollectionStatus = FeeCollectionStatus.NotCharged,
    val feeFailureReason: String? = null,
) {
    val isConfirmed: Boolean get() = status == RecoveryRequestStatus.Confirmed

    /** Fee owed because recovery succeeded (whether or not it was collected). */
    val feeAssessed: Money get() = if (isConfirmed) quotedFee else Money.Zero

    /** Fee actually taken from the account. */
    val feeCollected: Money get() = if (feeStatus == FeeCollectionStatus.Collected) quotedFee else Money.Zero

    /** Balance right after the recovery and fee, from the figures at the time. */
    val balanceAfterRecovery: Money get() = balanceAfter + amountRestored - feeCollected
}

/** Progress of asking the bank whether one charge can be recovered. Not saved; it's re-checked. */
sealed interface EligibilityCheck {
    data object Checking : EligibilityCheck
    data class Done(val result: RecoveryEligibility) : EligibilityCheck

    /** The check itself failed (for example the bank didn't respond). Nothing is known; retry. */
    data class Failed(val message: String) : EligibilityCheck
}
