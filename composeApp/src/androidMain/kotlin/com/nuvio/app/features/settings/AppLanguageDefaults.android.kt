package com.nuvio.app.features.settings

import java.util.Locale

internal actual object AppLanguageDefaults {
    actual fun systemLanguageCode(): String? =
        Locale.getDefault().toLanguageTag().takeIf { it.isNotBlank() }
}
