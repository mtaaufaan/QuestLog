package com.rds.questlog.data.scraper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSplittingTest {

    @Test
    fun `paragraf pendek tidak dipecah`() {
        val text = "Satu. Dua. Tiga."
        assertEquals(listOf(text), splitParagraph(text))
    }

    @Test
    fun `paragraf lebih dari 400 karakter dan 3 kalimat dipecah di batas kalimat`() {
        val sentence = "Kalimat uji dengan panjang sedang yang cukup. "
        val chunks = splitParagraph(sentence.repeat(20).trim())
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 450 && it.endsWith(".") })
    }

    @Test
    fun `panjang tapi hanya sedikit kalimat tidak dipecah`() {
        val text = "a".repeat(500) + ". " + "b".repeat(200) + "."
        assertEquals(listOf(text), splitParagraph(text))
    }

    @Test
    fun `pre dipotong di batas baris dan tidak ada baris yang hilang`() {
        val lines = (1..300).map { "baris nomor $it dengan sedikit teks tambahan" }
        val chunks = chunkPreformatted(lines.joinToString("\n"), maxChars = 1000)
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 1000 })
        assertEquals(lines, chunks.flatMap { it.lines() })
    }

    @Test
    fun `pre mempertahankan indentasi dan membuang potongan kosong`() {
        assertEquals(listOf("  a\n    b"), chunkPreformatted("\n\n  a\n    b\n\n"))
        assertTrue(chunkPreformatted("   \n\n  ").isEmpty())
    }
}
