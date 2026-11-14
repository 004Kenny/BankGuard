package com.example.bankguard.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.data.simulated.SimulatedBankMode
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.ui.components.IconBox
import com.example.bankguard.ui.components.InfoNote
import com.example.bankguard.ui.components.SecondaryButton
import com.example.bankguard.ui.components.StatusBadge
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.components.SurfaceCard
import com.example.bankguard.ui.components.TonalButton
import com.example.bankguard.ui.theme.BankGuardShapes
import com.example.bankguard.ui.theme.Spacing

/** Bottom sheet opened from the top bar: shows the active persona, switches it, or resets the demo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoProfileSheet(
    profile: DemoProfile,
    bankMode: SimulatedBankMode,
    onSetBankMode: (SimulatedBankMode) -> Unit,
    onSwitch: (DemoPersona) -> Unit,
    onReset: () -> Unit,
    onOpenAbout: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val other = DemoPersona.entries.first { it != profile.persona }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = BankGuardShapes.sheet,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = stringResource(R.string.profile_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    IconBox(iconRes = profile.persona.iconRes, tone = StatusTone.Brand)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(profile.firstName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        StatusBadge(stringResource(profile.persona.modeRes), StatusTone.Info)
                    }
                }
                Text(
                    stringResource(R.string.profile_current, profile.firstName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (profile.persona == DemoPersona.Underbanked) {
                BankModeSwitch(
                    title = stringResource(R.string.profile_bank_title),
                    body = stringResource(R.string.profile_bank_body),
                    checked = bankMode == SimulatedBankMode.DeclineAll,
                    onChange = { on -> onSetBankMode(if (on) SimulatedBankMode.DeclineAll else SimulatedBankMode.ApproveEligible) },
                )
                BankModeSwitch(
                    title = stringResource(R.string.profile_outage_title),
                    body = stringResource(R.string.profile_outage_body),
                    checked = bankMode == SimulatedBankMode.Unavailable,
                    onChange = { on -> onSetBankMode(if (on) SimulatedBankMode.Unavailable else SimulatedBankMode.ApproveEligible) },
                )
            }
            TonalButton(
                text = stringResource(other.switchToRes),
                onClick = { onSwitch(other) },
                leadingIcon = other.iconRes,
            )
            SecondaryButton(
                text = stringResource(R.string.profile_reset),
                onClick = onReset,
                leadingIcon = R.drawable.ic_published_with_changes,
            )
            InfoNote(text = stringResource(R.string.profile_reset_body), tone = StatusTone.Demo, iconRes = R.drawable.ic_science)
            TonalButton(
                text = stringResource(R.string.about_open),
                onClick = onOpenAbout,
                leadingIcon = R.drawable.ic_info,
            )
        }
    }
}

/** One simulated-bank behaviour the presenter can switch on. The modes are exclusive. */
@Composable
private fun BankModeSwitch(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SurfaceCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}
