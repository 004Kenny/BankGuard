package com.example.bankguard.data.demo

import com.example.bankguard.model.AccountOption
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.Money
import com.example.bankguard.model.SavingsInputs

/**
 * Sample low-fee accounts for the Discover demo. EVERY account and institution is fictional,
 * modeled loosely on the features common to low-cost "starter" checking accounts.
 */
object DemoAccountOptions {

    val all: List<AccountOption> = listOf(
        AccountOption(
            id = "starter",
            name = "Starter Checking (sample)",
            institution = "Harbor Community Credit Union (demo)",
            bankId = "harbor-community-cu",
            monthlyFee = Money.Zero,
            monthlyFeeWaivedWithDirectDeposit = false,
            minimumBalance = Money.Zero,
            overdraftFee = null,
            openingDeposit = Money.of(25.00),
            outOfNetworkAtmFee = Money.of(2.00),
            secondChance = false,
            proposedPartner = true,
            features = listOf(
                "No overdraft fees: purchases that would overdraw are declined",
                "No monthly fee and no minimum balance",
                "Free direct deposit",
            ),
        ),
        AccountOption(
            id = "basic",
            name = "Everyday Basic (sample)",
            institution = "Neighborhood Bank (demo)",
            bankId = "neighborhood-bank",
            monthlyFee = Money.of(5.00),
            monthlyFeeWaivedWithDirectDeposit = true,
            minimumBalance = Money.Zero,
            overdraftFee = null,
            openingDeposit = Money.Zero,
            outOfNetworkAtmFee = Money.of(2.50),
            secondChance = false,
            proposedPartner = false,
            features = listOf(
                "No overdraft fees",
                "\$5 monthly fee, waived with any direct deposit",
                "No opening deposit",
            ),
        ),
        AccountOption(
            id = "second-chance",
            name = "Fresh Start Checking (sample)",
            institution = "Riverbend Bank (demo)",
            bankId = "riverbend-bank",
            monthlyFee = Money.of(9.95),
            monthlyFeeWaivedWithDirectDeposit = false,
            minimumBalance = Money.Zero,
            overdraftFee = null,
            openingDeposit = Money.of(10.00),
            outOfNetworkAtmFee = Money.of(3.00),
            secondChance = true,
            proposedPartner = false,
            features = listOf(
                "For people who've had trouble with a bank account before",
                "No overdraft fees",
                "\$9.95 monthly fee",
            ),
        ),
    )

    /**
     * Starting estimate inputs, from each demo profile's recent fees. Shown to the user as
     * assumptions they can change.
     */
    fun defaultInputs(persona: DemoPersona): SavingsInputs = when (persona) {
        DemoPersona.Underbanked -> SavingsInputs(
            overdraftsPerYear = 4,
            monthsBelowMinimum = 12,
            outOfNetworkAtmPerMonth = 1,
            hasDirectDeposit = true,
            currentAltServiceMonthly = Money.Zero,
        )
        DemoPersona.Unbanked -> SavingsInputs(
            overdraftsPerYear = 0,
            monthsBelowMinimum = 0,
            outOfNetworkAtmPerMonth = 2,
            hasDirectDeposit = true,
            currentAltServiceMonthly = DemoProfiles.unbanked.monthlyAltServiceCost,
        )
    }
}
