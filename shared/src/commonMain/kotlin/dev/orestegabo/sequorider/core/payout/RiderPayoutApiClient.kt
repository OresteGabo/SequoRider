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
    private val baseUrl: String = NetworkConfig.BaseUrl,
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

    fun close() = httpClient.close()

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

    override fun close() = apiClient.close()
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
    val earnedTotalCfa: Int? = null,
    val settledMissionCount: Int? = null,
    val processingCfa: Int? = null,
    val paidCfa: Int? = null,
    val transferStatus: String? = null,
    val unverifiedTransferCfa: Int? = null,
    val reviewRequiredCfa: Int? = null,
) {
    fun toModel(): PayoutSummary =
        PayoutSummary(
            availableCfa = availableBalanceCfa,
            pendingReviewCfa = 0,
            onHoldCfa = onHoldCfa,
            status = if (!payable) {
                PayoutStatus.NotApplicable
            } else if (availableBalanceCfa > 0) {
                PayoutStatus.Available
            } else if (onHoldCfa > 0) {
                PayoutStatus.OnHold
            } else if ((reviewRequiredCfa ?: 0) > 0) {
                PayoutStatus.ReviewRequired
            } else {
                PayoutStatus.PendingCompletion
            },
            payable = payable,
            paidOrProcessingCfa = paidOrProcessingCfa,
            historyCount = historyCount,
            message = message,
            workforceType = workforceType,
            earnedTotalCfa = earnedTotalCfa,
            settledMissionCount = settledMissionCount,
            processingCfa = processingCfa,
            paidCfa = paidCfa,
            transferStatus = transferStatus,
            unverifiedTransferCfa = unverifiedTransferCfa,
            reviewRequiredCfa = reviewRequiredCfa,
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
    val workforceType: String? = null,
    val account: String? = null,
    val direction: String? = null,
    val publicReference: String? = null,
    val missionReference: String? = null,
) {
    fun toModel(): PayoutHistoryItem =
        PayoutHistoryItem(
            id = id,
            missionId = missionId,
            publicReference = publicReference,
            missionReference = missionReference,
            amountCfa = amountCfa,
            payableAmountCfa = payableAmountCfa,
            status = status,
            payableToRider = payableToRider,
            description = description,
            createdAt = createdAt,
            workforceType = workforceType,
            account = account,
            direction = direction,
        )
}
