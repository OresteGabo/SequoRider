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
    val missions = remember { missionRepository.missions() }
    val routePlan = remember { missionRepository.activeRoute() }
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
