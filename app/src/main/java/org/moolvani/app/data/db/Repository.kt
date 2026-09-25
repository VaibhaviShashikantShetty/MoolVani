package org.moolvani.app.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import org.moolvani.app.data.model.*
import java.util.Collections

class MoolVaniRepository(context: Context) {

    private val dbHelper = MoolVaniDbHelper(context.applicationContext)

    fun getAllPhrases(): List<Phrase> {
        val list = mutableListOf<Phrase>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            MoolVaniDbHelper.TABLE_PHRASES,
            null,
            null,
            null,
            null,
            null,
            "${MoolVaniDbHelper.COL_PHRASE_ID} ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToPhrase(it))
            }
        }
        return list
    }

    fun getPhrasesByCategory(category: String): List<Phrase> {
        if (category.equals("All", ignoreCase = true)) {
            return getAllPhrases()
        }
        val list = mutableListOf<Phrase>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            MoolVaniDbHelper.TABLE_PHRASES,
            null,
            "${MoolVaniDbHelper.COL_CATEGORY} = ?",
            arrayOf(category),
            null,
            null,
            "${MoolVaniDbHelper.COL_PHRASE_ID} ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToPhrase(it))
            }
        }
        return list
    }

    fun searchPhrases(query: String): List<Phrase> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return getAllPhrases()

        val list = mutableListOf<Phrase>()
        val db = dbHelper.readableDatabase
        val wild = "%$trimmed%"
        val cursor = db.query(
            MoolVaniDbHelper.TABLE_PHRASES,
            null,
            "${MoolVaniDbHelper.COL_HINDI_TEXT} LIKE ? OR ${MoolVaniDbHelper.COL_SANTHALI_OLCHIKI} LIKE ? OR ${MoolVaniDbHelper.COL_SANTHALI_ROMAN} LIKE ?",
            arrayOf(wild, wild, wild),
            null,
            null,
            "${MoolVaniDbHelper.COL_PHRASE_ID} ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToPhrase(it))
            }
        }
        return list
    }

    fun insertPhrase(phrase: Phrase): Long {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put(MoolVaniDbHelper.COL_HINDI_TEXT, phrase.hindiText)
            put(MoolVaniDbHelper.COL_SANTHALI_OLCHIKI, phrase.santhaliOlchiki)
            put(MoolVaniDbHelper.COL_SANTHALI_ROMAN, phrase.santhaliRoman)
            put(MoolVaniDbHelper.COL_ENGLISH_MEANING, phrase.englishMeaning)
            put(MoolVaniDbHelper.COL_CATEGORY, phrase.category)
            put(MoolVaniDbHelper.COL_IS_CUSTOM, if (phrase.isCustom) 1 else 0)
            put(MoolVaniDbHelper.COL_CREATED_AT, phrase.createdAt)
        }
        return db.insert(MoolVaniDbHelper.TABLE_PHRASES, null, cv)
    }

    fun deletePhrase(id: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(MoolVaniDbHelper.TABLE_PHRASES, "${MoolVaniDbHelper.COL_PHRASE_ID} = ?", arrayOf(id.toString()))
    }

    fun getAllCourses(): List<Course> {
        val list = mutableListOf<Course>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            MoolVaniDbHelper.TABLE_COURSES,
            null,
            null,
            null,
            null,
            null,
            "${MoolVaniDbHelper.COL_COURSE_ID} ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val code = it.getString(it.getColumnIndexOrThrow(MoolVaniDbHelper.COL_COURSE_CODE))
                val count = getItemCountForCourse(db, code)
                list.add(
                    Course(
                        id = it.getLong(it.getColumnIndexOrThrow(MoolVaniDbHelper.COL_COURSE_ID)),
                        code = code,
                        titleEn = it.getString(it.getColumnIndexOrThrow(MoolVaniDbHelper.COL_TITLE_EN)),
                        titleHi = it.getString(it.getColumnIndexOrThrow(MoolVaniDbHelper.COL_TITLE_HI)),
                        titleOlchiki = it.getString(it.getColumnIndexOrThrow(MoolVaniDbHelper.COL_TITLE_OLCHIKI)),
                        description = it.getString(it.getColumnIndexOrThrow(MoolVaniDbHelper.COL_DESC)) ?: "",
                        iconEmoji = it.getString(it.getColumnIndexOrThrow(MoolVaniDbHelper.COL_ICON)) ?: "📚",
                        itemCount = count
                    )
                )
            }
        }
        return list
    }

    private fun getItemCountForCourse(db: android.database.sqlite.SQLiteDatabase, code: String): Int {
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ${MoolVaniDbHelper.TABLE_COURSE_ITEMS} WHERE ${MoolVaniDbHelper.COL_ITEM_COURSE_CODE} = ?",
            arrayOf(code)
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }
        return 0
    }

    fun getCourseItems(courseCode: String): List<CourseItem> {
        val list = mutableListOf<CourseItem>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            MoolVaniDbHelper.TABLE_COURSE_ITEMS,
            null,
            "${MoolVaniDbHelper.COL_ITEM_COURSE_CODE} = ?",
            arrayOf(courseCode),
            null,
            null,
            "${MoolVaniDbHelper.COL_ITEM_ID} ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToCourseItem(it))
            }
        }
        return list
    }

    fun insertCourseItem(item: CourseItem): Long {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put(MoolVaniDbHelper.COL_ITEM_COURSE_CODE, item.courseCode)
            put(MoolVaniDbHelper.COL_ITEM_HINDI, item.hindiText)
            put(MoolVaniDbHelper.COL_ITEM_OLCHIKI, item.santhaliOlchiki)
            put(MoolVaniDbHelper.COL_ITEM_ROMAN, item.santhaliRoman)
            put(MoolVaniDbHelper.COL_ITEM_ENGLISH, item.englishMeaning)
            put(MoolVaniDbHelper.COL_ITEM_ICON, item.iconEmoji)
        }
        return db.insert(MoolVaniDbHelper.TABLE_COURSE_ITEMS, null, cv)
    }

    fun getAllCourseItems(): List<CourseItem> {
        val list = mutableListOf<CourseItem>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            MoolVaniDbHelper.TABLE_COURSE_ITEMS,
            null,
            null,
            null,
            null,
            null,
            "${MoolVaniDbHelper.COL_ITEM_ID} ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToCourseItem(it))
            }
        }
        return list
    }

    private fun cursorToPhrase(cursor: Cursor): Phrase {
        return Phrase(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(MoolVaniDbHelper.COL_PHRASE_ID)),
            hindiText = cursor.getString(cursor.getColumnIndexOrThrow(MoolVaniDbHelper.COL_HINDI_TEXT)),
            santhaliOlchiki = cursor.getString(cursor.getColumnIndexOrThrow(MoolVaniDbHelper.COL_SANTHALI_OLCHIKI)),
            santhaliRoman = cursor.getString(cursor.getColumnIndexOrThrow(MoolVaniDbHelper.COL_SANTHALI_ROMAN)),
            englishMeaning = cursor.getString(cursor.getColumnIndexOrThrow(MoolVaniDbHelper.COL_ENGLISH_MEANING)) ?: "",
            category = cursor.getString(cursor.getColumnIndexOrThrow(MoolVaniDbHelper.COL_CATEGORY)),
            isCustom = cursor.getInt(cursor.getColumnIndexOrThrow(MoolVaniDbHelper.COL_IS_CUSTOM)) == 1,
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(MoolVaniDbHelper.COL_CREATED_AT))
        )
    }

    private fun cursorToCourseItem(cursor: Cursor): CourseItem {
        return CourseItem(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(MoolVaniDbHelper.COL_ITEM_ID)),
            courseCode = cursor.getString(cursor.getColumnIndexOrThrow(MoolVaniDbHelper.COL_ITEM_COURSE_CODE)),
            hindiText = cursor.getString(cursor.getColumnIndexOrThrow(MoolVaniDbHelper.COL_ITEM_HINDI)),
            santhaliOlchiki = cursor.getString(cursor.getColumnIndexOrThrow(MoolVaniDbHelper.COL_ITEM_OLCHIKI)),
            santhaliRoman = cursor.getString(cursor.getColumnIndexOrThrow(MoolVaniDbHelper.COL_ITEM_ROMAN)),
            englishMeaning = cursor.getString(cursor.getColumnIndexOrThrow(MoolVaniDbHelper.COL_ITEM_ENGLISH)) ?: "",
            iconEmoji = cursor.getString(cursor.getColumnIndexOrThrow(MoolVaniDbHelper.COL_ITEM_ICON)) ?: "✨"
        )
    }
}
