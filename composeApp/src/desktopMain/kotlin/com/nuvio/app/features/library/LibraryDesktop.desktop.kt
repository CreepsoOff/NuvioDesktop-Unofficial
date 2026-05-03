package com.nuvio.app.features.library

import com.nuvio.app.desktop.DesktopPreferences

internal actual object LibraryStorage {
    private const val preferencesName = "nuvio_library"
    private const val payloadKey = "library_payload"

    actual fun loadPayload(profileId: Int): String? =
        DesktopPreferences.getString(preferencesName, "${payloadKey}_$profileId")

    actual fun savePayload(profileId: Int, payload: String) {
        DesktopPreferences.putString(preferencesName, "${payloadKey}_$profileId", payload)
    }
}

internal actual object LibraryClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()
}