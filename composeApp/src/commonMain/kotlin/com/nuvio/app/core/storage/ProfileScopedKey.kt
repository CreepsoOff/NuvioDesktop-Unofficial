package com.nuvio.app.core.storage

import com.nuvio.app.features.profiles.ProfileRepository


object ProfileScopedKey {
    fun of(baseKey: String): String = "${baseKey}_${ProfileRepository.activeProfileId}"
}
