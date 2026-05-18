package com.nuvio.app.features.player

internal actual object PlayerRuntimeTrace {
    actual fun info(message: String) {
        println("PlayerScreen $message")
    }

    actual fun warn(message: String) {
        println("PlayerScreen WARN $message")
    }
}
