package dev.orestegabo.sequorider.core.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthRepository(
    private val authApiClient: AuthApiClient,
    private val sessionStore: AuthSessionStore,
    private val secureSessionCache: SecureSessionCache,
) {
    private val sessionMutex = Mutex()

    suspend fun developmentAccess(): DevelopmentRiderAccess = authApiClient.developmentAccess()

    suspend fun loginWithFixture(accountKey: String): CurrentUser = sessionMutex.withLock {
        val session = authApiClient.loginWithFixture(accountKey)
        val user = requireRiderAccess(authApiClient.currentUser(session.accessToken))
        val expectedId = when (accountKey) {
            "senam" -> "local-ops-v1-rider-senam"
            "komlan" -> "local-ops-v1-rider-komlan"
            else -> error("Local fixture account is unavailable.")
        }
        require(user.id == expectedId) { "The local fixture response did not match the selected account." }
        saveSession(session, user)
        user
    }

    suspend fun loginWithGoogle(idToken: String): CurrentUser = sessionMutex.withLock {
        if (idToken.isBlank()) throw GoogleIdTokenMissingException()
        val session = authApiClient.loginWithGoogle(idToken).session
        val user = requireRiderAccess(authApiClient.currentUser(session.accessToken))
        saveSession(session, user)
        user
    }

    suspend fun restoreSavedSession(): CurrentUser? = sessionMutex.withLock {
        val session = sessionStore.get() ?: return@withLock null
        try {
            val user = try {
                authApiClient.currentUser(session.accessToken)
            } catch (firstError: AuthApiException) {
                if (firstError.statusCode != 401) throw firstError
                val refreshedSession = authApiClient.refreshSession(session.refreshToken)
                val refreshedUser = requireRiderAccess(authApiClient.currentUser(refreshedSession.accessToken))
                saveSession(refreshedSession, refreshedUser)
                refreshedUser
            }
            requireRiderAccess(user).also { sessionStore.saveRememberedUser(it) }
        } catch (error: AuthApiException) {
            if (error.statusCode in setOf(401, 403)) clearSession()
            throw error
        }
    }

    suspend fun currentUser(): CurrentUser? = sessionMutex.withLock {
        val session = sessionStore.get() ?: return@withLock null
        requireRiderAccess(authApiClient.currentUser(session.accessToken))
    }
    suspend fun getRememberedUser(): CurrentUser? = sessionStore.getRememberedUser()
    suspend fun hasCachedRefreshToken(): Boolean = secureSessionCache.hasCachedRefreshToken()

    suspend fun logout() = sessionMutex.withLock {
        try {
            sessionStore.get()?.let { session ->
                try { authApiClient.logout(session.refreshToken) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Local logout still clears an unavailable server session. */ }
            }
        } finally { clearSession() }
    }
    fun close() { authApiClient.close() }

    private suspend fun saveSession(session: AuthSession, user: CurrentUser) {
        sessionStore.save(session)
        secureSessionCache.cache(session)
        sessionStore.saveRememberedUser(user)
    }
    private suspend fun clearSession() {
        try { sessionStore.clear() } finally { secureSessionCache.clearSession() }
    }
    private fun requireRiderAccess(user: CurrentUser): CurrentUser {
        if (!user.hasRiderAccess()) throw AuthApiException(403, AuthUserMessage.RiderRoleRequired)
        return user
    }
}

class GoogleIdTokenMissingException : Exception(AuthUserMessage.GoogleSignInTemporarilyUnavailable.name)
