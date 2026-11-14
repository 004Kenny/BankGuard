package com.example.bankguard.model

// Plain data for the demo profiles. Dates are stored as days from today
// (negative = past) so the demo always looks current without date libraries.

/** A simulated checking account. Available balance = current balance minus pending charges. */
data class BankAccount(
    val name: String,
    val institution: String,
    val last4: String,
    val currentBalance: Money,
    /** Fee the (fictional) bank charges when a payment overdraws the account. */
    val overdraftFee: Money,
    /** Balance below which the monthly maintenance fee applies, if any. */
    val minimumBalance: Money?,
    val maintenanceFee: Money?,
    /** Fee for using an ATM outside the bank's network. */
    val outOfNetworkAtmFee: Money,
)

enum class TransactionStatus { Posted, Pending }

/** Whether the user may ask the (simulated) partner bank to cancel a pending charge. */
sealed interface CancellationEligibility {
    data object Eligible : CancellationEligibility
    data class NotEligible(val reason: String) : CancellationEligibility
}

data class Transaction(
    val id: String,
    val merchant: String,
    val category: TransactionCategory,
    /** Negative for money out. */
    val amount: Money,
    val dayOffset: Int,
    val status: TransactionStatus,
    /** Only set for pending charges. */
    val cancellation: CancellationEligibility? = null,
)

enum class TransactionCategory { Groceries, Dining, Shopping, Subscriptions, Utilities, Housing, Income, Transport, Fees, Other }

enum class ScheduledKind { Income, Bill, Rent }

/** A known future deposit or bill, used for the cash-flow forecast. */
data class ScheduledItem(
    val id: String,
    val title: String,
    val kind: ScheduledKind,
    /** Negative for money out. */
    val amount: Money,
    val dayOffset: Int,
    val note: String,
)

enum class BankFeeType { Overdraft, MonthlyMaintenance, OutOfNetworkAtm }

/** A fee the demo bank already charged, which Recover can explain and help dispute. */
data class BankFee(
    val id: String,
    val type: BankFeeType,
    val amount: Money,
    val dayOffset: Int,
    val institution: String,
    val trigger: String,
)

enum class AltServiceType { CheckCashing, PrepaidCardFees, MoneyOrders }

/** A monthly cost the unbanked persona pays to alternative financial services. */
data class AltServiceCost(
    val type: AltServiceType,
    val label: String,
    val monthly: Money,
    val detail: String,
)

/** A single fee paid to an alternative financial service, for the fee log. */
data class AltServiceCharge(
    val id: String,
    val title: String,
    val provider: String,
    val detail: String,
    val amount: Money,
    val dayOffset: Int,
)

/** A cash or prepaid budget envelope for someone without a bank account. */
data class Envelope(
    val name: String,
    val used: Money,
    val limit: Money,
)
