package dev.orestegabo.sequorider.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.orestegabo.sequorider.model.RiderAvailability
import dev.orestegabo.sequorider.model.RiderNotification
import dev.orestegabo.sequorider.model.RiderNotificationEventType
import dev.orestegabo.sequorider.model.RiderNotificationPreference
import dev.orestegabo.sequorider.model.RiderNotificationPreferenceEventType
import dev.orestegabo.sequorider.model.RiderNotificationSeverity
import dev.orestegabo.sequorider.model.RiderProfile
import dev.orestegabo.sequorider.ui.components.RiderCard
import dev.orestegabo.sequorider.ui.components.RiderKeyValueRow
import dev.orestegabo.sequorider.ui.components.RiderStatusPill

@Composable
fun AccountSettingsScreen(
    profile: RiderProfile,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    notifications: List<RiderNotification>,
    notificationPreference: RiderNotificationPreference,
    notificationsLoading: Boolean,
    notificationsOffline: Boolean,
    notificationActionFailed: Boolean,
    notificationPreferenceSaved: Boolean,
    includeArchivedNotifications: Boolean,
    onIncludeArchivedNotificationsChange: (Boolean) -> Unit,
    onNotificationsRefresh: () -> Unit,
    onNotificationRead: (RiderNotification) -> Unit,
    onNotificationArchive: (RiderNotification) -> Unit,
    onNotificationUnarchive: (RiderNotification) -> Unit,
    onNotificationPreferenceChange: (RiderNotificationPreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleNotifications = notifications.filter { includeArchivedNotifications || !it.archived }
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(appText(RiderText.AccountSettings), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            RiderCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(profile.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(appText(RiderText.LocalDemo), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RiderStatusPill(profile.availability.label())
                    }
                    RiderKeyValueRow(appText(RiderText.Fleet), profile.fleetClass.name)
                    RiderKeyValueRow(appText(RiderText.ServiceZone), profile.serviceZone)
                    RiderKeyValueRow(appText(RiderText.KycApproved), if (profile.kycApproved) appText(RiderText.Completed) else appText(RiderText.Pending))
                }
            }
        }
        item {
            RiderCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(appText(RiderText.Language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.FilterChip(
                            selected = language == AppLanguage.English,
                            onClick = { onLanguageChange(AppLanguage.English) },
                            label = { Text(appText(RiderText.English)) },
                            modifier = Modifier.weight(1f),
                        )
                        androidx.compose.material3.FilterChip(
                            selected = language == AppLanguage.French,
                            onClick = { onLanguageChange(AppLanguage.French) },
                            label = { Text(appText(RiderText.French)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    RiderKeyValueRow(appText(RiderText.Theme), appText(RiderText.System))
                }
            }
        }
        item {
            NotificationPreferencesCard(
                preference = notificationPreference,
                preferenceSaved = notificationPreferenceSaved,
                onPreferenceChange = onNotificationPreferenceChange,
            )
        }
        item {
            RiderCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(appText(RiderText.NotificationInbox), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(appText(RiderText.Notifications), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = onNotificationsRefresh) {
                            Text(appText(RiderText.Refresh))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(appText(RiderText.NotificationShowArchived), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Switch(checked = includeArchivedNotifications, onCheckedChange = onIncludeArchivedNotificationsChange)
                    }
                    if (notificationsLoading) {
                        Text(appText(RiderText.NotificationLoading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (notificationsOffline) {
                        Text(appText(RiderText.NotificationOffline), color = MaterialTheme.colorScheme.error)
                    }
                    if (notificationActionFailed) {
                        Text(appText(RiderText.NotificationActionFailed), color = MaterialTheme.colorScheme.error)
                    }
                    if (visibleNotifications.isEmpty()) {
                        Text(appText(RiderText.NotificationEmpty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        visibleNotifications.forEach { notification ->
                            NotificationRow(
                                notification = notification,
                                onMarkRead = onNotificationRead,
                                onArchive = onNotificationArchive,
                                onUnarchive = onNotificationUnarchive,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationPreferencesCard(
    preference: RiderNotificationPreference,
    preferenceSaved: Boolean,
    onPreferenceChange: (RiderNotificationPreference) -> Unit,
) {
    RiderCard {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(appText(RiderText.NotificationPreferences), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            NotificationSwitch(appText(RiderText.NotificationPush), preference.pushEnabled) {
                onPreferenceChange(preference.copy(eventType = RiderNotificationPreferenceEventType.ALL, pushEnabled = it))
            }
            NotificationSwitch(appText(RiderText.NotificationInApp), preference.inAppEnabled) {
                onPreferenceChange(preference.copy(eventType = RiderNotificationPreferenceEventType.ALL, inAppEnabled = it))
            }
            NotificationSwitch(appText(RiderText.NotificationSms), preference.smsEnabled) {
                onPreferenceChange(preference.copy(eventType = RiderNotificationPreferenceEventType.ALL, smsEnabled = it))
            }
            if (preferenceSaved) {
                Text(appText(RiderText.NotificationPreferenceSaved), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun NotificationSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NotificationRow(
    notification: RiderNotification,
    onMarkRead: (RiderNotification) -> Unit,
    onArchive: (RiderNotification) -> Unit,
    onUnarchive: (RiderNotification) -> Unit,
) {
    RiderCard {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(notification.categoryLabel(), style = MaterialTheme.typography.labelMedium, color = notification.severityColor())
                    Text(notification.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                if (notification.unread) {
                    RiderStatusPill(appText(RiderText.Pending))
                }
            }
            Text(notification.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            RiderKeyValueRow(appText(RiderText.CreatedAt), notification.createdAt)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { onMarkRead(notification) },
                    enabled = notification.unread,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(appText(RiderText.NotificationMarkRead))
                }
                Button(
                    onClick = {
                        if (notification.archived) {
                            onUnarchive(notification)
                        } else {
                            onArchive(notification)
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (notification.archived) appText(RiderText.NotificationUnarchive) else appText(RiderText.NotificationArchive))
                }
            }
        }
    }
}

@Composable
private fun RiderNotification.categoryLabel(): String =
    when (eventType) {
        RiderNotificationEventType.RIDER_MISSION_OFFERED -> appText(RiderText.NotificationNewMission)
        RiderNotificationEventType.RIDER_ACCEPTED_MISSION,
        RiderNotificationEventType.RIDER_PICKED_UP,
        RiderNotificationEventType.RIDER_ARRIVED,
        -> appText(RiderText.NotificationRouteUpdate)
        RiderNotificationEventType.DELIVERY_PROBLEM_REPORTED -> appText(RiderText.NotificationDeliveryProblem)
        RiderNotificationEventType.MISSING_DEPOT_BLOCKED_TOUR -> appText(RiderText.NotificationPickupProblem)
        RiderNotificationEventType.PAYOUT_SENT -> appText(RiderText.NotificationPayoutStatus)
    }

@Composable
private fun RiderNotification.severityColor() =
    when (severity) {
        RiderNotificationSeverity.URGENT,
        RiderNotificationSeverity.ACTION_REQUIRED,
        -> MaterialTheme.colorScheme.error
        RiderNotificationSeverity.FINANCIAL -> MaterialTheme.colorScheme.primary
        RiderNotificationSeverity.INFO,
        RiderNotificationSeverity.SECURITY,
        -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
private fun RiderAvailability.label(): String = when (this) {
    RiderAvailability.Available -> appText(RiderText.Available)
    RiderAvailability.OnMission -> appText(RiderText.OnMission)
    RiderAvailability.Offline -> appText(RiderText.Offline)
}
