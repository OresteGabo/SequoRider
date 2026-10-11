package dev.orestegabo.sequorider.ui.missions

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
import dev.orestegabo.sequorider.model.MissionStatus
import dev.orestegabo.sequorider.model.RiderMission
import dev.orestegabo.sequorider.model.formatCfa
import dev.orestegabo.sequorider.ui.components.RiderCard
import dev.orestegabo.sequorider.ui.components.RiderMetric
import dev.orestegabo.sequorider.ui.components.RiderStatusPill

@Composable
fun MissionsScreen(
    missions: List<RiderMission>,
    onRouteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeOffers = missions.count { it.status == MissionStatus.OFFERED_TO_COURIER }
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(appText(RiderText.Missions), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(appText(RiderText.NoServerActions), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RiderMetric(appText(RiderText.TodayMissions), missions.size.toString(), Modifier.weight(1f))
                RiderMetric(appText(RiderText.ActiveOffers), activeOffers.toString(), Modifier.weight(1f))
            }
        }
        items(missions.size) { index ->
            MissionCard(mission = missions[index], onRouteClick = onRouteClick)
        }
    }
}

@Composable
private fun MissionCard(
    mission: RiderMission,
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
                RiderMetric(appText(RiderText.NextPickup), "${mission.etaMinutes} min", Modifier.weight(1f))
                RiderMetric(appText(RiderText.Payout), mission.expectedPayoutCfa?.formatCfa().orEmpty(), Modifier.weight(1f))
            }
            Text(mission.slaLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onRouteClick, modifier = Modifier.weight(1f)) {
                    Text(appText(RiderText.ViewRoute))
                }
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f), enabled = false) {
                    Text(appText(RiderText.Support))
                }
            }
        }
    }
}
