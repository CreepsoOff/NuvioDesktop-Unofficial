package com.nuvio.app.features.player

internal expect object PlayerRuntimeTrace {
    fun info(message: String)
    fun warn(message: String)
}
