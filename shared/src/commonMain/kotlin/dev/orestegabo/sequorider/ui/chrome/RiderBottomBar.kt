package dev.orestegabo.sequorider.ui.chrome

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.orestegabo.sequo.design.SequoNavDestination
import dev.orestegabo.sequo.design.SequoNavigationBar
import dev.orestegabo.sequorider.feature.settings.RiderText
import dev.orestegabo.sequorider.feature.settings.appText
import dev.orestegabo.sequorider.model.RiderSection

@Composable
fun RiderBottomBar(
    currentSection: RiderSection,
    onSectionSelected: (RiderSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    SequoNavigationBar(
        destinations = RiderSection.entries.map { section ->
            SequoNavDestination(section.name, section.navigationLabel(), section.navigationIcon())
        },
        selectedKey = currentSection.name,
        onSelect = { key -> RiderSection.entries.firstOrNull { it.name == key }?.let(onSectionSelected) },
        modifier = modifier,
    )
}

@Composable
fun RiderSection.label(): String = when (this) {
    RiderSection.Missions -> appText(RiderText.Missions)
    RiderSection.ActiveRoute -> appText(RiderText.ActiveRoute)
    RiderSection.History -> appText(RiderText.History)
    RiderSection.Payout -> appText(RiderText.Payout)
    RiderSection.Account -> appText(RiderText.Account)
}

/** Compact tab labels prevent a word splitting across lines; full screen titles remain unchanged. */
@Composable
private fun RiderSection.navigationLabel(): String = when (this) {
    RiderSection.ActiveRoute -> appText(RiderText.NavRoute)
    RiderSection.History -> appText(RiderText.NavHistory)
    else -> label()
}
