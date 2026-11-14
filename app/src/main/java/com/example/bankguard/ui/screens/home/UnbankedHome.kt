package com.example.bankguard.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.model.AltServiceCost
import com.example.bankguard.ui.BankGuardTab
import com.example.bankguard.ui.components.AmountTone
import com.example.bankguard.ui.components.BarSegment
import com.example.bankguard.ui.components.HeroCard
import com.example.bankguard.ui.components.HeroInset
import com.example.bankguard.ui.components.HeroLabel
import com.example.bankguard.ui.components.IconBox
import com.example.bankguard.ui.components.MoneyRow
import com.example.bankguard.ui.components.PrimaryButton
import com.example.bankguard.ui.components.ScreenColumn
import com.example.bankguard.ui.components.SectionHeader
import com.example.bankguard.ui.components.SegmentedBar
import com.example.bankguard.ui.components.SimulatedDataBanner
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.theme.NavyMuted
import com.example.bankguard.ui.theme.Spacing
import com.example.bankguard.ui.theme.TealLight
import com.example.bankguard.ui.screens.shared.EnvelopeBudgetCard
import com.example.bankguard.ui.screens.shared.ScheduledListCard
import com.example.bankguard.ui.util.relativeDay

/**
 * Home for someone without a bank account. Based on the Stitch "unbanked Home", with the
 * unsupported savings figures, "certified" claims and statistics removed.
 */
@Composable
internal fun UnbankedHome(
    profile: DemoProfile,
    onOpenTab: (BankGuardTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val res = LocalResources.current
    val monthly = profile.monthlyAltServiceCost
    val segmentColors = listOf(BankGuardTheme.colors.heroAccent, TealLight, NavyMuted)

    ScreenColumn(modifier = modifier) {
        HomeGreeting(
            firstName = profile.firstName,
            subtitle = stringResource(R.string.unb_subtitle),
            chip = stringResource(profile.persona.modeRes),
        )

        // What alternative financial services cost each month
        HeroCard {
            HeroLabel(stringResource(R.string.unb_hero_label))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(monthly.format(), style = MoneyText.hero)
                Text(
                    stringResource(R.string.unb_per_month),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BankGuardTheme.colors.onHeroMuted,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Text(stringResource(R.string.unb_hero_body), style = MaterialTheme.typography.bodySmall, color = BankGuardTheme.colors.onHeroMuted)
            SegmentedBar(
                segments = profile.altServiceCosts.mapIndexed { i, c ->
                    BarSegment(c.monthly.cents.toFloat(), segmentColors[i % segmentColors.size])
                },
                trackColor = BankGuardTheme.colors.heroInset,
                modifier = Modifier.padding(vertical = Spacing.xs),
            )
            HeroInset {
                profile.altServiceCosts.forEachIndexed { i, cost ->
                    CostLegendRow(cost, segmentColors[i % segmentColors.size])
                }
            }
        }

        // Yearly cost, then the way out
        SurfaceCard {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                IconBox(iconRes = R.drawable.ic_trending_up, tone = StatusTone.Warning)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        stringResource(R.string.unb_year_title, (monthly * 12).format()),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(stringResource(R.string.unb_year_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SurfaceCard(verticalSpacing = Spacing.md) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                IconBox(iconRes = R.drawable.ic_account_balance, tone = StatusTone.Info)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        stringResource(R.string.unb_opportunity_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(stringResource(R.string.unb_opportunity_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            PrimaryButton(
                text = stringResource(R.string.unb_opportunity_action),
                onClick = { onOpenTab(BankGuardTab.Discover) },
                trailingIcon = R.drawable.ic_arrow_forward,
            )
        }

        // Cash envelopes and prepaid card
        SectionHeader(title = stringResource(R.string.unb_budget_title))
        EnvelopeBudgetCard(profile.envelopes, profile.prepaidCardBalance)

        SectionHeader(title = stringResource(R.string.home_section_upcoming))
        ScheduledListCard(profile.scheduled)

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

        SimulatedDataBanner(text = stringResource(R.string.unb_footer))
    }
}

@Composable
private fun CostLegendRow(cost: AltServiceCost, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Column(modifier = Modifier.weight(1f)) {
            Text(cost.label, style = MaterialTheme.typography.titleSmall)
            Text(cost.detail, style = MaterialTheme.typography.bodySmall, color = BankGuardTheme.colors.onHeroMuted)
        }
        Text(cost.monthly.format(), style = MoneyText.row)
    }
}

@Preview(showBackground = true, heightDp = 2000)
@Composable
private fun UnbankedHomePreview() {
    BankGuardTheme { UnbankedHome(DemoProfiles.unbanked, onOpenTab = {}) }
}
