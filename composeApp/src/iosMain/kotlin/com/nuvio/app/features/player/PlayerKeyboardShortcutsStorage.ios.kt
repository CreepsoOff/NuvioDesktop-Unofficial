package com.nuvio.app.features.player

internal actual object PlayerKeyboardShortcutsStorage {
    actual fun loadPlayPauseKey(): String? = null
    actual fun savePlayPauseKey(key: String) = Unit
    actual fun loadSeekForwardKey(): String? = null
    actual fun saveSeekForwardKey(key: String) = Unit
    actual fun loadSeekBackwardKey(): String? = null
    actual fun saveSeekBackwardKey(key: String) = Unit
    actual fun loadToggleFullscreenKey(): String? = null
    actual fun saveToggleFullscreenKey(key: String) = Unit
}
