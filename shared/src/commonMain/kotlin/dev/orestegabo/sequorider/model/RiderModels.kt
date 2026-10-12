package dev.orestegabo.sequorider.model

enum class RiderSection {
    Missions,
    ActiveRoute,
    History,
    Payout,
    Account,
}

enum class RiderFleetClass {
    InternalDriver,
    FreelanceMoto,
    FreelanceBicycle,
}

enum class RiderAvailability {
    Available,
    OnMission,
    Offline,
}

enum class MissionStatus {
    CREATED,
    OFFERED_TO_COURIER,
    ACCEPTED_BY_COURIER,
    PICKED_UP_FROM_SELLER,
    DEPOSITED_AT_RELAY,
    DELIVERED_TO_CUSTOMER,
    RELEASED_BY_RELAY,
    PROBLEM_REPORTED,
    CANCELLED,
}

enum class MissionServiceType {
    STANDARD,
    EXPRESS,
    PROGRAMMED,
    CLICK_COLLECT,
    RELAY,
}

enum class MissionDestinationType {
    CUSTOMER_ADDRESS,
    RELAY_POINT,
    SEQUO_CONSOLIDATION,
}

enum class ProofRequirement {
    SELLER_PICKUP_PROOF,
    CUSTOMER_DELIVERY_PROOF,
    DELIVERY_PIN,
    RELAY_DEPOSIT_PROOF,
    PROBLEM_REASON,
}

enum class CourierAvailabilityStatus {
    ACTIVE,
    PAUSED,
}

enum class PayoutStatus {
    NotApplicable,
    Estimated,
    PendingCompletion,
    Available,
    OnHold,
    Requested,
    Processing,
    Paid,
}

enum class PayoutEntryStatus {
    NOT_APPLICABLE,
    AVAILABLE,
    ON_HOLD,
    PROCESSING,
    PAID,
    CORRECTED,
}

data class RiderProfile(
    val riderId: String,
    val displayName: String,
    val fleetClass: RiderFleetClass,
    val availability: RiderAvailability,
    val serviceZone: String,
    val payoutProvider: String?,
    val kycApproved: Boolean,
)

