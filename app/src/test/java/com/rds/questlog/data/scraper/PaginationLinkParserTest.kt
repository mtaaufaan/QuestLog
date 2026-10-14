package com.rds.questlog.data.scraper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** QL-34: fixture HTML untuk tiap jenis tautan lanjutan yang dikenali. */
class PaginationLinkParserTest {

    private val parser = PaginationLinkParser()

    private fun parse(html: String, base: String = "https://site.com/guide") =
        parser.parse("<html><head></head><body>$html</body></html>", base)

    @Test
    fun `rel next pada link di head dan pada tautan`() {
        val head = parser.parse(
            "<html><head><link rel=\"next\" href=\"/guide?page=2\"></head><body></body></html>",
            "https://site.com/guide",
        )
        assertEquals(listOf("https://site.com/guide?page=2"), head)

        assertEquals(
            listOf("https://site.com/guide/part-2"),
            parse("<a rel=\"next\" href=\"/guide/part-2\">lanjut baca</a>"),
        )
    }

    @Test
    fun `pola parameter page hanya yang lebih besar dari halaman sekarang dan di jalur yang sama`() {
        val html = """
            <a href="/guide?page=1">a</a><a href="/guide?page=2">b</a><a href="/guide?page=3">c</a>
            <a href="/guide?page=5">e</a><a href="/guide?page=4">d</a><a href="/lain?page=9">lain</a>
        """
        val result = parse(html, base = "https://site.com/guide?page=2")

        // Terurut menurut nomor halaman; halaman sekarang, yang lebih kecil, dan jalur lain tidak ikut.
        assertEquals(
            listOf(3, 4, 5).map { "https://site.com/guide?page=$it" },
            result,
        )
    }

    @Test
    fun `pola jalur slash page n`() {
        val html = "<a href=\"/blog/page/3/\">3</a><a href=\"/blog/page/2/\">2</a><a href=\"/blog/page/1/\">1</a>"

        assertEquals(
            listOf("https://site.com/blog/page/2/", "https://site.com/blog/page/3/"),
            parse(html, base = "https://site.com/blog/page/1/"),
        )
    }

    @Test
    fun `tautan teks Next dan Berikutnya`() {
        val html = "<a href=\"/faq/part2\">Next »</a><a href=\"/faq/part3\">Berikutnya</a>" +
            "<a href=\"/faq/x\">Tentang kami</a>"

        assertEquals(
            listOf("https://site.com/faq/part2", "https://site.com/faq/part3"),
            parse(html, base = "https://site.com/faq/part1"),
        )
    }

    @Test
    fun `daftar nomor halaman terurut menaik dan melewati halaman sekarang`() {
        val html = "<nav><a href=\"/faq/3\">3</a><a href=\"/faq/1\">1</a><a href=\"/faq/2\">2</a>" +
            "<a href=\"/faq/10\">10</a></nav>"

        assertEquals(
            listOf("https://site.com/faq/2", "https://site.com/faq/3", "https://site.com/faq/10"),
            parse(html, base = "https://site.com/faq/1"),
        )
    }

    @Test
    fun `urutan hasil rel next dulu, lalu nomor halaman menaik, lalu teks, tanpa duplikat`() {
        val html = """
            <head><link rel="next" href="/g?page=2"></head>
            <a href="/g?page=4">4</a><a href="/g?page=3">3</a><a href="/g?page=2">2</a>
            <a href="/g/lampiran">Berikutnya</a><a href="/g?page=2">Next</a>
        """
        val result = parser.parse("<html>$html<body></body></html>", "https://site.com/g?page=1")

        assertEquals(
            listOf(
                "https://site.com/g?page=2",
                "https://site.com/g?page=3",
                "https://site.com/g?page=4",
                "https://site.com/g/lampiran",
            ),
            result,
        )
    }

    @Test
    fun `fragmen dibuang dan tautan ke halaman sendiri atau jangkar saja diabaikan`() {
        val html = "<a href=\"#top\">1</a><a href=\"/guide#bagian\">2</a><a href=\"/guide?page=2#isi\">Next</a>"

        assertEquals(listOf("https://site.com/guide?page=2"), parse(html))
    }

    @Test
    fun `halaman tanpa tautan lanjutan menghasilkan daftar kosong`() {
        assertTrue(parse("<p>Hanya paragraf biasa.</p><a href=\"/tentang\">Tentang</a>").isEmpty())
        assertTrue(parser.parse("", "https://site.com/guide").isEmpty())
    }

    @Test
    fun `tautan lintas domain dan http dikembalikan apa adanya, penyaringan oleh use case`() {
        val html = "<a href=\"https://lain.com/guide?page=2\">Next</a>" +
            "<a href=\"http://site.com/guide?page=3\">Next page</a>"

        assertEquals(
            listOf("https://lain.com/guide?page=2", "http://site.com/guide?page=3"),
            parse(html),
        )
    }
}
