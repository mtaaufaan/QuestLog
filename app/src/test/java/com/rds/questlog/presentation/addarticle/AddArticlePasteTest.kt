package com.rds.questlog.presentation.addarticle

import org.junit.Assert.assertEquals
import org.junit.Test

/** QL-22: tombol Tempel menambahkan URL https:// dari clipboard tanpa menimpa isian yang ada. */
class AddArticlePasteTest {

    private fun urls(count: Int, from: Int = 1) = List(count) { "https://a.com/${it + from}" }

    @Test
    fun `multi URL dari clipboard terpecah per baris`() {
        val (form, outcome) = AddArticleForm().withPasted("https://a.com/1\nhttps://a.com/2\r\nhttps://a.com/3")

        assertEquals(urls(3), form.urls)
        assertEquals(PasteOutcome.Added(3), outcome)
    }

    @Test
    fun `URL diambil dari teks yang memuat kata lain atau dipisah koma`() {
        val (form, outcome) = AddArticleForm().withPasted("Walkthrough bagus: https://a.com/1, lalu https://a.com/2.")

        assertEquals(listOf("https://a.com/1", "https://a.com/2."), form.urls)
        assertEquals(PasteOutcome.Added(2), outcome)
    }

    @Test
    fun `URL yang sudah ada dilewati dan isian lama dipertahankan di depan`() {
        val start = AddArticleForm(urls = listOf("https://a.com/1", ""))

        val (form, outcome) = start.withPasted("https://a.com/1 https://a.com/2")

        assertEquals(urls(2), form.urls)
        assertEquals(PasteOutcome.Added(1), outcome)
    }

    @Test
    fun `hasil dibatasi 10 URL`() {
        val clipboard = urls(12).joinToString("\n")

        val (form, outcome) = AddArticleForm(urls = urls(2, from = 100)).withPasted(clipboard)

        assertEquals(MAX_URLS, form.urls.size)
        assertEquals(urls(2, from = 100), form.urls.take(2))
        assertEquals(PasteOutcome.Added(MAX_URLS - 2), outcome)
    }

    @Test
    fun `clipboard kosong atau tanpa URL https tidak mengubah form`() {
        val start = AddArticleForm(urls = listOf("https://a.com/1"))

        listOf(null, "", "   ", "bukan url", "http://tidak-aman.com/a", "ftp://a.com").forEach { text ->
            val (form, outcome) = start.withPasted(text)

            assertEquals(start, form)
            assertEquals(PasteOutcome.NoUrls, outcome)
        }
    }

    @Test
    fun `semua URL sudah ada atau daftar penuh berarti tidak ada yang baru`() {
        val full = AddArticleForm(urls = urls(MAX_URLS))

        assertEquals(PasteOutcome.NothingNew, full.withPasted("https://a.com/1 https://a.com/2").second)
        assertEquals(PasteOutcome.NothingNew, full.withPasted("https://baru.com/x").second)
        assertEquals(full, full.withPasted("https://baru.com/x").first)
    }
}
