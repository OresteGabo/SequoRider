package dev.orestegabo.sequorider.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.orestegabo.sequorider.core.auth.AdminLoginRequestSummary
import dev.orestegabo.sequorider.feature.settings.RiderText
import dev.orestegabo.sequorider.feature.settings.appText

@Composable
fun AdminLoginRequestsScreen(
    requests: List<AdminLoginRequestSummary>,
    isLoading: Boolean,
    hasError: Boolean,
    onRefresh: () -> Unit,
    onApprove: (AdminLoginRequestSummary) -> Unit,
    onReject: (AdminLoginRequestSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(appText(RiderText.AdminLoginRequests), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(appText(RiderText.AdminLoginWarning), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (hasError) Text(appText(RiderText.AdminLoginError), color = MaterialTheme.colorScheme.error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onRefresh, enabled = !isLoading) { Text(appText(RiderText.AdminLoginRefresh)) }
                    if (isLoading) CircularProgressIndicator()
                }
            }
        }

        if (!isLoading && requests.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text(appText(RiderText.AdminLoginEmpty), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        items(requests, key = { it.requestId }) { request ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(request.code, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    request.deviceLabel?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                    request.browserInfo?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text("${appText(RiderText.AdminLoginRequested)} ${request.requestedAt}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${appText(RiderText.AdminLoginExpires)} ${request.expiresAt}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onApprove(request) }) { Text(appText(RiderText.AdminLoginApprove)) }
                        OutlinedButton(onClick = { onReject(request) }) { Text(appText(RiderText.AdminLoginReject)) }
                    }
                }
            }
        }
    }
}
