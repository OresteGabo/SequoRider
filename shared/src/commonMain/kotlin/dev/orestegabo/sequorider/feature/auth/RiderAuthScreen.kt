package dev.orestegabo.sequorider.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.orestegabo.sequorider.feature.settings.RiderText
import dev.orestegabo.sequorider.feature.settings.appText
import dev.orestegabo.sequorider.ui.components.RiderCard

@Composable
fun RiderAuthScreen(modifier: Modifier = Modifier) {
    RiderCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(appText(RiderText.AppName), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(appText(RiderText.NoServerActions), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
