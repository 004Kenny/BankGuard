package com.example.bankguard.data.demo

import com.example.bankguard.model.AltServiceCharge
import com.example.bankguard.model.AltServiceCost
import com.example.bankguard.model.AltServiceType
import com.example.bankguard.model.BankAccount
import com.example.bankguard.model.BankFee
import com.example.bankguard.model.BankFeeType
import com.example.bankguard.model.CancellationEligibility
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.Envelope
import com.example.bankguard.model.Money
import com.example.bankguard.model.ScheduledItem
import com.example.bankguard.model.ScheduledKind
import com.example.bankguard.model.Transaction
import com.example.bankguard.model.TransactionCategory
import com.example.bankguard.model.TransactionStatus
import com.example.bankguard.model.sum

/**
 * Everything BankGuard knows about one demo user.
 *
 * ALL OF THIS IS FICTIONAL. People, banks, merchants and amounts are invented for the
 * hackathon demo; no figure comes from, or is sent to, a real financial institution.
 */
data class DemoProfile(
    val persona: DemoPersona,
    val firstName: String,
    /** Null for the unbanked persona. */
    val account: BankAccount?,
    val transactions: List<Transaction>,
    val scheduled: List<ScheduledItem>,
    val bankFees: List<BankFee>,
    val altServiceCosts: List<AltServiceCost>,
    val altServiceCharges: List<AltServiceCharge>,
    val envelopes: List<Envelope>,
    val prepaidCardBalance: Money?,
    /** Which simulated account state this is. Only the underbanked persona has more than one. */
    val scenario: AccountScenario = AccountScenario.UpcomingBill,
) {
    val pendingTransactions: List<Transaction>
        get() = transactions.filter { it.status == TransactionStatus.Pending }

    /** Current balance minus pending charges, or null without an account. */
    val availableBalance: Money?
        get() = account?.let { it.currentBalance + pendingTransactions.map(Transaction::amount).sum() }

    val monthlyAltServiceCost: Money get() = altServiceCosts.map(AltServiceCost::monthly).sum()
    val totalBankFees: Money get() = bankFees.map(BankFee::amount).sum()
}

object DemoProfiles {

    fun forPersona(persona: DemoPersona): DemoProfile = when (persona) {
        DemoPersona.Underbanked -> underbanked
        DemoPersona.Unbanked -> unbanked
    }

    /**
     * The persona's data in the chosen account scenario, before any demo action. The unbanked
     * persona has no account, so it ignores the scenario (no overdraft is ever invented for it).
     */
    fun forScenario(persona: DemoPersona, scenario: AccountScenario): DemoProfile = when (persona) {
        DemoPersona.Underbanked -> DemoScenarios.underbanked(scenario)
        DemoPersona.Unbanked -> unbanked
    }

