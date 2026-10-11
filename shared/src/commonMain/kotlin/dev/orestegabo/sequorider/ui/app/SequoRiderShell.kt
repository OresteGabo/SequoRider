package dev.orestegabo.sequorider.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.orestegabo.sequorider.core.auth.AuthApiClient
import dev.orestegabo.sequorider.core.auth.AuthApiException
import dev.orestegabo.sequorider.core.auth.AuthRepository
import dev.orestegabo.sequorider.core.auth.AuthUserMessage
import dev.orestegabo.sequorider.core.auth.CurrentUser
import dev.orestegabo.sequorider.core.auth.DemoRiderSessionRepository
import dev.orestegabo.sequorider.core.auth.GoogleSignInFailureReason
import dev.orestegabo.sequorider.core.auth.GoogleSignInResult
import dev.orestegabo.sequorider.core.auth.SecureSessionCache
import dev.orestegabo.sequorider.core.auth.rememberAuthDeviceId
import dev.orestegabo.sequorider.core.auth.rememberAuthSessionStore
import dev.orestegabo.sequorider.core.auth.rememberSecureTokenStorage
import dev.orestegabo.sequorider.core.missions.DemoMissionRepository
import dev.orestegabo.sequorider.core.payout.DemoPayoutRepository
import dev.orestegabo.sequorider.feature.auth.RiderAuthScreen
import dev.orestegabo.sequorider.feature.settings.AccountSettingsScreen
import dev.orestegabo.sequorider.feature.settings.AppLanguage
import dev.orestegabo.sequorider.feature.settings.LocalAppLanguage
import dev.orestegabo.sequorider.feature.settings.RiderText
import dev.orestegabo.sequorider.feature.settings.appText
import dev.orestegabo.sequorider.model.DemoRiderData
import dev.orestegabo.sequorider.model.RiderMission
import dev.orestegabo.sequorider.model.RiderSection
import dev.orestegabo.sequorider.ui.chrome.RiderBottomBar
import dev.orestegabo.sequorider.ui.chrome.label
import dev.orestegabo.sequorider.ui.history.HistoryScreen
import dev.orestegabo.sequorider.ui.missions.MissionsScreen
import dev.orestegabo.sequorider.ui.payout.PayoutScreen
import dev.orestegabo.sequorider.ui.route.ActiveRouteScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SequoRiderShell(
    onGoogleSignIn: suspend () -> GoogleSignInResult,
    onGoogleSignOut: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentSection by remember { mutableStateOf(RiderSection.Missions) }
    var language by remember { mutableStateOf(AppLanguage.English) }
    val deviceId = rememberAuthDeviceId()
    val sessionStore = rememberAuthSessionStore()
    val secureTokenStorage = rememberSecureTokenStorage()
    val authRepository = remember(deviceId, sessionStore, secureTokenStorage) {
        AuthRepository(
            authApiClient = AuthApiClient(deviceIdProvider = { deviceId }),
            sessionStore = sessionStore,
            secureSessionCache = SecureSessionCache(secureTokenStorage),
        )
    }
    val scope = rememberCoroutineScope()
    val sessionRepository = remember { DemoRiderSessionRepository() }
    val missionRepository = remember { DemoMissionRepository() }
    val payoutRepository = remember { DemoPayoutRepository() }
    val profile = remember { sessionRepository.currentProfile() }
    var missions by remember { mutableStateOf(DemoRiderData.missions) }
    var missionsLoading by remember { mutableStateOf(false) }
    var missionsOffline by remember { mutableStateOf(false) }
    var missionActionInProgressId by remember { mutableStateOf<String?>(null) }
    var missionActionFailed by remember { mutableStateOf(false) }
    var selectedMissionId by remember { mutableStateOf<String?>(null) }
    val payoutSummary = remember { payoutRepository.summary() }
    var signedInUser by remember { mutableStateOf<CurrentUser?>(null) }
    var useLocalDemo by remember { mutableStateOf(false) }
    var authLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authRepository) {
        authLoading = true
        runCatching { authRepository.restoreSavedSession() }
            .onSuccess { user -> signedInUser = user }
            .onFailure { error -> authError = userFacingAuthError(error, language) }
        authLoading = false
    }

    fun refreshMissions(showLoading: Boolean = true) {
        scope.launch {
            if (showLoading) missionsLoading = true
            missionActionFailed = false
            runCatching { missionRepository.listMissions(profile.riderId) }
                .onSuccess { latest ->
                    missions = latest
                    missionsOffline = false
                }
                .onFailure {
                    missionsOffline = true
                }
            missionsLoading = false
        }
    }

    fun acceptMission(mission: RiderMission) {
        if (missionActionInProgressId != null) return
        scope.launch {
            missionActionInProgressId = mission.id
            missionActionFailed = false
            runCatching { missionRepository.acceptMission(mission.id) }
                .onSuccess { updated ->
                    missions = missions.replaceMission(updated)
                    selectedMissionId = updated.id
                    missionsOffline = false
                }
                .onFailure {
                    missionActionFailed = true
                }
            missionActionInProgressId = null
        }
    }

    fun updateMissionAction(mission: RiderMission, action: suspend () -> RiderMission) {
        if (missionActionInProgressId != null) return
        scope.launch {
            missionActionInProgressId = mission.id
            missionActionFailed = false
            runCatching { action() }
                .onSuccess { updated ->
                    missions = missions.replaceMission(updated)
                    selectedMissionId = updated.id
                    missionsOffline = false
                }
                .onFailure {
                    missionActionFailed = true
                }
            missionActionInProgressId = null
        }
    }

    LaunchedEffect(missionRepository, profile.riderId) {
        missionsLoading = true
        runCatching { missionRepository.listMissions(profile.riderId) }
            .onSuccess { latest ->
                missions = latest
                missionsOffline = false
            }
            .onFailure {
                missionsOffline = true
            }
        missionsLoading = false
    }

    CompositionLocalProvider(LocalAppLanguage provides language) {
        if (signedInUser == null && !useLocalDemo) {
            Box(modifier = modifier.fillMaxSize().padding(PaddingValues(16.dp))) {
                RiderAuthScreen(
                    isLoading = authLoading,
                    errorMessage = authError,
                    onGoogleLogin = {
                        if (authLoading) return@RiderAuthScreen
                        scope.launch {
                            authLoading = true
                            authError = null
                            when (val result = runCatching { onGoogleSignIn() }.getOrElse { GoogleSignInResult.Failure(GoogleSignInFailureReason.Failed) }) {
                                is GoogleSignInResult.Success -> {
                                    runCatching { authRepository.loginWithGoogle(result.idToken) }
                                        .onSuccess { user -> signedInUser = user }
                                        .onFailure { error -> authError = userFacingAuthError(error, language) }
                                }
                                GoogleSignInResult.Cancelled -> Unit
                                is GoogleSignInResult.Failure -> {
                                    authError = userFacingGoogleError(result.reason, language)
                                }
                            }
                            authLoading = false
                        }
                    },
                    onLocalDemo = {
                        useLocalDemo = true
                        authError = null
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            return@CompositionLocalProvider
        }

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
                    actions = {
                        if (signedInUser != null) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        authRepository.logout()
                                        onGoogleSignOut()
                                        signedInUser = null
                                        currentSection = RiderSection.Missions
                                    }
                                },
                            ) {
                                Text(appText(RiderText.SignOut))
                            }
                        }
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
                            isLoading = missionsLoading,
                            isOffline = missionsOffline,
                            actionInProgressMissionId = missionActionInProgressId,
                            actionFailed = missionActionFailed,
                            onRefresh = { refreshMissions() },
                            onRetry = { refreshMissions() },
                            onAcceptMission = ::acceptMission,
                            onRouteClick = { mission ->
                                selectedMissionId = mission.id
                                currentSection = RiderSection.ActiveRoute
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                        RiderSection.ActiveRoute -> ActiveRouteScreen(
                            mission = missions.firstOrNull { it.id == selectedMissionId } ?: missions.firstOrNull(),
                            actionInProgress = missionActionInProgressId != null,
                            actionFailed = missionActionFailed,
                            onMarkPickupDone = { mission, custodyNote ->
                                updateMissionAction(mission) {
                                    missionRepository.markPickedUp(
                                        mission.id,
                                        proofMetadata = custodyNote.ifBlank { "seller-pickup-confirmed" },
                                        idempotencyKey = mission.idempotencyKey("pickup"),
                                    )
                                }
                            },
                            onConfirmDelivery = { mission, deliveryPin, deliveryNote ->
                                updateMissionAction(mission) {
                                    missionRepository.markDelivered(
                                        mission.id,
                                        proofMetadata = deliveryNote.ifBlank { "customer-delivery-confirmed" },
                                        deliveryPin = deliveryPin.ifBlank { null },
                                        idempotencyKey = mission.idempotencyKey("deliver"),
                                    )
                                }
                            },
                            onConfirmRelayDeposit = { mission, packageCode, lockerSlot ->
                                updateMissionAction(mission) {
                                    val metadata = listOfNotNull(
                                        packageCode.takeIf { it.isNotBlank() }?.let { "package=$it" },
                                        lockerSlot.takeIf { it.isNotBlank() }?.let { "slot=$it" },
                                    ).joinToString("; ").ifBlank { "relay-deposit-confirmed" }
                                    missionRepository.depositAtRelay(mission.id, proofMetadata = metadata, idempotencyKey = mission.idempotencyKey("relay-deposit"))
                                }
                            },
                            onReportProblem = { mission, reason ->
                                updateMissionAction(mission) {
                                    missionRepository.reportProblem(mission.id, reason = reason, idempotencyKey = mission.idempotencyKey("problem"))
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                        RiderSection.History -> HistoryScreen(
                            missions = missions,
                            isLoading = missionsLoading,
                            isOffline = missionsOffline,
                            onRefresh = { refreshMissions() },
                            onRetry = { refreshMissions() },
                            modifier = Modifier.fillMaxSize(),
                        )
                        RiderSection.Payout -> PayoutScreen(profile = profile, summary = payoutSummary, missions = missions, modifier = Modifier.fillMaxSize())
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

private fun List<RiderMission>.replaceMission(updated: RiderMission): List<RiderMission> =
    map { mission -> if (mission.id == updated.id) updated else mission }

private fun RiderMission.idempotencyKey(action: String): String =
    "$id-$action-${updatedAt ?: status.name}"

private fun userFacingAuthError(error: Throwable, language: AppLanguage): String =
    when ((error as? AuthApiException)?.userMessage) {
        AuthUserMessage.AccountUsesAnotherMethod -> appText(RiderText.AuthGoogleFailed, language)
        AuthUserMessage.TooManyAttempts -> appText(RiderText.AuthGoogleUnavailable, language)
        AuthUserMessage.SocialSignInUnavailable -> appText(RiderText.AuthGoogleUnavailable, language)
        AuthUserMessage.GoogleSignInUnavailable -> appText(RiderText.AuthGoogleUnavailable, language)
        AuthUserMessage.GoogleSignInTemporarilyUnavailable -> appText(RiderText.AuthGoogleNotConfigured, language)
        AuthUserMessage.GoogleSignInExpired -> appText(RiderText.AuthGoogleFailed, language)
        AuthUserMessage.GoogleMissingEmail -> appText(RiderText.AuthGoogleFailed, language)
        AuthUserMessage.GoogleEmailUnverified -> appText(RiderText.AuthGoogleFailed, language)
        AuthUserMessage.GoogleSignInCouldNotVerify -> appText(RiderText.AuthGoogleFailed, language)
        AuthUserMessage.ProfileLoadFailed -> appText(RiderText.AuthProfileFailed, language)
        AuthUserMessage.SessionRestoreFailed -> appText(RiderText.AuthRestoreFailed, language)
        AuthUserMessage.RiderRoleRequired -> appText(RiderText.AuthRiderRequired, language)
        null -> appText(RiderText.AuthGoogleFailed, language)
    }

private fun userFacingGoogleError(reason: GoogleSignInFailureReason, language: AppLanguage): String =
    when (reason) {
        GoogleSignInFailureReason.NotConfigured -> appText(RiderText.AuthGoogleNotConfigured, language)
        GoogleSignInFailureReason.Unavailable -> appText(RiderText.AuthGoogleUnavailable, language)
        GoogleSignInFailureReason.Failed,
        GoogleSignInFailureReason.Interrupted,
        GoogleSignInFailureReason.InProgress,
        -> appText(RiderText.AuthGoogleFailed, language)
    }
