package org.moolvani.app

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.moolvani.app.data.model.Language
import org.moolvani.app.engine.MoolvaniTranslationEngine
import org.moolvani.app.engine.OfflineNlpModel

class TranslationEngineTest {

    private lateinit var engine: MoolvaniTranslationEngine

    @Before
    fun setUp() {
        engine = MoolvaniTranslationEngine()
    }

    @Test
    fun testClassroomPhraseTranslation() {
        val result = engine.translate("किताब खोलिए", Language.HINDI, Language.SANTHALI)
        assertEquals("ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ", result.targetOlChiki)
        assertEquals("Potob jhij me", result.targetRoman)
        assertTrue("Latency must be sub-3-seconds (was ${result.latencyMs}ms)", result.latencyMs < 3000)

        // Spoken variants
        val variant1 = engine.translate("किताब खोलो", Language.HINDI, Language.SANTHALI)
        assertEquals("ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ", variant1.targetOlChiki)
        assertEquals("Potob jhij me", variant1.targetRoman)

        val sitDown = engine.translate("बैठ जाओ", Language.HINDI, Language.SANTHALI)
        assertEquals("ᱫᱩᱲᱩᱵ ᱢᱮ", sitDown.targetOlChiki)
        assertEquals("Durub me", sitDown.targetRoman)

        val quiet = engine.translate("शांत रहिए", Language.HINDI, Language.SANTHALI)
        assertEquals("ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱢᱮ", quiet.targetOlChiki)
        assertEquals("Thir tahen me", quiet.targetRoman)

        val waterQuestion = engine.translate("पानी पीना है?", Language.HINDI, Language.SANTHALI)
        assertEquals("ᱫᱟᱜ ᱧᱩ ᱥᱟᱱᱟᱭᱮᱫ ᱢᱮᱭᱟ?", waterQuestion.targetOlChiki)
    }

    @Test
    fun testBidirectionalTranslation() {
        val satToHi = engine.translate("ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ", Language.SANTHALI, Language.HINDI)
        assertEquals("किताब खोलिए", satToHi.targetDevanagari)

        val romanToHi = engine.translate("potob jhij me", Language.SANTHALI, Language.HINDI)
        assertEquals("किताब खोलिए", romanToHi.targetDevanagari)

        val joharToHi = engine.translate("johar machet", Language.SANTHALI, Language.HINDI)
        assertEquals("सुप्रभात शिक्षक", joharToHi.targetDevanagari)

        val dagToHi = engine.translate("dag", Language.SANTHALI, Language.HINDI)
        assertEquals("पानी", dagToHi.targetDevanagari)

        val yesUnderstood = engine.translate("ᱦᱮᱸ, ᱤᱧ ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟ", Language.SANTHALI, Language.HINDI)
        assertEquals("हाँ, मुझे समझ आ गया", yesUnderstood.targetDevanagari)

        val noUnderstood = engine.translate("ᱵᱟᱝ, ᱤᱧ ᱵᱟᱹᱧ ᱵᱩᱡᱷᱟᱹᱣ ᱞᱮᱫᱟ", Language.SANTHALI, Language.HINDI)
        assertEquals("नहीं, मुझे समझ नहीं आया", noUnderstood.targetDevanagari)
    }

    @Test
    fun testVocabularyLookup() {
        val elephant = engine.translate("हाथी", Language.HINDI, Language.SANTHALI)
        assertEquals("ᱦᱟᱹᱛᱤ", elephant.targetOlChiki)
        assertEquals("Hati", elephant.targetRoman)

        val red = engine.translate("लाल", Language.HINDI, Language.SANTHALI)
        assertEquals("ᱟᱨᱟᱜ", red.targetOlChiki)
        assertEquals("Arag", red.targetRoman)
    }

    @Test
    fun testPluggableNlpModel() {
        // Test custom offline model slot
        val mockModel = object : OfflineNlpModel {
            override val modelName = "MockTransformerOffline"
            override val version = "1.0"
            override val isLoaded = true
            override fun infer(input: String, fromCode: String, toCode: String): String? {
                return if (input == "कक्षा") "ᱚᱲᱟᱜ" else null
            }
        }

        engine.nlpModel = mockModel
        val result = engine.translate("कक्षा", Language.HINDI, Language.SANTHALI)
        assertEquals("ᱚᱲᱟᱜ", result.targetOlChiki)
        assertTrue(result.matchedCategory.contains("AI Model"))
    }

    @Test
    fun testEnglishOfflineTranslation() {
        val openBook = engine.translate("open your book", Language.HINDI, Language.SANTHALI)
        assertEquals("ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ", openBook.targetOlChiki)
        assertEquals("Potob jhij me", openBook.targetRoman)

        val sitDown = engine.translate("sit down", Language.HINDI, Language.SANTHALI)
        assertEquals("ᱫᱩᱲᱩᱵ ᱢᱮ", sitDown.targetOlChiki)
        assertEquals("Durub me", sitDown.targetRoman)

        val water = engine.translate("water", Language.HINDI, Language.SANTHALI)
        assertEquals("ᱫᱟᱜ", water.targetOlChiki)
        assertEquals("Dag", water.targetRoman)

        val elephant = engine.translate("elephant", Language.HINDI, Language.SANTHALI)
        assertEquals("ᱦᱟᱹᱛᱤ", elephant.targetOlChiki)
        assertEquals("Hati", elephant.targetRoman)

        val thankYou = engine.translate("thank you", Language.HINDI, Language.SANTHALI)
        assertEquals("ᱥᱟᱨᱦᱟᱣ", thankYou.targetOlChiki)
        assertEquals("Sarhaw", thankYou.targetRoman)
    }

    @Test
    fun testCombinedSentencesTranslation() {
        val sitAndOpen = engine.translate("बैठ जाओ और किताब खोलो", Language.HINDI, Language.SANTHALI)
        assertEquals("ᱫᱩᱲᱩᱵ ᱢᱮ ᱟᱨ ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ", sitAndOpen.targetOlChiki)
        assertEquals("Durub me ar Potob jhij me", sitAndOpen.targetRoman)

        val quietAndListen = engine.translate("शांत रहो और ध्यान से सुनो", Language.HINDI, Language.SANTHALI)
        assertEquals("ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱢᱮ ᱟᱨ ᱫᱷᱮᱭᱟᱱ ᱛᱮ ᱟᱧᱡᱚᱢ ᱢᱮ", quietAndListen.targetOlChiki)
        assertEquals("Thir tahen me ar Dheyan te anjom me", quietAndListen.targetRoman)

        val standAndCome = engine.translate("खड़े हो जाओ और यहाँ आओ", Language.HINDI, Language.SANTHALI)
        assertEquals("ᱛᱤᱸᱜᱩᱱ ᱢᱮ ᱟᱨ ᱱᱚᱸᱰᱮ ᱦᱤᱡᱩᱜ ᱢᱮ", standAndCome.targetOlChiki)
        assertEquals("Tingun me ar Nonde hijug me", standAndCome.targetRoman)
    }
}
