package dev.orestegabo.sequorider.ui.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.orestegabo.sequorider.feature.settings.RiderText
import dev.orestegabo.sequorider.feature.settings.appText
import dev.orestegabo.sequorider.model.RiderRoutePlan
import dev.orestegabo.sequorider.ui.components.RiderCard
import dev.orestegabo.sequorider.ui.components.RiderKeyValueRow
import dev.orestegabo.sequorider.ui.components.RiderMetric

@Composable
fun ActiveRouteScreen(
    routePlan: RiderRoutePlan,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(appText(RiderText.ActiveRoute), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(routePlan.headline, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            RiderCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(appText(RiderText.RouteSummary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RiderMetric(appText(RiderText.Stops), routePlan.stops.size.toString(), Modifier.weight(1f))
                        RiderMetric("ETA", "${routePlan.estimatedDurationMin} min", Modifier.weight(1f))
                    }
                    RiderKeyValueRow("Distance", routePlan.totalDistanceKm)
                    RiderKeyValueRow("Type", routePlan.routeType)
                }
            }
        }
        item {
            RiderCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(appText(RiderText.Stops), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    routePlan.stops.forEach { stop ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        RiderKeyValueRow("${stop.sequence}. ${stop.title}", if (stop.completed) appText(RiderText.Completed) else appText(RiderText.Pending))
                        Text(stop.area, style = MaterialTheme.typography.bodyMedium)
                        Text(stop.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
