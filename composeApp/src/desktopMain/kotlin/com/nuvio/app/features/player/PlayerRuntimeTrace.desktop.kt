package com.nuvio.app.features.player

import com.nuvio.app.desktop.DesktopRuntimeLog

internal actual object PlayerRuntimeTrace {
    actual fun info(message: String) {
        DesktopRuntimeLog.info("PlayerScreen $message")
    }

    actual fun warn(message: String) {
        DesktopRuntimeLog.warn("PlayerScreen $message")
    }
}
