package org.moolvani.app.engine

object OlChikiTransliterator {

    private val romanToOlChiki = mapOf(
        // Digits
        "0" to "᱐", "1" to "᱑", "2" to "᱒", "3" to "᱓", "4" to "᱔",
        "5" to "᱕", "6" to "᱖", "7" to "᱗", "8" to "᱘", "9" to "᱙",

        // Digraphs & special syllables
        "kh" to "ᱠᱷ", "gh" to "ᱜᱷ", "ch" to "ᱪ", "jh" to "ᱡᱷ", "th" to "ᱛᱷ", "dh" to "ᱫᱷ",
        "ph" to "ᱯᱷ", "bh" to "ᱵᱷ", "sh" to "ᱥ", "ng" to "ᱝ", "ny" to "ᱧ", "nj" to "ᱧ",
        "rr" to "ᱲ", "nn" to "ᱬ", "tt" to "ᱴ", "dd" to "ᱰ",

        // Single vowels
        "a" to "ᱟ", "e" to "ᱮ", "i" to "ᱤ", "o" to "ᱳ", "u" to "ᱩ",

        // Consonants
        "k" to "ᱠ", "g" to "ᱜ", "c" to "ᱪ", "j" to "ᱡ", "t" to "ᱛ", "d" to "ᱫ",
        "n" to "ᱱ", "p" to "ᱯ", "b" to "ᱵ", "m" to "ᱢ", "y" to "ᱭ", "r" to "ᱨ",
        "l" to "ᱞ", "v" to "ᱣ", "w" to "ᱣ", "s" to "ᱥ", "h" to "ᱦ"
    )

    private val devanagariToOlChiki = mapOf(
        "०" to "᱐", "१" to "᱑", "२" to "᱒", "३" to "᱓", "४" to "᱔",
        "५" to "᱕", "६" to "᱖", "७" to "᱗", "८" to "᱘", "९" to "᱙",
        "अ" to "ᱚ", "आ" to "ᱟ", "इ" to "ᱤ", "ई" to "ᱤ", "उ" to "ᱩ", "ऊ" to "ᱩ",
        "ए" to "ᱮ", "ऐ" to "ᱮ", "ओ" to "ᱳ", "औ" to "ᱳ",
        "क" to "ᱠ", "ख" to "ᱠᱷ", "ग" to "ᱜ", "घ" to "ᱜᱷ", "ङ" to "ᱝ",
        "च" to "ᱪ", "छ" to "ᱪᱷ", "ज" to "ᱡ", "झ" to "ᱡᱷ", "ञ" to "ᱧ",
        "ट" to "ᱴ", "ठ" to "ᱴᱷ", "ड" to "ᱰ", "ढ" to "ᱰᱷ", "ण" to "ᱬ",
        "त" to "ᱛ", "थ" to "ᱛᱷ", "द" to "ᱫ", "ध" to "ᱫᱷ", "न" to "ᱱ",
        "प" to "ᱯ", "फ" to "ᱯᱷ", "ब" to "ᱵ", "भ" to "ᱵᱷ", "म" to "ᱢ",
        "य" to "ᱭ", "र" to "ᱨ", "ल" to "ᱞ", "व" to "ᱣ",
        "श" to "ᱥ", "ष" to "ᱥ", "स" to "ᱥ", "ह" to "ᱦ", "ड़" to "ᱲ"
    )

