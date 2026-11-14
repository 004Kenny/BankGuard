package com.example.bankguard.ui.screens.recover

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.bankguard.R
import com.example.bankguard.data.demo.AccountScenario
import com.example.bankguard.model.EligibilityCheck
import com.example.bankguard.model.FeeCollectionStatus
import com.example.bankguard.model.RecoveryEligibility
import com.example.bankguard.model.RecoveryMechanism
import com.example.bankguard.model.RecoveryRecord
import com.example.bankguard.model.RecoveryRequestStatus
import com.example.bankguard.model.TransactionStatus
import com.example.bankguard.ui.components.StatusBadge
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.theme.Spacing
import com.example.bankguard.ui.util.relativeDay
import androidx.compose.ui.platform.LocalResources
import java.text.DateFormat
import java.util.Date

internal data class RecoveryBadge(@param:StringRes val textRes: Int, val tone: StatusTone)

/** One badge for a charge: the request's status if there is one, otherwise the eligibility check's. */
internal fun recoveryBadge(record: RecoveryRecord?, check: EligibilityCheck?): RecoveryBadge = when (record?.status) {
    RecoveryRequestStatus.Submitted -> RecoveryBadge(R.string.rv_badge_submitted, StatusTone.Warning)
    RecoveryRequestStatus.AwaitingConfirmation -> RecoveryBadge(R.string.rv_badge_awaiting, StatusTone.Warning)
    RecoveryRequestStatus.Rejected -> RecoveryBadge(R.string.rv_badge_rejected, StatusTone.Critical)
    RecoveryRequestStatus.Confirmed -> when (record.mechanism) {
        RecoveryMechanism.AuthorizationHoldRelease -> RecoveryBadge(R.string.rv_badge_released, StatusTone.Success)
        RecoveryMechanism.PostedTransactionReversal -> RecoveryBadge(R.string.rv_badge_reversed, StatusTone.Success)
    }
    null -> when (check) {
        null -> RecoveryBadge(R.string.rv_badge_not_checked, StatusTone.Neutral)
        EligibilityCheck.Checking -> RecoveryBadge(R.string.rv_badge_checking, StatusTone.Info)
        is EligibilityCheck.Failed -> RecoveryBadge(R.string.rv_badge_check_failed, StatusTone.Critical)
        is EligibilityCheck.Done -> when (check.result) {
            is RecoveryEligibility.Eligible -> RecoveryBadge(R.string.rv_badge_eligible, StatusTone.Info)
            is RecoveryEligibility.Ineligible -> RecoveryBadge(R.string.rv_badge_ineligible, StatusTone.Neutral)
        }
    }
}

@StringRes
internal fun RecoveryMechanism.nameRes(): Int = when (this) {
    RecoveryMechanism.AuthorizationHoldRelease -> R.string.rv_mechanism_hold
    RecoveryMechanism.PostedTransactionReversal -> R.string.rv_mechanism_reversal
}

@StringRes
internal fun TransactionStatus.labelRes(): Int = when (this) {
    TransactionStatus.Pending -> R.string.rv_txn_pending
    TransactionStatus.Posted -> R.string.rv_txn_posted
    TransactionStatus.Released -> R.string.rv_txn_released
}

/** What the original charge is now, after any confirmed recovery. */
@StringRes
internal fun RecoveryRecord.transactionStatusRes(): Int = when {
    !isConfirmed -> originalStatus.labelRes()
    mechanism == RecoveryMechanism.AuthorizationHoldRelease -> R.string.rv_txn_released
    else -> R.string.rv_txn_reversed
}

@StringRes
internal fun FeeCollectionStatus.labelRes(): Int = when (this) {
    FeeCollectionStatus.NotCharged -> R.string.rv_fee_status_none
    FeeCollectionStatus.Assessed -> R.string.rv_fee_status_assessed
    FeeCollectionStatus.Collected -> R.string.rv_fee_status_collected
    FeeCollectionStatus.CollectionFailed -> R.string.rv_fee_status_failed
}

@StringRes
internal fun AccountScenario.titleRes(): Int = when (this) {
    AccountScenario.UpcomingBill -> R.string.rv_sc_upcoming
    AccountScenario.PendingRelease -> R.string.rv_sc_a
    AccountScenario.PostedReversal -> R.string.rv_sc_b
    AccountScenario.OutsideWindow -> R.string.rv_sc_c
    AccountScenario.BankRejects -> R.string.rv_sc_d
    AccountScenario.AwaitingConfirmation -> R.string.rv_sc_e
}

@StringRes
internal fun AccountScenario.bodyRes(): Int = when (this) {
    AccountScenario.UpcomingBill -> R.string.rv_sc_upcoming_body
    AccountScenario.PendingRelease -> R.string.rv_sc_a_body
    AccountScenario.PostedReversal -> R.string.rv_sc_b_body
    AccountScenario.OutsideWindow -> R.string.rv_sc_c_body
    AccountScenario.BankRejects -> R.string.rv_sc_d_body
    AccountScenario.AwaitingConfirmation -> R.string.rv_sc_e_body
}

internal fun formatTimestamp(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

/** Label on the left, figure on the right; [strong] for totals. */
@Composable
internal fun BreakdownRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            label,
            style = if (strong) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (strong) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        // Capped, so a long value wraps instead of squeezing the label.
        Text(
            value,
            style = if (strong) MoneyText.row.copy(fontWeight = FontWeight.Bold) else MoneyText.row,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 200.dp),
        )
    }
}

@Composable
internal fun moneyColor(negative: Boolean): Color =
    if (negative) BankGuardTheme.colors.critical else MaterialTheme.colorScheme.onSurface

/** One request in the Recovery history list. */
@Composable
internal fun RecoveryHistoryCard(record: RecoveryRecord, onClick: () -> Unit) {
    val res = LocalResources.current
    val badge = recoveryBadge(record, null)
    SurfaceCard(onClick = onClick, verticalSpacing = Spacing.sm) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.merchant, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${record.transactionAmount.formatSigned()} · ${res.relativeDay(record.transactionDayOffset)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusBadge(stringResource(badge.textRes), badge.tone)
        }
        HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
        BreakdownRow(stringResource(R.string.rv_h_restored), record.amountRestored.formatSigned())
        BreakdownRow(stringResource(R.string.rv_h_fee_assessed), record.feeAssessed.format())
        BreakdownRow(stringResource(R.string.rv_h_fee_collected), record.feeCollected.format())
        record.requestReference?.let { BreakdownRow(stringResource(R.string.rv_r_request_ref), it) }
        record.confirmationReference?.let { BreakdownRow(stringResource(R.string.rv_r_reference), it) }
        record.rejectionReason?.let {
            Text(stringResource(R.string.rv_rejected_reason, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            stringResource(R.string.rv_h_submitted, formatTimestamp(record.submittedAtMillis)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Everything the Recover screens show about transaction recovery, for the current scenario. */
data class RecoveryUiState(
    /** Requests in the current scenario, by transaction id. */
    val records: Map<String, RecoveryRecord> = emptyMap(),
    val eligibility: Map<String, EligibilityCheck> = emptyMap(),
    /** Requests whose status is being re-checked right now. */
    val statusChecks: Set<String> = emptySet(),
    val scenario: AccountScenario = AccountScenario.UpcomingBill,
)
