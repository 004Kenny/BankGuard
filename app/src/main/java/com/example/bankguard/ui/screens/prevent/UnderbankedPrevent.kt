package com.example.bankguard.ui.screens.prevent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.domain.forecast.CashFlowForecaster
import com.example.bankguard.model.CancellationEligibility
import com.example.bankguard.model.CancellationStatus
import com.example.bankguard.model.ScheduledKind
import com.example.bankguard.ui.components.AlertCard
import com.example.bankguard.ui.components.ForecastChart
import com.example.bankguard.ui.components.HeroCard
import com.example.bankguard.ui.components.HeroLabel
import com.example.bankguard.ui.components.InfoNote
import com.example.bankguard.ui.components.BulletList
import com.example.bankguard.ui.components.InsetPanel
import com.example.bankguard.ui.components.MoneyRow
import com.example.bankguard.ui.components.ScreenColumn
import com.example.bankguard.ui.components.SectionHeader
import com.example.bankguard.ui.components.StatusBadge
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.components.TonalButton
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.theme.Spacing
import com.example.bankguard.ui.util.iconRes
import com.example.bankguard.ui.util.relativeDay

private const val HORIZON_DAYS = 7

/** Prevent for someone with a (simulated) checking account. Visual reference: Stitch "Risk Alerts". */
@Composable
internal fun UnderbankedPrevent(
    profile: DemoProfile,
    cancellations: Map<String, CancellationStatus>,
    onOpenForecast: () -> Unit,
    onOpenCancellation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = checkNotNull(profile.account)
    val available = checkNotNull(profile.availableBalance)
    val forecast = remember(profile) { CashFlowForecaster.forecast(available, profile.scheduled, HORIZON_DAYS) }
    // Pending charges as originally loaded, so cancelled ones still show with their result.
    val pending = remember(profile.persona) { DemoProfiles.forPersona(profile.persona).pendingTransactions }
    val res = LocalResources.current
    val dip = forecast.firstNegativeDay
    val billBeforeDip = profile.scheduled
        .filter { it.kind != ScheduledKind.Income && dip != null && it.dayOffset <= dip.dayOffset }
        .minByOrNull { it.dayOffset }
    val canStillCancel = pending.any { it.cancellation == CancellationEligibility.Eligible && cancellations[it.id] == null }
    val belowMinimum = account.minimumBalance != null && forecast.lowest.balance < account.minimumBalance

    ScreenColumn(modifier = modifier) {
        // Summary of the week ahead
        HeroCard {
            HeroLabel(stringResource(R.string.pv_hero_label))
            Text(
                forecast.lowest.balance.format(),
                style = MoneyText.hero,
                color = if (forecast.lowest.balance.isNegative) BankGuardTheme.colors.onHeroCritical else BankGuardTheme.colors.onHero,
            )
            Text(
                stringResource(R.string.pv_hero_when, res.relativeDay(forecast.lowest.dayOffset)),
                style = MaterialTheme.typography.bodyMedium,
                color = BankGuardTheme.colors.onHeroMuted,
            )
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
        }
        TonalButton(
            text = stringResource(R.string.pv_view_forecast),
            onClick = onOpenForecast,
            leadingIcon = R.drawable.ic_insights,
        )

        // Alerts
        SectionHeader(title = stringResource(R.string.pv_alerts_title))
        if (dip != null && billBeforeDip != null) {
            AlertCard(
                title = stringResource(R.string.ub_risk_title),
                body = stringResource(
                    R.string.pv_overdraft_body,
                    billBeforeDip.title,
                    billBeforeDip.amount.absolute.format(),
                    res.relativeDay(billBeforeDip.dayOffset).lowercase(),
                    forecast.lowest.balance.format(),
                    account.overdraftFee.format(),
                ),
                badgeText = stringResource(R.string.ub_risk_badge),
                tone = StatusTone.Critical,
                iconRes = R.drawable.ic_warning,
                meta = res.relativeDay(dip.dayOffset),
            ) {
                InsetPanel {
                    Text(
                        stringResource(R.string.pv_things_title),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    BulletList(
                        buildList {
                            if (canStillCancel) add(stringResource(R.string.pv_tip_cancel))
                            add(stringResource(R.string.pv_tip_biller, billBeforeDip.title))
                            add(stringResource(R.string.pv_tip_cash))
                        },
                    )
                }
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
        if (belowMinimum) {
            AlertCard(
                title = stringResource(R.string.pv_minimum_title),
                body = stringResource(
                    R.string.pv_minimum_body,
                    account.minimumBalance!!.format(),
                    account.institution,
                    (account.maintenanceFee ?: account.overdraftFee).format(),
                ),
                badgeText = stringResource(R.string.pv_minimum_badge),
                tone = StatusTone.Warning,
                iconRes = R.drawable.ic_account_balance,
            )
        }

        // Pending charges and cancellation requests
        SectionHeader(title = stringResource(R.string.pv_pending_title))
        SurfaceCard(verticalSpacing = Spacing.sm) {
            Text(stringResource(R.string.pv_pending_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (pending.isEmpty()) {
                Text(stringResource(R.string.pv_pending_none), style = MaterialTheme.typography.bodyMedium)
            }
            pending.forEach { t ->
                HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
                val badge = chargeBadge(t, cancellations[t.id])
                Column {
                    MoneyRow(
                        iconRes = t.iconRes(),
                        title = t.merchant,
                        subtitle = res.relativeDay(t.dayOffset),
                        amount = t.amount.formatSigned(),
                        onClick = { onOpenCancellation(t.id) },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 52.dp, bottom = Spacing.xs)) {
                        StatusBadge(stringResource(badge.textRes), badge.tone)
                    }
                }
            }
        }

        InfoNote(text = stringResource(R.string.pv_footer), tone = StatusTone.Demo, iconRes = R.drawable.ic_science)
    }
}

@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun UnderbankedPreventPreview() {
    BankGuardTheme {
        UnderbankedPrevent(DemoProfiles.underbanked, emptyMap(), {}, {})
    }
}
