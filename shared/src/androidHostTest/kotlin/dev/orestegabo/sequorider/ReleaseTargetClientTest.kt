package dev.orestegabo.sequorider

import dev.orestegabo.sequorider.core.auth.AdminLoginApprovalApiClient
import dev.orestegabo.sequorider.core.auth.AuthApiClient
import dev.orestegabo.sequorider.core.missions.RiderMissionApiClient
import dev.orestegabo.sequorider.core.network.ApiHealthClient
import dev.orestegabo.sequorider.core.network.NetworkConfig
import dev.orestegabo.sequorider.core.notifications.RiderNotificationsApiClient
import dev.orestegabo.sequorider.core.payout.RiderPayoutApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReleaseTargetClientTest {
    @Test
    fun authenticationAndOperationsUseTheSameBuildTargetWithoutNetwork() = runBlocking<Unit> {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            requests += request.url.toString()
            val response = when (request.url.encodedPath) {
                "/api/auth/me" -> """{"id":"fixture-rider","provider":"GOOGLE","status":"ACTIVE","roles":["COURIER"]}"""
                "/actuator/health" -> """{"status":"UP"}"""
                else -> "[]"
            }
            respond(response, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        try {
            val auth = AuthApiClient(httpClient = client, deviceIdProvider = { "fixture-device" })
            assertEquals("fixture-rider", auth.currentUser("fixture-token").id)
            assertTrue(RiderMissionApiClient(accessTokenProvider = { "fixture-token" }, httpClient = client).listMissions().isEmpty())
            assertTrue(RiderPayoutApiClient(accessTokenProvider = { "fixture-token" }, httpClient = client).history().isEmpty())
            assertTrue(RiderNotificationsApiClient(accessTokenProvider = { "fixture-token" }, httpClient = client).inbox().isEmpty())
            assertTrue(AdminLoginApprovalApiClient(accessTokenProvider = { "fixture-token" }, httpClient = client).getPendingLoginRequests().getOrThrow().isEmpty())
            assertTrue(ApiHealthClient(httpClient = client).checkHealth().isHealthyResponse)
            val expectedPaths = listOf("/api/auth/me", "/api/rider/missions", "/api/rider/payouts/history",
                "/api/notifications/inbox", "/api/admin-auth/mobile/login-requests", "/actuator/health")
            assertEquals(expectedPaths.size, requests.size)
            requests.zip(expectedPaths).forEach { (url, path) ->
                assertEquals(NetworkConfig.BaseUrl + path, url.substringBefore('?'))
            }
        } finally { client.close() }
    }
}
