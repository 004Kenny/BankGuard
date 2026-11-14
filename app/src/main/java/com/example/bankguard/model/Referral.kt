package com.example.bankguard.model

// Simulated referral attribution for the Discover pillar.
// BankGuard has NO partner banks and generates no affiliate links. Under its PROPOSED business
// model a bank would pay a referral fee only when a referred user opens a QUALIFYING account,
// so a referral record here never means a commission was earned.

/** Where a referral came from. Every referral in this app comes from BankGuard. */
enum class ReferralSource { BankGuard }

/** How far a simulated referral got. Neither status means an account was opened. */
enum class ReferralStatus {
    /** The user tapped Open Account on a Discover account. */
    Initiated,

    /** The user chose to continue to the bank's (simulated) application. */
    ContinuedToBank,
}

/** One simulated referral to one Discover account. Kept as history for the demo session. */
data class ReferralRecord(
    /** "REF-BG-0001" style, sequential within the demo session. */
    val referralId: String,
    val bankId: String,
    /** The [AccountOption.id] the user chose. */
    val accountId: String,
    val source: ReferralSource = ReferralSource.BankGuard,
    val createdAtMillis: Long,
    val status: ReferralStatus = ReferralStatus.Initiated,
    val continuedAtMillis: Long? = null,
)
