package com.example.bankguard.model

/**
 * Where the user is with asking for one fee back. BankGuard never contacts the bank:
 * "Sent" and the decision are what the USER reports after acting themselves.
 */
enum class RecoveryStage { Found, Drafted, Sent, Refunded, NotRefunded }

/** Reasons the user can choose to include in a waiver letter. Only true ones should be picked. */
enum class WaiverReason { RarelyCharged, LongTimeCustomer, Hardship, DelayedDeposit }

data class FeeRecovery(
    val stage: RecoveryStage = RecoveryStage.Found,
    val letter: String? = null,
    val reasons: Set<WaiverReason> = emptySet(),
)
