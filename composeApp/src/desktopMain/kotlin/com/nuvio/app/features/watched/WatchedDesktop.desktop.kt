package com.nuvio.app.features.watched

import com.nuvio.app.desktop.DesktopPreferences

actual object WatchedStorage {
    private const val preferencesName = "nuvio_watched"
    private const val payloadKey = "watched_payload"

    actual fun loadPayload(profileId: Int): String? =
        DesktopPreferences.getString(preferencesName, "${payloadKey}_$profileId")

    actual fun savePayload(profileId: Int, payload: String) {
        DesktopPreferences.putString(preferencesName, "${payloadKey}_$profileId", payload)
    }
}

actual object WatchedClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()
}