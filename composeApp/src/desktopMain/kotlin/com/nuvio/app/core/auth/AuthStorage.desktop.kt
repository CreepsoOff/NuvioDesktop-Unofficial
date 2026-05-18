package com.nuvio.app.core.auth

import com.nuvio.app.desktop.DesktopPreferences

internal actual object AuthStorage {
    private const val preferencesName = "nuvio_auth"
    private const val anonymousUserIdKey = "anonymous_user_id"

    actual fun loadAnonymousUserId(): String? =
        DesktopPreferences.getString(preferencesName, anonymousUserIdKey)

    actual fun saveAnonymousUserId(userId: String) {
        DesktopPreferences.putString(preferencesName, anonymousUserIdKey, userId)
    }

    actual fun clearAnonymousUserId() {
        DesktopPreferences.remove(preferencesName, anonymousUserIdKey)
    }
}