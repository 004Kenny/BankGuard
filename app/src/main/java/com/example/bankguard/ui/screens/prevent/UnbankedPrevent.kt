package com.example.bankguard.ui.screens.prevent

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.ui.BankGuardTab
import com.example.bankguard.ui.components.BulletList
import com.example.bankguard.ui.components.InfoNote
import com.example.bankguard.ui.components.InsetPanel
import com.example.bankguard.ui.components.PillarHero
import com.example.bankguard.ui.components.ScreenColumn
import com.example.bankguard.ui.components.SecondaryButton
import com.example.bankguard.ui.components.SectionHeader
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.screens.shared.EnvelopeBudgetCard
import com.example.bankguard.ui.screens.shared.ScheduledListCard
import com.example.bankguard.ui.theme.BankGuardTheme

/**
 * Prevent for someone without a bank account: a cash plan instead of overdraft tools,
 * which only apply to bank accounts.
 */
@Composable
internal fun UnbankedPrevent(
    profile: DemoProfile,
    onOpenTab: (BankGuardTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenColumn(modifier = modifier) {
        PillarHero(
            iconRes = R.drawable.ic_shield,
            label = stringResource(R.string.nav_prevent),
            title = stringResource(R.string.pvu_title),
            subtitle = stringResource(R.string.pvu_subtitle),
        )

        SectionHeader(title = stringResource(R.string.pvu_week_title))
        ScheduledListCard(profile.scheduled)

        SectionHeader(title = stringResource(R.string.pvu_budget_title))
        EnvelopeBudgetCard(profile.envelopes, profile.prepaidCardBalance)

        SurfaceCard {
            InsetPanel(tone = StatusTone.Info) {
                Text(
                    stringResource(R.string.pvu_tips_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.semantics { heading() },
                )
                BulletList(
                    listOf(
                        stringResource(R.string.pvu_tip_deposit),
                        stringResource(R.string.pvu_tip_atm),
                        stringResource(R.string.pvu_tip_discover),
                    ),
                )
            }
            SecondaryButton(
                text = stringResource(R.string.unb_opportunity_action),
                onClick = { onOpenTab(BankGuardTab.Discover) },
                trailingIcon = R.drawable.ic_arrow_forward,
            )
        }

        InfoNote(text = stringResource(R.string.pvu_bank_only))
        InfoNote(text = stringResource(R.string.unb_footer), tone = StatusTone.Demo, iconRes = R.drawable.ic_science)
    }
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun UnbankedPreventPreview() {
    BankGuardTheme { UnbankedPrevent(DemoProfiles.unbanked, onOpenTab = {}) }
}
