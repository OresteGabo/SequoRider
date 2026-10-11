package dev.orestegabo.sequorider.core.missions

import dev.orestegabo.sequorider.model.CourierAvailabilityState
import dev.orestegabo.sequorider.model.CourierAvailabilityStatus
import dev.orestegabo.sequorider.model.DropoffStop
import dev.orestegabo.sequorider.model.MissionDestinationType
import dev.orestegabo.sequorider.model.MissionServiceType
import dev.orestegabo.sequorider.model.MissionStatus
import dev.orestegabo.sequorider.model.PickupStop
import dev.orestegabo.sequorider.model.ProofRequirement
import dev.orestegabo.sequorider.model.RiderMission
import dev.orestegabo.sequorider.model.RouteStop
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class RiderMissionApiClient(
    private val baseUrl: String = ProductionBaseUrl,
    private val accessTokenProvider: () -> String?,
    private val httpClient: HttpClient = createMissionHttpClient(),
) {
    suspend fun listMissions(courierId: String? = null, status: MissionStatus? = null): List<RiderMission> =
        httpClient.get(apiUrl("delivery/missions")) {
            bearer()
            courierId?.let { parameter("courierId", it) }
            status?.let { parameter("status", it.name) }
        }.body<List<DeliveryMissionDto>>().map { it.toModel() }

    suspend fun getMission(missionId: String): RiderMission =
        httpClient.get(apiUrl("delivery/missions/$missionId")) {
            bearer()
        }.body<DeliveryMissionDto>().toModel()

    suspend fun acceptMission(missionId: String): RiderMission =
        httpClient.post(apiUrl("delivery/missions/$missionId/accept")) {
            bearer()
            accept(ContentType.Application.Json)
        }.body<DeliveryMissionDto>().toModel()

    suspend fun markPickedUp(missionId: String, proofMetadata: String?, idempotencyKey: String?): RiderMission =
        postMission(missionId, "pickup", ProofRequest(proofMetadata = proofMetadata, idempotencyKey = idempotencyKey))

    suspend fun markDelivered(missionId: String, proofMetadata: String?, deliveryPin: String?, idempotencyKey: String?): RiderMission =
        postMission(missionId, "deliver", ProofRequest(proofMetadata = proofMetadata, deliveryPin = deliveryPin, idempotencyKey = idempotencyKey))

    suspend fun depositAtRelay(missionId: String, proofMetadata: String?, idempotencyKey: String?): RiderMission =
        postMission(missionId, "relay-deposit", ProofRequest(proofMetadata = proofMetadata, idempotencyKey = idempotencyKey))

    suspend fun reportProblem(missionId: String, reason: String, idempotencyKey: String?): RiderMission =
        postMission(missionId, "problem", ProblemRequest(reason = reason, idempotencyKey = idempotencyKey))

    suspend fun courierAvailability(courierId: String): CourierAvailabilityState =
        httpClient.get(apiUrl("delivery/missions/couriers/$courierId/availability")) {
            bearer()
        }.body<CourierAvailabilityDto>().toModel()

    fun close() {
        httpClient.close()
    }

    private suspend inline fun <reified T : Any> postMission(missionId: String, action: String, body: T?): RiderMission =
        httpClient.post(apiUrl("delivery/missions/$missionId/$action")) {
            bearer()
            accept(ContentType.Application.Json)
            if (body != null) setBody(body)
        }.body<DeliveryMissionDto>().toModel()

    private fun io.ktor.client.request.HttpRequestBuilder.bearer() {
        accessTokenProvider()?.takeIf { it.isNotBlank() }?.let { bearerAuth(it) }
    }

    private fun apiUrl(path: String): String =
        "${baseUrl.trimEnd('/')}/api/${path.trimStart('/')}"
}

class NetworkRiderMissionRepository(
    private val apiClient: RiderMissionApiClient,
) : RiderMissionRepository {
    override suspend fun listMissions(courierId: String?, status: MissionStatus?): List<RiderMission> =
        apiClient.listMissions(courierId, status)

    override suspend fun getMission(missionId: String): RiderMission =
        apiClient.getMission(missionId)

    override suspend fun acceptMission(missionId: String): RiderMission =
        apiClient.acceptMission(missionId)

    override suspend fun markPickedUp(missionId: String, proofMetadata: String?, idempotencyKey: String?): RiderMission =
        apiClient.markPickedUp(missionId, proofMetadata, idempotencyKey)

    override suspend fun markDelivered(missionId: String, proofMetadata: String?, deliveryPin: String?, idempotencyKey: String?): RiderMission =
        apiClient.markDelivered(missionId, proofMetadata, deliveryPin, idempotencyKey)

    override suspend fun depositAtRelay(missionId: String, proofMetadata: String?, idempotencyKey: String?): RiderMission =
        apiClient.depositAtRelay(missionId, proofMetadata, idempotencyKey)

    override suspend fun reportProblem(missionId: String, reason: String, idempotencyKey: String?): RiderMission =
        apiClient.reportProblem(missionId, reason, idempotencyKey)

    override suspend fun courierAvailability(courierId: String): CourierAvailabilityState =
        apiClient.courierAvailability(courierId)

    override fun activeRoute() =
        dev.orestegabo.sequorider.model.DemoRiderData.routePlan
}

