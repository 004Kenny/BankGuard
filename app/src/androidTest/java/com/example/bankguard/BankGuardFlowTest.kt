package com.example.bankguard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end checks through the real app (activity, ViewModel and screens).
 * Each test gets a fresh activity, so it starts at the welcome intro.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BankGuardFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun enterAs(personaTitle: String) {
        compose.onNodeWithText("Skip").performClick()
        compose.onNodeWithText(personaTitle).performClick()
        compose.onNodeWithText("Enter BankGuard demo").performScrollTo().performClick()
    }

    @Test
    fun introLeadsToPersonaSelectionThenHome() {
        compose.onNodeWithText("Welcome to BankGuard").assertIsDisplayed()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("See trouble coming").assertIsDisplayed()
        enterAs("I have a bank account")
        compose.onNodeWithText("Hi, Alex").assertIsDisplayed()
    }

    @Test
    fun bottomTabsOpenEachPillar() {
        enterAs("I have a bank account")
        compose.onNodeWithTag("tab_Prevent").performClick()
        compose.onNodeWithText("Lowest projected balance", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithTag("tab_Recover").performClick()
        compose.onNodeWithText("Fees found in your demo account", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithTag("tab_Discover").performClick()
        compose.onNodeWithText("Estimated yearly savings", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithTag("tab_Home").performClick()
        compose.onNodeWithText("Hi, Alex").assertIsDisplayed()
    }

    @Test
    fun cancellationIsSentOnlyAfterExplicitConfirmation() {
        enterAs("I have a bank account")
        compose.onNodeWithTag("tab_Prevent").performClick()
        compose.onNodeWithText("QuickShop Online (sample)").performScrollTo().performClick()

        // Backing out of the confirmation sends nothing.
        compose.onNodeWithText("Request cancellation").performScrollTo().performClick()
        compose.onNodeWithText("Send cancellation request?").assertIsDisplayed()
        compose.onNodeWithText("Not now").performClick()
        compose.onNodeWithText("Send cancellation request?").assertDoesNotExist()
        compose.onNodeWithText("If the bank approves").assertExists()

        // Confirming sends it to the simulated bank, which approves eligible charges by default.
        compose.onNodeWithText("Request cancellation").performScrollTo().performClick()
        compose.onNodeWithText("Send request").performClick()
        compose.waitUntilExactlyOneExists(hasText("Cancellation approved"), timeoutMillis = 10_000)
        compose.onNodeWithText("Reference SIM-", substring = true).assertExists()
    }

    @Test
    fun switchingPersonaShowsTheOtherDashboard() {
        enterAs("I have a bank account")
        compose.onNodeWithContentDescription("Demo profile").performClick()
        compose.onNodeWithText("Switch to the unbanked profile").performClick()
        compose.waitUntilExactlyOneExists(hasText("Hi, Jordan"), timeoutMillis = 5_000)
        compose.onNodeWithText("Estimated monthly fees", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun discoverDisclosesTheProposedReferralModel() {
        enterAs("I don't have a bank account")
        compose.onNodeWithTag("tab_Discover").performClick()
        compose.onNodeWithText("How BankGuard would make money").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("No partnerships exist in this demo", substring = true).assertExists()
    }
}
