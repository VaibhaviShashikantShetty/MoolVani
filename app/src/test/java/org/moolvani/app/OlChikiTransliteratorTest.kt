package org.moolvani.app

import org.junit.Assert.*
import org.junit.Test
import org.moolvani.app.engine.OlChikiTransliterator

class OlChikiTransliteratorTest {

    @Test
    fun testIsOlChikiDetection() {
        assertTrue(OlChikiTransliterator.isOlChiki("ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ"))
        assertTrue(OlChikiTransliterator.isOlChiki("ᱥᱟᱱᱛᱟᱲᱤ"))
        assertFalse(OlChikiTransliterator.isOlChiki("किताब खोलिए"))
        assertFalse(OlChikiTransliterator.isOlChiki("Potob jhij me"))
    }

    @Test
    fun testDigitsConversion() {
        val converted = OlChikiTransliterator.toOlChiki("1 2 3")
        assertTrue(converted.contains("᱑"))
        assertTrue(converted.contains("᱒"))
        assertTrue(converted.contains("᱓"))
    }

    @Test
    fun testPhoneticDevanagariConversion() {
        val bookOpen = OlChikiTransliterator.toPhoneticDevanagari("ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ")
        assertEquals("पोतोब झीज मे", bookOpen)

        val elephant = OlChikiTransliterator.toPhoneticDevanagari("ᱦᱟᱹᱛᱤ")
        assertEquals("हाती", elephant)
    }

    @Test
    fun testRomanSanthaliToPhoneticDevanagari() {
        // Words used in CourseDetail, Flashcards, and Phrases
        assertEquals("पोतोब झीज मे", OlChikiTransliterator.toPhoneticDevanagari("Potob jhij me"))
        assertEquals("मिद", OlChikiTransliterator.toPhoneticDevanagari("Mid"))
        assertEquals("बार", OlChikiTransliterator.toPhoneticDevanagari("Bar"))
        assertEquals("गई", OlChikiTransliterator.toPhoneticDevanagari("Gai"))
        assertEquals("सेता", OlChikiTransliterator.toPhoneticDevanagari("Seta"))
        assertEquals("जोहार माचेत", OlChikiTransliterator.toPhoneticDevanagari("Johar Machet"))
        assertEquals("सारहाव", OlChikiTransliterator.toPhoneticDevanagari("Sarhaw"))
    }

    @Test
    fun testSyllabicMatraCombiningForUnseenOlChiki() {
        // Test that consonant + vowel produces matras instead of independent vowels
        // ᱥ + ᱮ + ᱛ + ᱟ -> सेता
        val setaConverted = OlChikiTransliterator.toPhoneticDevanagari("ᱥᱮᱛᱟ")
        assertEquals("सेता", setaConverted)

        // ᱢ + ᱤ + ᱫ -> मिद
        val midConverted = OlChikiTransliterator.toPhoneticDevanagari("ᱢᱤᱫ")
        assertEquals("मिद", midConverted)
    }

    @Test
    fun testEnglishTtsFallbackConversion() {
        val romanBook = OlChikiTransliterator.toPhoneticRoman("ᱯᱚᱛᱚᱵ ᱡᱷᱤᱡᱽ ᱢᱮ")
        assertEquals("Potob jhij me", romanBook)

        val romanDog = OlChikiTransliterator.toPhoneticRoman("ᱥᱮᱛᱟ")
        assertEquals("Seta", romanDog)
    }
}
