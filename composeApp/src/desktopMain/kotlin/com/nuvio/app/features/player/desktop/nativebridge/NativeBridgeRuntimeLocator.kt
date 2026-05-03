package com.nuvio.app.features.player.desktop.nativebridge

import com.nuvio.app.features.player.WindowsDesktopMPVBridgeLib

internal data class NativeBridgeRuntimeStatus(
    val available: Boolean,
    val diagnostics: String,
)

internal object NativeBridgeRuntimeLocator {
    fun resolve(): NativeBridgeRuntimeStatus {
        if (!devLookupEnabled()) {
            return NativeBridgeRuntimeStatus(
                available = false,
                diagnostics = "Native bridge disabled; set NUVIO_DEV_PLAYER_LOOKUP=true and request backend=native to enable dev lookup.",
            )
        }
        val bridge = WindowsDesktopMPVBridgeLib.loadOrNull()
        return NativeBridgeRuntimeStatus(
            available = bridge != null,
            diagnostics = if (bridge != null) "Native bridge loaded via explicit dev lookup" else "Native bridge DLL not found",
        )
    }

    internal fun loadBridgeOrNull(): WindowsDesktopMPVBridgeLib? {
        if (!devLookupEnabled()) return null
        return WindowsDesktopMPVBridgeLib.loadOrNull()
    }

    private fun devLookupEnabled(): Boolean =
        System.getenv("NUVIO_DEV_PLAYER_LOOKUP").equals("true", ignoreCase = true) ||
            System.getProperty("nuvio.dev.player.lookup").equals("true", ignoreCase = true)
}
