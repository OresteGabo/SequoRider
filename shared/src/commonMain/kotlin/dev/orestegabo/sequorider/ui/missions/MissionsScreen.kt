package dev.orestegabo.sequorider.ui.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.orestegabo.sequorider.feature.settings.RiderText
import dev.orestegabo.sequorider.feature.settings.appText
import dev.orestegabo.sequorider.model.MissionStatus
import dev.orestegabo.sequorider.model.RiderMission
import dev.orestegabo.sequorider.model.formatCfa
import dev.orestegabo.sequorider.ui.components.RiderCard
import dev.orestegabo.sequorider.ui.components.RiderMetric
import dev.orestegabo.sequorider.ui.components.RiderStatusPill

@Composable
fun MissionsScreen(
    missions: List<RiderMission>,
    isLoading: Boolean,
    isOffline: Boolean,
    actionInProgressMissionId: String?,
    actionFailed: Boolean,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onAcceptMission: (RiderMission) -> Unit,
    onRouteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeOffers = missions.count { it.status == MissionStatus.OFFERED_TO_COURIER }
    var selectedFilter by remember { mutableStateOf(MissionFilter.Available) }
    val filteredMissions = missions.filter { it.status in selectedFilter.statuses }
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(appText(RiderText.Missions), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onRefresh, enabled = !isLoading) {
                        Text(appText(RiderText.Refresh))
                    }
                }
                Text(
                    when {
                        isLoading -> appText(RiderText.LoadingMissions)
                        isOffline -> appText(RiderText.OfflineMissions)
                        else -> appText(RiderText.NoServerActions)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (actionFailed) {
                    Text(appText(RiderText.MissionActionFailed), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RiderMetric(appText(RiderText.TodayMissions), missions.size.toString(), Modifier.weight(1f))
                RiderMetric(appText(RiderText.ActiveOffers), activeOffers.toString(), Modifier.weight(1f))
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MissionFilter.entries.forEach { filter ->
                    item {
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label()) },
                        )
                    }
                }
            }
        }
        if (!isLoading && filteredMissions.isEmpty()) {
            item {
                RiderCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(appText(RiderText.EmptyMissions), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (isOffline) {
                            OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                                Text(appText(RiderText.Retry))
                            }
                        }
                    }
                }
            }
        }
        items(filteredMissions.size) { index ->
            MissionCard(
                mission = filteredMissions[index],
                actionInProgress = actionInProgressMissionId == filteredMissions[index].id,
                onAcceptMission = onAcceptMission,
                onRouteClick = onRouteClick,
            )
        }
    }
}

@Composable
private fun MissionCard(
    mission: RiderMission,
    actionInProgress: Boolean,
    onAcceptMission: (RiderMission) -> Unit,
    onRouteClick: () -> Unit,
) {
    RiderCard {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(mission.code, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(mission.pickupName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${mission.pickupArea} -> ${mission.dropoffArea}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RiderStatusPill(mission.status.name)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RiderMetric(appText(RiderText.MissionType), mission.serviceType.name, Modifier.weight(1f))
                RiderMetric(appText(RiderText.Status), mission.status.name, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RiderMetric(appText(RiderText.PickupCount), mission.pickupStops.size.toString(), Modifier.weight(1f))
                RiderMetric(appText(RiderText.DropoffCount), "1", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RiderMetric(appText(RiderText.Distance), mission.distanceKm.ifBlank { appText(RiderText.NotAvailableYet) }, Modifier.weight(1f))
                RiderMetric(appText(RiderText.EtaSla), mission.etaLabel(), Modifier.weight(1f))
            }
            RiderMetric(appText(RiderText.Payout), mission.expectedPayoutCfa?.formatCfa() ?: appText(RiderText.NotAvailableYet), Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (mission.status == MissionStatus.OFFERED_TO_COURIER) {
                    Button(
                        onClick = { onAcceptMission(mission) },
                        enabled = !actionInProgress,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(appText(RiderText.AcceptMission))
                    }
                } else {
                    Button(onClick = onRouteClick, enabled = !actionInProgress, modifier = Modifier.weight(1f)) {
                        Text(appText(RiderText.ViewRoute))
                    }
                }
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f), enabled = false) {
                    Text(appText(RiderText.ActionUnavailable))
                }
            }
        }
    }
}

private enum class MissionFilter(val statuses: Set<MissionStatus>) {
    Available(setOf(MissionStatus.OFFERED_TO_COURIER, MissionStatus.CREATED)),
    Assigned(setOf(MissionStatus.ACCEPTED_BY_COURIER)),
    Active(setOf(MissionStatus.PICKED_UP_FROM_SELLER, MissionStatus.DEPOSITED_AT_RELAY)),
    Completed(setOf(MissionStatus.DELIVERED_TO_CUSTOMER, MissionStatus.RELEASED_BY_RELAY)),
    Problem(setOf(MissionStatus.PROBLEM_REPORTED, MissionStatus.CANCELLED)),
}

@Composable
private fun MissionFilter.label(): String =
    when (this) {
        MissionFilter.Available -> appText(RiderText.MissionFilterAvailable)
        MissionFilter.Assigned -> appText(RiderText.MissionFilterAssigned)
        MissionFilter.Active -> appText(RiderText.MissionFilterActive)
        MissionFilter.Completed -> appText(RiderText.MissionFilterCompleted)
        MissionFilter.Problem -> appText(RiderText.MissionFilterProblem)
    }

@Composable
private fun RiderMission.etaLabel(): String =
    when {
        etaMinutes > 0 && slaLabel.isNotBlank() -> "${etaMinutes} min / $slaLabel"
        etaMinutes > 0 -> "${etaMinutes} min"
        slaLabel.isNotBlank() -> slaLabel
        else -> appText(RiderText.NotAvailableYet)
    }
