package dev.orestegabo.sequorider

import dev.orestegabo.sequorider.core.auth.*
import dev.orestegabo.sequorider.core.network.riderRequest
import dev.orestegabo.sequorider.core.network.riderWorkspaceScope
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class DevelopmentRiderSessionTest {
    @Test fun fixtureEntryNeverCallsProductionOrForeignTargets() = runBlocking {
        val calls = mutableListOf<String>()
        val client = client { calls += it; """{"enabled":true,"mode":"LOCAL_FIXTURE","accounts":[{"key":"senam","label":"Test rider"}]}""" }
        try {
            for ((environment, target) in listOf("PRODUCTION" to "http://127.0.0.1:55840", "STAGING" to "http://127.0.0.1:55840",
                "LOCAL_FIXTURE" to "https://api.sequoservice.com", "LOCAL_FIXTURE" to "http://127.0.0.1.evil.test:55840",
                "LOCAL_FIXTURE" to "http://user@127.0.0.1:55840", "LOCAL_FIXTURE" to "http://127.0.0.1:55840/foreign")) {
                val api = AuthApiClient(target, client, { "fixture-device" }, environment)
                assertFalse(api.developmentAccess().enabled)
                assertFailsWith<IllegalArgumentException> { api.loginWithFixture("senam") }
            }
            assertTrue(calls.isEmpty())
            assertTrue(AuthApiClient("http://127.0.0.1:55840", client, { "device" }, "LOCAL_FIXTURE").developmentAccess().enabled)
            assertTrue(AuthApiClient("http://10.0.2.2:55840", client, { "device" }, "LOCAL_FIXTURE").developmentAccess().enabled)
        } finally { client.close() }
    }

    @Test fun capabilityMustBeExplicitAndContainOnlyKnownUniqueAccounts() = runBlocking {
        for (body in listOf("{}", """{"enabled":true,"mode":"PRODUCTION","accounts":[{"key":"senam","label":"Test"}]}""",
            """{"enabled":true,"mode":"LOCAL_FIXTURE","accounts":[{"key":"admin","label":"Wrong"}]}""",
            """{"enabled":true,"mode":"LOCAL_FIXTURE","accounts":[{"key":"senam","label":"Test"},{"key":"senam","label":"Test"}]}""")) {
            val client = client { body }
            try { assertFalse(AuthApiClient("http://127.0.0.1:55840", client, { "device" }, "LOCAL_FIXTURE").developmentAccess().enabled) }
            finally { client.close() }
        }
    }

    @Test fun fixtureLoginReadsActualUserBeforeSavingNormalRestorableSession() = runBlocking {
        val calls = mutableListOf<String>()
        val client = client { path ->
            calls += path
            when (path) {
                "/api/auth/development-access" -> capability
                "/api/auth/development-session" -> tokens
                "/api/auth/me" -> rider
                else -> error("Unexpected call $path")
            }
        }
        val store = MemorySessionStore(); val secure = MemoryTokens()
        val repository = AuthRepository(AuthApiClient("http://127.0.0.1:55840", client, { "device" }, "LOCAL_FIXTURE"), store, SecureSessionCache(secure))
        try {
            val user = repository.loginWithFixture("senam")
            assertEquals("local-ops-v1-rider-senam", user.id)
            assertEquals("access", store.session?.accessToken); assertEquals("refresh", secure.token)
            assertEquals(user, store.user); assertEquals(user, repository.restoreSavedSession())
            assertEquals(listOf("/api/auth/development-access", "/api/auth/development-session", "/api/auth/me", "/api/auth/me"), calls)
        } finally { repository.close() }
    }

    @Test fun foreignIdentityOrWrongRoleNeverBecomesCachedRiderSession() = runBlocking {
        for (body in listOf(rider.replace("local-ops-v1-rider-senam", "local-ops-v1-rider-komlan"), rider.replace("COURIER", "CUSTOMER"))) {
            val client = client { path -> when (path) { "/api/auth/development-access" -> capability; "/api/auth/development-session" -> tokens; else -> body } }
            val store = MemorySessionStore(); val secure = MemoryTokens()
            val repository = AuthRepository(AuthApiClient("http://127.0.0.1:55840", client, { "device" }, "LOCAL_FIXTURE"), store, SecureSessionCache(secure))
            try {
                assertFails { repository.loginWithFixture("senam") }
                assertNull(store.session); assertNull(store.user); assertNull(secure.token)
            } finally { repository.close() }
        }
    }

    @Test fun logoutSerializesWithPendingRestoreAndClearsStoresWhenServerUnavailable() = runBlocking {
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val client = client { path ->
            if (path == "/api/auth/me") { entered.complete(Unit); release.await(); rider }
            else error("Local server unavailable")
        }
        val store = MemorySessionStore().apply { session = AuthSession("access", "refresh", 3600) }
        val secure = MemoryTokens().apply { token = "refresh" }
        val repository = AuthRepository(AuthApiClient("http://127.0.0.1:55840", client, { "device" }, "LOCAL_FIXTURE"), store, SecureSessionCache(secure))
        try {
            val restore = async { repository.restoreSavedSession() }; entered.await()
            val logout = async(start = CoroutineStart.UNDISPATCHED) { repository.logout() }
            assertFalse(logout.isCompleted)
            release.complete(Unit); restore.await(); logout.await()
            assertNull(store.session); assertNull(store.user); assertNull(secure.token)
        } finally { repository.close() }
    }

    @Test fun rejectedRestoreClearsRememberedCredentialsInsteadOfFakingProfile() = runBlocking {
        val client = client { throw AuthApiException(401, AuthUserMessage.SessionRestoreFailed) }
        val store = MemorySessionStore().apply { session = AuthSession("expired", "revoked", 3600) }
        val secure = MemoryTokens().apply { token = "revoked" }
        val repository = AuthRepository(AuthApiClient("http://127.0.0.1:55840", client, { "device" }, "LOCAL_FIXTURE"), store, SecureSessionCache(secure))
        try {
            assertFailsWith<AuthApiException> { repository.restoreSavedSession() }
            assertNull(store.session); assertNull(secure.token)
        } finally { repository.close() }
    }

    @Test fun oldSessionCancellationDoesNotPublishOrBecomeOfflineFailure() = runBlocking {
        val workspace = riderWorkspaceScope(this)
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        var published = false; var failed = false
        val job = workspace.launch(start = CoroutineStart.UNDISPATCHED) {
            riderRequest {
                withContext(NonCancellable) { entered.complete(Unit); release.await() }
                "previous account response"
            }.onSuccess { published = true }.onFailure { failed = true }
        }
        entered.await(); workspace.cancel(); release.complete(Unit); job.join()
        assertTrue(job.isCancelled); assertFalse(published); assertFalse(failed)
    }

    private fun client(response: suspend (String) -> String) = HttpClient(MockEngine { request ->
        if (request.url.encodedPath == "/api/auth/development-session") {
            val payload = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals("SEQUO_RIDER", payload.getValue("appSource").jsonPrimitive.content)
            assertEquals("senam", payload.getValue("accountKey").jsonPrimitive.content)
            assertEquals("device", payload.getValue("deviceId").jsonPrimitive.content)
        }
        respond(response(request.url.encodedPath), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
    }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
    private class MemorySessionStore : AuthSessionStore {
        var session: AuthSession? = null; var user: CurrentUser? = null
        override suspend fun save(session: AuthSession) { this.session = session }
        override suspend fun get() = session
        override suspend fun saveRememberedUser(user: CurrentUser) { this.user = user }
        override suspend fun getRememberedUser() = user
        override suspend fun clear() { session = null; user = null }
    }
    private class MemoryTokens : SecureTokenStorage {
        var token: String? = null
        override suspend fun saveRefreshToken(token: String) { this.token = token }
        override suspend fun getRefreshToken() = token
        override suspend fun clearSession() { token = null }
    }
    private companion object {
        const val capability = """{"enabled":true,"mode":"LOCAL_FIXTURE","accounts":[{"key":"senam","label":"[TEST] Senam"}]}"""
        const val tokens = """{"accessToken":"access","refreshToken":"refresh","expiresIn":3600,"user":{"id":"ignored"}}"""
        const val rider = """{"id":"local-ops-v1-rider-senam","displayName":"[TEST] Senam","provider":"EMAIL","status":"ACTIVE","roles":["COURIER"]}"""
    }
}
