package com.nuvio.app.features.player

import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerKeyboardShortcutsUiState(
    val playPauseKey: String = DEFAULT_PLAY_PAUSE_KEY,
    val seekForwardKey: String = DEFAULT_SEEK_FORWARD_KEY,
    val seekBackwardKey: String = DEFAULT_SEEK_BACKWARD_KEY,
    val toggleFullscreenKey: String = DEFAULT_FULLSCREEN_KEY,
)

object PlayerKeyboardShortcutsRepository {
    private val _uiState = MutableStateFlow(PlayerKeyboardShortcutsUiState())
    val uiState: StateFlow<PlayerKeyboardShortcutsUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var playPauseKey = DEFAULT_PLAY_PAUSE_KEY
    private var seekForwardKey = DEFAULT_SEEK_FORWARD_KEY
    private var seekBackwardKey = DEFAULT_SEEK_BACKWARD_KEY
    private var toggleFullscreenKey = DEFAULT_FULLSCREEN_KEY

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        playPauseKey = PlayerKeyboardShortcutsStorage.loadPlayPauseKey() ?: DEFAULT_PLAY_PAUSE_KEY
        seekForwardKey = PlayerKeyboardShortcutsStorage.loadSeekForwardKey() ?: DEFAULT_SEEK_FORWARD_KEY
        seekBackwardKey = PlayerKeyboardShortcutsStorage.loadSeekBackwardKey() ?: DEFAULT_SEEK_BACKWARD_KEY
        toggleFullscreenKey = PlayerKeyboardShortcutsStorage.loadToggleFullscreenKey() ?: DEFAULT_FULLSCREEN_KEY
        publish()
    }

    fun setPlayPauseKey(key: String) {
        ensureLoaded()
        if (playPauseKey == key) return
        playPauseKey = key
        PlayerKeyboardShortcutsStorage.savePlayPauseKey(key)
        publish()
    }

    fun setSeekForwardKey(key: String) {
        ensureLoaded()
        if (seekForwardKey == key) return
        seekForwardKey = key
        PlayerKeyboardShortcutsStorage.saveSeekForwardKey(key)
        publish()
    }

    fun setSeekBackwardKey(key: String) {
        ensureLoaded()
        if (seekBackwardKey == key) return
        seekBackwardKey = key
        PlayerKeyboardShortcutsStorage.saveSeekBackwardKey(key)
        publish()
    }

    fun setToggleFullscreenKey(key: String) {
        ensureLoaded()
        if (toggleFullscreenKey == key) return
        toggleFullscreenKey = key
        PlayerKeyboardShortcutsStorage.saveToggleFullscreenKey(key)
        publish()
    }

    fun keyMatches(expected: String, key: Key): Boolean = keyMatchesNormalized(normalizeKeyName(expected), key)

    fun keyMatchesNormalized(normalizedExpected: String, key: Key): Boolean =
        normalizedExpected == normalizeKeyName(keyNameForComposeKey(key))

    private fun publish() {
        _uiState.value = PlayerKeyboardShortcutsUiState(
            playPauseKey = playPauseKey,
            seekForwardKey = seekForwardKey,
            seekBackwardKey = seekBackwardKey,
            toggleFullscreenKey = toggleFullscreenKey,
        )
    }
}

internal const val DEFAULT_PLAY_PAUSE_KEY = "Space"
internal const val DEFAULT_SEEK_FORWARD_KEY = "ArrowRight"
internal const val DEFAULT_SEEK_BACKWARD_KEY = "ArrowLeft"
internal const val DEFAULT_FULLSCREEN_KEY = "F"

internal val DesktopShortcutKeyOptions = listOf(
    "Space",
    "ArrowRight",
    "ArrowLeft",
    "F",
    "J",
    "K",
    "L",
)

internal fun nextShortcutOption(current: String): String {
    val index = DesktopShortcutKeyOptions.indexOf(current)
    return if (index == -1 || index + 1 >= DesktopShortcutKeyOptions.size) {
        DesktopShortcutKeyOptions.first()
    } else {
        DesktopShortcutKeyOptions[index + 1]
    }
}

internal fun keyNameForComposeKey(key: Key): String = when (key) {
    Key.Spacebar -> "Space"
    Key.DirectionRight -> "ArrowRight"
    Key.DirectionLeft -> "ArrowLeft"
    Key.F -> "F"
    Key.J -> "J"
    Key.K -> "K"
    Key.L -> "L"
    // Fallback assumes Compose keeps the current Key.toString() format.
    else -> key.toString().removePrefix("Key(").removeSuffix(")")
}

internal fun normalizeKeyName(key: String): String = key.trim().lowercase()
