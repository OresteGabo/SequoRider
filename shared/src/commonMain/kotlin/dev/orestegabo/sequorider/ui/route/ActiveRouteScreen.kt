package dev.orestegabo.sequorider.ui.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import dev.orestegabo.sequo.design.SequoActionButton as Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import dev.orestegabo.sequo.design.SequoOutlineButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.orestegabo.sequo.design.SequoDesign
import dev.orestegabo.sequorider.feature.settings.missionModeText
import dev.orestegabo.sequorider.feature.settings.missionWorkforceText
import dev.orestegabo.sequorider.feature.settings.missionStatusText
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
import dev.orestegabo.sequorider.ui.components.RiderMissionHeader
import dev.orestegabo.sequorider.feature.settings.missionEtaText
import dev.orestegabo.sequorider.feature.settings.missionPlaceText

@Composable
fun ActiveRouteScreen(
    mission: RiderMission?,
    actionInProgress: Boolean,
    actionFailed: Boolean,
    isOffline: Boolean = false,
    isLoading: Boolean = false,
    accessDenied: Boolean = false,
    onMarkPickupDone: (RiderMission, String) -> Unit,
    onConfirmDelivery: (RiderMission, String, String) -> Unit,
    onConfirmRelayDeposit: (RiderMission, String, String) -> Unit,
    onReportProblem: (RiderMission, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextStep = riderMissionStep(mission?.status, mission?.destinationType)
    val actionsDisabled = actionInProgress || isLoading || isOffline || accessDenied
    val unavailableMessage = when {
        accessDenied -> appText(RiderText.PayoutAccessDenied)
        isOffline -> appText(RiderText.OfflineMissions)
        isLoading -> appText(RiderText.LoadingMissions)
        else -> null
    }
    var custodyNote by remember(mission?.id) { mutableStateOf("") }
    var cannotPickupDialogOpen by remember(mission?.id) { mutableStateOf(false) }
    var cannotPickupReason by remember(mission?.id) { mutableStateOf("") }
    var cannotPickupReasonTouched by remember(mission?.id) { mutableStateOf(false) }
    var deliveryPin by remember(mission?.id) { mutableStateOf("") }
    var deliveryProblemReason by remember(mission?.id) { mutableStateOf("") }
    var deliveryProblemTouched by remember(mission?.id) { mutableStateOf(false) }
    var packageCode by remember(mission?.id) { mutableStateOf("") }
    var lockerSlot by remember(mission?.id) { mutableStateOf("") }

    if (mission != null && cannotPickupDialogOpen) {
        CannotPickupDialog(
            pickupProblem = mission.status == MissionStatus.ACCEPTED_BY_COURIER,
            reason = cannotPickupReason,
            reasonTouched = cannotPickupReasonTouched,
            onReasonChange = {
                cannotPickupReason = sanitizePickupText(it, MaxPickupReasonLength)
                cannotPickupReasonTouched = true
            },
            onDismiss = {
                cannotPickupDialogOpen = false
                cannotPickupReasonTouched = false
            },
            onSubmit = {
                cannotPickupReasonTouched = true
                if (cannotPickupReason.isNotBlank()) {
                    onReportProblem(mission, cannotPickupReason)
                    cannotPickupDialogOpen = false
                }
            },
        )
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = SequoDesign.ScreenGutter),
        verticalArrangement = Arrangement.spacedBy(SequoDesign.SectionGap),
    ) {
        if (mission == null) {
            item {
                RiderCard {
                    Column(Modifier.fillMaxWidth().padding(SequoDesign.CardPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(appText(RiderText.ActiveRoute), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(unavailableMessage ?: appText(RiderText.EmptyMissions), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            return@LazyColumn
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(appText(RiderText.MissionDetail), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                RiderMissionHeader(reference = mission.displayReference, status = missionStatusText(mission.status))
                if (actionFailed) {
                    Text(appText(RiderText.MissionActionFailed), color = MaterialTheme.colorScheme.error)
                }
                if (unavailableMessage != null) {
                    Text(unavailableMessage, color = MaterialTheme.colorScheme.error)
                } else if (actionInProgress) {
                    Text(appText(RiderText.MissionActionInProgress), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (nextStep == RiderMissionStep.HomeDelivery) {
            item {
                HomeDeliveryPanel(
                    mission = mission,
                    deliveryPin = deliveryPin,
                    deliveryProblemReason = deliveryProblemReason,
                    deliveryProblemTouched = deliveryProblemTouched,
                    actionInProgress = actionsDisabled,
                    onDeliveryPinChange = { deliveryPin = sanitizeCode(it, MaxDeliveryPinLength) },
                    onDeliveryProblemReasonChange = {
                        deliveryProblemReason = sanitizePickupText(it, MaxPickupReasonLength)
                        deliveryProblemTouched = true
                    },
                    onConfirmDelivery = { onConfirmDelivery(mission, deliveryPin, deliveryProblemReason) },
                    onCustomerUnavailable = {
                        deliveryProblemTouched = true
                        if (deliveryProblemReason.isNotBlank()) {
                            onReportProblem(mission, deliveryProblemReason)
                        }
                    },
                )
            }
        } else if (nextStep == RiderMissionStep.RelayDeposit) {
            item {
                RelayDepositPanel(
                    mission = mission,
                    packageCode = packageCode,
                    lockerSlot = lockerSlot,
                    actionInProgress = actionsDisabled,
                    onPackageCodeChange = { packageCode = sanitizePackageCode(it) },
                    onLockerSlotChange = { lockerSlot = sanitizePickupText(it, MaxLockerSlotLength) },
                    onConfirmRelayDeposit = { onConfirmRelayDeposit(mission, packageCode, lockerSlot) },
                )
            }
        }

        if (mission.destinationType == MissionDestinationType.SEQUO_CONSOLIDATION) {
            item {
                RiderCard {
                    Column(Modifier.fillMaxWidth().padding(SequoDesign.CardPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(missionPlaceText(mission.dropoffStop.title), style = MaterialTheme.typography.titleMedium)
                        Text(appText(if (mission.status == MissionStatus.HANDED_TO_SEQUO) RiderText.ConsolidationReceived else RiderText.ConsolidationReceiptPending))
                    }
                }
            }
        }

        if (nextStep == RiderMissionStep.Pickup) {
            item {
                RiderCard {
                    Column(Modifier.fillMaxWidth().padding(SequoDesign.CardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(appText(RiderText.PickupFlow), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RiderMetric(appText(RiderText.PickupSource), mission.pickupSourceLabel(), Modifier.weight(1f))
                            RiderMetric(appText(RiderText.ExpectedPackages), mission.packageCount.toString(), Modifier.weight(1f))
                        }
                        OutlinedTextField(
                            value = custodyNote,
                            onValueChange = { custodyNote = sanitizePickupText(it, MaxCustodyNoteLength) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(appText(RiderText.CustodyNote)) },
                            placeholder = { Text(appText(RiderText.CustodyNotePlaceholder)) },
                            supportingText = { Text(appText(RiderText.CharacterCount).formatCount(custodyNote.length, MaxCustodyNoteLength)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {}),
                            maxLines = 3,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onMarkPickupDone(mission, custodyNote) },
                                enabled = !actionsDisabled && mission.status == MissionStatus.ACCEPTED_BY_COURIER,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(appText(RiderText.MarkPickupDone))
                            }
                            OutlinedButton(
                                onClick = { cannotPickupDialogOpen = true },
                                enabled = !actionsDisabled && mission.status == MissionStatus.ACCEPTED_BY_COURIER,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(appText(RiderText.CannotPickup))
                            }
                        }
                        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false) {
                            Text(appText(RiderText.ScanPackage))
                        }
                        Text(appText(RiderText.ScanPlaceholder), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            RiderCard {
                Column(Modifier.fillMaxWidth().padding(SequoDesign.CardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(appText(RiderText.RouteSummary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RiderMetric(appText(RiderText.PackageCount), mission.packageCount.toString(), Modifier.weight(1f))
                        RiderMetric(appText(RiderText.EtaSla), missionEtaText(mission.etaMinutes, mission.slaLabel), Modifier.weight(1f))
                    }
                    RiderKeyValueRow(appText(RiderText.MissionCode), mission.displayReference)
                    RiderKeyValueRow(appText(RiderText.MissionId), mission.publicReference?.takeIf(String::isNotBlank) ?: mission.id)
                    RiderKeyValueRow(appText(RiderText.OrderId), mission.displayOrderReference)
                    RiderKeyValueRow(appText(RiderText.MissionType), missionModeText(mission.deliveryOptionId, mission.serviceType.name))
                    mission.assignmentWorkforceType?.let { RiderKeyValueRow(appText(RiderText.Workforce), missionWorkforceText(it)) }
                    RiderKeyValueRow(appText(RiderText.Distance), mission.distanceKm.ifBlank { appText(RiderText.NotAvailableYet) })
                    RiderKeyValueRow(appText(RiderText.PayoutPreview), mission.expectedPayoutCfa?.formatCfa() ?: appText(RiderText.NotAvailableYet))
                }
            }
        }

        item {
            RiderCard {
                Column(Modifier.fillMaxWidth().padding(SequoDesign.CardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                Column(Modifier.fillMaxWidth().padding(SequoDesign.CardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                Column(Modifier.fillMaxWidth().padding(SequoDesign.CardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (mission.destinationType == MissionDestinationType.RELAY_POINT) appText(RiderText.RelayStep) else appText(RiderText.DropoffStop),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    RiderKeyValueRow(missionPlaceText(mission.dropoffStop.title), if (mission.dropoffStop.completed) appText(RiderText.Completed) else appText(RiderText.Pending))
                    Text(mission.dropoffStop.area, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    mission.customerDeliveryNote?.takeIf { it.isNotBlank() }?.let { note ->
                        RiderKeyValueRow(appText(RiderText.CustomerDeliveryNote), note)
                    }
                }
            }
        }

        item {
            RiderCard {
                Column(Modifier.fillMaxWidth().padding(SequoDesign.CardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                Column(Modifier.fillMaxWidth().padding(SequoDesign.CardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(appText(RiderText.RouteSteps), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    mission.displayRouteStops().forEach { stop ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        RiderKeyValueRow("${stop.sequence}. ${missionPlaceText(stop.title)}", if (stop.completed) appText(RiderText.Completed) else appText(RiderText.Pending))
                        Text(stop.area, style = MaterialTheme.typography.bodyMedium)
                        Text(missionPlaceText(stop.detail), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (mission.status in problemReportableStatuses && nextStep != RiderMissionStep.Pickup) {
            item {
                OutlinedButton(
                    onClick = {
                        cannotPickupReason = deliveryProblemReason
                        cannotPickupDialogOpen = true
                    },
                    enabled = !actionsDisabled,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(appText(RiderText.ReportDeliveryProblem)) }
            }
        }

    }
}

@Composable
private fun HomeDeliveryPanel(
    mission: RiderMission,
    deliveryPin: String,
    deliveryProblemReason: String,
    deliveryProblemTouched: Boolean,
    actionInProgress: Boolean,
    onDeliveryPinChange: (String) -> Unit,
    onDeliveryProblemReasonChange: (String) -> Unit,
    onConfirmDelivery: () -> Unit,
    onCustomerUnavailable: () -> Unit,
) {
    val pinRequired = ProofRequirement.DELIVERY_PIN in mission.proofRequirements
    RiderCard {
        Column(Modifier.fillMaxWidth().padding(SequoDesign.CardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(appText(RiderText.HomeDelivery), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (pinRequired) {
                OutlinedTextField(
                    value = deliveryPin,
                    onValueChange = onDeliveryPinChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appText(RiderText.DeliveryPin)) },
                    placeholder = { Text(appText(RiderText.DeliveryPinPlaceholder)) },
                    supportingText = {
                        Text(
                            if (deliveryPin.length in 1 until MaxDeliveryPinLength) {
                                appText(RiderText.DeliveryPinRequired)
                            } else {
                                appText(RiderText.CharacterCount).formatCount(deliveryPin.length, MaxDeliveryPinLength)
                            },
                        )
                    },
                    isError = deliveryPin.length in 1 until MaxDeliveryPinLength,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    maxLines = 1,
                )
            }
            OutlinedTextField(
                value = deliveryProblemReason,
                onValueChange = onDeliveryProblemReasonChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(appText(RiderText.DeliveryProblemReason)) },
                placeholder = { Text(appText(RiderText.DeliveryProblemReasonPlaceholder)) },
                supportingText = {
                    Text(
                        if (deliveryProblemTouched && deliveryProblemReason.isBlank()) {
                            appText(RiderText.CannotPickupReasonRequired)
                        } else {
                            appText(RiderText.CharacterCount).formatCount(deliveryProblemReason.length, MaxPickupReasonLength)
                        },
                    )
                },
                isError = deliveryProblemTouched && deliveryProblemReason.isBlank(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                maxLines = 3,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onConfirmDelivery,
                    enabled = !actionInProgress && mission.canConfirmDropoff(deliveryPin),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(appText(RiderText.ConfirmDelivery))
                }
                OutlinedButton(
                    onClick = onCustomerUnavailable,
                    enabled = !actionInProgress && mission.status == MissionStatus.PICKED_UP_FROM_SELLER,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(appText(RiderText.CustomerUnavailable))
                }
            }
        }
    }
}

@Composable
private fun RelayDepositPanel(
    mission: RiderMission,
    packageCode: String,
    lockerSlot: String,
    actionInProgress: Boolean,
    onPackageCodeChange: (String) -> Unit,
    onLockerSlotChange: (String) -> Unit,
    onConfirmRelayDeposit: () -> Unit,
) {
    RiderCard {
        Column(Modifier.fillMaxWidth().padding(SequoDesign.CardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(appText(RiderText.RelayDeposit), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            RiderKeyValueRow(appText(RiderText.RelayPoint), missionPlaceText(mission.dropoffStop.title))
            OutlinedTextField(
                value = packageCode,
                onValueChange = onPackageCodeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(appText(RiderText.PackageCode)) },
                placeholder = { Text(appText(RiderText.PackageCodePlaceholder)) },
                supportingText = { Text(appText(RiderText.QrPlaceholder)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                maxLines = 1,
            )
            OutlinedTextField(
                value = lockerSlot,
                onValueChange = onLockerSlotChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(appText(RiderText.LockerSlot)) },
                placeholder = { Text(appText(RiderText.LockerSlotPlaceholder)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                maxLines = 1,
            )
            Button(
                onClick = onConfirmRelayDeposit,
                enabled = !actionInProgress && mission.status == MissionStatus.PICKED_UP_FROM_SELLER && lockerSlot.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(appText(RiderText.ConfirmDeposit))
            }
        }
    }
}

@Composable
private fun CannotPickupDialog(
    pickupProblem: Boolean,
    reason: String,
    reasonTouched: Boolean,
    onReasonChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appText(if (pickupProblem) RiderText.CannotPickup else RiderText.ReportDeliveryProblem)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appText(RiderText.CannotPickupReason)) },
                    placeholder = { Text(appText(if (pickupProblem) RiderText.CannotPickupReasonPlaceholder else RiderText.DeliveryProblemReasonPlaceholder)) },
                    supportingText = {
                        Text(
                            if (reasonTouched && reason.isBlank()) {
                                appText(RiderText.CannotPickupReasonRequired)
                            } else {
                                appText(RiderText.CharacterCount).formatCount(reason.length, MaxPickupReasonLength)
                            },
                        )
                    },
                    isError = reasonTouched && reason.isBlank(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            Button(onClick = onSubmit, enabled = reason.isNotBlank()) {
                Text(appText(RiderText.ConfirmProblem))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(appText(RiderText.Dismiss))
            }
        },
    )
}

@Composable
private fun TimelineRow(label: String, value: String?) {
    RiderKeyValueRow(label, value?.takeIf { it.isNotBlank() } ?: appText(RiderText.NotAvailableYet))
}

@Composable
private fun RiderMission.pickupSourceLabel(): String =
    when (pickupSource ?: "MERCHANT".takeIf { merchantSubOrderId != null }) {
        "SEQUO_CONSOLIDATION" -> appText(RiderText.PickupSourceSequo)
        "MERCHANT" -> appText(RiderText.PickupSourceMerchant)
        "RELAY_POINT" -> appText(RiderText.PickupSourceHub)
        else -> appText(RiderText.NotAvailableYet)
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

private val problemReportableStatuses = setOf(
    MissionStatus.ACCEPTED_BY_COURIER,
    MissionStatus.PICKED_UP_FROM_SELLER,
    MissionStatus.DEPOSITED_AT_RELAY,
)

private fun RiderMission.canConfirmDropoff(deliveryPin: String): Boolean =
    status == MissionStatus.PICKED_UP_FROM_SELLER && destinationType != MissionDestinationType.SEQUO_CONSOLIDATION &&
        (destinationType != MissionDestinationType.CUSTOMER_ADDRESS ||
            ProofRequirement.DELIVERY_PIN !in proofRequirements ||
            deliveryPin.length == MaxDeliveryPinLength)

private fun sanitizePickupText(input: String, maxLength: Int): String =
    input
        .filter { it.isAllowedPickupTextChar() }
        .take(maxLength)

private fun Char.isAllowedPickupTextChar(): Boolean =
    isLetterOrDigit() || isWhitespace() || this in ".,'!?;:-()/+"

private fun sanitizeCode(input: String, maxLength: Int): String =
    input.filter(Char::isDigit).take(maxLength)

private fun sanitizePackageCode(input: String): String =
    input.filter { it.isLetterOrDigit() || it in "-_" }.take(MaxPackageCodeLength)

private fun String.formatCount(count: Int, max: Int): String =
    replace("%1\$s", count.toString()).replace("%2\$s", max.toString())

private const val MaxCustodyNoteLength = 120
private const val MaxPickupReasonLength = 100
private const val MaxDeliveryPinLength = 6
private const val MaxPackageCodeLength = 32
private const val MaxLockerSlotLength = 24
