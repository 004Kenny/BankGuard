package com.example.bankguard.ui.screens.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoAccountOptions
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.domain.savings.SavingsEstimator
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.SavingsInputs
import com.example.bankguard.ui.components.DisclosureCard
import com.example.bankguard.ui.components.HeroCard
import com.example.bankguard.ui.components.HeroLabel
import com.example.bankguard.ui.components.InfoNote
import com.example.bankguard.ui.components.ScreenColumn
import com.example.bankguard.ui.components.SectionHeader
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.components.TonalButton
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.theme.Spacing

/**
 * Discover tab: sample low-fee accounts ranked by the user's estimated yearly savings.
 * Visual reference: Stitch "Better Options", without APY, debt refinancing, APR or
 * "no kickback" claims, and with the proposed referral model disclosed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(
    profile: DemoProfile,
    inputs: SavingsInputs,
    onCompare: (optionId: String) -> Unit,
    onAdjust: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filterNames by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val filters = filterNames.map(AccountFilter::valueOf).toSet()
    val ranked = remember(profile, inputs) { SavingsEstimator.rank(DemoAccountOptions.all, profile.account, inputs) }
    val current = remember(profile, inputs) { SavingsEstimator.currentYearlyCost(profile.account, inputs) }
    val best = ranked.firstOrNull()?.takeIf { it.yearlySavings.cents > 0 }
    val shown = ranked.filter { estimate -> filters.all { it.matches(estimate.option) } }

    ScreenColumn(modifier = modifier) {
        HeroCard {
            HeroLabel(stringResource(R.string.ds_hero_label))
            if (best != null) {
                Text(best.yearlySavings.format(), style = MoneyText.hero, color = BankGuardTheme.colors.heroAccent)
                Text(stringResource(R.string.ds_hero_with, best.option.name), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(stringResource(R.string.ds_hero_none), style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                stringResource(
                    if (profile.persona == DemoPersona.Underbanked) R.string.ds_hero_current_ub else R.string.ds_hero_current_unb,
                    current.format(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = BankGuardTheme.colors.onHeroMuted,
            )
            Text(stringResource(R.string.ds_hero_note), style = MaterialTheme.typography.bodySmall, color = BankGuardTheme.colors.onHeroMuted)
        }
        TonalButton(text = stringResource(R.string.ds_adjust), onClick = onAdjust, leadingIcon = R.drawable.ic_insights)

        SectionHeader(title = stringResource(R.string.ds_filters_title))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AccountFilter.entries.forEach { filter ->
                val on = filter in filters
                FilterChip(
                    selected = on,
                    onClick = { filterNames = (if (on) filters - filter else filters + filter).map { it.name } },
                    label = { Text(stringResource(filter.labelRes)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                )
            }
        }

        SectionHeader(title = stringResource(R.string.ds_options_title))
        if (shown.isEmpty()) {
            SurfaceCard { Text(stringResource(R.string.ds_none_match), style = MaterialTheme.typography.bodyMedium) }
        }
        shown.forEach { estimate ->
            AccountOptionCard(estimate = estimate, best = estimate == best, onCompare = { onCompare(estimate.option.id) })
        }

        DisclosureCard(title = stringResource(R.string.disc_title), body = stringResource(R.string.disc_body))
        InfoNote(text = stringResource(R.string.ds_footer), tone = StatusTone.Demo, iconRes = R.drawable.ic_science)
    }
}

@Preview(showBackground = true, heightDp = 2600)
@Composable
private fun DiscoverPreview() {
    BankGuardTheme {
        DiscoverScreen(DemoProfiles.underbanked, DemoAccountOptions.defaultInputs(DemoPersona.Underbanked), {}, {})
    }
}
