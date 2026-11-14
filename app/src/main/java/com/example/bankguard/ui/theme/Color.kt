package com.example.bankguard.ui.theme

import androidx.compose.ui.graphics.Color

// BankGuard palette, consolidated from the Stitch round-2 design system
// (design-reference/.../design-system/BANKGUARD_DESIGN_DECISIONS.md).
// Where a Stitch value failed WCAG AA contrast for text, a darker text shade is used
// and the original is kept for icons and bars (noted inline).

// Brand
val Navy = Color(0xFF102A43)          // primary
val NavyLight = Color(0xFF1B3B5A)     // hero gradient end
val NavyDeep = Color(0xFF0A1F33)
val NavyTint = Color(0xFFD1E4FF)      // primary container
val NavyOnTint = Color(0xFF011D35)
val NavyMuted = Color(0xFFB0C9E8)     // secondary text on navy, inverse primary
val Teal = Color(0xFF087F8C)          // secondary
val TealTint = Color(0xFFD2F3F6)
val TealOnTint = Color(0xFF00474F)
val TealLight = Color(0xFF79D4E2)     // secondary in dark mode
val TealDark = Color(0xFF004F57)
val Aqua = Color(0xFF22C7B8)          // accent, on navy only (too light for text on white)
val Mint = Color(0xFF00695F)          // tertiary
val MintTint = Color(0xFFCFF5EF)
val MintOnTint = Color(0xFF00201D)
val MintLight = Color(0xFF45DCCC)

// Neutrals (light)
val Canvas = Color(0xFFF7FAFC)
val White = Color(0xFFFFFFFF)
val Ink = Color(0xFF172B4D)           // primary text
val InkMuted = Color(0xFF43474D)      // secondary text
val Border = Color(0xFFE2E8F0)        // card hairline
val OutlineGrey = Color(0xFF74777E)
val SurfaceLow = Color(0xFFF1F5F9)
val SurfaceHigh = Color(0xFFEDF2F7)
val SurfaceTonal = Color(0xFFF0F4F8)  // tonal button

// Neutrals (dark)
val NightCanvas = Color(0xFF0B1622)
val NightLowest = Color(0xFF08111B)
val NightLow = Color(0xFF111D2B)
val NightContainer = Color(0xFF152334)
val NightHigh = Color(0xFF1B2B3F)
val NightHighest = Color(0xFF23354B)
val NightText = Color(0xFFE3E9F1)
val NightTextMuted = Color(0xFFB8C3D1)
val NightOutline = Color(0xFF8792A2)
val NightBorder = Color(0xFF2C3B4F)

// Semantic: success
val SuccessText = Color(0xFF0F6E4C)       // Stitch #16865D fails AA on its container
val SuccessIcon = Color(0xFF16865D)
val SuccessContainer = Color(0xFFE3F7EE)
val NightSuccessText = Color(0xFF7FDDB0)
val NightSuccessContainer = Color(0xFF0F3B2A)

// Semantic: warning
val WarningText = Color(0xFF8A5A00)       // Stitch #E9A23B fails AA as text
val WarningIcon = Color(0xFFE9A23B)
val WarningContainer = Color(0xFFFFF4E5)
val NightWarningText = Color(0xFFF5C77A)
val NightWarningContainer = Color(0xFF3D2E0F)

// Semantic: critical
val CriticalText = Color(0xFFB3261E)      // Stitch #D64550 fails AA for small text
val CriticalIcon = Color(0xFFD64550)
val CriticalContainer = Color(0xFFFFEBEA)
val NightCriticalText = Color(0xFFFFB4AB)
val NightCriticalContainer = Color(0xFF4A1C1E)
val NightOnError = Color(0xFF690005)
val NightErrorContainer = Color(0xFF93000A)
val NightOnErrorContainer = Color(0xFFFFDAD6)
val OnCriticalContainer = Color(0xFF8C1D18)

// Demo / simulation transparency tag
val DemoContainer = Color(0xFFE0E8F9)
val NightDemoContainer = Color(0xFF1F2D45)
