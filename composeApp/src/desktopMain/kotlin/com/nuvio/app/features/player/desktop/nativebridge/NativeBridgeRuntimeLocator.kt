package com.nuvio.app.features.player.desktop.nativebridge

import com.nuvio.app.features.player.WindowsDesktopMPVBridgeLib

internal data class NativeBridgeRuntimeStatus(
    val available: Boolean,
    val diagnostics: String,
)

internal object NativeBridgeRuntimeLocator {
    fun resolve(): NativeBridgeRuntimeStatus {
        val bridge = WindowsDesktopMPVBridgeLib.loadOrNull()
        return NativeBridgeRuntimeStatus(
            available = bridge != null,
            diagnostics = WindowsDesktopMPVBridgeLib.loadDiagnostics,
        )
    }

    internal fun loadBridgeOrNull(): WindowsDesktopMPVBridgeLib? {
        return WindowsDesktopMPVBridgeLib.loadOrNull()
    }
}
