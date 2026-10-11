package dev.orestegabo.sequorider.core.auth

class SecureSessionCache(
    private val tokenStorage: SecureTokenStorage,
) {
    suspend fun cache(session: AuthSession) {
        tokenStorage.saveRefreshToken(session.refreshToken)
    }

    suspend fun hasCachedRefreshToken(): Boolean =
        tokenStorage.getRefreshToken()?.isNotBlank() == true

    suspend fun refreshToken(): String? =
        tokenStorage.getRefreshToken()

    suspend fun clearSession() {
        tokenStorage.clearSession()
    }
}
