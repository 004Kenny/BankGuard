package com.example.bankguard.model

/** A low-fee account BankGuard can compare against what the user pays today. ALL are fictional. */
data class AccountOption(
    val id: String,
    val name: String,
    val institution: String,
    /** Identifies the (fictional) bank, for referral attribution. */
    val bankId: String,
    val monthlyFee: Money,
    /** How the monthly fee can be avoided, if it can. */
    val monthlyFeeWaivedWithDirectDeposit: Boolean,
    val minimumBalance: Money,
    /** Null means the account has no overdraft fee: purchases that would overdraw are declined. */
    val overdraftFee: Money?,
    val openingDeposit: Money,
    val outOfNetworkAtmFee: Money,
    /** Accepts people who've had trouble with a bank account before. */
    val secondChance: Boolean,
    /**
     * Would be a referral partner under BankGuard's PROPOSED business model. No partnerships
     * exist; this only drives the disclosure label and never the ranking.
     */
    val proposedPartner: Boolean,
    val features: List<String>,
)

/** How the user banks, for estimating yearly costs. The user can change every value. */
data class SavingsInputs(
    val overdraftsPerYear: Int,
    /** Months a year the balance drops below the current account's minimum. */
    val monthsBelowMinimum: Int,
    val outOfNetworkAtmPerMonth: Int,
    val hasDirectDeposit: Boolean,
    /** Unbanked only: what check cashing, prepaid cards and money orders cost each month now. */
    val currentAltServiceMonthly: Money,
)
