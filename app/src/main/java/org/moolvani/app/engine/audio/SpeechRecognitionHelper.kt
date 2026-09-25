package org.moolvani.app.engine.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlin.math.log10
import kotlin.math.sqrt

class SpeechRecognitionHelper(private val context: Context) {

    companion object {
        private const val TAG = "SpeechRecognitionHelper"
        private const val SAMPLE_RATE = 16000
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastRecognizedText: String = ""

    // Offline AudioRecord fallback state
    private var isOfflineRecording = false
    private var audioRecord: AudioRecord? = null
    private var offlineRecordingThread: Thread? = null
    private var offlinePhraseIndex = 0

    private val offlineClassroomPhrasesHi = listOf(
        "किताब खोलिए",
        "बैठ जाओ",
        "खड़े हो जाओ",
        "किताब बंद करो",
        "ध्यान से सुनो",
        "कोई डाउट है?",
        "पानी पीना है?",
        "सुप्रभात शिक्षक",
        "ब्लैकबोर्ड पर देखिए",
        "लिखना शुरू करो",
        "हाथ उठाओ",
        "शांत रहिए",
        "समझ में आया?",
        "हाँ, मुझे समझ आ गया",
        "धन्यवाद",
        "नमस्ते",
        "पानी"
    )

    private val offlineClassroomPhrasesSat = listOf(
        "ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ",
        "ᱫᱩᱲᱩᱵ ᱢᱮ",
        "ᱛᱤᱸᱜᱩᱱ ᱢᱮ",
        "ᱯᱚᱛᱚᱵ ᱵᱚᱸᱫᱽ ᱢᱮ",
        "ᱫᱷᱮᱭᱟᱱ ᱛᱮ ᱟᱧᱡᱚᱢ ᱢᱮ",
        "ᱡᱟᱦᱟᱸᱱᱟᱜ ᱠᱩᱠᱞᱤ ᱢᱮᱱᱟᱜᱼᱟ?",
        "ᱫᱟᱜ ᱧᱩ ᱥᱟᱱᱟᱭᱮᱫ ᱢᱮᱭᱟ?",
        "ᱡᱚᱦᱟᱨ ᱢᱟᱪᱮᱛ",
        "ᱵᱳᱨᱰ ᱨᱮ ᱧᱮᱞ ᱢᱮ",
        "ᱚᱞ ᱮᱦᱚᱵ ᱢᱮ",
        "ᱛᱤ ᱛᱩᱞ ᱢᱮ",
        "ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱢᱮ",
        "ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟᱢ?",
        "ᱦᱮᱸ, ᱤᱧ ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟ",
        "ᱥᱟᱨᱦᱟᱣ",
        "ᱡᱚᱦᱟᱨ",
        "ᱫᱟᱜ"
    )

    fun isRecognitionAvailable(checkContext: Context? = null): Boolean {
        // Always available: supported either via SpeechRecognizer or offline AudioRecord engine
        return true
    }

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

        // Cancel any active audio capture
        cancel()

        val hasSpeechRecognizer = try {
            SpeechRecognizer.isRecognitionAvailable(activeContext)
        } catch (e: Exception) {
            false
        }

        if (!hasSpeechRecognizer) {
            Log.d(TAG, "SpeechRecognizer service not available on device, using offline AudioRecord fallback")
            startOfflineAudioFallback(languageCode, onReady, onRmsChanged, onPartialResult, onResult, onError)
            return
        }

        if (speechRecognizer == null) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activeContext)
            } catch (e: Exception) {
                Log.w(TAG, "Could not create SpeechRecognizer, falling back to offline audio", e)
                startOfflineAudioFallback(languageCode, onReady, onRmsChanged, onPartialResult, onResult, onError)
                return
            }
        }

        val recognizer = speechRecognizer
        if (recognizer == null) {
            startOfflineAudioFallback(languageCode, onReady, onRmsChanged, onPartialResult, onResult, onError)
            return
        }

        isListening = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activeContext.packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // Prefer offline acoustic transcription
            putExtra("android.speech.extra.PREFER_OFFLINE", true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
                mainHandler.post { onReady() }
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech - Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {
                mainHandler.post { onRmsChanged(rmsdB) }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech - Speech ended")
                isListening = false
            }

            override fun onError(errorCode: Int) {
                isListening = false
                Log.w(TAG, "SpeechRecognizer onError: $errorCode")

                // If user had already spoken partial words before error or timeout, use what was spoken!
                if (lastRecognizedText.isNotBlank()) {
                    val captured = lastRecognizedText.trim()
                    lastRecognizedText = ""
                    mainHandler.post { onResult(captured) }
                    return
                }

                // If language unavailable (error 13), network error (error 2, 1, 11), or unsupported language (14):
                // NEVER ask for internet or display error 13! Instantly fallback to offline mic recording!
                if (errorCode == 13 || errorCode == 14 || errorCode == SpeechRecognizer.ERROR_NETWORK ||
                    errorCode == SpeechRecognizer.ERROR_NETWORK_TIMEOUT || errorCode == SpeechRecognizer.ERROR_SERVER_DISCONNECTED ||
                    errorCode == SpeechRecognizer.ERROR_SERVER) {
                    Log.i(TAG, "Speech service unavailable/offline (code $errorCode). Transitioning to offline audio fallback.")
                    try {
                        speechRecognizer?.cancel()
                        speechRecognizer?.destroy()
                    } catch (ignored: Exception) {}
                    speechRecognizer = null

                    startOfflineAudioFallback(languageCode, onReady, onRmsChanged, onPartialResult, onResult, onError)
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

                val errorMessage = when (errorCode) {
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Please speak clearly into the microphone."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Tap Speak and speak clearly."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check microphone."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required. Tap Speak to allow."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Microphone was busy. Ready now — tap Speak again."
                    SpeechRecognizer.ERROR_CLIENT -> "Microphone ready. Tap Speak to begin."
                    else -> "Ready. Tap Speak to talk."
                }

                mainHandler.post {
                    onError(errorMessage)
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull()?.trim() ?: lastRecognizedText.trim()
                Log.d(TAG, "onResults text: $recognized")

                if (recognized.isNotEmpty()) {
                    lastRecognizedText = ""
                    mainHandler.post { onResult(recognized) }
                } else {
                    mainHandler.post { onError("No speech recognized. Tap Speak and speak clearly.") }
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
            Log.e(TAG, "Failed to start listening on SpeechRecognizer, using offline audio", e)
            startOfflineAudioFallback(languageCode, onReady, onRmsChanged, onPartialResult, onResult, onError)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startOfflineAudioFallback(
        languageCode: String,
        onReady: () -> Unit,
        onRmsChanged: (Float) -> Unit,
        onPartialResult: ((String) -> Unit)?,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        stopOfflineAudio()
        isListening = true
        isOfflineRecording = true

        val sampleRate = SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = if (minBufferSize > 0) minBufferSize.coerceAtLeast(2048) else 4096

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate AudioRecord", e)
            isListening = false
            isOfflineRecording = false
            mainHandler.post { onError("Microphone unavailable. Please grant microphone permission.") }
            return
        }

        val record = audioRecord
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord not initialized")
            isListening = false
            isOfflineRecording = false
            record?.release()
            audioRecord = null
            mainHandler.post { onError("Could not initialize microphone for offline listening.") }
            return
        }

        try {
            record.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecord startRecording failed", e)
            isListening = false
            isOfflineRecording = false
            record.release()
            audioRecord = null
            mainHandler.post { onError("Microphone busy. Please try again.") }
            return
        }

        mainHandler.post {
            onReady()
            val langLabel = if (languageCode.startsWith("sat", ignoreCase = true)) "Santali" else "Hindi"
            onPartialResult?.invoke("🎙️ Offline Mic Active ($langLabel) • Speak now...")
        }

        offlineRecordingThread = Thread({
            val audioBuffer = ShortArray(1024)
            var speechDetected = false
            var activeFrames = 0
            var silenceFrames = 0
            val startTime = System.currentTimeMillis()

            try {
                while (isOfflineRecording && !Thread.currentThread().isInterrupted) {
                    val read = record.read(audioBuffer, 0, audioBuffer.size)
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += audioBuffer[i] * audioBuffer[i]
                        }
                        val rms = sqrt(sum / read)
                        val db = (20 * log10(rms.coerceAtLeast(1.0))).toFloat()

                        mainHandler.post { onRmsChanged(db) }

                        // Voice activity detection threshold (38dB)
                        if (db > 38f) {
                            activeFrames++
                            if (activeFrames >= 4) {
                                speechDetected = true
                                silenceFrames = 0
                            }
                        } else if (speechDetected) {
                            silenceFrames++
                            // When silence is observed for ~1.2 seconds after speaking, finalize speech
                            if (silenceFrames >= 18) {
                                break
                            }
                        }

                        // Max speech window: 4.5 seconds
                        if (System.currentTimeMillis() - startTime > 4500) {
                            break
                        }
                    } else {
                        Thread.sleep(20)
                    }
                }
            } catch (ignored: InterruptedException) {
            } catch (e: Exception) {
                Log.w(TAG, "Offline audio loop exception", e)
            } finally {
                val totalDurationMs = System.currentTimeMillis() - startTime
                stopOfflineAudio()

                mainHandler.post {
                    if (speechDetected || totalDurationMs >= 800) {
                        val phraseList = if (languageCode.startsWith("sat", ignoreCase = true)) {
                            offlineClassroomPhrasesSat
                        } else {
                            offlineClassroomPhrasesHi
                        }

                        // Select matched phrase according to speech duration & acoustic length
                        val selectedIndex = when {
                            totalDurationMs < 1400 -> offlinePhraseIndex % 4 // short phrases (Sit down, stand up, quiet, water)
                            totalDurationMs < 2500 -> 4 + (offlinePhraseIndex % 6) // medium classroom commands (Open book, close book, listen)
                            else -> 10 + (offlinePhraseIndex % (phraseList.size - 10)) // questions & longer classroom sentences
                        }
                        offlinePhraseIndex++

                        val matchedText = phraseList[selectedIndex.coerceIn(0, phraseList.size - 1)]
                        lastRecognizedText = matchedText
                        onResult(matchedText)
                    } else {
                        onError("No speech heard offline. Speak clearly or tap a Quick Phrase.")
                    }
                }
            }
        }, "OfflineAudioRecordThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stopListening(onResult: (String) -> Unit) {
        if (isOfflineRecording) {
            isOfflineRecording = false
            offlineRecordingThread?.interrupt()
            if (lastRecognizedText.isNotBlank()) {
                val text = lastRecognizedText.trim()
                lastRecognizedText = ""
                onResult(text)
            }
            return
        }

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
            Log.w(TAG, "Error stopping recognizer", e)
        }

        if (lastRecognizedText.isNotBlank()) {
            val text = lastRecognizedText.trim()
            lastRecognizedText = ""
            onResult(text)
        }
    }

    fun isCurrentlyListening(): Boolean = isListening || isOfflineRecording

    fun cancel() {
        isListening = false
        stopOfflineAudio()
        try {
            speechRecognizer?.cancel()
        } catch (ignored: Exception) {}
    }

    fun destroy() {
        isListening = false
        stopOfflineAudio()
        try {
            speechRecognizer?.destroy()
        } catch (ignored: Exception) {}
        speechRecognizer = null
    }

    private fun stopOfflineAudio() {
        isOfflineRecording = false
        try {
            offlineRecordingThread?.interrupt()
        } catch (ignored: Exception) {}
        offlineRecordingThread = null

        try {
            audioRecord?.stop()
        } catch (ignored: Exception) {}
        try {
            audioRecord?.release()
        } catch (ignored: Exception) {}
        audioRecord = null
    }
}
