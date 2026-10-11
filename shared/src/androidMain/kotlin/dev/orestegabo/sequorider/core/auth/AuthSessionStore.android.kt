package dev.orestegabo.sequorider.core.auth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
actual fun rememberAuthSessionStore(): AuthSessionStore {
    val context = LocalContext.current.applicationContext
    val isPreview = LocalInspectionMode.current
    return remember(context, isPreview) {
        if (isPreview) PreviewAuthSessionStore() else AndroidAuthSessionStore(context)
    }
}

private class AndroidAuthSessionStore(
    context: Context,
    private val json: Json = Json,
) : AuthSessionStore {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "sequo_rider_auth_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override suspend fun save(session: AuthSession) {
        preferences.edit().putString(AuthSessionKey, json.encodeToString(session)).apply()
    }

    override suspend fun get(): AuthSession? =
        preferences.getString(AuthSessionKey, null)?.let { encoded ->
            runCatching { json.decodeFromString<AuthSession>(encoded) }.getOrNull()
        }

    override suspend fun saveRememberedUser(user: CurrentUser) {
        preferences.edit().putString(RememberedUserKey, json.encodeToString(user)).apply()
    }

    override suspend fun getRememberedUser(): CurrentUser? =
        preferences.getString(RememberedUserKey, null)?.let { encoded ->
            runCatching { json.decodeFromString<CurrentUser>(encoded) }.getOrNull()
        }

    override suspend fun clear() {
        preferences.edit().remove(AuthSessionKey).remove(RememberedUserKey).apply()
    }

    private companion object {
        const val AuthSessionKey = "auth_session"
        const val RememberedUserKey = "remembered_user"
    }
}

private class PreviewAuthSessionStore : AuthSessionStore {
    private var session: AuthSession? = null
    private var user: CurrentUser? = null

    override suspend fun save(session: AuthSession) {
        this.session = session
    }

    override suspend fun get(): AuthSession? = session

    override suspend fun saveRememberedUser(user: CurrentUser) {
        this.user = user
    }

    override suspend fun getRememberedUser(): CurrentUser? = user

    override suspend fun clear() {
        session = null
        user = null
    }
}
