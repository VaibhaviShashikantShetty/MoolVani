package org.moolvani.app.ui.translator

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.moolvani.app.MoolVaniApplication
import org.moolvani.app.R
import org.moolvani.app.data.model.Language
import org.moolvani.app.data.model.Phrase
import org.moolvani.app.data.model.TranslationResult
import org.moolvani.app.databinding.FragmentTranslatorBinding

class TranslatorFragment : Fragment() {

    private var _binding: FragmentTranslatorBinding? = null
    private val binding get() = _binding!!

    private var currentSourceLang = Language.HINDI
    private var currentTargetLang = Language.SANTHALI
    private var lastResult: TranslationResult? = null
    private var pulseAnimator: ObjectAnimator? = null

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechListening()
        } else {
            binding.tvMicStatus.text = "Microphone access denied. Tap Speak to try again."
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTranslatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLanguageToggle()
        setupActionButtons()
        setupQuickChips()
        updateLanguageLabels()

        // Ready state without auto-playing audio on launch
        binding.tvResultOlchiki.text = "MoolVani Translator"
        binding.tvResultRoman.text = "Tap 'Speak' to speak, or tap a quick phrase below"
        binding.tvResultMeaning.text = "Offline Real-Time Hindi ⇄ Santali Pedagogy"
        binding.tvMicStatus.text = "Ready • Tap Speak to start recording"
        binding.tvPlaybackStatus.text = "Acoustic audio engine ready"
    }

    private fun setupLanguageToggle() {
        binding.btnSwapLang.setOnClickListener {
            val temp = currentSourceLang
            currentSourceLang = currentTargetLang
            currentTargetLang = temp
            updateLanguageLabels()
            setupQuickChips()

            // Translate existing text if user has entered any
            val text = binding.etSourceInput.text.toString().trim()
            if (text.isNotEmpty()) {
                performTranslation(text)
            }
        }
    }

    private fun updateLanguageLabels() {
        if (currentSourceLang == Language.HINDI) {
            binding.tvSourceLang.text = "Hindi (हिन्दी)"
            binding.tvTargetLang.text = "Santali (ᱚᱞ ᱪᱤᱠᱤ)"
            binding.etSourceInput.hint = "यहाँ बोलिए या लिखिए... (e.g. किताब खोलिए)"
            binding.tvResultHeader.text = "TRANSLATION RESULT (ᱥᱟᱱᱛᱟᱲᱤ ᱛᱚᱨᱡᱚᱢᱟ):"
        } else {
            binding.tvSourceLang.text = "Santali (ᱚᱞ ᱪᱤᱠᱤ)"
            binding.tvTargetLang.text = "Hindi (हिन्दी)"
            binding.etSourceInput.hint = "ᱚᱞ ᱢᱮ ᱥᱮ ᱨᱚᱲ ᱢᱮ... (e.g. ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ / Potob jhij me)"
            binding.tvResultHeader.text = "TRANSLATION RESULT (हिन्दी अनुवाद):"
        }
    }

    private fun setupActionButtons() {
        val app = MoolVaniApplication.instance

        // 1. Speak Button & Center Mic
        val speakClickListener = View.OnClickListener {
            if (app.speechHelper.isCurrentlyListening()) {
                // If user taps while listening, stop listening and translate whatever was captured
                binding.tvMicStatus.text = "Finalizing speech recording..."
                stopMicPulseAnimation()
                app.speechHelper.stopListening { recognizedText ->
                    if (recognizedText.isNotEmpty()) {
                        binding.etSourceInput.setText(recognizedText)
                        performTranslation(recognizedText)
                    }
                }
            } else {
                startSpeechListening()
            }
        }

        binding.btnSpeak.setOnClickListener(speakClickListener)
        binding.ivCenterMic.setOnClickListener(speakClickListener)

        // 2. Translate Button
        binding.btnTranslate.setOnClickListener {
            if (app.speechHelper.isCurrentlyListening()) {
                binding.tvMicStatus.text = "Stopping mic & translating..."
                stopMicPulseAnimation()
                app.speechHelper.stopListening { recognizedText ->
                    if (recognizedText.isNotEmpty()) {
                        binding.etSourceInput.setText(recognizedText)
                        performTranslation(recognizedText)
                    }
                }
                return@setOnClickListener
            }

            val input = binding.etSourceInput.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(requireContext(), "Please speak or type a sentence first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performTranslation(input)
        }

        // 3. Repeat Button
        binding.btnRepeat.setOnClickListener {
            val success = app.audioEngine.repeatLast {
                activity?.runOnUiThread {
                    binding.tvPlaybackStatus.text = "Playback finished"
                }
            }
            if (success) {
                binding.tvPlaybackStatus.text = "Replaying translation audio..."
            } else {
                Toast.makeText(requireContext(), "No previous audio to repeat", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Store Button
        binding.btnStore.setOnClickListener {
            val result = lastResult
            if (result == null || result.sourceText.isEmpty()) {
                Toast.makeText(requireContext(), "No translation to store", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val phrase = if (result.sourceLang == Language.HINDI) {
                Phrase(
                    hindiText = result.sourceText,
                    santhaliOlchiki = result.targetOlChiki,
                    santhaliRoman = result.targetRoman,
                    englishMeaning = result.matchedCategory,
                    category = "Classroom Commands",
                    isCustom = true
                )
            } else {
                Phrase(
                    hindiText = result.targetDevanagari.ifEmpty { result.targetRoman },
                    santhaliOlchiki = result.sourceText,
                    santhaliRoman = result.targetRoman,
                    englishMeaning = result.matchedCategory,
                    category = "Classroom Commands",
                    isCustom = true
                )
            }

            app.repository.insertPhrase(phrase)
            app.translationEngine.syncFromDatabase()
            Toast.makeText(requireContext(), "Saved to Common Phrases! ⭐", Toast.LENGTH_SHORT).show()
        }

        // Play result audio button inside card
        binding.btnPlayResultAudio.setOnClickListener {
            lastResult?.let { res ->
                playTranslationAudio(res)
            }
        }

        // Clear input button
        binding.btnClearInput.setOnClickListener {
            binding.etSourceInput.setText("")
        }
    }

    private fun startSpeechListening() {
        val app = MoolVaniApplication.instance

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            binding.tvMicStatus.text = "Microphone permission required. Tap Allow to speak."
            return
        }

        startMicPulseAnimation()
        val roleDesc = if (currentSourceLang == Language.HINDI) "Teacher (Hindi)" else "Student (Santali)"
        binding.tvMicStatus.text = "🎙️ Listening... Speak now ($roleDesc)"

        app.speechHelper.startListening(
            callingContext = requireContext(),
            languageCode = if (currentSourceLang == Language.HINDI) "hi-IN" else "sat-IN",
            onReady = {
                binding.tvMicStatus.text = "🎙️ Listening... Speak now ($roleDesc)"
            },
            onRmsChanged = { rms ->
                val scale = (1.0f + (rms / 25.0f).coerceIn(0f, 0.45f))
                binding.micPulseRing.scaleX = scale
                binding.micPulseRing.scaleY = scale
            },
            onPartialResult = { partial ->
                binding.tvMicStatus.text = partial
                if (!partial.startsWith("🎙️")) {
                    binding.etSourceInput.setText(partial)
                }
            },
            onResult = { recognizedText ->
                stopMicPulseAnimation()
                binding.tvMicStatus.text = "Speech recognized! Translating..."
                binding.etSourceInput.setText(recognizedText)
                performTranslation(recognizedText)
            },
            onError = { error ->
                stopMicPulseAnimation()
                binding.tvMicStatus.text = error
            }
        )
    }

    private fun performTranslation(input: String) {
        val app = MoolVaniApplication.instance
        val result = app.translationEngine.translate(input, currentSourceLang, currentTargetLang)
        lastResult = result

        // Update UI according to target language
        if (result.targetLang == Language.SANTHALI) {
            binding.tvResultHeader.text = "TRANSLATION RESULT (ᱥᱟᱱᱛᱟᱲᱤ ᱛᱚᱨᱡᱚᱢᱟ):"
            binding.tvResultOlchiki.text = result.targetOlChiki
            binding.tvResultRoman.text = "Pronunciation: ${result.targetRoman}"
            binding.tvResultMeaning.text = "Source: ${result.sourceText} (${result.matchedCategory})"
        } else {
            binding.tvResultHeader.text = "TRANSLATION RESULT (हिन्दी अनुवाद):"
            binding.tvResultOlchiki.text = result.targetDevanagari.ifEmpty { result.targetRoman }
            binding.tvResultRoman.text = "Original: ${result.sourceText} • (Pronunciation: ${result.targetRoman})"
            binding.tvResultMeaning.text = "Category: ${result.matchedCategory}"
        }

        binding.tvLatencyBadge.text = "⚡ ${result.latencyMs}ms • 100% Offline"
        binding.tvMicStatus.text = "Translation ready! Playing audio..."

        // Play translated audio cleanly on both teacher's and student's end
        playTranslationAudio(result)
    }

    private fun playTranslationAudio(result: TranslationResult) {
        val app = MoolVaniApplication.instance
        val langLabel = if (result.targetLang == Language.SANTHALI) "Santali" else "Hindi"
        binding.tvPlaybackStatus.text = "Playing $langLabel voice 🔊"
        app.audioEngine.playTranslation(result) {
            activity?.runOnUiThread {
                binding.tvPlaybackStatus.text = "Playback complete"
            }
        }
    }

    private data class QuickChipItem(val displayText: String, val phraseText: String)

    private fun setupQuickChips() {
        val container = binding.quickChipsContainer
        container.removeAllViews()

        val samplePhrases = if (currentSourceLang == Language.HINDI) {
            listOf(
                QuickChipItem("📖 किताब खोलिए (Open book)", "किताब खोलिए"),
                QuickChipItem("🪑 बैठ जाओ (Sit down)", "बैठ जाओ"),
                QuickChipItem("🧍 खड़े हो जाओ (Stand up)", "खड़े हो जाओ"),
                QuickChipItem("📕 किताब बंद करो (Close book)", "किताब बंद करो"),
                QuickChipItem("👂 ध्यान से सुनो (Listen)", "ध्यान से सुनो"),
                QuickChipItem("❓ कोई डाउट है? (Any doubts?)", "कोई डाउट है?"),
                QuickChipItem("🥛 पानी पीना है? (Want water?)", "पानी पीना है?"),
                QuickChipItem("✍️ लिखना शुरू करो (Write)", "लिखना शुरू करो"),
                QuickChipItem("🙋 हाथ उठाओ (Raise hand)", "हाथ उठाओ"),
                QuickChipItem("🤫 शांत रहिए (Be quiet)", "शांत रहिए"),
                QuickChipItem("🙏 सुप्रभात शिक्षक (Good morning)", "सुप्रभात शिक्षक"),
                QuickChipItem("💐 धन्यवाद (Thank you)", "धन्यवाद"),
                QuickChipItem("💡 समझ में आया? (Understood?)", "समझ में आया?"),
                QuickChipItem("✅ समझ आ गया (Understood)", "हाँ, मुझे समझ आ गया"),
                QuickChipItem("💧 पानी (Water)", "पानी"),
                QuickChipItem("🍛 खाना (Food)", "खाना"),
                QuickChipItem("🏫 स्कूल (School)", "स्कूल"),
                QuickChipItem("🐘 हाथी (Elephant)", "हाथी"),
                QuickChipItem("🐄 गाय (Cow)", "गाय"),
                QuickChipItem("🔴 लाल (Red)", "लाल"),
                QuickChipItem("1️⃣ एक (One)", "एक (1)"),
                QuickChipItem("2️⃣ दो (Two)", "दो (2)")
            )
        } else {
            listOf(
                QuickChipItem("📖 ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ (Potob jhij me)", "ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ"),
                QuickChipItem("🪑 ᱫᱩᱲᱩᱵ ᱢᱮ (Durub me)", "ᱫᱩᱲᱩᱵ ᱢᱮ"),
                QuickChipItem("🧍 ᱛᱤᱸᱜᱩᱱ ᱢᱮ (Tingun me)", "ᱛᱤᱸᱜᱩᱱ ᱢᱮ"),
                QuickChipItem("📕 ᱯᱚᱛᱚᱵ ᱵᱚᱸᱫᱽ ᱢᱮ (Potob bond me)", "ᱯᱚᱛᱚᱵ ᱵᱚᱸᱫᱽ ᱢᱮ"),
                QuickChipItem("👂 ᱫᱷᱮᱭᱟᱱ ᱛᱮ ᱟᱧᱡᱚᱢ ᱢᱮ", "ᱫᱷᱮᱭᱟᱱ ᱛᱮ ᱟᱧᱡᱚᱢ ᱢᱮ"),
                QuickChipItem("❓ ᱡᱟᱦᱟᱸᱱᱟᱜ ᱠᱩᱠᱞᱤ ᱢᱮᱱᱟᱜᱼᱟ?", "ᱡᱟᱦᱟᱸᱱᱟᱜ ᱠᱩᱠᱞᱤ ᱢᱮᱱᱟᱜᱼᱟ?"),
                QuickChipItem("🥛 ᱫᱟᱜ ᱧᱩ ᱥᱟᱱᱟᱭᱮᱫ ᱢᱮᱭᱟ?", "ᱫᱟᱜ ᱧᱩ ᱥᱟᱱᱟᱭᱮᱫ ᱢᱮᱭᱟ?"),
                QuickChipItem("✍️ ᱚᱞ ᱮᱦᱚᱵ ᱢᱮ (Ol ehob me)", "ᱚᱞ ᱮᱦᱚᱵ ᱢᱮ"),
                QuickChipItem("🙋 ᱛᱤ ᱛᱩᱞ ᱢᱮ (Ti tul me)", "ᱛᱤ ᱛᱩᱞ ᱢᱮ"),
                QuickChipItem("🤫 ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱢᱮ (Thir tahen me)", "ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱢᱮ"),
                QuickChipItem("🙏 ᱡᱚᱦᱟᱨ ᱢᱟᱪᱮᱛ (Johar Machet)", "ᱡᱚᱦᱟᱨ ᱢᱟᱪᱮᱛ"),
                QuickChipItem("💐 ᱥᱟᱨᱦᱟᱣ (Sarhaw)", "ᱥᱟᱨᱦᱟᱣ"),
                QuickChipItem("💡 ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟᱢ? (Bujhaw kedam?)", "ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟᱢ?"),
                QuickChipItem("✅ ᱦᱮᱸ, ᱤᱧ ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟ", "ᱦᱮᱸ, ᱤᱧ ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟ"),
                QuickChipItem("💧 ᱫᱟᱜ (Water)", "ᱫᱟᱜ"),
                QuickChipItem("🍛 ᱡᱚᱢᱟᱜ (Food)", "ᱡᱚᱢᱟᱜ"),
                QuickChipItem("🏫 ᱟᱥᱲᱟ (School)", "ᱟᱥᱲᱟ"),
                QuickChipItem("🐘 ᱦᱟᱹᱛᱤ (Elephant)", "ᱦᱟᱹᱛᱤ"),
                QuickChipItem("🐄 ᱜᱟᱹᱭ (Cow)", "ᱜᱟᱹᱭ"),
                QuickChipItem("🔴 ᱟᱨᱟᱜ (Red)", "ᱟᱨᱟᱜ"),
                QuickChipItem("1️⃣ ᱑ - ᱢᱤᱫ (One)", "᱑ - ᱢᱤᱫ"),
                QuickChipItem("2️⃣ ᱒ - ᱵᱟᱨ (Two)", "᱒ - ᱵᱟᱨ")
            )
        }

        for (item in samplePhrases) {
            val chip = Button(requireContext(), null, 0, androidx.appcompat.R.style.Widget_AppCompat_Button_Borderless).apply {
                text = item.displayText
                textSize = 12f
                setTextColor(resources.getColor(R.color.navy_primary, null))
                setBackgroundResource(R.drawable.bg_pill_outline)
                setPadding(24, 0, 24, 0)
                val params = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    resources.getDimensionPixelSize(androidx.appcompat.R.dimen.abc_action_button_min_height_material)
                ).apply {
                    setMargins(8, 4, 8, 4)
                }
                layoutParams = params
                setOnClickListener {
                    binding.etSourceInput.setText(item.phraseText)
                    performTranslation(item.phraseText)
                }
            }
            container.addView(chip)
        }
    }

    private fun startMicPulseAnimation() {
        pulseAnimator?.cancel()
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.35f, 1.0f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.35f, 1.0f)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.4f, 0.9f, 0.4f)
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(binding.micPulseRing, scaleX, scaleY, alpha).apply {
            duration = 900
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun stopMicPulseAnimation() {
        pulseAnimator?.cancel()
        binding.micPulseRing.scaleX = 1.0f
        binding.micPulseRing.scaleY = 1.0f
        binding.micPulseRing.alpha = 0.3f
    }

    override fun onDestroyView() {
        stopMicPulseAnimation()
        try {
            MoolVaniApplication.instance.speechHelper.cancel()
        } catch (ignored: Exception) {}
        super.onDestroyView()
        _binding = null
    }
}
