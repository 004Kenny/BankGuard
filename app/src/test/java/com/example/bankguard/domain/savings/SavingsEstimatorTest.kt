package com.example.bankguard.domain.savings

import com.example.bankguard.data.demo.DemoAccountOptions
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsEstimatorTest {

    private val account = DemoProfiles.underbanked.account!!
    private val underbankedInputs = DemoAccountOptions.defaultInputs(DemoPersona.Underbanked)
    private val starter = DemoAccountOptions.all.single { it.id == "starter" }
    private val basic = DemoAccountOptions.all.single { it.id == "basic" }
    private val secondChance = DemoAccountOptions.all.single { it.id == "second-chance" }

    @Test fun currentAccountCostAddsMaintenanceOverdraftsAndAtm() {
        // $12 x 12 + $35 x 4 + $3.50 x 12 = 144 + 140 + 42
        assertEquals(Money.of(326.00), SavingsEstimator.currentYearlyCost(account, underbankedInputs))
    }

    @Test fun noAccountCostIsAltServicesTimesTwelve() {
        val inputs = DemoAccountOptions.defaultInputs(DemoPersona.Unbanked)
        assertEquals(Money.of(1782.00), SavingsEstimator.currentYearlyCost(null, inputs))
    }

    @Test fun waiverAppliesOnlyWithDirectDeposit() {
        val withDd = underbankedInputs.copy(outOfNetworkAtmPerMonth = 0)
        assertEquals(Money.Zero, SavingsEstimator.optionYearlyCost(basic, withDd))
        assertEquals(Money.of(60.00), SavingsEstimator.optionYearlyCost(basic, withDd.copy(hasDirectDeposit = false)))
    }

    @Test fun accountsWithoutOverdraftFeesChargeNothingForOverdrafts() {
        val inputs = underbankedInputs.copy(overdraftsPerYear = 10, outOfNetworkAtmPerMonth = 0)
        assertEquals(Money.Zero, SavingsEstimator.optionYearlyCost(starter, inputs))
    }

    @Test fun savingsCanBeNegative() {
        val lightUser = underbankedInputs.copy(overdraftsPerYear = 0, monthsBelowMinimum = 0, outOfNetworkAtmPerMonth = 0)
        val estimate = SavingsEstimator.rank(listOf(secondChance), account, lightUser).single()
        assertEquals(Money.of(-119.40), estimate.yearlySavings)
    }

    @Test fun rankingIsByEstimatedSavingsNotPartnerStatus() {
        // Make the partner account the most expensive; it must then rank last.
        val pricierPartner = starter.copy(monthlyFee = Money.of(20.00))
        val ranked = SavingsEstimator.rank(listOf(pricierPartner, basic, secondChance), account, underbankedInputs)
        assertEquals(listOf("basic", "second-chance", "starter"), ranked.map { it.option.id })
        assertTrue(ranked.zipWithNext().all { (a, b) -> a.yearlySavings >= b.yearlySavings })
    }

    @Test fun demoRankingForUnderbanked() {
        val ranked = SavingsEstimator.rank(DemoAccountOptions.all, account, underbankedInputs)
        // starter: 0 + 0 + 2 x 12 = 24 ; basic: 0 (waived) + 0 + 2.50 x 12 = 30 ; second chance: 119.40 + 36
        assertEquals(listOf("starter", "basic", "second-chance"), ranked.map { it.option.id })
        assertEquals(Money.of(302.00), ranked.first().yearlySavings)
    }
}
