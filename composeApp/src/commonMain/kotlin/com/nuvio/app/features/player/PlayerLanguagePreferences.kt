package com.nuvio.app.features.player

data class LanguagePreferenceOption(
    val code: String,
    val label: String,
)

object AudioLanguageOption {
    const val DEFAULT = "default"
    const val DEVICE = "device"
}

object SubtitleLanguageOption {
    const val NONE = "none"
    const val DEVICE = "device"
    const val FORCED = "forced"
}

val AvailableLanguageOptions: List<LanguagePreferenceOption> = listOf(
    LanguagePreferenceOption("af", "Afrikaans"),
    LanguagePreferenceOption("sq", "Albanian"),
    LanguagePreferenceOption("am", "Amharic"),
    LanguagePreferenceOption("ar", "Arabic"),
    LanguagePreferenceOption("hy", "Armenian"),
    LanguagePreferenceOption("az", "Azerbaijani"),
    LanguagePreferenceOption("eu", "Basque"),
    LanguagePreferenceOption("be", "Belarusian"),
    LanguagePreferenceOption("bn", "Bengali"),
    LanguagePreferenceOption("bs", "Bosnian"),
    LanguagePreferenceOption("bg", "Bulgarian"),
    LanguagePreferenceOption("my", "Burmese"),
    LanguagePreferenceOption("ca", "Catalan"),
    LanguagePreferenceOption("zh", "Chinese"),
    LanguagePreferenceOption("zh-CN", "Chinese (Simplified)"),
    LanguagePreferenceOption("zh-TW", "Chinese (Traditional)"),
    LanguagePreferenceOption("hr", "Croatian"),
    LanguagePreferenceOption("cs", "Czech"),
    LanguagePreferenceOption("da", "Danish"),
    LanguagePreferenceOption("nl", "Dutch"),
    LanguagePreferenceOption("en", "English"),
    LanguagePreferenceOption("et", "Estonian"),
    LanguagePreferenceOption("tl", "Filipino"),
    LanguagePreferenceOption("fi", "Finnish"),
    LanguagePreferenceOption("fr", "French"),
    LanguagePreferenceOption("gl", "Galician"),
    LanguagePreferenceOption("ka", "Georgian"),
    LanguagePreferenceOption("de", "German"),
    LanguagePreferenceOption("el", "Greek"),
    LanguagePreferenceOption("gu", "Gujarati"),
    LanguagePreferenceOption("he", "Hebrew"),
    LanguagePreferenceOption("hi", "Hindi"),
    LanguagePreferenceOption("hu", "Hungarian"),
    LanguagePreferenceOption("is", "Icelandic"),
    LanguagePreferenceOption("id", "Indonesian"),
    LanguagePreferenceOption("ga", "Irish"),
    LanguagePreferenceOption("it", "Italian"),
    LanguagePreferenceOption("ja", "Japanese"),
    LanguagePreferenceOption("kn", "Kannada"),
    LanguagePreferenceOption("kk", "Kazakh"),
    LanguagePreferenceOption("km", "Khmer"),
    LanguagePreferenceOption("ko", "Korean"),
    LanguagePreferenceOption("lo", "Lao"),
    LanguagePreferenceOption("lv", "Latvian"),
    LanguagePreferenceOption("lt", "Lithuanian"),
    LanguagePreferenceOption("mk", "Macedonian"),
    LanguagePreferenceOption("ms", "Malay"),
    LanguagePreferenceOption("ml", "Malayalam"),
    LanguagePreferenceOption("mt", "Maltese"),
    LanguagePreferenceOption("mr", "Marathi"),
    LanguagePreferenceOption("mn", "Mongolian"),
    LanguagePreferenceOption("ne", "Nepali"),
    LanguagePreferenceOption("no", "Norwegian"),
    LanguagePreferenceOption("pa", "Punjabi"),
    LanguagePreferenceOption("fa", "Persian"),
    LanguagePreferenceOption("pl", "Polish"),
    LanguagePreferenceOption("pt", "Portuguese (Portugal)"),
    LanguagePreferenceOption("pt-BR", "Portuguese (Brazil)"),
    LanguagePreferenceOption("ro", "Romanian"),
    LanguagePreferenceOption("ru", "Russian"),
    LanguagePreferenceOption("sr", "Serbian"),
    LanguagePreferenceOption("si", "Sinhala"),
    LanguagePreferenceOption("sk", "Slovak"),
    LanguagePreferenceOption("sl", "Slovenian"),
    LanguagePreferenceOption("es", "Spanish"),
    LanguagePreferenceOption("es-419", "Spanish (Latin America)"),
    LanguagePreferenceOption("sw", "Swahili"),
    LanguagePreferenceOption("sv", "Swedish"),
    LanguagePreferenceOption("ta", "Tamil"),
    LanguagePreferenceOption("te", "Telugu"),
    LanguagePreferenceOption("th", "Thai"),
    LanguagePreferenceOption("tr", "Turkish"),
    LanguagePreferenceOption("uk", "Ukrainian"),
    LanguagePreferenceOption("ur", "Urdu"),
    LanguagePreferenceOption("uz", "Uzbek"),
    LanguagePreferenceOption("vi", "Vietnamese"),
    LanguagePreferenceOption("cy", "Welsh"),
    LanguagePreferenceOption("zu", "Zulu"),
)

