package com.example.bankguard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bankguard.data.demo.AccountScenario
import com.example.bankguard.data.demo.DemoAccountOptions
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.data.simulated.SimulatedBankMode
import com.example.bankguard.domain.recovery.OverdraftDetector
import com.example.bankguard.model.CancellationStatus
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.FeeRecovery
import com.example.bankguard.model.SavingsInputs
import com.example.bankguard.model.WaiverReason
import com.example.bankguard.ui.components.BankGuardTopBar
import com.example.bankguard.ui.navigation.DiscoverRoute
import com.example.bankguard.ui.navigation.HomeRoute
import com.example.bankguard.ui.screens.streaks.StreakScreen
import com.example.bankguard.domain.streaks.StreakSummary
import com.example.bankguard.ui.navigation.PreventRoute
import com.example.bankguard.ui.navigation.RecoverRoute
import com.example.bankguard.ui.navigation.TabBackStack
import com.example.bankguard.ui.navigation.rememberTabBackStack
import com.example.bankguard.ui.screens.discover.AccountComparisonScreen
import com.example.bankguard.ui.screens.discover.AccountOpeningScreen
import com.example.bankguard.model.ReferralRecord
import com.example.bankguard.ui.screens.discover.DiscoverScreen
import com.example.bankguard.ui.screens.discover.SavingsCalculatorScreen
import com.example.bankguard.ui.screens.home.HomeScreen
import com.example.bankguard.ui.screens.onboarding.PersonaSelectionScreen
import com.example.bankguard.ui.screens.onboarding.WelcomeScreen
import com.example.bankguard.ui.screens.prevent.CancellationScreen
import com.example.bankguard.ui.screens.prevent.ForecastDetailScreen
import com.example.bankguard.ui.screens.prevent.PreventScreen
import com.example.bankguard.ui.screens.recover.FeeDetailScreen
import com.example.bankguard.ui.screens.recover.RecoverScreen
import com.example.bankguard.ui.screens.recover.RecoveryDetailScreen
import com.example.bankguard.ui.screens.recover.RecoveryUiState
import com.example.bankguard.ui.screens.recover.WaiverLetterScreen
import com.example.bankguard.ui.screens.settings.AboutScreen
import com.example.bankguard.ui.screens.settings.DemoProfileSheet
import com.example.bankguard.ui.theme.BankGuardTheme

/**
 * BankGuard's root. Shows the welcome intro, then persona selection until a demo persona is
 * chosen, then the tabbed app.
 * Switching or resetting the persona rebuilds the tabbed app, so it always reopens on Home.
 */
@Composable
fun BankGuardApp(
    modifier: Modifier = Modifier,
    session: DemoSessionViewModel = viewModel(),
) {
    val persona by session.persona.collectAsState()
    val introSeen by session.introSeen.collectAsState()
    val profile by session.profile.collectAsState()
    val cancellations by session.cancellations.collectAsState()
    val bankMode by session.bankMode.collectAsState()
    val recoveries by session.recoveries.collectAsState()
    val savingsInputs by session.savingsInputs.collectAsState()
    val scenario by session.scenario.collectAsState()
    val recoveryRecords by session.recoveryRecords.collectAsState()
    val eligibility by session.eligibility.collectAsState()
    val statusChecks by session.statusChecks.collectAsState()
    val referrals by session.referrals.collectAsState()
    val streak by session.streak.collectAsState()
    val lastCompletedDay by session.lastCompletedDay.collectAsState()

    // Welcome intro -> persona selection -> tabbed app. Keyed on persona and intro state.
    Crossfade(targetState = persona to introSeen, label = "onboarding", modifier = modifier) { (active, seen) ->
        if (active == null && !seen) {
            WelcomeScreen(onFinish = session::finishIntro)
        } else if (active == null) {
            PersonaSelectionScreen(onEnter = session::selectPersona)
        } else {
            BankGuardMain(
                profile = profile?.takeIf { it.persona == active } ?: DemoProfiles.forPersona(active),
                cancellations = cancellations,
                bankMode = bankMode,
                onRequestCancellation = session::requestCancellation,
                onSetBankMode = session::setBankMode,
                recoveries = recoveries,
                recovery = RecoveryUiState(
                    // Only this scenario's requests; other scenarios keep theirs for when they're loaded again.
                    records = recoveryRecords.filterValues { it.scenarioKey == scenario.key },
                    eligibility = eligibility,
                    statusChecks = statusChecks,
                    scenario = scenario,
                ),
                savingsInputs = savingsInputs[active] ?: DemoAccountOptions.defaultInputs(active),
                onSavingsInputsChange = { inputs -> session.setSavingsInputs(active, inputs) },
                onResetSavingsInputs = { session.resetSavingsInputs(active) },
                referrals = referrals,
                streak = StreakUiState(streak, lastCompletedDay, session::completeDemoDay, session::dismissCompletedDay),
                onStartReferral = session::startReferral,
                onContinueReferral = session::continueReferral,
                recoverActions = RecoverActions(
                    onSaveLetter = session::saveLetter,
                    onMarkSent = session::markSent,
                    onRecordDecision = session::recordDecision,
                    onCheckEligibility = session::checkRecoveryEligibility,
                    onRequestRecovery = session::requestRecovery,
                    onRefreshStatus = session::refreshRecoveryStatus,
                    onSetScenario = session::setScenario,
                ),
                onSwitchPersona = session::selectPersona,
                onResetDemo = session::resetDemo,
            )
        }
    }
}

