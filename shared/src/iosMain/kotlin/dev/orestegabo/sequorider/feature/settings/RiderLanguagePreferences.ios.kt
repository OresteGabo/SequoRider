package dev.orestegabo.sequorider.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun rememberRiderLanguagePreferences(): RiderLanguagePreferences = remember {
    val preferences = NSUserDefaults.standardUserDefaults
    RiderLanguagePreferences(object : RiderLanguageStorage {
        override fun read(): String? = preferences.stringForKey("sequorider.ui.language")
        override fun write(value: String) {
            preferences.setObject(value, forKey = "sequorider.ui.language")
        }
    })
}
