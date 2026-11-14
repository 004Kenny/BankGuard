package com.example.bankguard.ui.util

import java.text.DateFormat
import java.util.Calendar
import java.util.Locale

/** A day offset from today as a long date, e.g. "November 13, 2026". Works on API 24 without desugaring. */
fun longDate(dayOffset: Int, locale: Locale = Locale.getDefault()): String {
    val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
    return DateFormat.getDateInstance(DateFormat.LONG, locale).format(calendar.time)
}
