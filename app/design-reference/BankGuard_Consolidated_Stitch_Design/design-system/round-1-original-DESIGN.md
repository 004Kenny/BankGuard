---
name: Sentinel Trust
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#45464d'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#76777d'
  outline-variant: '#c6c6cd'
  surface-tint: '#565e74'
  primary: '#000000'
  on-primary: '#ffffff'
  primary-container: '#131b2e'
  on-primary-container: '#7c839b'
  inverse-primary: '#bec6e0'
  secondary: '#006a61'
  on-secondary: '#ffffff'
  secondary-container: '#86f2e4'
  on-secondary-container: '#006f66'
  tertiary: '#000000'
  on-tertiary: '#ffffff'
  tertiary-container: '#2a1700'
  on-tertiary-container: '#b87500'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dae2fd'
  primary-fixed-dim: '#bec6e0'
  on-primary-fixed: '#131b2e'
  on-primary-fixed-variant: '#3f465c'
  secondary-fixed: '#89f5e7'
  secondary-fixed-dim: '#6bd8cb'
  on-secondary-fixed: '#00201d'
  on-secondary-fixed-variant: '#005049'
  tertiary-fixed: '#ffddb8'
  tertiary-fixed-dim: '#ffb95f'
  on-tertiary-fixed: '#2a1700'
  on-tertiary-fixed-variant: '#653e00'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.015em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  title-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 10px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-sm: 0.75rem
  margin: 1rem
  margin-tablet: 1.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style
The design system reflects proactive protection, institutional strength, and reassuring clarity. Designed primarily for modern mobile banking experiences on Android, the aesthetic bridges enterprise-level cybersecurity with approachable everyday consumer finance. 

The visual language follows **Tactile Minimalism with Frosted Depth**: clean layouts, expansive whitespace, intentional surface elevation, and precise feedback states. Interfaces avoid cold intimidation through humanized micro-copy, tactile feedback metaphors, soft rounded shapes, and clear status communication. Visual clutter is stripped away so users immediately understand their risk posture, security shield health, and daily balance movements.

## Colors
The palette leverages high-contrast deep slate neutrals anchored against vivid, reassuring protective accents:

- **Primary (`#0F172A`, `#1E293B`)**: The deep navy ground communicates regulatory permanence, security architecture, and steadfast reliability. Applied to high-emphasis headers, active primary action surfaces, and fundamental navigation chrome.
- **Secondary (`#0D9488`, `#14B8A6`, `#10B981`)**: Protective teal and electric mint denote proactive shielding, validated security locks, approved transactions, and positive cash flow. 
- **Tertiary (`#F59E0B`, `#EF4444`)**: Alert amber and crisp coral are reserved strictly for preemptive threat defense, anomaly detection, real-time fee warnings, and critical authorization confirmations.
- **Surfaces & Neutrals (`#FFFFFF`, `#F8FAFC`, `#F1F5F9`, `#E2E8F0`, `#64748B`)**: Delicately stepped tonal layers maintain strict optical hierarchy without reliance on harsh divider lines.

## Typography
The pairing combines **Plus Jakarta Sans** for headlines and structural body elements with **Inter** for data tables, metric numerals, security labels, and interactive tokens.

- **Plus Jakarta Sans** provides human warmth with slightly rounded geometry, removing austerity from complex financial concepts.
- **Inter** ensures absolute legibility for financial figures, fractional cents, currency markers, timestamps, and compact status pills.
- Numerical values (balances, transactions, two-factor codes) must always utilize tabular figures (`tnum`) to maintain structural vertical alignment across dynamic lists and cards.

## Layout & Spacing
The layout adheres to a strict 4-column fluid mobile grid (expanding to 8 columns on tablets and foldables) built on an 8px base rhythm (with 4px micro-increments for compact badges and icon aligners).

