package dev.orestegabo.sequorider.core.auth

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberAuthDeviceId(): String {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() }
            ?: "android-rider-device"
    }
}
