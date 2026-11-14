package com.example.bankguard.ui.screens.streaks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoStreakHistory
import com.example.bankguard.domain.streaks.DayStatus
import com.example.bankguard.domain.streaks.DaySummary
import com.example.bankguard.domain.streaks.StreakBadge
import com.example.bankguard.domain.streaks.StreakEngine
import com.example.bankguard.domain.streaks.StreakSummary
import com.example.bankguard.ui.CompletedDay
import com.example.bankguard.ui.components.BulletList
import com.example.bankguard.ui.components.HeroCard
import com.example.bankguard.ui.components.HeroInset
import com.example.bankguard.ui.components.HeroLabel
import com.example.bankguard.ui.components.InfoNote
import com.example.bankguard.ui.components.ScreenColumn
import com.example.bankguard.ui.components.SectionHeader
import com.example.bankguard.ui.components.StatusBadge
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SubScreenHeader
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.components.TonalButton
import com.example.bankguard.ui.components.colors
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.theme.Spacing

/** Streak summary, the last 7 days, the badge collection and the demo-day control. */
@Composable
fun StreakScreen(
    summary: StreakSummary,
    lastCompletedDay: CompletedDay?,
    onCompleteDemoDay: () -> Unit,
    onDismissResult: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenColumn(modifier = modifier) {
        SubScreenHeader(title = stringResource(R.string.sk_screen_title), onBack = onBack)

        DayResult(lastCompletedDay, onDismissResult)

        HeroCard {
            HeroLabel(stringResource(R.string.sk_label_streak))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Flame(active = summary.currentStreak > 0)
                Text(
                    pluralStringResource(R.plurals.sk_days, summary.currentStreak, summary.currentStreak),
                    style = MoneyText.hero,
                    color = BankGuardTheme.colors.heroAccent,
                    modifier = Modifier.testTag("streak_hero_days"),
                )
            }
            Text(streakMessage(summary), style = MaterialTheme.typography.bodyMedium)
            HeroInset {
                HeroStat(stringResource(R.string.sk_label_stars), "⭐ " + pluralStringResource(R.plurals.sk_stars, summary.totalStars, summary.totalStars))
                HeroStat(stringResource(R.string.sk_label_longest), pluralStringResource(R.plurals.sk_days, summary.longestStreak, summary.longestStreak))
                HeroStat(
                    stringResource(R.string.sk_label_next),
                    summary.nextBadge?.let { "${it.emoji} ${stringResource(it.nameRes())}" } ?: "—",
                )
            }
        }
        SurfaceCard { NextMilestone(summary) }

        SectionHeader(title = stringResource(R.string.sk_recent_title))
        SurfaceCard(verticalSpacing = Spacing.md) {
            Row(modifier = Modifier.fillMaxWidth()) {
                summary.recent(7).forEach { d -> DayCell(d, summary.currentDay, Modifier.weight(1f)) }
            }
            Legend()
        }

        SectionHeader(title = stringResource(R.string.sk_badges_title))
        StreakBadge.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                pair.forEach { BadgeTile(it, summary.isUnlocked(it), Modifier.weight(1f)) }
            }
        }

        // Demo-only control, kept apart and labeled so it can't be mistaken for a real feature.
        SurfaceCard(verticalSpacing = Spacing.sm) {
            StatusBadge(stringResource(R.string.sk_demo_title), StatusTone.Demo)
            Text(stringResource(R.string.sk_demo_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TonalButton(text = stringResource(R.string.sk_demo_button), onClick = onCompleteDemoDay, leadingIcon = R.drawable.ic_science)
        }

        SectionHeader(title = stringResource(R.string.sk_encourage_title))
        SurfaceCard(verticalSpacing = Spacing.sm) {
            BulletList(
                listOf(
                    stringResource(R.string.sk_encourage_1),
                    stringResource(R.string.sk_encourage_2),
                    stringResource(R.string.sk_encourage_3),
                ),
            )
        }
        InfoNote(text = stringResource(R.string.sk_rule))
        InfoNote(text = stringResource(R.string.sk_recover_note))
        InfoNote(text = stringResource(R.string.sk_demo_history_note), tone = StatusTone.Demo, iconRes = R.drawable.ic_science)
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = BankGuardTheme.colors.onHeroMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.End)
    }
}

private fun DayStatus.symbol(): String = when (this) {
    DayStatus.OverdraftFree -> "⭐"
    DayStatus.Overdraft -> "✕"
    DayStatus.Unknown -> "?"
    DayStatus.InProgress -> "…"
}