    // Master dictionary for authentic spoken voice: Ol Chiki & Roman -> Devanagari phonetics
    private val santhaliVoiceDictionary = mapOf(
        // Classroom Commands
        "ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ" to "पोतोब झीज मे",
        "potob jhij me" to "पोतोब झीज मे",
        "ᱡᱟᱦᱟᱸᱱᱟᱜ ᱠᱩᱠᱞᱤ ᱢᱮᱱᱟᱜᱼᱟ?" to "जहाँनाग कुकली मेनागा?",
        "jahannag kukli menaga?" to "जहाँनाग कुकली मेनागा?",
        "jahannag kukli menaga" to "जहाँनाग कुकली मेनागा?",
        "ᱛᱤᱸᱜᱩᱱ ᱢᱮ" to "तिंगुन मे",
        "tingun me" to "तिंगुन मे",
        "ᱫᱩᱲᱩᱵ ᱢᱮ" to "दुरुब मे",
        "durub me" to "दुरुब मे",
        "ᱯᱚᱛᱚᱵ ᱵᱚᱸᱫᱽ ᱢᱮ" to "पोतोब बोंद मे",
        "potob bond me" to "पोतोब बोंद मे",
        "ᱟᱢ ᱪᱮᱫ ᱮᱢ ᱪᱮᱠᱟᱭᱮᱫᱟ?" to "आम चेद एम चेकायेदा?",
        "am ched em chekayeda?" to "आम चेद एम चेकायेदा?",
        "am ched em chekayeda" to "आम चेद एम चेकायेदा?",
        "ᱤᱧ ᱯᱟᱲᱦᱟᱣᱜ ᱠᱟᱱᱟ" to "इंज पाड़हावग काना",
        "inj parhawg kana" to "इंज पाड़हावग काना",
        "ᱤᱧ ᱚᱲᱟᱜ ᱠᱟᱹᱢᱤᱧ ᱠᱟᱹᱢᱤ ᱠᱟᱱᱟ" to "इंज ओड़ाग कामिंज कामी काना",
        "inj orag kaminj kami kana" to "इंज ओड़ाग कामिंज कामी काना",
        "ᱫᱷᱮᱭᱟᱱ ᱛᱮ ᱟᱧᱡᱚᱢ ᱢᱮ" to "धेयान ते आंजोम मे",
        "dheyan te anjom me" to "धेयान ते आंजोम मे",
        "ᱟᱨ ᱢᱤᱫᱫᱷᱟᱣ ᱨᱚᱲ ᱢᱮ" to "आर मिदधाव रोड़ मे",
        "ar middhaw ror me" to "आर मिदधाव रोड़ मे",
        "ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟᱢ?" to "बुझाव केदाम?",
        "bujhaw kedam?" to "बुझाव केदाम?",
        "bujhaw kedam" to "बुझाव केदाम?",
        "ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱢᱮ" to "थिर ताहेन मे",
        "thir tahen me" to "थिर ताहेन मे",
        "ᱵᱳᱨᱰ ᱨᱮ ᱧᱮᱞ ᱢᱮ" to "बोर्ड रे ञेल मे",
        "board re nel me" to "बोर्ड रे ञेल मे",
        "ᱚᱞ ᱮᱦᱚᱵ ᱢᱮ" to "ओल एहोब मे",
        "ol ehob me" to "ओल एहोब मे",
        "ᱫᱟᱜ ᱧᱩ ᱥᱟᱱᱟᱭᱮᱫ ᱢᱮᱭᱟ?" to "दाग ञु सानायेद मेया?",
        "dag nyu sanayed meya?" to "दाग ञु सानायेद मेया?",
        "dag nyu sanayed meya" to "दाग ञु सानायेद मेया?",
        "ᱡᱚᱦᱟᱨ ᱢᱟᱪᱮᱛ" to "जोहार माचेत",
        "johar machet" to "जोहार माचेत",
        "ᱥᱟᱨᱦᱟᱣ" to "सारहाव",
        "sarhaw" to "सारहाव",
        "ᱦᱮᱸ, ᱤᱧ ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟ" to "हें, इंज बुझाव केदा",
        "hen, inj bujhaw keda" to "हें, इंज बुझाव केदा",
        "ᱵᱟᱝ, ᱤᱧ ᱵᱟᱹᱧ ᱵᱩᱡᱷᱟᱹᱣ ᱞᱮᱫᱟ" to "बांग, इंज बांज बुझाव लेदा",
        "bang, inj banj bujhaw leda" to "बांग, इंज बांज बुझाव लेदा",

        // Numbers (Ol Chiki and Roman)
        "᱑ - ᱢᱤᱫ" to "मिद", "ᱢᱤᱫ" to "मिद", "mid" to "मिद",
        "᱒ - ᱵᱟᱨ" to "बार", "ᱵᱟᱨ" to "बार", "bar" to "बार",
        "᱓ - ᱯᱮ" to "पे", "ᱯᱮ" to "पे", "pe" to "पे",
        "᱔ - ᱯᱩᱱ" to "पुन", "ᱯᱩᱱ" to "पुन", "pun" to "पुन",
        "᱕ - ᱢᱚᱬᱮ" to "मोड़े", "ᱢᱚᱬᱮ" to "मोड़े", "more" to "मोड़े",
        "᱖ - ᱛᱩᱨᱩᱭ" to "तुरुय", "ᱛᱩᱨᱩᱭ" to "तुरुय", "turuy" to "तुरुय",
        "᱗ - ᱮᱭᱟᱭ" to "एयाय", "ᱮᱭᱟᱭ" to "एयाय", "eyay" to "एयाय",
        "᱘ - ᱤᱨᱟᱹᱞ" to "इरल", "ᱤᱨᱟᱹᱞ" to "इरल", "iral" to "इरल",
        "᱙ - ᱟᱨᱮ" to "आरे", "ᱟᱨᱮ" to "आरे", "are" to "आरे",
        "᱑᱐ - ᱜᱮᱞ" to "गेल", "ᱜᱮᱞ" to "गेल", "gel" to "गेल",

        // Animals
        "ᱜᱟᱹᱭ" to "गई", "gai" to "गई",
        "ᱥᱮᱛᱟ" to "सेता", "seta" to "सेता",
        "ᱯᱩᱥᱤ" to "पुसी", "pusi" to "पुसी",
        "ᱦᱟᱹᱛᱤ" to "हाती", "hati" to "हाती",
        "ᱛᱟᱹᱨᱩᱵ" to "तारुब", "tarub" to "तारुब",
        "ᱢᱮᱨᱚᱢ" to "मेरम", "merom" to "मेरम",
        "ᱪᱮᱬᱮ" to "चेणे", "chene" to "चेणे",
        "ᱥᱟᱫᱚᱢ" to "सादम", "sadom" to "सादम",
        "ᱦᱟᱹᱠᱩ" to "हाकु", "haku" to "हाकु",
        "ᱠᱩᱞ" to "कुल", "kul" to "कुल",

        // Colors
        "ᱟᱨᱟᱜ" to "आराग", "arag" to "आराग",
        "ᱞᱤᱞ" to "लील", "lil" to "लील",
        "ᱦᱟᱹᱨᱤᱭᱟᱹᱲ" to "हारियाड़", "hariyar" to "हारियाड़",
        "ᱥᱟᱥᱟᱝ" to "सासांग", "sasang" to "सासांग",
        "ᱯᱩᱸᱰ" to "पुंड", "pund" to "पुंड",
        "ᱦᱮᱸᱫᱮ" to "हेंदे", "hende" to "हेंदे",
        "ᱥᱟᱥᱟᱝ ᱟᱨᱟᱜ" to "सासांग आराग", "sasang arag" to "सासांग आराग",
        "ᱠᱷᱟᱹᱭᱨᱟ" to "खायरा", "khayra" to "खायरा",

        // Vegetables
        "ᱟᱹᱞᱩ" to "आलू", "alu" to "आलू",
        "ᱵᱤᱞᱟᱹᱛᱤ" to "बिलाती", "bilati" to "बिलाती",
        "ᱯᱮᱭᱟᱸᱡᱽ" to "पेयांज", "peyanj" to "पेयांज",
        "ᱵᱮᱝᱜᱟᱲ" to "बेंगाड़", "bengar" to "बेंगाड़",
        "ᱟᱫᱟ" to "आदा", "ada" to "आदा",
        "ᱢᱟᱹᱨᱤᱪ" to "मारिच", "marich" to "मारिच",
        "ᱨᱟᱹᱥᱩᱬ" to "रासुण", "rasun" to "रासुण",
        "ᱦᱳᱛᱚᱛ" to "होतोत", "hotot" to "होतोत",

        // Additional Classroom vocabulary
        "ᱫᱟᱜ" to "दाग", "dag" to "दाग",
        "ᱡᱚᱢᱟᱜ" to "जोमाग", "jomag" to "जोमाग",
        "ᱜᱟᱛᱮ" to "गाते", "gate" to "गाते",
        "ᱢᱟᱪᱮᱛ" to "माचेत", "machet" to "माचेत",
        "ᱯᱟᱹᱴᱷᱩᱣᱟᱹ" to "पाटुवा", "pathuwa" to "पाटुवा",
        "ᱟᱥᱲᱟ" to "आसड़ा", "asra" to "आसड़ा",
        "ᱠᱚᱞᱚᱢ" to "कलम", "kalam" to "कलम",
        "ᱠᱷᱟᱛᱟ" to "खाता", "khata" to "खाता",
        "ᱚᱲᱟᱜ" to "ओड़ाग", "orag" to "ओड़ाग",
        "ᱦᱮᱸ" to "हें", "hen" to "हें",
        "ᱵᱟᱝ" to "बांग", "bang" to "बांग",
        "ᱵᱷᱟᱹᱜᱤ" to "भागी", "bhagi" to "भागी",
        "ᱟᱹᱰᱤ ᱵᱷᱟᱹᱜᱤ" to "आडी भागी", "adi bhagi" to "आडी भागी",
        "ᱡᱚᱦᱟᱨ" to "जोहार", "johar" to "जोहार"
    )

