package com.example.bankguard.model

/** Where a user's cancellation request for one pending charge stands. */
sealed interface CancellationStatus {
    /** Sent to the simulated partner bank; waiting for its answer. */
    data object Processing : CancellationStatus

    /** The simulated partner bank approved it. The charge no longer counts against the balance. */
    data class Approved(val referenceId: String) : CancellationStatus

    /** The simulated partner bank declined it. The charge still stands. */
    data class Declined(val reason: String) : CancellationStatus
}

/** The simulated partner bank's answer to a cancellation request. */
sealed interface CancellationDecision {
    data class Approved(val referenceId: String) : CancellationDecision
    data class Declined(val reason: String) : CancellationDecision
}
