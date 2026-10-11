package dev.orestegabo.sequorider.core.missions

import dev.orestegabo.sequorider.model.DemoRiderData
import dev.orestegabo.sequorider.model.RiderMission
import dev.orestegabo.sequorider.model.RiderRoutePlan

interface RiderMissionRepository {
    fun missions(): List<RiderMission>
    fun activeRoute(): RiderRoutePlan
}

class DemoMissionRepository : RiderMissionRepository {
    override fun missions(): List<RiderMission> = DemoRiderData.missions
    override fun activeRoute(): RiderRoutePlan = DemoRiderData.routePlan
}
