package org.moolvani.app.engine.audio

import android.content.Context
import org.moolvani.app.data.model.Language
import org.moolvani.app.data.model.TranslationResult

class AudioEngine(context: Context) {

    private val synthesizer = SanthaliAudioSynthesizer(context)
    private var lastResult: TranslationResult? = null
    private var lastText: String? = null
    private var lastIsSanthali: Boolean = true

    var playbackSpeed: Float = 1.0f
        set(value) {
            field = value
            synthesizer.currentSpeed = value
        }

    fun playTranslation(result: TranslationResult, onDone: (() -> Unit)? = null) {
        lastResult = result
        val isTargetSanthali = result.targetLang == Language.SANTHALI
        val textToSpeak = if (isTargetSanthali) {
            if (result.targetOlChiki.isNotEmpty()) result.targetOlChiki else result.targetRoman
        } else {
            result.targetDevanagari.ifEmpty { result.targetRoman }
        }
        lastText = textToSpeak
        lastIsSanthali = isTargetSanthali

        synthesizer.speak(textToSpeak, isSanthali = isTargetSanthali, onCompletion = onDone)
    }

    fun playText(text: String, isSanthali: Boolean, onDone: (() -> Unit)? = null) {
        lastText = text
        lastIsSanthali = isSanthali
        synthesizer.speak(text, isSanthali = isSanthali, onCompletion = onDone)
    }

    fun repeatLast(onDone: (() -> Unit)? = null): Boolean {
        val text = lastText ?: return false
        synthesizer.speak(text, isSanthali = lastIsSanthali, onCompletion = onDone)
        return true
    }

    fun stop() {
        synthesizer.stop()
    }

    fun isPlaying(): Boolean = synthesizer.isCurrentlyPlaying()

    fun release() {
        synthesizer.shutdown()
    }
}
