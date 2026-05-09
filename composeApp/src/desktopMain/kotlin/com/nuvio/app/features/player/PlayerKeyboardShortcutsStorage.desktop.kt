package com.nuvio.app.features.player

import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.desktop.DesktopPreferences

internal actual object PlayerKeyboardShortcutsStorage {
    private const val preferencesName = "nuvio_player_keyboard_shortcuts"
    private const val playPauseKey = "play_pause"
    private const val seekForwardKey = "seek_forward"
    private const val seekBackwardKey = "seek_backward"
    private const val toggleFullscreenKey = "toggle_fullscreen"

    actual fun loadPlayPauseKey(): String? = DesktopPreferences.getString(preferencesName, ProfileScopedKey.of(playPauseKey))
    actual fun savePlayPauseKey(key: String) = DesktopPreferences.putString(preferencesName, ProfileScopedKey.of(playPauseKey), key)
    actual fun loadSeekForwardKey(): String? = DesktopPreferences.getString(preferencesName, ProfileScopedKey.of(seekForwardKey))
    actual fun saveSeekForwardKey(key: String) = DesktopPreferences.putString(preferencesName, ProfileScopedKey.of(seekForwardKey), key)
    actual fun loadSeekBackwardKey(): String? = DesktopPreferences.getString(preferencesName, ProfileScopedKey.of(seekBackwardKey))
    actual fun saveSeekBackwardKey(key: String) = DesktopPreferences.putString(preferencesName, ProfileScopedKey.of(seekBackwardKey), key)
    actual fun loadToggleFullscreenKey(): String? = DesktopPreferences.getString(preferencesName, ProfileScopedKey.of(toggleFullscreenKey))
    actual fun saveToggleFullscreenKey(key: String) = DesktopPreferences.putString(preferencesName, ProfileScopedKey.of(toggleFullscreenKey), key)
}