    // Reverse dictionary for English TTS voice: Ol Chiki -> Roman English phonetics
    private val olChikiToRomanVoiceMap = mapOf(
        "ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ" to "Potob jhij me",
        "ᱡᱟᱦᱟᱸᱱᱟᱜ ᱠᱩᱠᱞᱤ ᱢᱮᱱᱟᱜᱼᱟ?" to "Jahannag kukli menaga?",
        "ᱛᱤᱸᱜᱩᱱ ᱢᱮ" to "Tingun me",
        "ᱫᱩᱲᱩᱵ ᱢᱮ" to "Durub me",
        "ᱯᱚᱛᱚᱵ ᱵᱚᱸᱫᱽ ᱢᱮ" to "Potob bond me",
        "ᱟᱢ ᱪᱮᱫ ᱮᱢ ᱪᱮᱠᱟᱭᱮᱫᱟ?" to "Am ched em chekayeda?",
        "ᱤᱧ ᱯᱟᱲᱦᱟᱣᱜ ᱠᱟᱱᱟ" to "Inj parhawg kana",
        "ᱤᱧ ᱚᱲᱟᱜ ᱠᱟᱹᱢᱤᱧ ᱠᱟᱹᱢᱤ ᱠᱟᱱᱟ" to "Inj orag kaminj kami kana",
        "ᱫᱷᱮᱭᱟᱱ ᱛᱮ ᱟᱧᱡᱚᱢ ᱢᱮ" to "Dheyan te anjom me",
        "ᱟᱨ ᱢᱤᱫᱫᱷᱟᱣ ᱨᱚᱲ ᱢᱮ" to "Ar middhaw ror me",
        "ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟᱢ?" to "Bujhaw kedam?",
        "ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱢᱮ" to "Thir tahen me",
        "ᱵᱳᱨᱰ ᱨᱮ ᱧᱮᱞ ᱢᱮ" to "Board re nel me",
        "ᱚᱞ ᱮᱦᱚᱵ ᱢᱮ" to "Ol ehob me",
        "ᱫᱟᱜ ᱧᱩ ᱥᱟᱱᱟᱭᱮᱫ ᱢᱮᱭᱟ?" to "Dag nyu sanayed meya?",
        "ᱡᱚᱦᱟᱨ ᱢᱟᱪᱮᱛ" to "Johar Machet",
        "ᱥᱟᱨᱦᱟᱣ" to "Sarhaw",
        "ᱦᱮᱸ, ᱤᱧ ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟ" to "Hen, inj bujhaw keda",
        "ᱵᱟᱝ, ᱤᱧ ᱵᱟᱹᱧ ᱵᱩᱡᱷᱟᱹᱣ ᱞᱮᱫᱟ" to "Bang, inj banj bujhaw leda",

        // Numbers
        "᱑ - ᱢᱤᱫ" to "Mid", "ᱢᱤᱫ" to "Mid",
        "᱒ - ᱵᱟᱨ" to "Bar", "ᱵᱟᱨ" to "Bar",
        "᱓ - ᱯᱮ" to "Pe", "ᱯᱮ" to "Pe",
        "᱔ - ᱯᱩᱱ" to "Pun", "ᱯᱩᱱ" to "Pun",
        "᱕ - ᱢᱚᱬᱮ" to "More", "ᱢᱚᱬᱮ" to "More",
        "᱖ - ᱛᱩᱨᱩᱭ" to "Turuy", "ᱛᱩᱨᱩᱭ" to "Turuy",
        "᱗ - ᱮᱭᱟᱭ" to "Eyay", "ᱮᱭᱟᱭ" to "Eyay",
        "᱘ - ᱤᱨᱟᱹᱞ" to "Iral", "ᱤᱨᱟᱹᱞ" to "Iral",
        "᱙ - ᱟᱨᱮ" to "Are", "ᱟᱨᱮ" to "Are",
        "᱑᱐ - ᱜᱮᱞ" to "Gel", "ᱜᱮᱞ" to "Gel",

        // Animals
        "ᱜᱟᱹᱭ" to "Gai", "ᱥᱮᱛᱟ" to "Seta", "ᱯᱩᱥᱤ" to "Pusi", "ᱦᱟᱹᱛᱤ" to "Hati",
        "ᱛᱟᱹᱨᱩᱵ" to "Tarub", "ᱢᱮᱨᱚᱢ" to "Merom", "ᱪᱮᱬᱮ" to "Chene", "ᱥᱟᱫᱚᱢ" to "Sadom",
        "ᱦᱟᱹᱠᱩ" to "Haku", "ᱠᱩᱞ" to "Kul",

        // Colors
        "ᱟᱨᱟᱜ" to "Arag", "ᱞᱤᱞ" to "Lil", "ᱦᱟᱹᱨᱤᱭᱟᱹᱲ" to "Hariyar", "ᱥᱟᱥᱟᱝ" to "Sasang",
        "ᱯᱩᱸᱰ" to "Pund", "ᱦᱮᱸᱫᱮ" to "Hende", "ᱥᱟᱥᱟᱝ ᱟᱨᱟᱜ" to "Sasang Arag", "ᱠᱷᱟᱹᱭᱨᱟ" to "Khayra",

        // Vegetables
        "ᱟᱹᱞᱩ" to "Alu", "ᱵᱤᱞᱟᱹᱛᱤ" to "Bilati", "ᱯᱮᱭᱟᱸᱡᱽ" to "Peyanj", "ᱵᱮᱝᱜᱟᱲ" to "Bengar",
        "ᱟᱫᱟ" to "Ada", "ᱢᱟᱹᱨᱤᱪ" to "Marich", "ᱨᱟᱹᱥᱩᱬ" to "Rasun", "ᱦᱳᱛᱚᱛ" to "Hotot",

        // Vocabulary
        "ᱫᱟᱜ" to "Dag", "ᱡᱚᱢᱟᱜ" to "Jomag", "ᱜᱟᱛᱮ" to "Gate", "ᱢᱟᱪᱮᱛ" to "Machet",
        "ᱯᱟᱹᱴᱷᱩᱣᱟᱹ" to "Pathuwa", "ᱟᱥᱲᱟ" to "Asra", "ᱠᱚᱞᱚᱢ" to "Kalam", "ᱠᱷᱟᱛᱟ" to "Khata",
        "ᱚᱲᱟᱜ" to "Orag", "ᱦᱮᱸ" to "Hen", "ᱵᱟᱝ" to "Bang", "ᱵᱷᱟᱹᱜᱤ" to "Bhagi",
        "ᱟᱹᱰᱤ ᱵᱷᱟᱹᱜᱤ" to "Adi bhagi", "ᱡᱚᱦᱟᱨ" to "Johar"
    )

