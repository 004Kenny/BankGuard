# BankGuard merged design decisions

These are **implementation decisions**, not claims about the source HTML. Both original Stitch DESIGN.md files are preserved for comparison.

- Use round-2 visual system as the main starting point: navy `#102A43`, teal `#087F8C`, white/light surfaces, Plus Jakarta Sans for headings and Inter for body where feasible.
- Round-1 cards, charts, and visual patterns are references only, not authority for functionality.
- Native implementation: Kotlin, Jetpack Compose, Material 3; do not paste HTML into a WebView.
- Four tabs: Home, Prevent, Recover, Discover.
- Underbanked and unbanked Home are separate states, selected by demo persona.
- Never present unverified bank actions or financial outcomes as real. No autonomous fund transfers, guaranteed fee reversals, or automatic subscription cancellation.
- Cancellation: eligible pending transactions only, explicit user request, simulated bank approval/rejection, update forecast after confirmed approval only.
- Recover: fee explanation and user-reviewed editable AI waiver letter; bank makes refund decision.
- Discover: affordable bank account comparison and estimated savings, not debt consolidation or APR optimization.
- Remove unsupported ratings, statistics, and savings claims. Mark sample data fictional.
- Remaining screens are not included in this package. Do not infer they have been designed.
