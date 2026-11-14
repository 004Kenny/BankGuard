package com.example.bankguard.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Status and brand colors that Material 3's ColorScheme has no slot for. */
@Immutable
data class BankGuardColors(
    val success: Color,
    val successIcon: Color,
    val successContainer: Color,
    val warning: Color,
    val warningIcon: Color,
    val warningContainer: Color,
    val critical: Color,
    val criticalIcon: Color,
    val criticalContainer: Color,
    val demo: Color,
    val demoContainer: Color,
    val demoBorder: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val heroAccent: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    /** Negative figures on a navy hero card. */
    val onHeroCritical: Color,
    val heroInset: Color,
    val cardBorder: Color,
)

val LightBankGuardColors = BankGuardColors(
    success = SuccessText,
    successIcon = SuccessIcon,
    successContainer = SuccessContainer,
    warning = WarningText,
    warningIcon = WarningIcon,
    warningContainer = WarningContainer,
    critical = CriticalText,
    criticalIcon = CriticalIcon,
    criticalContainer = CriticalContainer,
    demo = Navy,
    demoContainer = DemoContainer,
    demoBorder = Navy,
    heroStart = Navy,
    heroEnd = NavyLight,
    heroAccent = Aqua,
    onHero = White,
    onHeroMuted = NavyMuted,
    onHeroCritical = NightCriticalText,
    heroInset = White.copy(alpha = 0.08f),
    cardBorder = Border,
)

val DarkBankGuardColors = BankGuardColors(
    success = NightSuccessText,
    successIcon = NightSuccessText,
    successContainer = NightSuccessContainer,
    warning = NightWarningText,
    warningIcon = WarningIcon,
    warningContainer = NightWarningContainer,
    critical = NightCriticalText,
    criticalIcon = NightCriticalText,
    criticalContainer = NightCriticalContainer,
    demo = NavyTint,
    demoContainer = NightDemoContainer,
    demoBorder = NavyMuted,
    heroStart = NavyLight,
    heroEnd = Navy,
    heroAccent = Aqua,
    onHero = White,
    onHeroMuted = NavyMuted,
    onHeroCritical = NightCriticalText,
    heroInset = White.copy(alpha = 0.08f),
    cardBorder = NightBorder,
)

val LocalBankGuardColors = staticCompositionLocalOf { LightBankGuardColors }
