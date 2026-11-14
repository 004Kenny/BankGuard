package com.example.bankguard.ui.screens.recover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.model.BankFee
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.FeeRecovery
import com.example.bankguard.model.RecoveryStage
import com.example.bankguard.model.sum
import com.example.bankguard.ui.BankGuardTab
import com.example.bankguard.ui.components.AmountTone
import com.example.bankguard.ui.components.BulletList
import com.example.bankguard.ui.components.HeroCard
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
import com.example.bankguard.ui.components.TonalButton
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.theme.Spacing
import com.example.bankguard.ui.util.badge
import com.example.bankguard.ui.util.explanationRes
import com.example.bankguard.ui.util.relativeDay
import com.example.bankguard.ui.util.text

/** Recover tab: fees found and help asking for them back (underbanked); what fees cost (unbanked). */
@Composable
fun RecoverScreen(
    profile: DemoProfile,
    recoveries: Map<String, FeeRecovery>,
    onOpenFee: (feeId: String) -> Unit,
    onOpenTab: (BankGuardTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (profile.persona) {
        DemoPersona.Underbanked -> UnderbankedRecover(profile, recoveries, onOpenFee, modifier)
        DemoPersona.Unbanked -> UnbankedRecover(profile, onOpenTab, modifier)
    }
}

/** Visual reference: Stitch "Fee Recovery", without its recovered totals, success rates or automatic actions. */
@Composable
private fun UnderbankedRecover(
    profile: DemoProfile,
    recoveries: Map<String, FeeRecovery>,
    onOpenFee: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val res = LocalResources.current
    val refunded = profile.bankFees
        .filter { recoveries[it.id]?.stage == RecoveryStage.Refunded }
        .map(BankFee::amount)
        .sum()

    ScreenColumn(modifier = modifier) {
        HeroCard {
            HeroLabel(stringResource(R.string.rc_hero_label))
            Text(profile.totalBankFees.format(), style = MoneyText.hero)
            Text(
                pluralStringResource(R.plurals.rc_hero_count, profile.bankFees.size, profile.bankFees.size),
                style = MaterialTheme.typography.bodyMedium,
                color = BankGuardTheme.colors.onHeroMuted,
            )
            Text(stringResource(R.string.rc_hero_note), style = MaterialTheme.typography.bodySmall, color = BankGuardTheme.colors.onHeroMuted)
            if (refunded.cents > 0) {
                Text(
                    stringResource(R.string.rc_reported_refunds, refunded.format()),
                    style = MaterialTheme.typography.labelLarge,
                    color = BankGuardTheme.colors.heroAccent,
                )
            }
        }

        SectionHeader(title = stringResource(R.string.rc_fees_title))
        profile.bankFees.sortedBy { -it.dayOffset }.forEach { fee ->
            val stage = recoveries[fee.id]?.stage ?: RecoveryStage.Found
            val text = fee.type.text()
            SurfaceCard(verticalSpacing = Spacing.md) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    IconBox(iconRes = text.iconRes, tone = StatusTone.Critical)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(fee.amount.format(), style = MoneyText.large, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            stringResource(text.nameRes),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            "${fee.institution} · ${stringResource(R.string.rc_charged, res.relativeDay(fee.dayOffset).lowercase())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val badge = stage.badge()
                StatusBadge(stringResource(badge.textRes), badge.tone)
                InsetPanel {
                    Text(stringResource(text.whatRes), style = MaterialTheme.typography.bodySmall)
                }
                when (stage) {
                    RecoveryStage.Found -> PrimaryButton(stringResource(R.string.rc_action_start), onClick = { onOpenFee(fee.id) }, trailingIcon = R.drawable.ic_arrow_forward)
                    RecoveryStage.Drafted, RecoveryStage.Sent -> SecondaryButton(stringResource(R.string.rc_action_continue), onClick = { onOpenFee(fee.id) }, trailingIcon = R.drawable.ic_arrow_forward)
                    else -> TonalButton(stringResource(R.string.rc_action_view), onClick = { onOpenFee(fee.id) })
                }
            }
        }

        SurfaceCard {
            Text(
                stringResource(R.string.rc_how_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            BulletList(
                listOf(stringResource(R.string.rc_how_1), stringResource(R.string.rc_how_2), stringResource(R.string.rc_how_3)),
            )
        }
        InfoNote(text = stringResource(R.string.rc_ai_note))
        InfoNote(text = stringResource(R.string.rc_footer), tone = StatusTone.Demo, iconRes = R.drawable.ic_science)
    }
}

/** For someone without a bank account: what alternative services cost, and how to avoid them. */
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
        }

        SurfaceCard(verticalSpacing = Spacing.md) {
            InsetPanel(tone = StatusTone.Info) {
                Text(stringResource(R.string.rcu_refund_note), style = MaterialTheme.typography.bodyMedium)
            }
            PrimaryButton(
                text = stringResource(R.string.unb_opportunity_action),
                onClick = { onOpenTab(BankGuardTab.Discover) },
                trailingIcon = R.drawable.ic_arrow_forward,
            )
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

@Preview(showBackground = true, heightDp = 2200)
@Composable
private fun UnderbankedRecoverPreview() {
    BankGuardTheme {
        RecoverScreen(DemoProfiles.underbanked, mapOf("f-atm" to FeeRecovery(RecoveryStage.Sent)), {}, {})
    }
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun UnbankedRecoverPreview() {
    BankGuardTheme { RecoverScreen(DemoProfiles.unbanked, emptyMap(), {}, {}) }
}
