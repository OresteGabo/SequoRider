package dev.orestegabo.sequorider.ui.payout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.orestegabo.sequorider.feature.settings.RiderText
import dev.orestegabo.sequorider.feature.settings.appText
import dev.orestegabo.sequorider.model.PayoutHistoryItem
import dev.orestegabo.sequorider.model.PayoutSummary
import dev.orestegabo.sequorider.model.MissionStatus
import dev.orestegabo.sequorider.model.RiderMission
import dev.orestegabo.sequorider.model.RiderFleetClass
import dev.orestegabo.sequorider.model.RiderProfile
import dev.orestegabo.sequorider.model.formatCfa
import dev.orestegabo.sequorider.ui.components.RiderCard
import dev.orestegabo.sequorider.ui.components.RiderKeyValueRow
import dev.orestegabo.sequorider.ui.components.RiderMetric

@Composable
fun PayoutScreen(
    profile: RiderProfile,
    summary: PayoutSummary,
    history: List<PayoutHistoryItem>,
    missions: List<RiderMission>,
    isLoading: Boolean,
    isOffline: Boolean,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val completedMissions = missions.filter { it.status == MissionStatus.DELIVERED_TO_CUSTOMER || it.status == MissionStatus.RELEASED_BY_RELAY }
    val currentPeriodEarnings = completedMissions.sumOf { it.expectedPayoutCfa ?: 0 }
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(appText(RiderText.PayoutVisibility), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(appText(RiderText.DigitalOnly), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    when {
                        isLoading -> appText(RiderText.PayoutLoading)
                        isOffline -> appText(RiderText.PayoutOffline)
                        else -> summary.message ?: appText(RiderText.PayoutLocalDemo)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onRefresh) {
                        Text(appText(RiderText.Refresh))
                    }
                    if (isOffline) {
                        Button(onClick = onRetry) {
                            Text(appText(RiderText.Retry))
                        }
                    }
                }
            }
        }
        if (profile.fleetClass == RiderFleetClass.InternalDriver || !summary.payable) {
            item {
                RiderCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(summary.message ?: appText(RiderText.SalaryHandledOutsideSplits), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        RiderKeyValueRow(appText(RiderText.CompletedDeliveries), completedMissions.size.toString())
                    }
                }
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RiderMetric(appText(RiderText.CurrentPeriodEarnings), currentPeriodEarnings.formatCfa(), Modifier.weight(1f))
                    RiderMetric(appText(RiderText.CompletedDeliveries), completedMissions.size.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RiderMetric(appText(RiderText.PendingPayout), summary.availableCfa.formatCfa(), Modifier.weight(1f))
                    RiderMetric(appText(RiderText.HeldProblemPayout), summary.onHoldCfa.formatCfa(), Modifier.weight(1f))
                }
            }
            item {
                RiderMetric(appText(RiderText.PaidOrProcessing), summary.paidOrProcessingCfa.formatCfa(), Modifier.fillMaxWidth())
            }
            item {
                RiderCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        RiderKeyValueRow(appText(RiderText.PendingReview), summary.pendingReviewCfa.formatCfa())
                        RiderKeyValueRow(appText(RiderText.LastPayout), summary.lastPayoutCfa.formatCfa())
                        RiderKeyValueRow(appText(RiderText.PayoutMethod), profile.payoutProvider.orEmpty().ifBlank { appText(RiderText.NotAvailableYet) })
                        RiderKeyValueRow(appText(RiderText.PayoutStatus), summary.status.name)
                        Text(appText(RiderText.NoPayoutExecution), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Text(appText(RiderText.PayoutHistory), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (history.isEmpty()) {
            item {
                RiderCard {
                    Text(
                        appText(RiderText.PayoutHistoryEmpty),
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            history.forEach { payout ->
                item {
                    RiderCard {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RiderKeyValueRow(appText(RiderText.MissionId), payout.missionId)
                            RiderKeyValueRow(appText(RiderText.PayableAmount), payout.payableAmountCfa.formatCfa())
                            RiderKeyValueRow(appText(RiderText.Status), payout.status.name)
                            Text(payout.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
