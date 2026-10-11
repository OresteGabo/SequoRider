package dev.orestegabo.sequorider.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.orestegabo.sequorider.feature.settings.RiderText
import dev.orestegabo.sequorider.feature.settings.appText
import dev.orestegabo.sequorider.model.MissionStatus
import dev.orestegabo.sequorider.model.RiderMission
import dev.orestegabo.sequorider.ui.components.RiderCard
import dev.orestegabo.sequorider.ui.components.RiderKeyValueRow

@Composable
fun HistoryScreen(
    missions: List<RiderMission>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(appText(RiderText.MissionHistory), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            RiderCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RiderKeyValueRow(appText(RiderText.Delivered), missions.count { it.status == MissionStatus.DELIVERED_TO_CUSTOMER || it.status == MissionStatus.RELEASED_BY_RELAY }.toString())
                    RiderKeyValueRow(appText(RiderText.Failed), missions.count { it.status == MissionStatus.PROBLEM_REPORTED || it.status == MissionStatus.CANCELLED }.toString())
                    RiderKeyValueRow(appText(RiderText.Returned), "0")
                }
            }
        }
        items(missions.size) { index ->
            val mission = missions[index]
            RiderCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RiderKeyValueRow(mission.code, mission.status.name)
                    Text("${mission.pickupArea} -> ${mission.dropoffArea}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
