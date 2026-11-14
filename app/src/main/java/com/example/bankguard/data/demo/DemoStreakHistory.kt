package com.example.bankguard.data.demo

import com.example.bankguard.model.Money

/**
 * DEMO HISTORY for Overdraft-Free Streaks. The demo account has no day-by-day balance history,
 * so these are fixed, clearly labeled sample figures: the lowest available balance on each of
 * Alex's six completed days before the demo starts. The last one ends at today's $142.90.
 * Days are numbered on the simulated demo calendar; day 7 is "today" when the demo starts.
 */
object DemoStreakHistory {

    const val START_DAY = 7

    val lowestBalances: Map<Int, Money?> = mapOf(
        1 to Money.of(318.45),
        2 to Money.of(276.10),
        3 to Money.of(241.85),
        4 to Money.of(205.30),
        5 to Money.of(171.40),
        6 to Money.of(142.90),
    )
}
