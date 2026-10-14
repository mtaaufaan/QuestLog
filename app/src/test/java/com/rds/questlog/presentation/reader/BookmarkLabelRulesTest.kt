package com.rds.questlog.presentation.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkLabelRulesTest {

    private val base = listOf("Peti", "Boss", "Mini Medal", "Sidequest", "Item")

    @Test
    fun `tanpa label terbaru hanya saran baku`() {
        val result = labelSuggestions(emptyList(), "")
        assertEquals(base, result.map { it.text })
        assertTrue(result.none { it.isRecent })
    }

    @Test
    fun `label terbaru maksimal tiga, di depan, dan tidak diulang di saran baku`() {
        val result = labelSuggestions(listOf("Boss Dharma", "peti", "", "A", "B"), "")
        assertEquals(
            listOf("Boss Dharma", "peti", "A", "Boss", "Mini Medal", "Sidequest", "Item"),
            result.map { it.text },
        )
        assertEquals(listOf(true, true, true, false, false, false, false), result.map { it.isRecent })
    }

    @Test
    fun `chip terpilih bila sama dengan isian tanpa memandang huruf besar`() {
        assertEquals(listOf("Boss"), labelSuggestions(emptyList(), " boss ").filter { it.selected }.map { it.text })
    }

    @Test
    fun `ketuk saran mengganti isian kosong atau isian yang hanya saran`() {
        val boss = labelSuggestions(emptyList(), "").first { it.text == "Boss" }
        assertEquals("Boss", applySuggestion(boss, "", emptyList()))
        assertEquals("Boss", applySuggestion(boss, "Peti", emptyList()))
        assertEquals("Boss", applySuggestion(boss, "Kemarin", listOf("Kemarin")))
    }

    @Test
    fun `ketuk saran menambahkan ke isian bebas dan dipotong ke batas`() {
        val boss = labelSuggestions(emptyList(), "").first { it.text == "Boss" }
        assertEquals("Dharma — Boss", applySuggestion(boss, "Dharma", emptyList()))
        val long = "x".repeat(MAX_BOOKMARK_LABEL)
        assertEquals(MAX_BOOKMARK_LABEL, applySuggestion(boss, long, emptyList()).length)
        assertFalse(applySuggestion(boss, long, emptyList()).endsWith("Boss"))
    }
}
