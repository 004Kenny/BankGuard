package com.example.bankguard.domain.cancellation

import com.example.bankguard.data.demo.DemoProfile
import com.example.bankguard.model.CancellationStatus

/**
 * The profile as it looks after the user's cancellation requests.
 * Only APPROVED cancellations change anything: the charge is removed, so the available
 * balance and forecast go up. Processing or declined requests change nothing.
 */
fun DemoProfile.withCancellations(statuses: Map<String, CancellationStatus>): DemoProfile {
    val approved = statuses.filterValues { it is CancellationStatus.Approved }.keys
    if (approved.isEmpty()) return this
    return copy(transactions = transactions.filterNot { it.id in approved })
}
