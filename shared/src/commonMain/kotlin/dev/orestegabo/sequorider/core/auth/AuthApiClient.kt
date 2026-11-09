package dev.orestegabo.sequorider.core.auth

import dev.orestegabo.sequorider.core.network.NetworkConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class AuthApiClient(
    private val baseUrl: String = NetworkConfig.BaseUrl,
    private val httpClient: HttpClient = createAuthHttpClient(),
    private val deviceIdProvider: () -> String,
    private val environment: String = NetworkConfig.Environment,
) {
    suspend fun testAccountsAvailable(): Boolean = httpClient.get(apiUrl("auth/test-accounts")).body<TestCapability>().enabled
    suspend fun loginWithTestAccount(username:String,password:String): AuthSession = httpClient.post(apiUrl("auth/test-accounts/session")) {
        headers { append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) }
        setBody(TestLoginRequest(username,password,"RIDER",TestLoginDevice(deviceIdProvider(),"SEQUO_RIDER")))
    }.body()

    suspend fun developmentAccess(): DevelopmentRiderAccess {
        if (!developmentRiderAccessAllowed(environment, baseUrl)) return DevelopmentRiderAccess()
        val access: DevelopmentRiderAccess = httpClient.get(apiUrl("auth/development-access")) {
            url { parameters.append("appSource", "SEQUO_RIDER") }
        }.body()
        return access.takeIf { it.enabled && it.mode == "LOCAL_FIXTURE" && it.accounts.isNotEmpty() &&
            it.accounts.all { account -> account.key in setOf("senam", "komlan") && account.label.isNotBlank() } &&
            it.accounts.map { account -> account.key }.distinct().size == it.accounts.size } ?: DevelopmentRiderAccess()
    }

    suspend fun loginWithFixture(accountKey: String): AuthSession {
        require(developmentRiderAccessAllowed(environment, baseUrl)) { "Local fixture access is disabled." }
        val access = developmentAccess()
        require(access.enabled && access.accounts.any { it.key == accountKey }) { "Local fixture account is unavailable." }
        return httpClient.post(apiUrl("auth/development-session")) {
            headers { append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) }
            setBody(DevelopmentRiderSessionRequest(appSource = "SEQUO_RIDER", accountKey = accountKey, deviceId = deviceIdProvider()))
        }.body()
    }

    suspend fun loginWithGoogle(idToken: String): AuthApiResponse =
        loginWithSocialToken(SocialLoginProvider.Google, idToken)

    private suspend fun loginWithSocialToken(provider: SocialLoginProvider, token: String): AuthApiResponse {
        val response = try {
            httpClient.post(apiUrl("auth/social/${provider.wireName}")) {
                headers { append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) }
                setBody(
                    SocialLoginRequest(
                        token = token,
                        device = DeviceRequest(
                            deviceId = deviceIdProvider(),
                            appSource = AppSource.SequoRider,
                        ),
                    ),
                )
            }
        } catch (error: RiderAuthHttpException) {
            throw AuthApiException(
                statusCode = error.statusCode,
                userMessage = socialLoginFailureMessage(provider, error.statusCode ?: 0, error.responseBody),
                cause = error,
            )
        }
        return AuthApiResponse(statusCode = response.status.value, session = response.body())
    }

    suspend fun currentUser(accessToken: String): CurrentUser =
        try {
            httpClient.get(apiUrl("auth/me")) {
                bearerAuth(accessToken)
            }.body()
        } catch (error: RiderAuthHttpException) {
            throw AuthApiException(
                statusCode = error.statusCode,
                userMessage = AuthUserMessage.ProfileLoadFailed,
                cause = error,
            )
        }

    suspend fun refreshSession(refreshToken: String): AuthSession =
        try {
            httpClient.post(apiUrl("auth/refresh")) {
                headers { append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) }
                setBody(RefreshRequest(refreshToken = refreshToken, deviceId = deviceIdProvider()))
            }.body()
        } catch (error: RiderAuthHttpException) {
            throw AuthApiException(
                statusCode = error.statusCode,
                userMessage = AuthUserMessage.SessionRestoreFailed,
                cause = error,
            )
        }

    suspend fun logout(refreshToken: String) {
        try {
            httpClient.post(apiUrl("auth/logout")) {
                headers { append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) }
                setBody(LogoutRequest(refreshToken))
            }
        } catch (_: RiderAuthHttpException) {
            // Local logout must still complete when the API is unavailable.
        }
    }

    fun close() {
        httpClient.close()
    }

    private fun apiUrl(path: String): String =
        "${baseUrl.trimEnd('/')}/api/${path.trimStart('/')}"
}

