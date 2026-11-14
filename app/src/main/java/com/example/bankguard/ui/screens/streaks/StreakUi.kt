package com.example.bankguard.ui.screens.streaks

import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoStreakHistory
import com.example.bankguard.domain.streaks.StreakBadge
import com.example.bankguard.domain.streaks.StreakEngine
import com.example.bankguard.domain.streaks.StreakSummary
import com.example.bankguard.ui.components.BudgetProgress
import com.example.bankguard.ui.components.SecondaryButton
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@StringRes
internal fun StreakBadge.nameRes(): Int = when (this) {
    StreakBadge.GettingStarted -> R.string.sk_badge_getting_started
    StreakBadge.MoneyGuardian -> R.string.sk_badge_money_guardian
    StreakBadge.BalanceProtector -> R.string.sk_badge_balance_protector
    StreakBadge.OverdraftChampion -> R.string.sk_badge_overdraft_champion
    StreakBadge.FinancialGuardian -> R.string.sk_badge_financial_guardian
    StreakBadge.BankGuardLegend -> R.string.sk_badge_legend
}

/**
 * Short weekday for a simulated day, on the user's local calendar: the day in progress is
 * today's date, earlier days count back from it. The device clock is only read for display.
 */
internal fun weekdayLabel(day: Int, currentDay: Int, locale: Locale = Locale.getDefault()): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, day - currentDay)
    return SimpleDateFormat("EEE", locale).format(cal.time)
}

/** A gently pulsing flame, the only looping animation in the feature. */
@Composable
internal fun Flame(modifier: Modifier = Modifier, active: Boolean = true) {
    val pulse = if (active) {
        val transition = rememberInfiniteTransition(label = "flame")
        val s by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "flameScale",
        )
        s
    } else {
        1f
    }
    Text("🔥", style = MaterialTheme.typography.headlineMedium, modifier = modifier.scale(pulse))
}

/** The supportive one-liner for the current state of the streak. */
@Composable
internal fun streakMessage(summary: StreakSummary): String = when {
    summary.currentStreak > 0 -> pluralStringResource(R.plurals.sk_msg_streak, summary.currentStreak, summary.currentStreak)
    summary.totalStars > 0 -> pluralStringResource(R.plurals.sk_msg_restart, summary.totalStars, summary.totalStars)
    else -> stringResource(R.string.sk_msg_first)
}

/** Progress from the current streak toward the next badge, or a note when all are unlocked. */
@Composable
internal fun NextMilestone(summary: StreakSummary) {
    val next = summary.nextBadge
    if (next == null) {
        Text(stringResource(R.string.sk_all_unlocked), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        return
    }
    BudgetProgress(
        label = stringResource(
            R.string.sk_next_badge,
            "${next.emoji} ${stringResource(next.nameRes())}",
            pluralStringResource(R.plurals.sk_days, next.days, next.days),
        ),
        valueText = stringResource(R.string.sk_progress_value, summary.currentStreak.coerceAtMost(next.days), next.days),
        fraction = summary.currentStreak.toFloat() / next.days,
        tone = StatusTone.Success,
    )
}

/** Streak card on the underbanked Home dashboard. */
@Composable
fun StreakHomeCard(summary: StreakSummary, onViewAchievements: () -> Unit, modifier: Modifier = Modifier) {
    SurfaceCard(modifier = modifier.testTag("streak_card"), verticalSpacing = Spacing.md) {
        Text(
            "⭐ " + stringResource(R.string.sk_card_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            StreakStat(
                label = stringResource(R.string.sk_label_streak),
                value = pluralStringResource(R.plurals.sk_days, summary.currentStreak, summary.currentStreak),
                modifier = Modifier.weight(1f),
                leading = { Flame(active = summary.currentStreak > 0) },
            )
            StreakStat(
                label = stringResource(R.string.sk_label_stars),
                value = pluralStringResource(R.plurals.sk_stars, summary.totalStars, summary.totalStars),
                modifier = Modifier.weight(1f),
                leading = { Text("⭐", style = MaterialTheme.typography.headlineMedium) },
            )
        }
        Text(streakMessage(summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        NextMilestone(summary)
        SecondaryButton(text = stringResource(R.string.sk_view), onClick = onViewAchievements, trailingIcon = R.drawable.ic_arrow_forward)
    }
}

/** Emoji plus a big value over a small label; read as "label: value". */
@Composable
internal fun StreakStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$label: $value" },
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StreakHomeCardPreview() {
    BankGuardTheme {
        StreakHomeCard(StreakEngine.summarize(DemoStreakHistory.lowestBalances, DemoStreakHistory.START_DAY), {})
    }
}
