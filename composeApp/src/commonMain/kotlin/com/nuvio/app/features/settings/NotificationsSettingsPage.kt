package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationsRepository
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationsUiState

internal fun LazyListScope.notificationsSettingsContent(
    isTablet: Boolean,
    uiState: EpisodeReleaseNotificationsUiState,
) {
    item {
        SettingsSection(
            title = "ALERTS",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = "Episode release alerts",
                    description = "Schedule local notifications when a new episode for a saved show becomes available.",
                    checked = uiState.isEnabled,
                    enabled = !uiState.isLoading,
                    isTablet = isTablet,
                    onCheckedChange = EpisodeReleaseNotificationsRepository::setEnabled,
                )
            }
        }
    }

    item {
        SettingsSection(
            title = "TEST",
            isTablet = isTablet,
        ) {
            NotificationTestCard(
                isTablet = isTablet,
                uiState = uiState,
            )
        }
    }
}

@Composable
private fun NotificationTestCard(
    isTablet: Boolean,
    uiState: EpisodeReleaseNotificationsUiState,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 18.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Test notification",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = uiState.testTargetTitle?.let { title ->
                        "Send a local test notification for $title."
                    } ?: "Save a show to your library first to test notifications.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (uiState.isEnabled) {
                        "${uiState.scheduledCount} release alerts are currently scheduled on this device."
                    } else {
                        "Notifications are currently disabled in Nuvio."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = EpisodeReleaseNotificationsRepository::sendTestNotification,
                enabled = !uiState.isSendingTest && !uiState.isLoading && uiState.testTargetTitle != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(if (uiState.isSendingTest) "Sending Test Notification..." else "Send Test Notification")
            }

            uiState.statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (!uiState.permissionGranted) {
                Text(
                    text = "System notifications are disabled for Nuvio. Enable them to receive alerts and test notifications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}