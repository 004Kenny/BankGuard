package com.example.bankguard.ui.util

import androidx.annotation.DrawableRes
import com.example.bankguard.R
import com.example.bankguard.model.ScheduledKind
import com.example.bankguard.model.Transaction
import com.example.bankguard.model.TransactionCategory
import com.example.bankguard.model.TransactionStatus

@DrawableRes
fun ScheduledKind.iconRes(): Int = when (this) {
    ScheduledKind.Income -> R.drawable.ic_payments
    ScheduledKind.Bill -> R.drawable.ic_receipt_long
    ScheduledKind.Rent -> R.drawable.ic_account_balance
}

@DrawableRes
fun Transaction.iconRes(): Int = when {
    status == TransactionStatus.Pending -> R.drawable.ic_schedule
    category == TransactionCategory.Income -> R.drawable.ic_payments
    category == TransactionCategory.Subscriptions -> R.drawable.ic_event
    else -> R.drawable.ic_receipt_long
}
