package com.rds.questlog.presentation.reader

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AutosaveNoteTest {

    @Test
    fun `teks sama dengan yang tersimpan tidak menyimpan lagi`() = runBlocking {
        var calls = 0

        val result = autosaveNote("a", "a", 1) { calls++ >= 0 }

        assertEquals("a", result)
        assertEquals(0, calls)
    }

    @Test
    fun `teks baru disimpan setelah jeda dan menjadi yang tersimpan`() = runBlocking {
        val saved = mutableListOf<String>()

        val result = autosaveNote("catatan", "", 1) {
            saved += it
            true
        }

        assertEquals("catatan", result)
        assertEquals(listOf("catatan"), saved)
    }

    @Test
    fun `gagal menyimpan mempertahankan yang tersimpan sehingga status tetap menyimpan`() = runBlocking {
        val result = autosaveNote("baru", "lama", 1) { false }

        assertEquals("lama", result)
    }
}
