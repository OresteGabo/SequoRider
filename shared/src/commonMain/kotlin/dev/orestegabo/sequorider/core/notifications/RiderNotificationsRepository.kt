package dev.orestegabo.sequorider.core.notifications

import dev.orestegabo.sequorider.model.RiderNotification
import dev.orestegabo.sequorider.model.RiderNotificationPreference
import dev.orestegabo.sequorider.model.RiderNotificationPreferenceEventType

interface RiderNotificationsRepository {
    suspend fun registerFcmToken(deviceId: String, fcmToken: String, platform: String): Unit
    suspend fun revokeDevice(deviceId: String): Unit
    suspend fun inbox(includeArchived: Boolean = false): List<RiderNotification>
    suspend fun markRead(messageId: String): RiderNotification
    suspend fun archive(messageId: String): RiderNotification
    suspend fun unarchive(messageId: String): RiderNotification
    suspend fun effectivePreference(eventType: RiderNotificationPreferenceEventType): RiderNotificationPreference
    suspend fun savePreference(preference: RiderNotificationPreference): RiderNotificationPreference
}

class NetworkRiderNotificationsRepository(
    private val apiClient: RiderNotificationsApiClient,
) : RiderNotificationsRepository {
    override suspend fun registerFcmToken(deviceId: String, fcmToken: String, platform: String) {
        apiClient.registerFcmToken(deviceId, fcmToken, platform)
    }

    override suspend fun revokeDevice(deviceId: String) {
        apiClient.revokeDevice(deviceId)
    }

    override suspend fun inbox(includeArchived: Boolean): List<RiderNotification> =
        apiClient.inbox(includeArchived = includeArchived)

    override suspend fun markRead(messageId: String): RiderNotification =
        apiClient.markRead(messageId)

    override suspend fun archive(messageId: String): RiderNotification =
        apiClient.archive(messageId)

    override suspend fun unarchive(messageId: String): RiderNotification =
        apiClient.unarchive(messageId)

    override suspend fun effectivePreference(eventType: RiderNotificationPreferenceEventType): RiderNotificationPreference =
        apiClient.effectivePreference(eventType.toApiEventType())

    override suspend fun savePreference(preference: RiderNotificationPreference): RiderNotificationPreference =
        apiClient.savePreference(preference)
}

class DemoRiderNotificationsRepository(
    initialNotifications: List<RiderNotification>,
) : RiderNotificationsRepository {
    private var messages = initialNotifications
    private var preference = RiderNotificationPreference()

    override suspend fun registerFcmToken(deviceId: String, fcmToken: String, platform: String) = Unit

    override suspend fun revokeDevice(deviceId: String) = Unit

    override suspend fun inbox(includeArchived: Boolean): List<RiderNotification> =
        messages.filter { includeArchived || !it.archived }

    override suspend fun markRead(messageId: String): RiderNotification =
        update(messageId) { it.copy(readAt = it.readAt ?: "Local update") }

    override suspend fun archive(messageId: String): RiderNotification =
        update(messageId) { it.copy(readAt = it.readAt ?: "Local update", archivedAt = it.archivedAt ?: "Local update") }

    override suspend fun unarchive(messageId: String): RiderNotification =
        update(messageId) { it.copy(archivedAt = null) }

    override suspend fun effectivePreference(eventType: RiderNotificationPreferenceEventType): RiderNotificationPreference =
        preference.copy(eventType = eventType)

    override suspend fun savePreference(preference: RiderNotificationPreference): RiderNotificationPreference {
        this.preference = preference
        return preference
    }

    private fun update(
        messageId: String,
        transform: (RiderNotification) -> RiderNotification,
    ): RiderNotification {
        val updated = transform(messages.first { it.id == messageId })
        messages = messages.map { if (it.id == messageId) updated else it }
        return updated
    }
}
