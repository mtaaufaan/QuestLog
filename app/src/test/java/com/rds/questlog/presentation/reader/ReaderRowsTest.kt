package com.rds.questlog.presentation.reader

import com.rds.questlog.presentation.preview.ReaderPreviewData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderRowsTest {

    private val article = ReaderPreviewData.article
    private val content = ReaderPreviewData.content

    @Test
    fun `seamless menyambung halaman dengan pemisah dan placeholder untuk halaman gagal`() {
        val rows = readerRows(article, content, paged = false, pageIndex = 0)

        assertEquals(listOf(2, 3, 5), rows.filterIsInstance<ReaderRow.Break>().map { it.page })
        assertEquals(listOf(4), rows.filterIsInstance<ReaderRow.Missing>().map { it.page })
        assertEquals("gamefaqs.gamespot.com", rows.filterIsInstance<ReaderRow.Break>().first().host)
        assertEquals(content.flatten().size, rows.count { it.nodeId != null })
    }

    @Test
    fun `per halaman hanya menampilkan halaman yang berhasil dan melewati yang gagal`() {
        val last = readerRows(article, content, paged = true, pageIndex = 3)

        assertEquals(content[4].map { it.id }, last.map { it.nodeId })
        assertTrue(readerRows(article, content, paged = true, pageIndex = 1).none { it !is ReaderRow.Node })
        assertEquals(3, pageNumber(article, 2))
        assertEquals(5, pageNumber(article, 3))
    }

    @Test
    fun `resolveNode memilih id persis, lalu id berikutnya, lalu node terakhir`() {
        assertEquals(NodeHit(204, 1), resolveNode(204, article, content))
        assertEquals(NodeHit(301, 2), resolveNode(209, article, content))
        assertEquals(NodeHit(501, 3), resolveNode(401, article, content))
        assertEquals(NodeHit(507, 3), resolveNode(9_999, article, content))
    }
}
