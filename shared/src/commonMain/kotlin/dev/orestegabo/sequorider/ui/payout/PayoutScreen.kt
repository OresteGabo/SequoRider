package dev.orestegabo.sequorider.ui.payout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import dev.orestegabo.sequorider.model.PayoutSummary
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(appText(RiderText.PayoutVisibility), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(appText(RiderText.DigitalOnly), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (profile.fleetClass == RiderFleetClass.InternalDriver) {
            item {
                RiderCard {
                    Text(
                        appText(RiderText.NoServerActions),
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RiderMetric(appText(RiderText.AvailableBalance), summary.availableCfa.formatCfa(), Modifier.weight(1f))
                    RiderMetric(appText(RiderText.PendingReview), summary.pendingReviewCfa.formatCfa(), Modifier.weight(1f))
                }
            }
            item {
                RiderCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        RiderKeyValueRow(appText(RiderText.OnHold), summary.onHoldCfa.formatCfa())
                        RiderKeyValueRow(appText(RiderText.LastPayout), summary.lastPayoutCfa.formatCfa())
                        RiderKeyValueRow("Provider", profile.payoutProvider.orEmpty())
                        RiderKeyValueRow("Status", summary.status.name)
                    }
                }
            }
        }
    }
}
