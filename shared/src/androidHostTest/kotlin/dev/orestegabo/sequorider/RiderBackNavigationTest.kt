package dev.orestegabo.sequorider

import dev.orestegabo.sequorider.model.RiderSection
import dev.orestegabo.sequorider.ui.navigation.riderBackDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RiderBackNavigationTest {
    @Test
    fun missionsRootLeavesBackToTheAndroidActivity() {
        assertNull(riderBackDestination(RiderSection.Missions))
    }

    @Test
    fun missionDetailAndSecondaryTabsReturnToMissions() {
        RiderSection.entries.filter { it != RiderSection.Missions }.forEach { section ->
            assertEquals(RiderSection.Missions, riderBackDestination(section), section.name)
        }
    }
}
