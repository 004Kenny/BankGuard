package com.example.bankguard.ui.screens.recover

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.model.BankFee
import com.example.bankguard.model.FeeRecovery
import com.example.bankguard.model.RecoveryStage
import com.example.bankguard.ui.components.ConfirmDialog
import com.example.bankguard.ui.components.HeroCard
import com.example.bankguard.ui.components.HeroLabel
import com.example.bankguard.ui.components.InfoNote
import com.example.bankguard.ui.components.PrimaryButton
import com.example.bankguard.ui.components.ScreenColumn
import com.example.bankguard.ui.components.SecondaryButton
import com.example.bankguard.ui.components.SectionHeader
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.StepState
import com.example.bankguard.ui.components.StepTracker
import com.example.bankguard.ui.components.SubScreenHeader
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.components.TonalButton
import com.example.bankguard.ui.components.TrackerStep
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.util.relativeDay
import com.example.bankguard.ui.util.text

/**
 * One fee: what it is, why it was charged, how to avoid it, and the user's own progress
 * asking for it back. Every step after "Fee found" is something the USER does or reports.
 */
@Composable
fun FeeDetailScreen(
    fee: BankFee,
    recovery: FeeRecovery?,
    onOpenLetter: () -> Unit,
    onMarkSent: () -> Unit,
    onRecordDecision: (refunded: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val res = LocalResources.current
    val text = fee.type.text()
    val stage = recovery?.stage ?: RecoveryStage.Found
    var confirmSent by rememberSaveable { mutableStateOf(false) }

    ScreenColumn(modifier = modifier) {
        SubScreenHeader(title = stringResource(R.string.fd_title), onBack = onBack)

        HeroCard {
            HeroLabel(stringResource(text.nameRes))
            Text(fee.amount.format(), style = MoneyText.hero)
            Text(
                "${fee.institution} · ${stringResource(R.string.rc_charged, res.relativeDay(fee.dayOffset).lowercase())}",
                style = MaterialTheme.typography.bodyMedium,
                color = BankGuardTheme.colors.onHeroMuted,
            )
        }

        ExplainCard(stringResource(R.string.fd_what), stringResource(text.whatRes))
        ExplainCard(stringResource(R.string.fd_why), fee.trigger)
        ExplainCard(stringResource(R.string.fd_avoid), stringResource(text.avoidRes))

        SectionHeader(title = stringResource(R.string.fd_progress))
        SurfaceCard {
            StepTracker(steps(stage))
        }

        when (stage) {
            RecoveryStage.Found -> {
                PrimaryButton(stringResource(R.string.fd_draft), onClick = onOpenLetter, leadingIcon = R.drawable.ic_description)
            }
            RecoveryStage.Drafted -> {
                InfoNote(stringResource(R.string.fd_sent_note), tone = StatusTone.Warning, iconRes = R.drawable.ic_info)
                PrimaryButton(stringResource(R.string.fd_mark_sent), onClick = { confirmSent = true })
                TonalButton(stringResource(R.string.fd_edit), onClick = onOpenLetter, leadingIcon = R.drawable.ic_description)
            }
            RecoveryStage.Sent -> {
                Text(
                    stringResource(R.string.fd_decision_q),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() },
                )
                PrimaryButton(stringResource(R.string.fd_refunded), onClick = { onRecordDecision(true) })
                SecondaryButton(stringResource(R.string.fd_not_refunded), onClick = { onRecordDecision(false) })
                TonalButton(stringResource(R.string.fd_edit), onClick = onOpenLetter, leadingIcon = R.drawable.ic_description)
            }
            RecoveryStage.Refunded -> {
                InfoNote(stringResource(R.string.fd_refunded_result, fee.institution), tone = StatusTone.Success, iconRes = R.drawable.ic_check_circle)
                TonalButton(stringResource(R.string.fd_edit), onClick = onOpenLetter, leadingIcon = R.drawable.ic_description)
            }
            RecoveryStage.NotRefunded -> {
                InfoNote(stringResource(R.string.fd_not_refunded_result, fee.institution), tone = StatusTone.Warning, iconRes = R.drawable.ic_info)
                TonalButton(stringResource(R.string.fd_edit), onClick = onOpenLetter, leadingIcon = R.drawable.ic_description)
            }
        }
        InfoNote(text = stringResource(R.string.rc_footer), tone = StatusTone.Demo, iconRes = R.drawable.ic_science)
    }

    if (confirmSent) {
        ConfirmDialog(
            title = stringResource(R.string.fd_sent_confirm_title),
            body = stringResource(R.string.fd_sent_confirm_body),
            confirmLabel = stringResource(R.string.fd_sent_confirm_yes),
            dismissLabel = stringResource(R.string.fd_sent_confirm_no),
            onConfirm = {
                confirmSent = false
                onMarkSent()
            },
            onDismiss = { confirmSent = false },
        )
    }
}

@Composable
private fun ExplainCard(title: String, body: String) {
    SurfaceCard {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun steps(stage: RecoveryStage): List<TrackerStep> {
    val states = when (stage) {
        RecoveryStage.Found -> listOf(StepState.Done, StepState.Current, StepState.Upcoming, StepState.Upcoming)
        RecoveryStage.Drafted -> listOf(StepState.Done, StepState.Done, StepState.Current, StepState.Upcoming)
        RecoveryStage.Sent -> listOf(StepState.Done, StepState.Done, StepState.Done, StepState.Current)
        RecoveryStage.Refunded, RecoveryStage.NotRefunded -> List(4) { StepState.Done }
    }
    return listOf(
        TrackerStep(stringResource(R.string.fd_step_found), stringResource(R.string.fd_step_found_body), states[0]),
        TrackerStep(stringResource(R.string.fd_step_letter), stringResource(R.string.fd_step_letter_body), states[1]),
        TrackerStep(stringResource(R.string.fd_step_sent), stringResource(R.string.fd_step_sent_body), states[2]),
        TrackerStep(stringResource(R.string.fd_step_decision), stringResource(R.string.fd_step_decision_body), states[3]),
    )
}

@Preview(showBackground = true, heightDp = 1700)
@Composable
private fun FeeDetailPreview() {
    BankGuardTheme {
        FeeDetailScreen(DemoProfiles.underbanked.bankFees.first(), FeeRecovery(RecoveryStage.Drafted), {}, {}, {}, {})
    }
}
