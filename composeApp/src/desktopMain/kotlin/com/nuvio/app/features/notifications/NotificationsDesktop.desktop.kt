package com.nuvio.app.features.notifications

import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.desktop.DesktopPreferences
import com.nuvio.app.desktop.DesktopRuntimeLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.AWTException
import java.awt.Image
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val scheduledJobs = ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    actual suspend fun notificationsAuthorized(): Boolean = withContext(Dispatchers.IO) {
        isWindows() && DesktopToastNotifier.isSupported()
    }

    actual suspend fun requestAuthorization(): Boolean = withContext(Dispatchers.IO) {
        if (!isWindows()) return@withContext false
        val ready = DesktopToastNotifier.initialize().isSuccess
        if (!ready) {
            DesktopRuntimeLog.warn("desktop notifications unavailable: tray icon init failed")
            NuvioToastController.show("Desktop notifications are unavailable in this build. You will still receive in-app alerts.")
        }
        ready
    }

    actual suspend fun scheduleEpisodeReleaseNotifications(requests: List<EpisodeReleaseNotificationRequest>) {
        withContext(Dispatchers.Default) {
            clearScheduledEpisodeReleaseNotifications()
            if (!isWindows()) return@withContext
            DesktopRuntimeLog.info("Desktop notifications scheduling is in-memory and resets after app restart.")
            requests.forEach { request ->
                val triggerAt = scheduledNotificationTime(request.releaseDateIso) ?: return@forEach
                val delayMs = (triggerAt.toEpochMilli() - System.currentTimeMillis()).coerceAtLeast(0L)
                val job = scope.launch {
                    delay(delayMs)
                    runCatching { showNow(request) }
                        .onFailure { DesktopRuntimeLog.warn("desktop scheduled notification failed id=${request.requestId}: ${it.message}") }
                }
                scheduledJobs[request.requestId] = job
            }
        }
    }

    actual suspend fun clearScheduledEpisodeReleaseNotifications() {
        withContext(Dispatchers.Default) {
            scheduledJobs.values.toList().forEach { it.cancel() }
            scheduledJobs.clear()
        }
    }

    actual suspend fun showTestNotification(request: EpisodeReleaseNotificationRequest) {
        withContext(Dispatchers.Default) {
            if (!isWindows()) return@withContext
            showNow(request)
        }
    }

    private suspend fun showNow(request: EpisodeReleaseNotificationRequest) {
        val result = withContext(Dispatchers.IO) {
            DesktopToastNotifier.show(
                title = request.notificationTitle,
                body = request.notificationBody,
            )
        }
        result.onFailure { throwable ->
            NuvioToastController.show("Desktop notification failed, showing in-app alert instead.")
            DesktopRuntimeLog.warn("desktop notification failed id=${request.requestId}: ${throwable.message}")
            throw throwable
        }.onSuccess {
            NuvioToastController.show("Alert sent: ${request.notificationTitle}")
        }
    }

    private fun scheduledNotificationTime(releaseDateIso: String): Instant? {
        val date = try {
            LocalDate.parse(releaseDateIso)
        } catch (_: DateTimeParseException) {
            return null
        }
        val scheduledInstant = date
            .atTime(EpisodeReleaseNotificationHour, EpisodeReleaseNotificationMinute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
        return scheduledInstant.takeIf { it.isAfter(Instant.now()) }
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name")?.lowercase(Locale.US)?.contains("windows") == true
}

private object DesktopToastNotifier {
    private val lock = Any()
    private var trayIcon: TrayIcon? = null
    @Volatile private var shutdownHookInstalled = false

    fun isSupported(): Boolean = SystemTray.isSupported()

    fun initialize(): Result<Unit> = runCatching {
        if (!SystemTray.isSupported()) error("SystemTray is not supported")
        synchronized(lock) {
            if (trayIcon != null) return@synchronized
            val tray = SystemTray.getSystemTray()
            val iconImage: Image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB).apply {
                val g = createGraphics()
                try {
                    g.color = java.awt.Color(0x62, 0x7E, 0xFF)
                    g.fillRoundRect(0, 0, 16, 16, 6, 6)
                    g.color = java.awt.Color.WHITE
                    g.drawString("N", 4, 12)
                } finally {
                    g.dispose()
                }
            }
            val icon = TrayIcon(iconImage, "Nuvio").apply {
                isImageAutoSize = true
                toolTip = "Nuvio"
            }
            try {
                tray.add(icon)
            } catch (error: AWTException) {
                throw IllegalStateException("Failed to initialize tray icon", error)
            }
            trayIcon = icon
            if (!shutdownHookInstalled) {
                Runtime.getRuntime().addShutdownHook(Thread {
                    runCatching { tray.remove(icon) }
                        .onFailure { DesktopRuntimeLog.warn("Failed to remove tray icon during shutdown: ${it.message}") }
                })
                shutdownHookInstalled = true
            }
        }
    }

    fun show(
        title: String,
        body: String,
    ): Result<Unit> = runCatching {
        initialize().getOrThrow()
        val icon = synchronized(lock) { trayIcon } ?: error("Tray icon was not initialized")
        icon.displayMessage(title.ifBlank { "Nuvio" }, body.ifBlank { "New alert" }, TrayIcon.MessageType.NONE)
    }
}

internal actual object EpisodeReleaseNotificationsClock {
    actual fun isoDateFromEpochMs(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()
}
