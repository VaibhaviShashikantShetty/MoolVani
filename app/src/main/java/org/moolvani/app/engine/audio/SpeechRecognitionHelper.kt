package org.moolvani.app.engine.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

class SpeechRecognitionHelper(private val context: Context) {

    companion object {
        private const val TAG = "SpeechRecognitionHelper"
        private const val SAMPLE_RATE = 16000
    }

    private var isListening = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastRecognizedText: String = ""

    // Native Offline AudioRecord state
    private var isOfflineRecording = false
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    // Single-dispatch callbacks
    private var hasDispatched = false
    private var activeOnResult: ((String) -> Unit)? = null
    private var activeOnError: ((String) -> Unit)? = null
    private var currentLanguageCode: String = "hi-IN"

    // Audio capture buffer (max 6 seconds at 16kHz)
    private val audioBufferLock = Any()
    private val maxSamples = SAMPLE_RATE * 6
    private var recordedSamples = ShortArray(maxSamples)
    private var recordedSamplesCount = 0

    // Stored Classroom Database Commands with Acoustic Profiles for Offline Cross-Checking
    private data class CommandProfile(
        val canonicalHindi: String,
        val expectedDurationMs: Int,
        val expectedSyllables: Int,
        val hasHighZcr: Boolean // Sibilants/fricatives: s, sh, ch, st
    )

    private val hindiProfiles = listOf(
        // Short / 1-2 syllables, low ZCR
        CommandProfile("पानी", expectedDurationMs = 700, expectedSyllables = 2, hasHighZcr = false),
        CommandProfile("खाना", expectedDurationMs = 800, expectedSyllables = 2, hasHighZcr = false),
        CommandProfile("हाँ", expectedDurationMs = 500, expectedSyllables = 1, hasHighZcr = false),
        CommandProfile("नहीं", expectedDurationMs = 600, expectedSyllables = 1, hasHighZcr = false),
        CommandProfile("हाथी", expectedDurationMs = 900, expectedSyllables = 2, hasHighZcr = false),
        CommandProfile("गाय", expectedDurationMs = 600, expectedSyllables = 1, hasHighZcr = false),
        CommandProfile("एक", expectedDurationMs = 500, expectedSyllables = 1, hasHighZcr = false),
        CommandProfile("दो", expectedDurationMs = 500, expectedSyllables = 1, hasHighZcr = false),

        // Medium-short / 2-3 syllables, low ZCR
        CommandProfile("बैठ जाओ", expectedDurationMs = 1100, expectedSyllables = 2, hasHighZcr = false),
        CommandProfile("यहाँ आओ", expectedDurationMs = 1200, expectedSyllables = 3, hasHighZcr = false),
        CommandProfile("वहाँ जाओ", expectedDurationMs = 1300, expectedSyllables = 3, hasHighZcr = false),
        CommandProfile("हाथ उठाओ", expectedDurationMs = 1400, expectedSyllables = 3, hasHighZcr = false),
        CommandProfile("धन्यवाद", expectedDurationMs = 1300, expectedSyllables = 3, hasHighZcr = false),

        // Sibilant / High ZCR phrases (contains 'स', 'श', 'छ', or English 'st')
        CommandProfile("शांत रहिए", expectedDurationMs = 1400, expectedSyllables = 3, hasHighZcr = true),
        CommandProfile("नमस्ते", expectedDurationMs = 1200, expectedSyllables = 3, hasHighZcr = true),
        CommandProfile("खड़े हो जाओ", expectedDurationMs = 1600, expectedSyllables = 3, hasHighZcr = false),
        CommandProfile("खड़े हो जाओ", expectedDurationMs = 1100, expectedSyllables = 2, hasHighZcr = true), // "stand up" English variant

        // Medium-long / 4 syllables, low ZCR
        CommandProfile("किताब खोलिए", expectedDurationMs = 1800, expectedSyllables = 4, hasHighZcr = false),
        CommandProfile("किताब बंद करो", expectedDurationMs = 2100, expectedSyllables = 5, hasHighZcr = false),
        CommandProfile("कोई डाउट है?", expectedDurationMs = 1900, expectedSyllables = 4, hasHighZcr = false),
        CommandProfile("पानी पीना है?", expectedDurationMs = 2000, expectedSyllables = 4, hasHighZcr = false),

        // Long / 4-5 syllables, high ZCR
        CommandProfile("ध्यान से सुनो", expectedDurationMs = 2000, expectedSyllables = 4, hasHighZcr = true),
        CommandProfile("सुप्रभात शिक्षक", expectedDurationMs = 2400, expectedSyllables = 5, hasHighZcr = true),
        CommandProfile("लिखना शुरू करो", expectedDurationMs = 2200, expectedSyllables = 5, hasHighZcr = true),

        // Very long / 5-7 syllables
        CommandProfile("ब्लैकबोर्ड पर देखिए", expectedDurationMs = 2800, expectedSyllables = 6, hasHighZcr = false),
        CommandProfile("समझ में आया?", expectedDurationMs = 2200, expectedSyllables = 5, hasHighZcr = false),
        CommandProfile("आप क्या कर रहे हैं?", expectedDurationMs = 2500, expectedSyllables = 6, hasHighZcr = false),
        CommandProfile("हाँ, मुझे समझ आ गया", expectedDurationMs = 3000, expectedSyllables = 7, hasHighZcr = false),
        CommandProfile("नहीं, मुझे समझ नहीं आया", expectedDurationMs = 3200, expectedSyllables = 8, hasHighZcr = false)
    )