private fun socialLoginFailureMessage(provider: SocialLoginProvider, statusCode: Int, responseBody: String?): AuthUserMessage {
    if (provider != SocialLoginProvider.Google) {
        return AuthUserMessage.SocialSignInUnavailable
    }

    val googleError = responseBody
        ?.takeIf { it.isNotBlank() }
        ?.let { body -> runCatching { authJson.decodeFromString<GoogleLoginErrorResponse>(body) }.getOrNull() }

    return when (googleError?.reason ?: googleError?.code ?: googleError?.error) {
        "invalid_audience", "missing_allowed_audience" -> AuthUserMessage.GoogleSignInTemporarilyUnavailable
        "expired_token" -> AuthUserMessage.GoogleSignInExpired
        "missing_email" -> AuthUserMessage.GoogleMissingEmail
        "unverified_email" -> AuthUserMessage.GoogleEmailUnverified
        "invalid_issuer", "invalid_signature", "malformed_token", "invalid_token" -> AuthUserMessage.GoogleSignInCouldNotVerify
        "account_link_required" -> AuthUserMessage.AccountUsesAnotherMethod
        else -> when (statusCode) {
            409 -> AuthUserMessage.AccountUsesAnotherMethod
            429 -> AuthUserMessage.TooManyAttempts
            else -> AuthUserMessage.GoogleSignInUnavailable
        }
    }
}

private fun createAuthHttpClient(json: Json = authJson): HttpClient = HttpClient {
    expectSuccess = true
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 30_000
    }
    install(ContentNegotiation) {
        json(json)
    }
    HttpResponseValidator {
        handleResponseExceptionWithRequest { cause, _ ->
            when (cause) {
                is ClientRequestException -> throw RiderAuthHttpException(cause.response.status.value, cause.response.bodyAsText(), cause)
                is ServerResponseException -> throw RiderAuthHttpException(cause.response.status.value, cause.response.bodyAsText(), cause)
                is HttpRequestTimeoutException -> throw RiderAuthHttpException(null, null, cause)
            }
        }
    }
}

private val authJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private class RiderAuthHttpException(
    val statusCode: Int?,
    val responseBody: String?,
    cause: Throwable,
) : Exception(cause)

data class AuthApiResponse(
    val statusCode: Int,
    val session: AuthSession,
)

@Serializable
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)

@Serializable
data class CurrentUser(
    val id: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val name: String? = null,
    @SerialName("displayName")
    val backendDisplayName: String? = null,
    val provider: String,
    val status: String,
    val active: Boolean = true,
    val roles: Set<String> = emptySet(),
    val publicReference: String? = null,
) {
    val displayName: String
        get() = backendDisplayName?.takeIf { it.isNotBlank() }
            ?: name?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: "Sequo Rider"
}

fun CurrentUser.hasRiderAccess(): Boolean =
    active && roles.any { it.equals("COURIER", ignoreCase = true) }

class AuthApiException(
    val statusCode: Int?,
    val userMessage: AuthUserMessage,
    override val cause: Throwable? = null,
) : Exception(userMessage.name, cause)

enum class AuthUserMessage {
    AccountUsesAnotherMethod,
    TooManyAttempts,
    SocialSignInUnavailable,
    GoogleSignInUnavailable,
    GoogleSignInTemporarilyUnavailable,
    GoogleSignInExpired,
    GoogleMissingEmail,
    GoogleEmailUnverified,
    GoogleSignInCouldNotVerify,
    ProfileLoadFailed,
    SessionRestoreFailed,
    RiderRoleRequired,
}

@Serializable
private data class SocialLoginRequest(
    val token: String,
    val device: DeviceRequest,
)

@Serializable
private data class DeviceRequest(
    val deviceId: String,
    val appSource: AppSource,
)

@Serializable
private data class RefreshRequest(
    val refreshToken: String,
    val deviceId: String,
)

@Serializable
private data class LogoutRequest(
    val refreshToken: String,
)

@Serializable
private data class GoogleLoginErrorResponse(
    val error: String? = null,
    val reason: String? = null,
    val code: String? = null,
    val message: String? = null,
)

@Serializable
private enum class SocialLoginProvider {
    @SerialName("GOOGLE")
    Google;

    val wireName: String
        get() = when (this) {
            Google -> "GOOGLE"
        }
}

@Serializable
private enum class AppSource {
    @SerialName("SEQUO_RIDER")
    SequoRider,
}

/** Capability lookup is never attempted from staging/production or a nonlocal build target. */
internal fun developmentRiderAccessAllowed(environment: String, baseUrl: String): Boolean =
    environment == "LOCAL_FIXTURE" &&
        Regex("""https?://(?:127\.0\.0\.1|10\.0\.2\.2|localhost|\[::1\])(?::[0-9]{1,5})?/?""").matches(baseUrl)

@Serializable
data class DevelopmentRiderAccount(val key: String, val label: String)
@Serializable
data class DevelopmentRiderAccess(val enabled: Boolean = false, val mode: String? = null,
    val accounts: List<DevelopmentRiderAccount> = emptyList())
@Serializable
private data class DevelopmentRiderSessionRequest(val appSource: String, val accountKey: String,
    val deviceId: String, val deviceLabel: String = "SEQUO Rider local fixture")

@Serializable private data class TestCapability(val enabled: Boolean = false)
@Serializable private data class TestLoginRequest(val username:String,val password:String,val appFamily:String,val device:TestLoginDevice)
@Serializable private data class TestLoginDevice(val deviceId:String,val appSource:String)
