package com.nuvio.app.features.settings

internal expect object AppLanguageDefaults {
    fun systemLanguageCode(): String?
}