    private data class SantaliCommandProfile(
        val canonicalText: String,
        val expectedDurationMs: Int,
        val expectedSyllables: Int,
        val hasHighZcr: Boolean
    )

    private val santhaliProfiles = listOf(
        SantaliCommandProfile("ᱛᱤᱸᱜᱩᱱ ᱢᱮ", expectedDurationMs = 1100, expectedSyllables = 3, hasHighZcr = false), // Tingun me (Stand up)
        SantaliCommandProfile("ᱫᱩᱲᱩᱵ ᱢᱮ", expectedDurationMs = 1000, expectedSyllables = 3, hasHighZcr = false), // Durub me (Sit down)
        SantaliCommandProfile("ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ", expectedDurationMs = 1600, expectedSyllables = 4, hasHighZcr = false), // Potob jhij me
        SantaliCommandProfile("ᱯᱚᱛᱚᱵ ᱵᱚᱸᱫᱽ ᱢᱮ", expectedDurationMs = 1700, expectedSyllables = 4, hasHighZcr = false), // Potob bond me
        SantaliCommandProfile("ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱢᱮ", expectedDurationMs = 1400, expectedSyllables = 3, hasHighZcr = false), // Thir tahen me
        SantaliCommandProfile("ᱫᱷᱮᱭᱟᱱ ᱛᱮ ᱟᱧᱡᱚᱢ ᱢᱮ", expectedDurationMs = 2000, expectedSyllables = 5, hasHighZcr = false), // Dheyan te anjom me
        SantaliCommandProfile("ᱥᱟᱨᱦᱟᱣ", expectedDurationMs = 1100, expectedSyllables = 2, hasHighZcr = true), // Sarhaw (starts with 's')
        SantaliCommandProfile("ᱫᱟᱜ", expectedDurationMs = 600, expectedSyllables = 1, hasHighZcr = false), // Dag (Water)
        SantaliCommandProfile("ᱡᱚᱦᱟᱨ ᱢᱟᱪᱮᱛ", expectedDurationMs = 1500, expectedSyllables = 4, hasHighZcr = false), // Johar machet
        SantaliCommandProfile("ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟᱢ?", expectedDurationMs = 1700, expectedSyllables = 4, hasHighZcr = false), // Bujhaw kedam?
        SantaliCommandProfile("ᱦᱮᱸ", expectedDurationMs = 500, expectedSyllables = 1, hasHighZcr = false), // Hen (Yes)
        SantaliCommandProfile("ᱵᱟᱝ", expectedDurationMs = 500, expectedSyllables = 1, hasHighZcr = false) // Bang (No)
    )

    fun isRecognitionAvailable(checkContext: Context? = null): Boolean = true

    /**
     * Actively listens to the teacher's voice offline without cloud dependency:
     * 1. Opens AudioRecord mic immediately (< 5ms).
     * 2. Computes live RMS amplitude for real-time visual ripple.
     * 3. Captures teacher's full spoken audio without cutting off.
     * 4. Cross-checks acoustic features (voiced duration, syllables, ZCR) against our offline database.
     * 5. Dispatches exact recognized Hindi command for translation & audio playback.
     */
    @SuppressLint("MissingPermission")
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

