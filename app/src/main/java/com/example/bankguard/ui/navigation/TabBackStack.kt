package com.example.bankguard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Screens opened inside a tab, on top of the tab's main screen.
 * An empty stack means the tab's main screen is showing.
 *
 * This is a deliberately small alternative to Navigation Compose: routes are plain strings
 * (see [PreventRoute]), the stack survives rotation and process death, and the system Back
 * button pops it (wired up in BankGuardApp).
 */
@Stable
class TabBackStack(initial: List<String> = emptyList()) {
    private val entries = mutableStateListOf<String>().apply { addAll(initial) }

    val top: String? get() = entries.lastOrNull()
    val isEmpty: Boolean get() = entries.isEmpty()

    fun push(route: String) {
        if (entries.lastOrNull() != route) entries.add(route)
    }

    fun pop() {
        if (entries.isNotEmpty()) entries.removeAt(entries.lastIndex)
    }

    fun popToRoot() = entries.clear()

    companion object {
        val Saver: Saver<TabBackStack, Any> = listSaver(
            save = { it.entries.toList() },
            restore = { TabBackStack(it) },
        )
    }
}

@Composable
fun rememberTabBackStack(): TabBackStack = rememberSaveable(saver = TabBackStack.Saver) { TabBackStack() }

/** Screens that open inside the Home tab. */
sealed interface HomeRoute {
    /** Overdraft-free streak details and achievements. */
    data object Streaks : HomeRoute

    val key: String get() = "streaks"

    companion object {
        fun parse(key: String): HomeRoute? = if (key == "streaks") Streaks else null
    }
}

/** Screens that open inside the Prevent tab. */
sealed interface PreventRoute {
    data object Forecast : PreventRoute
    data class Cancellation(val transactionId: String) : PreventRoute

    val key: String
        get() = when (this) {
            Forecast -> "forecast"
            is Cancellation -> "cancel:$transactionId"
        }

    companion object {
        fun parse(key: String): PreventRoute? = when {
            key == "forecast" -> Forecast
            key.startsWith("cancel:") -> Cancellation(key.removePrefix("cancel:"))
            else -> null
        }
    }
}

/** Screens that open inside the Recover tab. */
sealed interface RecoverRoute {
    /** Recovery details for one overdraft-causing charge. */
    data class Recovery(val transactionId: String) : RecoverRoute
    data class Fee(val feeId: String) : RecoverRoute
    data class Letter(val feeId: String) : RecoverRoute

    val key: String
        get() = when (this) {
            is Recovery -> "recovery:$transactionId"
            is Fee -> "fee:$feeId"
            is Letter -> "letter:$feeId"
        }

    companion object {
        fun parse(key: String): RecoverRoute? = when {
            key.startsWith("recovery:") -> Recovery(key.removePrefix("recovery:"))
            key.startsWith("fee:") -> Fee(key.removePrefix("fee:"))
            key.startsWith("letter:") -> Letter(key.removePrefix("letter:"))
            else -> null
        }
    }
}

/** Screens that open inside the Discover tab. */
sealed interface DiscoverRoute {
    data class Compare(val optionId: String) : DiscoverRoute
    data object Calculator : DiscoverRoute

    /** The selected bank's simulated account-opening page, for one referral. */
    data class OpenAccount(val referralId: String) : DiscoverRoute

    val key: String
        get() = when (this) {
            is Compare -> "compare:$optionId"
            Calculator -> "calculator"
            is OpenAccount -> "open:$referralId"
        }

    companion object {
        fun parse(key: String): DiscoverRoute? = when {
            key.startsWith("compare:") -> Compare(key.removePrefix("compare:"))
            key == "calculator" -> Calculator
            key.startsWith("open:") -> OpenAccount(key.removePrefix("open:"))
            else -> null
        }
    }
}
