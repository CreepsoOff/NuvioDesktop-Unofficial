package com.nuvio.app.features.player

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

/**
 * Resume seek for [PlayerScreen]: fraction-based resume waits for a known duration; absolute
 * resume seeks even when duration stays unknown (live streams).
 */
internal sealed class InitialResumeSeekAction {
    data object NoSeekNeeded : InitialResumeSeekAction()

    /** Fraction resume needs duration; keep waiting for timeline metadata. */
    data object DeferUntilTimelineKnown : InitialResumeSeekAction()

    data class Seek(val positionMs: Long) : InitialResumeSeekAction()
}

internal fun resolveInitialResumeSeekAction(
    activeInitialPositionMs: Long,
    activeInitialProgressFraction: Float?,
    durationMs: Long,
): InitialResumeSeekAction {
    val progressFraction = activeInitialProgressFraction
        ?.takeIf { it > 0f }
        ?.coerceIn(0f, 1f)

    return when {
        activeInitialPositionMs > 0L -> {
            val pos =
                if (durationMs > 0L) {
                    activeInitialPositionMs.coerceIn(0L, durationMs)
                } else {
                    activeInitialPositionMs
                }
            InitialResumeSeekAction.Seek(pos)
        }

        progressFraction != null -> {
            if (durationMs <= 0L) return InitialResumeSeekAction.DeferUntilTimelineKnown
            val pos = (durationMs.toDouble() * progressFraction.toDouble()).toLong()
            if (pos <= 0L) InitialResumeSeekAction.NoSeekNeeded else InitialResumeSeekAction.Seek(pos)
        }

        else -> InitialResumeSeekAction.NoSeekNeeded
    }
}
