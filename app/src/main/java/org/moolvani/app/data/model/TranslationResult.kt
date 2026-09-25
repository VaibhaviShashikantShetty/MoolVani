package org.moolvani.app.data.model

enum class Language(val code: String, val displayName: String, val scriptName: String) {
    HINDI("hi", "Hindi", "देवनागरी"),
    SANTHALI("sat", "Santali", "ᱚᱞ ᱪᱤᱠᱤ")
}

data class TranslationResult(
    val sourceText: String,
    val sourceLang: Language,
    val targetLang: Language,
    val targetOlChiki: String,
    val targetRoman: String,
    val targetDevanagari: String = "",
    val confidence: Float = 1.0f,
    val latencyMs: Long = 0,
    val isExactMatch: Boolean = true,
    val matchedCategory: String = "General"
)
