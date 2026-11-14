package com.example.bankguard.ui.screens.recover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankguard.R
import com.example.bankguard.data.demo.AccountScenario
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.domain.recovery.OverdraftCause
import com.example.bankguard.domain.recovery.OverdraftDetector
import com.example.bankguard.domain.recovery.RecoveryFeeCalculator
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.EligibilityCheck
import com.example.bankguard.model.FeeRecovery
import com.example.bankguard.model.Money
import com.example.bankguard.model.RecoveryEligibility
import com.example.bankguard.model.RecoveryRecord
import com.example.bankguard.model.RecoveryStage
import com.example.bankguard.model.sum
import com.example.bankguard.ui.BankGuardTab
import com.example.bankguard.ui.components.AmountTone
import com.example.bankguard.ui.components.HeroCard
import com.example.bankguard.ui.components.HeroInset
import com.example.bankguard.ui.components.HeroLabel
import com.example.bankguard.ui.components.IconBox
import com.example.bankguard.ui.components.InfoNote
import com.example.bankguard.ui.components.InsetPanel
import com.example.bankguard.ui.components.MoneyRow
import com.example.bankguard.ui.components.PillarHero
import com.example.bankguard.ui.components.PrimaryButton
import com.example.bankguard.ui.components.ScreenColumn
import com.example.bankguard.ui.components.SecondaryButton
import com.example.bankguard.ui.components.SectionHeader
import com.example.bankguard.ui.components.StatusBadge
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.theme.Spacing
import com.example.bankguard.ui.util.badge
import com.example.bankguard.ui.util.explanationRes
import com.example.bankguard.ui.util.iconRes
import com.example.bankguard.ui.util.relativeDay
import com.example.bankguard.ui.util.text

/**
 * Recover tab. With a bank account: recover a charge that has ALREADY overdrawn the account
 * (primary), with fee-waiver help as a secondary feature. Without one: what alternative
 * services cost, and why there's no transaction to recover.
 */
