package com.example.bankguard.ui.screens.recover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankguard.R
import com.example.bankguard.data.demo.AccountScenario
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.domain.recovery.OverdraftCause
import com.example.bankguard.domain.recovery.RecoveryFeeCalculator
import com.example.bankguard.domain.recovery.RecoveryQuote
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.EligibilityCheck
import com.example.bankguard.model.FeeCollectionStatus
import com.example.bankguard.model.Money
import com.example.bankguard.model.RecoveryEligibility
import com.example.bankguard.model.RecoveryMechanism
import com.example.bankguard.model.RecoveryRecord
import com.example.bankguard.model.RecoveryRequestStatus
import com.example.bankguard.model.Transaction
import com.example.bankguard.ui.components.AmountTone
import com.example.bankguard.ui.components.BulletList
import com.example.bankguard.ui.components.ErrorState
import com.example.bankguard.ui.components.IconBox
import com.example.bankguard.ui.components.InfoNote
import com.example.bankguard.ui.components.InsetPanel
import com.example.bankguard.ui.components.LoadingState
import com.example.bankguard.ui.components.MoneyRow
import com.example.bankguard.ui.components.PrimaryButton
import com.example.bankguard.ui.components.ScreenColumn
import com.example.bankguard.ui.components.SecondaryButton
import com.example.bankguard.ui.components.SectionHeader
import com.example.bankguard.ui.components.StatusBadge
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SubScreenHeader
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.components.TonalButton
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.Spacing
import com.example.bankguard.ui.util.iconRes
import com.example.bankguard.ui.util.longDate

/**
 * One overdraft-causing charge: eligibility, what recovery would mean (including BankGuard's
 * exact fee), explicit consent, then the simulated bank's processing and result.
 * Nothing is sent until the user ticks the fee consent and confirms.
 *
 * @param transaction the charge as originally loaded (its live status can change once recovered)
 * @param cause the current overdraft if this charge is causing it, otherwise null
 * @param record the recovery request for this charge, if one was made
 */
@Composable
fun RecoveryDetailScreen(
    transaction: Transaction,
    cause: OverdraftCause?,
    record: RecoveryRecord?,
    check: EligibilityCheck?,
    checkingStatus: Boolean,
    onCheckEligibility: () -> Unit,
    onRequest: (feeConsent: Boolean) -> Unit,
    onRefreshStatus: () -> Unit,
    onOpenFeeHelp: () -> Unit,
    onOpenPrevent: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }

    // Ask the bank as soon as the screen opens, unless there's already an answer or a request.
    LaunchedEffect(transaction.id, record == null, cause != null) {
        if (record == null && cause != null && check == null) onCheckEligibility()
    }

    ScreenColumn(modifier = modifier) {
        SubScreenHeader(title = stringResource(R.string.rv_detail_title), onBack = onBack)

        TransactionCard(transaction, cause, record, check)

        when {
            record != null -> when (record.status) {
                RecoveryRequestStatus.Submitted -> ProcessingContent()
                RecoveryRequestStatus.AwaitingConfirmation -> PendingContent(record, checkingStatus, onRefreshStatus, onBack)
                RecoveryRequestStatus.Confirmed -> ConfirmedContent(record, onBack)
                RecoveryRequestStatus.Rejected -> RejectedContent(record, onOpenFeeHelp, onOpenPrevent)
            }
            cause == null -> {
                InsetPanel { Text(stringResource(R.string.rv_not_cause), style = MaterialTheme.typography.bodyMedium) }
                TonalButton(stringResource(R.string.rv_done), onClick = onBack)
            }
            else -> when (check) {
                null, EligibilityCheck.Checking -> LoadingState(message = stringResource(R.string.rv_checking))
                is EligibilityCheck.Failed -> ErrorState(
                    title = stringResource(R.string.rv_check_failed_title),
                    body = check.message,
                    retryLabel = stringResource(R.string.rv_retry),
                    onRetry = onCheckEligibility,
                )
                is EligibilityCheck.Done -> when (val result = check.result) {
                    is RecoveryEligibility.Ineligible -> IneligibleContent(result.reason, onOpenFeeHelp, onOpenPrevent)
                    is RecoveryEligibility.Eligible -> AvailableContent(
                        quote = RecoveryQuote.of(cause),
                        eligible = result,
                        onRequest = { confirming = true },
                        onNotNow = onBack,
                    )
                }
            }
        }

        InfoNote(text = stringResource(R.string.rv_footer), tone = StatusTone.Demo, iconRes = R.drawable.ic_science)
    }

    if (confirming && cause != null && record == null) {
        ConfirmRecoveryDialog(
            transaction = transaction,
            quote = RecoveryQuote.of(cause),
            onConfirm = { consent ->
                confirming = false
                onRequest(consent)
            },
            onDismiss = { confirming = false },
        )
    }
}

