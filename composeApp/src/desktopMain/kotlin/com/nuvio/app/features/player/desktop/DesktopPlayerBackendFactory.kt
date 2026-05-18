package com.nuvio.app.features.player.desktop

import com.nuvio.app.desktop.DesktopRuntimeLog
import com.nuvio.app.features.player.desktop.mpv.MpvDesktopPlayerBackend
import com.nuvio.app.features.player.desktop.mpv.MpvRuntimeBootstrap
import com.nuvio.app.features.player.desktop.mpv.MpvRuntimeLocator
import com.nuvio.app.features.player.desktop.nativebridge.NativeBridgeDesktopPlayerBackend
import com.nuvio.app.features.player.desktop.nativebridge.NativeBridgeRuntimeLocator

internal object DesktopPlayerBackendFactory {
    private const val BACKEND_PROPERTY = "nuvio.windows.player.backend"
    private const val BACKEND_ENV = "NUVIO_WINDOWS_PLAYER_BACKEND"

    fun createWindowsBackend(): DesktopPlayerBackend {
        val selection = DesktopPlayerBackendSelection.resolve()
        DesktopRuntimeLog.info("Selected Windows player backend request=${selection.value} source=${selection.source}")
        return when (selection.backend) {
            DesktopPlayerBackendKind.None -> unavailable(
                backendName = "windows-none",
                technicalMessage = "Windows player backend disabled by configuration.",
                selection = selection,
            )
            DesktopPlayerBackendKind.Mpv -> createMpvOrUnavailable(selection)
            DesktopPlayerBackendKind.Auto -> createMpvOrUnavailable(selection)
            DesktopPlayerBackendKind.Native -> createNativeWithMpvFallback(selection)
        }
    }

    private fun createMpvOrUnavailable(selection: DesktopPlayerBackendSelection): DesktopPlayerBackend =
        createMpvOrNull(selection) ?: unavailable(
            backendName = "windows-mediamp-mpv",
            technicalMessage = "MPV backend is unavailable.",
            selection = selection,
        )

    private fun createNativeWithMpvFallback(selection: DesktopPlayerBackendSelection): DesktopPlayerBackend {
        val nativeRuntime = NativeBridgeRuntimeLocator.resolve()
        if (!nativeRuntime.available) {
            DesktopRuntimeLog.warn(
                "Windows native bridge unavailable before playback; trying MPV fallback diagnostics=${nativeRuntime.diagnostics}",
            )
            return createMpvOrUnavailable(selection)
        }
        return NativeBridgeDesktopPlayerBackend.create()
            .onSuccess {
                DesktopRuntimeLog.info("Selected player backend=${it.backendName} (source=${selection.source} request=${selection.value})")
            }
            .getOrElse { throwable ->
                DesktopRuntimeLog.error("Windows native bridge init failed; trying MPV fallback", throwable)
                createMpvOrUnavailable(selection)
            }
    }

    private fun createMpvOrNull(selection: DesktopPlayerBackendSelection): DesktopPlayerBackend? {
        val runtime = MpvRuntimeLocator.resolve()
        val bootstrap = MpvRuntimeBootstrap.apply(runtime)
        if (!bootstrap.success) {
            DesktopRuntimeLog.error("MPV runtime bootstrap failed diagnostics=${bootstrap.diagnostics}", bootstrap.error)
            return null
        }
        return MpvDesktopPlayerBackend.create(runtime)
            .onSuccess {
                DesktopRuntimeLog.info("Selected player backend=${it.backendName} (source=${selection.source} request=${selection.value})")
            }
            .onFailure { DesktopRuntimeLog.error("MPV backend init failed", it) }
            .getOrNull()
    }

    private fun unavailable(
        backendName: String,
        technicalMessage: String,
        selection: DesktopPlayerBackendSelection,
    ): DesktopPlayerBackend {
        DesktopRuntimeLog.warn("Selected player backend=$backendName (source=${selection.source} request=${selection.value})")
        return UnavailableDesktopPlayerBackend(
            backendName = backendName,
            error = DesktopPlayerError.RuntimeUnavailable(
                backendName = backendName,
                technicalMessage = technicalMessage,
                suggestedAction = "Check the MPV runtime files and restart the app.",
            ),
        )
    }

    private enum class DesktopPlayerBackendKind {
        Auto,
        Mpv,
        Native,
        None,
    }

    private data class DesktopPlayerBackendSelection(
        val backend: DesktopPlayerBackendKind,
        val value: String,
        val source: String,
    ) {
        companion object {
            fun resolve(): DesktopPlayerBackendSelection {
                val property = System.getProperty(BACKEND_PROPERTY)?.trim()?.lowercase()
                if (!property.isNullOrBlank()) return fromValue(property, "system-property:$BACKEND_PROPERTY")
                val env = System.getenv(BACKEND_ENV)?.trim()?.lowercase()
                if (!env.isNullOrBlank()) return fromValue(env, "env:$BACKEND_ENV")
                return DesktopPlayerBackendSelection(DesktopPlayerBackendKind.Auto, "auto", "default")
            }

            private fun fromValue(value: String, source: String): DesktopPlayerBackendSelection =
                DesktopPlayerBackendSelection(
                    backend = when (value) {
                        "mpv" -> DesktopPlayerBackendKind.Mpv
                        "native" -> DesktopPlayerBackendKind.Native
                        "none" -> DesktopPlayerBackendKind.None
                        else -> DesktopPlayerBackendKind.Auto
                    },
                    value = value,
                    source = source,
                )
        }
    }
}
