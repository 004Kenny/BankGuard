package com.example.bankguard.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.bankguard.ui.theme.BankGuardShapes
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.Sizes
import com.example.bankguard.ui.theme.Spacing

/**
 * Large radio-style option card (from the Stitch demo-profile selection).
 * Put several inside a `Modifier.selectableGroup()` column.
 */
@Composable
fun SelectableOptionCard(
    @DrawableRes iconRes: Int,
    title: String,
    badge: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .cardShadow()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        shape = BankGuardShapes.card,
        color = if (selected) scheme.primaryContainer.copy(alpha = 0.55f) else scheme.surfaceContainer,
        border = if (selected) BorderStroke(2.dp, scheme.secondary) else BorderStroke(Sizes.hairline, BankGuardTheme.colors.cardBorder),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.card),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            IconBox(iconRes = iconRes, tone = if (selected) StatusTone.Brand else StatusTone.Neutral, size = 48.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                StatusBadge(text = badge, tone = StatusTone.Info)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            // The whole card is the radio target; this is only the visual indicator.
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = scheme.secondary),
                modifier = Modifier.align(Alignment.Top),
            )
        }
    }
}
