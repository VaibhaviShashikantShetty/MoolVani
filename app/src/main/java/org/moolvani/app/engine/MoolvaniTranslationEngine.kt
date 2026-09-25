package org.moolvani.app.engine

import org.moolvani.app.data.db.MoolVaniRepository
import org.moolvani.app.data.model.Language
import org.moolvani.app.data.model.TranslationResult
import org.moolvani.app.data.seed.SeedData

class MoolvaniTranslationEngine(
    private val repository: MoolVaniRepository? = null,
    var nlpModel: OfflineNlpModel? = null
) : TranslationEngine {

    data class ClassroomPhrase(
        val canonicalHindi: String,
        val variants: List<String>,
        val olChiki: String,
        val roman: String,
        val englishMeaning: String,
        val category: String
    )

    data class TranslationEntry(
        val hindi: String,
        val santhaliOlchiki: String,
        val santhaliRoman: String,
        val englishMeaning: String = "",
        val category: String = "General"
    )

    private val storedClassroomPhrases = listOf(
        ClassroomPhrase(
            canonicalHindi = "किताब खोलिए",
            variants = listOf("किताब खोलो", "किताब खोलिए", "किताब खोल", "पुस्तक खोलो", "पुस्तक खोलिए", "किताब निकालो", "किताब निकालिए"),
            olChiki = "ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ",
            roman = "Potob jhij me",
            englishMeaning = "Open your book",
            category = "Classroom Commands"
        ),
        ClassroomPhrase(
            canonicalHindi = "किताब बंद करो",
            variants = listOf("किताब बंद करो", "किताब बंद करिए", "किताब बंद", "पुस्तक बंद करो", "किताब रखो"),
            olChiki = "ᱯᱚᱛᱚᱵ ᱵᱚᱸᱫᱽ ᱢᱮ",
            roman = "Potob bond me",
            englishMeaning = "Close your book",
            category = "Classroom Commands"
        ),
        ClassroomPhrase(
            canonicalHindi = "बैठ जाओ",
            variants = listOf("बैठ जाओ", "बैठ जाइए", "बैठो", "बैठिए", "नीचे बैठो", "अपनी जगह पर बैठो", "सिट डाउन", "sit down"),
            olChiki = "ᱫᱩᱲᱩᱵ ᱢᱮ",
            roman = "Durub me",
            englishMeaning = "Sit down",
            category = "Classroom Commands"
        ),
        ClassroomPhrase(
            canonicalHindi = "खड़े हो जाओ",
            variants = listOf("खड़े हो जाओ", "खड़े हो जाइए", "खड़े हो", "खड़ा हो जाओ", "उठो", "खड़े रहिए", "स्टैंड अप", "stand up"),
            olChiki = "ᱛᱤᱸᱜᱩᱱ ᱢᱮ",
            roman = "Tingun me",
            englishMeaning = "Stand up",
            category = "Classroom Commands"
        ),
        ClassroomPhrase(
            canonicalHindi = "शांत रहिए",
            variants = listOf("शांत रहिए", "शांत रहो", "चुप रहो", "चुप रहिए", "आवाज मत करो", "शोर मत करो", "सब चुप रहो", "बी क्वाइट", "be quiet"),
            olChiki = "ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱢᱮ",
            roman = "Thir tahen me",
            englishMeaning = "Be quiet",
            category = "Classroom Commands"
        ),
        ClassroomPhrase(
            canonicalHindi = "ध्यान से सुनो",
            variants = listOf("ध्यान से सुनो", "ध्यान से सुनिए", "सुनो", "सुनिए", "मेरी बात सुनो", "गौर से सुनो", "लिसन", "listen"),
            olChiki = "ᱫᱷᱮᱭᱟᱱ ᱛᱮ ᱟᱧᱡᱚᱢ ᱢᱮ",
            roman = "Dheyan te anjom me",
            englishMeaning = "Listen carefully",
            category = "Classroom Commands"
        ),
        ClassroomPhrase(
            canonicalHindi = "कोई डाउट है?",
            variants = listOf("कोई डाउट है?", "कोई डाउट है", "कोई सवाल है?", "कोई सवाल है", "कोई प्रश्न है?", "कुछ पूछना है?", "एनी डाउट", "any doubt"),
            olChiki = "ᱡᱟᱦᱟᱸᱱᱟᱜ ᱠᱩᱠᱞᱤ ᱢᱮᱱᱟᱜᱼᱟ?",
            roman = "Jahannag kukli menaga?",
            englishMeaning = "Any doubts / questions?",
            category = "Questions"
        ),
        ClassroomPhrase(
            canonicalHindi = "पानी पीना है?",
            variants = listOf("पानी पीना है?", "पानी पीना है", "पानी चाहिए?", "पानी चाहिए", "पानी पीना चाहते हो?", "want water", "need water"),
            olChiki = "ᱫᱟᱜ ᱧᱩ ᱥᱟᱱᱟᱭᱮᱫ ᱢᱮᱭᱟ?",
            roman = "Dag nyu sanayed meya?",
            englishMeaning = "Do you want water?",
            category = "Questions"
        ),
        ClassroomPhrase(
            canonicalHindi = "सुप्रभात शिक्षक",
            variants = listOf("सुप्रभात शिक्षक", "सुप्रभात", "शुभ प्रभात", "नमस्ते शिक्षक", "नमस्ते सर", "नमस्ते मैडम", "गुड मॉर्निंग", "good morning"),
            olChiki = "ᱡᱚᱦᱟᱨ ᱢᱟᱪᱮᱛ",
            roman = "Johar Machet",
            englishMeaning = "Good morning teacher",
            category = "Greetings"
        ),
        ClassroomPhrase(
            canonicalHindi = "धन्यवाद",
            variants = listOf("धन्यवाद", "बहुत धन्यवाद", "शुक्रिया", "थैंक यू", "thank you"),
            olChiki = "ᱥᱟᱨᱦᱟᱣ",
            roman = "Sarhaw",
            englishMeaning = "Thank you",
            category = "Greetings"
        ),
        ClassroomPhrase(
            canonicalHindi = "समझ में आया?",
            variants = listOf("समझ में आया?", "समझ में आया", "समझ आया?", "समझ आया", "समझे?", "क्या समझ में आया?"),
            olChiki = "ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟᱢ?",
            roman = "Bujhaw kedam?",
            englishMeaning = "Did you understand?",
            category = "Questions"
        ),
        ClassroomPhrase(
            canonicalHindi = "हाँ, मुझे समझ आ गया",
            variants = listOf("हाँ, मुझे समझ आ गया", "हाँ मुझे समझ आ गया", "मुझे समझ आ गया", "समझ आ गया", "हाँ समझ गया", "हाँ"),
            olChiki = "ᱦᱮᱸ, ᱤᱧ ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟ",
            roman = "Hen, inj bujhaw keda",
            englishMeaning = "Yes, I understood",
            category = "Student Responses"
        ),
        ClassroomPhrase(
            canonicalHindi = "नहीं, मुझे समझ नहीं आया",
            variants = listOf("नहीं, मुझे समझ नहीं आया", "नहीं मुझे समझ नहीं आया", "मुझे समझ नहीं आया", "समझ नहीं आया", "नहीं समझा", "नहीं"),
            olChiki = "ᱵᱟᱝ, ᱤᱧ ᱵᱟᱹᱧ ᱵᱩᱡᱷᱟᱹᱣ ᱞᱮᱫᱟ",
            roman = "Bang, inj banj bujhaw leda",
            englishMeaning = "No, I did not understand",
            category = "Student Responses"
        ),
        ClassroomPhrase(
            canonicalHindi = "फिर से बोलो",
            variants = listOf("फिर से बोलो", "फिर से बताइए", "दोबारा बोलो", "एक बार फिर बोलो", "फिर से कहो"),
            olChiki = "ᱟᱨ ᱢᱤᱫᱫᱷᱟᱣ ᱨᱚᱲ ᱢᱮ",
            roman = "Ar middhaw ror me",
            englishMeaning = "Say it again",
            category = "Classroom Commands"
        ),
        ClassroomPhrase(
            canonicalHindi = "ब्लैकबोर्ड पर देखिए",
            variants = listOf("ब्लैकबोर्ड पर देखिए", "बोर्ड देखो", "बोर्ड पर देखो", "बोर्ड देखिए", "श्यामपट्ट पर देखो", "बोर्ड की तरफ देखो"),
            olChiki = "ᱵᱳᱨᱰ ᱨᱮ ᱧᱮᱞ ᱢᱮ",
            roman = "Board re nel me",
            englishMeaning = "Look at the blackboard",
            category = "Classroom Commands"
        ),
        ClassroomPhrase(
            canonicalHindi = "लिखना शुरू करो",
            variants = listOf("लिखना शुरू करो", "लिखना शुरू करिए", "लिखो", "लिखिए", "अपनी कॉपी में लिखो", "शुरू करो"),
            olChiki = "ᱚᱞ ᱮᱦᱚᱵ ᱢᱮ",
            roman = "Ol ehob me",
            englishMeaning = "Start writing",
            category = "Classroom Commands"
        ),
        ClassroomPhrase(
            canonicalHindi = "आप क्या कर रहे हैं?",
            variants = listOf("आप क्या कर रहे हैं?", "आप क्या कर रहे हो?", "तुम क्या कर रहे हो?", "क्या कर रहे हो?"),
            olChiki = "ᱟᱢ ᱪᱮᱫ ᱮᱢ ᱪᱮᱠᱟᱭᱮᱫᱟ?",
            roman = "Am ched em chekayeda?",
            englishMeaning = "What are you doing?",
            category = "Questions"
        ),
        ClassroomPhrase(
            canonicalHindi = "मैं पढ़ रहा हूँ",
            variants = listOf("मैं पढ़ रहा हूँ", "मैं पढ़ रहा हूं", "पढ़ रहा हूँ", "पढ़ रहा हूं"),
            olChiki = "ᱤᱧ ᱯᱟᱲᱦᱟᱣᱜ ᱠᱟᱱᱟ",
            roman = "Inj parhawg kana",
            englishMeaning = "I am reading",
            category = "Student Responses"
        ),
        ClassroomPhrase(
            canonicalHindi = "मैं होमवर्क कर रहा हूँ",
            variants = listOf("मैं होमवर्क कर रहा हूँ", "होमवर्क कर रहा हूँ", "काम कर रहा हूँ"),
            olChiki = "ᱤᱧ ᱚᱲᱟᱜ ᱠᱟᱹᱢᱤᱧ ᱠᱟᱹᱢᱤ ᱠᱟᱱᱟ",
            roman = "Inj orag kaminj kami kana",
            englishMeaning = "I am doing homework",
            category = "Student Responses"
        ),
        ClassroomPhrase(
            canonicalHindi = "यहाँ आओ",
            variants = listOf("यहाँ आओ", "इधर आओ", "आइए", "पास आओ", "यहाँ आइए"),
            olChiki = "ᱱᱚᱸᱰᱮ ᱦᱤᱡᱩᱜ ᱢᱮ",
            roman = "Nonde hijug me",
            englishMeaning = "Come here",
            category = "Classroom Commands"
        ),
        ClassroomPhrase(
            canonicalHindi = "वहाँ जाओ",
            variants = listOf("वहाँ जाओ", "उधर जाओ", "जाओ", "जाइए", "वहाँ जाइए"),
            olChiki = "ᱦᱟᱸᱰᱮ ᱪᱟᱞᱟᱣ ᱢᱮ",
            roman = "Hande chalaw me",
            englishMeaning = "Go there",
            category = "Classroom Commands"
        ),
        ClassroomPhrase(
            canonicalHindi = "हाथ उठाओ",
            variants = listOf("हाथ उठाओ", "हाथ ऊपर करो", "हाथ उठाइए"),
            olChiki = "ᱛᱤ ᱛᱩᱞ ᱢᱮ",
            roman = "Ti tul me",
            englishMeaning = "Raise your hand",
            category = "Classroom Commands"
        ),
        ClassroomPhrase(
            canonicalHindi = "सवाल पूछो",
            variants = listOf("सवाल पूछो", "प्रश्न पूछो", "पूछो", "पूछिए"),
            olChiki = "ᱠᱩᱠᱞᱤ ᱠᱩᱞᱤ ᱢᱮ",
            roman = "Kukli kuli me",
            englishMeaning = "Ask a question",
            category = "Classroom Commands"
        ),
        ClassroomPhrase(
            canonicalHindi = "उत्तर दो",
            variants = listOf("उत्तर दो", "जवाब दो", "उत्तर दीजिए", "जवाब दीजिए"),
            olChiki = "ᱛᱮᱞᱟ ᱮᱢ ᱢᱮ",
            roman = "Tela em me",
            englishMeaning = "Give the answer",
            category = "Classroom Commands"
        ),
        ClassroomPhrase(
            canonicalHindi = "आप कैसे हैं",
            variants = listOf("आप कैसे हैं", "आप कैसे हो", "तुम कैसे हो", "कैसे हो", "कैसी हो", "क्या हाल है"),
            olChiki = "ᱟᱢ ᱪᱮᱫ ᱞᱮᱠᱟ ᱢᱮᱱᱟᱢᱟ",
            roman = "Am ched leka menama",
            englishMeaning = "How are you?",
            category = "Greetings"
        ),
        ClassroomPhrase(
            canonicalHindi = "आपका नाम क्या है",
            variants = listOf("आपका नाम क्या है", "तुम्हारा नाम क्या है", "नाम क्या है", "अपना नाम बताओ"),
            olChiki = "ᱟᱢᱟᱜ ᱧᱩᱛᱩᱢ ᱪᱮᱫ",
            roman = "Amag nyutum ched",
            englishMeaning = "What is your name?",
            category = "Questions"
        ),
        ClassroomPhrase(
            canonicalHindi = "मेरा नाम",
            variants = listOf("मेरा नाम", "नाम"),
            olChiki = "ᱤᱧᱟᱜ ᱧᱩᱛᱩᱢ",
            roman = "Injag nyutum",
            englishMeaning = "My name",
            category = "Phrases"
        ),
        ClassroomPhrase(
            canonicalHindi = "बहुत अच्छा",
            variants = listOf("बहुत अच्छा", "बहुत बढ़िया", "शाबाश", "बढ़िया"),
            olChiki = "ᱟᱹᱰᱤ ᱵᱷᱟᱹᱜᱤ",
            roman = "Adi bhagi",
            englishMeaning = "Very good",
            category = "Phrases"
        )
    )

    // Comprehensive offline dictionary
    private val hindiToEntry = mutableMapOf<String, TranslationEntry>()
    private val santhaliToEntry = mutableMapOf<String, TranslationEntry>()
    private val englishToEntry = mutableMapOf<String, TranslationEntry>()

    init {
        loadBaseDictionary()
    }

    private fun loadBaseDictionary() {
        // 1. Index stored classroom phrases and all spoken variants
        for (cp in storedClassroomPhrases) {
            val entry = TranslationEntry(
                hindi = cp.canonicalHindi,
                santhaliOlchiki = cp.olChiki,
                santhaliRoman = cp.roman,
                englishMeaning = cp.englishMeaning,
                category = cp.category
            )
            indexEntry(entry)
            for (variant in cp.variants) {
                val variantEntry = TranslationEntry(
                    hindi = cp.canonicalHindi,
                    santhaliOlchiki = cp.olChiki,
                    santhaliRoman = cp.roman,
                    englishMeaning = cp.englishMeaning,
                    category = cp.category
                )
                val normV = normalize(variant)
                hindiToEntry[normV] = variantEntry
            }
        }

        // 2. Load from SeedData initial phrases
        for (p in SeedData.initialPhrases) {
            val entry = TranslationEntry(
                hindi = p.hindiText,
                santhaliOlchiki = p.santhaliOlchiki,
                santhaliRoman = p.santhaliRoman,
                englishMeaning = p.englishMeaning,
                category = p.category
            )
            indexEntry(entry)
        }

        // 3. Load from SeedData courses (Numbers, Animals, Colors, Vegetables)
        for (item in SeedData.initialCourseItems) {
            val cleanHindi = item.hindiText.replace(Regex("\\s*\\(\\d+\\)"), "").trim()
            val cleanOlchiki = item.santhaliOlchiki.replace(Regex("^[᱐-᱙0-9]+\\s*-\\s*"), "").trim()
            val cleanRoman = item.santhaliRoman.replace(Regex("^[0-9]+\\s*-\\s*"), "").trim()
            val entry = TranslationEntry(
                hindi = cleanHindi,
                santhaliOlchiki = cleanOlchiki,
                santhaliRoman = cleanRoman,
                englishMeaning = item.englishMeaning,
                category = item.courseCode
            )
            indexEntry(entry)

            if (cleanHindi != item.hindiText || cleanOlchiki != item.santhaliOlchiki) {
                val fullEntry = TranslationEntry(
                    hindi = item.hindiText,
                    santhaliOlchiki = item.santhaliOlchiki,
                    santhaliRoman = item.santhaliRoman,
                    englishMeaning = item.englishMeaning,
                    category = item.courseCode
                )
                indexEntry(fullEntry)
            }
        }

        // 4. Extended classroom & linguistic vocabulary from Bhasha Setu knowledge base
        val extraVocab = listOf(
            TranslationEntry("नमस्ते", "ᱡᱚᱦᱟᱨ", "Johar", "Greetings"),
            TranslationEntry("नमस्कार", "ᱡᱚᱦᱟᱨ", "Johar", "Greetings"),
            TranslationEntry("प्रणाम", "ᱡᱚᱦᱟᱨ", "Johar", "Greetings"),
            TranslationEntry("धन्यवाद", "ᱥᱟᱨᱦᱟᱣ", "Sarhaw", "Greetings"),
            TranslationEntry("शुक्रिया", "ᱥᱟᱨᱦᱟᱣ", "Sarhaw", "Greetings"),
            TranslationEntry("पानी", "ᱫᱟᱜ", "Dag", "Environment"),
            TranslationEntry("जल", "ᱫᱟᱜ", "Dag", "Environment"),
            TranslationEntry("खाना", "ᱡᱚᱢᱟᱜ", "Jomag", "Daily"),
            TranslationEntry("भोजन", "ᱡᱚᱢᱟᱜ", "Jomag", "Daily"),
            TranslationEntry("दोस्त", "ᱜᱟᱛᱮ", "Gate", "Classroom"),
            TranslationEntry("मित्र", "ᱜᱟᱛᱮ", "Gate", "Classroom"),
            TranslationEntry("शिक्षक", "ᱢᱟᱪᱮᱛ", "Machet", "Classroom"),
            TranslationEntry("अध्यापक", "ᱢᱟᱪᱮᱛ", "Machet", "Classroom"),
            TranslationEntry("गुरुजी", "ᱢᱟᱪᱮᱛ", "Machet", "Classroom"),
            TranslationEntry("छात्र", "ᱯᱟᱹᱴᱷᱩᱣᱟᱹ", "Pathuwa", "Classroom"),
            TranslationEntry("छात्रा", "ᱯᱟᱹᱴᱷᱩᱣᱟᱹ", "Pathuwa", "Classroom"),
            TranslationEntry("बच्चा", "ᱜᱤᱫᱽᱨᱟᱹ", "Gidra", "People"),
            TranslationEntry("बच्चे", "ᱜᱤᱫᱽᱨᱟᱹ ᱠᱚ", "Gidra ko", "People"),
            TranslationEntry("बच्चों", "ᱜᱤᱫᱽᱨᱟᱹ ᱠᱚ", "Gidra ko", "People"),
            TranslationEntry("लड़का", "ᱠᱚᱲᱟ", "Kora", "People"),
            TranslationEntry("लड़की", "ᱠᱩᱲᱤ", "Kuri", "People"),
            TranslationEntry("लोग", "ᱦᱚᱲ", "Hor", "People"),
            TranslationEntry("माँ", "ᱟᱭᱳ", "Ayo", "Family"),
            TranslationEntry("माता", "ᱟᱭᱳ", "Ayo", "Family"),
            TranslationEntry("पिता", "ᱵᱟᱵᱟ", "Baba", "Family"),
            TranslationEntry("भाई", "ᱵᱚᱭᱦᱟ", "Boyha", "Family"),
            TranslationEntry("बहन", "ᱢᱤᱥᱨᱟ", "Misra", "Family"),
            TranslationEntry("स्कूल", "ᱟᱥᱲᱟ", "Asra", "School"),
            TranslationEntry("विद्यालय", "ᱟᱥᱲᱟ", "Asra", "School"),
            TranslationEntry("कक्षा", "ᱪᱟᱱᱟᱪ", "Chanach", "School"),
            TranslationEntry("कमरा", "ᱚᱲᱟᱜ", "Orag", "School"),
            TranslationEntry("घर", "ᱚᱲᱟᱜ", "Orag", "Environment"),
            TranslationEntry("किताब", "ᱯᱚᱛᱚᱵ", "Potob", "Classroom Objects"),
            TranslationEntry("पुस्तक", "ᱯᱚᱛᱚᱵ", "Potob", "Classroom Objects"),
            TranslationEntry("कॉपी", "ᱠᱷᱟᱛᱟ", "Khata", "Classroom Objects"),
            TranslationEntry("कापी", "ᱠᱷᱟᱛᱟ", "Khata", "Classroom Objects"),
            TranslationEntry("कलम", "ᱠᱚᱞᱚᱢ", "Kalam", "Classroom Objects"),
            TranslationEntry("पेन", "ᱠᱚᱞᱚᱢ", "Kalam", "Classroom Objects"),
            TranslationEntry("पेंसिल", "ᱯᱮᱱᱥᱤᱞ", "Pensil", "Classroom Objects"),
            TranslationEntry("बोर्ड", "ᱵᱚᱨᱰ", "Board", "Classroom Objects"),
            TranslationEntry("श्यामपट्ट", "ᱵᱚᱨᱰ", "Board", "Classroom Objects"),
            TranslationEntry("सवाल", "ᱠᱩᱠᱞᱤ", "Kukli", "Classroom"),
            TranslationEntry("प्रश्न", "ᱠᱩᱠᱞᱤ", "Kukli", "Classroom"),
            TranslationEntry("उत्तर", "ᱛᱮᱞᱟ", "Tela", "Classroom"),
            TranslationEntry("जवाब", "ᱛᱮᱞᱟ", "Tela", "Classroom"),
            TranslationEntry("नाम", "ᱧᱩᱛᱩᱢ", "Nyutum", "General"),
            TranslationEntry("पढ़ो", "ᱯᱟᱲᱦᱟᱣ ᱢᱮ", "Parhaw me", "Verbs"),
            TranslationEntry("पढ़ना", "ᱯᱟᱲᱦᱟᱣ", "Parhaw", "Verbs"),
            TranslationEntry("पढ़िए", "ᱯᱟᱲᱦᱟᱣ ᱢᱮ", "Parhaw me", "Verbs"),
            TranslationEntry("लिखो", "ᱚᱞ ᱢᱮ", "Ol me", "Verbs"),
            TranslationEntry("लिखना", "ᱚᱞ", "Ol", "Verbs"),
            TranslationEntry("लिखिए", "ᱚᱞ ᱢᱮ", "Ol me", "Verbs"),
            TranslationEntry("सुनो", "ᱟᱸᱡᱚᱢ ᱢᱮ", "Anjom me", "Verbs"),
            TranslationEntry("सुनना", "ᱟᱸᱡᱚᱢ", "Anjom", "Verbs"),
            TranslationEntry("सुनिए", "ᱟᱸᱡᱚᱢ ᱢᱮ", "Anjom me", "Verbs"),
            TranslationEntry("बोलो", "ᱨᱚᱲ ᱢᱮ", "Ror me", "Verbs"),
            TranslationEntry("बोलना", "ᱨᱚᱲ", "Ror", "Verbs"),
            TranslationEntry("बोलिए", "ᱨᱚᱲ ᱢᱮ", "Ror me", "Verbs"),
            TranslationEntry("देखो", "ᱧᱮᱞ ᱢᱮ", "Nel me", "Verbs"),
            TranslationEntry("देखना", "ᱧᱮᱞ", "Nel", "Verbs"),
            TranslationEntry("देखिए", "ᱧᱮᱞ ᱢᱮ", "Nel me", "Verbs"),
            TranslationEntry("बैठो", "ᱫᱩᱲᱩᱵ ᱢᱮ", "Durub me", "Verbs"),
            TranslationEntry("बैठना", "ᱫᱩᱲᱩᱵ", "Durub", "Verbs"),
            TranslationEntry("बैठिए", "ᱫᱩᱲᱩᱵ ᱢᱮ", "Durub me", "Verbs"),
            TranslationEntry("आओ", "ᱦᱤᱡᱩᱜ ᱢᱮ", "Hijug me", "Verbs"),
            TranslationEntry("आना", "ᱦᱤᱡᱩᱜ", "Hijug", "Verbs"),
            TranslationEntry("आइए", "ᱦᱤᱡᱩᱜ ᱢᱮ", "Hijug me", "Verbs"),
            TranslationEntry("जाओ", "ᱪᱟᱞᱟᱣ ᱢᱮ", "Chalaw me", "Verbs"),
            TranslationEntry("जाना", "ᱪᱟᱞᱟᱣ", "Chalaw", "Verbs"),
            TranslationEntry("जाइए", "ᱪᱟᱞᱟᱣ ᱢᱮ", "Chalaw me", "Verbs"),
            TranslationEntry("खोलो", "ᱡᱷᱤᱡᱽ ᱢᱮ", "Jhij me", "Verbs"),
            TranslationEntry("खोलना", "ᱡᱷᱤᱡᱽ", "Jhij", "Verbs"),
            TranslationEntry("खोलिए", "ᱡᱷᱤᱡᱽ ᱢᱮ", "Jhij me", "Verbs"),
            TranslationEntry("बंद", "ᱵᱚᱸᱫᱽ", "Bond", "Verbs"),
            TranslationEntry("सीखो", "ᱥᱮᱪᱮᱫᱚᱜ ᱢᱮ", "Sechedog me", "Verbs"),
            TranslationEntry("समझो", "ᱵᱩᱡᱷᱟᱹᱣ ᱢᱮ", "Bujhaw me", "Verbs"),
            TranslationEntry("खेलना", "ᱮᱱᱮᱡ", "Enej", "Verbs"),
            TranslationEntry("खेलो", "ᱮᱱᱮᱡ ᱢᱮ", "Enej me", "Verbs"),
            TranslationEntry("खाओ", "ᱡᱚᱢ ᱢᱮ", "Jom me", "Verbs"),
            TranslationEntry("पीना", "ᱧᱩ", "Nyu", "Verbs"),
            TranslationEntry("पीओ", "ᱧᱩ ᱢᱮ", "Nyu me", "Verbs"),
            TranslationEntry("पेड़", "ᱫᱟᱨᱮ", "Dare", "Environment"),
            TranslationEntry("वृक्ष", "ᱫᱟᱨᱮ", "Dare", "Environment"),
            TranslationEntry("फूल", "ᱵᱟᱦᱟ", "Baha", "Environment"),
            TranslationEntry("फल", "ᱡᱚ", "Jo", "Environment"),
            TranslationEntry("सूरज", "ᱵᱮᱲᱟ", "Bera", "Environment"),
            TranslationEntry("सूर्य", "ᱵᱮᱲᱟ", "Bera", "Environment"),
            TranslationEntry("चाँद", "ᱪᱟᱸᱫᱚ", "Chando", "Environment"),
            TranslationEntry("नदी", "ᱜᱟᱰᱟ", "Gada", "Environment"),
            TranslationEntry("जंगल", "ᱵᱤᱨ", "Bir", "Environment"),
            TranslationEntry("गाँव", "ᱟᱹᱛᱩ", "Atu", "Environment"),
            TranslationEntry("शहर", "ᱵᱟᱡᱟᱨ", "Bajar", "Environment"),
            TranslationEntry("दिन", "ᱢᱟᱦᱟᱸ", "Mahan", "Time"),
            TranslationEntry("रात", "ᱧᱤᱫᱟᱹ", "Nyida", "Time"),
            TranslationEntry("सुबह", "ᱥᱮᱛᱟ", "Seta", "Time"),
            TranslationEntry("शाम", "ᱟᱹᱭᱩᱵ", "Ayub", "Time"),
            TranslationEntry("आज", "ᱛᱮᱦᱮᱧ", "Tehenj", "Time"),
            TranslationEntry("कल", "ᱜᱟᱯᱟ", "Gapa", "Time"),
            TranslationEntry("मैं", "ᱤᱧ", "Inj", "Pronouns"),
            TranslationEntry("हम", "ᱟᱵᱚ", "Abo", "Pronouns"),
            TranslationEntry("आप", "ᱟᱢ", "Am", "Pronouns"),
            TranslationEntry("तुम", "ᱟᱢ", "Am", "Pronouns"),
            TranslationEntry("वह", "ᱩᱱᱤ", "Uni", "Pronouns"),
            TranslationEntry("वे", "ᱩᱱᱠᱩ", "Unku", "Pronouns"),
            TranslationEntry("यह", "ᱱᱚᱣᱟ", "Nowa", "Pronouns"),
            TranslationEntry("ये", "ᱱᱚᱣᱟ ᱠᱚ", "Nowa ko", "Pronouns"),
            TranslationEntry("वो", "ᱦᱟᱱᱟ", "Hana", "Pronouns"),
            TranslationEntry("मेरा", "ᱤᱧᱟᱜ", "Injag", "Pronouns"),
            TranslationEntry("मेरी", "ᱤᱧᱟᱜ", "Injag", "Pronouns"),
            TranslationEntry("हमारा", "ᱟᱵᱚᱣᱟᱜ", "Abowag", "Pronouns"),
            TranslationEntry("आपका", "ᱟᱢᱟᱜ", "Amag", "Pronouns"),
            TranslationEntry("तुम्हारा", "ᱟᱢᱟᱜ", "Amag", "Pronouns"),
            TranslationEntry("अच्छा", "ᱵᱷᱟᱹᱜᱤ", "Bhagi", "Modifiers"),
            TranslationEntry("बढ़िया", "ᱵᱮᱥ", "Bes", "Modifiers"),
            TranslationEntry("खराब", "ᱵᱟᱹᱲᱤᱡ", "Barij", "Modifiers"),
            TranslationEntry("बड़ा", "ᱢᱟᱨᱟᱝ", "Marang", "Modifiers"),
            TranslationEntry("छोटा", "ᱠᱟᱹᱴᱤᱡ", "Katij", "Modifiers"),
            TranslationEntry("हाँ", "ᱦᱮᱸ", "Hen", "Responses"),
            TranslationEntry("नहीं", "ᱵᱟᱝ", "Bang", "Responses"),
            TranslationEntry("और", "ᱟᱨ", "Ar", "Conjunctions"),
            TranslationEntry("लेकिन", "ᱢᱮᱱᱠᱷᱟᱱ", "Menkhan", "Conjunctions"),
            TranslationEntry("बहुत", "ᱟᱹᱰᱤ", "Adi", "Modifiers"),
            TranslationEntry("क्या", "ᱪᱮᱫ", "Ched", "Questions"),
            TranslationEntry("कहाँ", "ᱚᱠᱟᱨᱮ", "Okare", "Questions"),
            TranslationEntry("कब", "ᱛᱤᱥ", "Tis", "Questions"),
            TranslationEntry("कौन", "ᱚᱠᱚᱭ", "Okoy", "Questions"),
            TranslationEntry("कैसे", "ᱪᱮᱫ ᱞᱮᱠᱟ", "Ched leka", "Questions"),
            TranslationEntry("कैसा", "ᱪᱮᱫ ᱞᱮᱠᱟ", "Ched leka", "Questions"),
            TranslationEntry("क्यों", "ᱪᱮᱫᱟᱜ", "Chedag", "Questions"),
            TranslationEntry("कितना", "ᱛᱤᱱᱟᱹᱜ", "Tinag", "Questions"),
            TranslationEntry("शून्य", "ᱥᱩᱱ", "Sun", "Numbers"),
            TranslationEntry("एक", "ᱢᱤᱫ", "Mid", "Numbers"),
            TranslationEntry("दो", "ᱵᱟᱨ", "Bar", "Numbers"),
            TranslationEntry("तीन", "ᱯᱮ", "Pe", "Numbers"),
            TranslationEntry("चार", "ᱯᱩᱱ", "Pun", "Numbers"),
            TranslationEntry("पाँच", "ᱢᱚᱬᱮ", "More", "Numbers"),
            TranslationEntry("पांच", "ᱢᱚᱬᱮ", "More", "Numbers"),
            TranslationEntry("छह", "ᱛᱩᱨᱩᱭ", "Turuy", "Numbers"),
            TranslationEntry("सात", "ᱮᱭᱟᱭ", "Eyay", "Numbers"),
            TranslationEntry("आठ", "ᱤᱨᱟᱹᱞ", "Iral", "Numbers"),
            TranslationEntry("नौ", "ᱟᱨᱮ", "Are", "Numbers"),
            TranslationEntry("दस", "ᱜᱮᱞ", "Gel", "Numbers")
        )

        for (entry in extraVocab) {
            indexEntry(entry)
        }
    }

    private fun indexEntry(entry: TranslationEntry) {
        val normHi = normalize(entry.hindi)
        if (normHi.isNotEmpty() && !hindiToEntry.containsKey(normHi)) {
            hindiToEntry[normHi] = entry
        }
        val cleanHi = normHi.replace(Regex("[0-9()]+"), "").trim()
        if (cleanHi.isNotEmpty() && cleanHi != normHi && !hindiToEntry.containsKey(cleanHi)) {
            hindiToEntry[cleanHi] = entry
        }

        val normEn = normalize(entry.englishMeaning)
        if (normEn.isNotEmpty() && !englishToEntry.containsKey(normEn)) {
            englishToEntry[normEn] = entry
        }
        val cleanEn = normEn.replace(Regex("[0-9()?!/]+"), " ").trim()
        if (cleanEn.isNotEmpty() && cleanEn != normEn && !englishToEntry.containsKey(cleanEn)) {
            englishToEntry[cleanEn] = entry
        }

        val normSat = normalize(entry.santhaliOlchiki)
        if (normSat.isNotEmpty() && !santhaliToEntry.containsKey(normSat)) {
            santhaliToEntry[normSat] = entry
        }

        val normRoman = normalize(entry.santhaliRoman)
        if (normRoman.isNotEmpty() && !santhaliToEntry.containsKey(normRoman)) {
            santhaliToEntry[normRoman] = entry
        }

        val devaPhonetic = normalize(OlChikiTransliterator.toPhoneticDevanagari(entry.santhaliOlchiki))
        if (devaPhonetic.isNotEmpty() && !santhaliToEntry.containsKey(devaPhonetic)) {
            santhaliToEntry[devaPhonetic] = entry
        }

        val cleanOl = normSat.replace(Regex("^[᱐-᱙0-9]+\\s*"), "").trim()
        if (cleanOl.isNotEmpty() && cleanOl != normSat && !santhaliToEntry.containsKey(cleanOl)) {
            santhaliToEntry[cleanOl] = entry
        }
        val cleanRo = normRoman.replace(Regex("^[0-9]+\\s*"), "").trim()
        if (cleanRo.isNotEmpty() && cleanRo != normRoman && !santhaliToEntry.containsKey(cleanRo)) {
            santhaliToEntry[cleanRo] = entry
        }
    }

    fun syncFromDatabase() {
        repository?.let { repo ->
            try {
                val phrases = repo.getAllPhrases()
                for (p in phrases) {
                    val entry = TranslationEntry(
                        hindi = p.hindiText,
                        santhaliOlchiki = p.santhaliOlchiki,
                        santhaliRoman = p.santhaliRoman,
                        category = p.category
                    )
                    indexEntry(entry)
                }
            } catch (ignored: Exception) {}
        }
    }

    override fun translate(text: String, from: Language, to: Language): TranslationResult {
        val startTime = System.currentTimeMillis()
        val trimmed = text.trim()
        val norm = normalize(trimmed)

        if (trimmed.isEmpty()) {
            return TranslationResult(
                sourceText = "",
                sourceLang = from,
                targetLang = to,
                targetOlChiki = "",
                targetRoman = "",
                targetDevanagari = "",
                latencyMs = 0
            )
        }

        // STEP 0: Check pluggable offline NLP model if attached
        nlpModel?.let { model ->
            if (model.isLoaded) {
                val modelResult = model.infer(trimmed, from.code, to.code)
                if (!modelResult.isNullOrEmpty()) {
                    val latency = System.currentTimeMillis() - startTime
                    return TranslationResult(
                        sourceText = trimmed,
                        sourceLang = from,
                        targetLang = to,
                        targetOlChiki = if (to == Language.SANTHALI) modelResult else trimmed,
                        targetRoman = if (to == Language.SANTHALI) OlChikiTransliterator.toPhoneticRoman(modelResult) else trimmed,
                        targetDevanagari = if (to == Language.HINDI) modelResult else trimmed,
                        confidence = 0.95f,
                        latencyMs = latency,
                        isExactMatch = true,
                        matchedCategory = "AI Model (${model.modelName})"
                    )
                }
            }
        }

        if (from == Language.HINDI) {
            // STEP 0.5: Multi-clause / Combined sentence support (e.g. "बैठ जाओ और किताब खोलो", "खड़े हो जाओ और यहाँ आओ")
            val clauseSplitters = listOf(" और ", " तथा ", " फिर ", " and ", " & ", ", ", "। ")
            for (splitter in clauseSplitters) {
                if (trimmed.contains(splitter, ignoreCase = true)) {
                    val parts = trimmed.split(splitter.toRegex(RegexOption.IGNORE_CASE))
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    if (parts.size >= 2) {
                        val subResults = parts.map { translate(it, from, to) }
                        val combinedOlChiki = subResults.joinToString(" ᱟᱨ ") { it.targetOlChiki }
                        val combinedRoman = subResults.joinToString(" ar ") { it.targetRoman }
                        val combinedDevanagari = subResults.joinToString(" और ") { it.targetDevanagari.ifEmpty { it.sourceText } }
                        val avgConfidence = subResults.map { it.confidence }.average().toFloat()
                        val latency = System.currentTimeMillis() - startTime
                        return TranslationResult(
                            sourceText = trimmed,
                            sourceLang = from,
                            targetLang = to,
                            targetOlChiki = combinedOlChiki,
                            targetRoman = combinedRoman,
                            targetDevanagari = combinedDevanagari,
                            confidence = avgConfidence,
                            latencyMs = latency,
                            isExactMatch = subResults.all { it.isExactMatch },
                            matchedCategory = "Combined Command"
                        )
                    }
                }
            }

            // STEP 1: Direct match against stored classroom phrases and all known spoken variants
            val directClassroom = findDirectClassroomPhrase(norm)
            if (directClassroom != null) {
                val latency = System.currentTimeMillis() - startTime
                return TranslationResult(
                    sourceText = trimmed,
                    sourceLang = from,
                    targetLang = to,
                    targetOlChiki = directClassroom.olChiki,
                    targetRoman = directClassroom.roman,
                    targetDevanagari = directClassroom.canonicalHindi,
                    confidence = 0.98f,
                    latencyMs = latency,
                    isExactMatch = true,
                    matchedCategory = directClassroom.category
                )
            }

            // STEP 2: Exact match in dictionary (e.g. "हाथी", "लाल", "पानी", "गाय")
            val direct = hindiToEntry[norm]
            if (direct != null) {
                val latency = System.currentTimeMillis() - startTime
                return TranslationResult(
                    sourceText = trimmed,
                    sourceLang = from,
                    targetLang = to,
                    targetOlChiki = direct.santhaliOlchiki,
                    targetRoman = direct.santhaliRoman,
                    targetDevanagari = direct.hindi,
                    confidence = 0.95f,
                    latencyMs = latency,
                    isExactMatch = true,
                    matchedCategory = direct.category
                )
            }

            // STEP 2B: Direct match in English dictionary (e.g. "open book", "sit down", "water", "elephant")
            val englishDirect = englishToEntry[norm]
            if (englishDirect != null) {
                val latency = System.currentTimeMillis() - startTime
                return TranslationResult(
                    sourceText = trimmed,
                    sourceLang = from,
                    targetLang = to,
                    targetOlChiki = englishDirect.santhaliOlchiki,
                    targetRoman = englishDirect.santhaliRoman,
                    targetDevanagari = englishDirect.hindi,
                    confidence = 0.98f,
                    latencyMs = latency,
                    isExactMatch = true,
                    matchedCategory = englishDirect.category
                )
            }

            for ((enKey, enEntry) in englishToEntry) {
                if (enKey.length >= 4 && (norm == enKey || (norm.length >= 5 && norm.contains(enKey)) || (norm.length >= 5 && enKey.contains(norm)))) {
                    val latency = System.currentTimeMillis() - startTime
                    return TranslationResult(
                        sourceText = trimmed,
                        sourceLang = from,
                        targetLang = to,
                        targetOlChiki = enEntry.santhaliOlchiki,
                        targetRoman = enEntry.santhaliRoman,
                        targetDevanagari = enEntry.hindi,
                        confidence = 0.95f,
                        latencyMs = latency,
                        isExactMatch = true,
                        matchedCategory = enEntry.category
                    )
                }
            }

            // STEP 3: Fuzzy / Keyword matching for spoken classroom variations (e.g. "सब बच्चे किताब खोलो", "हाथ ऊपर करो")
            val fuzzyClassroom = findFuzzyClassroomPhrase(norm)
            if (fuzzyClassroom != null) {
                val latency = System.currentTimeMillis() - startTime
                return TranslationResult(
                    sourceText = trimmed,
                    sourceLang = from,
                    targetLang = to,
                    targetOlChiki = fuzzyClassroom.olChiki,
                    targetRoman = fuzzyClassroom.roman,
                    targetDevanagari = fuzzyClassroom.canonicalHindi,
                    confidence = 0.95f,
                    latencyMs = latency,
                    isExactMatch = true,
                    matchedCategory = fuzzyClassroom.category
                )
            }

            // STEP 4: Multi-word greedy phrase matching (4-word, 3-word, 2-word, 1-word)
            val words = trimmed.split("\\s+".toRegex())
            if (words.size > 1) {
                val olChikiWords = mutableListOf<String>()
                val romanWords = mutableListOf<String>()
                var matchCount = 0
                var i = 0

                while (i < words.size) {
                    var segmentMatched = false
                    // Try 4-word, 3-word, 2-word combinations
                    for (len in 4 downTo 2) {
                        if (i + len <= words.size) {
                            val subPhrase = words.subList(i, i + len).joinToString(" ")
                            val normSub = normalize(subPhrase)
                            val subMatch = hindiToEntry[normSub]
                            if (subMatch != null) {
                                olChikiWords.add(subMatch.santhaliOlchiki)
                                romanWords.add(subMatch.santhaliRoman)
                                matchCount += len
                                i += len
                                segmentMatched = true
                                break
                            }
                        }
                    }

                    if (!segmentMatched) {
                        val word = words[i]
                        val normWord = normalize(word)
                        val wordMatch = hindiToEntry[normWord]
                        if (wordMatch != null) {
                            olChikiWords.add(wordMatch.santhaliOlchiki)
                            romanWords.add(wordMatch.santhaliRoman)
                            matchCount++
                        } else {
                            val ol = OlChikiTransliterator.toOlChiki(word)
                            val ro = OlChikiTransliterator.devanagariToRoman(word)
                            olChikiWords.add(ol)
                            romanWords.add(ro)
                        }
                        i++
                    }
                }

                val latency = System.currentTimeMillis() - startTime
                val confidence = (matchCount.toFloat() / words.size).coerceIn(0.5f, 0.95f)
                return TranslationResult(
                    sourceText = trimmed,
                    sourceLang = from,
                    targetLang = to,
                    targetOlChiki = olChikiWords.joinToString(" "),
                    targetRoman = romanWords.joinToString(" "),
                    targetDevanagari = trimmed,
                    confidence = confidence,
                    latencyMs = latency,
                    isExactMatch = matchCount == words.size,
                    matchedCategory = "Spoken Sentence"
                )
            }

            // STEP 5: Phonetic transliteration into Ol Chiki
            val fallbackOl = OlChikiTransliterator.toOlChiki(trimmed)
            val fallbackRoman = OlChikiTransliterator.devanagariToRoman(trimmed)
            val latency = System.currentTimeMillis() - startTime
            return TranslationResult(
                sourceText = trimmed,
                sourceLang = from,
                targetLang = to,
                targetOlChiki = fallbackOl,
                targetRoman = fallbackRoman,
                targetDevanagari = trimmed,
                confidence = 0.70f,
                latencyMs = latency,
                isExactMatch = false,
                matchedCategory = "Phonetic Translation"
            )

        } else {
            // Santali to Hindi translation
            val direct = santhaliToEntry[norm]
            if (direct != null) {
                val latency = System.currentTimeMillis() - startTime
                return TranslationResult(
                    sourceText = trimmed,
                    sourceLang = from,
                    targetLang = to,
                    targetOlChiki = direct.santhaliOlchiki,
                    targetRoman = direct.santhaliRoman,
                    targetDevanagari = direct.hindi,
                    confidence = 0.95f,
                    latencyMs = latency,
                    isExactMatch = true,
                    matchedCategory = direct.category
                )
            }

            for ((key, entry) in santhaliToEntry) {
                if (key.length >= 3 && (norm.startsWith(key) || norm.contains(key))) {
                    val latency = System.currentTimeMillis() - startTime
                    return TranslationResult(
                        sourceText = trimmed,
                        sourceLang = from,
                        targetLang = to,
                        targetOlChiki = entry.santhaliOlchiki,
                        targetRoman = entry.santhaliRoman,
                        targetDevanagari = entry.hindi,
                        confidence = 0.90f,
                        latencyMs = latency,
                        isExactMatch = true,
                        matchedCategory = entry.category
                    )
                }
            }

            // Multi-word composite translation for Santali
            val tokens = trimmed.split("\\s+".toRegex())
            if (tokens.size > 1) {
                val hindiWords = mutableListOf<String>()
                val olChikiWords = mutableListOf<String>()
                val romanWords = mutableListOf<String>()
                var matchCount = 0

                for (token in tokens) {
                    val normToken = normalize(token)
                    val match = santhaliToEntry[normToken]
                    if (match != null) {
                        hindiWords.add(match.hindi)
                        olChikiWords.add(match.santhaliOlchiki)
                        romanWords.add(match.santhaliRoman)
                        matchCount++
                    } else {
                        val deva = OlChikiTransliterator.toPhoneticDevanagari(token)
                        hindiWords.add(deva.ifEmpty { token })
                        olChikiWords.add(token)
                        romanWords.add(token)
                    }
                }

                val latency = System.currentTimeMillis() - startTime
                return TranslationResult(
                    sourceText = trimmed,
                    sourceLang = from,
                    targetLang = to,
                    targetOlChiki = olChikiWords.joinToString(" "),
                    targetRoman = romanWords.joinToString(" "),
                    targetDevanagari = hindiWords.joinToString(" "),
                    confidence = matchCount.toFloat() / tokens.size,
                    latencyMs = latency,
                    isExactMatch = matchCount == tokens.size,
                    matchedCategory = "Composite Santali"
                )
            }

            val phoneticDeva = OlChikiTransliterator.toPhoneticDevanagari(trimmed)
            val latency = System.currentTimeMillis() - startTime
            return TranslationResult(
                sourceText = trimmed,
                sourceLang = from,
                targetLang = to,
                targetOlChiki = trimmed,
                targetRoman = OlChikiTransliterator.toPhoneticRoman(trimmed),
                targetDevanagari = phoneticDeva.ifEmpty { trimmed },
                confidence = 0.65f,
                latencyMs = latency,
                isExactMatch = false,
                matchedCategory = "Phonetic Santali"
            )
        }
    }

    private fun findDirectClassroomPhrase(normInput: String): ClassroomPhrase? {
        for (cp in storedClassroomPhrases) {
            if (normalize(cp.canonicalHindi) == normInput) return cp
            for (v in cp.variants) {
                if (normalize(v) == normInput) return cp
            }
        }
        return null
    }

    private fun findFuzzyClassroomPhrase(normInput: String): ClassroomPhrase? {
        val words = normInput.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.isEmpty()) return null

        for (cp in storedClassroomPhrases) {
            val normCanonical = normalize(cp.canonicalHindi)
            if (normInput.contains(normCanonical)) {
                return cp
            }

            // Keyword pairs with word boundary safety
            if (words.any { it == "किताब" || it == "पुस्तक" || it == "कापी" || it == "कॉपी" }) {
                if (words.any { it.startsWith("खोल") || it.startsWith("निकाल") }) {
                    if (cp.canonicalHindi.contains("खोल")) return cp
                }
                if (words.any { it.startsWith("बंद") || it.startsWith("रख") }) {
                    if (cp.canonicalHindi.contains("बंद")) return cp
                }
            }

            if (words.any { it.startsWith("बैठ") } && cp.canonicalHindi.contains("बैठ")) {
                return cp
            }

            if (words.any { it.startsWith("खड़े") || it.startsWith("खड़ा") || it == "उठो" } && cp.canonicalHindi.contains("खड़े")) {
                return cp
            }

            if (words.any { it.startsWith("शांत") || it.startsWith("चुप") || it == "शोर" } && cp.canonicalHindi.contains("शांत")) {
                return cp
            }

            if (words.any { it.startsWith("सुन") || it == "ध्यान" } && cp.canonicalHindi.contains("सुनो")) {
                return cp
            }

            if (words.any { it == "डाउट" || it == "सवाल" || it == "प्रश्न" } && cp.canonicalHindi.contains("डाउट")) {
                return cp
            }

            if (words.contains("पानी") && words.any { it.startsWith("पी") || it.startsWith("चाहिए") } && cp.canonicalHindi.contains("पानी")) {
                return cp
            }

            if (words.any { it.startsWith("सुप्रभात") || it.startsWith("नमस्ते") || it.startsWith("प्रणाम") } && cp.canonicalHindi.contains("सुप्रभात")) {
                return cp
            }

            if (words.any { it.startsWith("धन्यवाद") || it.startsWith("शुक्रिया") } && cp.canonicalHindi.contains("धन्यवाद")) {
                return cp
            }

            if (words.any { it == "फिर" || it == "दोबारा" } && words.any { it.startsWith("बोल") || it.startsWith("बता") || it.startsWith("कह") } && cp.canonicalHindi.contains("फिर")) {
                return cp
            }

            if (words.any { it.startsWith("समझ") }) {
                if (words.any { it == "नहीं" || it == "ना" || it == "मत" } && cp.canonicalHindi.contains("नहीं")) {
                    return cp
                }
                if (words.any { it == "हाँ" || it == "हां" } && cp.canonicalHindi.contains("हाँ")) {
                    return cp
                }
                if (cp.canonicalHindi.contains("समझ में आया")) {
                    return cp
                }
            }

            if (words.any { it == "बोर्ड" || it == "श्यामपट्ट" } && cp.canonicalHindi.contains("बोर्ड")) {
                return cp
            }

            if (words.any { it.startsWith("लिख") } && cp.canonicalHindi.contains("लिखना")) {
                return cp
            }

            if (words.contains("होमवर्क") && cp.canonicalHindi.contains("होमवर्क")) {
                return cp
            }

            if (words.any { it.startsWith("पढ़") } && cp.canonicalHindi.contains("पढ़ रहा")) {
                return cp
            }

            if (words.any { it == "यहाँ" || it == "इधर" } && words.any { it.startsWith("आ") } && cp.canonicalHindi.contains("यहाँ आओ")) {
                return cp
            }

            if (words.any { it == "वहाँ" || it == "उधर" } && words.any { it.startsWith("जा") } && cp.canonicalHindi.contains("वहाँ जाओ")) {
                return cp
            }

            if (words.contains("हाथ") && words.any { it.startsWith("उठा") || it.startsWith("ऊपर") } && cp.canonicalHindi.contains("हाथ उठाओ")) {
                return cp
            }

            if (words.contains("नाम") && words.any { it == "क्या" || it.startsWith("बता") } && cp.canonicalHindi.contains("नाम क्या है")) {
                return cp
            }

            if (words.any { it == "कैसे" || it == "कैसी" } && cp.canonicalHindi.contains("आप कैसे हैं")) {
                return cp
            }
        }

        return null
    }

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("[?!.,।॥\"'()\\-_]+"), "")
            .trim()
    }
}
