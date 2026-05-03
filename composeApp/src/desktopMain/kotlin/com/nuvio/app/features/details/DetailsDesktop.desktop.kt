package com.nuvio.app.features.details

import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.desktop.DesktopPreferences

internal actual object MetaScreenSettingsStorage {
    private const val preferencesName = "nuvio_meta_screen_settings"
    private const val payloadKey = "meta_screen_settings_payload"

    actual fun loadPayload(): String? =
        DesktopPreferences.getString(preferencesName, ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        DesktopPreferences.putString(preferencesName, ProfileScopedKey.of(payloadKey), payload)
    }
}

internal actual object SeasonViewModeStorage {
    private const val preferencesName = "nuvio_season_view_mode"
    private const val valueKey = "season_view_mode"

    actual fun load(): SeasonViewMode? =
        SeasonViewMode.parse(
            DesktopPreferences.getString(preferencesName, ProfileScopedKey.of(valueKey)),
        )

    actual fun save(mode: SeasonViewMode) {
        DesktopPreferences.putString(
            preferencesName,
            ProfileScopedKey.of(valueKey),
            SeasonViewMode.persist(mode),
        )
    }
}