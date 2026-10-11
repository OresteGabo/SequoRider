package dev.orestegabo.sequorider.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.orestegabo.sequorider.ui.components.RiderKeyValueRow
import dev.orestegabo.sequorider.ui.components.RiderMetric
import dev.orestegabo.sequorider.ui.components.RiderStatusPill

@Composable
fun HistoryScreen(
    missions: List<RiderMission>,
    isLoading: Boolean,
    isOffline: Boolean,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFilter by remember { mutableStateOf(HistoryFilter.All) }
    val historyMissions = missions.filter { it.status in historyStatuses }
    val filteredMissions = historyMissions.filter { it.status in selectedFilter.statuses }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(appText(RiderText.MissionHistory), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onRefresh, enabled = !isLoading) {
                        Text(appText(RiderText.Refresh))
                    }
                }
                Text(
                    when {
                        isLoading -> appText(RiderText.LoadingMissions)
                        isOffline -> appText(RiderText.OfflineMissions)
                        else -> appText(RiderText.HistoryLocalDemo)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            RiderCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RiderMetric(appText(RiderText.Delivered), historyMissions.count { it.status.isCompletedHistory() }.toString(), Modifier.weight(1f))
                        RiderMetric(appText(RiderText.Failed), historyMissions.count { it.status.isProblemHistory() }.toString(), Modifier.weight(1f))
                    }
                    RiderKeyValueRow(appText(RiderText.Returned), "0")
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryFilter.entries.forEach { filter ->
                    item {
                        FilterChip(
                            selected = filter == selectedFilter,
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
                        Text(appText(RiderText.HistoryEmpty), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            HistoryMissionCard(filteredMissions[index])
        }
    }
}

@Composable
private fun HistoryMissionCard(mission: RiderMission) {
    RiderCard {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(mission.code, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("${mission.pickupArea} -> ${mission.dropoffArea}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RiderStatusPill(mission.status.name)
            }
            RiderKeyValueRow(appText(RiderText.PickupStops), mission.pickupStops.joinToString { it.title })
            RiderKeyValueRow(appText(RiderText.DropoffStop), mission.dropoffStop.title)
            RiderKeyValueRow(appText(RiderText.CompletionTime), mission.completionTime())
            RiderKeyValueRow(appText(RiderText.IssueStatus), mission.problemMetadata?.takeIf { it.isNotBlank() } ?: mission.status.issueLabel())
            RiderKeyValueRow(appText(RiderText.PayoutPreview), mission.expectedPayoutCfa?.formatCfa() ?: appText(RiderText.NotAvailableYet))
        }
    }
}

private enum class HistoryFilter(val statuses: Set<MissionStatus>) {
    All(historyStatuses),
    Completed(setOf(MissionStatus.DELIVERED_TO_CUSTOMER, MissionStatus.RELEASED_BY_RELAY)),
    Problems(setOf(MissionStatus.PROBLEM_REPORTED)),
    Cancelled(setOf(MissionStatus.CANCELLED)),
}

@Composable
private fun HistoryFilter.label(): String =
    when (this) {
        HistoryFilter.All -> appText(RiderText.HistoryFilterAll)
        HistoryFilter.Completed -> appText(RiderText.HistoryFilterCompleted)
        HistoryFilter.Problems -> appText(RiderText.HistoryFilterProblems)
        HistoryFilter.Cancelled -> appText(RiderText.HistoryFilterCancelled)
    }

private fun MissionStatus.isCompletedHistory(): Boolean =
    this == MissionStatus.DELIVERED_TO_CUSTOMER || this == MissionStatus.RELEASED_BY_RELAY

private fun MissionStatus.isProblemHistory(): Boolean =
    this == MissionStatus.PROBLEM_REPORTED || this == MissionStatus.CANCELLED

@Composable
private fun MissionStatus.issueLabel(): String =
    when {
        isCompletedHistory() -> appText(RiderText.Completed)
        isProblemHistory() -> appText(RiderText.Failed)
        else -> appText(RiderText.Pending)
    }

private fun RiderMission.completionTime(): String =
    deliveredAt ?: relayDepositedAt ?: updatedAt ?: createdAt ?: "-"

private val historyStatuses = setOf(
    MissionStatus.DELIVERED_TO_CUSTOMER,
    MissionStatus.RELEASED_BY_RELAY,
    MissionStatus.PROBLEM_REPORTED,
    MissionStatus.CANCELLED,
)