- **Screen Margins**: Set to `1rem` (16px) on compact handhelds, extending to `1.5rem` (24px) on large screens and foldables.
- **Touch Targets**: All primary interactive elements maintain a minimum vertical hit footprint of 48dp, respecting Android accessibility standards.
- **Form Factors**: On foldable and split-screen Android configurations, the balance card and active security alert banner dock into a persistent left pane while transaction feeds expand across the secondary viewport.

## Elevation & Depth
Depth is constructed through **tonal surface layering paired with ultra-diffuse ambient shadows**, avoiding heavy skeuomorphism in favor of soft, tangible clarity.

- **Level 0 (Canvas Ground)**: Deep tint surface `#F8FAFC`. Completely flat.
- **Level 1 (Cards, List Groups)**: Pure white `#FFFFFF` resting on `#F8FAFC` with a hairline stroke (`#F1F5F9`) and an ambient shadow (`box-shadow: 0 4px 20px -2px rgba(15, 23, 42, 0.05)`).
- **Level 2 (Active Security Overlays, Sticky Heads)**: Elevated cards with `box-shadow: 0 10px 25px -4px rgba(15, 23, 42, 0.08), 0 4px 6px -2px rgba(15, 23, 42, 0.04)`.
- **Level 3 (Modals, Biometric Prompts, Bottom Sheets)**: Supported by a 40% opacity slate scrim (`#0F172A` at 40% opacity) with a directional top-surface shadow (`0 -8px 30px rgba(15, 23, 42, 0.12)`).

## Shapes
A consistent medium-rounded geometry (`roundedness: 2`) balances modern friendliness with institutional stability.

- **Base Radius (0.5rem / 8px)**: Applied to text fields, action buttons, alert banners, and table rows.
- **Large Radius (1rem / 16px)**: Applied to metric balance cards, transaction summary panels, and modal containers.
- **Pill Radius (Full / 9999px)**: Applied strictly to chip filters, security level tags, biometric verification triggers, and quick-action icons.

## Components

### Buttons
- **Primary Action**: Solid `#0F172A` with `#FFFFFF` text, 48dp height, 8px border radius, subtle scale depression (`scale(0.98)`) on press.
- **Protective Action (Shield/Resolve)**: Solid `#0D9488` with `#FFFFFF` text for confirming security patches, enabling card freezes, or approving trusted transfers.
- **Ghost/Secondary**: `#F1F5F9` background with `#1E293B` text, shifting to `#E2E8F0` on tactile feedback.

### Status Badges & Chips
- **Pill Badges**: Pill-shaped (`rounded-full`), padded 4px vertical by 10px horizontal.
  - *Shield Active / Safe*: Light mint background (`#CCFBF1`), deep teal text (`#0F766E`), leading 6px pulsing dot.
  - *Risk / Suspicious Alert*: Light coral background (`#FEE2E2`), deep red text (`#B91C1C`).
  - *Pending / Audit*: Soft amber background (`#FEF3C7`), dark amber text (`#B45309`).

### Input Fields & Controls
- **Text Inputs**: Crisp `#FFFFFF` surface, 1px border (`#E2E8F0`), 8px border radius. On focus: 2px border using `#0D9488` with no harsh outline glow.
- **Checkboxes & Radios**: 20px controls with `#0D9488` active fill and animated micro-check verification.
- **Security Switches (Toggles)**: 28px height, 48px width pill slider; deep slate track turns protective teal on activation.

### Cards & Metric Displays
- **Shield Health Card**: Primary dashboard anchor. Distinct deep slate background (`#0F172A`) with high-contrast mint telemetry readouts (`#14B8A6`) and real-time defense indicators.
- **Transaction Item**: Borderless white card with leading circular merchant icon container (40px, `#F8FAFC`), vertical headline/timestamp stack, and trailing monospace tabular amount text.
- **Proactive Threat Banner**: Floating warning card docked above lower navigation when a potential fraudulent charge or hidden fee is detected, styled with `#FFFBEB` background and `#D97706` left accent rail.