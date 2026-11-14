# BankGuard — consolidated Stitch design references

## What this is
A **curated merger of seven existing Stitch screen exports**, preserving each screen's original `code.html` and `screen.png` plus both original design-system files. It is a **reference package, not a running Android app and not a fully redesigned set of screens**.

## Source mapping
| Path | Source | Intended use |
|---|---|---|
| onboarding/demo-profile-selection | Round 2 | Persona selection (copy requires corrections) |
| home/underbanked | Round 2 | Underbanked Home |
| home/unbanked | Round 2 | Unbanked Home |
| prevent/risk-alerts | Round 1 | Visual reference for Prevent; remove unsupported actions |
| recover/fee-recovery | Round 1 | Visual reference for Recover; remove unsupported outcomes |
| discover/better-options | Round 1 | Visual reference for Discover; replace debt-centric content |
| references/original-overview | Round 1 | Optional cards/chart styling reference, **not an extra tab** |

## Critical warnings
- The seven exports were **not visually re-rendered or reconciled**. They may contain conflicting design tokens and unapproved financial claims.
- The `code.html` files are generated HTML references, not Compose code.
- **Do not implement every button or claim literally**. Use `design-system/BANKGUARD_DESIGN_DECISIONS.md` for approved scope.
- Missing: splash, welcome, feature-introduction, transactions, forecasting, budgeting, cancellation details/results, waiver-letter flow, banking comparisons/calculator, settings and shared states.
- Maintain the working Android app's Gradle and navigation configuration; implement incrementally, compile and test after each stage.

## Suggested Claude Code handoff
1. Extract this folder inside the Android project at `design-reference/BankGuard_Consolidated_Stitch_Design/` (or provide its absolute path).
2. Ask Claude to inspect README, both original DESIGN.md files, design decisions, PNGs, and HTML.
3. Request a mapping of references to existing Compose components **before** making edits.
4. Implement only the first approved UI batch and confirm it builds before proceeding.
