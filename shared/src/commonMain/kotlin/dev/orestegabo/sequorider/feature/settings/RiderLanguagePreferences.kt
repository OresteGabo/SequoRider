package dev.orestegabo.sequorider.feature.settings

import androidx.compose.runtime.Composable

/** UI preference only. This store never reads or changes an account/session. */
internal interface RiderLanguageStorage {
    fun read(): String?
    fun write(value: String)
}

internal class RiderLanguagePreferences(private val storage: RiderLanguageStorage) {
    fun load(): AppLanguage = when (storage.read()) {
        "fr" -> AppLanguage.French
        else -> AppLanguage.English // Preserve the existing default for absent/unknown legacy values.
    }

    fun save(language: AppLanguage) {
        storage.write(when (language) {
            AppLanguage.English -> "en"
            AppLanguage.French -> "fr"
        })
    }
}

@Composable
internal expect fun rememberRiderLanguagePreferences(): RiderLanguagePreferences
