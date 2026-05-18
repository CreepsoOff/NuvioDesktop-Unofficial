package com.nuvio.app.core.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs

internal val DefaultPosterCardWidthPreset = PosterCardWidthPreset.Balanced
internal const val DefaultPosterCardWidthDp = 126
internal const val DefaultPosterCardHeightDp = 189
internal const val DefaultPosterCardCornerRadiusDp = 12

enum class PosterCardWidthPreset(val storageKey: String) {
    Compact("compact"),
    Dense("dense"),
    Standard("standard"),
    Balanced("balanced"),
    Comfort("comfort"),
    Large("large"),
}

internal fun legacyMobilePosterWidthDp(preset: PosterCardWidthPreset): Int =
    when (preset) {
        PosterCardWidthPreset.Compact -> 104
        PosterCardWidthPreset.Dense -> 112
        PosterCardWidthPreset.Standard -> 120
        PosterCardWidthPreset.Balanced -> 126
        PosterCardWidthPreset.Comfort -> 134
        PosterCardWidthPreset.Large -> 140
    }

internal fun posterHeightForWidth(widthDp: Int): Int = (widthDp * 3) / 2

internal fun posterWidthPresetFromStorageKey(value: String?): PosterCardWidthPreset? =
    PosterCardWidthPreset.entries.firstOrNull { it.storageKey == value || it.name == value }

internal fun posterWidthPresetFromLegacyWidth(widthDp: Int): PosterCardWidthPreset =
    PosterCardWidthPreset.entries.minBy { preset ->
        abs(legacyMobilePosterWidthDp(preset) - widthDp)
    }

internal expect fun resolvedPosterWidthDp(preset: PosterCardWidthPreset): Int

@Serializable
private data class StoredPosterCardStylePreferences(
    val widthPreset: String? = null,
    val widthDp: Int = DefaultPosterCardWidthDp,
    val heightDp: Int = DefaultPosterCardHeightDp,
    val cornerRadiusDp: Int = DefaultPosterCardCornerRadiusDp,
    val catalogLandscapeModeEnabled: Boolean = false,
    val hideLabelsEnabled: Boolean = false,
)

data class PosterCardStyleUiState(
    val widthPreset: PosterCardWidthPreset = DefaultPosterCardWidthPreset,
    val widthDp: Int = resolvedPosterWidthDp(widthPreset),
    val heightDp: Int = posterHeightForWidth(widthDp),
    val cornerRadiusDp: Int = DefaultPosterCardCornerRadiusDp,
    val catalogLandscapeModeEnabled: Boolean = false,
    val hideLabelsEnabled: Boolean = false,
)

object PosterCardStyleRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _uiState = MutableStateFlow(PosterCardStyleUiState())
    val uiState: StateFlow<PosterCardStyleUiState> = _uiState.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        _uiState.value = PosterCardStyleUiState()
    }

    fun setWidthDp(widthDp: Int) {
        setWidthPreset(posterWidthPresetFromLegacyWidth(widthDp))
    }

    fun setWidthPreset(preset: PosterCardWidthPreset) {
        ensureLoaded()
        val nextWidth = resolvedPosterWidthDp(preset)
        val nextHeight = posterHeightForWidth(nextWidth)
        if (
            _uiState.value.widthPreset == preset &&
            _uiState.value.widthDp == nextWidth &&
            _uiState.value.heightDp == nextHeight
        ) return
        _uiState.value = _uiState.value.copy(
            widthPreset = preset,
            widthDp = nextWidth,
            heightDp = nextHeight,
        )
        persist()
    }

    fun setCornerRadiusDp(cornerRadiusDp: Int) {
        ensureLoaded()
        if (_uiState.value.cornerRadiusDp == cornerRadiusDp) return
        _uiState.value = _uiState.value.copy(cornerRadiusDp = cornerRadiusDp)
        persist()
    }

    fun setCatalogLandscapeModeEnabled(enabled: Boolean) {
        ensureLoaded()
        if (_uiState.value.catalogLandscapeModeEnabled == enabled) return
        _uiState.value = _uiState.value.copy(catalogLandscapeModeEnabled = enabled)
        persist()
    }

    fun setHideLabelsEnabled(enabled: Boolean) {
        ensureLoaded()
        if (_uiState.value.hideLabelsEnabled == enabled) return
        _uiState.value = _uiState.value.copy(hideLabelsEnabled = enabled)
        persist()
    }

    fun resetToDefaults() {
        ensureLoaded()
        if (_uiState.value == PosterCardStyleUiState()) return
        _uiState.value = PosterCardStyleUiState()
        persist()
    }

    private fun loadFromDisk() {
        hasLoaded = true

        val payload = PosterCardStyleStorage.loadPayload().orEmpty().trim()
        if (payload.isEmpty()) {
            _uiState.value = PosterCardStyleUiState()
            return
        }

        val stored = runCatching {
            json.decodeFromString<StoredPosterCardStylePreferences>(payload)
        }.getOrNull()

        _uiState.value = if (stored != null) {
            val preset = posterWidthPresetFromStorageKey(stored.widthPreset)
                ?: posterWidthPresetFromLegacyWidth(stored.widthDp.takeIf { it > 0 } ?: DefaultPosterCardWidthDp)
            val widthDp = resolvedPosterWidthDp(preset)
            val heightDp = posterHeightForWidth(widthDp)
            val cornerRadiusDp = stored.cornerRadiusDp.coerceAtLeast(0)
            PosterCardStyleUiState(
                widthPreset = preset,
                widthDp = widthDp,
                heightDp = heightDp,
                cornerRadiusDp = cornerRadiusDp,
                catalogLandscapeModeEnabled = stored.catalogLandscapeModeEnabled,
                hideLabelsEnabled = stored.hideLabelsEnabled,
            )
        } else {
            PosterCardStyleUiState()
        }
    }

    private fun persist() {
        val state = _uiState.value
        val legacyWidth = legacyMobilePosterWidthDp(state.widthPreset)
        PosterCardStyleStorage.savePayload(
            json.encodeToString(
                StoredPosterCardStylePreferences(
                    widthPreset = state.widthPreset.storageKey,
                    widthDp = legacyWidth,
                    heightDp = posterHeightForWidth(legacyWidth),
                    cornerRadiusDp = state.cornerRadiusDp,
                    catalogLandscapeModeEnabled = state.catalogLandscapeModeEnabled,
                    hideLabelsEnabled = state.hideLabelsEnabled,
                ),
            ),
        )
    }
}
