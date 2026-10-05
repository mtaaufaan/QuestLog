package com.rds.questlog.data.scraper

import com.rds.questlog.data.mapper.parseTableRows
import com.rds.questlog.domain.model.InlineMarkup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineMarkupTest {

    private val boldOn = InlineMarkup.BOLD_ON
    private val boldOff = InlineMarkup.BOLD_OFF

    @Test
    fun `strip membuang semua penanda`() {
        assertEquals("Halo dunia", InlineMarkup.strip("Halo ${boldOn}dunia$boldOff"))
    }

    @Test
    fun `pemecahan paragraf panjang menutup dan membuka lagi tebal yang melintasi potongan`() {
        val sentence = "Kalimat uji dengan panjang sedang yang cukup. "
        val text = "$boldOn" + sentence.repeat(20).trim() + "$boldOff"

        val chunks = splitParagraph(text)

        assertTrue(chunks.size > 1)
        chunks.forEach { chunk ->
            assertEquals(chunk, 1, chunk.count { it == boldOn })
            assertEquals(chunk, 1, chunk.count { it == boldOff })
            assertTrue(chunk.startsWith("$boldOn") && chunk.endsWith("$boldOff"))
        }
    }

    @Test
    fun `sel tabel mempertahankan tebal dan colspan`() {
        val rows = parseTableRows(
            "<table><tr><th colspan='2'>Judul</th></tr><tr><td>Item <b>langka</b></td><td>Desa</td></tr></table>",
        )

        assertEquals("Item ${boldOn}langka$boldOff", rows[1][0].text)
        assertEquals(2, rows[0][0].colSpan)
    }
}
