package com.nuvio.app.features.profiles

import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.desktop.DesktopPreferences
import java.security.MessageDigest

actual object AvatarStorage {
    private const val preferencesName = "nuvio_avatar_cache"
    private const val payloadKey = "avatar_catalog_payload"

    actual fun loadPayload(): String? =
        DesktopPreferences.getString(preferencesName, ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        DesktopPreferences.putString(preferencesName, ProfileScopedKey.of(payloadKey), payload)
    }
}

actual object ProfilePinCacheStorage {
    private const val preferencesName = "nuvio_profile_pin_cache"

    actual fun loadPayload(profileIndex: Int): String? =
        DesktopPreferences.getString(preferencesName, payloadKey(profileIndex))

    actual fun savePayload(profileIndex: Int, payload: String) {
        DesktopPreferences.putString(preferencesName, payloadKey(profileIndex), payload)
    }

    actual fun removePayload(profileIndex: Int) {
        DesktopPreferences.remove(preferencesName, payloadKey(profileIndex))
    }

    private fun payloadKey(profileIndex: Int): String =
        ProfileScopedKey.of("profile_pin_cache_$profileIndex")
}

actual object ProfilePinCrypto {
    actual fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.encodeToByteArray())
        return digest.joinToString(separator = "") { byte ->
            byte.toUByte().toString(16).padStart(2, '0')
        }
    }
}
