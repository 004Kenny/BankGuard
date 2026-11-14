package com.example.bankguard.ui.util

import android.content.res.Resources
import com.example.bankguard.R

/** "Today", "Tomorrow", "In 3 days", "Yesterday", "5 days ago" for a day offset. */
fun Resources.relativeDay(dayOffset: Int): String = when {
    dayOffset == 0 -> getString(R.string.day_today)
    dayOffset == 1 -> getString(R.string.day_tomorrow)
    dayOffset == -1 -> getString(R.string.day_yesterday)
    dayOffset > 1 -> getQuantityString(R.plurals.day_in_n, dayOffset, dayOffset)
    else -> getQuantityString(R.plurals.day_n_ago, -dayOffset, -dayOffset)
}
