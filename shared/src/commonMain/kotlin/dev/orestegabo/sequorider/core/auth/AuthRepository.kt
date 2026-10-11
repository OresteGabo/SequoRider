package dev.orestegabo.sequorider.core.auth

class AuthRepository(
    private val authApiClient: AuthApiClient,
    private val sessionStore: AuthSessionStore,
    private val secureSessionCache: SecureSessionCache,
) {
    suspend fun loginWithGoogle(idToken: String): CurrentUser {
        if (idToken.isBlank()) {
            throw GoogleIdTokenMissingException()
        }

        val session = authApiClient.loginWithGoogle(idToken).session
        saveSession(session)
        return requireRiderAccess(authApiClient.currentUser(session.accessToken))
    }

    suspend fun restoreSavedSession(): CurrentUser? {
        val session = sessionStore.get() ?: return null
        return runCatching {
            authApiClient.currentUser(session.accessToken)
        }.getOrElse { firstError ->
            if ((firstError as? AuthApiException)?.statusCode != 401) throw firstError
            val refreshedSession = authApiClient.refreshSession(session.refreshToken)
            saveSession(refreshedSession)
            authApiClient.currentUser(refreshedSession.accessToken)
        }.let(::requireRiderAccess).also { sessionStore.saveRememberedUser(it) }
    }

    suspend fun currentUser(): CurrentUser? {
        val session = sessionStore.get() ?: return null
        return requireRiderAccess(authApiClient.currentUser(session.accessToken))
    }

    suspend fun getRememberedUser(): CurrentUser? =
        sessionStore.getRememberedUser()

    suspend fun hasCachedRefreshToken(): Boolean =
        secureSessionCache.hasCachedRefreshToken()

    suspend fun logout() {
        sessionStore.get()?.let { session ->
            runCatching { authApiClient.logout(session.refreshToken) }
        }
        sessionStore.clear()
        secureSessionCache.clearSession()
    }

    fun close() {
        authApiClient.close()
    }

    private suspend fun saveSession(session: AuthSession) {
        sessionStore.save(session)
        secureSessionCache.cache(session)
    }

    private fun requireRiderAccess(user: CurrentUser): CurrentUser {
        if (!user.hasRiderAccess()) {
            throw AuthApiException(statusCode = 403, userMessage = AuthUserMessage.RiderRoleRequired)
        }
        return user
    }
}

class GoogleIdTokenMissingException : Exception(AuthUserMessage.GoogleSignInTemporarilyUnavailable.name)
