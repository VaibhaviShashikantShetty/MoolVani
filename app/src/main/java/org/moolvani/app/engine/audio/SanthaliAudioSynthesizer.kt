package org.moolvani.app.engine.audio

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import org.moolvani.app.engine.OlChikiTransliterator
import java.util.Locale

class SanthaliAudioSynthesizer(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "SanthaliAudioSynth"
    }

    private val assetPlayer = AssetAudioPlayer(context)
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    private var activeLocale: Locale = Locale("hi", "IN")

    // Pending speech request if called while TTS is initializing
    private var pendingSpeechText: String? = null
    private var pendingIsSanthali: Boolean = true
    private var pendingCompletion: (() -> Unit)? = null

    var currentSpeed: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            tts?.setSpeechRate(field)
        }

    init {
        initializeTts()
    }

    private fun initializeTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech", e)
            isTtsReady = false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return

            // Check available voices: prioritize Indian Hindi, then Indian English, then US English
            val hiLocale = Locale("hi", "IN")
            val hiResult = engine.isLanguageAvailable(hiLocale)

            if (hiResult >= TextToSpeech.LANG_AVAILABLE) {
                engine.setLanguage(hiLocale)
                activeLocale = hiLocale
                isTtsReady = true
                Log.d(TAG, "TTS initialized with Hindi (hi_IN)")
            } else {
                val enInLocale = Locale("en", "IN")
                val enResult = engine.isLanguageAvailable(enInLocale)
                if (enResult >= TextToSpeech.LANG_AVAILABLE) {
                    engine.setLanguage(enInLocale)
                    activeLocale = enInLocale
                    isTtsReady = true
                    Log.d(TAG, "TTS initialized with Indian English (en_IN)")
                } else {
                    val enUsLocale = Locale.US
                    val enUsResult = engine.isLanguageAvailable(enUsLocale)
                    if (enUsResult >= TextToSpeech.LANG_AVAILABLE) {
                        engine.setLanguage(enUsLocale)
                        activeLocale = enUsLocale
                        isTtsReady = true
                        Log.d(TAG, "TTS initialized with US English (en_US)")
                    } else {
                        val defLocale = Locale.getDefault()
                        val defResult = engine.setLanguage(defLocale)
                        activeLocale = defLocale
                        isTtsReady = (defResult >= TextToSpeech.LANG_AVAILABLE)
                        Log.d(TAG, "TTS initialized with default locale: $defLocale")
                    }
                }
            }

            engine.setPitch(1.0f)
            engine.setSpeechRate(currentSpeed)

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isPlaying = true
                }

                override fun onDone(utteranceId: String?) {
                    isPlaying = false
                    mainHandler.post {
                        val cb = pendingCompletion
                        pendingCompletion = null
                        cb?.invoke()
                    }
                }

                override fun onError(utteranceId: String?) {
                    isPlaying = false
                    Log.w(TAG, "TTS utterance playback error on ID: $utteranceId")
                    mainHandler.post {
                        val cb = pendingCompletion
                        pendingCompletion = null
                        cb?.invoke()
                    }
                }
            })

            // Execute queued speech if any
            pendingSpeechText?.let { text ->
                val santhali = pendingIsSanthali
                val cb = pendingCompletion
                pendingSpeechText = null
                speak(text, santhali, cb)
            }
        } else {
            Log.e(TAG, "TTS onInit failed with status: $status")
            isTtsReady = false
        }
    }

    /**
     * Speaks text using a robust hybrid engine:
     * 1. First priority: Pre-rendered, crystal-clear studio MP3 voice assets embedded directly in APK
     *    (guaranteed to play offline, loud, clear, zero robotic distortion or tone artifacts).
     * 2. Fallback: Dynamic offline Text-to-Speech using authentic vernacular phonetics.
     */
    fun speak(text: String, isSanthali: Boolean = true, onCompletion: (() -> Unit)? = null) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            onCompletion?.invoke()
            return
        }

        stop()

        // 1. Check if we have a crystal-clear pre-rendered MP3 recording in APK assets
        val assetPath = if (isSanthali) {
            AudioCatalog.findSanthaliAsset(trimmed)
        } else {
            AudioCatalog.findHindiAsset(trimmed)
        }

        if (assetPath != null) {
            val played = assetPlayer.playAsset(assetPath, currentSpeed) {
                isPlaying = false
                onCompletion?.invoke()
            }
            if (played) {
                isPlaying = true
                return
            }
        }

        // 1B. Check for multi-clause / combined sentences (e.g. "ᱫᱩᱲᱩᱵ ᱢᱮ ᱟᱨ ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ")
        if (isSanthali && (trimmed.contains(" ᱟᱨ ") || trimmed.contains(", "))) {
            val delimiter = if (trimmed.contains(" ᱟᱨ ")) " ᱟᱨ " else ", "
            val parts = trimmed.split(delimiter).map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size >= 2) {
                playSequentialParts(parts, 0, onCompletion)
                return
            }
        }

        // 2. Fallback for custom dynamic sentences via Android TextToSpeech
        if (!isTtsReady || tts == null) {
            // Queue request while TTS finishes initialization
            pendingSpeechText = trimmed
            pendingIsSanthali = isSanthali
            pendingCompletion = onCompletion
            return
        }

        pendingCompletion = onCompletion

        val engine = tts ?: run {
            pendingCompletion?.invoke()
            pendingCompletion = null
            return
        }

        // Prepare human speech text matching the active engine voice
        val textToSpeak: String = if (isSanthali) {
            if (activeLocale.language == "hi") {
                // In Hindi TTS, pronounce authentic Devanagari phonetics
                OlChikiTransliterator.toPhoneticDevanagari(trimmed)
            } else {
                // In English TTS, pronounce Roman phonetics
                OlChikiTransliterator.toPhoneticRoman(trimmed)
            }
        } else {
            if (activeLocale.language == "hi") {
                trimmed
            } else {
                OlChikiTransliterator.devanagariToRoman(trimmed)
            }
        }

        try {
            engine.setSpeechRate(currentSpeed)
            engine.setPitch(1.0f)

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            val utteranceId = "moolvani_${System.currentTimeMillis()}"

            isPlaying = true
            val result = engine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                Log.w(TAG, "speak queue flush returned error code: $result")
                isPlaying = false
                mainHandler.post {
                    val cb = pendingCompletion
                    pendingCompletion = null
                    cb?.invoke()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during TTS speak", e)
            isPlaying = false
            mainHandler.post {
                val cb = pendingCompletion
                pendingCompletion = null
                cb?.invoke()
            }
        }
    }

    private fun playSequentialParts(parts: List<String>, index: Int, onCompletion: (() -> Unit)?) {
        if (index >= parts.size) {
            isPlaying = false
            onCompletion?.invoke()
            return
        }

        val partText = parts[index]
        val assetPath = AudioCatalog.findSanthaliAsset(partText)
        if (assetPath != null) {
            isPlaying = true
            assetPlayer.playAsset(assetPath, currentSpeed) {
                mainHandler.postDelayed({
                    playSequentialParts(parts, index + 1, onCompletion)
                }, 350)
            }
        } else {
            speak(partText, isSanthali = true) {
                mainHandler.postDelayed({
                    playSequentialParts(parts, index + 1, onCompletion)
                }, 350)
            }
        }
    }

    fun stop() {
        assetPlayer.stop()
        try {
            tts?.stop()
        } catch (ignored: Exception) {}
        isPlaying = false
    }

    fun isCurrentlyPlaying(): Boolean = isPlaying || assetPlayer.isCurrentlyPlaying()

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (ignored: Exception) {}
        tts = null
        isTtsReady = false
    }
}
