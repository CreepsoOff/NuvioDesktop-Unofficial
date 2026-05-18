package com.nuvio.app.features.settings

import platform.Foundation.NSUserDefaults

internal actual object AppLanguageDefaults {
    actual fun systemLanguageCode(): String? {
        val preferred = NSUserDefaults.standardUserDefaults
            .objectForKey("AppleLanguages") as? List<*>
        return preferred
            ?.firstOrNull()
            ?.let { it as? String }
            ?.takeIf { it.isNotBlank() }
    }
}
