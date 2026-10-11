package dev.orestegabo.sequorider.core.payout

import dev.orestegabo.sequorider.model.DemoRiderData
import dev.orestegabo.sequorider.model.PayoutSummary

interface RiderPayoutRepository {
    fun summary(): PayoutSummary
}

class DemoPayoutRepository : RiderPayoutRepository {
    override fun summary(): PayoutSummary = DemoRiderData.payoutSummary
}
