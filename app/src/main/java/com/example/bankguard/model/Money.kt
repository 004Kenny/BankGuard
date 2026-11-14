package com.example.bankguard.model

import java.text.NumberFormat
import java.util.Locale

/**
 * A US dollar amount stored as whole cents, so sums never pick up floating-point error.
 * Negative values are money going out.
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {
    operator fun plus(other: Money) = Money(cents + other.cents)
    operator fun minus(other: Money) = Money(cents - other.cents)
    operator fun unaryMinus() = Money(-cents)
    operator fun times(factor: Int) = Money(cents * factor)
    override fun compareTo(other: Money) = cents.compareTo(other.cents)

    val isNegative: Boolean get() = cents < 0
    val absolute: Money get() = Money(kotlin.math.abs(cents))

    /** "$1,420.50", or "-$42.10" when negative. */
    fun format(): String {
        val text = currencyFormat().format(kotlin.math.abs(cents) / 100.0)
        return if (cents < 0) "-$text" else text
    }

    /** Always shows a sign: "+$1,650.00" or "-$185.00". Zero shows as "$0.00". */
    fun formatSigned(): String = when {
        cents > 0 -> "+" + format()
        else -> format()
    }

    companion object {
        val Zero = Money(0)

        /** From a dollar amount written in code, e.g. `Money.of(14.99)`. Rounds to the nearest cent. */
        fun of(dollars: Double): Money = Money(Math.round(dollars * 100))

        private fun currencyFormat(): NumberFormat =
            NumberFormat.getCurrencyInstance(Locale.US).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
    }
}

fun Iterable<Money>.sum(): Money = fold(Money.Zero) { acc, m -> acc + m }
