package com.example.bankguard.data.demo

import com.example.bankguard.model.Money
import com.example.bankguard.model.RecoveryMechanism
import com.example.bankguard.model.SimulatedRecoveryRule
import com.example.bankguard.model.SimulatedSubmitOutcome
import com.example.bankguard.model.Transaction
import com.example.bankguard.model.TransactionCategory
import com.example.bankguard.model.TransactionStatus

/**
 * Resettable states of Alex's simulated account. The default, [UpcomingBill], is the Prevent
 * story (an overdraft is coming, nothing has gone wrong yet). The others are Recover stories:
 * an overdraft has ALREADY happened. Picking one changes the account everywhere in the app;
 * "Reset demo" goes back to [UpcomingBill].
 *
 * Every overdraft scenario uses the same figures, so the fee math is easy to follow:
 * $100.00 available, a $150.00 charge, -$50.00 after it.
 */
enum class AccountScenario(val key: String) {
    UpcomingBill("upcoming"),

    /** A. A pending purchase overdrew the account; the bank can release its hold. */
    PendingRelease("a"),

    /** B. A posted purchase overdrew the account; the merchant agreed to a same-day reversal. */
    PostedReversal("b"),

    /** C. A posted purchase overdrew the account but is outside the reversal window. */
    OutsideWindow("c"),

    /** D. Looks eligible, but the bank rejects the request. */
    BankRejects("d"),

    /** E. The bank accepts the request but hasn't confirmed the money moved. */
    AwaitingConfirmation("e"),
    ;

    val isOverdraft: Boolean get() = this != UpcomingBill

    companion object {
        fun fromKey(key: String?): AccountScenario = entries.firstOrNull { it.key == key } ?: UpcomingBill
    }
}

object DemoScenarios {

    /** The overdraft scenarios' charge and the balance before it. */
    val OVERDRAFT_CHARGE: Money = Money.of(-150.00)
    val BALANCE_BEFORE: Money = Money.of(100.00)

    fun underbanked(scenario: AccountScenario): DemoProfile {
        val base = DemoProfiles.underbanked
        if (scenario == AccountScenario.UpcomingBill) return base
        val charge = chargeFor(scenario)
        // The charge is the only thing after the balance reached $100.00, so:
        //  - pending: current balance $100.00, available $100.00 - $150.00 = -$50.00
        //  - posted:  current balance = available = -$50.00
        val current = if (charge.status == TransactionStatus.Pending) BALANCE_BEFORE else BALANCE_BEFORE + charge.amount
        return base.copy(
            account = base.account!!.copy(currentBalance = current),
            transactions = listOf(charge) + history,
            scenario = scenario,
        )
    }

    /** Older posted activity shared by every overdraft scenario (newest first). */
    private val history = listOf(
        Transaction("t-h-stream", "StreamPlus (sample)", TransactionCategory.Subscriptions, Money.of(-14.99), -5, TransactionStatus.Posted),
        Transaction("t-h-grocery", "Grocery Co (sample)", TransactionCategory.Groceries, Money.of(-64.20), -8, TransactionStatus.Posted),
        Transaction("t-h-pay", "Riverside Logistics payroll (sample)", TransactionCategory.Income, Money.of(1650.00), -12, TransactionStatus.Posted),
    )

    private fun chargeFor(scenario: AccountScenario): Transaction = when (scenario) {
        AccountScenario.PendingRelease -> Transaction(
            id = "t-ovd-a",
            merchant = "ElectroMart Online (sample)",
            category = TransactionCategory.Shopping,
            amount = OVERDRAFT_CHARGE,
            dayOffset = 0,
            status = TransactionStatus.Pending,
            recoveryRule = SimulatedRecoveryRule.Eligible(
                mechanism = RecoveryMechanism.AuthorizationHoldRelease,
                explanation = "The purchase is still pending and the merchant hasn't captured the payment, so the simulated bank can release the authorization hold.",
                onSubmit = SimulatedSubmitOutcome.Confirm,
            ),
        )
        AccountScenario.PostedReversal -> Transaction(
            id = "t-ovd-b",
            merchant = "TechHub Store (sample)",
            category = TransactionCategory.Shopping,
            amount = OVERDRAFT_CHARGE,
            dayOffset = 0,
            status = TransactionStatus.Posted,
            recoveryRule = SimulatedRecoveryRule.Eligible(
                mechanism = RecoveryMechanism.PostedTransactionReversal,
                explanation = "The charge has posted, but the merchant accepted a same-day order cancellation, so the simulated bank can reverse it with a credit.",
                onSubmit = SimulatedSubmitOutcome.Confirm,
            ),
        )
        AccountScenario.OutsideWindow -> Transaction(
            id = "t-ovd-c",
            merchant = "HomeGoods Depot (sample)",
            category = TransactionCategory.Shopping,
            amount = OVERDRAFT_CHARGE,
            dayOffset = -3,
            status = TransactionStatus.Posted,
            recoveryRule = SimulatedRecoveryRule.Ineligible(
                "This charge posted 3 days ago. In this simulation, posted charges can only be reversed within 1 day, and only when the merchant agrees.",
            ),
        )
        AccountScenario.BankRejects -> Transaction(
            id = "t-ovd-d",
            merchant = "City Ticket Exchange (sample)",
            category = TransactionCategory.Shopping,
            amount = OVERDRAFT_CHARGE,
            dayOffset = 0,
            status = TransactionStatus.Pending,
            recoveryRule = SimulatedRecoveryRule.Eligible(
                mechanism = RecoveryMechanism.AuthorizationHoldRelease,
                explanation = "The purchase is still pending, so the simulated bank will ask the merchant to release the authorization hold.",
                onSubmit = SimulatedSubmitOutcome.Reject,
                rejectionReason = "The merchant captured the payment before the hold could be released.",
            ),
        )
        AccountScenario.AwaitingConfirmation -> Transaction(
            id = "t-ovd-e",
            merchant = "Campus Bookstore (sample)",
            category = TransactionCategory.Shopping,
            amount = OVERDRAFT_CHARGE,
            dayOffset = 0,
            status = TransactionStatus.Posted,
            recoveryRule = SimulatedRecoveryRule.Eligible(
                mechanism = RecoveryMechanism.PostedTransactionReversal,
                explanation = "The merchant accepted a same-day return, so the simulated bank can reverse the posted charge once the merchant's credit arrives.",
                onSubmit = SimulatedSubmitOutcome.StayPending,
            ),
        )
        AccountScenario.UpcomingBill -> error("No overdraft charge in the default scenario")
    }
}
