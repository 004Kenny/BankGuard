---
name: Sentinel Trust
colors:
  surface: '#f9f9ff'
  surface-dim: '#cadaff'
  surface-bright: '#f9f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f1f3ff'
  surface-container: '#e8edff'
  surface-container-high: '#e0e8ff'
  surface-container-highest: '#d7e2ff'
  on-surface: '#041b3c'
  on-surface-variant: '#43474d'
  inverse-surface: '#1d3052'
  inverse-on-surface: '#edf0ff'
  outline: '#74777e'
  outline-variant: '#c3c6ce'
  surface-tint: '#49607c'
  primary: '#00152a'
  on-primary: '#ffffff'
  primary-container: '#102a43'
  on-primary-container: '#7a92b0'
  inverse-primary: '#b0c9e8'
  secondary: '#006874'
  on-secondary: '#ffffff'
  secondary-container: '#95f1ff'
  on-secondary-container: '#006f7b'
  tertiary: '#001815'
  on-tertiary: '#ffffff'
  tertiary-container: '#002f2b'
  on-tertiary-container: '#00a295'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d1e4ff'
  primary-fixed-dim: '#b0c9e8'
  on-primary-fixed: '#011d35'
  on-primary-fixed-variant: '#314863'
  secondary-fixed: '#95f1ff'
  secondary-fixed-dim: '#79d4e2'
  on-secondary-fixed: '#001f24'
  on-secondary-fixed-variant: '#004f57'
  tertiary-fixed: '#69f9e8'
  tertiary-fixed-dim: '#45dccc'
  on-tertiary-fixed: '#00201d'
  on-tertiary-fixed-variant: '#005049'
  background: '#f9f9ff'
  on-background: '#041b3c'
  surface-variant: '#d7e2ff'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 44px
    fontWeight: '700'
    lineHeight: 52px
    letterSpacing: -0.02em
  display-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.015em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 26px
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  title-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.02em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.03em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-tablet: 1.5rem
  margin: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system establishes an architectural, high-fidelity security standard for mission-critical mobile finance on Android. Grounded in the rigorous structural logic of Material 3 and built specifically for Jetpack Compose, the aesthetic communicates unyielding institutional stability alongside modern operational agility.

The visual language rejects superficial ornamentation in favor of precision, high legibility, and unmistakable state communication. Interfaces evoke calm assurance, deliberate authority, and immediate clarity during high-stress financial events, balance transfers, and fraud mitigations.

Key styling philosophies:
- **Structural Integrity:** Crisp container demarcations, calibrated elevation tiers, and intentional information grouping.
- **Uncompromised Usability:** Strict adherence to Android accessibility conventions, featuring persistent 48dp minimum interactive bounds and generous touch envelopes.
- **Radical Transparency:** Dedicated visual indicators for simulated data, demo modes, test credentials, and sandbox states to prevent operational confusion during reviews and user testing.

## Colors

The color palette centers on deep oceanic navies anchored by vivid institutional teals and aquas. Surface architecture relies on a crisp off-white foundation that eliminates eye strain while preserving stark contrast for numerical values, ledger entries, and transaction details.

Semantic role mappings:
- **Primary Navy (`#102A43`):** Key interactive focal points, authoritative top application bars, biometric authentication controls, and prominent primary buttons.
- **Secondary Teal (`#087F8C`):** Sub-navigation, functional icons, contextual action chips, and focused state borders.
- **Tertiary Aqua (`#22C7B8`):** High-priority micro-interactions, active progress tracks, dynamic verification glows, and verified account badges.
- **Neutral Primary Text (`#172B4D`):** Primary data points, currency figures, and structural typography.
- **Surface (`#FFFFFF`) & Canvas Background (`#F7FAFC`):** Strict division between elevated interactive cards and canvas backing.
- **Semantic Feedback:**
  - Success (`#16865D`): Ledger credits, cleared transfers, biometric confirmations.
  - Warning (`#E9A23B`): Transaction limits approached, expiring credentials, pending manual reviews.
  - Critical (`#D64550`): Immediate fraud blocks, session terminations, unauthorized device logins.
- **Transparency Indicators:**
  - Demo/Simulation Tag: Indigo tint (`#E0E8F9`) background with Navy (`#102A43`) text and a dashed accent border.

## Typography

The typographic hierarchy combines the warm authority of Plus Jakarta Sans for displays, headings, and monetary balance banners with the neutral, analytical clarity of Inter for dense tabular data, legal disclosures, and form inputs.

Implementation guidelines:
- **Monetary Values:** Numbers must use proportional or tabular figure variants when displayed in comparison tables or running ledgers to guarantee vertical decimal alignment.
- **Headings Scaling:** Apply `headline-lg-mobile` across viewport widths below 600dp to avoid aggressive word wrapping on long transaction titles or localized currency strings.
- **Contrast Safeguards:** Ensure all text styles maintain a minimum 4.5:1 WCAG AA contrast ratio against their respective surface containers, while labels under 14px enforce 7:1 against container backings.

## Layout & Spacing

