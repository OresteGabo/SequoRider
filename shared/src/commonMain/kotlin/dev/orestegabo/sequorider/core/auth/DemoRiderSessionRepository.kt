package dev.orestegabo.sequorider.core.auth

import dev.orestegabo.sequorider.model.DemoRiderData
import dev.orestegabo.sequorider.model.RiderProfile

interface RiderSessionRepository {
    fun currentProfile(): RiderProfile
}

class DemoRiderSessionRepository : RiderSessionRepository {
    override fun currentProfile(): RiderProfile = DemoRiderData.profile
}
