package com.example.bankguard.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bankguard.R
import com.example.bankguard.ui.theme.BankGuardShapes
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.Navy
import com.example.bankguard.ui.theme.Sizes
import com.example.bankguard.ui.theme.Spacing

/** Soft navy-tinted shadow used under resting cards (Stitch elevation level 1). */
fun Modifier.cardShadow(): Modifier = shadow(
    elevation = 4.dp,
    shape = BankGuardShapes.card,
    clip = false,
    ambientColor = Navy.copy(alpha = 0.06f),
    spotColor = Navy.copy(alpha = 0.10f),
)

/** Standard white card with a hairline border. Pass [onClick] to make the whole card tappable. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(Spacing.card),
    verticalSpacing: Dp = Spacing.sm,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier
        .fillMaxWidth()
        .cardShadow()
    val border = BorderStroke(Sizes.hairline, BankGuardTheme.colors.cardBorder)
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content,
        )
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = base,
            shape = BankGuardShapes.card,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = border,
            content = body,
        )
    } else {
        Surface(
            modifier = base,
            shape = BankGuardShapes.card,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = border,
            content = body,
        )
    }
}

/** Navy gradient card that anchors the top of each screen. Content defaults to white text. */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = BankGuardTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .cardShadow()
            .clip(BankGuardShapes.card)
            .background(Brush.linearGradient(listOf(colors.heroStart, colors.heroEnd))),
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onHero) {
            Column(
                modifier = Modifier.padding(Spacing.hero),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                content = content,
            )
        }
    }
}

/** Small uppercase caption on a hero card, e.g. "SIMULATED AVAILABLE". */
@Composable
fun HeroLabel(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = BankGuardTheme.colors.onHeroMuted,
        textAlign = textAlign,
        modifier = modifier,
    )
}

/** Slightly lighter panel inside a hero card, for grouped rows. */
@Composable
fun HeroInset(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BankGuardTheme.colors.heroInset, BankGuardShapes.inset)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        content = content,
    )
}

/** Tinted panel inside a card, used for explanations and callouts. */
@Composable
fun InsetPanel(
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.Neutral,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = tone.colors()
    CompositionLocalProvider(LocalContentColor provides colors.content) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(colors.container, BankGuardShapes.inset)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            content = content,
        )
    }
}

/** Short note with an icon, for disclaimers and things BankGuard can't promise. */
@Composable
fun InfoNote(
    text: String,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.Info,
    @DrawableRes iconRes: Int = R.drawable.ic_info,
) {
    val colors = tone.colors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.container, BankGuardShapes.inset)
            .padding(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colors.icon,
            modifier = Modifier
                .padding(top = Spacing.xxs)
                .size(Sizes.iconSmall),
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = colors.content)
    }
}

/**
 * Alert with an urgency badge, icon, title and explanation (from the Stitch Risk Alerts cards).
 * Put supported actions only in [actions].
 */
@Composable
fun AlertCard(
    title: String,
    body: String,
    badgeText: String,
    tone: StatusTone,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    meta: String? = null,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    SurfaceCard(modifier = modifier, verticalSpacing = Spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(text = badgeText, tone = tone)
            if (meta != null) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBox(iconRes = iconRes, tone = tone)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
        }
        Text(text = body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        actions()
    }
}

/** Hero header for a pillar screen: icon, pillar name, title and one-line description. */
@Composable
fun PillarHero(
    @DrawableRes iconRes: Int,
    label: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    HeroCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Box(
                modifier = Modifier
                    .size(Sizes.iconBox)
                    .background(BankGuardTheme.colors.heroInset, BankGuardShapes.iconBox),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(iconRes), contentDescription = null, tint = BankGuardTheme.colors.heroAccent, modifier = Modifier.size(Sizes.icon))
            }
            HeroLabel(text = label)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .padding(top = Spacing.xs)
                .semantics { heading() },
        )
        Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, color = BankGuardTheme.colors.onHeroMuted)
    }
}
