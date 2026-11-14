package com.example.bankguard.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.bankguard.ui.theme.BankGuardTheme

/** Meaning of a badge, icon box, note or alert. Each tone maps to one color set. */
enum class StatusTone { Neutral, Brand, Info, Success, Warning, Critical, Demo }

@Immutable
data class ToneColors(val container: Color, val content: Color, val icon: Color)

@Composable
fun StatusTone.colors(): ToneColors {
    val scheme = MaterialTheme.colorScheme
    val bg = BankGuardTheme.colors
    return when (this) {
        StatusTone.Neutral -> ToneColors(scheme.surfaceContainerHigh, scheme.onSurfaceVariant, scheme.onSurfaceVariant)
        StatusTone.Brand -> ToneColors(scheme.primaryContainer, scheme.onPrimaryContainer, scheme.onPrimaryContainer)
        StatusTone.Info -> ToneColors(scheme.secondaryContainer, scheme.onSecondaryContainer, scheme.secondary)
        StatusTone.Success -> ToneColors(bg.successContainer, bg.success, bg.successIcon)
        StatusTone.Warning -> ToneColors(bg.warningContainer, bg.warning, bg.warningIcon)
        StatusTone.Critical -> ToneColors(bg.criticalContainer, bg.critical, bg.criticalIcon)
        StatusTone.Demo -> ToneColors(bg.demoContainer, bg.demo, bg.demo)
    }
}
