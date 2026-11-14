package com.example.bankguard.ui.screens.prevent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.domain.cancellation.withCancellations
import com.example.bankguard.domain.forecast.CashFlowForecaster
import com.example.bankguard.model.CancellationEligibility
import com.example.bankguard.model.CancellationStatus
import com.example.bankguard.model.Money
import com.example.bankguard.model.Transaction
import com.example.bankguard.ui.components.BulletList
import com.example.bankguard.ui.components.ConfirmDialog
import com.example.bankguard.ui.components.IconBox
import com.example.bankguard.ui.components.InfoNote
import com.example.bankguard.ui.components.InsetPanel
import com.example.bankguard.ui.components.LoadingState
import com.example.bankguard.ui.components.MoneyRow
import com.example.bankguard.ui.components.PrimaryButton
import com.example.bankguard.ui.components.ScreenColumn
import com.example.bankguard.ui.components.SectionHeader
import com.example.bankguard.ui.components.StatusBadge
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.StepState
import com.example.bankguard.ui.components.StepTracker
import com.example.bankguard.ui.components.SubScreenHeader
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.components.TonalButton
import com.example.bankguard.ui.components.TrackerStep
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.theme.Spacing
import com.example.bankguard.ui.util.iconRes
import com.example.bankguard.ui.util.relativeDay

private const val HORIZON_DAYS = 7

/**
 * Review, confirm and track a cancellation request for ONE pending charge.
 * Nothing is sent until the user confirms in a dialog, and the result comes from the
 * simulated partner bank. The forecast changes only after an approval.
 *
 * @param transaction the charge as originally loaded (it disappears from [profile] once approved)
 * @param profile the profile with approved cancellations already applied
 */
@Composable
fun CancellationScreen(
    transaction: Transaction,
    profile: DemoProfile,
    status: CancellationStatus?,
    onRequest: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val res = LocalResources.current
    var confirming by rememberSaveable { mutableStateOf(false) }
    val eligibility = transaction.cancellation

    ScreenColumn(modifier = modifier) {
        SubScreenHeader(title = stringResource(R.string.cx_title), onBack = onBack)

        // The charge
        SurfaceCard {
            MoneyRow(
                iconRes = transaction.iconRes(),
                title = transaction.merchant,
                // Once approved it's no longer pending; the badge below says what happened.
                subtitle = if (status is CancellationStatus.Approved) {
                    res.relativeDay(transaction.dayOffset)
                } else {
                    stringResource(R.string.cx_pending_since, res.relativeDay(transaction.dayOffset))
                },
                amount = transaction.amount.formatSigned(),
            )
            val badge = chargeBadge(transaction, status)
            StatusBadge(stringResource(badge.textRes), badge.tone)
        }

        when (status) {
            null -> when (eligibility) {
                CancellationEligibility.Eligible -> ReviewContent(
                    transaction = transaction,
                    profile = profile,
                    onRequest = { confirming = true },
                    onKeep = onBack,
                )
                is CancellationEligibility.NotEligible -> NotEligibleContent(eligibility.reason, onBack)
                null -> NotEligibleContent(stringResource(R.string.status_not_eligible), onBack)
            }
            CancellationStatus.Processing -> {
                LoadingState(message = stringResource(R.string.cx_processing))
                Steps(StepState.Done, StepState.Current, StepState.Upcoming)
            }
            is CancellationStatus.Approved -> ApprovedContent(status.referenceId, profile, onBack)
            is CancellationStatus.Declined -> DeclinedContent(status.reason, onBack)
        }
    }

    if (confirming) {
        ConfirmDialog(
            title = stringResource(R.string.cx_confirm_title),
            body = stringResource(R.string.cx_confirm_body, transaction.merchant, transaction.amount.absolute.format()),
            confirmLabel = stringResource(R.string.cx_confirm_yes),
            dismissLabel = stringResource(R.string.cx_confirm_no),
            onConfirm = {
                confirming = false
                onRequest()
            },
            onDismiss = { confirming = false },
        )
    }
}