@Composable
fun RecoverScreen(
    profile: DemoProfile,
    recovery: RecoveryUiState,
    feeRecoveries: Map<String, FeeRecovery>,
    onOpenRecovery: (transactionId: String) -> Unit,
    onOpenFee: (feeId: String) -> Unit,
    onSetScenario: (AccountScenario) -> Unit,
    onOpenTab: (BankGuardTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (profile.persona) {
        DemoPersona.Underbanked -> UnderbankedRecover(profile, recovery, feeRecoveries, onOpenRecovery, onOpenFee, onSetScenario, modifier)
        DemoPersona.Unbanked -> UnbankedRecover(profile, onOpenTab, modifier)
    }
}

@Composable
private fun UnderbankedRecover(
    profile: DemoProfile,
    recovery: RecoveryUiState,
    feeRecoveries: Map<String, FeeRecovery>,
    onOpenRecovery: (String) -> Unit,
    onOpenFee: (String) -> Unit,
    onSetScenario: (AccountScenario) -> Unit,
    modifier: Modifier = Modifier,
) {
    val available = checkNotNull(profile.availableBalance)
    val cause = remember(profile) { OverdraftDetector.find(profile) }
    val records = recovery.records.values.sortedByDescending { it.submittedAtMillis }

    ScreenColumn(modifier = modifier) {
        RecoverHero(available, cause, records)

        if (cause != null) {
            SectionHeader(title = stringResource(R.string.rv_opportunity_title))
            OpportunityCard(
                cause = cause,
                record = recovery.records[cause.transaction.id],
                check = recovery.eligibility[cause.transaction.id],
                onOpen = { onOpenRecovery(cause.transaction.id) },
            )
        } else {
            SurfaceCard(verticalSpacing = Spacing.md) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    IconBox(iconRes = R.drawable.ic_check_circle, tone = StatusTone.Success)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(
                            stringResource(R.string.rv_none_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(stringResource(R.string.rv_none_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (!recovery.scenario.isOverdraft) {
                    PrimaryButton(
                        text = stringResource(R.string.rv_load_demo),
                        onClick = { onSetScenario(AccountScenario.PendingRelease) },
                        leadingIcon = R.drawable.ic_science,
                    )
                }
            }
        }

        SectionHeader(title = stringResource(R.string.rv_history_title))
        if (records.isEmpty()) {
            SurfaceCard {
                Text(stringResource(R.string.rv_history_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            records.forEach { record -> RecoveryHistoryCard(record, onClick = { onOpenRecovery(record.transactionId) }) }
        }

        SectionHeader(title = stringResource(R.string.rv_scenarios_title))
        ScenarioPicker(selected = recovery.scenario, onSelect = onSetScenario)

        SectionHeader(title = stringResource(R.string.rv_feehelp_title))
        FeeHelpCard(profile, feeRecoveries, onOpenFee)

        InfoNote(text = stringResource(R.string.rv_footer), tone = StatusTone.Demo, iconRes = R.drawable.ic_science)
    }
}

@Composable
private fun RecoverHero(available: Money, cause: OverdraftCause?, records: List<RecoveryRecord>) {
    val confirmed = records.filter(RecoveryRecord::isConfirmed)
    HeroCard {
        HeroLabel(stringResource(R.string.rv_hero_label))
        Text(
            stringResource(R.string.rv_hero_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.rv_hero_subtitle), style = MaterialTheme.typography.bodyMedium, color = BankGuardTheme.colors.onHeroMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.padding(top = Spacing.xs)) {
            Column(modifier = Modifier.weight(1f)) {
                HeroLabel(stringResource(R.string.rv_available))
                Text(
                    available.format(),
                    style = MoneyText.large,
                    color = if (available.isNegative) BankGuardTheme.colors.onHeroCritical else BankGuardTheme.colors.onHero,
                )
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                HeroLabel(stringResource(R.string.rv_status_label), textAlign = TextAlign.End)
                Text(
                    if (cause != null) stringResource(R.string.rv_status_overdrawn, cause.overdraftAmount.format()) else stringResource(R.string.rv_status_ok),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (cause != null) BankGuardTheme.colors.onHeroCritical else BankGuardTheme.colors.heroAccent,
                    textAlign = TextAlign.End,
                )
            }
        }
        HeroInset {
            HeroStatRow(stringResource(R.string.rv_stat_restored), confirmed.map(RecoveryRecord::amountRestored).sum().format())
            HeroStatRow(stringResource(R.string.rv_stat_fees), records.map(RecoveryRecord::feeCollected).sum().format())
            HeroStatRow(stringResource(R.string.rv_stat_successful), stringResource(R.string.rv_stat_successful_value, confirmed.size, records.size))
        }
    }
}

@Composable
private fun HeroStatRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = BankGuardTheme.colors.onHeroMuted, modifier = Modifier.weight(1f))
        Text(value, style = MoneyText.row, color = BankGuardTheme.colors.onHero, textAlign = TextAlign.End)
    }
}

/** The charge that overdrew the account, with what recovering it could mean. */
@Composable
private fun OpportunityCard(
    cause: OverdraftCause,
    record: RecoveryRecord?,
    check: EligibilityCheck?,
    onOpen: () -> Unit,
) {
    val res = LocalResources.current
    val t = cause.transaction
    val ineligible = (check as? EligibilityCheck.Done)?.result is RecoveryEligibility.Ineligible
    val badge = recoveryBadge(record, check)
    SurfaceCard(verticalSpacing = Spacing.md) {
        MoneyRow(
            iconRes = t.iconRes(),
            title = t.merchant,
            subtitle = stringResource(R.string.rv_opp_caused, stringResource(t.status.labelRes()), res.relativeDay(t.dayOffset)),
            amount = t.amount.formatSigned(),
            amountTone = AmountTone.Negative,
        )
        StatusBadge(stringResource(badge.textRes), badge.tone)
        InsetPanel {
            BreakdownRow(stringResource(R.string.rv_opp_overdraft), cause.overdraftAmount.format(), valueColor = BankGuardTheme.colors.critical)
            BreakdownRow(stringResource(R.string.rv_opp_estimated), if (ineligible) Money.Zero.format() else t.amount.absolute.format())
            BreakdownRow(
                stringResource(R.string.rv_opp_fee),
                if (ineligible) Money.Zero.format() else RecoveryFeeCalculator.fee(cause.balanceBefore).format(),
            )
        }
        if (record == null) {
            PrimaryButton(stringResource(R.string.rv_check), onClick = onOpen, trailingIcon = R.drawable.ic_arrow_forward)
        } else {
            SecondaryButton(stringResource(R.string.rv_view_request), onClick = onOpen, trailingIcon = R.drawable.ic_arrow_forward)
        }
    }
}

/** Presenter control: which simulated account state to show across the app. */
@Composable
private fun ScenarioPicker(selected: AccountScenario, onSelect: (AccountScenario) -> Unit) {
    SurfaceCard(verticalSpacing = Spacing.xs) {
        Text(stringResource(R.string.rv_scenarios_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.selectableGroup()) {
            AccountScenario.entries.forEach { scenario ->
                val isSelected = scenario == selected
                Row(
                    modifier = Modifier
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { if (!isSelected) onSelect(scenario) })
                        .padding(vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(scenario.titleRes()), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(scenario.bodyRes()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Secondary feature: explain bank fees and help the user ask for a waiver themselves. */
@Composable
private fun FeeHelpCard(profile: DemoProfile, feeRecoveries: Map<String, FeeRecovery>, onOpenFee: (String) -> Unit) {
    val res = LocalResources.current
    SurfaceCard(verticalSpacing = Spacing.sm) {
        Text(stringResource(R.string.rv_feehelp_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        profile.bankFees.sortedBy { -it.dayOffset }.forEach { fee ->
            HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
            val stage = feeRecoveries[fee.id]?.stage ?: RecoveryStage.Found
            val badge = stage.badge()
            Column {
                MoneyRow(
                    iconRes = fee.type.text().iconRes,
                    title = stringResource(fee.type.text().nameRes),
                    subtitle = stringResource(R.string.rc_charged, res.relativeDay(fee.dayOffset).lowercase()),
                    amount = fee.amount.format(),
                    onClick = { onOpenFee(fee.id) },
                )
                Row(modifier = Modifier.padding(start = 52.dp, bottom = Spacing.xs)) {
                    StatusBadge(stringResource(badge.textRes), badge.tone)
                }
            }
        }
    }
}

/** For someone without a bank account: no transaction to recover, and what alternative services cost. */
@Composable
private fun UnbankedRecover(
    profile: DemoProfile,
    onOpenTab: (BankGuardTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val res = LocalResources.current
    ScreenColumn(modifier = modifier) {
        PillarHero(
            iconRes = R.drawable.ic_published_with_changes,
            label = stringResource(R.string.nav_recover),
            title = stringResource(R.string.rcu_title),
            subtitle = stringResource(R.string.rcu_subtitle),
        )

        SurfaceCard(verticalSpacing = Spacing.md) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                IconBox(iconRes = R.drawable.ic_account_balance, tone = StatusTone.Info)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        stringResource(R.string.rcu_recovery_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(stringResource(R.string.rcu_recovery_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            PrimaryButton(
                text = stringResource(R.string.unb_opportunity_action),
                onClick = { onOpenTab(BankGuardTab.Discover) },
                trailingIcon = R.drawable.ic_arrow_forward,
            )
        }

        SectionHeader(title = stringResource(R.string.rcu_types_title))
        SurfaceCard(verticalSpacing = Spacing.md) {
            profile.altServiceCosts.forEachIndexed { i, cost ->
                if (i > 0) HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cost.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.rcu_monthly, cost.monthly.format()), style = MoneyText.row, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(stringResource(cost.type.explanationRes()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            InsetPanel(tone = StatusTone.Info) {
                Text(stringResource(R.string.rcu_refund_note), style = MaterialTheme.typography.bodyMedium)
            }
        }

        SectionHeader(title = stringResource(R.string.unb_fee_log_title))
        SurfaceCard(verticalSpacing = 0.dp) {
            profile.altServiceCharges.sortedBy { -it.dayOffset }.forEachIndexed { i, charge ->
                if (i > 0) HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
                MoneyRow(
                    iconRes = R.drawable.ic_receipt_long,
                    title = charge.title,
                    subtitle = "${charge.provider} · ${charge.detail}",
                    amount = charge.amount.formatSigned(),
                    amountTone = AmountTone.Negative,
                    amountCaption = res.relativeDay(charge.dayOffset),
                )
            }
        }

        InfoNote(text = stringResource(R.string.unb_footer), tone = StatusTone.Demo, iconRes = R.drawable.ic_science)
    }
}

@Preview(showBackground = true, heightDp = 2400)
@Composable
private fun UnderbankedRecoverPreview() {
    BankGuardTheme {
        RecoverScreen(
            profile = DemoProfiles.forScenario(DemoPersona.Underbanked, AccountScenario.PendingRelease),
            recovery = RecoveryUiState(scenario = AccountScenario.PendingRelease),
            feeRecoveries = mapOf("f-atm" to FeeRecovery(RecoveryStage.Sent)),
            onOpenRecovery = {}, onOpenFee = {}, onSetScenario = {}, onOpenTab = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun UnbankedRecoverPreview() {
    BankGuardTheme { RecoverScreen(DemoProfiles.unbanked, RecoveryUiState(), emptyMap(), {}, {}, {}, {}) }
}
