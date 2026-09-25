package org.moolvani.app.engine

import org.moolvani.app.data.db.MoolVaniRepository
import org.moolvani.app.data.model.MatchingPair
import org.moolvani.app.data.model.QuestionType
import org.moolvani.app.data.model.WorksheetQuestion
import java.util.Collections

class WorksheetGenerator(private val repository: MoolVaniRepository) {

    fun generateWorksheet(count: Int = 8): List<WorksheetQuestion> {
        val courseItems = repository.getAllCourseItems().shuffled()
        val phrases = repository.getAllPhrases().shuffled()
        val questions = mutableListOf<WorksheetQuestion>()
        var qId = 1

        // 1. Picture-based questions
        val pictureItems = courseItems.filter { it.iconEmoji.isNotEmpty() && it.iconEmoji != "✨" }
        for (item in pictureItems.take(2)) {
            val distractors = courseItems.filter { it.id != item.id }.shuffled().take(3)
            val allOptions = (distractors.map { "${it.santhaliOlchiki} (${it.santhaliRoman})" } + "${item.santhaliOlchiki} (${item.santhaliRoman})").shuffled()
            val correctIdx = allOptions.indexOf("${item.santhaliOlchiki} (${item.santhaliRoman})")

            questions.add(
                WorksheetQuestion(
                    id = qId++,
                    type = QuestionType.PICTURE_BASED,
                    title = "चित्र पहचानिए (Picture Identification)",
                    promptHindi = "चित्र देखकर संथाली नाम चुनिए (${item.hindiText}):",
                    promptSanthali = "ᱪᱤᱛᱟᱹᱨ ᱧᱮᱞ ᱠᱟᱛᱮ ᱧᱩᱛᱩᱢ ᱵᱟᱪᱷᱟᱣ ᱢᱮ",
                    imageEmoji = item.iconEmoji,
                    options = allOptions,
                    correctIndex = correctIdx,
                    correctText = "${item.santhaliOlchiki} (${item.santhaliRoman})",
                    explanation = "${item.hindiText} को संथाली में '${item.santhaliOlchiki}' (${item.santhaliRoman}) कहते हैं।"
                )
            )
        }

        // 2. Multiple Choice Questions (Vocabulary & Meaning)
        for (item in courseItems.drop(2).take(2)) {
            val distractors = courseItems.filter { it.id != item.id }.shuffled().take(3)
            val allOptions = (distractors.map { "${it.santhaliOlchiki} (${it.santhaliRoman})" } + "${item.santhaliOlchiki} (${item.santhaliRoman})").shuffled()
            val correctIdx = allOptions.indexOf("${item.santhaliOlchiki} (${item.santhaliRoman})")

            questions.add(
                WorksheetQuestion(
                    id = qId++,
                    type = QuestionType.MCQ,
                    title = "बहुविकल्पीय प्रश्न (Multiple Choice)",
                    promptHindi = "'${item.hindiText}' का सही संथाली रूप क्या है?",
                    promptSanthali = "'${item.hindiText}' ᱨᱮᱭᱟᱜ ᱥᱟᱹᱨᱤ ᱥᱟᱱᱛᱟᱲᱤ ᱨᱩᱯ ᱫᱚ ᱪᱮᱫ?",
                    imageEmoji = item.iconEmoji,
                    options = allOptions,
                    correctIndex = correctIdx,
                    correctText = "${item.santhaliOlchiki} (${item.santhaliRoman})",
                    explanation = "'${item.hindiText}' -> '${item.santhaliOlchiki}' (${item.santhaliRoman})"
                )
            )
        }

        // 3. Fill in the Blank (Classroom Phrase completion)
        val fillPhrases = phrases.filter { it.santhaliOlchiki.split(" ").size >= 2 }.take(2)
        for (p in fillPhrases) {
            val words = p.santhaliOlchiki.split(" ")
            val hiddenWordIndex = (words.size - 2).coerceAtLeast(0)
            val missingWord = words[hiddenWordIndex]
            val prefix = words.subList(0, hiddenWordIndex).joinToString(" ")
            val suffix = words.subList(hiddenWordIndex + 1, words.size).joinToString(" ")

            val distractors = listOf("ᱡᱷᱤᱡᱽ", "ᱵᱚᱸᱫᱽ", "ᱫᱩᱲᱩᱵ", "ᱛᱤᱸᱜᱩᱱ", "ᱨᱚᱲ").filter { it != missingWord }.shuffled().take(3)
            val options = (distractors + missingWord).shuffled()
            val correctIdx = options.indexOf(missingWord)

            questions.add(
                WorksheetQuestion(
                    id = qId++,
                    type = QuestionType.FILL_BLANK,
                    title = "रिक्त स्थान भरिए (Fill in the Blank)",
                    promptHindi = "वाक्य पूरा कीजिए: '${p.hindiText}'",
                    promptSanthali = "ᱜᱟᱞᱟᱝ ᱯᱩᱨᱟᱹᱣ ᱢᱮ",
                    blankPrefix = prefix,
                    blankSuffix = suffix,
                    options = options,
                    correctIndex = correctIdx,
                    correctText = missingWord,
                    explanation = "सही वाक्य: ${p.santhaliOlchiki} (${p.santhaliRoman})"
                )
            )
        }

        // 4. Matching Pairs (4 pairs)
        val matchSubset = courseItems.shuffled().take(4)
        val pairs = matchSubset.map {
            MatchingPair(
                hindiText = it.hindiText,
                santhaliOlchiki = it.santhaliOlchiki,
                santhaliRoman = it.santhaliRoman
            )
        }
        questions.add(
            WorksheetQuestion(
                id = qId++,
                type = QuestionType.MATCHING,
                title = "जोड़ी मिलाइए (Match the Following)",
                promptHindi = "हिन्दी शब्दों को उनके सही संथाली (ओल चिकी) शब्दों से मिलाइए:",
                promptSanthali = "ᱦᱤᱱᱫᱤ ᱟᱹᱲᱟᱹ ᱠᱚ ᱥᱟᱱᱛᱟᱲᱤ ᱚᱞ ᱪᱤᱠᱤ ᱥᱟᱶ ᱡᱚᱲᱟᱣ ᱢᱮ",
                matchingPairs = pairs,
                explanation = "सभी शब्दों का सही संथाली मिलान पूरा हुआ!"
            )
        )

        return questions.take(count)
    }
}
