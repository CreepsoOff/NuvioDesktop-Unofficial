package com.nuvio.app.features.updater

expect object AppUpdaterPlatform {
    val isSupported: Boolean
    val supportsAutoCheck: Boolean
    val supportsDownloadAndInstall: Boolean
    val gitHubOwner: String
    val gitHubRepo: String
    val stableReleaseChannelBranch: String?
    val nightlyReleaseTag: String?

    /** Desktop: [".exe",".msi"] ; Android: [".apk"] */
    val installerAssetExtensions: List<String>

    /** Desktop: [".zip"] for portable builds; Android/iOS: empty. */
    val portableZipAssetExtensions: List<String>

    /** Hint used to distinguish portable assets when [portableZipAssetExtensions] is set. */
    val portableZipAssetNameContains: String?

    fun getSupportedAbis(): List<String>

    fun getIgnoredTag(): String?

    fun setIgnoredTag(tag: String?)

    fun getNightlyBuildMode(): Boolean

    fun setNightlyBuildMode(enabled: Boolean)

    fun prefersPortableUpdate(): Boolean

    suspend fun downloadApk(
        assetUrl: String,
        assetName: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    ): Result<String>

    fun canRequestPackageInstalls(): Boolean

    fun openUnknownSourcesSettings()

    fun installDownloadedApk(path: String): Result<Unit>

    fun openDownloadedFileLocation(path: String): Result<Unit>

    fun openReleasePage(url: String): Result<Unit>
}
