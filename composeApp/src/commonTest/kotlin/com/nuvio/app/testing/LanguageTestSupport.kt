package com.nuvio.app.testing

import com.nuvio.app.features.settings.AppLanguage
import com.nuvio.app.features.settings.ThemeSettingsRepository
import com.nuvio.app.features.settings.ThemeSettingsStorage

internal fun useEnglishTestLanguage() {
    ThemeSettingsRepository.clearLocalState()
    ThemeSettingsStorage.applySelectedAppLanguage(AppLanguage.ENGLISH.code)
}
