package com.example.bankguard.ui.screens.prevent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.domain.forecast.CashFlowForecaster
import com.example.bankguard.model.Money
import com.example.bankguard.ui.components.AmountTone
import com.example.bankguard.ui.components.ForecastChart
import com.example.bankguard.ui.components.HeroCard
import com.example.bankguard.ui.components.HeroLabel
import com.example.bankguard.ui.components.InfoNote
import com.example.bankguard.ui.components.ScreenColumn
import com.example.bankguard.ui.components.SectionHeader
import com.example.bankguard.ui.components.StatusBadge
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SubScreenHeader
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.screens.shared.tone
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.theme.Spacing
import com.example.bankguard.ui.util.relativeDay

private const val HORIZON_DAYS = 7

/** Day-by-day projection with an honest explanation of what it does and doesn't include. */
@Composable
fun ForecastDetailScreen(
    profile: DemoProfile,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val available = checkNotNull(profile.availableBalance)
    val forecast = remember(profile) { CashFlowForecaster.forecast(available, profile.scheduled, HORIZON_DAYS) }
    val res = LocalResources.current

    ScreenColumn(modifier = modifier) {
        SubScreenHeader(title = stringResource(R.string.fc_title), onBack = onBack)

        HeroCard {
            HeroLabel(stringResource(R.string.fc_hero_label))
            Row {
                HeroFigure(stringResource(R.string.fc_lowest), forecast.lowest.balance, res.relativeDay(forecast.lowest.dayOffset), Modifier.weight(1f))
                HeroFigure(stringResource(R.string.fc_ending), forecast.ending.balance, pluralStringResource(R.plurals.chart_end, HORIZON_DAYS, HORIZON_DAYS), Modifier.weight(1f))
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
        }

        InfoNote(text = stringResource(R.string.fc_how, available.format()))

        SectionHeader(title = stringResource(R.string.fc_days_title))
        SurfaceCard(verticalSpacing = 0.dp) {
            forecast.days.forEachIndexed { i, day ->
                if (i > 0) HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
                val items = profile.scheduled.filter { it.dayOffset == day.dayOffset }
                val dayLabel = res.relativeDay(day.dayOffset)
                val endText = stringResource(R.string.fc_end_balance, day.balance.format())
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.md)
                        .semantics(mergeDescendants = true) { contentDescription = "$dayLabel. ${items.joinToString { "${it.title} ${it.amount.formatSigned()}" }}. $endText" },
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(dayLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        if (day.balance.isNegative) StatusBadge(stringResource(R.string.fc_below_zero), StatusTone.Critical)
                        Text(
                            day.balance.format(),
                            style = MoneyText.row,
                            color = if (day.balance.isNegative) BankGuardTheme.colors.critical else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (items.isEmpty()) {
                        Text(stringResource(R.string.fc_no_activity), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items.forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text(item.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(
                                item.amount.formatSigned(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (item.amount.tone() == AmountTone.Positive) BankGuardTheme.colors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroFigure(label: String, amount: Money, caption: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = BankGuardTheme.colors.onHeroMuted)
        Text(
            amount.format(),
            style = MoneyText.large,
            color = if (amount.isNegative) BankGuardTheme.colors.onHeroCritical else BankGuardTheme.colors.onHero,
        )
        Text(caption, style = MaterialTheme.typography.bodySmall, color = BankGuardTheme.colors.onHeroMuted)
    }
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun ForecastDetailPreview() {
    BankGuardTheme { ForecastDetailScreen(DemoProfiles.underbanked, onBack = {}) }
}
