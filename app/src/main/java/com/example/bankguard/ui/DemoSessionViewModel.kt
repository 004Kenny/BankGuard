package com.example.bankguard.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bankguard.data.demo.AccountScenario
import com.example.bankguard.data.demo.DemoAccountOptions
import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.data.demo.DemoProfiles
import com.example.bankguard.data.simulated.PartnerBankGateway
import com.example.bankguard.data.simulated.RecoveryFeeCollector
import com.example.bankguard.data.simulated.SimulatedFeeCollector
import com.example.bankguard.data.simulated.SimulatedBankMode
import com.example.bankguard.data.simulated.SimulatedPartnerBank
import com.example.bankguard.domain.cancellation.withCancellations
import com.example.bankguard.domain.recovery.OverdraftDetector
import com.example.bankguard.domain.recovery.RecoveryFeeCalculator
import com.example.bankguard.domain.recovery.withRecoveries
import com.example.bankguard.model.CancellationDecision
import com.example.bankguard.model.CancellationStatus
import com.example.bankguard.model.DemoPersona
import com.example.bankguard.model.EligibilityCheck
import com.example.bankguard.model.FeeCollectionResult
import com.example.bankguard.model.FeeCollectionStatus
import com.example.bankguard.model.RecoveryEligibility
import com.example.bankguard.model.RecoveryMechanism
import com.example.bankguard.model.RecoveryRecord
import com.example.bankguard.model.RecoveryRequestStatus
import com.example.bankguard.model.RecoveryStatusUpdate
import com.example.bankguard.model.RecoverySubmission
import com.example.bankguard.model.FeeRecovery
import com.example.bankguard.model.Money
import com.example.bankguard.model.SavingsInputs
import com.example.bankguard.data.demo.DemoStreakHistory
import com.example.bankguard.domain.streaks.DayStatus
import com.example.bankguard.domain.streaks.StreakBadge
import com.example.bankguard.domain.streaks.StreakEngine
import com.example.bankguard.domain.streaks.StreakSummary
import com.example.bankguard.model.ReferralRecord
import com.example.bankguard.model.ReferralSource
import com.example.bankguard.model.ReferralStatus
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
 * App-wide demo state: the active persona and account scenario, cancellation requests,
 * transaction recovery requests, the simulated bank's mode, and the user's progress asking
 * for fees back. Every screen reads balances from [profile], so they always agree.
 *
 * Kept in [SavedStateHandle], so it survives rotation and the system killing the app in the
 * background, but a fresh launch always starts at persona selection. That is deliberate for
 * demos: every presentation begins from the same clean state.
 */
