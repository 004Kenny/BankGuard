package com.example.bankguard.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankguard.R
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.ui.components.BankGuardLogo
import com.example.bankguard.ui.components.DemoModeTag
import com.example.bankguard.ui.components.InfoNote
import com.example.bankguard.ui.components.PrimaryButton
import com.example.bankguard.ui.components.SelectableOptionCard
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.Sizes
import com.example.bankguard.ui.theme.Spacing

/**
 * First screen of the demo: pick the underbanked or unbanked persona.
 * Based on the Stitch "demo profile selection" design, with unsupported claims removed.
 */
@Composable
fun PersonaSelectionScreen(
    onEnter: (DemoPersona) -> Unit,
    modifier: Modifier = Modifier,
    initialSelection: DemoPersona = DemoPersona.Underbanked,
) {
    var selected by rememberSaveable { mutableStateOf(initialSelection) }

    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DemoModeTag(modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(Spacing.xl))

            BankGuardLogo(size = 72.dp)
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            val pillarsCd = stringResource(R.string.onboarding_pillars_cd)
            Text(
                text = stringResource(R.string.onboarding_pillars),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(top = Spacing.xs)
                    .semantics { contentDescription = pillarsCd },
            )
            Text(
                text = stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .padding(top = Spacing.md),
            )

            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = stringResource(R.string.onboarding_choose),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
            )
            Spacer(Modifier.height(Spacing.md))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                DemoPersona.entries.forEach { persona ->
                    SelectableOptionCard(
                        iconRes = persona.iconRes,
                        title = stringResource(persona.titleRes),
                        badge = stringResource(persona.modeRes),
                        description = stringResource(persona.descriptionRes),
                        selected = persona == selected,
                        onSelect = { selected = persona },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))
            InfoNote(text = stringResource(selected.preloadedRes), tone = StatusTone.Info)
            Spacer(Modifier.height(Spacing.sm))
            InfoNote(
                text = stringResource(R.string.onboarding_notice),
                tone = StatusTone.Demo,
                iconRes = R.drawable.ic_science,
            )

            Spacer(Modifier.height(Spacing.xl))
            PrimaryButton(
                text = stringResource(R.string.onboarding_enter),
                onClick = { onEnter(selected) },
                trailingIcon = R.drawable.ic_arrow_forward,
            )
            Spacer(Modifier.height(Spacing.md))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                FooterHint(R.drawable.ic_lock, stringResource(R.string.onboarding_no_credentials))
                FooterHint(R.drawable.ic_published_with_changes, stringResource(R.string.onboarding_switch_anytime))
            }
        }
    }
}

@Composable
private fun FooterHint(iconRes: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Sizes.iconSmall - 2.dp),
        )
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun PersonaSelectionPreview() {
    BankGuardTheme { PersonaSelectionScreen(onEnter = {}) }
}
