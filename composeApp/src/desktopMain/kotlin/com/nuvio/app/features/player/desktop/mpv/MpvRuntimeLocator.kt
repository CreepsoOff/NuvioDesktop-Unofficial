package com.nuvio.app.features.player.desktop.mpv

import java.io.File

internal data class MpvRuntimeResolution(
    val directory: File?,
    val checkedDirectories: List<String>,
    val diagnostics: String,
) {
    val available: Boolean get() = directory?.resolve(nativeLibName)?.isFile == true

    companion object {
        val nativeLibName: String get() = when {
            System.getProperty("os.name")?.contains("Windows", ignoreCase = true) == true -> "mediampv.dll"
            System.getProperty("os.name")?.contains("Mac", ignoreCase = true) == true -> "libmediampv.dylib"
            else -> "libmediampv.so"
        }
    }
}

internal object MpvRuntimeLocator {
    private val isWindows: Boolean
        get() = System.getProperty("os.name")?.contains("Windows", ignoreCase = true) == true
    private val isLinux: Boolean
        get() = System.getProperty("os.name")?.contains("Linux", ignoreCase = true) == true
    private val isDesktopSupported: Boolean
        get() = isWindows || isLinux

    fun resolve(): MpvRuntimeResolution {
        if (!isDesktopSupported) {
            return MpvRuntimeResolution(
                directory = null,
                checkedDirectories = emptyList(),
                diagnostics = "unsupported platform; explicit MPV runtime bootstrap skipped",
            )
        }

        val candidates = linkedMapOf<String, File>()
        fun add(label: String, file: File?) {
            if (file != null) candidates.putIfAbsent(label, file)
        }

        val resourcesDir = System.getProperty("compose.application.resources.dir")
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
        val appDir = resourcesDir?.parentFile
        add("appDir/native", appDir?.resolve("native"))
        add("resourcesDir/native", resourcesDir?.resolve("native"))

        add("env:NUVIO_MEDIAMP_RUNTIME_DIR", System.getenv("NUVIO_MEDIAMP_RUNTIME_DIR")?.toFileOrNull())
        System.getenv("NUVIO_MPV_DIR")?.toFileOrNull()?.let { dir ->
            add("env:NUVIO_MPV_DIR", dir)
            add("env:NUVIO_MPV_DIR/bin", dir.resolve("bin"))
        }
        add("property:nuvio.mediamp.runtime.dir", System.getProperty("nuvio.mediamp.runtime.dir")?.toFileOrNull())
        System.getProperty("nuvio.mpv.dir")?.toFileOrNull()?.let { dir ->
            add("property:nuvio.mpv.dir", dir)
            add("property:nuvio.mpv.dir/bin", dir.resolve("bin"))
        }

        javaLibraryPathEntries().forEach { entry ->
            val dir = File(entry)
            add("java.library.path:${dir.safePath()}", dir)
            add("java.library.path/native:${dir.safePath()}", dir.resolve("native"))
        }

        pathEntries().forEach { entry ->
            add("PATH:${entry.safePath()}", entry)
        }

        if (devLookupEnabled() || isLinux) {
            System.getProperty("user.dir")?.takeIf { it.isNotBlank() }?.let { userDir ->
                val base = File(userDir)
                add("dev:app/native", base.resolve("app/native"))
                add("dev:native", base.resolve("native"))
                add("dev:mediamp/build-ci", base.resolve("mediamp/mediamp-mpv/build-ci"))
                add("dev:mediamp/build-ci/Release", base.resolve("mediamp/mediamp-mpv/build-ci/Release"))
                if (isWindows) {
                    add("dev:mediamp/libmpv", base.resolve("mediamp/mediamp-mpv/libmpv/lib/windows/x86_64"))
                }
            }
        }

        if (isLinux) {
            add("system:/usr/lib", File("/usr/lib"))
            add("system:/usr/lib/x86_64-linux-gnu", File("/usr/lib/x86_64-linux-gnu"))
            add("system:/usr/local/lib", File("/usr/local/lib"))
        }

        val libName = MpvRuntimeResolution.nativeLibName
        val checked = candidates.map { (label, dir) ->
            "$label=${dir.safePath()} exists=${dir.isDirectory} nativeLib=${dir.resolve(libName).isFile}"
        }
        val selected = candidates.values.firstOrNull { it.resolve(libName).isFile }
        return MpvRuntimeResolution(
            directory = selected,
            checkedDirectories = checked,
            diagnostics = "selected=${selected?.safePath() ?: "none"} checked=${checked.joinToString(" | ")}",
        )
    }

    private fun devLookupEnabled(): Boolean =
        System.getenv("NUVIO_DEV_PLAYER_LOOKUP").equals("true", ignoreCase = true) ||
            System.getProperty("nuvio.dev.player.lookup").equals("true", ignoreCase = true)

    private fun javaLibraryPathEntries(): List<String> =
        System.getProperty("java.library.path")
            ?.split(File.pathSeparatorChar)
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            .orEmpty()

    private fun pathEntries(): List<File> =
        System.getenv("PATH")
            ?.split(File.pathSeparatorChar)
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.map(::File)
            .orEmpty()

    private fun String.toFileOrNull(): File? =
        takeIf { it.isNotBlank() }?.let(::File)
}

internal fun File.safePath(): String = absolutePath.replace("\\", "/")
