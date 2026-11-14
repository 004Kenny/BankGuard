package com.example.bankguard.ui.screens.prevent

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.model.CancellationEligibility
import com.example.bankguard.model.CancellationStatus
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.Transaction
import com.example.bankguard.ui.BankGuardTab
import com.example.bankguard.ui.components.StatusTone

/** Prevent tab: overdraft prevention for the underbanked persona, cash planning for the unbanked. */
@Composable
fun PreventScreen(
    profile: DemoProfile,
    cancellations: Map<String, CancellationStatus>,
    onOpenForecast: () -> Unit,
    onOpenCancellation: (transactionId: String) -> Unit,
    onOpenTab: (BankGuardTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (profile.persona) {
        DemoPersona.Underbanked -> UnderbankedPrevent(profile, cancellations, onOpenForecast, onOpenCancellation, modifier)
        DemoPersona.Unbanked -> UnbankedPrevent(profile, onOpenTab, modifier)
    }
}

/** Badge text and tone for a pending charge, from its eligibility and any request made. */
internal data class ChargeBadge(@param:StringRes val textRes: Int, val tone: StatusTone)

internal fun chargeBadge(transaction: Transaction, status: CancellationStatus?): ChargeBadge = when (status) {
    CancellationStatus.Processing -> ChargeBadge(R.string.status_processing, StatusTone.Warning)
    is CancellationStatus.Approved -> ChargeBadge(R.string.status_approved, StatusTone.Success)
    is CancellationStatus.Declined -> ChargeBadge(R.string.status_declined, StatusTone.Critical)
    null -> when (transaction.cancellation) {
        CancellationEligibility.Eligible -> ChargeBadge(R.string.status_eligible, StatusTone.Info)
        else -> ChargeBadge(R.string.status_not_eligible, StatusTone.Neutral)
    }
}
