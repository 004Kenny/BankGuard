package com.example.bankguard.ui.screens.shared

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.bankguard.R
import com.example.bankguard.model.Envelope
import com.example.bankguard.model.Money
import com.example.bankguard.model.ScheduledItem
import com.example.bankguard.ui.components.AmountTone
import com.example.bankguard.ui.components.BudgetProgress
import com.example.bankguard.ui.components.MoneyRow
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.Spacing
import com.example.bankguard.ui.util.iconRes
import com.example.bankguard.ui.util.relativeDay

/** Money in reads green; money out stays neutral so routine bills don't look like alarms. */
fun Money.tone(): AmountTone = if (cents > 0) AmountTone.Positive else AmountTone.Default

/** Card listing upcoming bills and deposits, soonest first. */
@Composable
fun ScheduledListCard(
    items: List<ScheduledItem>,
    modifier: Modifier = Modifier,
) {
    val res = LocalResources.current
    SurfaceCard(modifier = modifier, verticalSpacing = 0.dp) {
        items.sortedBy { it.dayOffset }.forEachIndexed { i, item ->
            if (i > 0) HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
            MoneyRow(
                iconRes = item.kind.iconRes(),
                title = item.title,
                subtitle = res.relativeDay(item.dayOffset),
                amount = item.amount.formatSigned(),
                amountTone = item.amount.tone(),
                amountCaption = item.note,
            )
        }
    }
}

/** Cash envelopes with progress bars, plus the prepaid card balance when there is one. */
@Composable
fun EnvelopeBudgetCard(
    envelopes: List<Envelope>,
    prepaidBalance: Money?,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier, verticalSpacing = Spacing.lg) {
        envelopes.forEach { env ->
            val fraction = if (env.limit.cents > 0) env.used.cents.toFloat() / env.limit.cents else 0f
            BudgetProgress(
                label = env.name,
                valueText = stringResource(R.string.unb_budget_value, env.used.format(), env.limit.format()),
                fraction = fraction,
                tone = if (fraction >= 1f) StatusTone.Success else StatusTone.Info,
            )
        }
        prepaidBalance?.let { balance ->
            HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
            MoneyRow(
                iconRes = R.drawable.ic_account_balance_wallet,
                title = stringResource(R.string.unb_prepaid_balance),
                subtitle = stringResource(R.string.simulated_data_title),
                amount = balance.format(),
            )
        }
    }
}