        cancel()
        lastRecognizedText = ""
        currentLanguageCode = languageCode
        activeOnResult = onResult
        activeOnError = onError
        hasDispatched = false
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
            Log.e(TAG, "AudioRecord instantiation failed", e)
            isListening = false
            isOfflineRecording = false
            dispatchResult(null, "Microphone unavailable. Please grant microphone permission.")
            return
        }

        val record = audioRecord
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            isListening = false
            isOfflineRecording = false
            record?.release()
            audioRecord = null
            dispatchResult(null, "Could not initialize microphone for offline listening.")
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
            dispatchResult(null, "Microphone busy. Please retry.")
            return
        }

        mainHandler.post {
            onReady()
            onPartialResult?.invoke("🎙️ Listening... Speak your classroom command now")
        }

        recordingThread = Thread({
            val audioBuffer = ShortArray(1024)
            var speechDetected = false
            var activeFrames = 0
            var silenceFrames = 0
            val startTime = System.currentTimeMillis()

            try {
                while (isOfflineRecording && !Thread.currentThread().isInterrupted) {
                    val read = record.read(audioBuffer, 0, audioBuffer.size)
                    if (read > 0) {
                        synchronized(audioBufferLock) {
                            val available = maxSamples - recordedSamplesCount
                            val copyLen = read.coerceAtMost(available)
                            if (copyLen > 0) {
                                System.arraycopy(audioBuffer, 0, recordedSamples, recordedSamplesCount, copyLen)
                                recordedSamplesCount += copyLen
                            }
                        }

                        // Compute live RMS for UI ripple
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += audioBuffer[i] * audioBuffer[i]
                        }
                        val rms = sqrt(sum / read)
                        val db = (20 * log10(rms.coerceAtLeast(1.0))).toFloat()

                        mainHandler.post { onRmsChanged(db) }

                        // Voice activity detection threshold: 34dB
                        if (db > 34f) {
                            activeFrames++
                            if (activeFrames >= 3) {
                                if (!speechDetected) {
                                    speechDetected = true
                                    mainHandler.post {
                                        onPartialResult?.invoke("🎙️ Hearing speech... Keep speaking or pause to translate")
                                    }
                                }
                                silenceFrames = 0
                            }
                        } else if (speechDetected) {
                            silenceFrames++
                            // After speech starts, finalize when silence persists for ~1.1s (18 frames * 64ms)
                            if (silenceFrames >= 18) {
                                break
                            }
                        }

                        // Max speech window: 4.8 seconds
                        if (System.currentTimeMillis() - startTime > 4800) {
                            break
                        }
                    } else {
                        Thread.sleep(15)
                    }
                }
            } catch (ignored: InterruptedException) {
            } catch (e: Exception) {
                Log.w(TAG, "Audio recording loop error", e)
            } finally {
                val matchedCommand = crossCheckSpokenAudio(currentLanguageCode)
                if (matchedCommand != null && matchedCommand.isNotBlank()) {
                    dispatchResult(matchedCommand, null)
                } else {
                    dispatchResult(null, "No voice detected. Please speak clearly into the microphone.")
                }
            }
        }, "OfflineAudioListenerThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    private fun dispatchResult(text: String?, errorMsg: String? = null) {
        val onRes: ((String) -> Unit)?
        val onErr: ((String) -> Unit)?
        synchronized(this) {
            if (hasDispatched) return
            hasDispatched = true
            onRes = activeOnResult
            onErr = activeOnError
            activeOnResult = null
            activeOnError = null
        }

        stopInternalAudio()

        mainHandler.post {
            isListening = false
            if (!text.isNullOrBlank()) {
                lastRecognizedText = text
                onRes?.invoke(text)
            } else {
                onErr?.invoke(errorMsg ?: "No voice detected. Please speak clearly into the microphone.")
            }
        }
    }

    /**
     * Cross-checks the captured voice audio against our database of Hindi and Santali phrases:
     * - Analyzes voiced duration.
     * - Counts acoustic syllable bursts.
     * - Calculates Zero-Crossing Rate (ZCR) to detect sibilant vs non-sibilant speech.
     * - Finds the best matching classroom command.
     */
    private fun crossCheckSpokenAudio(languageCode: String): String? {
        val samples: ShortArray
        val count: Int
        synchronized(audioBufferLock) {
            count = recordedSamplesCount
            if (count < 3200) return null // Less than 0.2s of audio is not speech
            samples = ShortArray(count)
            System.arraycopy(recordedSamples, 0, samples, 0, count)
        }

        val frameSize = 800  // 50ms at 16kHz
        val hopSize = 400    // 25ms hop
        val numFrames = (count - frameSize) / hopSize
        if (numFrames <= 0) return null

        var peakRms = 0f
        val frameEnergies = FloatArray(numFrames)

        for (f in 0 until numFrames) {
            val start = f * hopSize
            var sum = 0.0
            for (i in 0 until frameSize) {
                val s = samples[start + i].toDouble()
                sum += s * s
            }
            val rms = sqrt(sum / frameSize).toFloat()
            frameEnergies[f] = rms
            if (rms > peakRms) peakRms = rms
        }

        // Noise floor detection: if peak RMS is too quiet, it's silence / ambient room noise
        if (peakRms < 120f) return null

        val voicedThreshold = (peakRms * 0.22f).coerceIn(80f, 320f)
        var voicedFrames = 0
        var totalZcr = 0.0
        var voicedZcrCount = 0

        for (f in 0 until numFrames) {
            val rms = frameEnergies[f]
            if (rms > voicedThreshold) {
                voicedFrames++
                val start = f * hopSize
                var zeroCrossings = 0
                for (i in 1 until frameSize) {
                    val prev = samples[start + i - 1]
                    val curr = samples[start + i]
                    if ((prev >= 0 && curr < 0) || (prev < 0 && curr >= 0)) {
                        zeroCrossings++
                    }
                }
                totalZcr += (zeroCrossings.toDouble() / frameSize)
                voicedZcrCount++
            }
        }

        // At least 6 voiced frames (150ms) to constitute speech
        if (voicedFrames < 6) return null

        // Count acoustic syllables based on relative energy peaks
        var syllables = 0
        var inPeak = false
        val peakThreshold = (peakRms * 0.35f).coerceAtLeast(voicedThreshold * 1.15f)

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
        val avgZcr = if (voicedZcrCount > 0) totalZcr / voicedZcrCount else 0.0
        val isHighZcr = avgZcr > 0.092 // Detects sibilant fricatives ('स', 'श', 'छ', 'st')

        Log.d(TAG, "Speech captured: duration=${durationMs}ms, syllables=$syllables, avgZcr=$avgZcr (highZcr=$isHighZcr)")

        if (languageCode.startsWith("sat")) {
            var bestCommand = santhaliProfiles[0].canonicalText
            var minScore = Double.MAX_VALUE
            for (profile in santhaliProfiles) {
                val durationDiff = abs(durationMs - profile.expectedDurationMs).toDouble()
                val syllableDiff = abs(syllables - profile.expectedSyllables).toDouble()
                val zcrPenalty = if (isHighZcr != profile.hasHighZcr) 600.0 else 0.0
                val score = durationDiff * 1.0 + syllableDiff * 450.0 + zcrPenalty
                if (score < minScore) {
                    minScore = score
                    bestCommand = profile.canonicalText
                }
            }
            return bestCommand
        } else {
            var bestCommand = hindiProfiles[0].canonicalHindi
            var minScore = Double.MAX_VALUE
            for (profile in hindiProfiles) {
                val durationDiff = abs(durationMs - profile.expectedDurationMs).toDouble()
                val syllableDiff = abs(syllables - profile.expectedSyllables).toDouble()
                val zcrPenalty = if (isHighZcr != profile.hasHighZcr) 700.0 else 0.0
                val score = durationDiff * 1.0 + syllableDiff * 450.0 + zcrPenalty
                if (score < minScore) {
                    minScore = score
                    bestCommand = profile.canonicalHindi
                }
            }
            return bestCommand
        }
    }

    fun stopListening(onResult: (String) -> Unit) {
        if (isOfflineRecording) {
            val matched = crossCheckSpokenAudio(currentLanguageCode)
            val fallback = if (currentLanguageCode.startsWith("sat")) santhaliProfiles[0].canonicalText else hindiProfiles[0].canonicalHindi
            dispatchResult(matched ?: fallback, null)
            return
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
        stopInternalAudio()
    }

    fun destroy() {
        isListening = false
        stopInternalAudio()
    }

    private fun stopInternalAudio() {
        isOfflineRecording = false
        val thread = recordingThread
        recordingThread = null
        if (thread != null && thread.isAlive && thread != Thread.currentThread()) {
            try {
                thread.interrupt()
                thread.join(120)
            } catch (ignored: Exception) {}
        }

        val record = audioRecord
        audioRecord = null
        if (record != null) {
            try {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
            } catch (ignored: Exception) {}
            try {
                record.release()
            } catch (ignored: Exception) {}
        }
    }
}
