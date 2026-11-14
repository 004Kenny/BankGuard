package com.example.bankguard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.bankguard.R
import com.example.bankguard.ui.theme.BankGuardTheme
import com.example.bankguard.ui.theme.MoneyText
import com.example.bankguard.ui.theme.Spacing

// Preview-only gallery of the BankGuard component library. Every figure here is
// fictional sample data used to show the layout; none of it is shown in the app.

@Composable
private fun Gallery() {
    ScreenColumn {
        BankGuardTopBar(onProfileClick = {})
        HeroCard {
            HeroLabel("Simulated available")
            Text("$1,420.50", style = MoneyText.hero)
            HeroAccentBar(fraction = 0.7f)
            HeroInset {
                Text("Paycheck (sample)", style = MaterialTheme.typography.titleSmall)
                Text("Friday", style = MaterialTheme.typography.bodySmall, color = BankGuardTheme.colors.onHeroMuted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatusBadge("High priority", StatusTone.Critical)
            StatusBadge("Pending", StatusTone.Warning)
            StatusBadge("Approved", StatusTone.Success)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            DemoModeTag()
            ComingSoonBadge()
        }
        AlertCard(
            title = "Possible overdraft (sample)",
            body = "A scheduled bill may arrive before your next deposit.",
            badgeText = "High priority",
            tone = StatusTone.Critical,
            iconRes = R.drawable.ic_warning,
            meta = "Thu",
        ) {
            PrimaryButton("Review cash flow", onClick = {}, trailingIcon = R.drawable.ic_arrow_forward)
            TonalButton("Not now", onClick = {})
        }
        SurfaceCard {
            MoneyRow(R.drawable.ic_payments, "Paycheck (sample)", "Expected Fri", "+$1,650.00", amountTone = AmountTone.Positive, amountCaption = "Deposit")
            MoneyRow(R.drawable.ic_receipt_long, "Utility bill (sample)", "Due Thu", "-$185.00", amountTone = AmountTone.Negative, amountCaption = "Scheduled")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatTile(R.drawable.ic_warning, "2", "Alerts", Modifier.weight(1f), StatusTone.Warning)
            QuickActionTile(R.drawable.ic_insights, "View forecast", {}, Modifier.weight(1f))
        }
        SurfaceCard {
            SegmentedBar(listOf(BarSegment(0.45f, BankGuardTheme.colors.heroAccent), BarSegment(0.35f, MaterialTheme.colorScheme.secondary), BarSegment(0.2f, MaterialTheme.colorScheme.outlineVariant)))
            BudgetProgress("Food envelope (sample)", "$180 / $350", 0.51f)
            BudgetProgress("Prepaid card (sample)", "$45 left", 0.13f, tone = StatusTone.Critical)
        }
        SurfaceCard {
            StepTracker(
                listOf(
                    TrackerStep("Fee found", "Explained in plain language", StepState.Done),
                    TrackerStep("Letter drafted", "You reviewed and edited it", StepState.Current),
                    TrackerStep("Bank decision", "Your bank decides", StepState.Upcoming),
                ),
            )
        }
        SecondaryButton("Compare side by side", onClick = {})
        InfoNote("Your bank decides whether to refund a fee.", tone = StatusTone.Warning, iconRes = R.drawable.ic_warning)
        SimulatedDataBanner("Every figure on this screen is fictional.")
        EmptyState("No fees found", "Fees charged to the demo account will appear here.")
        LoadingState("Waiting for the simulated partner bank…")
        ErrorState("Request not sent", "Something went wrong. Nothing was changed.", retryLabel = "Try again", onRetry = {})
    }
}

@Preview(name = "Components, light", showBackground = true, heightDp = 2600)
@Composable
private fun GalleryLightPreview() {
    BankGuardTheme(darkTheme = false) { Gallery() }
}

@Preview(name = "Components, dark", showBackground = true, backgroundColor = 0xFF0B1622, heightDp = 2600)
@Composable
private fun GalleryDarkPreview() {
    BankGuardTheme(darkTheme = true) { Gallery() }
}
