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
import dev.orestegabo.sequorider.model.PayoutEntryStatus
import dev.orestegabo.sequorider.model.PayoutHistoryItem
import dev.orestegabo.sequorider.model.PayoutSummary
import dev.orestegabo.sequorider.model.formatCfa
import dev.orestegabo.sequorider.ui.components.RiderCard
import dev.orestegabo.sequorider.ui.components.RiderKeyValueRow
import dev.orestegabo.sequorider.ui.components.RiderMetric

@Composable
fun PayoutScreen(
    summary: PayoutSummary?,
    history: List<PayoutHistoryItem>,
    isLoading: Boolean,
    isOffline: Boolean,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    accessDenied: Boolean = false,
    isDemo: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val actual = summary.takeUnless { isLoading || isOffline }
    LazyColumn(modifier = modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(appText(RiderText.PayoutVisibility), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(appText(RiderText.DigitalOnly), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isDemo) Text(appText(RiderText.PayoutLocalDemo))
                Text(when {
                    isLoading -> appText(RiderText.PayoutLoading)
                    accessDenied -> appText(RiderText.PayoutAccessDenied)
                    isOffline -> appText(RiderText.PayoutOffline)
                    else -> actual?.message ?: appText(RiderText.NotAvailableYet)
                }, color = if (isOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(appText(RiderText.NoPayoutExecution), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onRefresh, enabled = !isLoading) { Text(appText(RiderText.Refresh)) }
                    if (isOffline) Button(onClick = onRetry, enabled = !isLoading) { Text(appText(RiderText.Retry)) }
                }
            }
        }
        if (actual != null) {
            if (!actual.payable) {
                val workforceCopy = if (actual.workforceType?.uppercase() in setOf("SEQUO_SALARIED", "SEQUOSALARIED", "SALARIED")) {
                    RiderText.SalaryHandledOutsideSplits
                } else RiderText.WorkforceUnknown
                item { RiderCard { Text(appText(workforceCopy), Modifier.padding(14.dp)) } }
            }
            if (actual.payable || (actual.earnedTotalCfa ?: 0) > 0 || actual.availableCfa > 0 || actual.onHoldCfa > 0) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RiderMetric(appText(RiderText.RecordedEarnings), actual.earnedTotalCfa?.formatCfa() ?: appText(RiderText.NotAvailableYet), Modifier.weight(1f))
                        RiderMetric(appText(RiderText.MissionsWithRecordedPay), actual.settledMissionCount?.toString() ?: appText(RiderText.NotAvailableYet), Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RiderMetric(appText(RiderText.RecordedRemainingPay), actual.availableCfa.formatCfa(), Modifier.weight(1f))
                        RiderMetric(appText(RiderText.HeldRecordedPay), actual.onHoldCfa.formatCfa(), Modifier.weight(1f))
                    }
                }
            }
            actual.reviewRequiredCfa?.let { amount ->
                item { RiderMetric(appText(RiderText.ReviewRequiredAmount), amount.formatCfa(), Modifier.fillMaxWidth()) }
            }
            item {
                RiderCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(appText(if (actual.transferStatus == "NOT_INTEGRATED") RiderText.TransferNotIntegrated else RiderText.TransferVerificationUnavailable))
                        RiderKeyValueRow(appText(RiderText.ConfirmedTransfers), actual.paidCfa?.formatCfa() ?: appText(RiderText.NotAvailableYet))
                        RiderKeyValueRow(appText(RiderText.ProcessingTransfers), actual.processingCfa?.formatCfa() ?: appText(RiderText.NotAvailableYet))
                        RiderKeyValueRow(appText(RiderText.UnverifiedTransfers), actual.unverifiedTransferCfa?.formatCfa() ?: appText(RiderText.NotAvailableYet))
                    }
                }
            }
            item { Text(appText(RiderText.PayoutHistory), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (history.isEmpty()) {
                item { RiderCard { Text(appText(RiderText.PayoutHistoryEmpty), Modifier.padding(14.dp)) } }
            } else {
                history.forEach { record ->
                    item(key = record.id) {
                        RiderCard {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                RiderKeyValueRow(appText(RiderText.SourceReference), record.missionId)
                                RiderKeyValueRow(appText(RiderText.RecordedAmount), record.amountCfa.formatCfa())
                                RiderKeyValueRow(appText(RiderText.RecordedPayImpact), record.recordedPayImpactCfa?.formatCfa() ?: appText(RiderText.NotAvailableYet))
                                Text(appText(RiderText.Status) + ": " + appText(when (record.status) {
                                    PayoutEntryStatus.AVAILABLE -> RiderText.LedgerEntryAvailable
                                    PayoutEntryStatus.ON_HOLD -> RiderText.LedgerEntryHold
                                    PayoutEntryStatus.CORRECTED -> RiderText.LedgerEntryCorrection
                                    PayoutEntryStatus.REVIEW_REQUIRED -> RiderText.LedgerEntryReview
                                    PayoutEntryStatus.PAID -> RiderText.LedgerEntryRecordedPaid
                                    PayoutEntryStatus.PROCESSING -> RiderText.LedgerEntryRecordedProcessing
                                    PayoutEntryStatus.NOT_APPLICABLE -> RiderText.LedgerEntryNotApplicable
                                }))
                                RiderKeyValueRow(appText(RiderText.HistoricalWorkforce), appText(when (record.workforceType) {
                                    "Freelancer" -> RiderText.WorkforceFreelancer
                                    "SequoSalaried" -> RiderText.WorkforceSalaried
                                    else -> RiderText.WorkforceUnknown
                                }))
                                Text(record.createdAt, style = MaterialTheme.typography.bodySmall)
                                Text(record.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
