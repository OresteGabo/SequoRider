package dev.orestegabo.sequorider.core.auth

sealed interface GoogleSignInResult {
    data class Success(
        val idToken: String,
        val email: String? = null,
        val displayName: String? = null,
    ) : GoogleSignInResult

    data object Cancelled : GoogleSignInResult

    data class Failure(
        val reason: GoogleSignInFailureReason,
    ) : GoogleSignInResult
}

enum class GoogleSignInFailureReason {
    Failed,
    Interrupted,
    InProgress,
    NotConfigured,
    Unavailable,
}