/**
 * The tabbed app: a Scaffold with a persistent bottom navigation bar and the selected tab's screen.
 *
 * Navigation is plain state, not Navigation Compose: a selected tab plus a small back stack per tab
 * ([TabBackStack]). Only the visible screen is composed, so there is never more than one copy of it. The selected tab survives rotation and process death via
 * [rememberSaveable], and each screen's own saved state (such as scroll position) is kept per tab by
 * a [rememberSaveableStateHolder], so switching tabs and coming back returns you to where you were.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BankGuardMain(
    profile: DemoProfile,
    cancellations: Map<String, CancellationStatus>,
    bankMode: SimulatedBankMode,
    onRequestCancellation: (String) -> Unit,
    onSetBankMode: (SimulatedBankMode) -> Unit,
    recoveries: Map<String, FeeRecovery>,
    recovery: RecoveryUiState,
    recoverActions: RecoverActions,
    savingsInputs: SavingsInputs,
    onSavingsInputsChange: (SavingsInputs) -> Unit,
    onResetSavingsInputs: () -> Unit,
    referrals: Map<String, ReferralRecord>,
    streak: StreakUiState,
    onStartReferral: (accountId: String) -> String?,
    onContinueReferral: (referralId: String) -> Unit,
    onSwitchPersona: (DemoPersona) -> Unit,
    onResetDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(BankGuardTab.Home) }
    var showProfileSheet by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val screenStateHolder = rememberSaveableStateHolder()
    // One back stack per tab, so each tab remembers the screen you left it on.
    val stacks = BankGuardTab.entries.associateWith { rememberTabBackStack() }
    val stack = stacks.getValue(selectedTab)

    // System Back closes the keyboard if it's open, then the open sub-screen; on a tab's main
    // screen it leaves the app as usual. (On recent Android versions, seen on Android 15, the app
    // can receive the same Back press that closes the keyboard, so it's checked here.)
    val imeVisible = WindowInsets.isImeVisible
    val focusManager = LocalFocusManager.current
    BackHandler(enabled = imeVisible || showAbout || !stack.isEmpty) {
        when {
            imeVisible -> focusManager.clearFocus()
            showAbout -> showAbout = false
            else -> stack.pop()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BankGuardTopBar(onProfileClick = { showProfileSheet = true }) },
        bottomBar = {
            BankGuardBottomBar(
                selectedTab = selectedTab,
                // Tapping the tab you're already on returns to its main screen.
                onTabSelected = { tab ->
                    showAbout = false
                    if (tab == selectedTab) stack.popToRoot() else selectedTab = tab
                },
            )
        },
    ) { innerPadding ->
        val screenModifier = Modifier
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)

        val route = stack.top
        if (showAbout) {
            screenStateHolder.SaveableStateProvider(key = "about") {
                AboutScreen(onBack = { showAbout = false }, modifier = screenModifier)
            }
        } else screenStateHolder.SaveableStateProvider(key = "${selectedTab.name}/${route ?: "main"}") {
            when (selectedTab) {
                BankGuardTab.Home -> when (route?.let(HomeRoute::parse)) {
                    HomeRoute.Streaks -> if (profile.persona == DemoPersona.Underbanked) {
                        StreakScreen(
                            summary = streak.summary,
                            lastCompletedDay = streak.lastCompletedDay,
                            onCompleteDemoDay = streak.onCompleteDemoDay,
                            onDismissResult = streak.onDismissResult,
                            onBack = {
                                streak.onDismissResult()
                                stack.pop()
                            },
                            modifier = screenModifier,
                        )
                    } else {
                        // No account, no streak: never show it for the unbanked persona.
                        LaunchedEffect(Unit) { stack.popToRoot() }
                    }
                    null -> HomeScreen(
                        profile = profile,
                        onOpenTab = { tab -> selectedTab = tab },
                        onOpenForecast = {
                            selectedTab = BankGuardTab.Prevent
                            stacks.getValue(BankGuardTab.Prevent).push(PreventRoute.Forecast.key)
                        },
                        streak = streak.summary,
                        onOpenStreaks = { stack.push(HomeRoute.Streaks.key) },
                        modifier = screenModifier,
                    )
                }
                BankGuardTab.Prevent -> PreventTab(
                    route = route?.let(PreventRoute::parse),
                    profile = profile,
                    cancellations = cancellations,
                    stack = stack,
                    onRequestCancellation = onRequestCancellation,
                    onOpenTab = { tab -> selectedTab = tab },
                    modifier = screenModifier,
                )
                BankGuardTab.Recover -> RecoverTab(
                    route = route?.let(RecoverRoute::parse),
                    profile = profile,
                    recoveries = recoveries,
                    recovery = recovery,
                    actions = recoverActions,
                    stack = stack,
                    onOpenTab = { tab -> selectedTab = tab },
                    modifier = screenModifier,
                )
                BankGuardTab.Discover -> DiscoverTab(
                    route = route?.let(DiscoverRoute::parse),
                    profile = profile,
                    inputs = savingsInputs,
                    onInputsChange = onSavingsInputsChange,
                    onResetInputs = onResetSavingsInputs,
                    referrals = referrals,
                    onStartReferral = onStartReferral,
                    onContinueReferral = onContinueReferral,
                    stack = stack,
                    modifier = screenModifier,
                )
            }
        }
    }

    if (showProfileSheet) {
        DemoProfileSheet(
            profile = profile,
            bankMode = bankMode,
            onSetBankMode = onSetBankMode,
            onSwitch = { persona ->
                showProfileSheet = false
                onSwitchPersona(persona)
            },
            onReset = {
                showProfileSheet = false
                onResetDemo()
            },
            onOpenAbout = {
                showProfileSheet = false
                showAbout = true
            },
            onDismiss = { showProfileSheet = false },
        )
    }
}

/** The Prevent tab's main screen or whichever Prevent sub-screen is on top of its stack. */
@Composable
private fun PreventTab(
    route: PreventRoute?,
    profile: DemoProfile,
    cancellations: Map<String, CancellationStatus>,
    stack: TabBackStack,
    onRequestCancellation: (String) -> Unit,
    onOpenTab: (BankGuardTab) -> Unit,
    modifier: Modifier,
) {
    when (route) {
        null -> PreventScreen(
            profile = profile,
            cancellations = cancellations,
            onOpenForecast = { stack.push(PreventRoute.Forecast.key) },
            onOpenCancellation = { id -> stack.push(PreventRoute.Cancellation(id).key) },
            onOpenTab = onOpenTab,
            modifier = modifier,
        )
        PreventRoute.Forecast -> {
            if (profile.account == null) {
                LaunchedEffect(route) { stack.pop() }
            } else {
                ForecastDetailScreen(profile = profile, onBack = stack::pop, modifier = modifier)
            }
        }
        is PreventRoute.Cancellation -> {
            // Look the charge up in the original data: once approved, it's gone from the live profile.
            val transaction = DemoProfiles.forScenario(profile.persona, profile.scenario).transactions
                .firstOrNull { it.id == route.transactionId }
            if (transaction == null || profile.account == null) {
                LaunchedEffect(route) { stack.pop() }
            } else {
                CancellationScreen(
                    transaction = transaction,
                    profile = profile,
                    status = cancellations[transaction.id],
                    onRequest = { onRequestCancellation(transaction.id) },
                    onBack = stack::pop,
                    modifier = modifier,
                )
            }
        }
    }
}

