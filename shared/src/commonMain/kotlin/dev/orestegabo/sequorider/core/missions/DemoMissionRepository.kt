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
    missions: List<RiderMission> = DemoRiderData.missions,
) : RiderMissionRepository {
    private val missionState = missions.toMutableList()

    override suspend fun listMissions(courierId: String?, status: MissionStatus?): List<RiderMission> =
        missionState
            .filter { courierId == null || it.courierId == courierId }
            .filter { status == null || it.status == status }

    override suspend fun getMission(missionId: String): RiderMission? =
        missionState.firstOrNull { it.id == missionId }

    override suspend fun acceptMission(missionId: String): RiderMission =
        updateMission(missionId) { it.copy(status = MissionStatus.ACCEPTED_BY_COURIER) }

    override suspend fun markPickedUp(missionId: String, proofMetadata: String?, idempotencyKey: String?): RiderMission =
        updateMission(missionId) { it.copy(status = MissionStatus.PICKED_UP_FROM_SELLER) }

    override suspend fun markDelivered(missionId: String, proofMetadata: String?, deliveryPin: String?, idempotencyKey: String?): RiderMission =
        updateMission(missionId) { it.copy(status = MissionStatus.DELIVERED_TO_CUSTOMER) }

    override suspend fun depositAtRelay(missionId: String, proofMetadata: String?, idempotencyKey: String?): RiderMission =
        updateMission(missionId) { it.copy(status = MissionStatus.DEPOSITED_AT_RELAY) }

    override suspend fun reportProblem(missionId: String, reason: String, idempotencyKey: String?): RiderMission =
        updateMission(missionId) { it.copy(status = MissionStatus.PROBLEM_REPORTED, problemMetadata = reason) }

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
        missionState.firstOrNull { it.id == missionId } ?: error("Fake mission not found.")

    private fun updateMission(missionId: String, update: (RiderMission) -> RiderMission): RiderMission {
        val index = missionState.indexOfFirst { it.id == missionId }
        if (index < 0) error("Fake mission not found.")
        val updated = update(missionState[index])
        missionState[index] = updated
        return updated
    }
}

class DemoMissionRepository : FakeRiderMissionRepository()
