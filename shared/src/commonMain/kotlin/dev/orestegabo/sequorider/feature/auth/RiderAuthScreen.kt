package dev.orestegabo.sequorider.feature.auth

import dev.orestegabo.sequo.design.sequoBrandSymbolPainter
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import dev.orestegabo.sequorider.core.auth.DevelopmentRiderAccount
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import dev.orestegabo.sequo.design.SequoActionButton as Button
import androidx.compose.material3.MaterialTheme
import dev.orestegabo.sequo.design.SequoOutlineButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.orestegabo.sequo.design.SequoDesign
import dev.orestegabo.sequorider.feature.settings.RiderText
import dev.orestegabo.sequorider.feature.settings.appText
import dev.orestegabo.sequorider.ui.components.RiderCard
import org.jetbrains.compose.resources.painterResource
import sequorider.shared.generated.resources.Res
@Composable
fun RiderAuthScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleLogin: () -> Unit,
    onLocalDemo: () -> Unit,
    testAccountsAvailable: suspend () -> Boolean = { false },
    onTestAccountLogin: suspend (String,String) -> Unit = { _,_ -> },
    fixtureAccounts: List<DevelopmentRiderAccount> = emptyList(),
    onFixtureLogin: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    RiderCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(SequoDesign.CardPadding), verticalArrangement = Arrangement.spacedBy(SequoDesign.SectionGap)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(sequoBrandSymbolPainter(), contentDescription = null, modifier = Modifier.size(48.dp))
                Text(appText(RiderText.AppName), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
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
            TestAccountLoginEntry(testAccountsAvailable, onTestAccountLogin,
                dev.orestegabo.sequorider.feature.settings.LocalAppLanguage.current == dev.orestegabo.sequorider.feature.settings.AppLanguage.French,
                Modifier.fillMaxWidth())
            if (fixtureAccounts.isNotEmpty()) {
                Text(appText(RiderText.LocalFixtureAccounts), style = MaterialTheme.typography.titleSmall)
                Text(appText(RiderText.LocalFixtureDetail), style = MaterialTheme.typography.bodySmall)
                fixtureAccounts.forEach { account ->
                    OutlinedButton(onClick = { onFixtureLogin(account.key) }, enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()) { Text(account.label) }
                }
            }
        }
    }
}