@Composable
private fun TransactionCard(transaction: Transaction, cause: OverdraftCause?, record: RecoveryRecord?, check: EligibilityCheck?) {
    val before = record?.balanceBefore ?: cause?.balanceBefore
    val after = record?.balanceAfter ?: cause?.balanceAfter
    val badge = recoveryBadge(record, check)
    val statusRes = record?.transactionStatusRes() ?: transaction.status.labelRes()
    SurfaceCard(verticalSpacing = Spacing.sm) {
        MoneyRow(
            iconRes = transaction.iconRes(),
            title = transaction.merchant,
            subtitle = stringResource(statusRes),
            amount = transaction.amount.formatSigned(),
            amountTone = AmountTone.Negative,
        )
        HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
        BreakdownRow(stringResource(R.string.rv_d_date), longDate(transaction.dayOffset))
        BreakdownRow(stringResource(R.string.rv_d_status), stringResource(statusRes))
        if (before != null) BreakdownRow(stringResource(R.string.rv_d_before), before.format(), valueColor = moneyColor(before.isNegative))
        if (after != null) BreakdownRow(stringResource(R.string.rv_d_after), after.format(), valueColor = moneyColor(after.isNegative))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.rv_d_eligibility),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            StatusBadge(stringResource(badge.textRes), badge.tone)
        }
    }
}

