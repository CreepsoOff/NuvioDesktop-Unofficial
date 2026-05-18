package com.nuvio.app.features.player

import android.util.Log

internal actual object PlayerRuntimeTrace {
    private const val Tag = "NuvioPlayerScreen"

    actual fun info(message: String) {
        Log.i(Tag, message)
    }

    actual fun warn(message: String) {
        Log.w(Tag, message)
    }
}
