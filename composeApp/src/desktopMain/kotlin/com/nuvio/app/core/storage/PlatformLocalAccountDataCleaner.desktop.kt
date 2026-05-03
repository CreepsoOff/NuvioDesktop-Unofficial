package com.nuvio.app.core.storage

import com.nuvio.app.desktop.DesktopPreferences

internal actual object PlatformLocalAccountDataCleaner {
    private val preferenceNames = listOf(
        "nuvio_addons",
        "nuvio_library",
        "nuvio_home_catalog_settings",
        "nuvio_meta_screen_settings",
        "nuvio_player_settings",
        "nuvio_profile_cache",
        "nuvio_search_history",
        "nuvio_theme_settings",
        "nuvio_poster_card_style",
        "nuvio_mdblist_settings",
        "nuvio_trakt_auth",
        "nuvio_trakt_comments",
        "nuvio_trakt_library",
        "nuvio_watched",
        "nuvio_stream_link_cache",
        "nuvio_continue_watching_preferences",
        "nuvio_cw_enrichment",
        "nuvio_resume_prompt",
        "nuvio_episode_release_notifications",
        "nuvio_watch_progress",
        "nuvio_plugins",
        "nuvio_collections",
        "nuvio_downloads",
        "nuvio_tmdb_settings",
        "nuvio_season_view_mode",
    )

    actual fun wipe() {
        preferenceNames.forEach(DesktopPreferences::clearNode)
    }
}