package dev.orestegabo.sequorider.core.payout

import dev.orestegabo.sequorider.core.network.NetworkConfig
import dev.orestegabo.sequorider.core.network.createSequoHttpClient
import dev.orestegabo.sequorider.model.PayoutEntryStatus
import dev.orestegabo.sequorider.model.PayoutHistoryItem
import dev.orestegabo.sequorider.model.PayoutStatus
import dev.orestegabo.sequorider.model.PayoutSummary
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

class RiderPayoutApiClient(
    private val baseUrl: String = NetworkConfig.ProductionBaseUrl,
    private val accessTokenProvider: () -> String?,
    private val httpClient: HttpClient = createSequoHttpClient(baseUrl = baseUrl),
) {
    suspend fun summary(): PayoutSummary =
        httpClient.get(apiUrl("rider/payouts/summary")) {
            bearer()
        }.body<RiderPayoutSummaryDto>().toModel()

    suspend fun history(): List<PayoutHistoryItem> =
        httpClient.get(apiUrl("rider/payouts/history")) {
            bearer()
        }.body<List<RiderPayoutHistoryItemDto>>().map { it.toModel() }

    private fun io.ktor.client.request.HttpRequestBuilder.bearer() {
        accessTokenProvider()?.takeIf { it.isNotBlank() }?.let { bearerAuth(it) }
    }

    private fun apiUrl(path: String): String =
        "${baseUrl.trimEnd('/')}/api/${path.trimStart('/')}"
}

class NetworkRiderPayoutRepository(
    private val apiClient: RiderPayoutApiClient,
) : RiderPayoutRepository {
    override suspend fun summary(): PayoutSummary = apiClient.summary()

    override suspend fun history(): List<PayoutHistoryItem> = apiClient.history()
}

@Serializable
private data class RiderPayoutSummaryDto(
    val riderId: String,
    val workforceType: String,
    val payable: Boolean,
    val availableBalanceCfa: Int,
    val onHoldCfa: Int,
    val paidOrProcessingCfa: Int,
    val historyCount: Int,
    val message: String,
) {
    fun toModel(): PayoutSummary =
        PayoutSummary(
            availableCfa = availableBalanceCfa,
            pendingReviewCfa = 0,
            onHoldCfa = onHoldCfa,
            lastPayoutCfa = paidOrProcessingCfa,
            status = if (!payable) {
                PayoutStatus.NotApplicable
            } else if (availableBalanceCfa > 0) {
                PayoutStatus.Available
            } else if (onHoldCfa > 0) {
                PayoutStatus.OnHold
            } else {
                PayoutStatus.PendingCompletion
            },
            payable = payable,
            paidOrProcessingCfa = paidOrProcessingCfa,
            historyCount = historyCount,
            message = message,
        )
}

@Serializable
private data class RiderPayoutHistoryItemDto(
    val id: String,
    val missionId: String,
    val amountCfa: Int,
    val payableAmountCfa: Int,
    val status: PayoutEntryStatus,
    val payableToRider: Boolean,
    val description: String,
    val createdAt: String,
) {
    fun toModel(): PayoutHistoryItem =
        PayoutHistoryItem(
            id = id,
            missionId = missionId,
            amountCfa = amountCfa,
            payableAmountCfa = payableAmountCfa,
            status = status,
            payableToRider = payableToRider,
            description = description,
            createdAt = createdAt,
        )
}
