package org.moolvani.app.engine.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

class SpeechRecognitionHelper(private val context: Context) {

    companion object {
        private const val TAG = "SpeechRecognitionHelper"
        private const val SAMPLE_RATE = 16000
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var isOfflineRecording = false
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    // Vosk Neural Engine State
    @Volatile
    private var voskModel: Model? = null
    @Volatile
    private var isModelLoading = false
    @Volatile
    private var isModelReady = false

    // Single-dispatch callbacks
    private var hasDispatched = false
    private var activeOnResult: ((String) -> Unit)? = null
    private var activeOnError: ((String) -> Unit)? = null
    private var currentLanguageCode: String = "hi-IN"
    private var lastRecognizedText: String = ""

    // Constrained Classroom Grammar with constituent tokens for single & multi-sentence combinations
    private val classroomGrammarJson = """
        [
            "किताब", "खोलिए", "खोलो", "खोल", "बंद", "करो", "करिए", "रखो",
            "बैठ", "जाओ", "बैठो", "बैठिए", "नीचे", "सिट", "डाउन",
            "खड़े", "हो", "उठो", "खड़ा", "स्टैंड", "अप",
            "शांत", "रहिए", "रहो", "चुप", "आवाज", "मत", "शोर", "बी", "क्वाइट",
            "ध्यान", "से", "सुनो", "सुनिए", "मेरी", "बात", "लिसन",
            "और", "तथा", "फिर", "भी", "सब", "बच्चे", "अपनी", "जगह", "पर", "को",
            "कोई", "डाउट", "है", "सवाल", "प्रश्न", "पूछो", "उत्तर", "दो", "जवाब",
            "पानी", "पीना", "चाहिए", "चाहते", "वाटर",
            "सुप्रभात", "शिक्षक", "नमस्ते", "गुड", "मॉर्निंग", "सर", "मैडम", "प्रणाम",
            "धन्यवाद", "थैंक", "यू", "शुक्रिया",
            "समझ", "में", "आया", "हाँ", "मुझे", "गया", "नहीं", "समझे",
            "लिखना", "शुरू", "लिखो", "पढ़ो", "पढ़ना", "कॉपी", "कलम", "पेन", "पेंसिल",
            "हाथ", "उठाओ", "ऊपर", "यहाँ", "आओ", "वहाँ", "इधर", "उधर", "पास",
            "ब्लैकबोर्ड", "बोर्ड", "देखिए", "देखो", "तरफ", "श्यामपट्ट",
            "एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ", "दस",
            "गाय", "हाथी", "कुत्ता", "बिल्ली", "लाल", "खाना", "स्कूल", "दोस्त", "मित्र", "घर",
            "आप", "कैसे", "हैं", "हो", "तुम", "नाम", "क्या", "मेरा", "बहुत", "अच्छा", "बढ़िया", "शाबाश",
            "[unk]"
        ]
    """.trimIndent().replace("\n", "").replace("  ", "")

    // Fallback Santali acoustic profiles (for student mode)
    private data class SantaliProfile(
        val canonicalText: String,
        val expectedDurationMs: Int,
        val expectedSyllables: Int,
        val hasHighZcr: Boolean
    )

    private val santhaliProfiles = listOf(
        SantaliProfile("ᱛᱤᱸᱜᱩᱱ ᱢᱮ", 1100, 3, false),
        SantaliProfile("ᱫᱩᱲᱩᱵ ᱢᱮ", 1000, 3, false),
        SantaliProfile("ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ", 1600, 4, false),
        SantaliProfile("ᱯᱚᱛᱚᱵ ᱵᱚᱸᱫᱽ ᱢᱮ", 1700, 4, false),
        SantaliProfile("ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱢᱮ", 1400, 3, false),
        SantaliProfile("ᱫᱷᱮᱭᱟᱱ ᱛᱮ ᱟᱧᱡᱚᱢ ᱢᱮ", 2000, 5, false),
        SantaliProfile("ᱥᱟᱨᱦᱟᱣ", 1100, 2, true),
        SantaliProfile("ᱫᱟᱜ", 600, 1, false),
        SantaliProfile("ᱡᱚᱦᱟᱨ ᱢᱟᱪᱮᱛ", 1500, 4, false),
        SantaliProfile("ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟᱢ?", 1700, 4, false),
        SantaliProfile("ᱦᱮᱸ", 500, 1, false),
        SantaliProfile("ᱵᱟᱝ", 500, 1, false)
    )

    init {
        loadVoskModelAsync()
    }

    /**
     * Initializes the Vosk Offline Hindi model in the background.
     * Checks if already extracted to app internal storage; if not, unpacks from APK assets.
     */
    fun loadVoskModelAsync() {
        if (isModelReady || isModelLoading) return
        isModelLoading = true

        thread(name = "VoskModelLoader", priority = Thread.NORM_PRIORITY) {
            try {
                val modelDir = File(context.filesDir, "model-hi")
                val finalMdl = File(modelDir, "am/final.mdl")

                if (!finalMdl.exists() || finalMdl.length() < 1000) {
                    Log.i(TAG, "Unpacking Vosk Hindi model from assets to ${modelDir.absolutePath}...")
                    unpackAssetFolder(context, "model-hi", modelDir)
                }

                if (finalMdl.exists()) {
                    Log.i(TAG, "Loading Vosk Model from: ${modelDir.absolutePath}")
                    val model = Model(modelDir.absolutePath)
                    voskModel = model
                    isModelReady = true
                    Log.i(TAG, "Vosk Hindi Model loaded successfully and ready!")
                } else {
                    Log.w(TAG, "Vosk model final.mdl missing after unpack")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Vosk model", e)
            } finally {
                isModelLoading = false
            }
        }
    }

    private fun unpackAssetFolder(context: Context, assetPath: String, targetDir: File) {
        val assetManager = context.assets
        val items = assetManager.list(assetPath) ?: return

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        for (item in items) {
            val subAsset = "$assetPath/$item"
            val subTarget = File(targetDir, item)
            val subItems = assetManager.list(subAsset)

            if (subItems != null && subItems.isNotEmpty()) {
                subTarget.mkdirs()
                unpackAssetFolder(context, subAsset, subTarget)
            } else {
                try {
                    assetManager.open(subAsset).use { input ->
                        FileOutputStream(subTarget).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error copying asset: $subAsset", e)
                }
            }
        }
    }

    fun isRecognitionAvailable(checkContext: Context? = null): Boolean = true

    /**
     * Actively listens to speech offline:
     * 1. Opens AudioRecord mic with VOICE_RECOGNITION hardware preprocessing & AGC.
     * 2. Digital gain multiplier (2.2x) enables sensitive pickup at 1-2 feet distance.
     * 3. Streams audio directly to Vosk Neural ASR.
     * 4. Dispatches exact recognized Hindi command for translation & audio playback.
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

        val sampleRate = SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = if (minBufferSize > 0) minBufferSize.coerceAtLeast(4096) else 4096

        // Prefer VOICE_RECOGNITION (enables hardware AGC, noise suppression, and speech tuning)
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
        } catch (e: Exception) {
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            } catch (e2: Exception) {
                Log.e(TAG, "AudioRecord instantiation failed", e2)
                dispatchResult(null, "Microphone unavailable. Please grant microphone permission.")
                return
            }
        }

        val record = audioRecord
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record?.release()
            audioRecord = null
            dispatchResult(null, "Could not initialize microphone hardware.")
            return
        }

        try {
            record.startRecording()
        } catch (e: Exception) {
            record.release()
            audioRecord = null
            dispatchResult(null, "Microphone busy. Please retry.")
            return
        }

        mainHandler.post {
            onReady()
            val initMsg = if (!isModelReady && isModelLoading) {
                "⏳ Preparing Vosk model... Listening now"
            } else {
                "🎙️ Listening... Speak your classroom command now"
            }
            onPartialResult?.invoke(initMsg)
        }

        recordingThread = Thread({
            val audioBuffer = ShortArray(1024)
            var recognizer: Recognizer? = null
            val model = voskModel

            if (model != null && languageCode.startsWith("hi")) {
                try {
                    recognizer = Recognizer(model, sampleRate.toFloat(), classroomGrammarJson)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to create grammar recognizer, falling back to general model", e)
                    try {
                        recognizer = Recognizer(model, sampleRate.toFloat())
                    } catch (ignored: Exception) {}
                }
            }

            var speechDetected = false
            var activeFrames = 0
            var silenceFrames = 0
            val startTime = System.currentTimeMillis()
            var recognizedFinal = ""
            var lastPartial = ""

            // Fallback audio samples buffer in case Vosk model is still loading
            val capturedSamples = mutableListOf<Short>()

            try {
                while (isOfflineRecording && !Thread.currentThread().isInterrupted) {
                    val read = record.read(audioBuffer, 0, audioBuffer.size)
                    if (read > 0) {
                        // Software digital gain boost (2.2x) for sensitive pickup at arm's length (1-2 ft)
                        val gain = 2.2f
                        var sum = 0.0
                        for (i in 0 until read) {
                            val boosted = (audioBuffer[i] * gain).toInt().coerceIn(-32768, 32767).toShort()
                            audioBuffer[i] = boosted
                            sum += boosted * boosted
                            if (capturedSamples.size < sampleRate * 7) {
                                capturedSamples.add(boosted)
                            }
                        }
                        val rms = sqrt(sum / read)
                        val db = (20 * log10(rms.coerceAtLeast(1.0))).toFloat()

                        mainHandler.post { onRmsChanged(db) }

                        // Feed to Vosk Neural Recognizer
                        if (recognizer != null) {
                            val accepted = recognizer.acceptWaveForm(audioBuffer, read)
                            if (accepted) {
                                val resJson = recognizer.result
                                val text = parseVoskText(resJson)
                                if (text.isNotBlank() && text != "[unk]") {
                                    recognizedFinal = text
                                    speechDetected = true
                                    silenceFrames = 0
                                    mainHandler.post {
                                        onPartialResult?.invoke("🎙️ Recognized: $text")
                                    }
                                }
                            } else {
                                val partialJson = recognizer.partialResult
                                val partial = parseVoskPartial(partialJson)
                                if (partial.isNotBlank() && partial != "[unk]") {
                                    lastPartial = partial
                                    speechDetected = true
                                    silenceFrames = 0
                                    mainHandler.post {
                                        onPartialResult?.invoke("🎙️ Hearing: $partial")
                                    }
                                }
                            }
                        }

                        // Sensitive voice activity detection (25dB captures normal voice at 1-2 feet)
                        if (db > 25f) {
                            activeFrames++
                            if (activeFrames >= 2) {
                                speechDetected = true
                                silenceFrames = 0
                            }
                        } else if (speechDetected) {
                            silenceFrames++
                            // After speech starts, allow ~1.4s pause between clauses (22 frames * 64ms)
                            if (silenceFrames >= 22) {
                                break
                            }
                        }

                        // Max speech window: 6.5s to comfortably fit combined 2-sentence commands
                        if (System.currentTimeMillis() - startTime > 6500) {
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
                // Get final result from Vosk
                if (recognizer != null) {
                    try {
                        val finalJson = recognizer.finalResult
                        val finalTxt = parseVoskText(finalJson)
                        if (finalTxt.isNotBlank() && finalTxt != "[unk]") {
                            recognizedFinal = finalTxt
                        }
                    } catch (ignored: Exception) {}
                    try {
                        recognizer.close()
                    } catch (ignored: Exception) {}
                }

                fun cleanRecognized(text: String): String {
                    return text.replace("[unk]", "")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                }

                val finalClean = cleanRecognized(recognizedFinal)
                val partialClean = cleanRecognized(lastPartial)

                val finalOutput = when {
                    finalClean.isNotBlank() -> finalClean
                    partialClean.isNotBlank() -> partialClean
                    languageCode.startsWith("sat") -> matchSantaliAudio(capturedSamples.toShortArray())
                    else -> null
                }

                if (!finalOutput.isNullOrBlank()) {
                    dispatchResult(finalOutput, null)
                } else if (speechDetected) {
                    dispatchResult(null, "Could not clearly understand the audio. Please speak again.")
                } else {
                    dispatchResult(null, "No voice detected. Please tap mic and speak.")
                }
            }
        }, "VoskOfflineAudioListenerThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    private fun parseVoskText(jsonStr: String): String {
        return try {
            JSONObject(jsonStr).optString("text", "").trim()
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseVoskPartial(jsonStr: String): String {
        return try {
            JSONObject(jsonStr).optString("partial", "").trim()
        } catch (e: Exception) {
            ""
        }
    }

    private fun matchSantaliAudio(samples: ShortArray): String? {
        val count = samples.size
        if (count < 3200) return null

        val frameSize = 800
        val hopSize = 400
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

        if (peakRms < 100f) return null
        val voicedThreshold = (peakRms * 0.22f).coerceIn(60f, 300f)
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

        if (voicedFrames < 5) return null

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
        val isHighZcr = avgZcr > 0.092

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

    fun stopListening(onResult: (String) -> Unit) {
        if (isOfflineRecording) {
            isOfflineRecording = false
            recordingThread?.interrupt()
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
        try {
            voskModel?.close()
        } catch (ignored: Exception) {}
        voskModel = null
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