    fun toOlChiki(input: String): String {
        var res = input
        val sortedRoman = romanToOlChiki.entries.sortedByDescending { it.key.length }
        for ((key, value) in sortedRoman) {
            res = res.replace(Regex("(?i)\\b$key\\b"), value)
        }
        for ((key, value) in devanagariToOlChiki) {
            res = res.replace(key, value)
        }
        return res
    }

    /**
     * Converts any Santhali text (Ol Chiki or Roman) into clean, natural
     * phonetic Devanagari text with correct vowel matras so the Android Speech Synthesis
     * engine speaks authentic, fluid human speech.
     */
    fun toPhoneticDevanagari(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""

        // 1. Direct dictionary lookup (exact match or lowercased)
        santhaliVoiceDictionary[trimmed]?.let { return it }
        santhaliVoiceDictionary[trimmed.lowercase()]?.let { return it }

        // Strip numbering like "1 - Mid" -> "Mid"
        val cleanRomanNumber = trimmed.replace(Regex("^[0-9]+\\s*-\\s*"), "").trim()
        santhaliVoiceDictionary[cleanRomanNumber.lowercase()]?.let { return it }

        // 2. If it is already pure Hindi/Devanagari, return as is
        if (!isOlChiki(trimmed) && trimmed.matches(Regex("[\\u0900-\\u097F\\s\\p{Punct}0-9]+"))) {
            return trimmed
        }

        // 3. If Ol Chiki, convert using syllable matra combining
        if (isOlChiki(trimmed)) {
            return convertOlChikiToDevanagariSyllables(trimmed)
        }

        // 4. Roman Santhali fallback to Devanagari phonetics
        return convertRomanToDevanagariPhonetics(trimmed)
    }

