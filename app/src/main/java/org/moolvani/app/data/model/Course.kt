package org.moolvani.app.data.model

data class Course(
    val id: Long = 0,
    val code: String,
    val titleEn: String,
    val titleHi: String,
    val titleOlchiki: String,
    val description: String = "",
    val iconEmoji: String = "📚",
    val itemCount: Int = 0
)
