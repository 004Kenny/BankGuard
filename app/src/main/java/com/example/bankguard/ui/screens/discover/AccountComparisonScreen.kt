package com.example.bankguard.ui.screens.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoAccountOptions
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.domain.savings.SavingsEstimator
import com.example.bankguard.model.AccountOption
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.Money
import com.example.bankguard.model.SavingsInputs
import com.example.bankguard.ui.components.BulletList
import com.example.bankguard.ui.components.DisclosureCard
import com.example.bankguard.ui.components.HeroCard
import com.example.bankguard.ui.components.HeroLabel
import com.example.bankguard.ui.components.InfoNote
import com.example.bankguard.ui.components.PrimaryButton
import com.example.bankguard.ui.components.ScreenColumn
import com.example.bankguard.ui.components.SectionHeader
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SubScreenHeader
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.components.TonalButton
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.theme.Spacing

/** Side-by-side: what the user pays now versus one sample account, with every assumption listed. */
@Composable
fun AccountComparisonScreen(
    option: AccountOption,
    profile: DemoProfile,
    inputs: SavingsInputs,
    onAdjust: () -> Unit,
    onOpenAccount: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = profile.account
    val estimate = remember(option, profile, inputs) {
        SavingsEstimator.rank(listOf(option), account, inputs).single()
    }
    val currentCost = remember(profile, inputs) { SavingsEstimator.currentYearlyCost(account, inputs) }
    val na = stringResource(R.string.fact_not_applicable)
    val currentLabel = stringResource(if (account != null) R.string.cmp_current_ub else R.string.cmp_current_unb)

    ScreenColumn(modifier = modifier) {
        SubScreenHeader(title = stringResource(R.string.cmp_title), onBack = onBack)

        HeroCard {
            HeroLabel(option.name)
            val (line, _) = savingsLine(estimate)
            Text(line, style = MaterialTheme.typography.headlineSmall, color = BankGuardTheme.colors.heroAccent)
            Text(option.institution, style = MaterialTheme.typography.bodySmall, color = BankGuardTheme.colors.onHeroMuted)
            Text(stringResource(R.string.ds_hero_note), style = MaterialTheme.typography.bodySmall, color = BankGuardTheme.colors.onHeroMuted)
        }
        OptionBadges(option, best = false)
        PrimaryButton(
            text = stringResource(R.string.ao_open_account),
            onClick = onOpenAccount,
            trailingIcon = R.drawable.ic_arrow_forward,
        )

        // Comparison table: label on its own line, then "now" and "option" values side by side.
        SurfaceCard(verticalSpacing = Spacing.md) {
            Row {
                Text(currentLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(option.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
            }
            CompareRow(
                stringResource(R.string.fact_monthly),
                account?.let { a -> a.maintenanceFee?.let { fee -> stringResource(R.string.fact_monthly_below_min, fee.format(), (a.minimumBalance ?: fee).format()) } ?: Money.Zero.format() } ?: na,
                monthlyFeeText(option),
                currentLabel, option.name,
            )
            CompareRow(stringResource(R.string.fact_overdraft), account?.overdraftFee?.format() ?: na, overdraftText(option), currentLabel, option.name)
            CompareRow(stringResource(R.string.fact_minimum), account?.minimumBalance?.format() ?: na, option.minimumBalance.format(), currentLabel, option.name)
            CompareRow(stringResource(R.string.fact_atm), account?.outOfNetworkAtmFee?.format() ?: na, option.outOfNetworkAtmFee.format(), currentLabel, option.name)
            if (account == null) {
                CompareRow(
                    stringResource(R.string.fact_alt_services),
                    stringResource(R.string.fact_per_month, inputs.currentAltServiceMonthly.format()),
                    stringResource(R.string.fact_alt_not_needed),
                    currentLabel, option.name,
                )
            }
            CompareRow(stringResource(R.string.fact_opening), na, option.openingDeposit.format(), currentLabel, option.name)
            HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
            CompareRow(stringResource(R.string.fact_yearly), currentCost.format(), estimate.yearlyCost.format(), currentLabel, option.name, emphasize = true)
        }

        SectionHeader(title = stringResource(R.string.cmp_features))
        SurfaceCard { BulletList(option.features) }

        SectionHeader(title = stringResource(R.string.cmp_how_title))
        SurfaceCard {
            BulletList(
                buildList {
                    if (account != null) {
                        add(pluralStringResource(R.plurals.cmp_how_overdrafts, inputs.overdraftsPerYear, inputs.overdraftsPerYear))
                        add(pluralStringResource(R.plurals.cmp_how_months_below, inputs.monthsBelowMinimum, inputs.monthsBelowMinimum))
                    } else {
                        add(stringResource(R.string.cmp_how_alt, inputs.currentAltServiceMonthly.format()))
                        add(stringResource(R.string.cmp_how_assume_dd))
                    }
                    add(pluralStringResource(R.plurals.cmp_how_atm, inputs.outOfNetworkAtmPerMonth, inputs.outOfNetworkAtmPerMonth))
                    add(stringResource(if (inputs.hasDirectDeposit) R.string.cmp_how_dd_yes else R.string.cmp_how_dd_no))
                },
            )
            TonalButton(text = stringResource(R.string.ds_adjust), onClick = onAdjust, leadingIcon = R.drawable.ic_insights)
        }

        SectionHeader(title = stringResource(R.string.cmp_before_title))
        SurfaceCard {
            BulletList(
                buildList {
                    add(stringResource(R.string.cmp_before_schedule))
                    add(stringResource(R.string.cmp_before_id))
                    add(stringResource(if (account != null) R.string.cmp_before_keep_open else R.string.cmp_before_employer))
                },
            )
        }

        InfoNote(text = stringResource(R.string.cmp_no_open))
        if (option.proposedPartner) {
            DisclosureCard(title = stringResource(R.string.disc_title), body = stringResource(R.string.disc_body))
        }
        InfoNote(text = stringResource(R.string.ds_footer), tone = StatusTone.Demo, iconRes = R.drawable.ic_science)
    }
}

@Composable
private fun CompareRow(
    label: String,
    current: String,
    option: String,
    currentLabel: String,
    optionLabel: String,
    emphasize: Boolean = false,
) {
    val style = if (emphasize) MoneyText.row else MaterialTheme.typography.bodyMedium
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = "$label. $currentLabel: $current. $optionLabel: $option." },
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.semantics { heading() })
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(current, style = style, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(option, style = style, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true, heightDp = 2200)
@Composable
private fun ComparisonPreview() {
    BankGuardTheme {
        AccountComparisonScreen(
            DemoAccountOptions.all.first(),
            DemoProfiles.unbanked,
            DemoAccountOptions.defaultInputs(DemoPersona.Unbanked),
            {}, {}, {},
        )
    }
}
