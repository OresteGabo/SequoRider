package dev.orestegabo.sequorider.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import dev.orestegabo.sequo.design.SequoActionButton as Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.orestegabo.sequo.design.SequoDesign
import dev.orestegabo.sequo.design.SequoPageHeader
import dev.orestegabo.sequorider.core.auth.AuthApiClient
import dev.orestegabo.sequorider.core.auth.AuthApiException
import dev.orestegabo.sequorider.core.auth.AuthRepository
import dev.orestegabo.sequorider.core.auth.AuthUserMessage
import dev.orestegabo.sequorider.core.auth.CurrentUser
import dev.orestegabo.sequorider.core.auth.GoogleSignInFailureReason
import dev.orestegabo.sequorider.core.auth.GoogleSignInResult
import dev.orestegabo.sequorider.core.auth.SecureSessionCache
import dev.orestegabo.sequorider.core.auth.rememberAuthDeviceId
import dev.orestegabo.sequorider.core.auth.rememberAuthSessionStore
import dev.orestegabo.sequorider.core.auth.rememberSecureTokenStorage
import dev.orestegabo.sequorider.core.missions.DemoMissionRepository
import dev.orestegabo.sequorider.core.missions.NetworkRiderMissionRepository
import dev.orestegabo.sequorider.core.missions.RiderMissionApiClient
import dev.orestegabo.sequorider.core.notifications.DemoRiderNotificationsRepository
import dev.orestegabo.sequorider.core.notifications.NetworkRiderNotificationsRepository
import dev.orestegabo.sequorider.core.notifications.RiderNotificationsApiClient
import dev.orestegabo.sequorider.core.notifications.RiderNotificationsRepository
import dev.orestegabo.sequorider.core.payout.DemoPayoutRepository
import dev.orestegabo.sequorider.core.payout.NetworkRiderPayoutRepository
import dev.orestegabo.sequorider.core.payout.RiderPayoutApiClient
import dev.orestegabo.sequorider.core.network.SequoApiException
import dev.orestegabo.sequorider.model.PayoutSummary
import dev.orestegabo.sequorider.feature.auth.RiderAuthScreen
import dev.orestegabo.sequorider.feature.settings.AccountSettingsScreen
import dev.orestegabo.sequorider.feature.settings.rememberRiderLanguagePreferences
import dev.orestegabo.sequorider.feature.settings.AppLanguage
import dev.orestegabo.sequorider.feature.settings.LocalAppLanguage
import dev.orestegabo.sequorider.feature.settings.RiderText
import dev.orestegabo.sequorider.feature.settings.appText
import dev.orestegabo.sequorider.model.DemoRiderData
import dev.orestegabo.sequorider.model.PayoutHistoryItem
import dev.orestegabo.sequorider.model.RiderMission
import dev.orestegabo.sequorider.model.RiderNotification
import dev.orestegabo.sequorider.model.RiderNotificationPreference
import dev.orestegabo.sequorider.model.RiderNotificationPreferenceEventType
import dev.orestegabo.sequorider.model.RiderSection
import dev.orestegabo.sequorider.ui.chrome.RiderBottomBar
import dev.orestegabo.sequorider.ui.chrome.label
import dev.orestegabo.sequorider.ui.history.HistoryScreen
import dev.orestegabo.sequorider.ui.missions.MissionsScreen
import dev.orestegabo.sequorider.ui.navigation.RiderBackHandler
import dev.orestegabo.sequorider.ui.navigation.riderBackDestination
import dev.orestegabo.sequorider.ui.payout.PayoutScreen
import dev.orestegabo.sequorider.ui.route.ActiveRouteScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import dev.orestegabo.sequorider.core.network.riderWorkspaceScope
import dev.orestegabo.sequorider.core.network.riderRequest
import dev.orestegabo.sequorider.core.network.riderAccessDenied
import dev.orestegabo.sequorider.core.auth.DevelopmentRiderAccess
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SequoRiderShell(
    onGoogleSignIn: suspend () -> GoogleSignInResult,
    onGoogleSignOut: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentSection by remember { mutableStateOf(RiderSection.Missions) }
    val languagePreferences = rememberRiderLanguagePreferences()
    var language by remember(languagePreferences) { mutableStateOf(languagePreferences.load()) }
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
    DisposableEffect(authRepository) { onDispose(authRepository::close) }
    val scope = rememberCoroutineScope()
    var signedInUser by remember { mutableStateOf<CurrentUser?>(null) }
    var accessToken by remember { mutableStateOf<String?>(null) }
    var useLocalDemo by remember { mutableStateOf(false) }
    val profile = if (useLocalDemo) DemoRiderData.profile else dev.orestegabo.sequorider.model.RiderProfile(
        riderId = signedInUser?.id.orEmpty(),
        displayName = signedInUser?.displayName.orEmpty(),
        fleetClass = null, availability = null, serviceZone = "", payoutProvider = null, kycApproved = null,
    )
    val missionRepository = remember(signedInUser, accessToken, useLocalDemo) {
        if (!useLocalDemo) {
            NetworkRiderMissionRepository(
                RiderMissionApiClient(accessTokenProvider = { accessToken }),
            )
        } else {
            DemoMissionRepository()
        }
    }
    val payoutRepository = remember(signedInUser, accessToken, useLocalDemo) {
        if (!useLocalDemo) {
            NetworkRiderPayoutRepository(
                RiderPayoutApiClient(accessTokenProvider = { accessToken }),
            )
        } else {
            DemoPayoutRepository()
        }
    }
    val notificationsRepository: RiderNotificationsRepository = remember(signedInUser, accessToken, useLocalDemo) {
        if (!useLocalDemo) {
            NetworkRiderNotificationsRepository(
                RiderNotificationsApiClient(accessTokenProvider = { accessToken }),
            )
        } else {
            DemoRiderNotificationsRepository(DemoRiderData.notifications)
        }
    }
    val workspaceScope = remember(missionRepository, payoutRepository, notificationsRepository) { riderWorkspaceScope(scope) }
    DisposableEffect(workspaceScope) {
        onDispose {
            workspaceScope.cancel()
            missionRepository.close()
            payoutRepository.close()
            notificationsRepository.close()
        }
    }
    var missions by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(if (useLocalDemo) DemoRiderData.missions else emptyList<RiderMission>()) }
    var missionsLoading by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(false) }
    var missionsAccessDenied by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(false) }
    var missionsOffline by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(false) }
    var missionActionInProgressId by remember(signedInUser?.id, useLocalDemo) { mutableStateOf<String?>(null) }
    var missionActionFailed by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(false) }
    var selectedMissionId by remember(signedInUser?.id, useLocalDemo) { mutableStateOf<String?>(null) }
    var payoutSummary by remember(signedInUser?.id, useLocalDemo) { mutableStateOf<PayoutSummary?>(if (useLocalDemo) DemoRiderData.payoutSummary else null) }
    var payoutHistory by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(if (useLocalDemo) DemoRiderData.payoutHistory else emptyList<PayoutHistoryItem>()) }
    var payoutLoading by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(false) }
    var payoutOffline by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(false) }
    var payoutAccessDenied by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(false) }
    var payoutRefresh by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(0) }
    var notifications by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(if (useLocalDemo) DemoRiderData.notifications else emptyList<RiderNotification>()) }
    var notificationPreference by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(RiderNotificationPreference()) }
    var notificationRefreshJob by remember(notificationsRepository) { mutableStateOf<Job?>(null) }
    var notificationsLoading by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(false) }
    var notificationsAccessDenied by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(false) }
    var notificationsOffline by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(false) }
    var notificationActionFailed by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(false) }
    var notificationPreferenceSaved by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(false) }
    var includeArchivedNotifications by remember(signedInUser?.id, useLocalDemo) { mutableStateOf(false) }
    var authLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var fixtureAccess by remember(authRepository) { mutableStateOf(DevelopmentRiderAccess()) }

    LaunchedEffect(authRepository) {
        authLoading = true
        riderRequest { authRepository.restoreSavedSession() }
            .onSuccess { user ->
                signedInUser = user
                accessToken = sessionStore.get()?.accessToken
            }
            .onFailure { error -> authError = userFacingAuthError(error, language) }
        authLoading = false
    }

    LaunchedEffect(authRepository) {
        riderRequest { authRepository.developmentAccess() }.onSuccess { fixtureAccess = it }
    }

    fun missionReadFailed(error: Throwable) {
        missionsOffline = true
        missionsAccessDenied = riderAccessDenied(error)
        if (missionsAccessDenied) missions = emptyList()
    }

    fun notificationReadFailed(error: Throwable) {
        notificationsOffline = true
        notificationsAccessDenied = riderAccessDenied(error)
        if (notificationsAccessDenied) notifications = emptyList()
    }

    fun refreshMissions(showLoading: Boolean = true) {
        if ((!useLocalDemo && signedInUser == null) || missionActionInProgressId != null) return
        workspaceScope.launch {
            if (showLoading) missionsLoading = true
            missionActionFailed = false
            riderRequest { missionRepository.listMissions(profile.riderId) }
                .onSuccess { latest ->
                    missions = latest
                    missionsOffline = false
                    missionsAccessDenied = false
                }
                .onFailure(::missionReadFailed)
            missionsLoading = false
        }
    }

    fun refreshPayout() { payoutRefresh++ }

    fun refreshNotifications(showLoading: Boolean = true) {
        if (!useLocalDemo && signedInUser == null) return
        notificationRefreshJob?.cancel()
        notificationRefreshJob = workspaceScope.launch {
            if (showLoading) notificationsLoading = true
            notificationActionFailed = false
            notificationPreferenceSaved = false
            riderRequest { notificationsRepository.inbox(includeArchivedNotifications) }
                .onSuccess { latest ->
                    notifications = latest
                    notificationsOffline = false
                    notificationsAccessDenied = false
                }
                .onFailure(::notificationReadFailed)
            riderRequest { notificationsRepository.effectivePreference(RiderNotificationPreferenceEventType.RIDER_MISSION_OFFERED) }
                .onSuccess { preference ->
                    notificationPreference = preference.copy(eventType = RiderNotificationPreferenceEventType.ALL)
                }
                .onFailure { if (riderAccessDenied(it)) notificationReadFailed(it) }
            notificationsLoading = false
        }
    }

    fun replaceNotification(updated: RiderNotification) {
        notifications = notifications.map { if (it.id == updated.id) updated else it }
    }

    fun handleNotificationAction(
        notification: RiderNotification,
        action: suspend (String) -> RiderNotification,
    ) {
        if (notificationsAccessDenied || notificationsOffline || notificationsLoading) return
        workspaceScope.launch {
            notificationActionFailed = false
            notificationPreferenceSaved = false
            riderRequest { action(notification.id) }
                .onSuccess { updated -> replaceNotification(updated) }
                .onFailure {
                    notificationActionFailed = true
                    if (riderAccessDenied(it)) notificationReadFailed(it)
                }
        }
    }

    fun saveNotificationPreference(preference: RiderNotificationPreference) {
        if (notificationsAccessDenied || notificationsOffline || notificationsLoading) return
        workspaceScope.launch {
            notificationActionFailed = false
            notificationPreferenceSaved = false
            riderRequest { notificationsRepository.savePreference(preference) }
                .onSuccess { updated ->
                    notificationPreference = updated.copy(eventType = RiderNotificationPreferenceEventType.ALL)
                    notificationPreferenceSaved = true
                }
                .onFailure {
                    notificationActionFailed = true
                    if (riderAccessDenied(it)) notificationReadFailed(it)
                }
        }
    }

    fun acceptMission(mission: RiderMission) {
        if (missionActionInProgressId != null || missionsOffline || missionsLoading) return
        missionActionInProgressId = mission.id
        workspaceScope.launch {
            try {
                missionActionFailed = false
                riderRequest { missionRepository.acceptMission(mission.id) }
                    .onSuccess { updated ->
                        missions = missions.replaceMission(updated)
                        selectedMissionId = updated.id
                        missionsOffline = false
                        riderRequest { missionRepository.listMissions(profile.riderId) }
                            .onSuccess { latest -> missions = latest; missionsAccessDenied = false }
                            .onFailure(::missionReadFailed)
                    }
                    .onFailure {
                        missionActionFailed = true
                        if (riderAccessDenied(it)) missionReadFailed(it)
                    }
            } finally {
                missionActionInProgressId = null
            }
        }
    }

    fun updateMissionAction(mission: RiderMission, action: suspend () -> RiderMission) {
        if (missionActionInProgressId != null || missionsOffline || missionsLoading) return
        missionActionInProgressId = mission.id
        workspaceScope.launch {
            try {
                missionActionFailed = false
                riderRequest { action() }
                    .onSuccess { updated ->
                        missions = missions.replaceMission(updated)
                        selectedMissionId = updated.id
                        missionsOffline = false
                        riderRequest { missionRepository.listMissions(profile.riderId) }
                            .onSuccess { latest -> missions = latest; missionsAccessDenied = false }
                            .onFailure(::missionReadFailed)
                    }
                    .onFailure {
                        missionActionFailed = true
                        if (riderAccessDenied(it)) missionReadFailed(it)
                    }
            } finally {
                missionActionInProgressId = null
            }
        }
    }


    LaunchedEffect(missionRepository, profile.riderId) {
        if (!useLocalDemo && signedInUser == null) return@LaunchedEffect
        missionsLoading = true
        riderRequest { missionRepository.listMissions(profile.riderId) }
            .onSuccess { latest ->
                missions = latest
                missionsOffline = false
                missionsAccessDenied = false
            }
            .onFailure(::missionReadFailed)
        missionsLoading = false
    }

    LaunchedEffect(payoutRepository, payoutRefresh) {
        payoutSummary = null
        payoutHistory = emptyList()
        payoutOffline = false
        payoutAccessDenied = false
        if (!useLocalDemo && signedInUser == null) return@LaunchedEffect
        payoutLoading = true
        try {
            val summary = payoutRepository.summary()
            val history = payoutRepository.history()
            payoutSummary = summary
            payoutHistory = history
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            payoutOffline = true
            payoutAccessDenied = (error as? SequoApiException)?.statusCode in setOf(401, 403)
        } finally { payoutLoading = false }
    }

    LaunchedEffect(notificationsRepository, includeArchivedNotifications) {
        refreshNotifications(showLoading = false)
    }

    CompositionLocalProvider(LocalAppLanguage provides language) {
        if (signedInUser == null && !useLocalDemo) {
            Box(modifier = modifier.fillMaxSize().padding(SequoDesign.ScreenGutter)) {
                RiderAuthScreen(
                    isLoading = authLoading,
                    errorMessage = authError,
                    onGoogleLogin = {
                        if (authLoading) return@RiderAuthScreen
                        scope.launch {
                            authLoading = true
                            authError = null
                            when (val result = riderRequest { onGoogleSignIn() }.getOrElse { GoogleSignInResult.Failure(GoogleSignInFailureReason.Failed) }) {
                                is GoogleSignInResult.Success -> {
                                    riderRequest { authRepository.loginWithGoogle(result.idToken) }
                                        .onSuccess { user ->
                                            signedInUser = user
                                            accessToken = sessionStore.get()?.accessToken
                                        }
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
                        currentSection = RiderSection.Missions
                        useLocalDemo = true
                        authError = null
                    },
                    fixtureAccounts = fixtureAccess.accounts,
                    onFixtureLogin = { accountKey ->
                        if (!authLoading) scope.launch {
                            authLoading = true
                            authError = null
                            try {
                                riderRequest { authRepository.loginWithFixture(accountKey) }
                                    .onSuccess { user ->
                                        currentSection = RiderSection.Missions
                                        signedInUser = user
                                        accessToken = sessionStore.get()?.accessToken
                                    }
                                    .onFailure { authError = userFacingAuthError(it, language) }
                            } finally { authLoading = false }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            return@CompositionLocalProvider
        }

        val backDestination = riderBackDestination(currentSection)
        RiderBackHandler(enabled = backDestination != null) {
            backDestination?.let { currentSection = it }
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                // Native hosts own safe-area insets; the shared header adds none.
                SequoPageHeader(
                    title = currentSection.label(),
                    subtitle = appText(RiderText.AppName),
                    actions = {
                        if (useLocalDemo) {
                            Button(onClick = {
                                useLocalDemo = false
                                currentSection = RiderSection.Missions
                            }) { Text(appText(RiderText.ExitDemo)) }
                        } else if (signedInUser != null) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            authRepository.logout()
                                            if (signedInUser?.provider.equals("GOOGLE", ignoreCase = true)) {
                                                riderRequest { onGoogleSignOut() }
                                            }
                                        } finally {
                                            signedInUser = null
                                            accessToken = null
                                            currentSection = RiderSection.Missions
                                        }
                                    }
                                },
                            ) {
                                Text(appText(RiderText.SignOut))
                            }
                        }
                    },
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
                            localMode = useLocalDemo,
                            isLoading = missionsLoading,
                            isOffline = missionsOffline,
                            accessDenied = missionsAccessDenied,
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
                            mission = missions.firstOrNull { it.id == selectedMissionId },
                            actionInProgress = missionActionInProgressId != null,
                            isOffline = missionsOffline,
                            isLoading = missionsLoading,
                            accessDenied = missionsAccessDenied,
                            actionFailed = missionActionFailed,
                            onMarkPickupDone = { mission, custodyNote ->
                                updateMissionAction(mission) {
                                    missionRepository.markPickedUp(
                                        mission.id,
                                        proofMetadata = custodyNote.takeIf { it.isNotBlank() },
                                        idempotencyKey = mission.idempotencyKey("pickup"),
                                    )
                                }
                            },
                            onConfirmDelivery = { mission, deliveryPin, deliveryNote ->
                                updateMissionAction(mission) {
                                    missionRepository.markDelivered(
                                        mission.id,
                                        proofMetadata = deliveryNote.takeIf { it.isNotBlank() },
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
                                    missionRepository.depositAtRelay(mission.id, proofMetadata = metadata, idempotencyKey = mission.idempotencyKey("relay-deposit"), relayPointId = mission.relayPointId, lockerCode = lockerSlot.trim())
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
                            localMode = useLocalDemo,
                            isLoading = missionsLoading,
                            isOffline = missionsOffline,
                            accessDenied = missionsAccessDenied,
                            onRefresh = { refreshMissions() },
                            onRetry = { refreshMissions() },
                            modifier = Modifier.fillMaxSize(),
                        )
                        RiderSection.Payout -> PayoutScreen(
                            summary = payoutSummary,
                            history = payoutHistory,
                            isLoading = payoutLoading,
                            accessDenied = payoutAccessDenied,
                            isDemo = useLocalDemo,
                            isOffline = payoutOffline,
                            onRefresh = { refreshPayout() },
                            onRetry = { refreshPayout() },
                            modifier = Modifier.fillMaxSize(),
                        )
                        RiderSection.Account -> AccountSettingsScreen(
                            profile = profile,
                            language = language,
                            onLanguageChange = { languagePreferences.save(it); language = it },
                            notifications = notifications,
                            notificationPreference = notificationPreference,
                            notificationsLoading = notificationsLoading,
                            notificationsOffline = notificationsOffline,
                            notificationsAccessDenied = notificationsAccessDenied,
                            notificationActionFailed = notificationActionFailed,
                            notificationPreferenceSaved = notificationPreferenceSaved,
                            includeArchivedNotifications = includeArchivedNotifications,
                            onIncludeArchivedNotificationsChange = { includeArchivedNotifications = it },
                            onNotificationsRefresh = { refreshNotifications() },
                            onNotificationRead = { notification ->
                                handleNotificationAction(notification, notificationsRepository::markRead)
                            },
                            onNotificationArchive = { notification ->
                                handleNotificationAction(notification, notificationsRepository::archive)
                            },
                            onNotificationUnarchive = { notification ->
                                handleNotificationAction(notification, notificationsRepository::unarchive)
                            },
                            onNotificationPreferenceChange = ::saveNotificationPreference,
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
