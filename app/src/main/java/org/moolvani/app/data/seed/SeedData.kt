package org.moolvani.app.data.seed

import org.moolvani.app.data.model.Course
import org.moolvani.app.data.model.CourseItem
import org.moolvani.app.data.model.Phrase

object SeedData {

    val initialPhrases = listOf(
        Phrase(
            hindiText = "किताब खोलिए",
            santhaliOlchiki = "ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ",
            santhaliRoman = "Potob jhij me",
            englishMeaning = "Open your book",
            category = "Classroom Commands"
        ),
        Phrase(
            hindiText = "कोई डाउट है?",
            santhaliOlchiki = "ᱡᱟᱦᱟᱸᱱᱟᱜ ᱠᱩᱠᱞᱤ ᱢᱮᱱᱟᱜᱼᱟ?",
            santhaliRoman = "Jahannag kukli menaga?",
            englishMeaning = "Any doubts / questions?",
            category = "Questions"
        ),
        Phrase(
            hindiText = "खड़े हो जाओ",
            santhaliOlchiki = "ᱛᱤᱸᱜᱩᱱ ᱢᱮ",
            santhaliRoman = "Tingun me",
            englishMeaning = "Stand up",
            category = "Classroom Commands"
        ),
        Phrase(
            hindiText = "बैठ जाओ",
            santhaliOlchiki = "ᱫᱩᱲᱩᱵ ᱢᱮ",
            santhaliRoman = "Durub me",
            englishMeaning = "Sit down",
            category = "Classroom Commands"
        ),
        Phrase(
            hindiText = "किताब बंद करो",
            santhaliOlchiki = "ᱯᱚᱛᱚᱵ ᱵᱚᱸᱫᱽ ᱢᱮ",
            santhaliRoman = "Potob bond me",
            englishMeaning = "Close your book",
            category = "Classroom Commands"
        ),
        Phrase(
            hindiText = "आप क्या कर रहे हैं?",
            santhaliOlchiki = "ᱟᱢ ᱪᱮᱫ ᱮᱢ ᱪᱮᱠᱟᱭᱮᱫᱟ?",
            santhaliRoman = "Am ched em chekayeda?",
            englishMeaning = "What are you doing?",
            category = "Questions"
        ),
        Phrase(
            hindiText = "मैं पढ़ रहा हूँ",
            santhaliOlchiki = "ᱤᱧ ᱯᱟᱲᱦᱟᱣᱜ ᱠᱟᱱᱟ",
            santhaliRoman = "Inj parhawg kana",
            englishMeaning = "I am reading",
            category = "Student Responses"
        ),
        Phrase(
            hindiText = "मैं होमवर्क कर रहा हूँ",
            santhaliOlchiki = "ᱤᱧ ᱚᱲᱟᱜ ᱠᱟᱹᱢᱤᱧ ᱠᱟᱹᱢᱤ ᱠᱟᱱᱟ",
            santhaliRoman = "Inj orag kaminj kami kana",
            englishMeaning = "I am doing homework",
            category = "Student Responses"
        ),
        Phrase(
            hindiText = "ध्यान से सुनो",
            santhaliOlchiki = "ᱫᱷᱮᱭᱟᱱ ᱛᱮ ᱟᱧᱡᱚᱢ ᱢᱮ",
            santhaliRoman = "Dheyan te anjom me",
            englishMeaning = "Listen carefully",
            category = "Classroom Commands"
        ),
        Phrase(
            hindiText = "फिर से बोलो",
            santhaliOlchiki = "ᱟᱨ ᱢᱤᱫᱫᱷᱟᱣ ᱨᱚᱲ ᱢᱮ",
            santhaliRoman = "Ar middhaw ror me",
            englishMeaning = "Say it again",
            category = "Classroom Commands"
        ),
        Phrase(
            hindiText = "समझ में आया?",
            santhaliOlchiki = "ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟᱢ?",
            santhaliRoman = "Bujhaw kedam?",
            englishMeaning = "Did you understand?",
            category = "Questions"
        ),
        Phrase(
            hindiText = "शांत रहिए",
            santhaliOlchiki = "ᱛᱷᱤᱨ ᱛᱟᱦᱮᱸᱱ ᱢᱮ",
            santhaliRoman = "Thir tahen me",
            englishMeaning = "Be quiet",
            category = "Classroom Commands"
        ),
        Phrase(
            hindiText = "ब्लैकबोर्ड पर देखिए",
            santhaliOlchiki = "ᱵᱳᱨᱰ ᱨᱮ ᱧᱮᱞ ᱢᱮ",
            santhaliRoman = "Board re nel me",
            englishMeaning = "Look at the blackboard",
            category = "Classroom Commands"
        ),
        Phrase(
            hindiText = "लिखना शुरू करो",
            santhaliOlchiki = "ᱚᱞ ᱮᱦᱚᱵ ᱢᱮ",
            santhaliRoman = "Ol ehob me",
            englishMeaning = "Start writing",
            category = "Classroom Commands"
        ),
        Phrase(
            hindiText = "पानी पीना है?",
            santhaliOlchiki = "ᱫᱟᱜ ᱧᱩ ᱥᱟᱱᱟᱭᱮᱫ ᱢᱮᱭᱟ?",
            santhaliRoman = "Dag nyu sanayed meya?",
            englishMeaning = "Do you want water?",
            category = "Questions"
        ),
        Phrase(
            hindiText = "सुप्रभात शिक्षक",
            santhaliOlchiki = "ᱡᱚᱦᱟᱨ ᱢᱟᱪᱮᱛ",
            santhaliRoman = "Johar Machet",
            englishMeaning = "Good morning teacher",
            category = "Greetings"
        ),
        Phrase(
            hindiText = "धन्यवाद",
            santhaliOlchiki = "ᱥᱟᱨᱦᱟᱣ",
            santhaliRoman = "Sarhaw",
            englishMeaning = "Thank you",
            category = "Greetings"
        ),
        Phrase(
            hindiText = "हाँ, मुझे समझ आ गया",
            santhaliOlchiki = "ᱦᱮᱸ, ᱤᱧ ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟ",
            santhaliRoman = "Hen, inj bujhaw keda",
            englishMeaning = "Yes, I understood",
            category = "Student Responses"
        ),
        Phrase(
            hindiText = "नहीं, मुझे समझ नहीं आया",
            santhaliOlchiki = "ᱵᱟᱝ, ᱤᱧ ᱵᱟᱹᱧ ᱵᱩᱡᱷᱟᱹᱣ ᱞᱮᱫᱟ",
            santhaliRoman = "Bang, inj banj bujhaw leda",
            englishMeaning = "No, I did not understand",
            category = "Student Responses"
        ),
        Phrase(
            hindiText = "हाथ उठाओ",
            santhaliOlchiki = "ᱛᱤ ᱛᱩᱞ ᱢᱮ",
            santhaliRoman = "Ti tul me",
            englishMeaning = "Raise your hand",
            category = "Classroom Commands"
        ),
        Phrase(
            hindiText = "यहाँ आओ",
            santhaliOlchiki = "ᱱᱚᱰᱮ ᱦᱤᱡᱩᱜ ᱢᱮ",
            santhaliRoman = "Node hijug me",
            englishMeaning = "Come here",
            category = "Classroom Commands"
        ),
        Phrase(
            hindiText = "वहाँ जाओ",
            santhaliOlchiki = "ᱦᱟᱱᱰᱮ ᱥᱮᱱᱚᱜ ᱢᱮ",
            santhaliRoman = "Hande senog me",
            englishMeaning = "Go there",
            category = "Classroom Commands"
        ),
        Phrase(
            hindiText = "आपका नाम क्या है?",
            santhaliOlchiki = "ᱟᱢᱟᱜ ᱧᱩᱛᱩᱢ ᱪᱮᱫ?",
            santhaliRoman = "Amag nyutum ched?",
            englishMeaning = "What is your name?",
            category = "Questions"
        ),
        Phrase(
            hindiText = "मेरा नाम",
            santhaliOlchiki = "ᱤᱧᱟᱜ ᱧᱩᱛᱩᱢ",
            santhaliRoman = "Injag nyutum",
            englishMeaning = "My name is",
            category = "Student Responses"
        ),
        Phrase(
            hindiText = "आप कैसे हैं?",
            santhaliOlchiki = "ᱟᱢ ᱪᱮᱫ ᱞᱮᱠᱟ ᱢᱮᱱᱟᱢᱟ?",
            santhaliRoman = "Am ched leka menama?",
            englishMeaning = "How are you?",
            category = "Questions"
        ),
        Phrase(
            hindiText = "बहुत अच्छा",
            santhaliOlchiki = "ᱟᱹᱰᱤ ᱵᱷᱟᱹᱜᱤ",
            santhaliRoman = "Adi bhagi",
            englishMeaning = "Very good",
            category = "Classroom Commands"
        ),
        Phrase(
            hindiText = "नमस्ते",
            santhaliOlchiki = "ᱡᱚᱦᱟᱨ",
            santhaliRoman = "Johar",
            englishMeaning = "Hello / Greetings",
            category = "Greetings"
        ),
        Phrase(
            hindiText = "हाँ",
            santhaliOlchiki = "ᱦᱮᱸ",
            santhaliRoman = "Hen",
            englishMeaning = "Yes",
            category = "Student Responses"
        ),
        Phrase(
            hindiText = "नहीं",
            santhaliOlchiki = "ᱵᱟᱝ",
            santhaliRoman = "Bang",
            englishMeaning = "No",
            category = "Student Responses"
        ),
        Phrase(
            hindiText = "पानी",
            santhaliOlchiki = "ᱫᱟᱜ",
            santhaliRoman = "Dag",
            englishMeaning = "Water",
            category = "Daily Environment"
        ),
        Phrase(
            hindiText = "खाना",
            santhaliOlchiki = "ᱡᱚᱢᱟᱜ",
            santhaliRoman = "Jomag",
            englishMeaning = "Food",
            category = "Daily Environment"
        ),
        Phrase(
            hindiText = "घर",
            santhaliOlchiki = "ᱚᱲᱟᱜ",
            santhaliRoman = "Orag",
            englishMeaning = "Home / House",
            category = "Daily Environment"
        ),
        Phrase(
            hindiText = "स्कूल",
            santhaliOlchiki = "ᱟᱥᱲᱟ",
            santhaliRoman = "Asra",
            englishMeaning = "School",
            category = "School"
        ),
        Phrase(
            hindiText = "दोस्त",
            santhaliOlchiki = "ᱜᱟᱛᱮ",
            santhaliRoman = "Gate",
            englishMeaning = "Friend",
            category = "School"
        )
    )

