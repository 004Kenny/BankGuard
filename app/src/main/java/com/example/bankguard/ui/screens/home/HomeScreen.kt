package com.example.bankguard.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.bankguard.R
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.ui.BankGuardTab
import com.example.bankguard.ui.components.StatusBadge
import com.example.bankguard.ui.components.StatusTone
import com.example.bankguard.ui.theme.Spacing

/** Home tab: shows the dashboard for the active demo persona. */
@Composable
fun HomeScreen(
    profile: DemoProfile,
    onOpenTab: (BankGuardTab) -> Unit,
    onOpenForecast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (profile.persona) {
        DemoPersona.Underbanked -> UnderbankedHome(profile, onOpenTab, onOpenForecast, modifier)
        DemoPersona.Unbanked -> UnbankedHome(profile, onOpenTab, modifier)
    }
}

/** "Hi, Alex" with a one-line subtitle and a context chip, at the top of both dashboards. */
@Composable
internal fun HomeGreeting(
    firstName: String,
    subtitle: String,
    chip: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = stringResource(R.string.home_greeting, firstName),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        StatusBadge(text = chip, tone = StatusTone.Brand)
    }
}
