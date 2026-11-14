package com.example.bankguard.domain.forecast

import com.example.bankguard.model.Money
import com.example.bankguard.model.ScheduledItem

/** Projected end-of-day balance for one day. Day 0 is today. */
data class ForecastDay(val dayOffset: Int, val balance: Money)

data class CashFlowForecast(
    val days: List<ForecastDay>,
) {
    val lowest: ForecastDay get() = days.minBy { it.balance.cents }
    val ending: ForecastDay get() = days.last()

    /** First day the balance goes below zero, or null if it never does. */
    val firstNegativeDay: ForecastDay? get() = days.firstOrNull { it.balance.isNegative }
    val hasOverdraftRisk: Boolean get() = firstNegativeDay != null
}

/**
 * Deliberately simple, rules-based projection: start from the available balance and apply
 * each known scheduled deposit or bill on its day. It does not guess at day-to-day spending.
 * This is a demo of the idea, not a financial prediction.
 */
object CashFlowForecaster {

    fun forecast(
        startingBalance: Money,
        scheduled: List<ScheduledItem>,
        horizonDays: Int = 7,
    ): CashFlowForecast {
        require(horizonDays >= 1) { "horizonDays must be at least 1" }
        var balance = startingBalance
        val days = (0..horizonDays).map { day ->
            scheduled.filter { it.dayOffset == day }.forEach { balance += it.amount }
            ForecastDay(day, balance)
        }
        return CashFlowForecast(days)
    }
}
