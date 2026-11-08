package dev.orestegabo.sequorider.feature.auth

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import dev.orestegabo.sequorider.core.auth.DevelopmentRiderAccount
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import dev.orestegabo.sequorider.ui.components.RiderCard

@Composable
fun RiderAuthScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleLogin: () -> Unit,
    onLocalDemo: () -> Unit,
    fixtureAccounts: List<DevelopmentRiderAccount> = emptyList(),
    onFixtureLogin: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    RiderCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(appText(RiderText.AuthTitle), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(appText(RiderText.AuthDetail), color = MaterialTheme.colorScheme.onSurfaceVariant)
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = onGoogleLogin,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isLoading) appText(RiderText.SigningIn) else appText(RiderText.ContinueWithGoogle))
            }
            OutlinedButton(
                onClick = onLocalDemo,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(appText(RiderText.ContinueLocalDemo))
            }
            if (fixtureAccounts.isNotEmpty()) {
                Text(appText(RiderText.LocalFixtureAccounts), style = MaterialTheme.typography.titleSmall)
                Text(appText(RiderText.LocalFixtureDetail), style = MaterialTheme.typography.bodySmall)
                fixtureAccounts.forEach { account ->
                    OutlinedButton(onClick = { onFixtureLogin(account.key) }, enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()) { Text(account.label) }
                }
            }
            Text(appText(RiderText.TemporaryDevContext), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
