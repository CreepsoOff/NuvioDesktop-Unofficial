package com.nuvio.app.features.player

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.io.File

private val isWindowsDesktop: Boolean by lazy {
    System.getProperty("os.name")?.lowercase()?.contains("windows") == true
}

internal interface DesktopMPVBridgeLib : Library {
    companion object {
        val INSTANCE: DesktopMPVBridgeLib by lazy {
            val libPath = resolveLibraryPath()
            if (libPath != null) {
                System.setProperty(
                    "jna.library.path",
                    (System.getProperty("jna.library.path") ?: "") + ":" + libPath,
                )
            }
            Native.load("DesktopMPVBridge", DesktopMPVBridgeLib::class.java)
        }

        private fun resolveLibraryPath(): String? {
            val candidates = listOf(
                "MPVKit/.build/arm64-apple-macosx/release",
                "MPVKit/.build/arm64-apple-macosx/debug",
                "../MPVKit/.build/arm64-apple-macosx/release",
                "../MPVKit/.build/arm64-apple-macosx/debug",
            )
            val userDir = System.getProperty("user.dir") ?: return null
            for (candidate in candidates) {
                val dir = java.io.File(userDir, candidate)
                if (dir.exists() && dir.isDirectory) {
                    val dylib = java.io.File(dir, "libDesktopMPVBridge.dylib")
                    if (dylib.exists()) return dir.absolutePath
                }
            }
            return null
        }
    }

    fun nuvio_player_create(): Pointer
    fun nuvio_player_destroy(player: Pointer)
    fun nuvio_player_show(player: Pointer)

    fun nuvio_player_set_metadata(
        player: Pointer,
        title: String,
        streamTitle: String,
        providerName: String,
        season: Int,
        episode: Int,
        episodeTitle: String?,
        artwork: String?,
        logo: String?,
    )

    fun nuvio_player_set_has_video_id(player: Pointer, value: Boolean)
    fun nuvio_player_set_is_series(player: Pointer, value: Boolean)

    fun nuvio_player_load_file(
        player: Pointer,
        url: String,
        audioUrl: String?,
        headersJson: String?,
    )

    fun nuvio_player_play(player: Pointer)
    fun nuvio_player_pause(player: Pointer)
    fun nuvio_player_seek_to(player: Pointer, positionMs: Long)
    fun nuvio_player_seek_by(player: Pointer, offsetMs: Long)
    fun nuvio_player_set_speed(player: Pointer, speed: Float)
    fun nuvio_player_set_resize_mode(player: Pointer, mode: Int)
    fun nuvio_player_retry(player: Pointer)

    fun nuvio_player_refresh_state(player: Pointer)
    fun nuvio_player_is_loading(player: Pointer): Boolean
    fun nuvio_player_is_playing(player: Pointer): Boolean
    fun nuvio_player_is_ended(player: Pointer): Boolean
    fun nuvio_player_get_position_ms(player: Pointer): Long
    fun nuvio_player_get_duration_ms(player: Pointer): Long
    fun nuvio_player_get_buffered_ms(player: Pointer): Long
    fun nuvio_player_get_speed(player: Pointer): Float
    fun nuvio_player_get_error(player: Pointer): String?

    fun nuvio_player_get_audio_track_count(player: Pointer): Int
    fun nuvio_player_get_audio_track_id(player: Pointer, index: Int): Int
    fun nuvio_player_get_audio_track_label(player: Pointer, index: Int): String?
    fun nuvio_player_get_audio_track_lang(player: Pointer, index: Int): String?
    fun nuvio_player_is_audio_track_selected(player: Pointer, index: Int): Boolean
    fun nuvio_player_select_audio_track(player: Pointer, trackId: Int)

    fun nuvio_player_get_subtitle_track_count(player: Pointer): Int
    fun nuvio_player_get_subtitle_track_id(player: Pointer, index: Int): Int
    fun nuvio_player_get_subtitle_track_label(player: Pointer, index: Int): String?
    fun nuvio_player_get_subtitle_track_lang(player: Pointer, index: Int): String?
    fun nuvio_player_is_subtitle_track_selected(player: Pointer, index: Int): Boolean
    fun nuvio_player_select_subtitle_track(player: Pointer, trackId: Int)

