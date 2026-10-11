package dev.orestegabo.sequorider.ui.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.orestegabo.sequorider.feature.settings.RiderText
import dev.orestegabo.sequorider.feature.settings.appText
import dev.orestegabo.sequorider.model.MissionDestinationType
import dev.orestegabo.sequorider.model.MissionStatus
import dev.orestegabo.sequorider.model.ProofRequirement
import dev.orestegabo.sequorider.model.RiderMission
import dev.orestegabo.sequorider.model.RouteStop
import dev.orestegabo.sequorider.model.formatCfa
import dev.orestegabo.sequorider.ui.components.RiderCard
import dev.orestegabo.sequorider.ui.components.RiderKeyValueRow
import dev.orestegabo.sequorider.ui.components.RiderMetric
import dev.orestegabo.sequorider.ui.components.RiderStatusPill

@Composable
fun ActiveRouteScreen(
    mission: RiderMission?,
    actionInProgress: Boolean,
    actionFailed: Boolean,
    onMarkPickupDone: (RiderMission) -> Unit,
    onConfirmDelivery: (RiderMission) -> Unit,
    onConfirmRelayDeposit: (RiderMission) -> Unit,
    onReportProblem: (RiderMission) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (mission == null) {
            item {
                RiderCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(appText(RiderText.ActiveRoute), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(appText(RiderText.EmptyMissions), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            return@LazyColumn
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(appText(RiderText.MissionDetail), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(mission.code, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    RiderStatusPill(mission.status.name)
                }
                if (actionFailed) {
                    Text(appText(RiderText.MissionActionFailed), color = MaterialTheme.colorScheme.error)
                }
                if (actionInProgress) {
                    Text(appText(RiderText.MissionActionInProgress), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            RiderCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(appText(RiderText.RouteSummary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RiderMetric(appText(RiderText.PackageCount), mission.packageCount.toString(), Modifier.weight(1f))
                        RiderMetric(appText(RiderText.EtaSla), mission.etaLabel(), Modifier.weight(1f))
                    }
                    RiderKeyValueRow(appText(RiderText.MissionCode), mission.code)
                    RiderKeyValueRow(appText(RiderText.MissionId), mission.id)
                    RiderKeyValueRow(appText(RiderText.OrderId), mission.orderId)
                    RiderKeyValueRow(appText(RiderText.MissionType), mission.serviceType.name)
                    RiderKeyValueRow(appText(RiderText.Distance), mission.distanceKm.ifBlank { appText(RiderText.NotAvailableYet) })
                    RiderKeyValueRow(appText(RiderText.PayoutPreview), mission.expectedPayoutCfa?.formatCfa() ?: appText(RiderText.NotAvailableYet))
                }
            }
        }

        item {
            RiderCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(appText(RiderText.StatusTimeline), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TimelineRow(appText(RiderText.CreatedAt), mission.createdAt)
                    TimelineRow(appText(RiderText.AssignedAt), mission.assignedAt)
                    TimelineRow(appText(RiderText.AcceptedAt), mission.acceptedAt)
                    TimelineRow(appText(RiderText.PickedUpAt), mission.pickupAt)
                    TimelineRow(appText(RiderText.RelayDepositedAt), mission.relayDepositedAt)
                    TimelineRow(appText(RiderText.DeliveredAt), mission.deliveredAt)
                    TimelineRow(appText(RiderText.UpdatedAt), mission.updatedAt)
                }
            }
        }

        item {
            RiderCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(appText(RiderText.PickupStops), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    mission.pickupStops.forEach { stop ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        RiderKeyValueRow(stop.title, if (stop.completed) appText(RiderText.Completed) else appText(RiderText.Pending))
                        Text(stop.area, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            RiderCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (mission.destinationType == MissionDestinationType.RELAY_POINT) appText(RiderText.RelayStep) else appText(RiderText.DropoffStop),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    RiderKeyValueRow(mission.dropoffStop.title, if (mission.dropoffStop.completed) appText(RiderText.Completed) else appText(RiderText.Pending))
                    Text(mission.dropoffStop.area, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    mission.customerDeliveryNote?.takeIf { it.isNotBlank() }?.let { note ->
                        RiderKeyValueRow(appText(RiderText.CustomerDeliveryNote), note)
                    }
                }
            }
        }

        item {
            RiderCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(appText(RiderText.ProofRequirements), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    mission.proofRequirements.ifEmpty { listOf(null) }.forEach { requirement ->
                        Text(
                            requirement?.label() ?: appText(RiderText.NotAvailableYet),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            RiderCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(appText(RiderText.RouteSteps), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    mission.displayRouteStops().forEach { stop ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        RiderKeyValueRow("${stop.sequence}. ${stop.title}", if (stop.completed) appText(RiderText.Completed) else appText(RiderText.Pending))
                        Text(stop.area, style = MaterialTheme.typography.bodyMedium)
                        Text(stop.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {}, modifier = Modifier.weight(1f), enabled = false) {
                        Text(appText(RiderText.StartPickup))
                    }
                    Button(
                        onClick = { onMarkPickupDone(mission) },
                        enabled = !actionInProgress && mission.status == MissionStatus.ACCEPTED_BY_COURIER,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(appText(RiderText.MarkPickupDone))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {}, modifier = Modifier.weight(1f), enabled = false) {
                        Text(appText(RiderText.StartDelivery))
                    }
                    Button(
                        onClick = {
                            if (mission.destinationType == MissionDestinationType.RELAY_POINT) {
                                onConfirmRelayDeposit(mission)
                            } else {
                                onConfirmDelivery(mission)
                            }
                        },
                        enabled = !actionInProgress && mission.status == MissionStatus.PICKED_UP_FROM_SELLER,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (mission.destinationType == MissionDestinationType.RELAY_POINT) appText(RiderText.ConfirmRelayDeposit) else appText(RiderText.ConfirmDelivery))
                    }
                }
                OutlinedButton(
                    onClick = { onReportProblem(mission) },
                    enabled = !actionInProgress && mission.status in problemReportableStatuses,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (mission.status == MissionStatus.ACCEPTED_BY_COURIER) appText(RiderText.ReportPickupProblem) else appText(RiderText.ReportDeliveryProblem))
                }
                Text(appText(RiderText.ActionRequiresEndpoint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TimelineRow(label: String, value: String?) {
    RiderKeyValueRow(label, value?.takeIf { it.isNotBlank() } ?: appText(RiderText.NotAvailableYet))
}

@Composable
private fun ProofRequirement.label(): String =
    when (this) {
        ProofRequirement.SELLER_PICKUP_PROOF -> appText(RiderText.ProofSellerPickup)
        ProofRequirement.CUSTOMER_DELIVERY_PROOF -> appText(RiderText.ProofCustomerDelivery)
        ProofRequirement.DELIVERY_PIN -> appText(RiderText.ProofDeliveryPin)
        ProofRequirement.RELAY_DEPOSIT_PROOF -> appText(RiderText.ProofRelayDeposit)
        ProofRequirement.PROBLEM_REASON -> appText(RiderText.ProofProblemReason)
    }

private fun RiderMission.displayRouteStops(): List<RouteStop> =
    routeStops.ifEmpty {
        pickupStops.mapIndexed { index, stop ->
            RouteStop(
                sequence = index + 1,
                title = stop.title,
                area = stop.area,
                detail = merchantSubOrderId ?: serviceType.name,
                completed = stop.completed,
            )
        } + RouteStop(
            sequence = pickupStops.size + 1,
            title = dropoffStop.title,
            area = dropoffStop.area,
            detail = destinationType.name,
            completed = dropoffStop.completed,
        )
    }

@Composable
private fun RiderMission.etaLabel(): String =
    when {
        etaMinutes > 0 && slaLabel.isNotBlank() -> "${etaMinutes} min / $slaLabel"
        etaMinutes > 0 -> "${etaMinutes} min"
        slaLabel.isNotBlank() -> slaLabel
        else -> appText(RiderText.NotAvailableYet)
    }

private val problemReportableStatuses = setOf(
    MissionStatus.ACCEPTED_BY_COURIER,
    MissionStatus.PICKED_UP_FROM_SELLER,
    MissionStatus.DEPOSITED_AT_RELAY,
)
