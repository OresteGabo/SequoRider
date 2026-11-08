package dev.orestegabo.sequorider.ui.navigation

import androidx.compose.runtime.Composable
import dev.orestegabo.sequorider.model.RiderSection

internal fun riderBackDestination(section: RiderSection): RiderSection? =
    if (section == RiderSection.Missions) null else RiderSection.Missions

@Composable
internal expect fun RiderBackHandler(enabled: Boolean, onBack: () -> Unit)
