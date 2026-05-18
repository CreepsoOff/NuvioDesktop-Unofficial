package com.nuvio.app.features.updater

actual object AppUpdaterPlatform {
    actual val isSupported: Boolean = false
    actual val supportsAutoCheck: Boolean = false
    actual val supportsDownloadAndInstall: Boolean = false
    actual val gitHubOwner: String = "NuvioMedia"
    actual val gitHubRepo: String = "NuvioMobile"
    actual val stableReleaseChannelBranch: String? = "cmp-rewrite"
    actual val nightlyReleaseTag: String? = null
    actual val installerAssetExtensions: List<String> = listOf(".apk")
    actual val portableZipAssetExtensions: List<String> = emptyList()
    actual val portableZipAssetNameContains: String? = null

    actual fun getSupportedAbis(): List<String> = emptyList()

    actual fun getIgnoredTag(): String? = null

    actual fun setIgnoredTag(tag: String?) = Unit

    actual fun getNightlyBuildMode(): Boolean = false

    actual fun setNightlyBuildMode(enabled: Boolean) = Unit

    actual fun prefersPortableUpdate(): Boolean = false

    actual suspend fun downloadApk(
        assetUrl: String,
        assetName: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    ): Result<String> = Result.failure(IllegalStateException("In-app updates are unavailable on this build."))

    actual fun canRequestPackageInstalls(): Boolean = false

    actual fun openUnknownSourcesSettings() = Unit

    actual fun installDownloadedApk(path: String): Result<Unit> =
        Result.failure(IllegalStateException("In-app updates are unavailable on this build."))

    actual fun openDownloadedFileLocation(path: String): Result<Unit> =
        Result.failure(IllegalStateException("Opening download location is unavailable on this build."))

    actual fun openReleasePage(url: String): Result<Unit> =
        Result.failure(IllegalStateException("Opening release pages is unavailable on this build."))
}
