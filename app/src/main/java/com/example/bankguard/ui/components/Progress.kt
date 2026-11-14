package com.example.bankguard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bankguard.R
import com.example.bankguard.ui.theme.BankGuardShapes
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.theme.Spacing

@Immutable
data class BarSegment(val weight: Float, val color: Color)

/** Horizontal bar split into colored parts (Stitch "projected dip" and spending-split bars). */
@Composable
fun SegmentedBar(
    segments: List<BarSegment>,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    gap: Dp = 2.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(BankGuardShapes.pill)
            .background(trackColor),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        segments.filter { it.weight > 0f }.forEach { segment ->
            Box(
                Modifier
                    .weight(segment.weight)
                    .fillMaxHeight()
                    .background(segment.color),
            )
        }
    }
}

/** Labeled progress toward a limit, e.g. an envelope budget. [fraction] is clamped to 0..1. */
@Composable
fun BudgetProgress(
    label: String,
    valueText: String,
    fraction: Float,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.Info,
) {
    val clamped = fraction.coerceIn(0f, 1f)
    val barColor = when (tone) {
        StatusTone.Critical -> BankGuardTheme.colors.criticalIcon
        StatusTone.Warning -> BankGuardTheme.colors.warningIcon
        StatusTone.Success -> BankGuardTheme.colors.successIcon
        else -> MaterialTheme.colorScheme.secondary
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(valueText, style = MoneyText.row, color = MaterialTheme.colorScheme.onSurface)
        }
        SegmentedBar(
            segments = listOf(BarSegment(clamped, barColor), BarSegment(1f - clamped, Color.Transparent)),
            gap = 0.dp,
        )
    }
}

enum class StepState { Done, Current, Upcoming }

@Immutable
data class TrackerStep(val title: String, val description: String, val state: StepState)

/** Vertical step list with a connecting line (from the Stitch Fee Recovery timeline). */
@Composable
fun StepTracker(
    steps: List<TrackerStep>,
    modifier: Modifier = Modifier,
    doneLabel: String = stringResource(R.string.step_done),
    currentLabel: String = stringResource(R.string.step_current),
    upcomingLabel: String = stringResource(R.string.step_upcoming),
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            val isLast = index == steps.lastIndex
            val stateLabel = when (step.state) {
                StepState.Done -> doneLabel
                StepState.Current -> currentLabel
                StepState.Upcoming -> upcomingLabel
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .semantics(mergeDescendants = true) { contentDescription = "${step.title}. $stateLabel. ${step.description}" },
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                    StepDot(step.state)
                    if (!isLast) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .weight(1f)
                                .background(if (step.state == StepState.Done) scheme.secondary else scheme.outlineVariant),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = if (isLast) 0.dp else Spacing.lg),
                ) {
                    Text(
                        step.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (step.state == StepState.Upcoming) scheme.onSurfaceVariant else scheme.onSurface,
                    )
                    Text(step.description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StepDot(state: StepState) {
    val scheme = MaterialTheme.colorScheme
    when (state) {
        StepState.Done -> Icon(
            painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            tint = scheme.secondary,
            modifier = Modifier.size(24.dp),
        )
        StepState.Current -> Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(scheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.size(10.dp).clip(CircleShape).background(scheme.secondary)) }
        StepState.Upcoming -> Box(
            Modifier
                .size(24.dp)
                .padding(4.dp)
                .clip(CircleShape)
                .background(scheme.outlineVariant),
        )
    }
    Spacer(Modifier.height(Spacing.xs))
}

/** Thin aqua accent bar used on hero cards. */
@Composable
fun HeroAccentBar(modifier: Modifier = Modifier, fraction: Float = 1f) {
    SegmentedBar(
        segments = listOf(
            BarSegment(fraction.coerceIn(0f, 1f), BankGuardTheme.colors.heroAccent),
            BarSegment(1f - fraction.coerceIn(0f, 1f), Color.Transparent),
        ),
        modifier = modifier,
        height = 4.dp,
        gap = 0.dp,
        trackColor = BankGuardTheme.colors.heroInset,
    )
}
