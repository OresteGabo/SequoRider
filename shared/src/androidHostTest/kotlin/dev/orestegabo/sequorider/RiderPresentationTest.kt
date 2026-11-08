package dev.orestegabo.sequorider

import dev.orestegabo.sequorider.feature.settings.*
import dev.orestegabo.sequorider.model.MissionStatus
import dev.orestegabo.sequorider.model.MissionDestinationType
import dev.orestegabo.sequorider.ui.route.RiderMissionStep
import dev.orestegabo.sequorider.ui.route.riderMissionStep
import dev.orestegabo.sequorider.core.network.riderAccessDenied
import dev.orestegabo.sequorider.core.missions.RiderMissionApiException
import dev.orestegabo.sequorider.core.network.SequoApiException
import kotlin.test.*

class RiderPresentationTest {
    @Test fun onlyTheActualNextCustodyStepIsShownAndTerminalMissionsHaveNoTransition() {
        val destinations = MissionDestinationType.entries
        for (destination in destinations) {
            assertEquals(RiderMissionStep.Pickup,
                riderMissionStep(MissionStatus.ACCEPTED_BY_COURIER, destination))
        }
        val expected = listOf(RiderMissionStep.HomeDelivery,
            RiderMissionStep.RelayDeposit,
            RiderMissionStep.SequoReceipt)
        destinations.zip(expected).forEach { (destination, step) ->
            assertEquals(step, riderMissionStep(MissionStatus.PICKED_UP_FROM_SELLER, destination))
        }
        MissionStatus.entries.filter { it !in setOf(MissionStatus.ACCEPTED_BY_COURIER, MissionStatus.PICKED_UP_FROM_SELLER) }.forEach { status ->
            assertEquals(RiderMissionStep.ReadOnly,
                riderMissionStep(status, destinations.first()))
        }
    }

    @Test fun authDenialIsDistinctFromOfflineBusinessRejectionAndServerErrors() {
        for (status in listOf(401, 403)) {
            assertTrue(riderAccessDenied(
                RiderMissionApiException(status, null, Exception())))
            assertTrue(riderAccessDenied(
                SequoApiException(status, null, "Unavailable")))
        }
        for (status in listOf(400, 404, 409, 429, 500)) {
            assertFalse(riderAccessDenied(
                RiderMissionApiException(status, null, Exception())))
        }
        assertFalse(riderAccessDenied(Exception("Offline")))
    }

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
