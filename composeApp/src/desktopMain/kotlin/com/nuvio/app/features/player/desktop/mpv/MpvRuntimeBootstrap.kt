package com.nuvio.app.features.player.desktop.mpv

import com.nuvio.app.desktop.DesktopRuntimeLog
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.win32.StdCallLibrary
import java.io.File

internal data class MpvRuntimeBootstrapResult(
    val success: Boolean,
    val diagnostics: String,
    val error: Throwable? = null,
)

internal object MpvRuntimeBootstrap {
    private const val LOAD_LIBRARY_SEARCH_DEFAULT_DIRS = 0x00001000
    private const val LOAD_LIBRARY_SEARCH_USER_DIRS = 0x00000400

    private val isWindows: Boolean
        get() = System.getProperty("os.name")?.contains("Windows", ignoreCase = true) == true

    @Volatile private var bootstrappedDirectory: String? = null

    private val isLinux: Boolean
        get() = System.getProperty("os.name")?.contains("Linux", ignoreCase = true) == true

    @Synchronized
    fun apply(runtime: MpvRuntimeResolution): MpvRuntimeBootstrapResult {
        if (isLinux) {
            return applyLinux(runtime)
        }
        if (!isWindows) {
            return MpvRuntimeBootstrapResult(success = true, diagnostics = runtime.diagnostics)
        }
        val directory = runtime.directory
        if (directory == null || !runtime.available) {
            return MpvRuntimeBootstrapResult(
                success = false,
                diagnostics = "MPV runtime directory unresolved. ${runtime.diagnostics}",
            )
        }
        val normalized = directory.absoluteFile.safePath()
        if (bootstrappedDirectory == normalized) {
            return MpvRuntimeBootstrapResult(success = true, diagnostics = "already bootstrapped dir=$normalized")
        }

        prependJavaLibraryPath(directory)
        val kernel32 = runCatching { Native.load("kernel32", Kernel32::class.java) }
            .onFailure { DesktopRuntimeLog.error("MPV runtime bootstrap cannot load kernel32", it) }
            .getOrNull()
        if (kernel32 != null) {
            val flags = LOAD_LIBRARY_SEARCH_DEFAULT_DIRS or LOAD_LIBRARY_SEARCH_USER_DIRS
            runCatching { kernel32.SetDefaultDllDirectories(flags) }
                .onFailure { DesktopRuntimeLog.error("MPV runtime bootstrap SetDefaultDllDirectories failed", it) }
            runCatching { kernel32.AddDllDirectory(WString(directory.absolutePath)) }
                .onFailure { DesktopRuntimeLog.error("MPV runtime bootstrap AddDllDirectory failed dir=$normalized", it) }
            runCatching { kernel32.SetDllDirectoryW(WString(directory.absolutePath)) }
                .onFailure { DesktopRuntimeLog.error("MPV runtime bootstrap SetDllDirectoryW failed dir=$normalized", it) }
        }

        val mediampDll = directory.resolve("mediampv.dll")
        return runCatching {
            System.load(mediampDll.absolutePath)
        }.fold(
            onSuccess = {
                bootstrappedDirectory = normalized
                DesktopRuntimeLog.info("MPV runtime bootstrap loaded dll=${mediampDll.safePath()}")
                MpvRuntimeBootstrapResult(success = true, diagnostics = "loaded=${mediampDll.safePath()}")
            },
            onFailure = { throwable ->
                if (throwable.message?.contains("already loaded", ignoreCase = true) == true) {
                    bootstrappedDirectory = normalized
                    MpvRuntimeBootstrapResult(success = true, diagnostics = "already loaded dll=${mediampDll.safePath()}")
                } else {
                    MpvRuntimeBootstrapResult(
                        success = false,
                        diagnostics = "System.load failed dll=${mediampDll.safePath()} runtime=${runtime.diagnostics}",
                        error = throwable,
                    )
                }
            },
        )
    }

    private fun applyLinux(runtime: MpvRuntimeResolution): MpvRuntimeBootstrapResult {
        val directory = runtime.directory
        if (directory != null) {
            prependJavaLibraryPath(directory)
            val mediampSo = directory.resolve("libmediampv.so")
            if (mediampSo.isFile) {
                return runCatching {
                    System.load(mediampSo.absolutePath)
                }.fold(
                    onSuccess = {
                        bootstrappedDirectory = directory.absoluteFile.safePath()
                        DesktopRuntimeLog.info("MPV runtime bootstrap loaded so=${mediampSo.safePath()}")
                        MpvRuntimeBootstrapResult(success = true, diagnostics = "loaded=${mediampSo.safePath()}")
                    },
                    onFailure = { throwable ->
                        if (throwable.message?.contains("already loaded", ignoreCase = true) == true) {
                            bootstrappedDirectory = directory.absoluteFile.safePath()
                            MpvRuntimeBootstrapResult(success = true, diagnostics = "already loaded so=${mediampSo.safePath()}")
                        } else {
                            MpvRuntimeBootstrapResult(
                                success = false,
                                diagnostics = "System.load failed so=${mediampSo.safePath()} runtime=${runtime.diagnostics}",
                                error = throwable,
                            )
                        }
                    },
                )
            }
        }
        return runCatching {
            System.loadLibrary("mediampv")
        }.fold(
            onSuccess = {
                DesktopRuntimeLog.info("MPV runtime bootstrap loaded mediampv via system loadLibrary")
                MpvRuntimeBootstrapResult(success = true, diagnostics = "loaded via System.loadLibrary")
            },
            onFailure = { throwable ->
                if (throwable.message?.contains("already loaded", ignoreCase = true) == true) {
                    MpvRuntimeBootstrapResult(success = true, diagnostics = "already loaded via System.loadLibrary")
                } else {
                    MpvRuntimeBootstrapResult(
                        success = false,
                        diagnostics = "Linux loadLibrary failed: ${throwable.message} runtime=${runtime.diagnostics}",
                        error = throwable,
                    )
                }
            },
        )
    }

    private fun prependJavaLibraryPath(directory: File) {
        val current = System.getProperty("java.library.path").orEmpty()
        val path = directory.absolutePath
        val entries = current.split(File.pathSeparatorChar).filter { it.isNotBlank() }
        if (entries.any { File(it).absolutePath.equals(path, ignoreCase = true) }) return
        System.setProperty("java.library.path", (listOf(path) + entries).joinToString(File.pathSeparator))
    }

    private interface Kernel32 : StdCallLibrary, Library {
        fun SetDefaultDllDirectories(directoryFlags: Int): Boolean
        fun AddDllDirectory(newDirectory: WString): Pointer?
        fun SetDllDirectoryW(pathName: WString): Boolean
    }
}