@Composable
private fun AvailableContent(
    quote: RecoveryQuote,
    eligible: RecoveryEligibility.Eligible,
    onRequest: () -> Unit,
    onNotNow: () -> Unit,
) {
    SectionHeader(title = stringResource(R.string.rv_available_title))
    SurfaceCard(verticalSpacing = Spacing.sm, modifier = Modifier.testTag("recovery_available")) {
        StatusBadge(stringResource(R.string.rv_mechanism_label, stringResource(eligible.mechanism.nameRes())), StatusTone.Info)
        Text(eligible.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
        BreakdownRow(stringResource(R.string.rv_b_transaction), quote.transactionAmount.absolute.format())
        BreakdownRow(stringResource(R.string.rv_b_original_balance), quote.balanceBefore.format())
        BreakdownRow(stringResource(R.string.rv_b_current), quote.currentBalance.format(), valueColor = moneyColor(quote.currentBalance.isNegative))
        BreakdownRow(stringResource(R.string.rv_b_potential), quote.expectedRestore.formatSigned(), valueColor = BankGuardTheme.colors.success)
        BreakdownRow(stringResource(R.string.rv_b_fee), (-quote.fee).format())
        HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
        BreakdownRow(stringResource(R.string.rv_b_final), quote.estimatedFinalBalance.format(), strong = true)
    }
    FeeExplanation(quote)
    PrimaryButton(stringResource(R.string.rv_request), onClick = onRequest)
    TonalButton(stringResource(R.string.rv_not_now), onClick = onNotNow)
}

@Composable
private fun FeeExplanation(quote: RecoveryQuote) {
    InsetPanel(tone = StatusTone.Info) {
        if (quote.fee == Money.Zero) {
            Text(stringResource(R.string.rv_fee_zero, quote.balanceBefore.format()), style = MaterialTheme.typography.bodySmall)
        } else {
            Text(
                stringResource(
                    R.string.rv_fee_explainer,
                    RecoveryFeeCalculator.RATE_PERCENT,
                    RecoveryFeeCalculator.feeBase(quote.balanceBefore).format(),
                    quote.fee.format(),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(stringResource(R.string.rv_fee_not_amount, quote.transactionAmount.absolute.format()), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Explicit confirmation with a required consent tick for the exact proposed fee. */
@Composable
private fun ConfirmRecoveryDialog(
    transaction: Transaction,
    quote: RecoveryQuote,
    onConfirm: (consent: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var consent by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.ic_info), contentDescription = null, modifier = Modifier.size(28.dp)) },
        title = { Text(stringResource(R.string.rv_confirm_title), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Column {
                    Text(stringResource(R.string.rv_confirm_txn), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${transaction.merchant} · ${transaction.amount.formatSigned()}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                BreakdownRow(stringResource(R.string.rv_confirm_expected), quote.expectedRestore.format())
                BreakdownRow(stringResource(R.string.rv_confirm_fee), quote.fee.format(), strong = true)
                Text(stringResource(R.string.rv_confirm_terms), style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = consent, role = Role.Checkbox, onValueChange = { consent = it })
                        .testTag("fee_consent"),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Checkbox(
                        checked = consent,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary),
                    )
                    Text(
                        stringResource(
                            R.string.rv_confirm_consent,
                            quote.fee.format(),
                            RecoveryFeeCalculator.RATE_PERCENT,
                            quote.balanceBefore.format(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(consent) }, enabled = consent) {
                Text(stringResource(R.string.rv_confirm_yes), style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.rv_confirm_no), style = MaterialTheme.typography.labelLarge) }
        },
    )
}

@Composable
private fun ProcessingContent() {
    SurfaceCard {
        Text(
            stringResource(R.string.rv_processing_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        LoadingState(message = stringResource(R.string.rv_processing_body))
    }
}

@Composable
private fun PendingContent(record: RecoveryRecord, checking: Boolean, onRefresh: () -> Unit, onBack: () -> Unit) {
    ResultCard(iconRes = R.drawable.ic_schedule, tone = StatusTone.Warning, title = stringResource(R.string.rv_pending_title)) {
        Text(stringResource(R.string.rv_pending_status), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        record.requestReference?.let { BreakdownRow(stringResource(R.string.rv_r_request_ref), it) }
        BreakdownRow(stringResource(R.string.rv_d_status), stringResource(R.string.rv_badge_awaiting))
        BulletList(listOf(stringResource(R.string.rv_pending_no_balance), stringResource(R.string.rv_pending_no_fee)))
    }
    if (checking) {
        LoadingState(message = stringResource(R.string.rv_checking_status))
    } else {
        PrimaryButton(stringResource(R.string.rv_check_status), onClick = onRefresh)
    }
    TonalButton(stringResource(R.string.rv_done), onClick = onBack)
}

@Composable
private fun ConfirmedContent(record: RecoveryRecord, onBack: () -> Unit) {
    val hold = record.mechanism == RecoveryMechanism.AuthorizationHoldRelease
    ResultCard(iconRes = R.drawable.ic_check_circle, tone = StatusTone.Success, title = stringResource(R.string.rv_confirmed_title)) {
        Text(
            stringResource(if (hold) R.string.rv_confirmed_body_hold else R.string.rv_confirmed_body_reversal),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
        BreakdownRow(stringResource(R.string.rv_r_original), record.transactionAmount.formatSigned())
        BreakdownRow(stringResource(R.string.rv_r_original_balance), record.balanceBefore.format())
        BreakdownRow(stringResource(R.string.rv_d_after), record.balanceAfter.format(), valueColor = moneyColor(record.balanceAfter.isNegative))
        BreakdownRow(
            stringResource(if (hold) R.string.rv_r_released else R.string.rv_r_restored),
            record.amountRestored.formatSigned(),
            valueColor = BankGuardTheme.colors.success,
        )
        BreakdownRow(stringResource(R.string.rv_r_fee), (-record.feeCollected).format())
        HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
        BreakdownRow(stringResource(R.string.rv_r_final), record.balanceAfterRecovery.format(), strong = true)
    }
    SurfaceCard(verticalSpacing = Spacing.sm) {
        record.confirmationReference?.let { BreakdownRow(stringResource(R.string.rv_r_reference), it) }
        record.requestReference?.let { BreakdownRow(stringResource(R.string.rv_r_request_ref), it) }
        BreakdownRow(stringResource(R.string.rv_r_txn_status), stringResource(record.transactionStatusRes()))
        record.confirmedAtMillis?.let { BreakdownRow(stringResource(R.string.rv_r_confirmed_at), formatTimestamp(it)) }
        BreakdownRow(stringResource(R.string.rv_r_fee_status), stringResource(record.feeStatus.labelRes()))
    }
    when (record.feeStatus) {
        FeeCollectionStatus.CollectionFailed -> InfoNote(
            text = stringResource(R.string.rv_fee_failed, record.feeFailureReason.orEmpty()),
            tone = StatusTone.Warning,
            iconRes = R.drawable.ic_warning,
        )
        FeeCollectionStatus.Collected -> InfoNote(text = stringResource(R.string.rv_fee_separate))
        else -> Unit
    }
    PrimaryButton(stringResource(R.string.rv_done), onClick = onBack)
}

@Composable
private fun RejectedContent(record: RecoveryRecord, onOpenFeeHelp: () -> Unit, onOpenPrevent: () -> Unit) {
    ResultCard(iconRes = R.drawable.ic_error, tone = StatusTone.Critical, title = stringResource(R.string.rv_rejected_title)) {
        record.rejectionReason?.let {
            Text(stringResource(R.string.rv_rejected_reason, it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        BreakdownRow(stringResource(R.string.rv_r_original), record.transactionAmount.formatSigned())
        BreakdownRow(stringResource(R.string.rv_h_restored), Money.Zero.format())
        BreakdownRow(stringResource(R.string.rv_r_fee), Money.Zero.format(), strong = true)
        BulletList(listOf(stringResource(R.string.rv_rejected_nothing), stringResource(R.string.rv_rejected_fee, Money.Zero.format())))
        record.requestReference?.let { BreakdownRow(stringResource(R.string.rv_r_request_ref), it) }
    }
    NextSteps(onOpenFeeHelp, onOpenPrevent)
}

@Composable
private fun IneligibleContent(reason: String, onOpenFeeHelp: () -> Unit, onOpenPrevent: () -> Unit) {
    InsetPanel(tone = StatusTone.Neutral) {
        Text(
            stringResource(R.string.rv_ineligible_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(reason, style = MaterialTheme.typography.bodyMedium)
    }
    NextSteps(onOpenFeeHelp, onOpenPrevent)
}

@Composable
private fun NextSteps(onOpenFeeHelp: () -> Unit, onOpenPrevent: () -> Unit) {
    SurfaceCard(verticalSpacing = Spacing.md) {
        Text(
            stringResource(R.string.rv_next_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        BulletList(
            listOf(
                stringResource(R.string.rv_next_merchant),
                stringResource(R.string.rv_next_bank),
                stringResource(R.string.rv_next_fee),
                stringResource(R.string.rv_next_deposit),
            ),
        )
        SecondaryButton(stringResource(R.string.rv_open_fee_help), onClick = onOpenFeeHelp)
        TonalButton(stringResource(R.string.rv_open_prevent), onClick = onOpenPrevent)
    }
}

@Composable
private fun ResultCard(
    iconRes: Int,
    tone: StatusTone,
    title: String,
    content: @Composable () -> Unit,
) {
    SurfaceCard(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalSpacing = Spacing.sm) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBox(iconRes = iconRes, tone = tone)
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
        }
        content()
    }
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun RecoveryAvailablePreview() {
    val alex = DemoProfiles.forScenario(DemoPersona.Underbanked, AccountScenario.PendingRelease)
    val cause = com.example.bankguard.domain.recovery.OverdraftDetector.find(alex)!!
    BankGuardTheme {
        RecoveryDetailScreen(
            transaction = cause.transaction,
            cause = cause,
            record = null,
            check = EligibilityCheck.Done(RecoveryEligibility.Eligible(RecoveryMechanism.AuthorizationHoldRelease, "Preview explanation.")),
            checkingStatus = false,
            onCheckEligibility = {}, onRequest = {}, onRefreshStatus = {}, onOpenFeeHelp = {}, onOpenPrevent = {}, onBack = {},
        )
    }
}
