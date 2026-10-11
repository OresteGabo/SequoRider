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
    Offered,
    Accepted,
    EnRoutePickup,
    ArrivedPickup,
    PickedUp,
    EnRouteDropoff,
    Delivered,
    Failed,
    Returned,
}

enum class MissionServiceType {
    SubscriberStandard,
    ExpressFood,
    CooperativeMarket,
    Standard,
    Return,
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
    val serviceType: MissionServiceType,
    val status: MissionStatus,
    val pickupName: String,
    val pickupArea: String,
    val dropoffArea: String,
    val packageCount: Int,
    val distanceKm: String,
    val etaMinutes: Int,
    val expectedPayoutCfa: Int?,
    val slaLabel: String,
)

data class RouteStop(
    val sequence: Int,
    val title: String,
    val area: String,
    val detail: String,
    val completed: Boolean,
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
            serviceType = MissionServiceType.ExpressFood,
            status = MissionStatus.Offered,
            pickupName = "Akif Burger",
            pickupArea = "Agoe",
            dropoffArea = "Tokoin",
            packageCount = 1,
            distanceKm = "4.8 km",
            etaMinutes = 18,
            expectedPayoutCfa = 650,
            slaLabel = "Hot food priority",
        ),
        RiderMission(
            id = "mission-1002",
            code = "SR-1002",
            serviceType = MissionServiceType.CooperativeMarket,
            status = MissionStatus.Accepted,
            pickupName = "Marche de Mulhouse",
            pickupArea = "Assigame",
            dropoffArea = "Be",
            packageCount = 3,
            distanceKm = "6.2 km",
            etaMinutes = 28,
            expectedPayoutCfa = 900,
            slaLabel = "Grouped pickup",
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
    )
}
