package com.nuvio.app.features.player

internal expect object PlayerKeyboardShortcutsStorage {
    fun loadPlayPauseKey(): String?
    fun savePlayPauseKey(key: String)
    fun loadSeekForwardKey(): String?
    fun saveSeekForwardKey(key: String)
    fun loadSeekBackwardKey(): String?
    fun saveSeekBackwardKey(key: String)
    fun loadToggleFullscreenKey(): String?
    fun saveToggleFullscreenKey(key: String)
}