    /**
     * Alex has a checking account with a thin buffer. The story the demo tells:
     * available $142.90, a $185.00 utility bill tomorrow, payday the day after, so the
     * forecast dips to -$42.10. Cancelling the eligible $58.40 pending order (if the
     * simulated partner bank approves) would keep the balance at $16.30 instead.
     */
    val underbanked = DemoProfile(
        persona = DemoPersona.Underbanked,
        firstName = "Alex",
        account = BankAccount(
            name = "Everyday Checking",
            institution = "Sample Community Bank (demo)",
            last4 = "4109",
            currentBalance = Money.of(233.30),
            overdraftFee = Money.of(35.00),
            minimumBalance = Money.of(500.00),
            maintenanceFee = Money.of(12.00),
            outOfNetworkAtmFee = Money.of(3.50),
        ),
        transactions = listOf(
            Transaction(
                id = "t-pending-shop",
                merchant = "QuickShop Online (sample)",
                category = TransactionCategory.Shopping,
                amount = Money.of(-58.40),
                dayOffset = 0,
                status = TransactionStatus.Pending,
                cancellation = CancellationEligibility.Eligible,
            ),
            Transaction(
                id = "t-pending-fuel",
                merchant = "Fuel Stop (sample)",
                category = TransactionCategory.Transport,
                amount = Money.of(-32.00),
                dayOffset = 0,
                status = TransactionStatus.Pending,
                cancellation = CancellationEligibility.NotEligible(
                    "In this demo, fuel-pump holds are treated as not eligible for cancellation.",
                ),
            ),
            Transaction("t-grocery", "Grocery Co (sample)", TransactionCategory.Groceries, Money.of(-64.20), 0, TransactionStatus.Posted),
            Transaction("t-coffee", "Coffee Corner (sample)", TransactionCategory.Dining, Money.of(-4.75), -1, TransactionStatus.Posted),
            Transaction("t-stream", "StreamPlus (sample)", TransactionCategory.Subscriptions, Money.of(-14.99), -3, TransactionStatus.Posted),
            Transaction("t-pay", "Riverside Logistics payroll (sample)", TransactionCategory.Income, Money.of(1650.00), -12, TransactionStatus.Posted),
        ),
        scheduled = listOf(
            ScheduledItem("s-power", "Metro Power & Light (sample)", ScheduledKind.Bill, Money.of(-185.00), 1, "Autopay"),
            ScheduledItem("s-pay", "Riverside Logistics payroll (sample)", ScheduledKind.Income, Money.of(1650.00), 2, "Direct deposit"),
            ScheduledItem("s-rent", "Northside Apartments (sample)", ScheduledKind.Rent, Money.of(-950.00), 8, "Scheduled payment"),
        ),
        bankFees = listOf(
            BankFee(
                id = "f-overdraft",
                type = BankFeeType.Overdraft,
                amount = Money.of(35.00),
                dayOffset = -20,
                institution = "Sample Community Bank (demo)",
                trigger = "A $12.99 card purchase posted when the balance was $4.10.",
            ),
            BankFee(
                id = "f-maintenance",
                type = BankFeeType.MonthlyMaintenance,
                amount = Money.of(12.00),
                dayOffset = -9,
                institution = "Sample Community Bank (demo)",
                trigger = "The balance dropped below the account's $500 minimum during the statement period.",
            ),
            BankFee(
                id = "f-atm",
                type = BankFeeType.OutOfNetworkAtm,
                amount = Money.of(3.50),
                dayOffset = -15,
                institution = "Sample Community Bank (demo)",
                trigger = "Cash withdrawal at an ATM outside the bank's network.",
            ),
        ),
        altServiceCosts = emptyList(),
        altServiceCharges = emptyList(),
        envelopes = emptyList(),
        prepaidCardBalance = null,
    )

    /**
     * Jordan has no bank account: cashes paychecks at a check casher, uses a prepaid card,
     * and pays rent and bills by money order. $148.50 a month goes to fees ($1,782 a year).
     */
    val unbanked = DemoProfile(
        persona = DemoPersona.Unbanked,
        firstName = "Jordan",
        account = null,
        transactions = emptyList(),
        scheduled = listOf(
            ScheduledItem("s-u-pay", "Paycheck to cash (sample)", ScheduledKind.Income, Money.of(1062.50), 4, "Paper check"),
            ScheduledItem("s-u-rent", "Rent by money order (sample)", ScheduledKind.Rent, Money.of(-750.00), 9, "Money order"),
            ScheduledItem("s-u-phone", "Prepaid phone plan (sample)", ScheduledKind.Bill, Money.of(-40.00), 6, "Paid at store"),
        ),
        bankFees = emptyList(),
        altServiceCosts = listOf(
            AltServiceCost(AltServiceType.CheckCashing, "Check cashing", Money.of(68.00), "2 paychecks a month at a 3.2% fee"),
            AltServiceCost(AltServiceType.PrepaidCardFees, "Prepaid card and ATM fees", Money.of(48.00), "Monthly card fee, reloads and ATM withdrawals"),
            AltServiceCost(AltServiceType.MoneyOrders, "Money orders and bill pay", Money.of(32.50), "Rent money order and bill-payment counter fees"),
        ),
        altServiceCharges = listOf(
            AltServiceCharge("c-cash", "Paycheck cashing", "Metro Check Express (sample)", "3.2% of $1,062.50", Money.of(-34.00), -2),
            AltServiceCharge("c-atm", "Prepaid card ATM withdrawal", "Out-of-network ATM", "ATM fee plus card surcharge", Money.of(-5.50), -6),
            AltServiceCharge("c-reload", "Prepaid card reload", "Corner Market (sample)", "Reload fee", Money.of(-3.95), -10),
            AltServiceCharge("c-mo", "Rent money order", "Corner Market (sample)", "Money order fee", Money.of(-3.75), -10),
        ),
        envelopes = listOf(
            Envelope("Rent (money order set aside)", Money.of(750.00), Money.of(750.00)),
            Envelope("Food and household", Money.of(180.00), Money.of(350.00)),
            Envelope("Transportation", Money.of(46.00), Money.of(120.00)),
        ),
        prepaidCardBalance = Money.of(45.20),
    )
}
