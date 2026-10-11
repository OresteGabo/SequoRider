package dev.orestegabo.sequorider.feature.settings

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
import dev.orestegabo.sequorider.model.RiderAvailability
import dev.orestegabo.sequorider.model.RiderProfile
import dev.orestegabo.sequorider.ui.components.RiderCard
import dev.orestegabo.sequorider.ui.components.RiderKeyValueRow
import dev.orestegabo.sequorider.ui.components.RiderStatusPill

@Composable
fun AccountSettingsScreen(
    profile: RiderProfile,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(appText(RiderText.AccountSettings), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            RiderCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(profile.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(appText(RiderText.LocalDemo), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RiderStatusPill(profile.availability.label())
                    }
                    RiderKeyValueRow(appText(RiderText.Fleet), profile.fleetClass.name)
                    RiderKeyValueRow(appText(RiderText.ServiceZone), profile.serviceZone)
                    RiderKeyValueRow(appText(RiderText.KycApproved), if (profile.kycApproved) appText(RiderText.Completed) else appText(RiderText.Pending))
                }
            }
        }
        item {
            RiderCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(appText(RiderText.Language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.FilterChip(
                            selected = language == AppLanguage.English,
                            onClick = { onLanguageChange(AppLanguage.English) },
                            label = { Text(appText(RiderText.English)) },
                            modifier = Modifier.weight(1f),
                        )
                        androidx.compose.material3.FilterChip(
                            selected = language == AppLanguage.French,
                            onClick = { onLanguageChange(AppLanguage.French) },
                            label = { Text(appText(RiderText.French)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    RiderKeyValueRow(appText(RiderText.Theme), appText(RiderText.System))
                }
            }
        }
    }
}

@Composable
private fun RiderAvailability.label(): String = when (this) {
    RiderAvailability.Available -> appText(RiderText.Available)
    RiderAvailability.OnMission -> appText(RiderText.OnMission)
    RiderAvailability.Offline -> appText(RiderText.Offline)
}
