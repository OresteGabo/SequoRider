package dev.orestegabo.sequorider.core.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
actual fun rememberAuthSessionStore(): AuthSessionStore =
    remember { IosAuthSessionStore() }

private class IosAuthSessionStore(
    private val keychain: AuthKeychainStore = AuthKeychainStore(),
    private val json: Json = Json,
) : AuthSessionStore {
    override suspend fun save(session: AuthSession) {
        keychain.save(AuthSessionKey, json.encodeToString(session))
    }

    override suspend fun get(): AuthSession? =
        keychain.get(AuthSessionKey)?.let { encoded ->
            runCatching { json.decodeFromString<AuthSession>(encoded) }.getOrNull()
        }

    override suspend fun saveRememberedUser(user: CurrentUser) {
        keychain.save(RememberedUserKey, json.encodeToString(user))
    }

    override suspend fun getRememberedUser(): CurrentUser? =
        keychain.get(RememberedUserKey)?.let { encoded ->
            runCatching { json.decodeFromString<CurrentUser>(encoded) }.getOrNull()
        }

    override suspend fun clear() {
        keychain.clear(AuthSessionKey)
        keychain.clear(RememberedUserKey)
    }

    private companion object {
        const val AuthSessionKey = "auth_session"
        const val RememberedUserKey = "remembered_user"
    }
}
