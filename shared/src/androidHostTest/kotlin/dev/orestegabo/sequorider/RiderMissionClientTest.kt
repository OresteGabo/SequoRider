package dev.orestegabo.sequorider

import com.sun.net.httpserver.HttpServer
import dev.orestegabo.sequorider.core.missions.NetworkRiderMissionRepository
import dev.orestegabo.sequorider.core.missions.RiderMissionApiClient
import dev.orestegabo.sequorider.core.missions.RiderMissionApiException
import dev.orestegabo.sequorider.model.MissionStatus
import dev.orestegabo.sequorider.model.ProofRequirement
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class RiderMissionClientTest {
    private val mission = """
        {"id":"mission-1","publicReference":"DZ9-X8W-7P3","orderReference":"C12-345-6E6","merchantSubOrderReference":"MAB-CDE-FJZ","deliveryCode":"delivery-1","orderId":"order-1","courierId":"courier-1",
         "merchantSubOrderId":"sub-1","deliveryMode":"STANDARD","status":"ACCEPTED_BY_COURIER",
         "destinationType":"CUSTOMER_ADDRESS","packageCount":2,"pickupName":"Fixture merchant",
         "dropoffArea":"Tokoin, Lomé","homeDestination":{"recipientName":"Fixture customer",
         "phoneNumber":"+22890000000","city":"Lomé","area":"Tokoin","streetHint":"Fixture destination"}}
    """.trimIndent()

    @Test
    fun readsActualRouteAndHistoryAndPropagatesOfflineFailures() = runBlocking<Unit> {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var unavailable = false
        server.createContext("/api/rider/missions") { exchange ->
            assertEquals("Bearer rider-fixture", exchange.requestHeaders.getFirst("Authorization"))
            val body = if (unavailable) """{"message":"Unavailable"}""" else
                "[$mission,${mission.replace("mission-1", "mission-done").replace("ACCEPTED_BY_COURIER", "DELIVERED_TO_CUSTOMER")}]"
            val bytes = body.toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(if (unavailable) 503 else 200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val client = RiderMissionApiClient("http://127.0.0.1:${server.address.port}", { "rider-fixture" })
        try {
            val records = client.listMissions()
            val active = records.first()
            assertEquals(2, active.packageCount)
            assertEquals("DZ9-X8W-7P3", active.displayReference)
            assertEquals("C12-345-6E6", active.displayOrderReference)
            assertEquals("mission-1", active.id)
            assertEquals("delivery-1", active.code)
            assertEquals("", active.slaLabel, "A status is not an ETA when no configured timing exists")
            assertEquals("Fixture merchant", active.pickupName)
            assertTrue(active.customerDeliveryNote.orEmpty().contains("Fixture destination"))
            assertTrue(records.any { it.status == MissionStatus.DELIVERED_TO_CUSTOMER })
            assertTrue(NetworkRiderMissionRepository(client).activeRoute().stops.isEmpty())
            unavailable = true
            assertEquals(503, assertFailsWith<RiderMissionApiException> { client.listMissions() }.statusCode)
        } finally {
            client.close()
            server.stop(0)
        }
    }

    @Test
    fun relayReadAndDepositUsePersistedDestinationAndTypedSlot() = runBlocking<Unit> {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val relay = mission.replace("CUSTOMER_ADDRESS", "RELAY_POINT").replace("ACCEPTED_BY_COURIER", "PICKED_UP_FROM_SELLER")
            .replace("\"homeDestination\":", "\"legacyHiddenContact\":").dropLast(1) +
            """, "relayDestination":{"id":"relay-A","name":"Relais A","city":"Lomé","landmark":"Approved staging landmark"}}"""
        server.createContext("/api/rider/missions/mission-1") { exchange ->
            assertEquals("Bearer rider-fixture", exchange.requestHeaders.getFirst("Authorization"))
            val deposit = exchange.requestURI.path.endsWith("/relay-deposit")
            if (deposit) {
                val body = Json.parseToJsonElement(exchange.requestBody.readBytes().decodeToString())
                assertEquals(Json.parseToJsonElement("""{"relayPointId":"relay-A","lockerCode":"SLOT-1","proofMetadata":"Custody accepted","idempotencyKey":"deposit-1"}"""), body)
            }
            val bytes = (if (deposit) relay.replace("PICKED_UP_FROM_SELLER", "DEPOSITED_AT_RELAY") else relay).toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val client = RiderMissionApiClient("http://127.0.0.1:${server.address.port}", { "rider-fixture" })
        try {
            val actual = client.getMission("mission-1")
            assertEquals("relay-A", actual.relayPointId)
            assertEquals("Relais A", actual.dropoffStop.title)
            assertTrue(actual.dropoffStop.area.contains("Approved staging landmark"))
            assertEquals(null, actual.customerDeliveryNote)
            val deposited = client.depositAtRelay("mission-1", "Custody accepted", "deposit-1", actual.relayPointId, "SLOT-1")
            assertEquals(MissionStatus.DEPOSITED_AT_RELAY, deposited.status)
        } finally { client.close(); server.stop(0) }
    }

    @Test
    fun readsTheConfiguredModeEtaAndActualWorkforce() = runBlocking<Unit> {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/rider/missions/mission-1") { exchange ->
            val response = mission.dropLast(1) + """, "deliveryOption":{"id":"DEFERRED","etaMinMinutes":1440,"etaMaxMinutes":2880},"assignmentWorkforceType":"Freelancer"}"""
            val bytes = response.toByteArray(); exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val client = RiderMissionApiClient("http://127.0.0.1:${server.address.port}", { "rider-fixture" })
        try {
            val current = client.getMission("mission-1")
            assertEquals("DEFERRED", current.deliveryOptionId)
            assertEquals("1440–2880 min", current.slaLabel)
            assertEquals("Freelancer", current.assignmentWorkforceType)
        } finally {client.close();server.stop(0)}
    }

    @Test
    fun consolidationCollectionAndFinalDispatchKeepTheirDistinctCustodyDestinations() = runBlocking<Unit> {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val intake = """
            {"id":"collection-1","deliveryCode":"collection-code","orderId":"order-1","courierId":"courier-1",
             "merchantSubOrderId":"sub-1","deliveryMode":"STANDARD","status":"PICKED_UP_FROM_SELLER",
             "destinationType":"SEQUO_CONSOLIDATION","packageCount":2,"pickupName":"Fixture merchant",
             "pickupSource":"MERCHANT","dropoffName":"SEQUO consolidation","pickupAt":"2026-11-08T10:00:00Z"}
        """.trimIndent()
        val finalDispatch = """
            {"id":"final-1","deliveryCode":"final-code","orderId":"order-1","courierId":"courier-1",
             "deliveryMode":"STANDARD","status":"PICKED_UP_FROM_SELLER","destinationType":"CUSTOMER_ADDRESS",
             "packageCount":1,"pickupName":"SEQUO consolidation","pickupSource":"SEQUO_CONSOLIDATION",
             "homeDestination":{"recipientName":"Fixture customer","phoneNumber":"+22890000000",
             "city":"Lomé","area":"Tokoin","streetHint":"Fixture destination"}}
        """.trimIndent()
        var received = false
        server.createContext("/api/rider/missions") { exchange ->
            assertEquals("Bearer rider-fixture", exchange.requestHeaders.getFirst("Authorization"))
            val collection = if (received) intake.replace("PICKED_UP_FROM_SELLER", "HANDED_TO_SEQUO") else intake
            val bytes = "[$collection,$finalDispatch]".toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val client = RiderMissionApiClient("http://127.0.0.1:${server.address.port}", { "rider-fixture" })
        try {
            val (collection, final) = client.listMissions()
            assertEquals("MERCHANT", collection.pickupSource)
            assertEquals("Fixture merchant", collection.pickupName)
            assertEquals("SEQUO consolidation", collection.dropoffStop.title)
            assertEquals(null, collection.customerDeliveryNote)
            assertTrue(collection.proofRequirements.isEmpty())
            assertTrue(!collection.dropoffStop.completed)
            assertEquals("SEQUO_CONSOLIDATION", final.pickupSource)
            assertEquals("SEQUO consolidation", final.pickupName)
            assertEquals(1, final.packageCount)
            assertEquals("Fixture customer", final.dropoffStop.title)
            assertTrue(final.customerDeliveryNote.orEmpty().contains("Fixture destination"))
            assertTrue(ProofRequirement.DELIVERY_PIN in final.proofRequirements)
            received = true
            val recordedReceipt = client.listMissions().first()
            assertEquals(MissionStatus.HANDED_TO_SEQUO, recordedReceipt.status)
            assertTrue(recordedReceipt.dropoffStop.completed)
            assertTrue(recordedReceipt.proofRequirements.isEmpty())
        } finally { client.close(); server.stop(0) }
    }

    @Test
    fun proofRequestUsesJsonAndStableRetryKeyAndDoesNotMaskRejection() = runBlocking<Unit> {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var calls = 0
        server.createContext("/api/rider/missions/mission-1/pickup/complete") { exchange ->
            calls++
            assertTrue(exchange.requestHeaders.getFirst("Content-Type").startsWith("application/json"))
            val body = exchange.requestBody.readBytes().decodeToString()
            assertEquals(Json.parseToJsonElement("""{"proofMetadata":"Receipt fixture","idempotencyKey":"pickup-1"}"""),
                Json.parseToJsonElement(body))
            val bytes = """{"code":"invalid_mission_transition","message":"Package is already delivered."}""".toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(400, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val client = RiderMissionApiClient("http://127.0.0.1:${server.address.port}", { "rider-fixture" })
        try {
            val error = assertFailsWith<RiderMissionApiException> {
                client.markPickedUp("mission-1", "Receipt fixture", "pickup-1")
            }
            assertEquals(400, error.statusCode)
            assertEquals(1, calls)
        } finally {
            client.close()
            server.stop(0)
        }
    }
}
