package dev.orestegabo.sequorider

import dev.orestegabo.sequorider.feature.settings.*
import dev.orestegabo.sequorider.model.MissionStatus
import kotlin.test.*

class RiderPresentationTest {
    @Test fun everyLifecycleStatusAndFixtureControlHasBothLanguageLabels() {
        for (language in AppLanguage.entries) {
            MissionStatus.entries.forEach { status ->
                val label = missionStatusText(status, language)
                assertTrue(label.isNotBlank())
                assertNotEquals(status.name, label)
                assertFalse(label.contains('_'))
            }
            for (key in listOf(RiderText.ExitDemo, RiderText.LocalFixtureAccounts, RiderText.LocalFixtureDetail)) {
                assertTrue(appText(key, language).isNotBlank())
            }
        }
        assertNotEquals(missionStatusText(MissionStatus.PICKED_UP_FROM_SELLER, AppLanguage.English),
            missionStatusText(MissionStatus.PICKED_UP_FROM_SELLER, AppLanguage.French))
    }
}
