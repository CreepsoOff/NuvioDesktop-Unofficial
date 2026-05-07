package com.nuvio.app.features.notifications

import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.desktop.DesktopPreferences
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.time.Instant
import java.time.ZoneId

internal actual object EpisodeReleaseNotificationsStorage {
    private const val preferencesName = "nuvio_episode_release_notifications"
    private const val payloadKey = "episode_release_notifications_payload"

    actual fun loadPayload(): String? =
        DesktopPreferences.getString(preferencesName, ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        DesktopPreferences.putString(preferencesName, ProfileScopedKey.of(payloadKey), payload)
    }
}

internal actual object EpisodeReleaseNotificationPlatform {
    actual suspend fun notificationsAuthorized(): Boolean = SystemTray.isSupported()

    actual suspend fun requestAuthorization(): Boolean = SystemTray.isSupported()

    actual suspend fun scheduleEpisodeReleaseNotifications(requests: List<EpisodeReleaseNotificationRequest>) = Unit

    actual suspend fun clearScheduledEpisodeReleaseNotifications() = Unit

    actual suspend fun showTestNotification(request: EpisodeReleaseNotificationRequest) {
        if (!SystemTray.isSupported()) return
        val tray = SystemTray.getSystemTray()
        val image = runCatching {
            val iconUrl = EpisodeReleaseNotificationPlatform::class.java
                .getResource("/nuvio_notification_icon.png")
            if (iconUrl != null) {
                Toolkit.getDefaultToolkit().getImage(iconUrl)
            } else {
                createFallbackIcon()
            }
        }.getOrElse { createFallbackIcon() }
        val trayIcon = TrayIcon(image, "Nuvio")
        trayIcon.isImageAutoSize = true
        runCatching { tray.add(trayIcon) }
        trayIcon.displayMessage(
            request.notificationTitle,
            request.notificationBody,
            TrayIcon.MessageType.INFO,
        )
        kotlinx.coroutines.delay(5000)
        runCatching { tray.remove(trayIcon) }
    }

    private fun createFallbackIcon(): java.awt.Image {
        val img = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.color = java.awt.Color(229, 9, 20)
        g.fillOval(0, 0, 16, 16)
        g.dispose()
        return img
    }
}

internal actual object EpisodeReleaseNotificationsClock {
    actual fun isoDateFromEpochMs(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()
}
