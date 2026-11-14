package com.example.bankguard.ui.screens.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoAccountOptions
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.domain.savings.SavingsEstimator
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.Money
import com.example.bankguard.model.SavingsInputs
import com.example.bankguard.ui.components.InfoNote
import com.example.bankguard.ui.components.NumberStepper
import com.example.bankguard.ui.components.ScreenColumn
import com.example.bankguard.ui.components.SectionHeader
import com.example.bankguard.ui.components.StatusBadge
import com.example.bankguard.ui.components.SubScreenHeader
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.components.TonalButton
import com.example.bankguard.ui.theme.BankGuardShapes
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.theme.Sizes
import com.example.bankguard.ui.theme.Spacing
import java.util.Locale

/** Lets the user replace the demo assumptions with their own; results update live. */
@Composable
fun SavingsCalculatorScreen(
    profile: DemoProfile,
    inputs: SavingsInputs,
    onInputsChange: (SavingsInputs) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasAccount = profile.account != null
    val ranked = remember(profile, inputs) { SavingsEstimator.rank(DemoAccountOptions.all, profile.account, inputs) }
    val current = remember(profile, inputs) { SavingsEstimator.currentYearlyCost(profile.account, inputs) }

    ScreenColumn(modifier = modifier) {
        SubScreenHeader(title = stringResource(R.string.calc_title), onBack = onBack)
        InfoNote(text = stringResource(R.string.calc_intro))

        SurfaceCard(verticalSpacing = Spacing.xs) {
            if (hasAccount) {
                NumberStepper(stringResource(R.string.calc_overdrafts), inputs.overdraftsPerYear, { onInputsChange(inputs.copy(overdraftsPerYear = it)) }, 0..30)
                NumberStepper(stringResource(R.string.calc_months_below), inputs.monthsBelowMinimum, { onInputsChange(inputs.copy(monthsBelowMinimum = it)) }, 0..12)
            } else {
                AltServiceField(inputs.currentAltServiceMonthly) { onInputsChange(inputs.copy(currentAltServiceMonthly = it)) }
            }
            NumberStepper(stringResource(R.string.calc_atm), inputs.outOfNetworkAtmPerMonth, { onInputsChange(inputs.copy(outOfNetworkAtmPerMonth = it)) }, 0..20)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Sizes.minTouch)
                    .toggleable(
                        value = inputs.hasDirectDeposit,
                        role = Role.Switch,
                        onValueChange = { onInputsChange(inputs.copy(hasDirectDeposit = it)) },
                    ),
            ) {
                Text(
                    stringResource(if (hasAccount) R.string.calc_dd else R.string.calc_dd_unb),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = inputs.hasDirectDeposit, onCheckedChange = null)
            }
        }

        SectionHeader(title = stringResource(R.string.calc_results))
        SurfaceCard(verticalSpacing = Spacing.sm) {
            ResultRow(stringResource(R.string.calc_today), current.format(), badge = null)
            ranked.forEach { estimate ->
                HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
                val (line, tone) = savingsLine(estimate)
                ResultRow(estimate.option.name, estimate.yearlyCost.format(), badge = { StatusBadge(line, tone) })
            }
        }
        TonalButton(text = stringResource(R.string.calc_reset), onClick = onReset, leadingIcon = R.drawable.ic_published_with_changes)
        InfoNote(text = stringResource(R.string.ds_hero_note))
    }
}

@Composable
private fun ResultRow(label: String, value: String, badge: (@Composable () -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(value, style = MoneyText.row, color = MaterialTheme.colorScheme.onSurface)
        }
        badge?.invoke()
    }
}

/** Dollar amount field. Keeps the last valid amount while the user is mid-edit. */
@Composable
private fun AltServiceField(value: Money, onValid: (Money) -> Unit) {
    fun format(m: Money) = String.format(Locale.US, "%.2f", m.cents / 100.0)
    var text by rememberSaveable { mutableStateOf(format(value)) }
    // Follow outside changes (e.g. "Reset to demo values"). While typing, value already matches.
    LaunchedEffect(value) {
        val typed = text.trim().removePrefix("$").replace(",", "").toBigDecimalOrNull()
        if (typed == null || typed.movePointRight(2).toLong() != value.cents) text = format(value)
    }
    val parsed = text.trim().removePrefix("$").replace(",", "").toBigDecimalOrNull()
    val valid = parsed != null && parsed.signum() >= 0 && parsed.scale() <= 2
    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            text = new
            new.trim().removePrefix("$").replace(",", "").toBigDecimalOrNull()
                ?.takeIf { it.signum() >= 0 && it.scale() <= 2 }
                ?.let { onValid(Money(it.movePointRight(2).toLong())) }
        },
        label = { Text(stringResource(R.string.calc_alt)) },
        supportingText = { Text(stringResource(if (valid) R.string.calc_alt_hint else R.string.calc_alt_error)) },
        isError = !valid,
        prefix = { Text("$") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        shape = BankGuardShapes.button,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.secondary, focusedLabelColor = MaterialTheme.colorScheme.secondary),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun CalculatorPreview() {
    BankGuardTheme {
        SavingsCalculatorScreen(DemoProfiles.underbanked, DemoAccountOptions.defaultInputs(DemoPersona.Underbanked), {}, {}, {})
    }
}
