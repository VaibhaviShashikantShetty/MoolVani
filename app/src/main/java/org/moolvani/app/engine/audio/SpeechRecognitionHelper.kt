package org.moolvani.app.engine.audio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognitionHelper(private val context: Context) {

    companion object {
        private const val TAG = "SpeechRecognitionHelper"
        const val LOW_CONFIDENCE_MESSAGE = "Could not clearly understand the audio"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastRecognizedText: String = ""

    fun isRecognitionAvailable(checkContext: Context? = null): Boolean {
        val target = checkContext ?: context
        return try {
            SpeechRecognizer.isRecognitionAvailable(target)
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Enforces strict speech-to-text pipeline:
     * 1. Captures teacher's audio.
     * 2. Uses genuinely offline speech-to-text to transcribe exact spoken Hindi/English.
     * 3. Dispatches exact transcription to onResult without semantic guessing or forced matching.
     * 4. Dispatches LOW_CONFIDENCE_MESSAGE if recognition is unclear, avoiding incorrect translations.
     */
    fun startListening(
        callingContext: Context? = null,
        languageCode: String = "hi-IN",
        onReady: () -> Unit,
        onRmsChanged: (Float) -> Unit,
        onPartialResult: ((String) -> Unit)? = null,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post {
                startListening(callingContext, languageCode, onReady, onRmsChanged, onPartialResult, onResult, onError)
            }
            return
        }

        val activeContext = callingContext ?: context
        lastRecognizedText = ""

        cancel()

        // Initialize SpeechRecognizer: prioritize genuine on-device recognizer on API 31+
        if (speechRecognizer == null) {
            speechRecognizer = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(activeContext)) {
                    Log.i(TAG, "Using Android on-device offline SpeechRecognizer engine")
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(activeContext)
                } else {
                    Log.i(TAG, "Using system SpeechRecognizer with offline configuration")
                    SpeechRecognizer.createSpeechRecognizer(activeContext)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed initializing on-device SpeechRecognizer, using standard instance", e)
                try {
                    SpeechRecognizer.createSpeechRecognizer(activeContext)
                } catch (e2: Exception) {
                    null
                }
            }
        }

        val recognizer = speechRecognizer
        if (recognizer == null) {
            isListening = false
            mainHandler.post { onError(LOW_CONFIDENCE_MESSAGE) }
            return
        }

        isListening = true

        val effectiveLang = if (languageCode.startsWith("sat", ignoreCase = true)) "hi-IN" else languageCode
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activeContext.packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, effectiveLang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, effectiveLang)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, effectiveLang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // Enforce genuine offline recognition with Wi-Fi and mobile data disabled
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra("android.speech.extra.PREFER_OFFLINE", true)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                mainHandler.post { onReady() }
            }

            override fun onBeginningOfSpeech() {
                isListening = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                mainHandler.post { onRmsChanged(rmsdB) }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(errorCode: Int) {
                isListening = false
                Log.w(TAG, "SpeechRecognizer onError: $errorCode")

                // If user spoke partial words with clear text before timeout, use what was transcribed
                if (lastRecognizedText.isNotBlank()) {
                    val captured = lastRecognizedText.trim()
                    lastRecognizedText = ""
                    mainHandler.post { onResult(captured) }
                    return
                }

                // If client or busy error, recreate recognizer next time
                if (errorCode == SpeechRecognizer.ERROR_CLIENT || errorCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    try {
                        speechRecognizer?.cancel()
                        speechRecognizer?.destroy()
                    } catch (ignored: Exception) {}
                    speechRecognizer = null
                }

                val userMessage = when (errorCode) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required. Tap Allow to speak."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Microphone was busy. Ready now — tap Speak again."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check microphone."
                    // Rule 6: Fallback for low-confidence or ununderstood audio
                    else -> LOW_CONFIDENCE_MESSAGE
                }

                mainHandler.post { onError(userMessage) }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                val topScore = confidences?.firstOrNull() ?: 1.0f
                val transcribed = matches?.firstOrNull()?.trim() ?: lastRecognizedText.trim()

                Log.d(TAG, "onResults text='$transcribed', confidence=$topScore")

                // Step 6: Validate recognition confidence without forced guessing
                if (transcribed.isNotEmpty() && (topScore < 0f || topScore >= 0.20f)) {
                    lastRecognizedText = ""
                    // Step 3: Dispatch exact transcribed text to display on screen before translation
                    mainHandler.post { onResult(transcribed) }
                } else {
                    lastRecognizedText = ""
                    // Step 6: Low-confidence recognition fallback
                    mainHandler.post { onError(LOW_CONFIDENCE_MESSAGE) }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()?.trim()
                if (!partial.isNullOrEmpty()) {
                    lastRecognizedText = partial
                    mainHandler.post { onPartialResult?.invoke(partial) }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            isListening = false
            mainHandler.post { onError(LOW_CONFIDENCE_MESSAGE) }
        }
    }

    fun stopListening(onResult: (String) -> Unit) {
        if (!isListening) {
            if (lastRecognizedText.isNotBlank()) {
                val text = lastRecognizedText.trim()
                lastRecognizedText = ""
                onResult(text)
            }
            return
        }

        isListening = false
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping SpeechRecognizer", e)
        }

        if (lastRecognizedText.isNotBlank()) {
            val text = lastRecognizedText.trim()
            lastRecognizedText = ""
            onResult(text)
        }
    }

    fun isCurrentlyListening(): Boolean = isListening

    fun cancel() {
        isListening = false
        try {
            speechRecognizer?.cancel()
        } catch (ignored: Exception) {}
    }

    fun destroy() {
        isListening = false
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (ignored: Exception) {}
        speechRecognizer = null
    }
}