    fun nuvio_player_set_subtitle_url(player: Pointer, url: String)
    fun nuvio_player_clear_external_subtitle(player: Pointer)
    fun nuvio_player_clear_external_subtitle_and_select(player: Pointer, trackId: Int)
    fun nuvio_player_apply_subtitle_style(
        player: Pointer,
        textColor: String,
        outlineSize: Float,
        fontSize: Float,
        subPos: Int,
    )

    fun nuvio_player_show_skip_button(player: Pointer, type: String, endTimeMs: Long)
    fun nuvio_player_hide_skip_button(player: Pointer)

    fun nuvio_player_show_next_episode(
        player: Pointer,
        season: Int,
        episode: Int,
        title: String,
        thumbnail: String?,
        hasAired: Boolean,
    )
    fun nuvio_player_hide_next_episode(player: Pointer)

    fun nuvio_player_is_closed(player: Pointer): Boolean
    fun nuvio_player_pop_next_episode_pressed(player: Pointer): Boolean
    fun nuvio_player_is_addon_subtitles_fetch_requested(player: Pointer): Boolean
    fun nuvio_player_set_addon_subtitles_loading(player: Pointer, loading: Boolean)
    fun nuvio_player_clear_addon_subtitles(player: Pointer)
    fun nuvio_player_add_addon_subtitle(player: Pointer, id: String, url: String, language: String, display: String)
    fun nuvio_player_pop_subtitle_style_changed(player: Pointer): Boolean
    fun nuvio_player_get_subtitle_style_color_index(player: Pointer): Int
    fun nuvio_player_get_subtitle_style_font_size(player: Pointer): Int
    fun nuvio_player_get_subtitle_style_outline_enabled(player: Pointer): Boolean
    fun nuvio_player_get_subtitle_style_bottom_offset(player: Pointer): Int

    fun nuvio_player_pop_sources_open_requested(player: Pointer): Boolean
    fun nuvio_player_pop_episodes_open_requested(player: Pointer): Boolean
    fun nuvio_player_pop_source_stream_selected(player: Pointer): String?
    fun nuvio_player_pop_source_filter_changed(player: Pointer): Boolean
    fun nuvio_player_get_source_filter_value(player: Pointer): String?
    fun nuvio_player_pop_source_reload(player: Pointer): Boolean
    fun nuvio_player_pop_episode_selected(player: Pointer): String?
    fun nuvio_player_pop_episode_stream_selected(player: Pointer): String?
    fun nuvio_player_pop_episode_filter_changed(player: Pointer): Boolean
    fun nuvio_player_get_episode_filter_value(player: Pointer): String?
    fun nuvio_player_pop_episode_reload(player: Pointer): Boolean
    fun nuvio_player_pop_episode_back(player: Pointer): Boolean

    fun nuvio_player_set_sources_loading(player: Pointer, loading: Boolean)
    fun nuvio_player_clear_source_streams(player: Pointer)
    fun nuvio_player_add_source_stream(player: Pointer, id: String, label: String, subtitle: String?, addonName: String, addonId: String, url: String, isCurrent: Boolean)
    fun nuvio_player_clear_source_addon_groups(player: Pointer)
    fun nuvio_player_add_source_addon_group(player: Pointer, id: String, addonName: String, addonId: String, isLoading: Boolean, hasError: Boolean)
    fun nuvio_player_set_source_selected_filter(player: Pointer, addonId: String?)

    fun nuvio_player_clear_episodes(player: Pointer)
    fun nuvio_player_add_episode(player: Pointer, id: String, title: String, overview: String?, thumbnail: String?, season: Int, episode: Int)
    fun nuvio_player_set_episode_streams_loading(player: Pointer, loading: Boolean)
    fun nuvio_player_clear_episode_streams(player: Pointer)
    fun nuvio_player_add_episode_stream(player: Pointer, id: String, label: String, subtitle: String?, addonName: String, addonId: String, url: String, isCurrent: Boolean)
    fun nuvio_player_clear_episode_addon_groups(player: Pointer)
    fun nuvio_player_add_episode_addon_group(player: Pointer, id: String, addonName: String, addonId: String, isLoading: Boolean, hasError: Boolean)
    fun nuvio_player_set_episode_selected_filter(player: Pointer, addonId: String?)
    fun nuvio_player_show_episode_streams(player: Pointer, season: Int, episode: Int, title: String?)
}

