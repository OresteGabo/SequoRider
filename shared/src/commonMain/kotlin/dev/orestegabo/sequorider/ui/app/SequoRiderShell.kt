package dev.orestegabo.sequorider.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.orestegabo.sequorider.core.auth.DemoRiderSessionRepository
import dev.orestegabo.sequorider.core.missions.DemoMissionRepository
import dev.orestegabo.sequorider.core.payout.DemoPayoutRepository
import dev.orestegabo.sequorider.feature.settings.AccountSettingsScreen
import dev.orestegabo.sequorider.feature.settings.AppLanguage
import dev.orestegabo.sequorider.feature.settings.LocalAppLanguage
import dev.orestegabo.sequorider.model.RiderSection
import dev.orestegabo.sequorider.ui.chrome.RiderBottomBar
import dev.orestegabo.sequorider.ui.chrome.label
import dev.orestegabo.sequorider.ui.history.HistoryScreen
import dev.orestegabo.sequorider.ui.missions.MissionsScreen
import dev.orestegabo.sequorider.ui.payout.PayoutScreen
import dev.orestegabo.sequorider.ui.route.ActiveRouteScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SequoRiderShell(
    modifier: Modifier = Modifier,
) {
    var currentSection by remember { mutableStateOf(RiderSection.Missions) }
    var language by remember { mutableStateOf(AppLanguage.English) }
    val sessionRepository = remember { DemoRiderSessionRepository() }
    val missionRepository = remember { DemoMissionRepository() }
    val payoutRepository = remember { DemoPayoutRepository() }
    val profile = remember { sessionRepository.currentProfile() }
    val missions = remember { missionRepository.missions() }
    val routePlan = remember { missionRepository.activeRoute() }
    val payoutSummary = remember { payoutRepository.summary() }

    CompositionLocalProvider(LocalAppLanguage provides language) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            currentSection.label(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
            bottomBar = {
                RiderBottomBar(currentSection = currentSection, onSectionSelected = { currentSection = it })
            },
        ) { paddingValues ->
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(paddingValues)) {
                    when (currentSection) {
                        RiderSection.Missions -> MissionsScreen(
                            missions = missions,
                            onRouteClick = { currentSection = RiderSection.ActiveRoute },
                            modifier = Modifier.fillMaxSize(),
                        )
                        RiderSection.ActiveRoute -> ActiveRouteScreen(routePlan = routePlan, modifier = Modifier.fillMaxSize())
                        RiderSection.History -> HistoryScreen(missions = missions, modifier = Modifier.fillMaxSize())
                        RiderSection.Payout -> PayoutScreen(profile = profile, summary = payoutSummary, modifier = Modifier.fillMaxSize())
                        RiderSection.Account -> AccountSettingsScreen(
                            profile = profile,
                            language = language,
                            onLanguageChange = { language = it },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
