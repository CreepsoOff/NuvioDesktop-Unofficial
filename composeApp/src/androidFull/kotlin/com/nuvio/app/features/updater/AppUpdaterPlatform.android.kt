package com.nuvio.app.features.updater

actual object AppUpdaterPlatform {
    actual val isSupported: Boolean = true
    actual val supportsAutoCheck: Boolean = true
    actual val supportsDownloadAndInstall: Boolean = true
    actual val gitHubOwner: String = "NuvioMedia"
    actual val gitHubRepo: String = "NuvioMobile"
    actual val stableReleaseChannelBranch: String? = "cmp-rewrite"
    actual val nightlyReleaseTag: String? = null
    actual val installerAssetExtensions: List<String> = listOf(".apk")
    actual val portableZipAssetExtensions: List<String> = emptyList()
    actual val portableZipAssetNameContains: String? = null

    actual fun getSupportedAbis(): List<String> = AndroidAppUpdaterPlatform.getSupportedAbis()

    actual fun getIgnoredTag(): String? = AndroidAppUpdaterPlatform.getIgnoredTag()

    actual fun setIgnoredTag(tag: String?) {
        AndroidAppUpdaterPlatform.setIgnoredTag(tag)
    }

    actual fun getNightlyBuildMode(): Boolean = false

    actual fun setNightlyBuildMode(enabled: Boolean) = Unit

    actual fun prefersPortableUpdate(): Boolean = false

    actual suspend fun downloadApk(
        assetUrl: String,
        assetName: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    ): Result<String> = AndroidAppUpdaterPlatform.downloadApk(assetUrl, assetName, onProgress)

    actual fun canRequestPackageInstalls(): Boolean = AndroidAppUpdaterPlatform.canRequestPackageInstalls()

    actual fun openUnknownSourcesSettings() {
        AndroidAppUpdaterPlatform.openUnknownSourcesSettings()
    }

    actual fun installDownloadedApk(path: String): Result<Unit> = AndroidAppUpdaterPlatform.installDownloadedApk(path)

    actual fun openDownloadedFileLocation(path: String): Result<Unit> =
        Result.failure(IllegalStateException("Opening download location is unavailable on this build."))

    actual fun openReleasePage(url: String): Result<Unit> =
        Result.failure(IllegalStateException("Opening release pages is unavailable on this build."))
}