    /**
     * Converts Santhali text (Ol Chiki or Devanagari) to clean Roman English phonetics
     * for devices where only English TTS voice is installed.
     */
    fun toPhoneticRoman(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""

        olChikiToRomanVoiceMap[trimmed]?.let { return it }
        val cleanNumber = trimmed.replace(Regex("^[᱐-᱙0-9]+\\s*-\\s*"), "").trim()
        olChikiToRomanVoiceMap[cleanNumber]?.let { return it }

        if (isOlChiki(trimmed)) {
            return convertOlChikiToRoman(trimmed)
        }

        if (trimmed.matches(Regex("[\\u0900-\\u097F\\s\\p{Punct}0-9]+"))) {
            return devanagariToRoman(trimmed)
        }

        // Already Roman
        return trimmed
    }

    private fun convertOlChikiToDevanagariSyllables(text: String): String {
        val vowelIndependent = mapOf(
            'ᱚ' to "ओ", 'ᱟ' to "आ", 'ᱤ' to "इ", 'ᱩ' to "उ", 'ᱮ' to "ए", 'ᱳ' to "ओ"
        )
        val vowelMatra = mapOf(
            'ᱚ' to "ो", 'ᱟ' to "ा", 'ᱤ' to "ि", 'ᱩ' to "ु", 'ᱮ' to "े", 'ᱳ' to "ो"
        )
        val singleConsonants = mapOf(
            'ᱛ' to "त", 'ᱜ' to "ग", 'ᱝ' to "ंग", 'ᱞ' to "ल", 'ᱠ' to "क", 'ᱡ' to "ज",
            'ᱢ' to "म", 'ᱣ' to "व", 'ᱥ' to "स", 'ᱦ' to "ह", 'ᱧ' to "ञ", 'ᱨ' to "र",
            'ᱪ' to "च", 'ᱫ' to "द", 'ᱬ' to "ण", 'ᱭ' to "य", 'ᱯ' to "प", 'ᱰ' to "ड",
            'ᱱ' to "न", 'ᱲ' to "ड़", 'ᱴ' to "ट", 'ᱵ' to "ब", 'ᱶ' to "व", 'ᱷ' to "ह"
        )
        val digits = mapOf(
            '᱐' to "०", '᱑' to "१", '᱒' to "२", '᱓' to "३", '᱔' to "४",
            '᱕' to "५", '᱖' to "६", '᱗' to "७", '᱘' to "८", '᱙' to "९"
        )

        val sb = StringBuilder()
        var i = 0
        var prevWasConsonant = false

        while (i < text.length) {
            // Check 2-character digraphs first: ᱠᱷ, ᱜᱷ, ᱪᱷ, ᱡᱷ, ᱛᱷ, ᱫᱷ, ᱯᱷ, ᱵᱷ, ᱴᱷ, ᱰᱷ
            if (i + 1 < text.length) {
                val pair = text.substring(i, i + 2)
                val devaDigraph = when (pair) {
                    "ᱠᱷ" -> "ख"
                    "ᱜᱷ" -> "घ"
                    "ᱪᱷ" -> "छ"
                    "ᱡᱷ" -> "झ"
                    "ᱛᱷ" -> "थ"
                    "ᱫᱷ" -> "ध"
                    "ᱯᱷ" -> "फ"
                    "ᱵᱷ" -> "भ"
                    "ᱴᱷ" -> "ठ"
                    "ᱰᱷ" -> "ढ"
                    else -> null
                }
                if (devaDigraph != null) {
                    sb.append(devaDigraph)
                    prevWasConsonant = true
                    i += 2
                    continue
                }
            }

            val ch = text[i]
            when {
                digits.containsKey(ch) -> {
                    sb.append(digits[ch])
                    prevWasConsonant = false
                    i++
                }
                vowelMatra.containsKey(ch) -> {
                    if (prevWasConsonant) {
                        sb.append(vowelMatra[ch])
                    } else {
                        sb.append(vowelIndependent[ch])
                    }
                    prevWasConsonant = false
                    i++
                }
                singleConsonants.containsKey(ch) -> {
                    sb.append(singleConsonants[ch])
                    prevWasConsonant = true
                    i++
                }
                ch == 'ᱸ' -> {
                    sb.append("ं")
                    prevWasConsonant = false
                    i++
                }
                ch in listOf('ᱹ', 'ᱺ', 'ᱼ', 'ᱽ') -> {
                    // Diacritical modifiers
                    i++
                }
                else -> {
                    sb.append(ch)
                    prevWasConsonant = false
                    i++
                }
            }
        }
        return sb.toString()
    }

