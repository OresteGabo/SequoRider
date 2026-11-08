package dev.orestegabo.sequorider

import dev.orestegabo.sequorider.feature.settings.AppLanguage
import dev.orestegabo.sequorider.feature.settings.RiderLanguagePreferences
import dev.orestegabo.sequorider.feature.settings.RiderLanguageStorage
import kotlin.test.Test
import kotlin.test.assertEquals

class RiderLanguagePreferencesTest {
    private class Storage(var persisted: String? = null) : RiderLanguageStorage {
        override fun read(): String? = persisted
        override fun write(value: String) { persisted = value }
    }

    @Test fun selectedLanguageSurvivesRecreationAndCanBeChangedBack() {
        val durable = Storage()
        RiderLanguagePreferences(durable).save(AppLanguage.French)
        assertEquals("fr", durable.persisted)
        // A new instance models recreation/relaunch; no account/session participates.
        assertEquals(AppLanguage.French, RiderLanguagePreferences(durable).load())
        RiderLanguagePreferences(durable).save(AppLanguage.English)
        assertEquals("en", durable.persisted)
        assertEquals(AppLanguage.English, RiderLanguagePreferences(durable).load())
    }

    @Test fun absentOrUnknownLegacyPreferenceKeepsTheExistingDefaultWithoutRewritingStorage() {
        for (value in listOf(null, "", "French", "unsupported")) {
            val durable = Storage(value)
            assertEquals(AppLanguage.English, RiderLanguagePreferences(durable).load())
            assertEquals(value, durable.persisted)
        }
    }
}
