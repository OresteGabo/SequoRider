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
            for (key in listOf(RiderText.ExitDemo, RiderText.LocalFixtureAccounts, RiderText.LocalFixtureDetail, RiderText.NavRoute, RiderText.NavHistory)) {
                assertTrue(appText(key, language).isNotBlank())
            }
        }
        assertNotEquals(missionStatusText(MissionStatus.PICKED_UP_FROM_SELLER, AppLanguage.English),
            missionStatusText(MissionStatus.PICKED_UP_FROM_SELLER, AppLanguage.French))
    }
    @Test fun timingUsesOnlyTimingDataAndNeverALifecycleStatus() {
        for (language in AppLanguage.entries) {
            val unknown = appText(RiderText.NotAvailableYet, language)
            assertEquals(unknown, missionEtaText(0, "", language))
            MissionStatus.entries.forEach { status ->
                assertEquals(unknown, missionEtaText(0, status.name, language))
                assertEquals("12 min", missionEtaText(12, status.name, language))
            }
            assertEquals("20–40 min", missionEtaText(0, "20–40 min", language))
            assertEquals("12 min / 20–40 min", missionEtaText(12, "20–40 min", language))
        }
    }

    @Test fun legacyModesAndDestinationsAreReadableWithoutInventingAnOrigin() {
        for (language in AppLanguage.entries) {
            dev.orestegabo.sequorider.model.MissionServiceType.entries.forEach { mode ->
                val label = missionModeText(null, mode.name, language)
                assertTrue(label.isNotBlank())
                assertNotEquals(mode.name, label)
                assertFalse(label.contains('_'))
            }
            MissionDestinationType.entries.forEach { destination ->
                assertFalse(missionPlaceText(destination.name, language).contains('_'))
            }
            assertEquals("Actual relay name", missionPlaceText("Actual relay name", language))
            assertEquals("SEQUO consolidation", missionRouteText("", "SEQUO consolidation", language))
            assertEquals("Tokoin → Adidogomé", missionRouteText(" Tokoin ", "Adidogomé", language))
            assertEquals("", missionRouteText("  ", "", language))
        }
        assertEquals(appText(RiderText.ModeDeferred, AppLanguage.French),
            missionModeText("DEFERRED", "PROGRAMMED", AppLanguage.French))
    }

}
