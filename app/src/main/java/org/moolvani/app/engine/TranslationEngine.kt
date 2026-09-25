package org.moolvani.app.engine

import org.moolvani.app.data.model.Language
import org.moolvani.app.data.model.TranslationResult

interface TranslationEngine {
    fun translate(text: String, from: Language, to: Language): TranslationResult
}

interface OfflineNlpModel {
    val modelName: String
    val version: String
    val isLoaded: Boolean
    fun infer(input: String, fromCode: String, toCode: String): String?
}