private val Iso639Aliases = mapOf(
    "eng" to "en",
    "spa" to "es",
    "fra" to "fr",
    "fre" to "fr",
    "deu" to "de",
    "ger" to "de",
    "ita" to "it",
    "por" to "pt",
    "rus" to "ru",
    "jpn" to "ja",
    "kor" to "ko",
    "zho" to "zh",
    "chi" to "zh",
    "ara" to "ar",
    "hin" to "hi",
    "nld" to "nl",
    "dut" to "nl",
    "pol" to "pl",
    "swe" to "sv",
    "tur" to "tr",
    "heb" to "he",
)

fun normalizeLanguageCode(language: String?): String? {
    val raw = language
        ?.trim()
        ?.replace('_', '-')
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }
        ?: return null

    val primary = raw.substringBefore('-')
    val canonicalPrimary = Iso639Aliases[primary] ?: primary
    val suffix = raw.substringAfter('-', "")
    return if (suffix.isBlank()) {
        canonicalPrimary
    } else {
        "$canonicalPrimary-$suffix"
    }
}

fun languageMatchesPreference(trackLanguage: String?, targetLanguage: String): Boolean {
    val normalizedTrack = normalizeLanguageCode(trackLanguage) ?: return false
    val normalizedTarget = normalizeLanguageCode(targetLanguage) ?: return false
    if (normalizedTrack == normalizedTarget) return true

    val trackPrimary = normalizedTrack.substringBefore('-')
    val targetPrimary = normalizedTarget.substringBefore('-')
    return trackPrimary == targetPrimary
}

fun languageLabelForCode(code: String?): String {
    if (code.isNullOrBlank()) return "None"
    if (code.equals(SubtitleLanguageOption.FORCED, ignoreCase = true)) return "Forced"
    return AvailableLanguageOptions.firstOrNull {
        it.code.equals(code, ignoreCase = true)
    }?.label ?: formatLanguage(code)
}

fun resolvePreferredAudioLanguageTargets(
    preferredAudioLanguage: String,
    secondaryPreferredAudioLanguage: String?,
    deviceLanguages: List<String>,
): List<String> {
    fun normalize(language: String?): String? {
        val normalized = normalizeLanguageCode(language)
        return when (normalized) {
            null,
            AudioLanguageOption.DEFAULT,
            AudioLanguageOption.DEVICE,
            SubtitleLanguageOption.NONE,
            SubtitleLanguageOption.FORCED,
            -> null
            else -> normalized
        }
    }

    val primary = normalizeLanguageCode(preferredAudioLanguage) ?: AudioLanguageOption.DEVICE

    return when (primary) {
        AudioLanguageOption.DEFAULT -> listOfNotNull(
            normalize(secondaryPreferredAudioLanguage),
        ).distinct()

        AudioLanguageOption.DEVICE -> (
            deviceLanguages.mapNotNull(::normalize)
                + listOfNotNull(normalize(secondaryPreferredAudioLanguage))
            ).distinct()

        else -> listOfNotNull(
            normalize(preferredAudioLanguage),
            normalize(secondaryPreferredAudioLanguage),
        ).distinct()
    }
}

fun resolvePreferredSubtitleLanguageTargets(
    preferredSubtitleLanguage: String,
    secondaryPreferredSubtitleLanguage: String?,
    deviceLanguages: List<String>,
): List<String> {
    fun normalize(language: String?): String? {
        val normalized = normalizeLanguageCode(language)
        return when (normalized) {
            null,
            SubtitleLanguageOption.NONE,
            -> null
            AudioLanguageOption.DEFAULT -> null
            else -> normalized
        }
    }

    val primary = normalizeLanguageCode(preferredSubtitleLanguage) ?: SubtitleLanguageOption.NONE

    return when (primary) {
        SubtitleLanguageOption.NONE -> listOfNotNull(
            normalize(secondaryPreferredSubtitleLanguage),
        ).distinct()

        SubtitleLanguageOption.DEVICE -> (
            deviceLanguages.mapNotNull(::normalize)
                + listOfNotNull(normalize(secondaryPreferredSubtitleLanguage))
            ).distinct()

        else -> listOfNotNull(
            normalize(preferredSubtitleLanguage),
            normalize(secondaryPreferredSubtitleLanguage),
        ).distinct()
    }
}

internal expect object DeviceLanguagePreferences {
    fun preferredLanguageCodes(): List<String>
}

fun inferForcedSubtitleTrack(
    label: String?,
    language: String?,
    trackId: String?,
    hasForcedSelectionFlag: Boolean = false,
): Boolean {
    if (hasForcedSelectionFlag) return true

    val normalizedLanguage = normalizeLanguageCode(language)
    if (normalizedLanguage == SubtitleLanguageOption.FORCED) return true

    val text = listOfNotNull(label, language, trackId)
        .joinToString(" ")
        .lowercase()

    if ("forced" in text) return true
    return text.contains("songs") && text.contains("sign")
}