private fun DayStatus.tone(): StatusTone = when (this) {
    DayStatus.OverdraftFree -> StatusTone.Success
    DayStatus.Overdraft -> StatusTone.Critical
    DayStatus.Unknown -> StatusTone.Neutral
    DayStatus.InProgress -> StatusTone.Info
}

@Composable
private fun DayStatus.description(): String = stringResource(
    when (this) {
        DayStatus.OverdraftFree -> R.string.sk_status_free
        DayStatus.Overdraft -> R.string.sk_status_overdraft
        DayStatus.Unknown -> R.string.sk_status_unknown
        DayStatus.InProgress -> R.string.sk_status_progress
    },
)

@Composable
private fun DayCell(day: DaySummary, currentDay: Int, modifier: Modifier) {
    val label = if (day.day == currentDay) stringResource(R.string.sk_today) else weekdayLabel(day.day, currentDay)
    val description = day.status.description()
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$label: $description" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        StatusDot(day.status)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun StatusDot(status: DayStatus, size: Int = 36) {
    val colors = status.tone().colors()
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(colors.container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(status.symbol(), style = MaterialTheme.typography.titleSmall, color = colors.content)
    }
}

@Composable
private fun Legend() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        listOf(
            DayStatus.OverdraftFree to R.string.sk_legend_free,
            DayStatus.Overdraft to R.string.sk_legend_overdraft,
            DayStatus.Unknown to R.string.sk_legend_unknown,
            DayStatus.InProgress to R.string.sk_legend_progress,
        ).chunked(2).forEach { row ->
            Row {
                row.forEach { (status, res) ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        StatusDot(status, size = 24)
                        Text(stringResource(res), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeTile(badge: StreakBadge, unlocked: Boolean, modifier: Modifier) {
    val name = stringResource(badge.nameRes())
    val days = stringResource(R.string.sk_badge_requirement, badge.days)
    val state = stringResource(if (unlocked) R.string.sk_badge_unlocked else R.string.sk_badge_locked)
    SurfaceCard(
        modifier = modifier
            .testTag("badge_${badge.name}")
            .clearAndSetSemantics { contentDescription = "$name, $days. $state" },
        verticalSpacing = Spacing.xs,
    ) {
        Text(
            if (unlocked) badge.emoji else "🔒",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.alpha(if (unlocked) 1f else 0.5f),
        )
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(days, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xxs))
        StatusBadge(state, if (unlocked) StatusTone.Success else StatusTone.Neutral)
    }
}

/** After "Complete demo day": a celebration for a new badge, otherwise the day's result. */
@Composable
private fun DayResult(result: CompletedDay?, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = result != null,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut(),
    ) {
        if (result == null) return@AnimatedVisibility
        val badge = result.newBadges.lastOrNull()
        val tone = when {
            badge != null || result.starAwarded -> StatusTone.Success
            result.status == DayStatus.Overdraft -> StatusTone.Warning
            else -> StatusTone.Neutral
        }
        SurfaceCard(
            modifier = Modifier
                .testTag("day_result")
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalSpacing = Spacing.sm,
        ) {
            if (badge != null) {
                // A single bounce of the new badge: the celebration.
                val pop = remember(result) { Animatable(0.4f) }
                LaunchedEffect(result) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(tone.colors().container, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(badge.emoji, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.scale(pop.value))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        StatusBadge(stringResource(R.string.sk_celebrate_label), StatusTone.Success)
                        Text(
                            stringResource(badge.nameRes()),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(top = Spacing.xs)
                                .semantics { heading() },
                        )
                    }
                }
                Text(
                    stringResource(R.string.sk_celebrate_body, stringResource(badge.nameRes()), badge.days),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("⭐ " + stringResource(R.string.sk_day_star), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            } else {
                val (title, body) = when (result.status) {
                    DayStatus.OverdraftFree -> R.string.sk_day_star to R.string.sk_day_star_body
                    DayStatus.Overdraft -> R.string.sk_day_overdraft to R.string.sk_day_overdraft_body
                    else -> R.string.sk_day_unknown to R.string.sk_day_unknown_body
                }
                Text(
                    (if (result.starAwarded) "⭐ " else "") + stringResource(title),
                    style = MaterialTheme.typography.titleMedium,
                    color = tone.colors().content,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TonalButton(text = stringResource(R.string.sk_dismiss), onClick = onDismiss)
        }
    }
}

@Preview(showBackground = true, heightDp = 2200)
@Composable
private fun StreakScreenPreview() {
    BankGuardTheme {
        StreakScreen(
            summary = StreakEngine.summarize(DemoStreakHistory.lowestBalances, DemoStreakHistory.START_DAY),
            lastCompletedDay = null,
            onCompleteDemoDay = {}, onDismissResult = {}, onBack = {},
        )
    }
}
