package org.moolvani.app.data.model

enum class QuestionType {
    MCQ,
    MATCHING,
    FILL_BLANK,
    PICTURE_BASED
}

data class MatchingPair(
    val hindiText: String,
    val santhaliOlchiki: String,
    val santhaliRoman: String
)

data class WorksheetQuestion(
    val id: Int,
    val type: QuestionType,
    val title: String,
    val promptHindi: String,
    val promptSanthali: String = "",
    val imageEmoji: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1,
    val correctText: String = "",
    val matchingPairs: List<MatchingPair> = emptyList(),
    val blankPrefix: String = "",
    val blankSuffix: String = "",
    val explanation: String = ""
)
