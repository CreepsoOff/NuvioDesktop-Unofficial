package com.nuvio.app.features.streams

import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.desktop.DesktopPreferences

internal actual object StreamLinkCacheStorage {
    private const val preferencesName = "nuvio_stream_link_cache"

    actual fun loadEntry(hashedKey: String): String? =
        DesktopPreferences.getString(preferencesName, ProfileScopedKey.of(hashedKey))

    actual fun saveEntry(hashedKey: String, payload: String) {
        DesktopPreferences.putString(preferencesName, ProfileScopedKey.of(hashedKey), payload)
    }

    actual fun removeEntry(hashedKey: String) {
        DesktopPreferences.remove(preferencesName, ProfileScopedKey.of(hashedKey))
    }
}

internal actual fun epochMs(): Long = System.currentTimeMillis()