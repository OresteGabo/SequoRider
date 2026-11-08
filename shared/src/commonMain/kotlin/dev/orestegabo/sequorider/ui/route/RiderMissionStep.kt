package dev.orestegabo.sequorider.ui.route

import dev.orestegabo.sequorider.model.MissionDestinationType
import dev.orestegabo.sequorider.model.MissionStatus

internal enum class RiderMissionStep { Pickup, HomeDelivery, RelayDeposit, SequoReceipt, ReadOnly }

internal fun riderMissionStep(status: MissionStatus?, destination: MissionDestinationType?): RiderMissionStep = when (status) {
    MissionStatus.ACCEPTED_BY_COURIER -> RiderMissionStep.Pickup
    MissionStatus.PICKED_UP_FROM_SELLER -> when (destination) {
        MissionDestinationType.CUSTOMER_ADDRESS -> RiderMissionStep.HomeDelivery
        MissionDestinationType.RELAY_POINT -> RiderMissionStep.RelayDeposit
        MissionDestinationType.SEQUO_CONSOLIDATION -> RiderMissionStep.SequoReceipt
        null -> RiderMissionStep.ReadOnly
    }
    else -> RiderMissionStep.ReadOnly
}
