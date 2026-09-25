package org.moolvani.app.data.model

data class Phrase(
    val id: Long = 0,
    val hindiText: String,
    val santhaliOlchiki: String,
    val santhaliRoman: String,
    val englishMeaning: String = "",
    val category: String = "Classroom Commands",
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
