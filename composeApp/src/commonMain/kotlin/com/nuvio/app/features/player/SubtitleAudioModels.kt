package com.nuvio.app.features.player

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

data class AudioTrack(
    val index: Int,
    val id: String,
    val label: String,
    val language: String? = null,
    val isSelected: Boolean = false,
)

data class SubtitleTrack(
    val index: Int,
    val id: String,
    val label: String,
    val language: String? = null,
    val isSelected: Boolean = false,
    val isForced: Boolean = false,
)

data class AddonSubtitle(
    val id: String,
    val url: String,
    val language: String,
    val display: String,
    val isSelected: Boolean = false,
)

enum class SubtitleTab {
    BuiltIn,
    Addons,
    Style,
}

data class SubtitleStyleState(
    val textColor: Color = Color.White,
    val outlineEnabled: Boolean = false,
    val fontSizeSp: Int = 18,
    val bottomOffset: Int = 20,
) {
    companion object {
        val DEFAULT = SubtitleStyleState()
    }
}

val SubtitleColorSwatches = listOf(
    Color.White,
    Color(0xFFFFD700),
    Color(0xFF00E5FF),
    Color(0xFFFF5C5C),
    Color(0xFF00FF88),
    Color(0xFF9B59B6),
    Color(0xFFF97316),
    Color(0xFF22C55E),
    Color(0xFF3B82F6),
    Color.Black,
)

fun Color.toStorageHexString(): String {
    fun component(value: Float): String =
        (value * 255f).roundToInt().coerceIn(0, 255).toString(16).padStart(2, '0').uppercase()

    return buildString {
        append('#')
        append(component(alpha))
        append(component(red))
        append(component(green))
        append(component(blue))
    }
}

fun subtitleColorFromStorage(value: String?): Color? {
    val normalized = value
        ?.trim()
        ?.removePrefix("#")
        ?.takeIf { it.length == 6 || it.length == 8 }
        ?: return null

    val argb = if (normalized.length == 6) {
        "FF$normalized"
    } else {
        normalized
    }

    val parsed = argb.toLongOrNull(16) ?: return null
    return Color(
        red = ((parsed shr 16) and 0xFF).toFloat() / 255f,
        green = ((parsed shr 8) and 0xFF).toFloat() / 255f,
        blue = (parsed and 0xFF).toFloat() / 255f,
        alpha = ((parsed shr 24) and 0xFF).toFloat() / 255f,
    )
}

data class SubtitleAudioUiState(
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val addonSubtitles: List<AddonSubtitle> = emptyList(),
    val isLoadingAddonSubtitles: Boolean = false,
    val addonSubtitleError: String? = null,
    val selectedAudioIndex: Int = -1,
    val selectedSubtitleIndex: Int = -1,
    val selectedAddonSubtitleId: String? = null,
    val useCustomSubtitles: Boolean = false,
    val subtitleStyle: SubtitleStyleState = SubtitleStyleState.DEFAULT,
    val showAudioModal: Boolean = false,
    val showSubtitleModal: Boolean = false,
    val activeSubtitleTab: SubtitleTab = SubtitleTab.BuiltIn,
)

fun getTrackDisplayName(label: String?, language: String?, index: Int): String {
    if (!label.isNullOrBlank()) return label
    if (!language.isNullOrBlank()) return formatLanguage(language)
    return "Track ${index + 1}"
}

fun formatLanguage(code: String): String {
    val lower = code.lowercase()
    return LanguageNames[lower] ?: lower.replaceFirstChar { it.uppercase() }
}

private val LanguageNames = mapOf(
    "en" to "English",
    "eng" to "English",
    "es" to "Spanish",
    "spa" to "Spanish",
    "fr" to "French",
    "fre" to "French",
    "fra" to "French",
    "de" to "German",
    "ger" to "German",
    "deu" to "German",
    "it" to "Italian",
    "ita" to "Italian",
    "pt" to "Portuguese",
    "por" to "Portuguese",
    "ru" to "Russian",
    "rus" to "Russian",
    "ja" to "Japanese",
    "jpn" to "Japanese",
    "ko" to "Korean",
    "kor" to "Korean",
    "zh" to "Chinese",
    "chi" to "Chinese",
    "zho" to "Chinese",
    "ar" to "Arabic",
    "ara" to "Arabic",
    "hi" to "Hindi",
    "hin" to "Hindi",
    "nl" to "Dutch",
    "nld" to "Dutch",
    "dut" to "Dutch",
    "pl" to "Polish",
    "pol" to "Polish",
    "sv" to "Swedish",
    "swe" to "Swedish",
    "tr" to "Turkish",
    "tur" to "Turkish",
    "he" to "Hebrew",
    "heb" to "Hebrew",
    "th" to "Thai",
    "tha" to "Thai",
    "vi" to "Vietnamese",
    "vie" to "Vietnamese",
    "cs" to "Czech",
    "ces" to "Czech",
    "cze" to "Czech",
    "ro" to "Romanian",
    "ron" to "Romanian",
    "rum" to "Romanian",
    "hu" to "Hungarian",
    "hun" to "Hungarian",
    "el" to "Greek",
    "ell" to "Greek",
    "gre" to "Greek",
    "da" to "Danish",
    "dan" to "Danish",
    "fi" to "Finnish",
    "fin" to "Finnish",
    "no" to "Norwegian",
    "nor" to "Norwegian",
    "uk" to "Ukrainian",
    "ukr" to "Ukrainian",
    "bg" to "Bulgarian",
    "bul" to "Bulgarian",
    "hr" to "Croatian",
    "hrv" to "Croatian",
    "sr" to "Serbian",
    "srp" to "Serbian",
    "sk" to "Slovak",
    "slk" to "Slovak",
    "slo" to "Slovak",
    "sl" to "Slovenian",
    "slv" to "Slovenian",
    "id" to "Indonesian",
    "ind" to "Indonesian",
    "ms" to "Malay",
    "msa" to "Malay",
    "may" to "Malay",
    "ta" to "Tamil",
    "tam" to "Tamil",
    "te" to "Telugu",
    "tel" to "Telugu",
    "ml" to "Malayalam",
    "mal" to "Malayalam",
    "bn" to "Bengali",
    "ben" to "Bengali",
    "ur" to "Urdu",
    "urd" to "Urdu",
)
