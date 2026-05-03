package com.nuvio.app.features.player.desktop.mpv

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nuvio.app.desktop.DesktopPlayerRegistry
import com.nuvio.app.desktop.DesktopRuntimeLog
import com.nuvio.app.features.player.AudioTrack
import com.nuvio.app.features.player.PlayerEngineController
import com.nuvio.app.features.player.PlayerResizeMode
import com.nuvio.app.features.player.SubtitleStyleState
import com.nuvio.app.features.player.SubtitleTrack
import com.nuvio.app.features.player.desktop.DesktopPlayerBackend
import com.nuvio.app.features.player.desktop.DesktopPlayerError
import com.nuvio.app.features.player.desktop.DesktopPlayerPhase
import com.nuvio.app.features.player.desktop.DesktopPlayerRequest
import com.nuvio.app.features.player.desktop.DesktopPlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.openani.mediamp.InternalMediampApi
import org.openani.mediamp.PlaybackState
import org.openani.mediamp.features.PlaybackSpeed
import org.openani.mediamp.mpv.MpvMediampPlayer
import org.openani.mediamp.source.UriMediaData
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(InternalMediampApi::class)
internal class MpvDesktopPlayerBackend private constructor(
    private val runtime: MpvRuntimeResolution,
    private val player: MpvMediampPlayer,
) : DesktopPlayerBackend {
    override val id: String = "windows-mpv-${System.identityHashCode(player)}"
    override val backendName: String = "windows-mediamp-mpv"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val stateFlow = MutableStateFlow(
        DesktopPlayerState(
            phase = DesktopPlayerPhase.Idle,
            backendName = backendName,
            diagnostics = runtime.diagnostics,
        ),
    )

    @Volatile private var stopped = false
    @Volatile private var nativeClosed = false
    @Volatile private var currentRequest: DesktopPlayerRequest? = null

    override val state: StateFlow<DesktopPlayerState> = stateFlow
    override val controller: PlayerEngineController = MpvController()

    init {
        observePlayerState()
        DesktopRuntimeLog.info("MPV backend created id=$id runtime=${runtime.directory?.safePath() ?: "none"}")
    }

    override suspend fun load(request: DesktopPlayerRequest) {
        if (nativeClosed) return
        if (request.sourceUrl.isBlank()) {
            fail(DesktopPlayerError.InvalidSource(backendName, "Blank source URL"))
            return
        }
        currentRequest = request
        stopped = false
        stateFlow.value = stateFlow.value.copy(phase = DesktopPlayerPhase.Preparing, error = null)
        runCatching {
            val headers = request.sourceHeaders.toMutableMap()
            DesktopRuntimeLog.info(
                "MPV load start session=${request.sessionKey} source=${request.sourceUrl.redactedMediaUrl()} " +
                    "audio=${request.sourceAudioUrl?.redactedMediaUrl() ?: "none"} headersPresent=${headers.isNotEmpty()}",
            )
            player.setMediaData(UriMediaData(request.sourceUrl, headers))
            request.sourceAudioUrl?.takeIf { it.isNotBlank() }?.let { audioUrl ->
                runCatching { player.impl.command("audio-add", audioUrl, "auto") }
                    .onFailure { DesktopRuntimeLog.error("MPV audio-add failed audio=${audioUrl.redactedMediaUrl()}", it) }
            }
            setResizeMode(request.resizeMode)
            if (request.playWhenReady) {
                player.resume()
                runCatching { player.impl.setPropertyBoolean("pause", false) }
                    .onFailure { DesktopRuntimeLog.error("MPV unpause after load failed", it) }
            } else {
                player.pause()
            }
            DesktopRuntimeLog.info("MPV load success session=${request.sessionKey}")
        }.onFailure { throwable ->
            DesktopRuntimeLog.error("MPV load failed source=${request.sourceUrl.redactedMediaUrl()}", throwable)
            fail(DesktopPlayerError.MediaLoadFailed(backendName, "MPV media load failed", throwable))
        }
    }

    override fun setResizeMode(resizeMode: PlayerResizeMode) {
        if (!canReceiveCommands()) return
        runCatching { player.impl.applyResizeMode(resizeMode) }
            .onSuccess { DesktopRuntimeLog.info("MPV resizeMode=$resizeMode applied") }
            .onFailure { DesktopRuntimeLog.error("MPV resizeMode=$resizeMode failed", it) }
    }

    override fun releaseSoft() {
        if (stopped) return
        stopped = true
        DesktopRuntimeLog.info("MPV releaseSoft id=$id")
        runCatching { player.impl.setPropertyBoolean("mute", true) }
        runCatching { player.impl.command("stop") }
            .onFailure { DesktopRuntimeLog.error("MPV stop failed id=$id", it) }
        stateFlow.value = stateFlow.value.copy(phase = DesktopPlayerPhase.Closed)
    }

    override fun close() {
        if (nativeClosed) return
        nativeClosed = true
        scope.cancel()
        DesktopRuntimeLog.info("MPV close async id=$id")
        val thread = Thread({
            val startMs = System.currentTimeMillis()
            runCatching { player.close() }
                .onSuccess {
                    DesktopRuntimeLog.info("MPV native close done id=$id elapsedMs=${System.currentTimeMillis() - startMs}")
                }
                .onFailure { DesktopRuntimeLog.error("MPV native close failed id=$id", it) }
        }, "mpv-close-$id").apply { isDaemon = true }
        DesktopPlayerRegistry.trackCloseThread(thread)
        thread.start()
    }

    @Composable
    override fun Surface(modifier: Modifier) {
        MpvDesktopPlayerSurface(player = player, modifier = modifier)
    }

    private fun observePlayerState() {
        combine(
            player.playbackState,
            player.currentPositionMillis,
            player.mediaProperties,
        ) { playbackState, position, props ->
            val phase = playbackState.toDesktopPhase()
            DesktopPlayerState(
                phase = phase,
                positionMs = position,
                durationMs = props?.durationMillis?.takeIf { it > 0 } ?: 0L,
                bufferedPositionMs = 0L,
                playbackSpeed = player.features[PlaybackSpeed]?.value ?: 1.0f,
                backendName = backendName,
                diagnostics = runtime.diagnostics,
                error = if (playbackState == PlaybackState.ERROR) {
                    DesktopPlayerError.PlaybackFailed(backendName, "MPV playback state is ERROR")
                } else {
                    null
                },
            )
        }.onEach { mapped ->
            if (!nativeClosed) {
                stateFlow.value = mapped
            }
        }.launchIn(scope)
    }

    private fun fail(error: DesktopPlayerError) {
        stateFlow.value = stateFlow.value.copy(
            phase = DesktopPlayerPhase.Error,
            error = error,
            diagnostics = error.technicalMessage,
        )
    }

    private fun canReceiveCommands(): Boolean =
        !stopped && !nativeClosed && player.getCurrentPlaybackState() != PlaybackState.FINISHED

    private fun durationMs(): Long? =
        player.mediaProperties.value?.durationMillis?.takeIf { it > 0L }

    private fun snapshotForLog(): String =
        "state=${player.getCurrentPlaybackState()} posMs=${player.currentPositionMillis.value} durationMs=${durationMs() ?: -1}"

    private inner class MpvController : PlayerEngineController {
        override fun release() = releaseSoft()

        override fun play() {
            if (!canReceiveCommands()) return
            val before = snapshotForLog()
            val result = runCatching {
                player.resume()
                player.impl.setPropertyBoolean("pause", false)
            }
            DesktopRuntimeLog.info("MPV controller play before=$before result=${result.getOrNull()} after=${snapshotForLog()}")
            result.onFailure { DesktopRuntimeLog.error("MPV controller play failed", it) }
        }

        override fun pause() {
            if (!canReceiveCommands()) return
            val before = snapshotForLog()
            val result = runCatching { player.pause() }
            DesktopRuntimeLog.info("MPV controller pause before=$before result=${result.getOrNull()} after=${snapshotForLog()}")
            result.onFailure { DesktopRuntimeLog.error("MPV controller pause failed", it) }
        }

        override fun seekTo(positionMs: Long) {
            if (!canReceiveCommands()) return
            val durationMs = durationMs()
            val targetMs = positionMs.coerceAtLeast(0L).let { target -> durationMs?.let(target::coerceAtMost) ?: target }
            val before = snapshotForLog()
            val result = runCatching { player.impl.command("seek", (targetMs / 1000.0).toString(), "absolute+exact") }
            if (result.getOrNull() == true) player.currentPositionMillis.value = targetMs
            DesktopRuntimeLog.info(
                "MPV controller seekTo targetMs=$targetMs durationMs=${durationMs ?: -1} " +
                    "before=$before result=${result.getOrNull()} after=${snapshotForLog()}",
            )
            result.onFailure { DesktopRuntimeLog.error("MPV controller seekTo failed targetMs=$targetMs", it) }
        }

        override fun seekBy(offsetMs: Long) {
            if (!canReceiveCommands()) return
            seekTo(player.currentPositionMillis.value.coerceAtLeast(0L) + offsetMs)
        }

        override fun retry() = play()

        override fun setPlaybackSpeed(speed: Float) {
            if (!canReceiveCommands()) return
            player.features[PlaybackSpeed]?.set(speed.coerceIn(0.25f, 4.0f))
        }

        override fun getAudioTracks(): List<AudioTrack> =
            if (canReceiveCommands()) runCatching { player.impl.audioTracks() }.getOrDefault(emptyList()) else emptyList()

        override fun getSubtitleTracks(): List<SubtitleTrack> =
            if (canReceiveCommands()) runCatching { player.impl.subtitleTracks() }.getOrDefault(emptyList()) else emptyList()

        override fun selectAudioTrack(index: Int) {
            if (!canReceiveCommands()) return
            val tracks = getAudioTracks()
            if (index in tracks.indices) {
                runCatching { player.impl.setMpvProperty("aid", tracks[index].id) }
                    .onFailure { DesktopRuntimeLog.error("MPV selectAudioTrack failed index=$index", it) }
            }
        }

        override fun selectSubtitleTrack(index: Int) {
            if (!canReceiveCommands()) return
            if (index < 0) {
                runCatching { player.impl.setMpvProperty("sid", "no") }
                return
            }
            val tracks = getSubtitleTracks()
            if (index in tracks.indices) {
                runCatching { player.impl.setMpvProperty("sid", tracks[index].id) }
                    .onFailure { DesktopRuntimeLog.error("MPV selectSubtitleTrack failed index=$index", it) }
            }
        }

        override fun setSubtitleUri(url: String) {
            if (!canReceiveCommands()) return
            runCatching { player.impl.command("sub-add", url, "auto") }
                .onFailure { DesktopRuntimeLog.error("MPV setSubtitleUri failed url=${url.redactedMediaUrl()}", it) }
        }

        override fun clearExternalSubtitle() {
            if (!canReceiveCommands()) return
            val handle = player.impl
            val count = handle.getMpvIntProperty("track-list/count") ?: return
            for (i in count - 1 downTo 0) {
                val type = handle.getMpvStringProperty("track-list/$i/type")
                val external = handle.getMpvBooleanProperty("track-list/$i/external")
                if (type == "sub" && external) {
                    val id = handle.getMpvIntProperty("track-list/$i/id") ?: return
                    runCatching { handle.command("sub-remove", id.toString()) }
                    return
                }
            }
        }

        override fun clearExternalSubtitleAndSelect(trackIndex: Int) {
            clearExternalSubtitle()
            selectSubtitleTrack(trackIndex)
        }

        override fun applySubtitleStyle(style: SubtitleStyleState) {
            if (!canReceiveCommands()) return
            val handle = player.impl
            val colorHex = style.textColor.toMpvColorString()
            val outline = if (style.outlineEnabled) 2.0 else 0.0
            val subPos = 100 - style.bottomOffset
            runCatching {
                handle.option("sub-color", colorHex)
                handle.setMpvProperty("sub-border-size", outline)
                handle.setMpvProperty("sub-font-size", style.fontSizeSp.toDouble())
                handle.setMpvProperty("sub-pos", subPos)
            }.onFailure { DesktopRuntimeLog.error("MPV applySubtitleStyle failed", it) }
        }

        override fun switchSource(url: String, audioUrl: String?, headersJson: String?) {
            if (!canReceiveCommands()) return
            val previous = currentRequest ?: return
            val headers = parseHeadersJson(headersJson).ifEmpty { previous.sourceHeaders }
            DesktopRuntimeLog.info(
                "MPV switchSource reloadInPlace url=${url.redactedMediaUrl()} " +
                    "audio=${audioUrl?.redactedMediaUrl() ?: "none"} headersPresent=${headers.isNotEmpty()}",
            )
            scope.launch {
                load(
                    previous.copy(
                        sourceUrl = url,
                        sourceAudioUrl = audioUrl,
                        sourceHeaders = headers,
                        playWhenReady = true,
                    ),
                )
            }
        }
    }

    companion object {
        fun create(runtime: MpvRuntimeResolution): Result<MpvDesktopPlayerBackend> =
            runCatching {
                MpvDesktopPlayerBackend(
                    runtime = runtime,
                    player = MpvMediampPlayer(Unit, EmptyCoroutineContext),
                )
            }
    }
}

private fun parseHeadersJson(headersJson: String?): Map<String, String> {
    if (headersJson.isNullOrBlank()) return emptyMap()
    return runCatching {
        Json.parseToJsonElement(headersJson).jsonObject.mapNotNull { (key, value) ->
            val primitive = value as? JsonPrimitive ?: return@mapNotNull null
            val content = primitive.jsonPrimitive.content.trim()
            if (key.isBlank() || content.isBlank()) null else key.trim() to content
        }.toMap()
    }.getOrDefault(emptyMap())
}

private fun Color.toMpvColorString(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    val a = (alpha * 255).toInt().coerceIn(0, 255)
    return "#${r.hex()}${g.hex()}${b.hex()}${a.hex()}"
}

private fun Int.hex(): String = toString(16).padStart(2, '0').uppercase()
