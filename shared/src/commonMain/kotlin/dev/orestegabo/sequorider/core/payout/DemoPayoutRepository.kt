package dev.orestegabo.sequorider.core.payout

import dev.orestegabo.sequorider.model.DemoRiderData
import dev.orestegabo.sequorider.model.PayoutHistoryItem
import dev.orestegabo.sequorider.model.PayoutSummary
import dev.orestegabo.sequorider.model.PayoutStatus

interface RiderPayoutRepository {
    suspend fun summary(): PayoutSummary
    suspend fun history(): List<PayoutHistoryItem>
}

class DemoPayoutRepository : RiderPayoutRepository {
    override suspend fun summary(): PayoutSummary = DemoRiderData.payoutSummary
    override suspend fun history(): List<PayoutHistoryItem> = DemoRiderData.payoutHistory
}

fun safePayoutFallbackSummary(): PayoutSummary =
    PayoutSummary(
        availableCfa = 0,
        pendingReviewCfa = 0,
        onHoldCfa = 0,
        lastPayoutCfa = 0,
        status = PayoutStatus.NotApplicable,
        payable = false,
        message = "Payout data is temporarily unavailable.",
    )
