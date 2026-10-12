package dev.orestegabo.sequorider.core.auth

import kotlinx.serialization.Serializable

@Serializable
data class AdminLoginRequestSummary(
    val requestId: String,
    val code: String,
    val deviceLabel: String? = null,
    val browserInfo: String? = null,
    val requestedAt: String,
    val expiresAt: String,
)

@Serializable
data class AdminLoginApprovalResult(
    val status: AdminLoginApprovalStatus,
    val message: AdminLoginApprovalMessage,
)

@Serializable
enum class AdminLoginApprovalStatus {
    Pending,
    Approved,
    Rejected,
    Expired,
    Consumed,
    Failed,
}

@Serializable
enum class AdminLoginApprovalMessage {
    PendingRequestsLoaded,
    RequestApproved,
    RequestRejected,
    RequestExpired,
    NoEligibleSession,
    NetworkUnavailable,
    ActionFailed,
}

interface AdminLoginApprovalRepository {
    suspend fun getPendingLoginRequests(): Result<List<AdminLoginRequestSummary>>

    suspend fun approveLoginRequest(requestId: String): Result<AdminLoginApprovalResult>

    suspend fun rejectLoginRequest(requestId: String): Result<AdminLoginApprovalResult>
}
