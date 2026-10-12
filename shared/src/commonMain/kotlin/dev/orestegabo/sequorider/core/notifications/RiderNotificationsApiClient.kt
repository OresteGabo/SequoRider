package dev.orestegabo.sequorider.core.notifications

import dev.orestegabo.sequorider.core.network.NetworkConfig
import dev.orestegabo.sequorider.core.network.createSequoHttpClient
import dev.orestegabo.sequorider.model.RiderNotification
import dev.orestegabo.sequorider.model.RiderNotificationPreference
import dev.orestegabo.sequorider.model.RiderNotificationPreferenceEventType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

class RiderNotificationsApiClient(
    private val baseUrl: String = NetworkConfig.ProductionBaseUrl,
    private val accessTokenProvider: () -> String?,
    private val httpClient: HttpClient = createSequoHttpClient(baseUrl = baseUrl),
) {
    suspend fun registerFcmToken(
        deviceId: String,
        fcmToken: String,
        platform: String,
        appVersion: String? = null,
        locale: String? = null,
        timezone: String? = null,
    ): RiderDeviceFcmTokenResponseDto =
        httpClient.post(apiUrl("notifications/devices/fcm")) {
            bearer()
            setBody(
                RegisterRiderFcmTokenRequestDto(
                    deviceId = deviceId,
                    fcmToken = fcmToken,
                    platform = platform,
                    appVersion = appVersion,
                    locale = locale,
                    timezone = timezone,
                ),
            )
        }.body()

    suspend fun revokeDevice(deviceId: String) {
        httpClient.delete(apiUrl("notifications/devices/$SequoRiderAppFamily/$deviceId")) {
            bearer()
        }
    }

    suspend fun inbox(
        includeArchived: Boolean = false,
        limit: Int = 50,
    ): List<RiderNotification> =
        httpClient.get(apiUrl("notifications/inbox")) {
            bearer()
            parameter("includeArchived", includeArchived)
            parameter("limit", limit)
        }.body<List<RiderNotificationMessageDto>>()
            .filter { it.appFamily == SequoRiderAppFamily }
            .map { it.toDomain() }

    suspend fun markRead(messageId: String): RiderNotification =
        httpClient.patch(apiUrl("notifications/inbox/$messageId/read")) {
            bearer()
        }.body<RiderNotificationMessageDto>().toDomain()

    suspend fun archive(messageId: String): RiderNotification =
        httpClient.post(apiUrl("notifications/inbox/$messageId/archive")) {
            bearer()
        }.body<RiderNotificationMessageDto>().toDomain()

    suspend fun unarchive(messageId: String): RiderNotification =
        httpClient.delete(apiUrl("notifications/inbox/$messageId/archive")) {
            bearer()
        }.body<RiderNotificationMessageDto>().toDomain()

    suspend fun effectivePreference(eventType: String): RiderNotificationPreference =
        httpClient.get(apiUrl("notifications/preferences/$SequoRiderAppFamily/effective")) {
            bearer()
            parameter("eventType", eventType)
        }.body<RiderNotificationPreferenceDto>().toDomain()

    suspend fun savePreference(preference: RiderNotificationPreference): RiderNotificationPreference =
        httpClient.put(apiUrl("notifications/preferences/$SequoRiderAppFamily")) {
            bearer()
            setBody(preference.toDto())
        }.body<RiderNotificationPreferenceDto>().toDomain()

    private fun io.ktor.client.request.HttpRequestBuilder.bearer() {
        accessTokenProvider().takeIf { !it.isNullOrBlank() }?.let { bearerAuth(it) }
    }

    private fun apiUrl(path: String): String =
        "${baseUrl.trimEnd('/')}/api/${path.trimStart('/')}"
}

fun RiderNotificationPreferenceEventType.toApiEventType(): String =
    when (this) {
        RiderNotificationPreferenceEventType.ALL -> "RIDER_MISSION_OFFERED"
        else -> name
    }
