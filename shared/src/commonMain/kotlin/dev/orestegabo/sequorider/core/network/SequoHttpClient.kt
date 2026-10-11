package dev.orestegabo.sequorider.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createSequoHttpClient(
    baseUrl: String = NetworkConfig.ProductionBaseUrl,
    json: Json = defaultNetworkJson,
    accessTokenProvider: (() -> String?)? = null,
): HttpClient = HttpClient {
    expectSuccess = true

    defaultRequest {
        url.takeFrom(baseUrl)
        headers.append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        accept(ContentType.Application.Json)
        accessTokenProvider?.invoke()?.takeIf { it.isNotBlank() }?.let { bearerAuth(it) }
    }

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
                is ClientRequestException -> throw SequoApiException(
                    statusCode = cause.response.status.value,
                    responseBody = cause.response.bodyAsText(),
                    message = "Sequo API rejected the request with HTTP ${cause.response.status.value}.",
                    cause = cause,
                )
                is ServerResponseException -> throw SequoApiException(
                    statusCode = cause.response.status.value,
                    responseBody = cause.response.bodyAsText(),
                    message = "Sequo API failed with HTTP ${cause.response.status.value}.",
                    cause = cause,
                )
                is HttpRequestTimeoutException -> throw SequoNetworkException(
                    message = "Sequo API request timed out.",
                    cause = cause,
                )
            }
        }
    }
}

val defaultNetworkJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

class SequoApiException(
    val statusCode: Int,
    val responseBody: String?,
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause)

class SequoNetworkException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause)
