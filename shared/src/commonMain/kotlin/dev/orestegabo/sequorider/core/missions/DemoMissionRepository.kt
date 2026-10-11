package dev.orestegabo.sequorider.core.missions

import dev.orestegabo.sequorider.model.DemoRiderData
import dev.orestegabo.sequorider.model.CourierAvailabilityState
import dev.orestegabo.sequorider.model.CourierAvailabilityStatus
import dev.orestegabo.sequorider.model.MissionStatus
import dev.orestegabo.sequorider.model.RiderMission
import dev.orestegabo.sequorider.model.RiderRoutePlan

interface RiderMissionRepository {
    suspend fun listMissions(courierId: String? = null, status: MissionStatus? = null): List<RiderMission>
    suspend fun getMission(missionId: String): RiderMission?
    suspend fun acceptMission(missionId: String): RiderMission
    suspend fun markPickedUp(missionId: String, proofMetadata: String?, idempotencyKey: String?): RiderMission
    suspend fun markDelivered(missionId: String, proofMetadata: String?, deliveryPin: String?, idempotencyKey: String?): RiderMission
    suspend fun depositAtRelay(missionId: String, proofMetadata: String?, idempotencyKey: String?): RiderMission
    suspend fun reportProblem(missionId: String, reason: String, idempotencyKey: String?): RiderMission
    suspend fun courierAvailability(courierId: String): CourierAvailabilityState?
    fun activeRoute(): RiderRoutePlan
}

open class FakeRiderMissionRepository(
    private val missions: List<RiderMission> = DemoRiderData.missions,
) : RiderMissionRepository {
    override suspend fun listMissions(courierId: String?, status: MissionStatus?): List<RiderMission> =
        missions
            .filter { courierId == null || it.courierId == courierId }
            .filter { status == null || it.status == status }

    override suspend fun getMission(missionId: String): RiderMission? =
        missions.firstOrNull { it.id == missionId }

    override suspend fun acceptMission(missionId: String): RiderMission =
        requireMission(missionId)

    override suspend fun markPickedUp(missionId: String, proofMetadata: String?, idempotencyKey: String?): RiderMission =
        requireMission(missionId)

    override suspend fun markDelivered(missionId: String, proofMetadata: String?, deliveryPin: String?, idempotencyKey: String?): RiderMission =
        requireMission(missionId)

    override suspend fun depositAtRelay(missionId: String, proofMetadata: String?, idempotencyKey: String?): RiderMission =
        requireMission(missionId)

    override suspend fun reportProblem(missionId: String, reason: String, idempotencyKey: String?): RiderMission =
        requireMission(missionId)

    override suspend fun courierAvailability(courierId: String): CourierAvailabilityState =
        CourierAvailabilityState(
            courierId = courierId,
            status = CourierAvailabilityStatus.ACTIVE,
            paused = false,
            pausedReason = null,
            pausedBy = null,
            pausedAt = null,
            pausedUntil = null,
            updatedAt = null,
        )

    override fun activeRoute(): RiderRoutePlan = DemoRiderData.routePlan

    private fun requireMission(missionId: String): RiderMission =
        missions.firstOrNull { it.id == missionId } ?: error("Fake mission not found.")
}

class DemoMissionRepository : FakeRiderMissionRepository()
