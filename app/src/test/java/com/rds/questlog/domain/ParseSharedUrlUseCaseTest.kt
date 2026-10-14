package com.rds.questlog.domain

import com.rds.questlog.domain.usecase.article.ParseSharedUrlUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** QL-21: URL diekstrak dari beragam teks yang dibagikan browser dan aplikasi lain. */
class ParseSharedUrlUseCaseTest {

    private val parse = ParseSharedUrlUseCase()

    @Test
    fun `URL polos dan URL dengan judul di sampingnya`() {
        assertEquals(
            "https://gamefaqs.gamespot.com/ps/dq7/faqs/1",
            parse("https://gamefaqs.gamespot.com/ps/dq7/faqs/1"),
        )
        assertEquals(
            "https://gamefaqs.gamespot.com/ps/dq7/faqs/1",
            parse("Dragon Quest VII FAQ https://gamefaqs.gamespot.com/ps/dq7/faqs/1"),
        )
        assertEquals("https://a.com/x", parse("Judul halaman\nhttps://a.com/x\n"))
        assertEquals("https://a.com/x", parse("  \t https://a.com/x  "))
    }

    @Test
    fun `tanda baca di ujung dan pembungkus dibuang`() {
        assertEquals("https://a.com/x", parse("Lihat https://a.com/x."))
        assertEquals("https://a.com/x", parse("Lihat: https://a.com/x, ya!"))
        assertEquals("https://a.com/x", parse("(lihat https://a.com/x)"))
        assertEquals("https://a.com/x", parse("[https://a.com/x]"))
        assertEquals("https://a.com/x", parse("<https://a.com/x>"))
        assertEquals("https://a.com/x", parse("\"https://a.com/x\""))
        assertEquals("https://a.com/x", parse("“https://a.com/x”"))
    }

    @Test
    fun `kurung yang berpasangan di dalam URL dipertahankan`() {
        assertEquals("https://en.wikipedia.org/wiki/Foo_(bar)", parse("https://en.wikipedia.org/wiki/Foo_(bar)"))
        assertEquals(
            "https://en.wikipedia.org/wiki/Foo_(bar)",
            parse("(lihat https://en.wikipedia.org/wiki/Foo_(bar))"),
        )
        assertEquals("https://en.wikipedia.org/wiki/Foo_(bar)", parse("https://en.wikipedia.org/wiki/Foo_(bar)."))
    }

    @Test
    fun `query dan fragment dipertahankan`() {
        assertEquals("https://a.com/p?id=1&x=2#bagian", parse("https://a.com/p?id=1&x=2#bagian"))
        assertEquals("https://a.com/faq?page=2", parse("Hal 2: https://a.com/faq?page=2"))
    }

    @Test
    fun `yang diambil URL valid pertama`() {
        assertEquals("https://a.com/x", parse("https://a.com/x, https://b.com/y"))
        assertEquals("https://b.com/y", parse("https://localhost/x lalu https://b.com/y"))
        assertEquals("https://b.com/y", parse("http://a.com/x lalu https://b.com/y"))
    }

    @Test
    fun `skema huruf besar menjadi huruf kecil agar lolos validasi layar simpan`() {
        assertEquals("https://A.com/x", parse("HTTPS://A.com/x"))
        assertEquals("https://a.com/x", parse("Https://a.com/x"))
    }

    @Test
    fun `spasi tak terputus memisahkan URL dari teks lain`() {
        assertEquals("https://a.com/x", parse("judul\u00A0https://a.com/x\u00A0lanjut"))
    }

    @Test
    fun `tanpa URL https yang valid hasilnya null`() {
        listOf(
            null,
            "",
            "   ",
            "teks biasa tanpa tautan",
            "http://tidak-aman.com/a",
            "ftp://a.com/x",
            "https://",
            "https://localhost",
            "https://a",
            "www.a.com/x",
        ).forEach { text -> assertNull("teks: $text", parse(text)) }
    }
}
