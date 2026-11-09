package dev.orestegabo.sequorider.feature.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import dev.orestegabo.sequo.design.sequoBrandLogoPainter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.orestegabo.sequorider.feature.settings.RiderText
import dev.orestegabo.sequorider.feature.settings.appText

@Composable
fun RiderSplash(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(sequoBrandLogoPainter(), contentDescription = "SEQUO", modifier = Modifier.width(240.dp).height(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(appText(RiderText.AppName), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(appText(RiderText.AppSubtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
