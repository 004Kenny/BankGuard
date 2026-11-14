package com.example.bankguard.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.domain.forecast.CashFlowForecaster
import com.example.bankguard.model.ScheduledItem
import com.example.bankguard.model.ScheduledKind
import com.example.bankguard.model.TransactionStatus
import com.example.bankguard.ui.BankGuardTab
import com.example.bankguard.ui.components.AlertCard
import com.example.bankguard.ui.components.ForecastChart
import com.example.bankguard.ui.components.HeroCard
import com.example.bankguard.ui.components.HeroInset
import com.example.bankguard.ui.components.HeroLabel
import com.example.bankguard.ui.components.MoneyRow
import com.example.bankguard.ui.components.PrimaryButton
import com.example.bankguard.ui.components.QuickActionTile
import com.example.bankguard.ui.components.ScreenColumn
import com.example.bankguard.ui.components.SectionHeader
import com.example.bankguard.ui.components.SimulatedDataBanner
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.theme.Spacing
import com.example.bankguard.ui.screens.shared.ScheduledListCard
import com.example.bankguard.ui.screens.shared.tone
import com.example.bankguard.ui.util.iconRes
import com.example.bankguard.ui.util.relativeDay

private const val HORIZON_DAYS = 7
private const val UPCOMING_DAYS = 14

/** Home for someone with a (simulated) checking account. Based on the Stitch "underbanked Home". */
@Composable
internal fun UnderbankedHome(
    profile: DemoProfile,
    onOpenTab: (BankGuardTab) -> Unit,
    onOpenForecast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = checkNotNull(profile.account) { "Underbanked profile needs an account" }
    val available = checkNotNull(profile.availableBalance)
    val forecast = remember(profile) { CashFlowForecaster.forecast(available, profile.scheduled, HORIZON_DAYS) }
    val upcoming = remember(profile) { profile.scheduled.filter { it.dayOffset in 0..UPCOMING_DAYS }.sortedBy { it.dayOffset } }
    val nextDeposit = upcoming.firstOrNull { it.kind == ScheduledKind.Income }
    val nextBill = upcoming.firstOrNull { it.kind != ScheduledKind.Income }
    val res = LocalResources.current

    ScreenColumn(modifier = modifier) {
        HomeGreeting(
            firstName = profile.firstName,
            subtitle = stringResource(R.string.ub_subtitle),
            chip = stringResource(R.string.ub_account_chip, account.name, account.last4),
        )

        // Balance and 7-day forecast
        HeroCard {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    HeroLabel(stringResource(R.string.ub_available))
                    Text(available.format(), style = MoneyText.hero)
                }
                Column(horizontalAlignment = Alignment.End) {
                    HeroLabel(stringResource(R.string.ub_lowest))
                    Text(
                        forecast.lowest.balance.format(),
                        style = MoneyText.large,
                        color = if (forecast.lowest.balance.isNegative) BankGuardTheme.colors.onHeroCritical else BankGuardTheme.colors.heroAccent,
                    )
                    Text(res.relativeDay(forecast.lowest.dayOffset), style = MaterialTheme.typography.labelMedium, color = BankGuardTheme.colors.onHeroMuted)
                }
            }
            ForecastChart(
                days = forecast.days,
                summary = stringResource(
                    R.string.ub_chart_summary,
                    forecast.lowest.balance.format(),
                    res.relativeDay(forecast.lowest.dayOffset),
                    forecast.ending.balance.format(),
                ),
                startLabel = stringResource(R.string.chart_start),
                endLabel = pluralStringResource(R.plurals.chart_end, HORIZON_DAYS, HORIZON_DAYS),
            )
            if (nextDeposit != null || nextBill != null) {
                HeroInset {
                    nextBill?.let { HeroScheduledRow(stringResource(R.string.ub_next_bill), it, res.relativeDay(it.dayOffset)) }
                    nextDeposit?.let { HeroScheduledRow(stringResource(R.string.ub_next_deposit), it, res.relativeDay(it.dayOffset)) }
                }
            }
        }

        // Overdraft risk, or reassurance when there is none
        val dip = forecast.firstNegativeDay
        if (dip != null && nextBill != null) {
            AlertCard(
                title = stringResource(R.string.ub_risk_title),
                body = stringResource(R.string.ub_risk_body, nextBill.title, nextBill.amount.absolute.format(), available.format(), forecast.lowest.balance.format()),
                badgeText = stringResource(R.string.ub_risk_badge),
                tone = StatusTone.Critical,
                iconRes = R.drawable.ic_warning,
                meta = res.relativeDay(dip.dayOffset),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.ub_risk_action),
                    onClick = { onOpenTab(BankGuardTab.Prevent) },
                    trailingIcon = R.drawable.ic_arrow_forward,
                )
            }
        } else {
            AlertCard(
                title = stringResource(R.string.ub_ok_title),
                body = stringResource(R.string.ub_ok_body),
                badgeText = stringResource(R.string.ub_ok_badge),
                tone = StatusTone.Success,
                iconRes = R.drawable.ic_check_circle,
            )
        }

        // Shortcuts into the three pillars
        SectionHeader(title = stringResource(R.string.ub_tools_title))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.height(IntrinsicSize.Min)) {
            val tile = Modifier.weight(1f).fillMaxHeight()
            QuickActionTile(R.drawable.ic_insights, stringResource(R.string.ub_tool_forecast), onOpenForecast, tile)
            QuickActionTile(R.drawable.ic_receipt_long, stringResource(R.string.ub_tool_fees), { onOpenTab(BankGuardTab.Recover) }, tile)
            QuickActionTile(R.drawable.ic_account_balance, stringResource(R.string.ub_tool_accounts), { onOpenTab(BankGuardTab.Discover) }, tile)
        }

        SectionHeader(title = stringResource(R.string.home_section_upcoming))
        ScheduledListCard(upcoming)

        SectionHeader(title = stringResource(R.string.ub_section_recent))
        SurfaceCard(verticalSpacing = 0.dp) {
            profile.transactions.sortedWith(compareBy({ it.status != TransactionStatus.Pending }, { -it.dayOffset })).forEachIndexed { i, t ->
                if (i > 0) HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
                MoneyRow(
                    iconRes = t.iconRes(),
                    title = t.merchant,
                    subtitle = res.relativeDay(t.dayOffset),
                    amount = t.amount.formatSigned(),
                    amountTone = t.amount.tone(),
                    amountCaption = stringResource(if (t.status == TransactionStatus.Pending) R.string.ub_pending else R.string.ub_posted),
                )
            }
        }

        SimulatedDataBanner(text = stringResource(R.string.ub_footer))
    }
}

@Composable
private fun HeroScheduledRow(label: String, item: ScheduledItem, whenText: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("$label · $whenText", style = MaterialTheme.typography.labelMedium, color = BankGuardTheme.colors.onHeroMuted)
            Text(item.title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            item.amount.formatSigned(),
            style = MoneyText.row,
            textAlign = TextAlign.End,
            color = if (item.amount.isNegative) BankGuardTheme.colors.onHeroCritical else BankGuardTheme.colors.heroAccent,
        )
    }
}

@Preview(showBackground = true, heightDp = 1900)
@Composable
private fun UnderbankedHomePreview() {
    BankGuardTheme { UnderbankedHome(DemoProfiles.underbanked, onOpenTab = {}, onOpenForecast = {}) }
}
