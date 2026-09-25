package org.moolvani.app.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.moolvani.app.data.model.Course
import org.moolvani.app.data.model.CourseItem
import org.moolvani.app.data.model.Phrase
import org.moolvani.app.data.seed.SeedData

class MoolVaniDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "moolvani_classroom.db"
        private const val DATABASE_VERSION = 1

        // Phrases Table
        const val TABLE_PHRASES = "phrases"
        const val COL_PHRASE_ID = "_id"
        const val COL_HINDI_TEXT = "hindi_text"
        const val COL_SANTHALI_OLCHIKI = "santhali_olchiki"
        const val COL_SANTHALI_ROMAN = "santhali_roman"
        const val COL_ENGLISH_MEANING = "english_meaning"
        const val COL_CATEGORY = "category"
        const val COL_IS_CUSTOM = "is_custom"
        const val COL_CREATED_AT = "created_at"

        // Courses Table
        const val TABLE_COURSES = "courses"
        const val COL_COURSE_ID = "_id"
        const val COL_COURSE_CODE = "code"
        const val COL_TITLE_EN = "title_en"
        const val COL_TITLE_HI = "title_hi"
        const val COL_TITLE_OLCHIKI = "title_olchiki"
        const val COL_DESC = "description"
        const val COL_ICON = "icon_emoji"

        // Course Items Table
        const val TABLE_COURSE_ITEMS = "course_items"
        const val COL_ITEM_ID = "_id"
        const val COL_ITEM_COURSE_CODE = "course_code"
        const val COL_ITEM_HINDI = "hindi_text"
        const val COL_ITEM_OLCHIKI = "santhali_olchiki"
        const val COL_ITEM_ROMAN = "santhali_roman"
        const val COL_ITEM_ENGLISH = "english_meaning"
        const val COL_ITEM_ICON = "icon_emoji"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_PHRASES (
                $COL_PHRASE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_HINDI_TEXT TEXT NOT NULL,
                $COL_SANTHALI_OLCHIKI TEXT NOT NULL,
                $COL_SANTHALI_ROMAN TEXT NOT NULL,
                $COL_ENGLISH_MEANING TEXT,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_IS_CUSTOM INTEGER DEFAULT 0,
                $COL_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_COURSES (
                $COL_COURSE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_COURSE_CODE TEXT UNIQUE NOT NULL,
                $COL_TITLE_EN TEXT NOT NULL,
                $COL_TITLE_HI TEXT NOT NULL,
                $COL_TITLE_OLCHIKI TEXT NOT NULL,
                $COL_DESC TEXT,
                $COL_ICON TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_COURSE_ITEMS (
                $COL_ITEM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ITEM_COURSE_CODE TEXT NOT NULL,
                $COL_ITEM_HINDI TEXT NOT NULL,
                $COL_ITEM_OLCHIKI TEXT NOT NULL,
                $COL_ITEM_ROMAN TEXT NOT NULL,
                $COL_ITEM_ENGLISH TEXT,
                $COL_ITEM_ICON TEXT,
                FOREIGN KEY ($COL_ITEM_COURSE_CODE) REFERENCES $TABLE_COURSES($COL_COURSE_CODE)
            )
        """.trimIndent())

        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COURSE_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COURSES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PHRASES")
        onCreate(db)
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            // Seed phrases
            for (p in SeedData.initialPhrases) {
                val cv = ContentValues().apply {
                    put(COL_HINDI_TEXT, p.hindiText)
                    put(COL_SANTHALI_OLCHIKI, p.santhaliOlchiki)
                    put(COL_SANTHALI_ROMAN, p.santhaliRoman)
                    put(COL_ENGLISH_MEANING, p.englishMeaning)
                    put(COL_CATEGORY, p.category)
                    put(COL_IS_CUSTOM, if (p.isCustom) 1 else 0)
                    put(COL_CREATED_AT, p.createdAt)
                }
                db.insert(TABLE_PHRASES, null, cv)
            }

            // Seed courses
            for (c in SeedData.initialCourses) {
                val cv = ContentValues().apply {
                    put(COL_COURSE_CODE, c.code)
                    put(COL_TITLE_EN, c.titleEn)
                    put(COL_TITLE_HI, c.titleHi)
                    put(COL_TITLE_OLCHIKI, c.titleOlchiki)
                    put(COL_DESC, c.description)
                    put(COL_ICON, c.iconEmoji)
                }
                db.insert(TABLE_COURSES, null, cv)
            }

            // Seed course items
            for (item in SeedData.initialCourseItems) {
                val cv = ContentValues().apply {
                    put(COL_ITEM_COURSE_CODE, item.courseCode)
                    put(COL_ITEM_HINDI, item.hindiText)
                    put(COL_ITEM_OLCHIKI, item.santhaliOlchiki)
                    put(COL_ITEM_ROMAN, item.santhaliRoman)
                    put(COL_ITEM_ENGLISH, item.englishMeaning)
                    put(COL_ITEM_ICON, item.iconEmoji)
                }
                db.insert(TABLE_COURSE_ITEMS, null, cv)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
