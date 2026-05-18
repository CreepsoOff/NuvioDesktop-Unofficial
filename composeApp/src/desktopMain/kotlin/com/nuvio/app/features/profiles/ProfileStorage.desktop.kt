package com.nuvio.app.features.profiles

import com.nuvio.app.desktop.DesktopPreferences

internal actual object ProfileStorage {
    private const val preferencesName = "nuvio_profile_cache"
    private const val payloadKey = "profile_payload"

    actual fun loadPayload(): String? =
        DesktopPreferences.getString(preferencesName, payloadKey)

    actual fun savePayload(payload: String) {
        DesktopPreferences.putString(preferencesName, payloadKey, payload)
    }
}