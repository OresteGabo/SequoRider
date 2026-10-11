package dev.orestegabo.sequorider.core.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberSecureTokenStorage(): SecureTokenStorage =
    remember { IosSecureTokenStorage() }

private class IosSecureTokenStorage(
    private val keychain: AuthKeychainStore = AuthKeychainStore(),
) : SecureTokenStorage {
    override suspend fun saveRefreshToken(token: String) {
        keychain.save(RefreshTokenKey, token)
    }

    override suspend fun getRefreshToken(): String? =
        keychain.get(RefreshTokenKey)

    override suspend fun clearSession() {
        keychain.clear(RefreshTokenKey)
    }

    private companion object {
        const val RefreshTokenKey = "refresh_token"
    }
}
