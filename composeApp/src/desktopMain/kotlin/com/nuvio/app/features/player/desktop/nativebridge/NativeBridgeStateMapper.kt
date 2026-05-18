package com.nuvio.app.features.player.desktop.nativebridge

import com.nuvio.app.features.player.PlayerPlaybackSnapshot

internal data class NativeBridgePollState(
    val isClosed: Boolean,
    val snapshot: PlayerPlaybackSnapshot,
    val error: String?,
    val addonSubtitlesFetchRequested: Boolean,
    val subtitleStyleChanged: Boolean,
    val subtitleStyleColorIndex: Int,
    val subtitleStyleOutlineEnabled: Boolean,
    val subtitleStyleFontSize: Int,
    val subtitleStyleBottomOffset: Int,
    val nextEpisodePressed: Boolean,
    val sourcesOpenRequested: Boolean,
    val episodesOpenRequested: Boolean,
    val selectedSourceUrl: String?,
    val sourceFilterChanged: Boolean,
    val sourceFilterValue: String?,
    val sourceReloadRequested: Boolean,
    val selectedEpisodeId: String?,
    val selectedEpisodeStreamUrl: String?,
    val episodeFilterChanged: Boolean,
    val episodeFilterValue: String?,
    val episodeReloadRequested: Boolean,
    val episodeBackRequested: Boolean,
)