internal interface WindowsDesktopMPVBridgeLib : Library {
    companion object {
        private val loadedInstance: WindowsDesktopMPVBridgeLib? by lazy {
            val userDir = System.getProperty("user.dir") ?: ""
            val candidates = listOf(
                File(userDir, "WindowsBridge/build/Release"),
                File(userDir, "WindowsBridge/build/Debug"),
                File(userDir, "../WindowsBridge/build/Release"),
                File(userDir, "../WindowsBridge/build/Debug"),
                File(userDir, "composeApp/build/bin/desktop/debugExecutable"),
                File(userDir, "composeApp/build/bin/desktop/releaseExecutable"),
            )
            val libraryFile = candidates
                .asSequence()
                .filter { it.exists() && it.isDirectory }
                .map { File(it, "NuvioWindowsBridge.dll") }
                .firstOrNull { it.exists() && it.isFile }

            if (libraryFile != null) {
                System.setProperty(
                    "jna.library.path",
                    listOfNotNull(System.getProperty("jna.library.path"), libraryFile.parentFile?.absolutePath).joinToString(";"),
                )
            }

            runCatching {
                if (libraryFile != null) {
                    Native.load(libraryFile.absolutePath, WindowsDesktopMPVBridgeLib::class.java)
                } else {
                    Native.load("NuvioWindowsBridge", WindowsDesktopMPVBridgeLib::class.java)
                }
            }.getOrNull()
        }

        val isAvailable: Boolean
            get() = loadedInstance != null

        fun loadOrNull(): WindowsDesktopMPVBridgeLib? = loadedInstance

        val INSTANCE: WindowsDesktopMPVBridgeLib
            get() = loadedInstance ?: error("NuvioWindowsBridge.dll is not available")
    }

    fun nuvio_player_create(): Pointer
    fun nuvio_player_destroy(player: Pointer)
    fun nuvio_player_show(player: Pointer, hwnd: Long)
    fun nuvio_player_set_bounds(player: Pointer, x: Int, y: Int, width: Int, height: Int)

