package dev.orestegabo.sequorider

import com.sun.net.httpserver.HttpServer
import dev.orestegabo.sequorider.core.network.SequoApiException
import dev.orestegabo.sequorider.core.payout.NetworkRiderPayoutRepository
import dev.orestegabo.sequorider.core.payout.RiderPayoutApiClient
import dev.orestegabo.sequorider.model.PayoutEntryStatus
import dev.orestegabo.sequorider.feature.settings.RiderText
import dev.orestegabo.sequorider.ui.payout.payoutReference
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RiderPayoutClientTest {
    private val summary = """
        {"riderId":"courier-fixture","workforceType":"SEQUO_SALARIED","payable":false,
         "availableBalanceCfa":150,"onHoldCfa":100,"paidOrProcessingCfa":0,"historyCount":5,
         "message":"Recorded independent earnings; transfers are not integrated.",
         "earnedTotalCfa":450,"settledMissionCount":1,"processingCfa":0,"paidCfa":0,
         "transferStatus":"NOT_INTEGRATED","unverifiedTransferCfa":200,"reviewRequiredCfa":200}
    """.trimIndent()

    @Test
    fun readsAuthoritativeRecordedPayAndHistoricalWorkforceWithoutTransferAliases() = runBlocking<Unit> {
        val history = """
            [{"id":"earning","missionReference":"DZ9-X8W-7P3","missionId":"mission-1","amountCfa":500,"payableAmountCfa":500,
              "status":"AVAILABLE","payableToRider":true,"description":"Recorded earning",
              "createdAt":"2026-11-08T10:00:00Z","workforceType":"Freelancer","account":"RiderPayable","direction":"Credit"},
             {"id":"hold","missionId":"mission-1","amountCfa":100,"payableAmountCfa":100,
              "status":"ON_HOLD","payableToRider":true,"description":"Hold",
              "createdAt":"2026-11-08T10:01:00Z","workforceType":"Freelancer","account":"RiderHold","direction":"Debit"},
             {"id":"correction","missionId":"mission-1","amountCfa":50,"payableAmountCfa":50,
              "status":"CORRECTED","payableToRider":true,"description":"Correction",
              "createdAt":"2026-11-08T10:02:00Z","workforceType":"Freelancer","account":"RiderPayable","direction":"Debit"},
             {"id":"unverified","publicReference":"W12-345-65Q","missionId":"payout-request-1","amountCfa":200,"payableAmountCfa":0,
              "status":"REVIEW_REQUIRED","payableToRider":true,"description":"Unverified transfer entry",
              "createdAt":"2026-11-08T10:03:00Z","workforceType":null,"account":"Payout","direction":"Debit"},
             {"id":"shortfall","missionId":"mission-1","amountCfa":100,"payableAmountCfa":0,
              "status":"NOT_APPLICABLE","payableToRider":false,"description":"Company delivery shortfall",
              "createdAt":"2026-11-08T10:04:00Z","workforceType":"Freelancer","account":"CompanyDeliveryShortfall","direction":"Debit"}]
        """.trimIndent()
        fixture(summary, history) { repository ->
            val current = repository.summary()
            assertEquals(false, current.payable, "Current salary status must not erase the separately recorded historical independent pay.")
            assertEquals("SEQUO_SALARIED", current.workforceType)
            assertEquals(450, current.earnedTotalCfa)
            assertEquals(1, current.settledMissionCount)
            assertEquals(150, current.availableCfa)
            assertEquals(100, current.onHoldCfa)
            assertEquals(200, current.unverifiedTransferCfa)
            assertEquals(200, current.reviewRequiredCfa)
            assertEquals("NOT_INTEGRATED", current.transferStatus)
            assertEquals(0, current.paidCfa)
            assertEquals(0, current.processingCfa)
            assertNull(current.lastPayoutCfa)
            val records = repository.history()
            assertEquals("DZ9-X8W-7P3", records.first().missionReference)
            assertEquals("mission-1", records.first().missionId)
            assertNull(records[1].missionReference, "Older ledger rows remain supported.")
            assertEquals(RiderText.MissionCode to "DZ9-X8W-7P3", payoutReference(records.first()))
            assertEquals(RiderText.SourceReference to "W12-345-65Q", payoutReference(records[3]))
            assertEquals("payout-request-1", records[3].missionId, "Internal source identity remains intact.")
            assertEquals(RiderText.SourceReference to null, payoutReference(records[1]), "Missing legacy aliases must not display a raw source ID.")
            assertEquals(listOf(500, 100, 50, 0, 0), records.map { it.payableAmountCfa })
            assertEquals(listOf(500, -100, -50, 0, 0), records.map { it.recordedPayImpactCfa })
            assertEquals(PayoutEntryStatus.REVIEW_REQUIRED, records[3].status)
            assertNull(records[3].workforceType)
            assertEquals("Freelancer", records.first().workforceType)
            assertEquals("Debit", records[2].direction)
            assertEquals(PayoutEntryStatus.NOT_APPLICABLE, records.last().status)
            assertEquals(false, records.last().payableToRider)
        }
    }

    @Test
    fun olderSummaryDoesNotInventEarningsOrLastTransferFromAggregate() = runBlocking<Unit> {
        val legacy = """
            {"riderId":"courier-fixture","workforceType":"INDEPENDENT","payable":true,
             "availableBalanceCfa":700,"onHoldCfa":100,"paidOrProcessingCfa":300,
             "historyCount":0,"message":"Legacy summary"}
        """.trimIndent()
        fixture(legacy, "[]") { repository ->
            val current = repository.summary()
            assertEquals(700, current.availableCfa)
            assertEquals(300, current.paidOrProcessingCfa)
            assertNull(current.lastPayoutCfa)
            assertNull(current.earnedTotalCfa)
            assertNull(current.settledMissionCount)
            assertNull(current.paidCfa)
            assertNull(current.processingCfa)
            assertNull(current.transferStatus)
            val unclassified = dev.orestegabo.sequorider.model.PayoutHistoryItem("legacy", "old-mission", 400, 400, PayoutEntryStatus.AVAILABLE, true, "Legacy row without direction", "2026-11-08T10:00:00Z")
            assertNull(unclassified.recordedPayImpactCfa)
        }
    }

    @Test
    fun refreshDenialsAndUnavailableHistoryPropagateInsteadOfReturningLocalFinance() = runBlocking<Unit> {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var status = 200
        var historyStatus = 200
        var response = summary
        val paths = mutableListOf<String>()
        server.createContext("/api/rider/payouts/") { exchange ->
            paths += exchange.requestURI.toString()
            assertEquals("Bearer own-rider-fixture", exchange.requestHeaders.getFirst("Authorization"))
            val history = exchange.requestURI.path.endsWith("/history")
            val code = if (history) historyStatus else status
            val body = if (code != 200) """{"message":"Fixture read denied or unavailable"}""" else if (history) "[]" else response
            val bytes = body.toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(code, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val repository = NetworkRiderPayoutRepository(RiderPayoutApiClient("http://127.0.0.1:${server.address.port}", { "own-rider-fixture" }))
        try {
            assertEquals(150, repository.summary().availableCfa)
            for (denied in listOf(401, 403, 503)) {
                status = denied
                assertEquals(denied, assertFailsWith<SequoApiException> { repository.summary() }.statusCode)
            }
            status = 200
            historyStatus = 503
            assertEquals(503, assertFailsWith<SequoApiException> { repository.history() }.statusCode)
            historyStatus = 200
            response = summary.replace("\"availableBalanceCfa\":150", "\"availableBalanceCfa\":125")
            assertEquals(125, repository.summary().availableCfa)
            assertTrue(repository.history().isEmpty())
            assertTrue(paths.all { it == "/api/rider/payouts/summary" || it == "/api/rider/payouts/history" }, "Courier identity must come from the authenticated session, not a client rider ID.")
        } finally {
            repository.close()
            server.stop(0)
        }
    }

    private suspend fun fixture(summary: String, history: String, block: suspend (NetworkRiderPayoutRepository) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/rider/payouts/") { exchange ->
            assertEquals("Bearer own-rider-fixture", exchange.requestHeaders.getFirst("Authorization"))
            val bytes = (if (exchange.requestURI.path.endsWith("/history")) history else summary).toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val repository = NetworkRiderPayoutRepository(RiderPayoutApiClient("http://127.0.0.1:${server.address.port}", { "own-rider-fixture" }))
        try { block(repository) } finally { repository.close(); server.stop(0) }
    }
}