/** The Discover tab's main screen or whichever Discover sub-screen is on top of its stack. */
@Composable
private fun DiscoverTab(
    route: DiscoverRoute?,
    profile: DemoProfile,
    inputs: SavingsInputs,
    onInputsChange: (SavingsInputs) -> Unit,
    onResetInputs: () -> Unit,
    referrals: Map<String, ReferralRecord>,
    onStartReferral: (accountId: String) -> String?,
    onContinueReferral: (referralId: String) -> Unit,
    stack: TabBackStack,
    modifier: Modifier,
) {
    val openCalculator = { stack.push(DiscoverRoute.Calculator.key) }
    when (route) {
        null -> DiscoverScreen(
            profile = profile,
            inputs = inputs,
            onCompare = { id -> stack.push(DiscoverRoute.Compare(id).key) },
            onAdjust = openCalculator,
            modifier = modifier,
        )
        is DiscoverRoute.Compare -> {
            val option = DemoAccountOptions.all.firstOrNull { it.id == route.optionId }
            if (option == null) {
                LaunchedEffect(route) { stack.pop() }
            } else {
                AccountComparisonScreen(
                    option, profile, inputs,
                    onAdjust = openCalculator,
                    // Each tap is a new simulated referral, tied to this account and its bank.
                    onOpenAccount = { onStartReferral(option.id)?.let { id -> stack.push(DiscoverRoute.OpenAccount(id).key) } },
                    onBack = stack::pop,
                    modifier = modifier,
                )
            }
        }
        is DiscoverRoute.OpenAccount -> {
            val referral = referrals[route.referralId]
            val option = referral?.let { r -> DemoAccountOptions.all.firstOrNull { it.id == r.accountId } }
            if (referral == null || option == null) {
                LaunchedEffect(route) { stack.pop() }
            } else {
                AccountOpeningScreen(
                    option = option,
                    referral = referral,
                    onContinueToBank = { onContinueReferral(referral.referralId) },
                    onBackToDiscover = stack::popToRoot,
                    onBack = stack::pop,
                    modifier = modifier,
                )
            }
        }
        DiscoverRoute.Calculator -> SavingsCalculatorScreen(
            profile = profile,
            inputs = inputs,
            onInputsChange = onInputsChange,
            onReset = onResetInputs,
            onBack = stack::pop,
            modifier = modifier,
        )
    }
}

