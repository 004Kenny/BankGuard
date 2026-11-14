package com.example.bankguard.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bankguard.data.demo.DemoAccountOptions
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.data.simulated.PartnerBankGateway
import com.example.bankguard.data.simulated.SimulatedBankMode
import com.example.bankguard.data.simulated.SimulatedPartnerBank
import com.example.bankguard.domain.cancellation.withCancellations
import com.example.bankguard.model.CancellationDecision
import com.example.bankguard.model.CancellationStatus
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.FeeRecovery
import com.example.bankguard.model.Money
import com.example.bankguard.model.SavingsInputs
import com.example.bankguard.model.RecoveryStage
import com.example.bankguard.model.WaiverReason
import com.example.bankguard.model.TransactionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * App-wide demo state: the active persona, cancellation requests, the simulated bank's mode,
 * and the user's progress asking for fees back.
 *
 * Kept in [SavedStateHandle], so it survives rotation and the system killing the app in the
 * background, but a fresh launch always starts at persona selection. That is deliberate for
 * demos: every presentation begins from the same clean state.
 */
class DemoSessionViewModel @JvmOverloads constructor(
    private val savedState: SavedStateHandle,
    gateway: PartnerBankGateway? = null,
) : ViewModel() {

    val persona: StateFlow<DemoPersona?> = savedState
        .getStateFlow<String?>(KEY_PERSONA, null)
        .map { name -> name?.let(DemoPersona::valueOf) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, currentPersona())

    val bankMode: StateFlow<SimulatedBankMode> = savedState
        .getStateFlow(KEY_BANK_MODE, SimulatedBankMode.ApproveEligible.name)
        .map(SimulatedBankMode::valueOf)
        .stateIn(viewModelScope, SharingStarted.Eagerly, currentBankMode())

    private val gateway: PartnerBankGateway = gateway ?: SimulatedPartnerBank(mode = { bankMode.value })

    private val _cancellations = MutableStateFlow(restoreCancellations())

    /** Cancellation requests by transaction id. Only approved ones change balances. */
    val cancellations: StateFlow<Map<String, CancellationStatus>> = _cancellations.asStateFlow()

    /** The active persona's demo data with approved cancellations applied. */
    val profile: StateFlow<DemoProfile?> = combine(persona, _cancellations) { p, c ->
        p?.let { DemoProfiles.forPersona(it).withCancellations(c) }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        currentPersona()?.let { DemoProfiles.forPersona(it).withCancellations(_cancellations.value) },
    )

    private val _recoveries = MutableStateFlow(restoreRecoveries())

    /** Progress asking for each bank fee back, by fee id. Missing means not started. */
    val recoveries: StateFlow<Map<String, FeeRecovery>> = _recoveries.asStateFlow()

    private val _savingsInputs = MutableStateFlow(restoreSavingsInputs())

    /** The user's own estimate inputs per persona; personas without an entry use the demo defaults. */
    val savingsInputs: StateFlow<Map<DemoPersona, SavingsInputs>> = _savingsInputs.asStateFlow()

    fun inputsFor(persona: DemoPersona): SavingsInputs =
        _savingsInputs.value[persona] ?: DemoAccountOptions.defaultInputs(persona)

    fun setSavingsInputs(persona: DemoPersona, inputs: SavingsInputs) {
        _savingsInputs.update { it + (persona to inputs) }
        persistSavingsInputs()
    }

    fun resetSavingsInputs(persona: DemoPersona) {
        _savingsInputs.update { it - persona }
        persistSavingsInputs()
    }

    /** Whether the welcome intro has been shown this session. Reset keeps it, so reset lands on persona selection. */
    val introSeen: StateFlow<Boolean> = savedState.getStateFlow(KEY_INTRO_SEEN, false)

    fun finishIntro() {
        savedState[KEY_INTRO_SEEN] = true
    }

    fun selectPersona(persona: DemoPersona) {
        savedState[KEY_PERSONA] = persona.name
    }

    fun setBankMode(mode: SimulatedBankMode) {
        savedState[KEY_BANK_MODE] = mode.name
    }

    /**
     * Sends ONE cancellation request for a pending charge to the simulated partner bank.
     * Called only after the user has explicitly confirmed. Ignored if a request for this charge
     * already exists, or if the charge isn't pending.
     */
    fun requestCancellation(transactionId: String) {
        if (_cancellations.value.containsKey(transactionId)) return
        val transaction = profile.value?.transactions
            ?.firstOrNull { it.id == transactionId && it.status == TransactionStatus.Pending }
            ?: return

        _cancellations.update { it + (transactionId to CancellationStatus.Processing) }
        viewModelScope.launch {
            val result = when (val decision = gateway.requestCancellation(transaction)) {
                is CancellationDecision.Approved -> CancellationStatus.Approved(decision.referenceId)
                is CancellationDecision.Declined -> CancellationStatus.Declined(decision.reason)
            }
            _cancellations.update { it + (transactionId to result) }
            persistCancellations()
        }
    }

    /** Saves the user's (possibly edited) letter. Moves a new request to Drafted; never moves one backwards. */
    fun saveLetter(feeId: String, letter: String, reasons: Set<WaiverReason>) {
        updateRecovery(feeId) { current ->
            current.copy(
                letter = letter,
                reasons = reasons,
                stage = if (current.stage == RecoveryStage.Found) RecoveryStage.Drafted else current.stage,
            )
        }
    }

    /** The user says they sent the letter themselves. BankGuard sends nothing. */
    fun markSent(feeId: String) {
        updateRecovery(feeId) { current ->
            if (current.stage == RecoveryStage.Drafted) current.copy(stage = RecoveryStage.Sent) else current
        }
    }

    /** The user records what their bank decided. Only possible after they've sent the request. */
    fun recordDecision(feeId: String, refunded: Boolean) {
        updateRecovery(feeId) { current ->
            if (current.stage == RecoveryStage.Sent) {
                current.copy(stage = if (refunded) RecoveryStage.Refunded else RecoveryStage.NotRefunded)
            } else {
                current
            }
        }
    }

    private fun updateRecovery(feeId: String, change: (FeeRecovery) -> FeeRecovery) {
        _recoveries.update { all -> all + (feeId to change(all[feeId] ?: FeeRecovery())) }
        persistRecoveries()
    }

    /** Back to persona selection with every demo action cleared. */
    fun resetDemo() {
        _cancellations.value = emptyMap()
        persistCancellations()
        _recoveries.value = emptyMap()
        persistRecoveries()
        _savingsInputs.value = emptyMap()
        persistSavingsInputs()
        savedState[KEY_BANK_MODE] = SimulatedBankMode.ApproveEligible.name
        savedState[KEY_PERSONA] = null
    }

    private fun currentPersona(): DemoPersona? =
        savedState.get<String>(KEY_PERSONA)?.let(DemoPersona::valueOf)

    private fun currentBankMode(): SimulatedBankMode =
        savedState.get<String>(KEY_BANK_MODE)?.let(SimulatedBankMode::valueOf) ?: SimulatedBankMode.ApproveEligible

    // Saved as "A|<reference>" or "D|<reason>". A request still processing when the app is
    // killed was never answered, so it isn't saved and the user can send it again.
    private fun persistCancellations() {
        val encoded = HashMap<String, String>()
        _cancellations.value.forEach { (id, status) ->
            when (status) {
                is CancellationStatus.Approved -> encoded[id] = "A|" + status.referenceId
                is CancellationStatus.Declined -> encoded[id] = "D|" + status.reason
                CancellationStatus.Processing -> Unit
            }
        }
        savedState[KEY_CANCELLATIONS] = encoded
    }

    private fun restoreCancellations(): Map<String, CancellationStatus> {
        val saved = savedState.get<HashMap<String, String>>(KEY_CANCELLATIONS) ?: return emptyMap()
        return saved.mapNotNull { (id, value) ->
            val detail = value.substringAfter('|', "")
            when (value.substringBefore('|')) {
                "A" -> id to CancellationStatus.Approved(detail)
                "D" -> id to CancellationStatus.Declined(detail)
                else -> null
            }
        }.toMap()
    }

    private fun persistRecoveries() {
        val stages = HashMap<String, String>()
        val letters = HashMap<String, String>()
        val reasons = HashMap<String, String>()
        _recoveries.value.forEach { (id, r) ->
            stages[id] = r.stage.name
            r.letter?.let { letters[id] = it }
            reasons[id] = r.reasons.joinToString(",") { it.name }
        }
        savedState[KEY_RECOVERY_STAGES] = stages
        savedState[KEY_RECOVERY_LETTERS] = letters
        savedState[KEY_RECOVERY_REASONS] = reasons
    }

    private fun restoreRecoveries(): Map<String, FeeRecovery> {
        val stages = savedState.get<HashMap<String, String>>(KEY_RECOVERY_STAGES) ?: return emptyMap()
        val letters = savedState.get<HashMap<String, String>>(KEY_RECOVERY_LETTERS) ?: emptyMap()
        val reasons = savedState.get<HashMap<String, String>>(KEY_RECOVERY_REASONS) ?: emptyMap()
        return stages.mapNotNull { (id, stage) ->
            val parsedStage = RecoveryStage.entries.firstOrNull { it.name == stage } ?: return@mapNotNull null
            val parsedReasons = reasons[id].orEmpty().split(',')
                .mapNotNull { name -> WaiverReason.entries.firstOrNull { it.name == name } }
                .toSet()
            id to FeeRecovery(parsedStage, letters[id], parsedReasons)
        }.toMap()
    }

    // Saved as "overdrafts,monthsBelowMinimum,atmPerMonth,directDeposit,altServiceCents".
    private fun persistSavingsInputs() {
        val encoded = HashMap<String, String>()
        _savingsInputs.value.forEach { (persona, i) ->
            encoded[persona.name] = listOf(
                i.overdraftsPerYear, i.monthsBelowMinimum, i.outOfNetworkAtmPerMonth,
                if (i.hasDirectDeposit) 1 else 0, i.currentAltServiceMonthly.cents,
            ).joinToString(",")
        }
        savedState[KEY_SAVINGS_INPUTS] = encoded
    }

    private fun restoreSavingsInputs(): Map<DemoPersona, SavingsInputs> {
        val saved = savedState.get<HashMap<String, String>>(KEY_SAVINGS_INPUTS) ?: return emptyMap()
        return saved.mapNotNull { (name, value) ->
            val persona = DemoPersona.entries.firstOrNull { it.name == name } ?: return@mapNotNull null
            val parts = value.split(',').mapNotNull { it.toLongOrNull() }
            if (parts.size != 5) return@mapNotNull null
            persona to SavingsInputs(parts[0].toInt(), parts[1].toInt(), parts[2].toInt(), parts[3] == 1L, Money(parts[4]))
        }.toMap()
    }

    private companion object {
        const val KEY_INTRO_SEEN = "demo_intro_seen"
        const val KEY_SAVINGS_INPUTS = "demo_savings_inputs"
        const val KEY_RECOVERY_STAGES = "demo_recovery_stages"
        const val KEY_RECOVERY_LETTERS = "demo_recovery_letters"
        const val KEY_RECOVERY_REASONS = "demo_recovery_reasons"
        const val KEY_PERSONA = "demo_persona"
        const val KEY_BANK_MODE = "demo_bank_mode"
        const val KEY_CANCELLATIONS = "demo_cancellations"
    }
}
