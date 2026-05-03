package com.nuvio.app.features.notifications

import kotlinx.serialization.Serializable
import kotlin.math.abs

data class EpisodeReleaseNotificationsUiState(
    val isEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val permissionGranted: Boolean = false,
    val scheduledCount: Int = 0,
    val testTargetTitle: String? = null,
    val isSendingTest: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

@Serializable
internal data class StoredEpisodeReleaseNotificationsPayload(
    val enabled: Boolean = false,
    val followedShows: List<TrackedFollowedShow> = emptyList(),
)

@Serializable
internal data class TrackedFollowedShow(
    val contentId: String,
    val contentType: String,
    val followedOnIsoDate: String,
)

internal data class EpisodeReleaseNotificationRequest(
    val requestId: String,
    val notificationTitle: String,
    val notificationBody: String,
    val releaseDateIso: String,
    val deepLinkUrl: String,
    val backdropUrl: String? = null,
)

internal const val EpisodeReleaseNotificationHour = 9
internal const val EpisodeReleaseNotificationMinute = 0
internal const val MinReasonableSavedAtEpochMs = 946684800000L

internal fun buildTrackedShowKey(
    type: String,
    id: String,
): String = "${normalizeSeriesType(type)}:${id.trim()}"

internal fun normalizeSeriesType(type: String): String = when (type.trim().lowercase()) {
    "tv", "show", "series", "tvshow" -> "series"
    else -> type.trim().lowercase()
}

internal fun isSeriesLibraryType(type: String): Boolean = normalizeSeriesType(type) == "series"

internal fun releaseDateIso(rawValue: String?): String? {
    val value = rawValue
        ?.substringBefore('T')
        ?.trim()
        .orEmpty()
    return value.takeIf { it.length == 10 }
}

internal fun buildEpisodeReleaseNotificationId(
    profileId: Int,
    contentType: String,
    contentId: String,
    episodeId: String,
    releaseDateIso: String,
): String {
    val contentHash = abs(buildTrackedShowKey(contentType, contentId).hashCode())
    val episodeHash = abs(episodeId.trim().ifBlank { releaseDateIso }.hashCode())
    return "episode-release-$profileId-$contentHash-$episodeHash-$releaseDateIso"
}

internal fun buildEpisodeReleaseNotificationBody(
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
): String {
    val seasonLabel = seasonNumber?.let { season -> "S${season.toString().padStart(2, '0')}" }
    val episodeLabel = episodeNumber?.let { episode -> "E${episode.toString().padStart(2, '0')}" }
    val code = listOfNotNull(seasonLabel, episodeLabel).joinToString(separator = "")
    val title = episodeTitle?.trim().takeUnless { it.isNullOrBlank() }

    return when {
        code.isNotBlank() && title != null -> "$code • $title is out now"
        code.isNotBlank() -> "$code is out now"
        title != null -> "$title is out now"
        else -> "A new episode is out now"
    }
}