    val initialCourses = listOf(
        Course(
            code = "NUMBERS",
            titleEn = "Numbers",
            titleHi = "संख्याएँ (गिनती)",
            titleOlchiki = "ᱮᱞᱠᱷᱟ (Elkha)",
            description = "Learn counting 1 to 20 in Hindi and Santhali Ol Chiki",
            iconEmoji = "🔢",
            itemCount = 10
        ),
        Course(
            code = "ANIMALS",
            titleEn = "Animals",
            titleHi = "जानवर",
            titleOlchiki = "ᱡᱤᱭᱟᱹᱞᱤ (Jiyali)",
            description = "Elementary animal vocabulary with pictures and audio",
            iconEmoji = "🐘",
            itemCount = 10
        ),
        Course(
            code = "COLORS",
            titleEn = "Colors",
            titleHi = "रंग",
            titleOlchiki = "ᱨᱚᱝ (Rong)",
            description = "Explore primary and classroom colors in Santhali",
            iconEmoji = "🎨",
            itemCount = 8
        ),
        Course(
            code = "VEGETABLES",
            titleEn = "Vegetables",
            titleHi = "सब्जियाँ",
            titleOlchiki = "ᱩᱛᱩ ᱟᱲᱟᱜ (Utu Arag)",
            description = "Common vegetables and daily food items",
            iconEmoji = "🥦",
            itemCount = 8
        )
    )

