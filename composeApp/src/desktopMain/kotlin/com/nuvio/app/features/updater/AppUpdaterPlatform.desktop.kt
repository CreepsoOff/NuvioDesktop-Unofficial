package com.nuvio.app.features.updater

import com.nuvio.app.desktop.DesktopPreferences
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.Locale
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

actual object AppUpdaterPlatform {
    private const val preferencesName = "nuvio_updater"
    private const val ignoredTagKey = "ignored_release_tag"
    private const val nightlyBuildModeKey = "nightly_build_mode"
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    actual val isSupported: Boolean = true
    actual val supportsAutoCheck: Boolean = true
    actual val supportsDownloadAndInstall: Boolean = true
    actual val gitHubOwner: String = "CreepsoOff"
    actual val gitHubRepo: String = "NuvioDesktop"
    actual val stableReleaseChannelBranch: String? = null
    actual val nightlyReleaseTag: String? = "pre"
    actual val installerAssetExtensions: List<String> = listOf(".exe", ".msi")
    actual val portableZipAssetExtensions: List<String> = listOf(".zip")
    actual val portableZipAssetNameContains: String? = "portable"

    actual fun getSupportedAbis(): List<String> = emptyList()

    actual fun getIgnoredTag(): String? =
        DesktopPreferences.getString(preferencesName, ignoredTagKey)

    actual fun setIgnoredTag(tag: String?) {
        DesktopPreferences.putNullableString(preferencesName, ignoredTagKey, tag)
    }

    actual fun getNightlyBuildMode(): Boolean =
        DesktopPreferences.getBoolean(preferencesName, nightlyBuildModeKey) ?: false

    actual fun setNightlyBuildMode(enabled: Boolean) {
        DesktopPreferences.putBoolean(preferencesName, nightlyBuildModeKey, enabled)
    }

    actual fun prefersPortableUpdate(): Boolean {
        val executablePath = runCatching { ProcessHandle.current().info().command().orElse(null) }.getOrNull()
            ?: return false
        val executable = File(executablePath)
        val marker = File(executable.parentFile ?: return false, "Nuvio.portable")
        return marker.exists()
    }

    actual suspend fun downloadApk(
        assetUrl: String,
        assetName: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    ): Result<String> = runCatching {
        val updatesDir = File(System.getProperty("java.io.tmpdir"), "nuvio-updates")
        if (!updatesDir.exists()) {
            check(updatesDir.mkdirs()) { "Unable to create update directory." }
        }

        val safeFileName = assetName
            .substringAfterLast('/')
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .ifBlank { "Nuvio-update.bin" }
        val destination = File(updatesDir, safeFileName)
        if (destination.exists()) {
            destination.delete()
        }

        val request = Request.Builder()
            .url(assetUrl)
            .header("User-Agent", "Nuvio")
            .build()

        var totalSize: Long? = null
        httpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) {
                "Update download failed: HTTP ${response.code}"
            }

            val body = checkNotNull(response.body) { "Update download body is empty." }
            totalSize = body.contentLength().takeIf { it > 0L }
            body.byteStream().use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, totalSize)
                    }
                    output.flush()
                }
            }
        }
        onProgress(destination.length(), totalSize ?: destination.length())
        destination.absolutePath
    }

    actual fun canRequestPackageInstalls(): Boolean = true

    actual fun openUnknownSourcesSettings() = Unit

    actual fun installDownloadedApk(path: String): Result<Unit> = runCatching {
        val installer = File(path)
        check(installer.exists()) { "Downloaded update file no longer exists." }
        val extension = installer.extension.lowercase(Locale.US)
        check(extension == "exe" || extension == "msi") { "Unsupported installer format: .$extension" }
        val desktop = checkNotNull(Desktop.getDesktop()) { "Desktop file launcher is unavailable." }
        check(desktop.isSupported(Desktop.Action.OPEN)) { "Opening downloaded updates is unavailable." }
        desktop.open(installer)
    }

    actual fun openDownloadedFileLocation(path: String): Result<Unit> = runCatching {
        val downloaded = File(path)
        check(downloaded.exists()) { "Downloaded update file no longer exists." }
        val parent = downloaded.parentFile ?: downloaded
        val desktop = checkNotNull(Desktop.getDesktop()) { "Desktop file launcher is unavailable." }
        check(desktop.isSupported(Desktop.Action.OPEN)) { "Opening folders is unavailable on this system." }
        desktop.open(parent)
    }

    actual fun openReleasePage(url: String): Result<Unit> = runCatching {
        val desktop = checkNotNull(Desktop.getDesktop()) { "Desktop browser integration is unavailable." }
        check(desktop.isSupported(Desktop.Action.BROWSE)) { "Opening links is unavailable on this system." }
        desktop.browse(URI(url))
    }
}
