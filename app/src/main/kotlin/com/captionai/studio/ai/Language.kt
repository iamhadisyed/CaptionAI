package com.captionai.studio.ai

/**
 * Maps the label stored in Settings (which may be shown in native script, e.g. "اردو")
 * to the canonical language name used in prompts.
 */
object Language {
    const val ENGLISH = "English"
    const val URDU = "Urdu"
    const val ROMAN_URDU = "Roman Urdu"

    /** Labels shown in the Settings language selector. */
    val DISPLAY_LABELS = listOf("English", "اردو", "Roman Urdu")

    fun normalize(label: String?): String {
        val v = label?.trim().orEmpty()
        return when {
            v.equals(URDU, ignoreCase = true) || v == "اردو" -> URDU
            v.equals(ROMAN_URDU, ignoreCase = true) -> ROMAN_URDU
            else -> ENGLISH
        }
    }

    fun outputInstruction(language: String): String = when (normalize(language)) {
        URDU -> "Write all captions and bio in Urdu script (اردو)."
        ROMAN_URDU -> "Write all captions and bio in Roman Urdu (Urdu written in English letters)."
        else -> "Write all captions and bio in English."
    }

    fun trendingName(language: String): String = when (normalize(language)) {
        URDU -> "Urdu script"
        ROMAN_URDU -> "Roman Urdu"
        else -> "English"
    }
}
