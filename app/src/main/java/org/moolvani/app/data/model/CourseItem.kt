package org.moolvani.app.data.model

data class CourseItem(
    val id: Long = 0,
    val courseCode: String,
    val hindiText: String,
    val santhaliOlchiki: String,
    val santhaliRoman: String,
    val englishMeaning: String = "",
    val iconEmoji: String = "✨",
    val audioKey: String = ""
)
