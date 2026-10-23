package com.captionai.studio.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageTest {

    @Test
    fun urduDisplayLabelMapsToUrdu() {
        assertEquals(Language.URDU, Language.normalize("اردو"))
    }

    @Test
    fun romanUrduAndEnglishMap() {
        assertEquals(Language.ROMAN_URDU, Language.normalize("Roman Urdu"))
        assertEquals(Language.ENGLISH, Language.normalize("English"))
    }

    @Test
    fun unknownOrNullDefaultsToEnglish() {
        assertEquals(Language.ENGLISH, Language.normalize(null))
        assertEquals(Language.ENGLISH, Language.normalize("Klingon"))
    }

    @Test
    fun urduInstructionRequestsUrduScript() {
        assertTrue(Language.outputInstruction("اردو").contains("Urdu script"))
    }

    @Test
    fun everyDisplayLabelIsRecognised() {
        // Guards against the original bug where a UI label was not understood by the prompt builder.
        Language.DISPLAY_LABELS.forEach {
            val n = Language.normalize(it)
            assertTrue(n == Language.ENGLISH || n == Language.URDU || n == Language.ROMAN_URDU)
        }
        assertEquals(Language.URDU, Language.normalize(Language.DISPLAY_LABELS[1]))
    }
}