/** Overdraft-free streak state and the demo-day controls, for Home and the Achievements screen. */
data class StreakUiState(
    val summary: StreakSummary,
    val lastCompletedDay: CompletedDay?,
    val onCompleteDemoDay: () -> Unit,
    val onDismissResult: () -> Unit,
)

/** What the Recover screens can change in the demo session. */
data class RecoverActions(
    val onSaveLetter: (feeId: String, letter: String, reasons: Set<WaiverReason>) -> Unit,
    val onMarkSent: (feeId: String) -> Unit,
    val onRecordDecision: (feeId: String, refunded: Boolean) -> Unit,
    val onCheckEligibility: (transactionId: String) -> Unit = {},
    val onRequestRecovery: (transactionId: String, feeConsent: Boolean) -> Unit = { _, _ -> },
    val onRefreshStatus: (transactionId: String) -> Unit = {},
    val onSetScenario: (AccountScenario) -> Unit = {},
)

/** The Recover tab's main screen or whichever Recover sub-screen is on top of its stack. */
@Composable
private fun RecoverTab(
    route: RecoverRoute?,
    profile: DemoProfile,
    recoveries: Map<String, FeeRecovery>,
    recovery: RecoveryUiState,
    actions: RecoverActions,
    stack: TabBackStack,
    onOpenTab: (BankGuardTab) -> Unit,
    modifier: Modifier,
) {
    val account = profile.account
    when (route) {
        null -> RecoverScreen(
            profile = profile,
            recovery = recovery,
            feeRecoveries = recoveries,
            onOpenRecovery = { id -> stack.push(RecoverRoute.Recovery(id).key) },
            onOpenFee = { id -> stack.push(RecoverRoute.Fee(id).key) },
            onSetScenario = actions.onSetScenario,
            onOpenTab = onOpenTab,
            modifier = modifier,
        )
        is RecoverRoute.Recovery -> {
            // The charge as originally loaded: its live status changes once a hold is released.
            val transaction = DemoProfiles.forScenario(profile.persona, profile.scenario).transactions
                .firstOrNull { it.id == route.transactionId }
            if (transaction == null || account == null) {
                LaunchedEffect(route) { stack.pop() }
            } else {
                RecoveryDetailScreen(
                    transaction = transaction,
                    cause = OverdraftDetector.find(profile)?.takeIf { it.transaction.id == transaction.id },
                    record = recovery.records[transaction.id],
                    check = recovery.eligibility[transaction.id],
                    checkingStatus = transaction.id in recovery.statusChecks,
                    onCheckEligibility = { actions.onCheckEligibility(transaction.id) },
                    onRequest = { consent -> actions.onRequestRecovery(transaction.id, consent) },
                    onRefreshStatus = { actions.onRefreshStatus(transaction.id) },
                    onOpenFeeHelp = stack::popToRoot,
                    onOpenPrevent = { onOpenTab(BankGuardTab.Prevent) },
                    onBack = stack::pop,
                    modifier = modifier,
                )
            }
        }
        is RecoverRoute.Fee, is RecoverRoute.Letter -> {
            val feeId = if (route is RecoverRoute.Fee) route.feeId else (route as RecoverRoute.Letter).feeId
            val fee = profile.bankFees.firstOrNull { it.id == feeId }
            when {
                fee == null || account == null -> LaunchedEffect(route) { stack.pop() }
                route is RecoverRoute.Fee -> FeeDetailScreen(
                    fee = fee,
                    recovery = recoveries[fee.id],
                    onOpenLetter = { stack.push(RecoverRoute.Letter(fee.id).key) },
                    onMarkSent = { actions.onMarkSent(fee.id) },
                    onRecordDecision = { refunded -> actions.onRecordDecision(fee.id, refunded) },
                    onBack = stack::pop,
                    modifier = modifier,
                )
                else -> WaiverLetterScreen(
                    fee = fee,
                    account = account,
                    recovery = recoveries[fee.id],
                    onSave = { letter, reasons -> actions.onSaveLetter(fee.id, letter, reasons) },
                    onBack = stack::pop,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun BankGuardBottomBar(
    selectedTab: BankGuardTab,
    onTabSelected: (BankGuardTab) -> Unit,
) {
    Column {
        HorizontalDivider(color = BankGuardTheme.colors.cardBorder)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
            BankGuardTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        // The label already names the tab, so the icon is decorative for screen readers.
                        Icon(
                            painter = painterResource(if (selected) tab.selectedIconRes else tab.iconRes),
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(tab.labelRes), style = MaterialTheme.typography.labelMedium) },
                    alwaysShowLabel = true,
                    modifier = Modifier.testTag("tab_${tab.name}"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun BankGuardMainPreview() {
    BankGuardTheme {
        BankGuardMain(
            profile = DemoProfiles.underbanked,
            cancellations = emptyMap(),
            bankMode = SimulatedBankMode.ApproveEligible,
            onRequestCancellation = {},
            onSetBankMode = {},
            recoveries = emptyMap(),
            recovery = RecoveryUiState(),
            recoverActions = RecoverActions({ _, _, _ -> }, {}, { _, _ -> }),
            savingsInputs = DemoAccountOptions.defaultInputs(DemoPersona.Underbanked),
            onSavingsInputsChange = {},
            onResetSavingsInputs = {},
            referrals = emptyMap(),
            streak = StreakUiState(
                com.example.bankguard.domain.streaks.StreakEngine.summarize(
                    com.example.bankguard.data.demo.DemoStreakHistory.lowestBalances,
                    com.example.bankguard.data.demo.DemoStreakHistory.START_DAY,
                ),
                null, {}, {},
            ),
            onStartReferral = { null },
            onContinueReferral = {},
            onSwitchPersona = {},
            onResetDemo = {},
        )
    }
}
