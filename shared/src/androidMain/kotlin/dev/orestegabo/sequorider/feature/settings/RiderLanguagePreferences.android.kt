package dev.orestegabo.sequorider.feature.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberRiderLanguagePreferences(): RiderLanguagePreferences {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val preferences = context.getSharedPreferences("sequo_rider_preferences", Context.MODE_PRIVATE)
        RiderLanguagePreferences(object : RiderLanguageStorage {
            override fun read(): String? = preferences.getString("app_language", null)
            override fun write(value: String) {
                preferences.edit().putString("app_language", value).apply()
            }
        })
    }
}
