package com.nuvio.app.features.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize

interface PlayerGestureController {
    fun currentBrightness(): Float?
    fun setBrightness(level: Float): Float?
    fun currentVolume(): PlayerAudioLevel?
    fun setVolume(level: Float): PlayerAudioLevel?
}

interface PlayerFullscreenController {
    val isFullscreenSupported: Boolean
    val isFullscreen: Boolean
    fun toggleFullscreen()
}

data class PlayerAudioLevel(
    val fraction: Float,
    val isMuted: Boolean,
)

@Composable
expect fun LockPlayerToLandscape()

@Composable
expect fun EnterImmersivePlayerMode()

@Composable
expect fun ManagePlayerPictureInPicture(
    isPlaying: Boolean,
    playerSize: IntSize,
)

@Composable
expect fun ManagePlayerCursorVisibility(visible: Boolean)

@Composable
expect fun rememberPlayerGestureController(): PlayerGestureController?

@Composable
expect fun rememberPlayerFullscreenController(): PlayerFullscreenController

expect val usesNativePlayerChrome: Boolean

expect val usesAnimatedPlayerChrome: Boolean