    private fun convertOlChikiToRoman(text: String): String {
        val olToRoman = mapOf(
            "᱐" to "0", "᱑" to "1", "᱒" to "2", "᱓" to "3", "᱔" to "4",
            "᱕" to "5", "᱖" to "6", "᱗" to "7", "᱘" to "8", "᱙" to "9",
            "ᱠᱷ" to "kh", "ᱜᱷ" to "gh", "ᱪᱷ" to "chh", "ᱡᱷ" to "jh", "ᱛᱷ" to "th", "ᱫᱷ" to "dh",
            "ᱯᱷ" to "ph", "ᱵᱷ" to "bh", "ᱴᱷ" to "th", "ᱰᱷ" to "dh",
            "ᱚ" to "o", "ᱛ" to "t", "ᱜ" to "g", "ᱝ" to "ng", "ᱞ" to "l",
            "ᱟ" to "a", "ᱠ" to "k", "ᱡ" to "j", "ᱢ" to "m", "ᱣ" to "w",
            "ᱤ" to "i", "ᱥ" to "s", "ᱦ" to "h", "ᱧ" to "ny", "ᱨ" to "r",
            "ᱩ" to "u", "ᱪ" to "ch", "ᱫ" to "d", "ᱬ" to "n", "ᱭ" to "y",
            "ᱮ" to "e", "ᱯ" to "p", "ᱰ" to "d", "ᱱ" to "n", "ᱲ" to "r",
            "ᱳ" to "o", "ᱴ" to "t", "ᱵ" to "b", "ᱶ" to "v", "ᱷ" to "h",
            "ᱸ" to "n", "ᱹ" to "", "ᱺ" to "", "ᱼ" to "", "ᱽ" to ""
        )

        var result = text
        val sortedKeys = olToRoman.keys.sortedByDescending { it.length }
        for (k in sortedKeys) {
            result = result.replace(k, olToRoman[k] ?: "")
        }
        return result
    }

