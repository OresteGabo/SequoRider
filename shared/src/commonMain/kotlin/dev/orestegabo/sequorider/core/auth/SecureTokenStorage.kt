package dev.orestegabo.sequorider.core.auth

import androidx.compose.runtime.Composable

interface SecureTokenStorage {
    suspend fun saveRefreshToken(token: String)
    suspend fun getRefreshToken(): String?
    suspend fun clearSession()
}

@Composable
expect fun rememberSecureTokenStorage(): SecureTokenStorage