    fun nuvio_player_set_metadata(player: Pointer, title: String, streamTitle: String, providerName: String, season: Int, episode: Int, episodeTitle: String?, artwork: String?, logo: String?)
    fun nuvio_player_set_has_video_id(player: Pointer, value: Boolean)
    fun nuvio_player_set_is_series(player: Pointer, value: Boolean)
    fun nuvio_player_load_file(player: Pointer, url: String, audioUrl: String?, headersJson: String?)
    fun nuvio_player_play(player: Pointer)
    fun nuvio_player_pause(player: Pointer)
    fun nuvio_player_seek_to(player: Pointer, positionMs: Long)
    fun nuvio_player_seek_by(player: Pointer, offsetMs: Long)
    fun nuvio_player_set_speed(player: Pointer, speed: Float)
    fun nuvio_player_set_resize_mode(player: Pointer, mode: Int)
    fun nuvio_player_retry(player: Pointer)
    fun nuvio_player_refresh_state(player: Pointer)
    fun nuvio_player_is_loading(player: Pointer): Boolean
    fun nuvio_player_is_playing(player: Pointer): Boolean
    fun nuvio_player_is_ended(player: Pointer): Boolean
    fun nuvio_player_get_position_ms(player: Pointer): Long
    fun nuvio_player_get_duration_ms(player: Pointer): Long
    fun nuvio_player_get_buffered_ms(player: Pointer): Long
    fun nuvio_player_get_speed(player: Pointer): Float
    fun nuvio_player_get_error(player: Pointer): String?
    fun nuvio_player_get_audio_track_count(player: Pointer): Int
    fun nuvio_player_get_audio_track_id(player: Pointer, index: Int): Int
    fun nuvio_player_get_audio_track_label(player: Pointer, index: Int): String?
    fun nuvio_player_get_audio_track_lang(player: Pointer, index: Int): String?
    fun nuvio_player_is_audio_track_selected(player: Pointer, index: Int): Boolean
    fun nuvio_player_select_audio_track(player: Pointer, trackId: Int)
    fun nuvio_player_get_subtitle_track_count(player: Pointer): Int
    fun nuvio_player_get_subtitle_track_id(player: Pointer, index: Int): Int
    fun nuvio_player_get_subtitle_track_label(player: Pointer, index: Int): String?
    fun nuvio_player_get_subtitle_track_lang(player: Pointer, index: Int): String?
    fun nuvio_player_is_subtitle_track_selected(player: Pointer, index: Int): Boolean
    fun nuvio_player_select_subtitle_track(player: Pointer, trackId: Int)
    fun nuvio_player_set_subtitle_url(player: Pointer, url: String)
    fun nuvio_player_clear_external_subtitle(player: Pointer)
    fun nuvio_player_clear_external_subtitle_and_select(player: Pointer, trackId: Int)
    fun nuvio_player_apply_subtitle_style(player: Pointer, textColor: String, outlineSize: Float, fontSize: Float, subPos: Int)
    fun nuvio_player_show_skip_button(player: Pointer, type: String, endTimeMs: Long)
    fun nuvio_player_hide_skip_button(player: Pointer)
    fun nuvio_player_show_next_episode(player: Pointer, season: Int, episode: Int, title: String, thumbnail: String?, hasAired: Boolean)
    fun nuvio_player_hide_next_episode(player: Pointer)
    fun nuvio_player_is_closed(player: Pointer): Boolean
    fun nuvio_player_pop_next_episode_pressed(player: Pointer): Boolean
    fun nuvio_player_is_addon_subtitles_fetch_requested(player: Pointer): Boolean
    fun nuvio_player_set_addon_subtitles_loading(player: Pointer, loading: Boolean)
    fun nuvio_player_clear_addon_subtitles(player: Pointer)
    fun nuvio_player_add_addon_subtitle(player: Pointer, id: String, url: String, language: String, display: String)
    fun nuvio_player_pop_subtitle_style_changed(player: Pointer): Boolean
    fun nuvio_player_get_subtitle_style_color_index(player: Pointer): Int
    fun nuvio_player_get_subtitle_style_font_size(player: Pointer): Int
    fun nuvio_player_get_subtitle_style_outline_enabled(player: Pointer): Boolean
    fun nuvio_player_get_subtitle_style_bottom_offset(player: Pointer): Int
    fun nuvio_player_pop_sources_open_requested(player: Pointer): Boolean
    fun nuvio_player_pop_episodes_open_requested(player: Pointer): Boolean
    fun nuvio_player_pop_source_stream_selected(player: Pointer): String?
    fun nuvio_player_pop_source_filter_changed(player: Pointer): Boolean
    fun nuvio_player_get_source_filter_value(player: Pointer): String?
    fun nuvio_player_pop_source_reload(player: Pointer): Boolean
    fun nuvio_player_pop_episode_selected(player: Pointer): String?
    fun nuvio_player_pop_episode_stream_selected(player: Pointer): String?
    fun nuvio_player_pop_episode_filter_changed(player: Pointer): Boolean
    fun nuvio_player_get_episode_filter_value(player: Pointer): String?
    fun nuvio_player_pop_episode_reload(player: Pointer): Boolean
    fun nuvio_player_pop_episode_back(player: Pointer): Boolean
    fun nuvio_player_set_sources_loading(player: Pointer, loading: Boolean)
    fun nuvio_player_clear_source_streams(player: Pointer)
    fun nuvio_player_add_source_stream(player: Pointer, id: String, label: String, subtitle: String?, addonName: String, addonId: String, url: String, isCurrent: Boolean)
    fun nuvio_player_clear_source_addon_groups(player: Pointer)
    fun nuvio_player_add_source_addon_group(player: Pointer, id: String, addonName: String, addonId: String, isLoading: Boolean, hasError: Boolean)
    fun nuvio_player_set_source_selected_filter(player: Pointer, addonId: String?)
    fun nuvio_player_clear_episodes(player: Pointer)
    fun nuvio_player_add_episode(player: Pointer, id: String, title: String, overview: String?, thumbnail: String?, season: Int, episode: Int)
    fun nuvio_player_set_episode_streams_loading(player: Pointer, loading: Boolean)
    fun nuvio_player_clear_episode_streams(player: Pointer)
    fun nuvio_player_add_episode_stream(player: Pointer, id: String, label: String, subtitle: String?, addonName: String, addonId: String, url: String, isCurrent: Boolean)
    fun nuvio_player_clear_episode_addon_groups(player: Pointer)
    fun nuvio_player_add_episode_addon_group(player: Pointer, id: String, addonName: String, addonId: String, isLoading: Boolean, hasError: Boolean)
    fun nuvio_player_set_episode_selected_filter(player: Pointer, addonId: String?)
    fun nuvio_player_show_episode_streams(player: Pointer, season: Int, episode: Int, title: String?)
}
