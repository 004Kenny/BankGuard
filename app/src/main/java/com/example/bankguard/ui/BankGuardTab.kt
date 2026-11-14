package com.example.bankguard.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.bankguard.R

/** The four top-level destinations shown in the bottom navigation bar. */
enum class BankGuardTab(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    @param:DrawableRes val selectedIconRes: Int,
) {
    Home(R.string.nav_home, R.drawable.ic_grid_view, R.drawable.ic_grid_view_filled),
    Prevent(R.string.nav_prevent, R.drawable.ic_shield, R.drawable.ic_shield_filled),
    Recover(R.string.nav_recover, R.drawable.ic_published_with_changes, R.drawable.ic_published_with_changes_filled),
    Discover(R.string.nav_discover, R.drawable.ic_explore, R.drawable.ic_explore_filled),
}