Layouts adhere strictly to an 8dp baseline grid (with 4dp sub-increments for compact iconography and chip internal padding) designed to match Jetpack Compose layout mechanics.

Adaptive grid behavior:
- **Mobile Handset (<600dp):** 4-column fluid layout with 16dp margins and 16dp gutters. Interactive actions must anchor to the lower navigation bar or persistent bottom sheet zones for one-handed reachability.
- **Foldable & Tablet (600dp–839dp):** 8-column layout with 24dp margins and 24dp gutters. Forms and transaction details split into master-detail side-by-side sheets.
- **Expanded/Desktop (≥840dp):** 12-column fixed layout constrained to a maximum content width of 1040dp with 32dp margins.

Touch boundary enforcement:
- All clickable, checkable, and interactive controls must measure at least 48dp × 48dp in real touch area, irrespective of internal visual glyph dimensions. Use `Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)` across all Jetpack Compose targets.

## Elevation & Depth

Depth is established primarily through crisp surface-container color tinting combined with subtle, high-diffusion ambient shadows. This prevents interface glare and preserves precise color recognition in daylight outdoor conditions.

Elevation levels:
- **Level 0 (Flat Surface / Canvas):** Hex `#F7FAFC`. No shadow. Baseline for page scaffolding, ledger backdrops, and unselected background regions.
- **Level 1 (Card / Resting Surface):** Hex `#FFFFFF`. Ambient drop shadow: `0dp 2dp 8dp rgba(16, 42, 67, 0.05)`, accompanied by a subtle 1dp structural hairline border in `#E2E8F0`. Used for transaction tiles, security feed entries, and balance containers.
- **Level 2 (Interactive Floating Surfaces / Menus):** Hex `#FFFFFF`. Shadow: `0dp 4dp 16dp rgba(16, 42, 67, 0.08)`. Used for dropdown selection menus, quick-action chips, and card hover/press states.
- **Level 3 (Modal Bottom Sheets / Dialogs):** Hex `#FFFFFF`. Shadow: `0dp 8dp 24dp rgba(16, 42, 67, 0.14)`. Applied to transaction authentication modals, pin-entry sheets, and biometric review overlays. Accompanied by a 40% `#102A43` scrim backdrop.

## Shapes

The design system adopts a balanced structural geometry centered on a 12dp to 16dp corner radius curve, establishing clean, modern card definitions that retain institutional discipline without appearing playful or casual.

Standard corner configurations:
- **Small Controls (Chips, Badges, System Tags):** 8dp corner radius (`RoundedCornerShape(8.dp)`).
- **Interactive Buttons & Input Fields:** 12dp corner radius (`RoundedCornerShape(12.dp)`).
- **Cards, Ledger Groupings & Containers:** 16dp corner radius (`RoundedCornerShape(16.dp)`).
- **Modal Sheets:** Top-left and top-right radii set to 24dp (`RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)`).
- **Verification Pills & Simulation Badges:** Fully circular pill shapes for distinct perimeter recognition.

## Components

### Buttons
- **Primary:** Solid `#102A43` background, `#FFFFFF` text, 12dp radius, minimum height 52dp to surpass accessibility limits.
- **Secondary:** Outlined with 1.5dp `#087F8C` border, transparent surface, `#087F8C` text.
- **Tonal / Ghost:** Tinted `#F0F4F8` surface with `#102A43` text for auxiliary actions like statement downloads.
- **Danger:** Solid `#D64550` for destructive authorizations (e.g., account lock, card freezing).

### Status Badges & Chips
- **Semantic Badges:** Compact labels styled in 11px uppercase bold (`label-sm`).
  - Active/Cleared: `#16865D` text on `#E3F7EE` surface.
  - Review Required: `#E9A23B` text on `#FFF4E5` surface.
  - Fraud/Blocked: `#D64550` text on `#FFEBEA` surface.
- **Filter Chips:** 8dp radius, 36dp height with 48dp hit-box expansion, 1dp border `#E2E8F0`, shifting to solid `#087F8C` with white text when selected.

### Input Fields & Controls
- **Text Inputs:** Outlined Material style with 12dp radius, 56dp container height. Inactive border `#CBD5E1`; focused border 2dp `#087F8C` with floating label. Floating error messages rendered in `#D64550`.
- **Selection Controls (Checkboxes & Radios):** Minimum visual dimensions 24dp centered in 48dp padded containers. Checked state fills with `#102A43`.

### Cards & Ledger Tiles
- **Account Balance Card:** High-priority cards feature a subtle linear gradient from `#102A43` to `#1B3B5A`, contrasting white numerical copy with an embedded `#22C7B8` highlight bar.
- **Transaction Item:** 16dp rounded card or seamless list item with a 40dp rounded-square category icon holder, dual-line title/timestamp, and right-aligned bold amount.

### Simulation & Demo Transparency Tags
- Dedicated component for development, mock, and prototype modes. Must feature a striped or dashed 1dp boundary, an indigo pill tag reading `SIMULATED DATA` or `DEMO MODE`, and a prominent warning tint ensuring users never mistake test interactions for live bank movements.