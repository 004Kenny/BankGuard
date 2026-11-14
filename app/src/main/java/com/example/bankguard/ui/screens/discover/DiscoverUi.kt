package com.example.bankguard.ui.screens.discover

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.example.bankguard.R
import com.example.bankguard.domain.savings.OptionEstimate
import com.example.bankguard.model.AccountOption
import com.example.bankguard.ui.components.IconBox
import com.example.bankguard.ui.components.InsetPanel
import com.example.bankguard.ui.components.SecondaryButton
import com.example.bankguard.ui.components.StatusBadge
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.theme.Spacing

/** Account filters on the Discover screen. Any combination can be on. */
enum class AccountFilter(@param:StringRes val labelRes: Int, val matches: (AccountOption) -> Boolean) {
    NoOverdraft(R.string.filter_no_overdraft, { it.overdraftFee == null }),
    NoMonthlyFee(R.string.filter_no_monthly, { it.monthlyFee.cents == 0L }),
    NoMinimum(R.string.filter_no_minimum, { it.minimumBalance.cents == 0L }),
    SecondChance(R.string.filter_second_chance, { it.secondChance }),
}

/** "Saves about $302.00 a year" / "Costs about $X more a year" / "About the same as today". */
@Composable
fun savingsLine(estimate: OptionEstimate): Pair<String, StatusTone> = when {
    estimate.yearlySavings.cents > 0 -> stringResource(R.string.ds_saves, estimate.yearlySavings.format()) to StatusTone.Success
    estimate.yearlySavings.cents < 0 -> stringResource(R.string.ds_costs_more, estimate.yearlySavings.absolute.format()) to StatusTone.Warning
    else -> stringResource(R.string.ds_same) to StatusTone.Neutral
}

@Composable
fun monthlyFeeText(option: AccountOption): String = when {
    option.monthlyFee.cents == 0L -> option.monthlyFee.format()
    option.monthlyFeeWaivedWithDirectDeposit -> stringResource(R.string.fact_monthly_waived, option.monthlyFee.format())
    else -> option.monthlyFee.format()
}

@Composable
fun overdraftText(option: AccountOption): String =
    option.overdraftFee?.format() ?: stringResource(R.string.fact_overdraft_none)

/** Label and value on one line, for account facts. */
@Composable
fun FactRow(label: String, value: String, modifier: Modifier = Modifier, emphasize: Boolean = false) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            value,
            style = if (emphasize) MoneyText.row else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Badges shown on an account: possible partner (disclosed) and second chance. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OptionBadges(option: AccountOption, best: Boolean) {
    if (!best && !option.proposedPartner && !option.secondChance) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (best) StatusBadge(stringResource(R.string.ds_best), StatusTone.Success)
        if (option.secondChance) StatusBadge(stringResource(R.string.badge_second_chance), StatusTone.Info)
        if (option.proposedPartner) StatusBadge(stringResource(R.string.badge_partner), StatusTone.Demo)
    }
}

/** One account in the Discover list (layout from the Stitch "Better Options" comparison cards). */
@Composable
fun AccountOptionCard(
    estimate: OptionEstimate,
    best: Boolean,
    onCompare: () -> Unit,
) {
    val option = estimate.option
    SurfaceCard(verticalSpacing = Spacing.md) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            IconBox(iconRes = R.drawable.ic_account_balance, tone = StatusTone.Info)
            Column(modifier = Modifier.weight(1f)) {
                Text(option.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { heading() })
                Text(option.institution, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OptionBadges(option, best)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            FactRow(stringResource(R.string.fact_monthly), monthlyFeeText(option))
            FactRow(stringResource(R.string.fact_overdraft), overdraftText(option))
            FactRow(stringResource(R.string.fact_minimum), option.minimumBalance.format())
            FactRow(stringResource(R.string.fact_opening), option.openingDeposit.format())
        }
        HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
        val (line, tone) = savingsLine(estimate)
        InsetPanel(tone = tone) {
            FactRow(stringResource(R.string.fact_yearly), estimate.yearlyCost.format(), emphasize = true)
            Text(line, style = MaterialTheme.typography.titleSmall)
        }
        SecondaryButton(text = stringResource(R.string.ds_compare), onClick = onCompare, trailingIcon = R.drawable.ic_arrow_forward)
    }
}