data class RiderMission(
    val id: String,
    val code: String,
    val orderId: String,
    val merchantSubOrderId: String?,
    val courierId: String?,
    val serviceType: MissionServiceType,
    val destinationType: MissionDestinationType,
    val status: MissionStatus,
    val pickupStops: List<PickupStop>,
    val dropoffStop: DropoffStop,
    val routeStops: List<RouteStop>,
    val proofRequirements: List<ProofRequirement>,
    val pickupName: String,
    val pickupArea: String,
    val dropoffArea: String,
    val packageCount: Int,
    val distanceKm: String,
    val etaMinutes: Int,
    val expectedPayoutCfa: Int?,
    val customerDeliveryFeeCfa: Int,
    val shortfallCfa: Int,
    val slaLabel: String,
    val customerDeliveryNote: String? = null,
    val assignedAt: String? = null,
    val acceptedAt: String? = null,
    val pickupAt: String? = null,
    val relayDepositedAt: String? = null,
    val deliveredAt: String? = null,
    val problemMetadata: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class PickupStop(
    val id: String,
    val title: String,
    val area: String,
    val merchantSubOrderId: String?,
    val completed: Boolean,
)

data class DropoffStop(
    val id: String,
    val destinationType: MissionDestinationType,
    val title: String,
    val area: String,
    val completed: Boolean,
)

data class RouteStop(
    val sequence: Int,
    val title: String,
    val area: String,
    val detail: String,
    val completed: Boolean,
)

data class CourierAvailabilityState(
    val courierId: String,
    val status: CourierAvailabilityStatus,
    val paused: Boolean,
    val pausedReason: String?,
    val pausedBy: String?,
    val pausedAt: String?,
    val pausedUntil: String?,
    val updatedAt: String?,
)

data class RiderRoutePlan(
    val routeId: String,
    val routeType: String,
    val headline: String,
    val totalDistanceKm: String,
    val estimatedDurationMin: Int,
    val stops: List<RouteStop>,
)

data class PayoutSummary(
    val availableCfa: Int,
    val pendingReviewCfa: Int,
    val onHoldCfa: Int,
    val lastPayoutCfa: Int,
    val status: PayoutStatus,
    val payable: Boolean = true,
    val paidOrProcessingCfa: Int = lastPayoutCfa,
    val historyCount: Int = 0,
    val message: String? = null,
)

data class PayoutHistoryItem(
    val id: String,
    val missionId: String,
    val amountCfa: Int,
    val payableAmountCfa: Int,
    val status: PayoutEntryStatus,
    val payableToRider: Boolean,
    val description: String,
    val createdAt: String,
)

fun Int.formatCfa(): String = "$this FCFA"

object DemoRiderData {
    val profile = RiderProfile(
        riderId = "demo-rider",
        displayName = "Afi Mensah",
        fleetClass = RiderFleetClass.FreelanceMoto,
        availability = RiderAvailability.Available,
        serviceZone = "Lome Centre",
        payoutProvider = "Yas Togo",
        kycApproved = true,
    )

    val missions = listOf(
        RiderMission(
            id = "mission-1001",
            code = "SR-1001",
            orderId = "order-1001",
            merchantSubOrderId = "sub-order-1001",
            courierId = "demo-rider",
            serviceType = MissionServiceType.EXPRESS,
            destinationType = MissionDestinationType.CUSTOMER_ADDRESS,
            status = MissionStatus.OFFERED_TO_COURIER,
            pickupStops = listOf(PickupStop("pickup-1001", "Akif Burger", "Agoe", "sub-order-1001", completed = false)),
            dropoffStop = DropoffStop("dropoff-1001", MissionDestinationType.CUSTOMER_ADDRESS, "Customer handoff", "Tokoin", completed = false),
            routeStops = emptyList(),
            proofRequirements = listOf(ProofRequirement.SELLER_PICKUP_PROOF, ProofRequirement.CUSTOMER_DELIVERY_PROOF, ProofRequirement.DELIVERY_PIN),
            pickupName = "Akif Burger",
            pickupArea = "Agoe",
            dropoffArea = "Tokoin",
            packageCount = 1,
            distanceKm = "4.8 km",
            etaMinutes = 18,
            expectedPayoutCfa = 650,
            customerDeliveryFeeCfa = 400,
            shortfallCfa = 250,
            slaLabel = "Hot food priority",
            customerDeliveryNote = "Call from the gate before handoff.",
        ),
        RiderMission(
            id = "mission-1002",
            code = "SR-1002",
            orderId = "order-1002",
            merchantSubOrderId = "sub-order-1002",
            courierId = "demo-rider",
            serviceType = MissionServiceType.STANDARD,
            destinationType = MissionDestinationType.CUSTOMER_ADDRESS,
            status = MissionStatus.ACCEPTED_BY_COURIER,
            pickupStops = listOf(PickupStop("pickup-1002", "Marche de Mulhouse", "Assigame", "sub-order-1002", completed = false)),
            dropoffStop = DropoffStop("dropoff-1002", MissionDestinationType.CUSTOMER_ADDRESS, "Customer handoff", "Be", completed = false),
            routeStops = emptyList(),
            proofRequirements = listOf(ProofRequirement.SELLER_PICKUP_PROOF, ProofRequirement.CUSTOMER_DELIVERY_PROOF),
            pickupName = "Marche de Mulhouse",
            pickupArea = "Assigame",
            dropoffArea = "Be",
            packageCount = 3,
            distanceKm = "6.2 km",
            etaMinutes = 28,
            expectedPayoutCfa = 900,
            customerDeliveryFeeCfa = 700,
            shortfallCfa = 200,
            slaLabel = "Grouped pickup",
            customerDeliveryNote = "Customer prefers one consolidated package.",
        ),
        RiderMission(
            id = "mission-0998",
            code = "SR-0998",
            orderId = "order-0998",
            merchantSubOrderId = "sub-order-0998",
            courierId = "demo-rider",
            serviceType = MissionServiceType.STANDARD,
            destinationType = MissionDestinationType.CUSTOMER_ADDRESS,
            status = MissionStatus.DELIVERED_TO_CUSTOMER,
            pickupStops = listOf(PickupStop("pickup-0998", "Pharmacie Centrale", "Tokoin", "sub-order-0998", completed = true)),
            dropoffStop = DropoffStop("dropoff-0998", MissionDestinationType.CUSTOMER_ADDRESS, "Customer handoff", "Ablogame", completed = true),
            routeStops = emptyList(),
            proofRequirements = listOf(ProofRequirement.CUSTOMER_DELIVERY_PROOF),
            pickupName = "Pharmacie Centrale",
            pickupArea = "Tokoin",
            dropoffArea = "Ablogame",
            packageCount = 1,
            distanceKm = "3.1 km",
            etaMinutes = 0,
            expectedPayoutCfa = 500,
            customerDeliveryFeeCfa = 500,
            shortfallCfa = 0,
            slaLabel = "Completed",
            deliveredAt = "local-demo",
            createdAt = "local-demo",
            updatedAt = "local-demo",
        ),
        RiderMission(
            id = "mission-0997",
            code = "SR-0997",
            orderId = "order-0997",
            merchantSubOrderId = "sub-order-0997",
            courierId = "demo-rider",
            serviceType = MissionServiceType.EXPRESS,
            destinationType = MissionDestinationType.CUSTOMER_ADDRESS,
            status = MissionStatus.PROBLEM_REPORTED,
            pickupStops = listOf(PickupStop("pickup-0997", "Tacos Express", "Be", "sub-order-0997", completed = true)),
            dropoffStop = DropoffStop("dropoff-0997", MissionDestinationType.CUSTOMER_ADDRESS, "Customer handoff", "Hedzranawoe", completed = false),
            routeStops = emptyList(),
            proofRequirements = listOf(ProofRequirement.PROBLEM_REASON),
            pickupName = "Tacos Express",
            pickupArea = "Be",
            dropoffArea = "Hedzranawoe",
            packageCount = 1,
            distanceKm = "5.4 km",
            etaMinutes = 0,
            expectedPayoutCfa = 650,
            customerDeliveryFeeCfa = 400,
            shortfallCfa = 250,
            slaLabel = "Support review",
            pickupAt = "local-demo",
            problemMetadata = "Customer unavailable",
            createdAt = "local-demo",
            updatedAt = "local-demo",
        ),
    )

    val routePlan = RiderRoutePlan(
        routeId = "route-1002",
        routeType = "Cooperative route",
        headline = "3 pickups, one customer handoff",
        totalDistanceKm = "6.2 km",
        estimatedDurationMin = 28,
        stops = listOf(
            RouteStop(1, "Pickup lot A", "Assigame", "Seller package ready", completed = true),
            RouteStop(2, "Pickup lot B", "Assigame", "Verify sealed package", completed = false),
            RouteStop(3, "Dropoff", "Be", "Confirm PIN before handoff", completed = false),
        ),
    )

    val payoutSummary = PayoutSummary(
        availableCfa = 8_500,
        pendingReviewCfa = 1_800,
        onHoldCfa = 0,
        lastPayoutCfa = 12_000,
        status = PayoutStatus.Available,
        historyCount = 2,
        message = "Local payout preview.",
    )

    val payoutHistory = listOf(
        PayoutHistoryItem(
            id = "demo-payout-1",
            missionId = "mission-0998",
            amountCfa = 500,
            payableAmountCfa = 500,
            status = PayoutEntryStatus.AVAILABLE,
            payableToRider = true,
            description = "Completed customer delivery.",
            createdAt = "local-demo",
        ),
        PayoutHistoryItem(
            id = "demo-payout-2",
            missionId = "mission-0997",
            amountCfa = 650,
            payableAmountCfa = 0,
            status = PayoutEntryStatus.ON_HOLD,
            payableToRider = false,
            description = "Problem reported; support review required.",
            createdAt = "local-demo",
        ),
    )
}