    val initialCourseItems = listOf(
        // NUMBERS
        CourseItem(courseCode = "NUMBERS", hindiText = "एक (1)", santhaliOlchiki = "᱑ - ᱢᱤᱫ", santhaliRoman = "Mid", englishMeaning = "One (1)", iconEmoji = "1️⃣"),
        CourseItem(courseCode = "NUMBERS", hindiText = "दो (2)", santhaliOlchiki = "᱒ - ᱵᱟᱨ", santhaliRoman = "Bar", englishMeaning = "Two (2)", iconEmoji = "2️⃣"),
        CourseItem(courseCode = "NUMBERS", hindiText = "तीन (3)", santhaliOlchiki = "᱓ - ᱯᱮ", santhaliRoman = "Pe", englishMeaning = "Three (3)", iconEmoji = "3️⃣"),
        CourseItem(courseCode = "NUMBERS", hindiText = "चार (4)", santhaliOlchiki = "᱔ - ᱯᱩᱱ", santhaliRoman = "Pun", englishMeaning = "Four (4)", iconEmoji = "4️⃣"),
        CourseItem(courseCode = "NUMBERS", hindiText = "पाँच (5)", santhaliOlchiki = "᱕ - ᱢᱚᱬᱮ", santhaliRoman = "More", englishMeaning = "Five (5)", iconEmoji = "5️⃣"),
        CourseItem(courseCode = "NUMBERS", hindiText = "छह (6)", santhaliOlchiki = "᱖ - ᱛᱩᱨᱩᱭ", santhaliRoman = "Turuy", englishMeaning = "Six (6)", iconEmoji = "6️⃣"),
        CourseItem(courseCode = "NUMBERS", hindiText = "सात (7)", santhaliOlchiki = "᱗ - ᱮᱭᱟᱭ", santhaliRoman = "Eyay", englishMeaning = "Seven (7)", iconEmoji = "7️⃣"),
        CourseItem(courseCode = "NUMBERS", hindiText = "आठ (8)", santhaliOlchiki = "᱘ - ᱤᱨᱟᱹᱞ", santhaliRoman = "Iral", englishMeaning = "Eight (8)", iconEmoji = "8️⃣"),
        CourseItem(courseCode = "NUMBERS", hindiText = "नौ (9)", santhaliOlchiki = "᱙ - ᱟᱨᱮ", santhaliRoman = "Are", englishMeaning = "Nine (9)", iconEmoji = "9️⃣"),
        CourseItem(courseCode = "NUMBERS", hindiText = "दस (10)", santhaliOlchiki = "᱑᱐ - ᱜᱮᱞ", santhaliRoman = "Gel", englishMeaning = "Ten (10)", iconEmoji = "🔟"),

        // ANIMALS
        CourseItem(courseCode = "ANIMALS", hindiText = "गाय", santhaliOlchiki = "ᱜᱟᱹᱭ", santhaliRoman = "Gai", englishMeaning = "Cow", iconEmoji = "🐄"),
        CourseItem(courseCode = "ANIMALS", hindiText = "कुत्ता", santhaliOlchiki = "ᱥᱮᱛᱟ", santhaliRoman = "Seta", englishMeaning = "Dog", iconEmoji = "🐕"),
        CourseItem(courseCode = "ANIMALS", hindiText = "बिल्ली", santhaliOlchiki = "ᱯᱩᱥᱤ", santhaliRoman = "Pusi", englishMeaning = "Cat", iconEmoji = "🐈"),
        CourseItem(courseCode = "ANIMALS", hindiText = "हाथी", santhaliOlchiki = "ᱦᱟᱹᱛᱤ", santhaliRoman = "Hati", englishMeaning = "Elephant", iconEmoji = "🐘"),
        CourseItem(courseCode = "ANIMALS", hindiText = "बाघ", santhaliOlchiki = "ᱛᱟᱹᱨᱩᱵ", santhaliRoman = "Tarub", englishMeaning = "Tiger", iconEmoji = "🐅"),
        CourseItem(courseCode = "ANIMALS", hindiText = "बकरी", santhaliOlchiki = "ᱢᱮᱨᱚᱢ", santhaliRoman = "Merom", englishMeaning = "Goat", iconEmoji = "🐐"),
        CourseItem(courseCode = "ANIMALS", hindiText = "पक्षी", santhaliOlchiki = "ᱪᱮᱬᱮ", santhaliRoman = "Chene", englishMeaning = "Bird", iconEmoji = "🐦"),
        CourseItem(courseCode = "ANIMALS", hindiText = "घोड़ा", santhaliOlchiki = "ᱥᱟᱫᱚᱢ", santhaliRoman = "Sadom", englishMeaning = "Horse", iconEmoji = "🐎"),
        CourseItem(courseCode = "ANIMALS", hindiText = "मछली", santhaliOlchiki = "ᱦᱟᱹᱠᱩ", santhaliRoman = "Haku", englishMeaning = "Fish", iconEmoji = "🐟"),
        CourseItem(courseCode = "ANIMALS", hindiText = "शेर", santhaliOlchiki = "ᱠᱩᱞ", santhaliRoman = "Kul", englishMeaning = "Lion", iconEmoji = "🦁"),

        // COLORS
        CourseItem(courseCode = "COLORS", hindiText = "लाल", santhaliOlchiki = "ᱟᱨᱟᱜ", santhaliRoman = "Arag", englishMeaning = "Red", iconEmoji = "🔴"),
        CourseItem(courseCode = "COLORS", hindiText = "नीला", santhaliOlchiki = "ᱞᱤᱞ", santhaliRoman = "Lil", englishMeaning = "Blue", iconEmoji = "🔵"),
        CourseItem(courseCode = "COLORS", hindiText = "हरा", santhaliOlchiki = "ᱦᱟᱹᱨᱤᱭᱟᱹᱲ", santhaliRoman = "Hariyar", englishMeaning = "Green", iconEmoji = "🟢"),
        CourseItem(courseCode = "COLORS", hindiText = "पीला", santhaliOlchiki = "ᱥᱟᱥᱟᱝ", santhaliRoman = "Sasang", englishMeaning = "Yellow", iconEmoji = "🟡"),
        CourseItem(courseCode = "COLORS", hindiText = "सफेद", santhaliOlchiki = "ᱯᱩᱸᱰ", santhaliRoman = "Pund", englishMeaning = "White", iconEmoji = "⚪"),
        CourseItem(courseCode = "COLORS", hindiText = "काला", santhaliOlchiki = "ᱦᱮᱸᱫᱮ", santhaliRoman = "Hende", englishMeaning = "Black", iconEmoji = "⚫"),
        CourseItem(courseCode = "COLORS", hindiText = "नारंगी", santhaliOlchiki = "ᱥᱟᱥᱟᱝ ᱟᱨᱟᱜ", santhaliRoman = "Sasang Arag", englishMeaning = "Orange", iconEmoji = "🟠"),
        CourseItem(courseCode = "COLORS", hindiText = "भूरा", santhaliOlchiki = "ᱠᱷᱟᱹᱭᱨᱟ", santhaliRoman = "Khayra", englishMeaning = "Brown", iconEmoji = "🟤"),

        // VEGETABLES
        CourseItem(courseCode = "VEGETABLES", hindiText = "आलू", santhaliOlchiki = "ᱟᱹᱞᱩ", santhaliRoman = "Alu", englishMeaning = "Potato", iconEmoji = "🥔"),
        CourseItem(courseCode = "VEGETABLES", hindiText = "टमाटर", santhaliOlchiki = "ᱵᱤᱞᱟᱹᱛᱤ", santhaliRoman = "Bilati", englishMeaning = "Tomato", iconEmoji = "🍅"),
        CourseItem(courseCode = "VEGETABLES", hindiText = "प्याज", santhaliOlchiki = "ᱯᱮᱭᱟᱸᱡᱽ", santhaliRoman = "Peyanj", englishMeaning = "Onion", iconEmoji = "🧅"),
        CourseItem(courseCode = "VEGETABLES", hindiText = "बैंगन", santhaliOlchiki = "ᱵᱮᱝᱜᱟᱲ", santhaliRoman = "Bengar", englishMeaning = "Brinjal", iconEmoji = "🍆"),
        CourseItem(courseCode = "VEGETABLES", hindiText = "अदरक", santhaliOlchiki = "ᱟᱫᱟ", santhaliRoman = "Ada", englishMeaning = "Ginger", iconEmoji = "🫚"),
        CourseItem(courseCode = "VEGETABLES", hindiText = "मिर्च", santhaliOlchiki = "ᱢᱟᱹᱨᱤᱪ", santhaliRoman = "Marich", englishMeaning = "Chilli", iconEmoji = "🌶️"),
        CourseItem(courseCode = "VEGETABLES", hindiText = "लहसुन", santhaliOlchiki = "ᱨᱟᱹᱥᱩᱬ", santhaliRoman = "Rasun", englishMeaning = "Garlic", iconEmoji = "🧄"),
        CourseItem(courseCode = "VEGETABLES", hindiText = "कद्दू", santhaliOlchiki = "ᱦᱳᱛᱚᱛ", santhaliRoman = "Hotot", englishMeaning = "Pumpkin", iconEmoji = "🎃")
    )
}
