package com.example.bankguard.ui.screens.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoAccountOptions
import com.example.bankguard.model.AccountOption
import com.example.bankguard.model.ReferralRecord
import com.example.bankguard.model.ReferralStatus
import com.example.bankguard.ui.components.BulletList
import com.example.bankguard.ui.components.HeroCard
import com.example.bankguard.ui.components.HeroLabel
import com.example.bankguard.ui.components.IconBox
import com.example.bankguard.ui.components.InfoNote
import com.example.bankguard.ui.components.PrimaryButton
import com.example.bankguard.ui.components.ScreenColumn
import com.example.bankguard.ui.components.SectionHeader
import com.example.bankguard.ui.components.StatusBadge
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SubScreenHeader
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.screens.recover.formatTimestamp
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.Spacing

/**
 * The selected bank's simulated account-opening page, reached from Discover through a
 * BankGuard referral. It collects nothing and opens nothing: "Continue to bank" only marks the
 * simulated referral as continued and explains what a live version would do.
 */
@Composable
fun AccountOpeningScreen(
    option: AccountOption,
    referral: ReferralRecord,
    onContinueToBank: () -> Unit,
    onBackToDiscover: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val continued = referral.status == ReferralStatus.ContinuedToBank

    ScreenColumn(modifier = modifier) {
        SubScreenHeader(title = stringResource(R.string.ao_screen_title), onBack = onBack)
        StatusBadge(stringResource(R.string.ao_demo_label), StatusTone.Demo)

        HeroCard {
            HeroLabel(option.name)
            Text(
                stringResource(R.string.ao_heading, option.institution),
                style = MaterialTheme.typography.headlineSmall,
                color = BankGuardTheme.colors.heroAccent,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.ao_referred, option.institution),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (continued) {
            ReadyCard(option, onBackToDiscover)
        } else {
            PrimaryButton(
                text = stringResource(R.string.ao_continue),
                onClick = onContinueToBank,
                trailingIcon = R.drawable.ic_arrow_forward,
            )
        }

        SectionHeader(title = stringResource(R.string.ao_account_title))
        SurfaceCard(verticalSpacing = Spacing.xs) {
            FactRow(stringResource(R.string.fact_monthly), monthlyFeeText(option))
            FactRow(stringResource(R.string.fact_opening), option.openingDeposit.format())
            FactRow(stringResource(R.string.fact_overdraft), overdraftText(option))
            FactRow(stringResource(R.string.fact_minimum), option.minimumBalance.format())
        }

        SectionHeader(title = stringResource(R.string.ao_benefits_title))
        SurfaceCard { BulletList(option.features) }

        SectionHeader(title = stringResource(R.string.ao_referral_title))
        SurfaceCard(modifier = Modifier.testTag("referral_card"), verticalSpacing = Spacing.xs) {
            FactRow(stringResource(R.string.ao_referral_id), referral.referralId)
            FactRow(stringResource(R.string.ao_referral_source), stringResource(R.string.ao_referral_source_bankguard))
            FactRow(
                stringResource(R.string.ao_referral_status),
                stringResource(if (continued) R.string.ao_status_continued else R.string.ao_status_initiated),
            )
            Text(
                stringResource(R.string.ao_referral_started, formatTimestamp(referral.createdAtMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
            Text(
                stringResource(R.string.ao_referral_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        InfoNote(
            text = stringResource(R.string.ao_simulation_note, option.institution),
            tone = StatusTone.Demo,
            iconRes = R.drawable.ic_science,
        )
    }
}

/** Shown once the user continues: what a live version would do next. */
@Composable
private fun ReadyCard(option: AccountOption, onBackToDiscover: () -> Unit) {
    SurfaceCard(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalSpacing = Spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBox(iconRes = R.drawable.ic_check_circle, tone = StatusTone.Success)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.ao_ready_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        Text(
            stringResource(R.string.ao_ready_body, option.institution),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        PrimaryButton(text = stringResource(R.string.ao_back_to_discover), onClick = onBackToDiscover)
    }
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun AccountOpeningPreview() {
    val option = DemoAccountOptions.all.first()
    BankGuardTheme {
        AccountOpeningScreen(
            option = option,
            referral = ReferralRecord("REF-BG-0001", option.bankId, option.id, createdAtMillis = 0L),
            onContinueToBank = {}, onBackToDiscover = {}, onBack = {},
        )
    }
}
