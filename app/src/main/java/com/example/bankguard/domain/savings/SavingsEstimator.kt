package com.example.bankguard.domain.savings

import com.example.bankguard.model.AccountOption
import com.example.bankguard.model.BankAccount
import com.example.bankguard.model.Money
import com.example.bankguard.model.SavingsInputs

/** Yearly fee estimate for one option, compared with what the user pays now. */
data class OptionEstimate(
    val option: AccountOption,
    val yearlyCost: Money,
    /** Positive = cheaper than today. Negative = would cost more. */
    val yearlySavings: Money,
)

/**
 * Simple, transparent yearly fee estimates. Every rule is visible here and in the app:
 *
 *  Current bank account: maintenance fee x months below minimum
 *                       + overdraft fee x overdrafts a year
 *                       + out-of-network ATM fee x withdrawals a year
 *  No bank account:      current alternative-service cost x 12, assuming the paycheck could be
 *                        deposited directly so check-cashing and money-order fees stop
 *  Each option:          monthly fee x 12 (zero if waived and the user has direct deposit)
 *                       + overdraft fee x overdrafts (zero if the account has no overdraft fee)
 *                       + its out-of-network ATM fee x withdrawals a year
 *
 * Estimates only. Real costs depend on each account's full terms and how the user banks.
 */
object SavingsEstimator {

    fun currentYearlyCost(account: BankAccount?, inputs: SavingsInputs): Money {
        if (account == null) return inputs.currentAltServiceMonthly * 12
        val maintenance = (account.maintenanceFee ?: Money.Zero) * inputs.monthsBelowMinimum.coerceIn(0, 12)
        val overdrafts = account.overdraftFee * inputs.overdraftsPerYear.coerceAtLeast(0)
        val atm = account.outOfNetworkAtmFee * (inputs.outOfNetworkAtmPerMonth.coerceAtLeast(0) * 12)
        return maintenance + overdrafts + atm
    }

    fun optionYearlyCost(option: AccountOption, inputs: SavingsInputs): Money {
        val waived = option.monthlyFeeWaivedWithDirectDeposit && inputs.hasDirectDeposit
        val monthly = if (waived) Money.Zero else option.monthlyFee * 12
        val overdrafts = (option.overdraftFee ?: Money.Zero) * inputs.overdraftsPerYear.coerceAtLeast(0)
        val atm = option.outOfNetworkAtmFee * (inputs.outOfNetworkAtmPerMonth.coerceAtLeast(0) * 12)
        return monthly + overdrafts + atm
    }

    /**
     * Every option with its estimate, best savings first. Ties go to the lower monthly fee,
     * then the name. Partner status is deliberately NOT part of the ordering.
     */
    fun rank(options: List<AccountOption>, account: BankAccount?, inputs: SavingsInputs): List<OptionEstimate> {
        val current = currentYearlyCost(account, inputs)
        return options
            .map { option ->
                val cost = optionYearlyCost(option, inputs)
                OptionEstimate(option, cost, current - cost)
            }
            .sortedWith(
                compareByDescending<OptionEstimate> { it.yearlySavings.cents }
                    .thenBy { it.option.monthlyFee.cents }
                    .thenBy { it.option.name },
            )
    }
}
