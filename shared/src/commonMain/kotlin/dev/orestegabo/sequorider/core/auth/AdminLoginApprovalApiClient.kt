package dev.orestegabo.sequorider.core.auth

import dev.orestegabo.sequorider.core.network.NetworkConfig
import dev.orestegabo.sequorider.core.network.SequoApiException
import dev.orestegabo.sequorider.core.network.SequoNetworkException
import dev.orestegabo.sequorider.core.network.createSequoHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable

class AdminLoginApprovalApiClient(
    private val baseUrl: String = NetworkConfig.ProductionBaseUrl,
    private val accessTokenProvider: () -> String?,
    private val httpClient: HttpClient = createSequoHttpClient(
        baseUrl = baseUrl,
        accessTokenProvider = accessTokenProvider,
    ),
) : AdminLoginApprovalRepository {
    override suspend fun getPendingLoginRequests(): Result<List<AdminLoginRequestSummary>> =
        safeAdminApprovalCall {
            httpClient.get(apiUrl("admin-auth/mobile/login-requests"))
                .body<List<AdminLoginRequestSummary>>()
        }

    override suspend fun approveLoginRequest(requestId: String): Result<AdminLoginApprovalResult> =
        safeAdminApprovalCall {
            httpClient.post(apiUrl("admin-auth/mobile/login-requests/${requestId.safePathSegment()}/approve")) {
                setBody(EmptyAdminLoginApprovalRequest)
            }.body<AdminLoginApprovalResponseDto>().toResult()
        }

    override suspend fun rejectLoginRequest(requestId: String): Result<AdminLoginApprovalResult> =
        safeAdminApprovalCall {
            httpClient.post(apiUrl("admin-auth/mobile/login-requests/${requestId.safePathSegment()}/reject")) {
                setBody(AdminLoginRejectionRequestDto(reason = "not_me"))
            }.body<AdminLoginApprovalResponseDto>().toResult()
        }

    fun close() {
        httpClient.close()
    }

    private fun apiUrl(path: String): String =
        "${baseUrl.trimEnd('/')}/api/${path.trimStart('/')}"
}

class AdminLoginApprovalException(
    val messageKey: AdminLoginApprovalMessage,
    override val cause: Throwable? = null,
) : Exception(messageKey.name, cause)

@Serializable
private data object EmptyAdminLoginApprovalRequest

@Serializable
private data class AdminLoginRejectionRequestDto(
    val reason: String? = null,
)

@Serializable
private data class AdminLoginApprovalResponseDto(
    val status: String,
)

private fun AdminLoginApprovalResponseDto.toResult(): AdminLoginApprovalResult {
    val approvalStatus = status.toApprovalStatus()
    return AdminLoginApprovalResult(
        status = approvalStatus,
        message = when (approvalStatus) {
            AdminLoginApprovalStatus.Approved -> AdminLoginApprovalMessage.RequestApproved
            AdminLoginApprovalStatus.Rejected -> AdminLoginApprovalMessage.RequestRejected
            AdminLoginApprovalStatus.Expired -> AdminLoginApprovalMessage.RequestExpired
            else -> AdminLoginApprovalMessage.PendingRequestsLoaded
        },
    )
}

private suspend fun <T> safeAdminApprovalCall(block: suspend () -> T): Result<T> =
    runCatching { block() }.recoverCatching { error ->
        throw AdminLoginApprovalException(error.toAdminApprovalMessage(), error)
    }

private fun Throwable.toAdminApprovalMessage(): AdminLoginApprovalMessage =
    when (this) {
        is SequoNetworkException -> AdminLoginApprovalMessage.NetworkUnavailable
        is SequoApiException -> when (statusCode) {
            401, 403 -> AdminLoginApprovalMessage.NoEligibleSession
            else -> AdminLoginApprovalMessage.ActionFailed
        }
        else -> AdminLoginApprovalMessage.ActionFailed
    }

private fun String.toApprovalStatus(): AdminLoginApprovalStatus =
    when (uppercase()) {
        "PENDING" -> AdminLoginApprovalStatus.Pending
        "APPROVED" -> AdminLoginApprovalStatus.Approved
        "REJECTED" -> AdminLoginApprovalStatus.Rejected
        "EXPIRED" -> AdminLoginApprovalStatus.Expired
        "CONSUMED" -> AdminLoginApprovalStatus.Consumed
        else -> AdminLoginApprovalStatus.Failed
    }

private fun String.safePathSegment(): String =
    trim().filter { it.isLetterOrDigit() || it == '-' || it == '_' }