class DemoSessionViewModel @JvmOverloads constructor(
    private val savedState: SavedStateHandle,
    gateway: PartnerBankGateway? = null,
    private val feeCollector: RecoveryFeeCollector = SimulatedFeeCollector(),
    private val clock: () -> Long = System::currentTimeMillis,
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

    /** Which simulated account state the underbanked persona is in. See [AccountScenario]. */
    val scenario: StateFlow<AccountScenario> = savedState
        .getStateFlow<String?>(KEY_SCENARIO, null)
        .map(AccountScenario::fromKey)
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccountScenario.fromKey(savedState[KEY_SCENARIO]))

    private val _recoveryRecords = MutableStateFlow(restoreRecoveryRecords())

    /**
     * Transaction recovery requests by transaction id, kept as permanent history. Ids are unique
     * per scenario, and each record names its scenario, so records never mix.
     */
    val recoveryRecords: StateFlow<Map<String, RecoveryRecord>> = _recoveryRecords.asStateFlow()

    private val _eligibility = MutableStateFlow<Map<String, EligibilityCheck>>(emptyMap())

    /** Eligibility checks with the simulated bank, by transaction id. Not saved: re-checked when needed. */
    val eligibility: StateFlow<Map<String, EligibilityCheck>> = _eligibility.asStateFlow()

    private val _statusChecks = MutableStateFlow<Set<String>>(emptySet())

    /** Transactions whose request status is being re-checked right now. */
    val statusChecks: StateFlow<Set<String>> = _statusChecks.asStateFlow()

    /**
     * The active persona's data in the current scenario, with approved cancellations and
     * confirmed recoveries applied. The single source of balances for Home, Prevent and Recover.
     */
    val profile: StateFlow<DemoProfile?> = combine(persona, scenario, _cancellations, _recoveryRecords) { p, sc, c, r ->
        p?.let { buildProfile(it, sc, c, r) }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        currentPersona()?.let { buildProfile(it, scenario.value, _cancellations.value, _recoveryRecords.value) },
    )

    private fun buildProfile(
        persona: DemoPersona,
        scenario: AccountScenario,
        cancellations: Map<String, CancellationStatus>,
        records: Map<String, RecoveryRecord>,
    ): DemoProfile = DemoProfiles.forScenario(persona, scenario)
        .withCancellations(cancellations)
        .withRecoveries(records.values)

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

    private val _referrals = MutableStateFlow(restoreReferrals())

    /** Simulated BankGuard referrals from Discover, by referral id. None means no commission. */
    val referrals: StateFlow<Map<String, ReferralRecord>> = _referrals.asStateFlow()

    /**
     * Records a simulated referral from BankGuard to the bank behind Discover account [accountId]
     * and returns its id, or null for an account Discover doesn't list. Nothing leaves the device.
     */
    fun startReferral(accountId: String): String? {
        val option = DemoAccountOptions.all.firstOrNull { it.id == accountId } ?: return null
        var id: String? = null
        _referrals.update { current ->
            val referralId = "REF-BG-%04d".format(current.size + 1)
            id = referralId
            current + (referralId to ReferralRecord(
                referralId = referralId,
                bankId = option.bankId,
                accountId = option.id,
                source = ReferralSource.BankGuard,
                createdAtMillis = clock(),
            ))
        }
        persistReferrals()
        return id
    }

    /** The user continued to the bank's simulated application. Only an Initiated referral moves on. */
    fun continueReferral(referralId: String) {
        _referrals.update { current ->
            val referral = current[referralId]?.takeIf { it.status == ReferralStatus.Initiated } ?: return@update current
            current + (referralId to referral.copy(status = ReferralStatus.ContinuedToBank, continuedAtMillis = clock()))
        }
        persistReferrals()
    }

    // Saved as "bankId<US>accountId<US>source<US>created<US>status<US>continued".
    private fun persistReferrals() {
        val encoded = HashMap<String, String>()
        _referrals.value.forEach { (id, r) ->
            encoded[id] = listOf(r.bankId, r.accountId, r.source.name, r.createdAtMillis, r.status.name, r.continuedAtMillis)
                .joinToString(FIELD_SEPARATOR) { it?.toString() ?: "" }
        }
        savedState[KEY_REFERRALS] = encoded
    }

    private fun restoreReferrals(): Map<String, ReferralRecord> {
        val saved = savedState.get<HashMap<String, String>>(KEY_REFERRALS) ?: return emptyMap()
        return saved.mapNotNull { (id, value) ->
            val f = value.split(FIELD_SEPARATOR)
            if (f.size != 6) return@mapNotNull null
            val source = ReferralSource.entries.firstOrNull { it.name == f[2] } ?: return@mapNotNull null
            val status = ReferralStatus.entries.firstOrNull { it.name == f[4] } ?: return@mapNotNull null
            val created = f[3].toLongOrNull() ?: return@mapNotNull null
            id to ReferralRecord(id, f[0], f[1], source, created, status, f[5].toLongOrNull())
        }.toMap()
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
        // A charge already being recovered is Recover's; never handle it twice.
        if (_recoveryRecords.value.containsKey(transactionId)) return
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


    // ---- Recover: post-overdraft transaction recovery -------------------------------------

    /** Loads one of the underbanked persona's account scenarios. Records are kept per scenario. */
    fun setScenario(scenario: AccountScenario) {
        savedState[KEY_SCENARIO] = scenario.key
        _eligibility.value = emptyMap()
    }

    /** Asks the simulated bank whether a charge qualifies for recovery. Ignored while a check is running. */
    fun checkRecoveryEligibility(transactionId: String) {
        if (_eligibility.value[transactionId] == EligibilityCheck.Checking) return
        val transaction = profile.value?.transactions?.firstOrNull { it.id == transactionId } ?: return
        _eligibility.update { it + (transactionId to EligibilityCheck.Checking) }
        viewModelScope.launch {
            val result = try {
                EligibilityCheck.Done(gateway.checkRecoveryEligibility(transaction))
            } catch (e: Exception) {
                EligibilityCheck.Failed(e.message ?: "The simulated partner bank didn't respond.")
            }
            _eligibility.update { it + (transactionId to result) }
        }
    }

    /**
     * Submits ONE recovery request for the charge that caused the current overdraft. Called only
     * after the user confirmed and agreed to the exact fee shown ([feeConsent]). Ignored unless:
     * consent was given, the bank said the charge is eligible, the charge is still the cause of
     * the overdraft, and no request or cancellation exists for it already.
     *
     * The fee is fixed now, from the balance BEFORE the charge, and only charged if the bank
     * later confirms the recovery.
     */
    fun requestRecovery(transactionId: String, feeConsent: Boolean) {
        if (!feeConsent) return
        if (_recoveryRecords.value.containsKey(transactionId)) return
        if (_cancellations.value.containsKey(transactionId)) return
        val eligible = (_eligibility.value[transactionId] as? EligibilityCheck.Done)?.result as? RecoveryEligibility.Eligible
            ?: return
        val current = profile.value ?: return
        val cause = OverdraftDetector.find(current)?.takeIf { it.transaction.id == transactionId } ?: return
        val transaction = cause.transaction

        _recoveryRecords.update {
            it + (transactionId to RecoveryRecord(
                transactionId = transactionId,
                scenarioKey = current.scenario.key,
                merchant = transaction.merchant,
                transactionAmount = transaction.amount,
                transactionDayOffset = transaction.dayOffset,
                originalStatus = transaction.status,
                mechanism = eligible.mechanism,
                balanceBefore = cause.balanceBefore,
                balanceAfter = cause.balanceAfter,
                quotedFee = RecoveryFeeCalculator.fee(cause.balanceBefore),
                status = RecoveryRequestStatus.Submitted,
                submittedAtMillis = clock(),
            ))
        }
        viewModelScope.launch {
            when (val submission = gateway.submitRecovery(transaction)) {
                is RecoverySubmission.Rejected -> updateRecord(transactionId) {
                    it.copy(status = RecoveryRequestStatus.Rejected, rejectionReason = submission.reason)
                }
                is RecoverySubmission.Accepted -> {
                    updateRecord(transactionId) {
                        it.copy(status = RecoveryRequestStatus.AwaitingConfirmation, requestReference = submission.requestReference)
                    }
                    pollStatus(transactionId)
                }
            }
        }
    }

    /** Asks the simulated bank again about a request that's awaiting confirmation. */
    fun refreshRecoveryStatus(transactionId: String) {
        if (_recoveryRecords.value[transactionId]?.status != RecoveryRequestStatus.AwaitingConfirmation) return
        if (transactionId in _statusChecks.value) return
        viewModelScope.launch { pollStatus(transactionId) }
    }

    private suspend fun pollStatus(transactionId: String) {
        val record = _recoveryRecords.value[transactionId] ?: return
        val reference = record.requestReference ?: return
        // Only the underbanked persona has an account, so only it can have recovery records.
        val transaction = DemoProfiles.forScenario(DemoPersona.Underbanked, AccountScenario.fromKey(record.scenarioKey))
            .transactions.firstOrNull { it.id == transactionId } ?: return
        _statusChecks.update { it + transactionId }
        try {
            when (val update = gateway.recoveryStatus(transaction, reference)) {
                RecoveryStatusUpdate.AwaitingConfirmation -> Unit
                is RecoveryStatusUpdate.Rejected -> updateRecord(transactionId) { r ->
                    if (r.status == RecoveryRequestStatus.AwaitingConfirmation) {
                        r.copy(status = RecoveryRequestStatus.Rejected, rejectionReason = update.reason)
                    } else {
                        r
                    }
                }
                is RecoveryStatusUpdate.Confirmed -> confirmRecovery(transactionId, update)
            }
        } finally {
            _statusChecks.update { it - transactionId }
        }
    }

    /**
     * Applies the bank's confirmation exactly once: a second confirmation for the same request
     * is ignored, so the credit and the fee can never be counted twice.
     */
    private suspend fun confirmRecovery(transactionId: String, confirmed: RecoveryStatusUpdate.Confirmed) {
        var applied = false
        updateRecord(transactionId) { r ->
            if (r.status != RecoveryRequestStatus.AwaitingConfirmation) return@updateRecord r
            applied = true
            r.copy(
                status = RecoveryRequestStatus.Confirmed,
                confirmationReference = confirmed.confirmationReference,
                confirmedAtMillis = clock(),
                // Never more than the original charge, whatever the bank reports.
                amountRestored = minOf(confirmed.amountRestored, r.transactionAmount.absolute),
                feeStatus = if (r.quotedFee == Money.Zero) FeeCollectionStatus.NotCharged else FeeCollectionStatus.Assessed,
            )
        }
        if (applied) collectFee(transactionId)
    }

    /** BankGuard's fee as its own event, only after a confirmed recovery, and only once. */
    private suspend fun collectFee(transactionId: String) {
        val record = _recoveryRecords.value[transactionId] ?: return
        if (!record.isConfirmed || record.feeStatus != FeeCollectionStatus.Assessed) return
        val result = try {
            feeCollector.collect(transactionId, record.quotedFee)
        } catch (e: Exception) {
            FeeCollectionResult.Failed(e.message ?: "The fee couldn't be collected.")
        }
        updateRecord(transactionId) { r ->
            if (r.feeStatus != FeeCollectionStatus.Assessed) return@updateRecord r
            when (result) {
                FeeCollectionResult.Collected -> r.copy(feeStatus = FeeCollectionStatus.Collected)
                is FeeCollectionResult.Failed -> r.copy(feeStatus = FeeCollectionStatus.CollectionFailed, feeFailureReason = result.reason)
            }
        }
    }

    private fun updateRecord(transactionId: String, change: (RecoveryRecord) -> RecoveryRecord) {
        _recoveryRecords.update { all ->
            val current = all[transactionId] ?: return@update all
            all + (transactionId to change(current))
        }
        persistRecoveryRecords()
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
        _recoveryRecords.value = emptyMap()
        persistRecoveryRecords()
        _eligibility.value = emptyMap()
        _referrals.value = emptyMap()
        persistReferrals()
        resetStreaks()
        savedState[KEY_SCENARIO] = null
        savedState[KEY_BANK_MODE] = SimulatedBankMode.ApproveEligible.name
        savedState[KEY_PERSONA] = null
    }

    private fun currentPersona(): DemoPersona? =
        savedState.get<String>(KEY_PERSONA)?.let(DemoPersona::valueOf)

    private fun currentBankMode(): SimulatedBankMode =
        savedState.get<String>(KEY_BANK_MODE)?.let(SimulatedBankMode::valueOf) ?: SimulatedBankMode.ApproveEligible

    // One string per record, fields separated by a unit separator. A request still waiting for
    // the bank's first answer when the app is killed was never answered, so it isn't saved.
    private fun persistRecoveryRecords() {
        val encoded = HashMap<String, String>()
        _recoveryRecords.value.forEach { (id, r) ->
            if (r.status == RecoveryRequestStatus.Submitted) return@forEach
            encoded[id] = listOf(
                r.scenarioKey, r.merchant, r.transactionAmount.cents, r.transactionDayOffset, r.originalStatus.name,
                r.mechanism.name, r.balanceBefore.cents, r.balanceAfter.cents, r.quotedFee.cents, r.status.name,
                r.requestReference, r.confirmationReference, r.rejectionReason, r.submittedAtMillis, r.confirmedAtMillis,
                r.amountRestored.cents, r.feeStatus.name, r.feeFailureReason,
            ).joinToString(FIELD_SEPARATOR) { it?.toString() ?: "" }
        }
        savedState[KEY_RECOVERY_RECORDS] = encoded
    }

    private fun restoreRecoveryRecords(): Map<String, RecoveryRecord> {
        val saved = savedState.get<HashMap<String, String>>(KEY_RECOVERY_RECORDS) ?: return emptyMap()
        return saved.mapNotNull { (id, value) ->
            val f = value.split(FIELD_SEPARATOR)
            if (f.size != 18) return@mapNotNull null
            fun text(i: Int) = f[i].ifEmpty { null }
            fun money(i: Int) = Money(f[i].toLong())
            runCatching {
                id to RecoveryRecord(
                    transactionId = id,
                    scenarioKey = f[0],
                    merchant = f[1],
                    transactionAmount = money(2),
                    transactionDayOffset = f[3].toInt(),
                    originalStatus = TransactionStatus.valueOf(f[4]),
                    mechanism = RecoveryMechanism.valueOf(f[5]),
                    balanceBefore = money(6),
                    balanceAfter = money(7),
                    quotedFee = money(8),
                    status = RecoveryRequestStatus.valueOf(f[9]),
                    requestReference = text(10),
                    confirmationReference = text(11),
                    rejectionReason = text(12),
                    submittedAtMillis = f[13].toLong(),
                    confirmedAtMillis = text(14)?.toLong(),
                    amountRestored = money(15),
                    feeStatus = FeeCollectionStatus.valueOf(f[16]),
                    feeFailureReason = text(17),
                )
            }.getOrNull()
        }.toMap()
    }

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

    // ----- Overdraft-free streaks (underbanked persona only) -----

    private val _streakDay = MutableStateFlow(savedState.get<Int>(KEY_STREAK_DAY) ?: DemoStreakHistory.START_DAY)
    private val _streakHistory = MutableStateFlow(restoreStreakHistory())

    /** Lowest available balance seen so far on the simulated day in progress; null if none seen yet. */
    private val _todayLowest = MutableStateFlow(savedState.get<Long>(KEY_STREAK_TODAY_LOW)?.let(::Money))

    /** Stars, streaks and badges for the underbanked persona, from its completed-day history. */
    val streak: StateFlow<StreakSummary> = combine(_streakHistory, _streakDay) { h, d -> StreakEngine.summarize(h, d) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StreakEngine.summarize(_streakHistory.value, _streakDay.value))

    private val _lastCompletedDay = MutableStateFlow<CompletedDay?>(null)

    /** What the last "Complete demo day" did, for the celebration or result message. Not saved. */
    val lastCompletedDay: StateFlow<CompletedDay?> = _lastCompletedDay.asStateFlow()

    /**
     * DEMO ONLY: ends the simulated day in progress and starts the next one. The finished day is
     * judged on the lowest available balance actually seen during it, so pressing this never
     * earns a star by itself. Only the underbanked persona has an account to judge.
     */
    fun completeDemoDay() {
        val current = profile.value?.takeIf { it.persona == DemoPersona.Underbanked } ?: return
        observeBalance(current)
        val before = streak.value
        val day = _streakDay.value
        val lowest = _todayLowest.value
        _streakHistory.update { it + (day to lowest) }
        _streakDay.value = day + 1
        _todayLowest.value = null
        // The new day starts with whatever the account holds now.
        observeBalance(current)
        persistStreaks()
        val after = StreakEngine.summarize(_streakHistory.value, _streakDay.value)
        _lastCompletedDay.value = CompletedDay(
            day = day,
            status = StreakEngine.statusOf(lowest),
            newBadges = StreakEngine.newlyUnlocked(before, after),
        )
    }

    fun dismissCompletedDay() {
        _lastCompletedDay.value = null
    }

    /**
     * Records the underbanked account's available balance for the day in progress. Called on
     * every change to the shared profile, so an overdraft counts the moment it happens, and a
     * later recovery that lifts the balance can't erase it. Projected (future) balances are
     * never observed, so a predicted overdraft that Prevent stops never counts.
     */
    private fun observeBalance(profile: DemoProfile?) {
        if (profile?.persona != DemoPersona.Underbanked) return
        val available = profile.availableBalance ?: return
        val lowest = StreakEngine.lowerOf(_todayLowest.value, available)
        if (lowest != _todayLowest.value) {
            _todayLowest.value = lowest
            savedState[KEY_STREAK_TODAY_LOW] = lowest?.cents
        }
    }

    private fun resetStreaks() {
        _streakHistory.value = DemoStreakHistory.lowestBalances
        _streakDay.value = DemoStreakHistory.START_DAY
        _todayLowest.value = null
        _lastCompletedDay.value = null
        persistStreaks()
    }

    // Saved as day -> lowest balance in cents, or "" for a day with no verified balance.
    private fun persistStreaks() {
        savedState[KEY_STREAK_DAY] = _streakDay.value
        savedState[KEY_STREAK_TODAY_LOW] = _todayLowest.value?.cents
        savedState[KEY_STREAK_HISTORY] = HashMap(_streakHistory.value.entries.associate { (d, m) -> d.toString() to (m?.cents?.toString() ?: "") })
    }

    private fun restoreStreakHistory(): Map<Int, Money?> {
        val saved = savedState.get<HashMap<String, String>>(KEY_STREAK_HISTORY) ?: return DemoStreakHistory.lowestBalances
        return saved.entries.mapNotNull { (d, v) -> d.toIntOrNull()?.let { it to v.toLongOrNull()?.let(::Money) } }.toMap()
    }

    init {
        viewModelScope.launch { profile.collect(::observeBalance) }
    }

    init {
        // A fee whose collection was interrupted (the app was killed) is finished, once.
        _recoveryRecords.value.values
            .filter { it.feeStatus == FeeCollectionStatus.Assessed }
            .forEach { viewModelScope.launch { collectFee(it.transactionId) } }
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
        const val KEY_SCENARIO = "demo_account_scenario"
        const val KEY_RECOVERY_RECORDS = "demo_recovery_records"
        const val KEY_REFERRALS = "demo_referrals"
        const val KEY_STREAK_DAY = "demo_streak_day"
        const val KEY_STREAK_HISTORY = "demo_streak_history"
        const val KEY_STREAK_TODAY_LOW = "demo_streak_today_low"
        const val FIELD_SEPARATOR = "\u001F"
    }
}

/** The result of one "Complete demo day". */
data class CompletedDay(val day: Int, val status: DayStatus, val newBadges: List<StreakBadge>) {
    val starAwarded: Boolean get() = status == DayStatus.OverdraftFree
}
