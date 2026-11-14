package com.example.bankguard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bankguard.data.demo.DemoAccountOptions
import com.example.bankguard.model.AccountOption
import org.junit.Assert.assertEquals
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
        compose.onNodeWithText("Recover your money").assertIsDisplayed()
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

    @Test
    fun recoveryNeedsFeeConsentAndEndsAtNinetyFive() {
        enterAs("I have a bank account")
        compose.onNodeWithTag("tab_Recover").performClick()
        compose.onNodeWithText("Load the $150 overdraft demo").performScrollTo().performClick()
        compose.onNodeWithText("Overdrawn by $50.00").assertIsDisplayed()
        compose.onNodeWithText("Check recovery").performScrollTo().performClick()

        // The simulated bank answers the eligibility check, then the exact fee is shown.
        compose.waitUntilExactlyOneExists(hasText("Request recovery"), timeoutMillis = 10_000)
        compose.onNodeWithText("Estimated final balance").assertExists()
        compose.onNodeWithText("Request recovery").performScrollTo().performClick()

        // Nothing can be sent until the user agrees to the fee.
        compose.onNodeWithText("Confirm Recovery Request").assertIsDisplayed()
        compose.onNodeWithText("Confirm Recovery").assertIsNotEnabled()
        compose.onNodeWithTag("fee_consent").performClick()
        compose.onNodeWithText("Confirm Recovery").assertIsEnabled().performClick()

        compose.waitUntilExactlyOneExists(hasText("Recovery Confirmed"), timeoutMillis = 10_000)
        compose.onNodeWithText("Final account balance").performScrollTo().assertIsDisplayed()

        // Home reads the same state.
        compose.onNodeWithTag("tab_Home").performClick()
        compose.onNodeWithText("$95.00").assertIsDisplayed()
    }

    @Test
    fun everyDiscoverAccountOpensItsOwnBankAndReturnsToDiscover() {
        enterAs("I have a bank account")
        assertEveryAccountOpens()
    }

    @Test
    fun unbankedPersonaCanAlsoOpenEveryAccount() {
        enterAs("I don't have a bank account")
        assertEveryAccountOpens()
    }

    @Test
    fun completingTheDemoDayReachesSevenAndUnlocksMoneyGuardian() {
        enterAs("I have a bank account")
        compose.onNodeWithTag("streak_card").performScrollTo()
        compose.onNodeWithContentDescription("Current streak: 6 days").assertExists()
        compose.onNodeWithContentDescription("Total stars: 6 stars").assertExists()
        compose.onNodeWithText("View achievements").performScrollTo().performClick()

        compose.onNodeWithText("Achievements").assertIsDisplayed()
        compose.onNodeWithContentDescription("Getting Started, 3-day streak. Unlocked").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Money Guardian, 7-day streak. Locked").assertExists()
        compose.onNodeWithContentDescription("Today: In progress").assertExists()

        compose.onNodeWithText("Complete demo day").performScrollTo().performClick()
        compose.onNodeWithText("Achievement unlocked").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Money Guardian, 7-day streak. Unlocked").assertExists()
        compose.onNodeWithTag("streak_hero_days").assertTextEquals("7 days")

        // Home reads the same state.
        compose.onNodeWithContentDescription("Back").performScrollTo().performClick()
        compose.onNodeWithTag("streak_card").performScrollTo()
        compose.onNodeWithContentDescription("Current streak: 7 days").assertExists()
        compose.onNodeWithContentDescription("Total stars: 7 stars").assertExists()
    }

    @Test
    fun unbankedHomeHasNoStreak() {
        enterAs("I don't have a bank account")
        compose.onNodeWithTag("streak_card").assertDoesNotExist()
        compose.onNodeWithText("View achievements").assertDoesNotExist()
    }

    /** Opens each Discover account in turn and walks the whole referral flow for it. */
    private fun assertEveryAccountOpens() {
        compose.onNodeWithTag("tab_Discover").performClick()
        val compare = "Compare with what you pay now"
        val count = compose.onAllNodesWithText(compare).fetchSemanticsNodes().size
        assertEquals(DemoAccountOptions.all.size, count)

        val opened = mutableSetOf<String>()
        repeat(count) { i ->
            compose.onAllNodesWithText(compare)[i].performScrollTo().performClick()
            val option = shownOption()
            // Existing comparison still works.
            compose.onNodeWithText("Estimated yearly cost", ignoreCase = true).performScrollTo().assertExists()

            compose.onNodeWithText("Open account").performScrollTo().performClick()
            compose.onNodeWithText("Demo account opening").assertIsDisplayed()
            compose.onNodeWithText("Open your account with ${option.institution}").assertIsDisplayed()
            compose.onNodeWithText("${option.institution}, referred by BankGuard", substring = true).assertIsDisplayed()
            compose.onNodeWithText(option.features.first()).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("REF-BG-", substring = true).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Started").performScrollTo().assertIsDisplayed()

            compose.onNodeWithText("Continue to bank").performScrollTo().performClick()
            compose.onNodeWithText("You're ready to continue").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("redirected to ${option.institution}", substring = true).assertExists()
            compose.onNodeWithText("Continued to bank").assertExists()

            compose.onNodeWithText("Back to Discover").performScrollTo().performClick()
            // Back on Discover's main screen (it keeps its scroll position).
            compose.onNodeWithText("Demo account opening").assertDoesNotExist()
            compose.onNodeWithText("Estimated yearly savings", ignoreCase = true).performScrollTo().assertIsDisplayed()
            opened += option.id
        }
        assertEquals(DemoAccountOptions.all.map { it.id }.toSet(), opened)
    }

    /** The one Discover account whose name is on screen (the comparison shows a single account). */
    private fun shownOption(): AccountOption = DemoAccountOptions.all.single { option ->
        compose.onAllNodesWithText(option.name).fetchSemanticsNodes().isNotEmpty()
    }
}
