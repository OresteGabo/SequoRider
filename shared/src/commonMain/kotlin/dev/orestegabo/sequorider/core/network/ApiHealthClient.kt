package dev.orestegabo.sequorider.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ApiHealthClient(
    private val baseUrl: String = NetworkConfig.ProductionBaseUrl,
    private val httpClient: HttpClient = createSequoHttpClient(baseUrl),
    private val json: Json = defaultNetworkJson,
) {
    suspend fun checkHealth(path: String = "/actuator/health"): ApiHealthResult =
        runCatching {
            val response = httpClient.get("${baseUrl.trimEnd('/')}/${path.trimStart('/')}")
            val body = response.bodyAsText()
            val actuatorStatus = body.actuatorStatusOrNull()

            ApiHealthResult(
                requestReachedBackend = true,
                isHealthyResponse = response.status.value in 200..299,
                statusCode = response.status.value,
                actuatorStatus = actuatorStatus,
                message = actuatorStatus ?: response.status.description,
            )
        }.getOrElse { error ->
            ApiHealthResult(
                requestReachedBackend = false,
                isHealthyResponse = false,
                message = error.message ?: "Unable to reach Sequo API",
            )
        }

    fun close() {
        httpClient.close()
    }

    private fun String.actuatorStatusOrNull(): String? =
        try {
            json.decodeFromString<ActuatorHealthResponse>(this).status
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
}

data class ApiHealthResult(
    val requestReachedBackend: Boolean,
    val isHealthyResponse: Boolean,
    val statusCode: Int? = null,
    val actuatorStatus: String? = null,
    val message: String,
)

@Serializable
private data class ActuatorHealthResponse(
    val status: String? = null,
)
