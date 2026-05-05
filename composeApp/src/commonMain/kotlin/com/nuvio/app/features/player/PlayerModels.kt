package com.nuvio.app.features.player

import kotlin.math.abs
import kotlinx.serialization.Serializable

@Serializable
data class PlayerRoute(
    val launchId: Long,
)

data class PlayerLaunch(
    val title: String,
    val sourceUrl: String,
    val sourceAudioUrl: String? = null,
    val sourceHeaders: Map<String, String> = emptyMap(),
    val sourceResponseHeaders: Map<String, String> = emptyMap(),
    val logo: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val episodeThumbnail: String? = null,
    val streamTitle: String,
    val streamSubtitle: String? = null,
    val bingeGroup: String? = null,
    val pauseDescription: String? = null,
    val providerName: String,
    val providerAddonId: String? = null,
    val contentType: String? = null,
    val videoId: String? = null,
    val parentMetaId: String,
    val parentMetaType: String,
    val initialPositionMs: Long = 0L,
    val initialProgressFraction: Float? = null,
)

object PlayerLaunchStore {
    private var nextLaunchId = 1L
    private val launches = mutableMapOf<Long, PlayerLaunch>()

    fun put(launch: PlayerLaunch): Long {
        val launchId = nextLaunchId++
        launches[launchId] = launch
        return launchId
    }

    fun get(launchId: Long): PlayerLaunch? = launches[launchId]

    fun remove(launchId: Long) {
        launches.remove(launchId)
    }

    fun clear() {
        nextLaunchId = 1L
        launches.clear()
    }
}

enum class PlayerResizeMode {
    Fit,
    Fill,
    Zoom,
}

data class PlayerPlaybackSnapshot(
    val isLoading: Boolean = true,
    val isPlaying: Boolean = false,
    val isEnded: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1f,
)

internal const val InitialResumeSeekPositionToleranceMs = 2_000L

internal const val InitialResumeSeekRetryThrottleMs = 750L

internal const val InitialResumeSeekMaxAttempts = 45

/**
 * One step of initial resume handling for [PlayerScreen].
 *
 * Absolute resume may issue [Seek] before duration is known; completion is confirmed via position
 * (and retries are throttled). Fraction-based resume defers until [durationMs] is known.
 */
internal sealed class InitialResumeSeekDecision {
    data object AlreadyComplete : InitialResumeSeekDecision()

    /** Fraction resume: wait for timeline metadata. */
    data object DeferTimeline : InitialResumeSeekDecision()

    data object WaitLoading : InitialResumeSeekDecision()

    /** No seek needed, or position confirmed close enough to target (or max attempts). */
    data object ConfirmComplete : InitialResumeSeekDecision()

    data class Seek(val positionMs: Long) : InitialResumeSeekDecision()

    /** Wait before another seek (throttle). */
    data class Sleep(val ms: Long) : InitialResumeSeekDecision()
}

internal fun resolveInitialResumeTargetMs(
    activeInitialPositionMs: Long,
    activeInitialProgressFraction: Float?,
    durationMs: Long,
): Long? {
    val progressFraction = activeInitialProgressFraction
        ?.takeIf { it > 0f }
        ?.coerceIn(0f, 1f)

    return when {
        activeInitialPositionMs > 0L -> {
            if (durationMs > 0L) {
                activeInitialPositionMs.coerceIn(0L, durationMs)
            } else {
                activeInitialPositionMs
            }
        }

        progressFraction != null -> {
            if (durationMs <= 0L) return null
            (durationMs.toDouble() * progressFraction.toDouble()).toLong()
        }

        else -> 0L
    }
}

internal fun evaluateInitialResumeSeekDecision(
    initialSeekApplied: Boolean,
    isLoading: Boolean,
    activeInitialPositionMs: Long,
    activeInitialProgressFraction: Float?,
    durationMs: Long,
    positionMs: Long,
    seekAttempts: Int,
    lastSeekAttemptTimeMs: Long?,
    nowTimeMs: Long,
): InitialResumeSeekDecision {
    if (initialSeekApplied) return InitialResumeSeekDecision.AlreadyComplete

    val progressFraction = activeInitialProgressFraction
        ?.takeIf { it > 0f }
        ?.coerceIn(0f, 1f)

    if (activeInitialPositionMs <= 0L && progressFraction != null && durationMs <= 0L) {
        return InitialResumeSeekDecision.DeferTimeline
    }

    if (isLoading) return InitialResumeSeekDecision.WaitLoading

    val targetMs = resolveInitialResumeTargetMs(
        activeInitialPositionMs,
        activeInitialProgressFraction,
        durationMs,
    ) ?: return InitialResumeSeekDecision.DeferTimeline

    if (targetMs <= 0L) return InitialResumeSeekDecision.ConfirmComplete

    if (seekAttempts >= InitialResumeSeekMaxAttempts) {
        return InitialResumeSeekDecision.ConfirmComplete
    }

    val closeEnough =
        abs(positionMs - targetMs) <= InitialResumeSeekPositionToleranceMs
    if (closeEnough) return InitialResumeSeekDecision.ConfirmComplete

    if (seekAttempts > 0 && lastSeekAttemptTimeMs != null) {
        val sinceSeek = nowTimeMs - lastSeekAttemptTimeMs
        if (sinceSeek < InitialResumeSeekRetryThrottleMs) {
            return InitialResumeSeekDecision.Sleep(InitialResumeSeekRetryThrottleMs - sinceSeek)
        }
    }

    return InitialResumeSeekDecision.Seek(targetMs)
}

/** Snapshot inputs for initial resume loop (fed by [androidx.compose.runtime.snapshotFlow] in [PlayerScreen]). */
internal data class InitialResumeSeekInputs(
    val initialSeekApplied: Boolean,
    val isLoading: Boolean,
    val durationMs: Long,
    val positionMs: Long,
    val controller: PlayerEngineController?,
    val controllerSourceUrl: String?,
)

internal data class InitialResumeSeekMutableState(
    var seekAttempts: Int = 0,
    var lastSeekAttemptTimeMs: Long? = null,
)

/**
 * Single polling step for the initial resume loop; updates [mutableState] when a seek is issued.
 *
 * [readInputs] is invoked each iteration so timeline/controller updates from Compose snapshots are visible.
 */
internal fun pollInitialResumeSeekStep(
    activeSourceUrl: String,
    activeInitialPositionMs: Long,
    activeInitialProgressFraction: Float?,
    readInputs: () -> InitialResumeSeekInputs,
    mutableState: InitialResumeSeekMutableState,
    nowTimeMs: Long,
): InitialResumeSeekDecision {
    val inputs = readInputs()
    val controller = inputs.controller
    if (controller == null || inputs.controllerSourceUrl != activeSourceUrl) {
        return InitialResumeSeekDecision.WaitLoading
    }

    val decision = evaluateInitialResumeSeekDecision(
        initialSeekApplied = inputs.initialSeekApplied,
        isLoading = inputs.isLoading,
        activeInitialPositionMs = activeInitialPositionMs,
        activeInitialProgressFraction = activeInitialProgressFraction,
        durationMs = inputs.durationMs,
        positionMs = inputs.positionMs,
        seekAttempts = mutableState.seekAttempts,
        lastSeekAttemptTimeMs = mutableState.lastSeekAttemptTimeMs,
        nowTimeMs = nowTimeMs,
    )

    return when (decision) {
        is InitialResumeSeekDecision.Seek -> {
            controller.seekTo(decision.positionMs)
            mutableState.seekAttempts += 1
            mutableState.lastSeekAttemptTimeMs = nowTimeMs
            decision
        }

        else -> decision
    }
}
