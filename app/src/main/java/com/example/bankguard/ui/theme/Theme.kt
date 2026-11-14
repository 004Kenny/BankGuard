package com.example.bankguard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Navy,
    onPrimary = White,
    primaryContainer = NavyTint,
    onPrimaryContainer = NavyOnTint,
    inversePrimary = NavyMuted,
    secondary = Teal,
    onSecondary = White,
    secondaryContainer = TealTint,
    onSecondaryContainer = TealOnTint,
    tertiary = Mint,
    onTertiary = White,
    tertiaryContainer = MintTint,
    onTertiaryContainer = MintOnTint,
    background = Canvas,
    onBackground = Ink,
    surface = Canvas,
    onSurface = Ink,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = InkMuted,
    surfaceTint = Navy,
    surfaceContainerLowest = White,
    surfaceContainerLow = SurfaceLow,
    surfaceContainer = White,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = Border,
    outline = OutlineGrey,
    outlineVariant = Border,
    error = CriticalText,
    onError = White,
    errorContainer = CriticalContainer,
    onErrorContainer = OnCriticalContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = NavyMuted,
    onPrimary = NavyDeep,
    primaryContainer = NavyLight,
    onPrimaryContainer = NavyTint,
    inversePrimary = Navy,
    secondary = TealLight,
    onSecondary = TealOnTint,
    secondaryContainer = TealDark,
    onSecondaryContainer = TealTint,
    tertiary = MintLight,
    onTertiary = MintOnTint,
    tertiaryContainer = TealDark,
    onTertiaryContainer = MintTint,
    background = NightCanvas,
    onBackground = NightText,
    surface = NightCanvas,
    onSurface = NightText,
    surfaceVariant = NightHigh,
    onSurfaceVariant = NightTextMuted,
    surfaceTint = NavyMuted,
    surfaceContainerLowest = NightLowest,
    surfaceContainerLow = NightLow,
    surfaceContainer = NightContainer,
    surfaceContainerHigh = NightHigh,
    surfaceContainerHighest = NightHighest,
    outline = NightOutline,
    outlineVariant = NightBorder,
    error = NightCriticalText,
    onError = NightOnError,
    errorContainer = NightErrorContainer,
    onErrorContainer = NightOnErrorContainer,
)

@Composable
fun BankGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so BankGuard keeps its own navy/teal identity.
    // Set to true to follow the user's wallpaper colors on Android 12+.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val bankGuardColors = if (darkTheme) DarkBankGuardColors else LightBankGuardColors

    CompositionLocalProvider(LocalBankGuardColors provides bankGuardColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

/** Access BankGuard's extra colors: `BankGuardTheme.colors.success`, etc. */
object BankGuardTheme {
    val colors: BankGuardColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBankGuardColors.current
}