@Composable
private fun ReviewContent(
    transaction: Transaction,
    profile: DemoProfile,
    onRequest: () -> Unit,
    onKeep: () -> Unit,
) {
    val before = checkNotNull(profile.availableBalance)
    val after = remember(profile, transaction) {
        profile.withCancellations(mapOf(transaction.id to CancellationStatus.Approved("preview")))
    }
    val lowestBefore = remember(profile) { CashFlowForecaster.forecast(before, profile.scheduled, HORIZON_DAYS).lowest.balance }
    val lowestAfter = remember(after) { CashFlowForecaster.forecast(after.availableBalance!!, after.scheduled, HORIZON_DAYS).lowest.balance }

    InsetPanel(tone = StatusTone.Info) {
        Text(stringResource(R.string.cx_eligible_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.cx_eligible_body), style = MaterialTheme.typography.bodySmall)
    }

    SectionHeader(title = stringResource(R.string.cx_impact_title))
    SurfaceCard(verticalSpacing = Spacing.md) {
        ImpactRow(stringResource(R.string.cx_impact_available), before, after.availableBalance!!)
        HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
        ImpactRow(stringResource(R.string.cx_impact_lowest), lowestBefore, lowestAfter)
    }

    SectionHeader(title = stringResource(R.string.cx_steps_title))
    Steps(StepState.Upcoming, StepState.Upcoming, StepState.Upcoming)

    PrimaryButton(text = stringResource(R.string.cx_request), onClick = onRequest)
    TonalButton(text = stringResource(R.string.cx_keep), onClick = onKeep)
}

@Composable
private fun ImpactRow(label: String, before: Money, after: Money) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.cx_impact_change, before.format(), after.format()),
            style = MoneyText.row,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Steps(confirm: StepState, review: StepState, decision: StepState) {
    SurfaceCard {
        StepTracker(
            listOf(
                TrackerStep(stringResource(R.string.cx_step_confirm), stringResource(R.string.cx_step_confirm_body), confirm),
                TrackerStep(stringResource(R.string.cx_step_review), stringResource(R.string.cx_step_review_body), review),
                TrackerStep(stringResource(R.string.cx_step_decision), stringResource(R.string.cx_step_decision_body), decision),
            ),
        )
    }
}

@Composable
private fun NotEligibleContent(reason: String, onBack: () -> Unit) {
    InsetPanel(tone = StatusTone.Neutral) {
        Text(stringResource(R.string.cx_not_eligible_title), style = MaterialTheme.typography.titleSmall)
        Text(reason, style = MaterialTheme.typography.bodySmall)
    }
    TonalButton(text = stringResource(R.string.cx_done), onClick = onBack)
}

@Composable
private fun ApprovedContent(referenceId: String, profile: DemoProfile, onBack: () -> Unit) {
    val lowest = remember(profile) {
        CashFlowForecaster.forecast(profile.availableBalance!!, profile.scheduled, HORIZON_DAYS).lowest.balance
    }
    ResultCard(
        iconRes = R.drawable.ic_check_circle,
        tone = StatusTone.Success,
        title = stringResource(R.string.cx_approved_title),
        body = stringResource(R.string.cx_approved_body),
    ) {
        Text(stringResource(R.string.cx_reference, referenceId), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    InfoNote(text = stringResource(R.string.cx_approved_forecast, lowest.format()), tone = StatusTone.Success, iconRes = R.drawable.ic_insights)
    Steps(StepState.Done, StepState.Done, StepState.Done)
    PrimaryButton(text = stringResource(R.string.cx_done), onClick = onBack)
}

@Composable
private fun DeclinedContent(reason: String, onBack: () -> Unit) {
    ResultCard(
        iconRes = R.drawable.ic_error,
        tone = StatusTone.Critical,
        title = stringResource(R.string.cx_declined_title),
        body = stringResource(R.string.cx_declined_body, reason),
    ) {
        Text(stringResource(R.string.cx_declined_forecast), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    InsetPanel(tone = StatusTone.Info) {
        Text(stringResource(R.string.pv_things_title), style = MaterialTheme.typography.titleSmall)
        BulletList(listOf(stringResource(R.string.cx_declined_tip_merchant), stringResource(R.string.cx_declined_tip_other)))
    }
    PrimaryButton(text = stringResource(R.string.cx_done), onClick = onBack)
}

@Composable
private fun ResultCard(
    iconRes: Int,
    tone: StatusTone,
    title: String,
    body: String,
    extra: @Composable () -> Unit,
) {
    SurfaceCard(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBox(iconRes = iconRes, tone = tone)
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Column { extra() }
    }
}

@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun CancellationReviewPreview() {
    val alex = DemoProfiles.underbanked
    BankGuardTheme {
        CancellationScreen(alex.pendingTransactions.first(), alex, status = null, onRequest = {}, onBack = {})
    }
}
