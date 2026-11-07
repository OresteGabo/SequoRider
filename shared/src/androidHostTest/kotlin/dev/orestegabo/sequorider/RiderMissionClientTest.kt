package dev.orestegabo.sequorider

import com.sun.net.httpserver.HttpServer
import dev.orestegabo.sequorider.core.missions.NetworkRiderMissionRepository
import dev.orestegabo.sequorider.core.missions.RiderMissionApiClient
import dev.orestegabo.sequorider.core.missions.RiderMissionApiException
import dev.orestegabo.sequorider.model.MissionStatus
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class RiderMissionClientTest {
    private val mission = """
        {"id":"mission-1","deliveryCode":"delivery-1","orderId":"order-1","courierId":"courier-1",
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