    private fun convertRomanToDevanagariPhonetics(roman: String): String {
        // Simple token phonetic replacement for words
        val words = roman.split("\\s+".toRegex())
        val converted = words.map { word ->
            val clean = word.lowercase().replace(Regex("[^a-z]"), "")
            santhaliVoiceDictionary[clean] ?: word
        }
        return converted.joinToString(" ")
    }

    fun devanagariToRoman(text: String): String {
        val mapping = mapOf(
            "ख" to "kh", "घ" to "gh", "छ" to "chh", "झ" to "jh", "ठ" to "th", "ढ" to "dh",
            "थ" to "th", "ध" to "dh", "फ" to "ph", "भ" to "bh", "श" to "sh", "ष" to "sh",
            "क" to "k", "ग" to "g", "च" to "ch", "ज" to "j", "ट" to "t", "ड" to "d",
            "ण" to "n", "त" to "t", "द" to "d", "न" to "n", "प" to "p", "ब" to "b",
            "म" to "m", "य" to "y", "र" to "r", "ल" to "l", "व" to "v", "स" to "s",
            "ह" to "h", "ड़" to "r", "ढ़" to "rh",
            "ा" to "a", "ि" to "i", "ी" to "ee", "ु" to "u", "ू" to "oo",
            "े" to "e", "ै" to "ai", "ो" to "o", "ौ" to "au", "ं" to "n", "्" to "",
            "अ" to "a", "आ" to "aa", "इ" to "i", "ई" to "ee", "उ" to "u", "ऊ" to "oo",
            "ए" to "e", "ऐ" to "ai", "ओ" to "o", "औ" to "au"
        )
        var result = text
        val sortedKeys = mapping.keys.sortedByDescending { it.length }
        for (k in sortedKeys) {
            result = result.replace(k, mapping[k] ?: "")
        }
        return result
    }

    fun isOlChiki(text: String): Boolean {
        for (char in text) {
            val code = char.code
            if (code in 0x1C50..0x1C7F) return true
        }
        return false
    }
}
