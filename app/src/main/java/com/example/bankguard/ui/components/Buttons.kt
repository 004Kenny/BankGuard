package com.example.bankguard.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bankguard.ui.theme.BankGuardShapes
import com.example.bankguard.ui.theme.Sizes
import com.example.bankguard.ui.theme.Spacing

/** Main action on a screen or card: solid navy, 52dp tall. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes leadingIcon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = BankGuardShapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.buttonHeight),
    ) { ButtonContent(text, leadingIcon, trailingIcon) }
}

/** Alternative action: teal outline, transparent fill. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes leadingIcon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = BankGuardShapes.button,
        border = BorderStroke(1.5.dp, if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.buttonHeight),
    ) { ButtonContent(text, leadingIcon, trailingIcon) }
}

/** Low-emphasis action: tinted surface, navy text. */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes leadingIcon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = BankGuardShapes.button,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.buttonHeight),
    ) { ButtonContent(text, leadingIcon, trailingIcon) }
}

@Composable
private fun RowScope.ButtonContent(text: String, leadingIcon: Int?, trailingIcon: Int?) {
    if (leadingIcon != null) {
        Icon(painterResource(leadingIcon), contentDescription = null, modifier = Modifier.size(Sizes.iconSmall))
        Spacer(Modifier.width(Spacing.sm))
    }
    Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    if (trailingIcon != null) {
        Spacer(Modifier.width(Spacing.sm))
        Icon(painterResource(trailingIcon), contentDescription = null, modifier = Modifier.size(Sizes.iconSmall))
    }
}
