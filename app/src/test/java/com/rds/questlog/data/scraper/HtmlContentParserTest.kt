package com.rds.questlog.data.scraper

import com.rds.questlog.domain.model.InlineMarkup
import com.rds.questlog.domain.model.NodeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlContentParserTest {

    private val boldOn = InlineMarkup.BOLD_ON
    private val boldOff = InlineMarkup.BOLD_OFF
    private val italicOn = InlineMarkup.ITALIC_ON
    private val italicOff = InlineMarkup.ITALIC_OFF

    private val parser = HtmlContentParser()
    private val base = "https://example.com/wiki/page"

    private fun parse(html: String) = parser.parse(html, base)

    @Test
    fun `iklan navigasi komentar footer dan skrip dibuang, konten utama dipertahankan`() {
        val nodes = parse(
            """
            <html><body>
              <nav>Menu Home About</nav>
              <header><h1>Header Situs</h1></header>
              <div class="ad">BELI SEKARANG</div>
              <article>
                <header><h1>Judul Artikel</h1></header>
                <p>Paragraf pertama.</p>
                <h2>Bagian Dua</h2>
                <ul><li>Satu</li><li>Dua</li></ul>
                <div id="comments"><p>komentar spam</p></div>
              </article>
              <footer>hak cipta</footer>
              <div class="popup">Berlangganan newsletter!</div>
              <script>alert('x')</script>
            </body></html>
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                NodeType.H1 to "Judul Artikel",
                NodeType.P to "Paragraf pertama.",
                NodeType.H2 to "Bagian Dua",
                NodeType.LI to "Satu",
                NodeType.LI to "Dua",
            ),
            nodes.map { it.type to it.text },
        )
    }

    @Test
    fun `heading h4 sampai h6 menjadi h3 dan spasi dirapikan`() {
        val nodes = parse("<body><h4>  Level   empat </h4><h6>Level enam</h6></body>")
        assertEquals(
            listOf(NodeType.H3 to "Level empat", NodeType.H3 to "Level enam"),
            nodes.map { it.type to it.text },
        )
    }

    @Test
    fun `teks lepas di dalam div dan elemen inline digabung menjadi satu paragraf`() {
        val nodes = parse("<body><div>Halo <b>dunia</b> dan <a href='/x'>tautan</a> ini.</div></body>")
        assertEquals(
            listOf(NodeType.P to "Halo ${boldOn}dunia$boldOff dan tautan ini."),
            nodes.map { it.type to it.text },
        )
    }

    @Test
    fun `tebal dan miring dipertahankan di paragraf, list, dan lewat gaya CSS`() {
        val nodes = parse(
            "<body><p>Ambil <strong>medicinal herb</strong> lalu <em>simpan</em>.</p>" +
                "<ul><li>Cek <b>laci</b> kanan</li></ul>" +
                "<p>Boleh <span style='font-weight: bold'>tebal css</span> juga.</p></body>",
        )
        assertEquals(
            listOf(
                "Ambil ${boldOn}medicinal herb$boldOff lalu ${italicOn}simpan$italicOff.",
                "Cek ${boldOn}laci$boldOff kanan",
                "Boleh ${boldOn}tebal css$boldOff juga.",
            ),
            nodes.map { it.text },
        )
    }

    @Test
    fun `tebal di dalam tebal dan gaya kosong tidak meninggalkan penanda kosong`() {
        val nodes = parse("<body><p>A <b>satu <b>dua</b></b><b> </b> tiga</p></body>")
        assertEquals("A ${boldOn}satu ${boldOn}dua${boldOff}$boldOff tiga", nodes.single().text)
    }

    @Test
    fun `teks tanpa gaya tetap polos tanpa penanda`() {
        val nodes = parse("<body><p>Hanya teks biasa dengan a * b = c.</p></body>")
        assertEquals("Hanya teks biasa dengan a * b = c.", nodes.single().text)
    }

    @Test
    fun `gambar memakai URL absolut dan data URI diabaikan`() {
        val nodes = parse(
            "<body><p>Peta:</p><img src='/img/a.png' alt='Peta dunia'><img src='data:image/png;base64,AAAA'>" +
                "<img data-src='https://cdn.example.com/b.jpg'></body>",
        )
        val images = nodes.filter { it.type == NodeType.IMG }
        assertEquals(
            listOf("https://example.com/img/a.png", "https://cdn.example.com/b.jpg"),
            images.map { it.imageUrl },
        )
        assertEquals("Peta dunia", images.first().imageAlt)
        assertNull(images.last().imageAlt)
    }

    @Test
    fun `tabel disimpan sebagai html mentah dan pre dipertahankan`() {
        val nodes = parse(
            "<body><table><tr><td>HP</td><td>100</td></tr></table><pre>  baris 1\n    baris 2</pre></body>",
        )
        val table = nodes.single { it.type == NodeType.TABLE }
        assertTrue(table.tableHtml.orEmpty().contains("<td>HP</td>"))
        assertNull(table.text)
        assertEquals("  baris 1\n    baris 2", nodes.single { it.type == NodeType.PRE }.text)
    }

    @Test
    fun `kontainer wiki dipilih sehingga teks di luarnya tidak ikut`() {
        val body = "Isi artikel wiki yang cukup panjang. ".repeat(10)
        val nodes = parse(
            "<body><div id='outside'>Teks di luar kontainer</div><div id='mw-content-text'><p>$body</p></div></body>",
        )
        assertTrue(nodes.isNotEmpty())
        assertFalse(nodes.any { it.text.orEmpty().contains("di luar kontainer") })
    }

    @Test
    fun `paragraf panjang dipecah menjadi p lalu p_cont`() {
        val sentence = "Ini adalah satu kalimat yang cukup panjang untuk uji. "
        val nodes = parse("<body><p>${sentence.repeat(12)}</p></body>")
        assertEquals(NodeType.P, nodes.first().type)
        assertTrue(nodes.size > 1)
        assertTrue(nodes.drop(1).all { it.type == NodeType.P_CONT })
    }

    @Test
    fun `halaman tanpa konten menghasilkan daftar kosong`() {
        assertTrue(parse("<html><body><nav>Menu</nav><script>x()</script></body></html>").isEmpty())
    }
}
