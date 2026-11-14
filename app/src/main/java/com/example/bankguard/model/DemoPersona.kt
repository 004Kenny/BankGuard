package com.example.bankguard.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.bankguard.R

/** The two demo personas BankGuard is designed around. */
enum class DemoPersona(
    @param:StringRes val titleRes: Int,
    @param:StringRes val modeRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:StringRes val preloadedRes: Int,
    @param:StringRes val switchToRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    /** Has a bank account but pays overdraft, minimum-balance and similar fees. */
    Underbanked(
        R.string.persona_underbanked_title,
        R.string.persona_underbanked_mode,
        R.string.persona_underbanked_body,
        R.string.persona_underbanked_preloaded,
        R.string.profile_switch_underbanked,
        R.drawable.ic_account_balance,
    ),

    /** Has no bank account and relies on check cashers, prepaid cards and money orders. */
    Unbanked(
        R.string.persona_unbanked_title,
        R.string.persona_unbanked_mode,
        R.string.persona_unbanked_body,
        R.string.persona_unbanked_preloaded,
        R.string.profile_switch_unbanked,
        R.drawable.ic_payments,
    ),
}
