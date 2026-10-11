package dev.orestegabo.sequorider.ui.chrome

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.orestegabo.sequorider.feature.settings.RiderText
import dev.orestegabo.sequorider.feature.settings.appText
import dev.orestegabo.sequorider.model.RiderSection

@Composable
fun RiderBottomBar(
    currentSection: RiderSection,
    onSectionSelected: (RiderSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = Color.Transparent,
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colorScheme.surface.copy(alpha = 0.98f),
                            colorScheme.surfaceContainer.copy(alpha = 0.94f),
                        ),
                    ),
                )
                .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.45f), MaterialTheme.shapes.extraLarge),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RiderSection.entries.forEach { section ->
                    RiderBottomBarItem(
                        section = section,
                        selected = section == currentSection,
                        onClick = { onSectionSelected(section) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RiderBottomBarItem(
    section: RiderSection,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(if (selected) colorScheme.primaryContainer else Color.Transparent)
    val contentColor by animateColorAsState(if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant)
    Surface(
        onClick = onClick,
        color = containerColor,
        shape = MaterialTheme.shapes.large,
        tonalElevation = if (selected) 3.dp else 0.dp,
        modifier = modifier,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (selected) colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(section.shortLabel(), color = contentColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                section.label(),
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun RiderSection.label(): String = when (this) {
    RiderSection.Missions -> appText(RiderText.Missions)
    RiderSection.ActiveRoute -> appText(RiderText.ActiveRoute)
    RiderSection.History -> appText(RiderText.History)
    RiderSection.Payout -> appText(RiderText.Payout)
    RiderSection.Account -> appText(RiderText.Account)
}

private fun RiderSection.shortLabel(): String = when (this) {
    RiderSection.Missions -> "M"
    RiderSection.ActiveRoute -> "R"
    RiderSection.History -> "H"
    RiderSection.Payout -> "P"
    RiderSection.Account -> "A"
}
