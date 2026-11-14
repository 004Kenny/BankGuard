package com.example.bankguard.ui.util

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.bankguard.R
import com.example.bankguard.model.AltServiceType
import com.example.bankguard.model.BankFeeType
import com.example.bankguard.model.RecoveryStage
import com.example.bankguard.model.WaiverReason
import com.example.bankguard.ui.components.StatusTone

/** Plain-language name, explanation and prevention tip for each kind of bank fee. */
data class FeeText(
    @param:StringRes val nameRes: Int,
    @param:StringRes val whatRes: Int,
    @param:StringRes val avoidRes: Int,
    @param:DrawableRes val iconRes: Int,
)

fun BankFeeType.text(): FeeText = when (this) {
    BankFeeType.Overdraft -> FeeText(R.string.fee_overdraft_name, R.string.fee_overdraft_what, R.string.fee_overdraft_avoid, R.drawable.ic_warning)
    BankFeeType.MonthlyMaintenance -> FeeText(R.string.fee_maintenance_name, R.string.fee_maintenance_what, R.string.fee_maintenance_avoid, R.drawable.ic_account_balance)
    BankFeeType.OutOfNetworkAtm -> FeeText(R.string.fee_atm_name, R.string.fee_atm_what, R.string.fee_atm_avoid, R.drawable.ic_payments)
}

@StringRes
fun AltServiceType.explanationRes(): Int = when (this) {
    AltServiceType.CheckCashing -> R.string.rcu_check_cashing
    AltServiceType.PrepaidCardFees -> R.string.rcu_prepaid
    AltServiceType.MoneyOrders -> R.string.rcu_money_orders
}

@StringRes
fun WaiverReason.labelRes(): Int = when (this) {
    WaiverReason.RarelyCharged -> R.string.le_reason_rarely
    WaiverReason.LongTimeCustomer -> R.string.le_reason_customer
    WaiverReason.Hardship -> R.string.le_reason_hardship
    WaiverReason.DelayedDeposit -> R.string.le_reason_deposit
}

data class StageBadge(@param:StringRes val textRes: Int, val tone: StatusTone)

fun RecoveryStage.badge(): StageBadge = when (this) {
    RecoveryStage.Found -> StageBadge(R.string.stage_found, StatusTone.Neutral)
    RecoveryStage.Drafted -> StageBadge(R.string.stage_drafted, StatusTone.Info)
    RecoveryStage.Sent -> StageBadge(R.string.stage_sent, StatusTone.Warning)
    RecoveryStage.Refunded -> StageBadge(R.string.stage_refunded, StatusTone.Success)
    RecoveryStage.NotRefunded -> StageBadge(R.string.stage_not_refunded, StatusTone.Critical)
}
