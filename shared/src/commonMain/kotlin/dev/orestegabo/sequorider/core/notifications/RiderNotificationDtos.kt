package dev.orestegabo.sequorider.core.notifications

import dev.orestegabo.sequorider.model.RiderNotification
import dev.orestegabo.sequorider.model.RiderNotificationEventType
import dev.orestegabo.sequorider.model.RiderNotificationPreference
import dev.orestegabo.sequorider.model.RiderNotificationPreferenceEventType
import dev.orestegabo.sequorider.model.RiderNotificationSeverity
import kotlinx.serialization.Serializable

internal const val SequoRiderAppFamily = "SEQUO_RIDER"

@Serializable
data class RegisterRiderFcmTokenRequestDto(
    val deviceId: String,
    val fcmToken: String,
    val appFamily: String = SequoRiderAppFamily,
    val platform: String,
    val appVersion: String? = null,
    val locale: String? = null,
    val timezone: String? = null,
)

@Serializable
data class RiderDeviceFcmTokenResponseDto(
    val id: String,
    val deviceId: String,
    val appFamily: String,
    val platform: String,
    val status: String,
    val appVersion: String? = null,
    val locale: String? = null,
    val timezone: String? = null,
    val lastSeenAt: String? = null,
)

@Serializable
data class RiderNotificationMessageDto(
    val id: String,
    val eventId: String,
    val recipientUserId: String,
    val appFamily: String,
    val eventType: String,
    val severity: String,
    val title: String,
    val body: String,
    val actionUrl: String? = null,
    val payload: String? = null,
    val readAt: String? = null,
    val archivedAt: String? = null,
    val createdAt: String,
)

@Serializable
data class SaveRiderNotificationPreferenceRequestDto(
    val eventType: String = "ALL",
    val pushEnabled: Boolean = true,
    val inAppEnabled: Boolean = true,
    val smsEnabled: Boolean = false,
    val quietHoursStart: String? = null,
    val quietHoursEnd: String? = null,
)

@Serializable
data class RiderNotificationPreferenceDto(
    val id: String? = null,
    val appFamily: String = SequoRiderAppFamily,
    val eventType: String = "ALL",
    val pushEnabled: Boolean = true,
    val inAppEnabled: Boolean = true,
    val smsEnabled: Boolean = false,
    val quietHoursStart: String? = null,
    val quietHoursEnd: String? = null,
)

fun RiderNotificationMessageDto.toDomain(): RiderNotification =
    RiderNotification(
        id = id,
        eventId = eventId,
        appFamily = appFamily,
        eventType = eventType.toRiderNotificationEventType(),
        severity = severity.toRiderNotificationSeverity(),
        title = title,
        body = body,
        actionUrl = actionUrl,
        readAt = readAt,
        archivedAt = archivedAt,
        createdAt = createdAt,
    )

fun RiderNotificationPreferenceDto.toDomain(): RiderNotificationPreference =
    RiderNotificationPreference(
        id = id,
        appFamily = appFamily,
        eventType = eventType.toRiderNotificationPreferenceEventType(),
        pushEnabled = pushEnabled,
        inAppEnabled = inAppEnabled,
        smsEnabled = smsEnabled,
        quietHoursStart = quietHoursStart,
        quietHoursEnd = quietHoursEnd,
    )

fun RiderNotificationPreference.toDto(): SaveRiderNotificationPreferenceRequestDto =
    SaveRiderNotificationPreferenceRequestDto(
        eventType = eventType.name,
        pushEnabled = pushEnabled,
        inAppEnabled = inAppEnabled,
        smsEnabled = smsEnabled,
        quietHoursStart = quietHoursStart,
        quietHoursEnd = quietHoursEnd,
    )

private fun String.toRiderNotificationEventType(): RiderNotificationEventType =
    runCatching { RiderNotificationEventType.valueOf(uppercase()) }
        .getOrDefault(RiderNotificationEventType.RIDER_MISSION_OFFERED)

private fun String.toRiderNotificationSeverity(): RiderNotificationSeverity =
    runCatching { RiderNotificationSeverity.valueOf(uppercase()) }
        .getOrDefault(RiderNotificationSeverity.INFO)

private fun String.toRiderNotificationPreferenceEventType(): RiderNotificationPreferenceEventType =
    runCatching { RiderNotificationPreferenceEventType.valueOf(uppercase()) }
        .getOrDefault(RiderNotificationPreferenceEventType.ALL)
