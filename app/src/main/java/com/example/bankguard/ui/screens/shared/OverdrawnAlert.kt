package com.example.bankguard.ui.screens.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.example.bankguard.R
import com.example.bankguard.domain.recovery.OverdraftCause
import com.example.bankguard.ui.components.AlertCard
import com.example.bankguard.ui.components.PrimaryButton
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.util.relativeDay

/** Shown on Home and Prevent when an overdraft has ALREADY happened. Points to Recover. */
@Composable
fun OverdrawnAlert(cause: OverdraftCause, onOpenRecover: () -> Unit) {
    val res = LocalResources.current
    AlertCard(
        title = stringResource(R.string.ub_overdrawn_title),
        body = stringResource(
            R.string.ub_overdrawn_body,
            cause.transaction.merchant,
            cause.transaction.amount.absolute.format(),
            cause.balanceBefore.format(),
            cause.balanceAfter.format(),
        ),
        badgeText = stringResource(R.string.ub_overdrawn_badge),
        tone = StatusTone.Critical,
        iconRes = R.drawable.ic_error,
        meta = res.relativeDay(cause.transaction.dayOffset),
    ) {
        PrimaryButton(
            text = stringResource(R.string.ub_overdrawn_action),
            onClick = onOpenRecover,
            trailingIcon = R.drawable.ic_arrow_forward,
        )
    }
}
