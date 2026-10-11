package dev.orestegabo.sequorider.core.auth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

@Composable
actual fun rememberSecureTokenStorage(): SecureTokenStorage {
    val context = LocalContext.current.applicationContext
    val isPreview = LocalInspectionMode.current
    return remember(context, isPreview) {
        if (isPreview) PreviewSecureTokenStorage() else AndroidSecureTokenStorage(context)
    }
}

private class AndroidSecureTokenStorage(
    context: Context,
) : SecureTokenStorage {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "sequo_rider_secure_tokens",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override suspend fun saveRefreshToken(token: String) {
        preferences.edit().putString(RefreshTokenKey, token).apply()
    }

    override suspend fun getRefreshToken(): String? =
        preferences.getString(RefreshTokenKey, null)

    override suspend fun clearSession() {
        preferences.edit().remove(RefreshTokenKey).apply()
    }

    private companion object {
        const val RefreshTokenKey = "refresh_token"
    }
}

private class PreviewSecureTokenStorage : SecureTokenStorage {
    private var refreshToken: String? = null

    override suspend fun saveRefreshToken(token: String) {
        refreshToken = token
    }

    override suspend fun getRefreshToken(): String? = refreshToken

    override suspend fun clearSession() {
        refreshToken = null
    }
}
