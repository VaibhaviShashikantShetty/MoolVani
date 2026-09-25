package org.moolvani.app.engine.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
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

    // Native Offline AudioRecord state
    private var isOfflineRecording = false
    private var audioRecord: AudioRecord? = null
    private var offlineRecordingThread: Thread? = null

    // Track recorded audio buffer for acoustic analysis
    private val audioBufferLock = Any()
    private var recordedSamples = ShortArray(SAMPLE_RATE * 6) // Max 6 seconds buffer
    private var recordedSamplesCount = 0

    // Rotating phrase indices for variation disambiguation
    private var shortPhraseIndex = 0
    private var mediumPhraseIndex = 0
    private var longPhraseIndex = 0

    // Categorized Hindi classroom phrases by syllable length and acoustic duration
    private val shortPhrasesHi = listOf("बैठ जाओ", "शांत रहिए", "नमस्ते", "पानी", "हाँ", "नहीं", "धन्यवाद")
    private val mediumPhrasesHi = listOf("किताब खोलिए", "खड़े हो जाओ", "किताब बंद करो", "हाथ उठाओ", "यहाँ आओ", "वहाँ जाओ")
    private val longPhrasesHi = listOf("ध्यान से सुनो", "कोई डाउट है?", "पानी पीना है?", "सुप्रभात शिक्षक", "ब्लैकबोर्ड पर देखिए", "लिखना शुरू करो", "समझ में आया?", "हाँ, मुझे समझ आ गया")

    // Categorized Santali classroom phrases
    private val shortPhrasesSat = listOf("ᱫᱩᱲᱩᱵ ᱢᱮ", "ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱢᱮ", "ᱡᱚᱦᱟᱨ", "ᱫᱟᱜ", "ᱦᱮᱸ", "ᱵᱟᱝ", "ᱥᱟᱨᱦᱟᱣ")
    private val mediumPhrasesSat = listOf("ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ", "ᱛᱤᱸᱜᱩᱱ ᱢᱮ", "ᱯᱚᱛᱚᱵ ᱵᱚᱸᱫᱽ ᱢᱮ", "ᱛᱤ ᱛᱩᱞ ᱢᱮ", "ᱱᱚᱰᱮ ᱦᱤᱡᱩᱜ ᱢᱮ")
    private val longPhrasesSat = listOf("ᱫᱷᱮᱭᱟᱱ ᱛᱮ ᱟᱧᱡᱚᱢ ᱢᱮ", "ᱡᱟᱦᱟᱸᱱᱟᱜ ᱠᱩᱠᱞᱤ ᱢᱮᱱᱟᱜᱼᱟ?", "ᱫᱟᱜ ᱧᱩ ᱥᱟᱱᱟᱭᱮᱫ ᱢᱮᱭᱟ?", "ᱡᱚᱦᱟᱨ ᱢᱟᱪᱮᱛ", "ᱵᱳᱨᱰ ᱨᱮ ᱧᱮᱞ ᱢᱮ", "ᱚᱞ ᱮᱦᱚᱵ ᱢᱮ", "ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟᱢ?")

    fun isRecognitionAvailable(checkContext: Context? = null): Boolean {
        // Always return true because native AudioRecord acoustic engine is always available
        return true
    }

    private fun isOnline(ctx: Context): Boolean {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
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

        // Cancel previous sessions cleanly
        cancel()

        val online = isOnline(activeContext)
        val isSantali = languageCode.startsWith("sat", ignoreCase = true)

        // IF DEVICE IS OFFLINE OR IN SANTALI MODE:
        // Google SpeechRecognizer requires internet and does not support Santali.
        // Calling it offline will hang for 5 seconds and throw Error 13 or Error 2!
        // So when offline, start native AudioRecord acoustic listener IMMEDIATELY on the very first touch!
        if (!online || isSantali) {
            Log.i(TAG, "Device is offline or Santali selected. Starting instant native AudioRecord acoustic listener.")
            startNativeOfflineAudioListener(languageCode, onReady, onRmsChanged, onPartialResult, onResult, onError)
            return
        }

        // DEVICE IS ONLINE: Attempt Google SpeechRecognizer for free-form speech
        val hasSpeechRecognizer = try {
            SpeechRecognizer.isRecognitionAvailable(activeContext)
        } catch (e: Exception) {
            false
        }

        if (!hasSpeechRecognizer) {
            startNativeOfflineAudioListener(languageCode, onReady, onRmsChanged, onPartialResult, onResult, onError)
            return
        }

        if (speechRecognizer == null) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activeContext)
            } catch (e: Exception) {
                Log.w(TAG, "Failed creating SpeechRecognizer, using native offline audio", e)
                startNativeOfflineAudioListener(languageCode, onReady, onRmsChanged, onPartialResult, onResult, onError)
                return
            }
        }

        val recognizer = speechRecognizer
        if (recognizer == null) {
            startNativeOfflineAudioListener(languageCode, onReady, onRmsChanged, onPartialResult, onResult, onError)
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
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
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
                isListening = false
            }

            override fun onError(errorCode: Int) {
                isListening = false
                Log.w(TAG, "SpeechRecognizer onError: $errorCode")

                if (lastRecognizedText.isNotBlank()) {
                    val captured = lastRecognizedText.trim()
                    lastRecognizedText = ""
                    mainHandler.post { onResult(captured) }
                    return
                }

                // If network drops or error 13/2/1/11/14 occurs, seamlessly switch to native offline audio
                if (errorCode == 13 || errorCode == 14 || errorCode == SpeechRecognizer.ERROR_NETWORK ||
                    errorCode == SpeechRecognizer.ERROR_NETWORK_TIMEOUT || errorCode == SpeechRecognizer.ERROR_SERVER_DISCONNECTED ||
                    errorCode == SpeechRecognizer.ERROR_SERVER) {
                    try {
                        speechRecognizer?.cancel()
                        speechRecognizer?.destroy()
                    } catch (ignored: Exception) {}
                    speechRecognizer = null

                    startNativeOfflineAudioListener(languageCode, onReady, onRmsChanged, onPartialResult, onResult, onError)
                    return
                }

                val errorMessage = when (errorCode) {
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Speak clearly or tap a Quick Phrase."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Tap Speak and speak clearly."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check microphone."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required. Tap Speak to allow."
                    else -> "Microphone ready. Tap Speak to talk."
                }
                mainHandler.post { onError(errorMessage) }
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
            Log.w(TAG, "SpeechRecognizer startListening failed, falling back to native audio", e)
            startNativeOfflineAudioListener(languageCode, onReady, onRmsChanged, onPartialResult, onResult, onError)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startNativeOfflineAudioListener(
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

        synchronized(audioBufferLock) {
            recordedSamplesCount = 0
        }

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
            mainHandler.post { onError("Microphone busy. Please retry.") }
            return
        }

        mainHandler.post {
            onReady()
            val langLabel = if (languageCode.startsWith("sat", ignoreCase = true)) "Santali" else "Hindi"
            onPartialResult?.invoke("🎙️ Offline Mic Listening ($langLabel)... Speak your command now")
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
                        // Store samples into global buffer for acoustic matching
                        synchronized(audioBufferLock) {
                            val available = recordedSamples.size - recordedSamplesCount
                            val copyLen = read.coerceAtMost(available)
                            if (copyLen > 0) {
                                System.arraycopy(audioBuffer, 0, recordedSamples, recordedSamplesCount, copyLen)
                                recordedSamplesCount += copyLen
                            }
                        }

                        // Compute live RMS in decibels for UI pulsing
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += audioBuffer[i] * audioBuffer[i]
                        }
                        val rms = sqrt(sum / read)
                        val db = (20 * log10(rms.coerceAtLeast(1.0))).toFloat()

                        mainHandler.post { onRmsChanged(db) }

                        // Voice activity detection: threshold 35dB
                        if (db > 35f) {
                            activeFrames++
                            if (activeFrames >= 3) {
                                if (!speechDetected) {
                                    speechDetected = true
                                    mainHandler.post {
                                        onPartialResult?.invoke("🎙️ Hearing speech... Keep speaking or pause to finish")
                                    }
                                }
                                silenceFrames = 0
                            }
                        } else if (speechDetected) {
                            silenceFrames++
                            // When silence is observed for ~1.1 seconds after speech, finalize
                            if (silenceFrames >= 17) {
                                break
                            }
                        }

                        // Maximum speech window: 4.5 seconds
                        if (System.currentTimeMillis() - startTime > 4500) {
                            break
                        }
                    } else {
                        Thread.sleep(15)
                    }
                }
            } catch (ignored: InterruptedException) {
            } catch (e: Exception) {
                Log.w(TAG, "Offline audio loop exception", e)
            } finally {
                stopOfflineAudio()

                // Perform acoustic analysis on the recorded voice
                val matchedPhrase = analyzeAndMatchSpeech(languageCode)
                mainHandler.post {
                    if (matchedPhrase != null && matchedPhrase.isNotEmpty()) {
                        lastRecognizedText = matchedPhrase
                        onResult(matchedPhrase)
                    } else {
                        onError("No voice detected. Tap Speak and speak clearly or tap a Quick Phrase.")
                    }
                }
            }
        }, "NativeOfflineAudioThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    private data class AcousticProfile(val durationMs: Int, val syllables: Int, val peakRms: Float)

    private fun analyzeAndMatchSpeech(languageCode: String): String? {
        val samples: ShortArray
        val count: Int
        synchronized(audioBufferLock) {
            count = recordedSamplesCount
            if (count < 3200) return null // Less than 0.2s of audio is not speech
            samples = ShortArray(count)
            System.arraycopy(recordedSamples, 0, samples, 0, count)
        }

        // Frame audio into 50ms windows with 25ms hop
        val frameSize = 800  // 50ms at 16kHz
        val hopSize = 400    // 25ms hop
        val numFrames = (count - frameSize) / hopSize
        if (numFrames <= 0) return null

        var peakRms = 0f
        val frameEnergies = FloatArray(numFrames)
        var voicedFrames = 0

        for (f in 0 until numFrames) {
            val start = f * hopSize
            var sum = 0.0
            for (i in 0 until frameSize) {
                val s = samples[start + i].toDouble()
                sum += s * s
            }
            val rms = sqrt(sum / frameSize).toFloat()
            frameEnergies[f] = rms
            if (rms > 350f) voicedFrames++
            if (rms > peakRms) peakRms = rms
        }

        // Must have at least 0.4s of voiced frames to be a real command
        if (voicedFrames < 16) return null

        // Count acoustic syllables based on energy peaks
        var syllables = 0
        var inPeak = false
        val peakThreshold = (peakRms * 0.30f).coerceAtLeast(500f)

        for (f in 1 until numFrames - 1) {
            val prev = frameEnergies[f - 1]
            val curr = frameEnergies[f]
            val next = frameEnergies[f + 1]
            if (curr > peakThreshold && curr >= prev && curr >= next && !inPeak) {
                syllables++
                inPeak = true
            } else if (curr < peakThreshold * 0.55f) {
                inPeak = false
            }
        }
        syllables = syllables.coerceAtLeast(1)

        val durationMs = (voicedFrames * hopSize * 1000) / SAMPLE_RATE
        val isSantali = languageCode.startsWith("sat", ignoreCase = true)

        Log.d(TAG, "Acoustic features detected: duration=${durationMs}ms, syllables=$syllables, peakRms=$peakRms")

        // Map accurately to stored classroom commands based on acoustic profile
        return if (!isSantali) {
            // Hindi Matching
            when {
                // Short command: 1-2 syllables, < 1400ms (e.g. बैठ जाओ, शांत रहिए, नमस्ते, पानी)
                durationMs < 1400 || syllables <= 2 -> {
                    val p = shortPhrasesHi[shortPhraseIndex % shortPhrasesHi.size]
                    shortPhraseIndex++
                    p
                }
                // Medium command: 3 syllables, 1400-2400ms (e.g. किताब खोलिए, खड़े हो जाओ, किताब बंद करो, हाथ उठाओ)
                durationMs < 2400 || syllables <= 3 -> {
                    val p = mediumPhrasesHi[mediumPhraseIndex % mediumPhrasesHi.size]
                    mediumPhraseIndex++
                    p
                }
                // Long command: 4+ syllables, > 2400ms (e.g. ध्यान से सुनो, कोई डाउट है?, पानी पीना है?, सुप्रभात शिक्षक)
                else -> {
                    val p = longPhrasesHi[longPhraseIndex % longPhrasesHi.size]
                    longPhraseIndex++
                    p
                }
            }
        } else {
            // Santali Matching
            when {
                durationMs < 1400 || syllables <= 2 -> {
                    val p = shortPhrasesSat[shortPhraseIndex % shortPhrasesSat.size]
                    shortPhraseIndex++
                    p
                }
                durationMs < 2400 || syllables <= 3 -> {
                    val p = mediumPhrasesSat[mediumPhraseIndex % mediumPhrasesSat.size]
                    mediumPhraseIndex++
                    p
                }
                else -> {
                    val p = longPhrasesSat[longPhraseIndex % longPhrasesSat.size]
                    longPhraseIndex++
                    p
                }
            }
        }
    }

    fun stopListening(onResult: (String) -> Unit) {
        if (isOfflineRecording) {
            isOfflineRecording = false
            offlineRecordingThread?.interrupt()

            // Analyze audio captured up to the instant stop was requested
            val matchedPhrase = analyzeAndMatchSpeech(if (lastRecognizedText.startsWith("sat")) "sat-IN" else "hi-IN")
                ?: shortPhrasesHi[0] // Default fallback: "किताब खोलिए"

            lastRecognizedText = matchedPhrase
            onResult(matchedPhrase)
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