@Serializable
private data class DeliveryMissionDto(
    val id: String,
    val deliveryCode: String,
    val orderId: String,
    val merchantSubOrderId: String? = null,
    val courierId: String? = null,
    val deliveryMode: MissionServiceType,
    val status: MissionStatus,
    val destinationType: MissionDestinationType,
    val assignedAt: String? = null,
    val acceptedAt: String? = null,
    val pickupAt: String? = null,
    val deliveredAt: String? = null,
    val relayDepositedAt: String? = null,
    val customerDeliveryFeeCfa: Int = 0,
    val courierFeeCfa: Int = 0,
    val shortfallCfa: Int = 0,
    val problemMetadata: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    fun toModel(): RiderMission {
        val pickup = PickupStop(
            id = merchantSubOrderId ?: id,
            title = merchantSubOrderId ?: deliveryCode,
            area = "",
            merchantSubOrderId = merchantSubOrderId,
            completed = pickupAt != null,
        )
        val dropoff = DropoffStop(
            id = "$id-dropoff",
            destinationType = destinationType,
            title = destinationType.name,
            area = "",
            completed = deliveredAt != null,
        )
        return RiderMission(
            id = id,
            code = deliveryCode,
            orderId = orderId,
            merchantSubOrderId = merchantSubOrderId,
            courierId = courierId,
            serviceType = deliveryMode,
            destinationType = destinationType,
            status = status,
            pickupStops = listOf(pickup),
            dropoffStop = dropoff,
            routeStops = listOf(
                RouteStop(1, pickup.title, pickup.area, deliveryMode.name, pickup.completed),
                RouteStop(2, dropoff.title, dropoff.area, destinationType.name, dropoff.completed),
            ),
            proofRequirements = proofRequirements(),
            pickupName = pickup.title,
            pickupArea = pickup.area,
            dropoffArea = dropoff.title,
            packageCount = 1,
            distanceKm = "",
            etaMinutes = 0,
            expectedPayoutCfa = courierFeeCfa,
            customerDeliveryFeeCfa = customerDeliveryFeeCfa,
            shortfallCfa = shortfallCfa,
            slaLabel = status.name,
            customerDeliveryNote = null,
            assignedAt = assignedAt,
            acceptedAt = acceptedAt,
            pickupAt = pickupAt,
            relayDepositedAt = relayDepositedAt,
            deliveredAt = deliveredAt,
            problemMetadata = problemMetadata,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun proofRequirements(): List<ProofRequirement> =
        when (status) {
            MissionStatus.OFFERED_TO_COURIER,
            MissionStatus.CREATED,
            -> emptyList()
            MissionStatus.ACCEPTED_BY_COURIER -> listOf(ProofRequirement.SELLER_PICKUP_PROOF)
            MissionStatus.PICKED_UP_FROM_SELLER -> if (destinationType == MissionDestinationType.RELAY_POINT) {
                listOf(ProofRequirement.RELAY_DEPOSIT_PROOF)
            } else {
                listOf(ProofRequirement.CUSTOMER_DELIVERY_PROOF, ProofRequirement.DELIVERY_PIN)
            }
            MissionStatus.DEPOSITED_AT_RELAY,
            MissionStatus.DELIVERED_TO_CUSTOMER,
            MissionStatus.RELEASED_BY_RELAY,
            MissionStatus.PROBLEM_REPORTED,
            MissionStatus.CANCELLED,
            -> emptyList()
        }
}

@Serializable
private data class CourierAvailabilityDto(
    val courierId: String,
    val status: CourierAvailabilityStatus,
    val paused: Boolean,
    val pausedReason: String? = null,
    val pausedBy: String? = null,
    val pausedAt: String? = null,
    val pausedUntil: String? = null,
    val updatedAt: String? = null,
) {
    fun toModel(): CourierAvailabilityState =
        CourierAvailabilityState(courierId, status, paused, pausedReason, pausedBy, pausedAt, pausedUntil, updatedAt)
}

@Serializable
private data class ProofRequest(
    val proofMetadata: String? = null,
    val deliveryPin: String? = null,
    val idempotencyKey: String? = null,
)

@Serializable
private data class ProblemRequest(
    val reason: String,
    val idempotencyKey: String? = null,
)

private fun createMissionHttpClient(json: Json = Json { ignoreUnknownKeys = true; isLenient = true }): HttpClient =
    HttpClient {
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
        install(ContentNegotiation) { json(json) }
        HttpResponseValidator {
            handleResponseExceptionWithRequest { cause, _ ->
                when (cause) {
                    is ClientRequestException -> throw RiderMissionApiException(cause.response.status.value, cause.response.bodyAsText(), cause)
                    is ServerResponseException -> throw RiderMissionApiException(cause.response.status.value, cause.response.bodyAsText(), cause)
                }
            }
        }
    }

class RiderMissionApiException(
    val statusCode: Int,
    val responseBody: String?,
    cause: Throwable,
) : Exception("Rider mission API request failed.", cause)

private const val ProductionBaseUrl = "https://api.sequoservice.com"
