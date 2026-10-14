package com.rds.questlog.presentation.addarticle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddArticleDetectTest {

    private val base = "https://site.com/guide?page=1"

    private fun urls(n: Int) = List(n) { "https://site.com/guide?page=${it + 1}" }

    @Test
    fun `sumber pemeriksaan adalah URL valid pertama`() {
        assertEquals(base, detectBaseUrl(listOf("", "bukan url", " $base ", "https://lain.com/x1")))
        assertNull(detectBaseUrl(listOf("", "http://tanpa-https.com/1", "site")))
    }

    @Test
    fun `sisa slot dari sepuluh URL dan tombol aktif hanya bila ada URL, slot, dan tidak memeriksa`() {
        assertEquals(7, detectSlots(AddArticleForm(urls = urls(3))))
        assertEquals(0, detectSlots(AddArticleForm(urls = urls(10))))

        assertTrue(canDetect(premium = true, base = base, slots = 2, state = DetectState.Idle))
        assertFalse(canDetect(premium = true, base = null, slots = 2, state = DetectState.Idle))
        assertFalse(canDetect(premium = true, base = base, slots = 0, state = DetectState.Idle))
        assertFalse(canDetect(premium = true, base = base, slots = 2, state = DetectState.Loading))
        // Gratis tetap bisa diketuk (membuka Unlock Sheet) meski tanpa URL.
        assertTrue(canDetect(premium = false, base = null, slots = 0, state = DetectState.Idle))
    }

    @Test
    fun `hasil awal mencentang sebanyak sisa slot dan membuang URL yang sudah ada`() {
        val found = urls(6).drop(1)

        val state = foundState(found, known = setOf(found[0]), slots = 3) as DetectState.Found

        assertEquals(found.drop(1), state.urls)
        assertEquals(found.drop(1).take(3).toSet(), state.selected)
    }

    @Test
    fun `tidak ada URL baru berarti tidak ada hasil`() {
        assertEquals(DetectState.NoResults, foundState(emptyList(), emptySet(), 5))
        assertEquals(DetectState.NoResults, foundState(urls(2), known = urls(2).toSet(), slots = 5))
    }

    @Test
    fun `centang berlebih diabaikan dan melepas centang membuka slot`() {
        val all = urls(5)
        var state = foundState(all, emptySet(), slots = 2) as DetectState.Found
        assertEquals(all.take(2).toSet(), state.selected)

        assertEquals(state, toggleDetected(state, all[3], slots = 2))

        state = toggleDetected(state, all[0], slots = 2)
        state = toggleDetected(state, all[3], slots = 2)
        assertEquals(setOf(all[1], all[3]), state.selected)
    }

    @Test
    fun `menambahkan URL tercentang ke form tanpa duplikat dan maksimal sepuluh`() {
        val form = AddArticleForm(urls = listOf(urls(1)[0], "", urls(2)[1]))
        val found = DetectState.Found(urls(5).drop(2), selected = urls(5).drop(2).toSet() + urls(1)[0])

        val result = applyDetected(form, found)

        assertEquals(urls(5), result.urls)
        val big = applyDetected(
            AddArticleForm(urls = urls(8)),
            DetectState.Found(List(5) { "https://x.com/$it" }, List(5) { "https://x.com/$it" }.toSet()),
        )
        assertEquals(MAX_URLS, big.urls.size)
    }

    @Test
    fun `sumber dummy menaikkan angka di ujung URL tanpa jaringan`() {
        assertEquals(
            listOf(2, 3, 4, 5, 6, 7).map { "https://site.com/guide?page=$it" },
            dummyContinuationUrls(base),
        )
        assertEquals(
            "https://site.com/faq/walkthrough/4/",
            dummyContinuationUrls("https://site.com/faq/walkthrough/3/").first(),
        )
        assertTrue(dummyContinuationUrls("https://site.com/guide").isEmpty())
    }
}
