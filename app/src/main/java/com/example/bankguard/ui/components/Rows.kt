package com.example.bankguard.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bankguard.ui.theme.BankGuardShapes
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.theme.Sizes
import com.example.bankguard.ui.theme.Spacing

/** Rounded square holding an icon, tinted by tone. */
@Composable
fun IconBox(
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.Neutral,
    size: Dp = Sizes.iconBox,
) {
    val colors = tone.colors()
    Box(
        modifier = modifier
            .size(size)
            .background(colors.container, BankGuardShapes.iconBox),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = colors.icon, modifier = Modifier.size(Sizes.icon * 0.85f))
    }
}

/** How an amount should read: plain, money coming in, or money going out. */
enum class AmountTone { Default, Positive, Negative }

/**
 * List row for a transaction, bill or fee: icon box, title and date, amount on the right.
 * [amount] arrives pre-formatted (formatting lives in the data layer).
 */
@Composable
fun MoneyRow(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    amount: String,
    modifier: Modifier = Modifier,
    amountTone: AmountTone = AmountTone.Default,
    amountCaption: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val amountColor = when (amountTone) {
        AmountTone.Default -> MaterialTheme.colorScheme.onSurface
        AmountTone.Positive -> BankGuardTheme.colors.success
        AmountTone.Negative -> BankGuardTheme.colors.critical
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        IconBox(iconRes = iconRes)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // Capped so a long caption can't squeeze the title out.
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 136.dp)) {
            Text(amount, style = MoneyText.row, color = amountColor, textAlign = TextAlign.End)
            if (amountCaption != null) {
                Text(amountCaption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, maxLines = 2)
            }
        }
    }
}

/** Small card showing one figure and its label (from the Stitch Overview stat row). */
@Composable
fun StatTile(
    @DrawableRes iconRes: Int,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.Brand,
) {
    SurfaceCard(modifier = modifier, verticalSpacing = Spacing.xs) {
        IconBox(iconRes = iconRes, tone = tone, size = 36.dp)
        Text(value, style = MoneyText.large, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}

/** Tappable shortcut tile with an icon and a short label (from the Stitch Home tools row). */
@Composable
fun QuickActionTile(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(vertical = Spacing.md, horizontal = Spacing.sm),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            IconBox(iconRes = iconRes, tone = StatusTone.Info, size = 36.dp)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}
