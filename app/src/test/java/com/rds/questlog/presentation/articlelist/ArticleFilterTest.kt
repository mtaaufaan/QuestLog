package com.rds.questlog.presentation.articlelist

import com.rds.questlog.presentation.preview.PreviewData
import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleFilterTest {

    private val all = PreviewData.articles

    private fun ids(query: String, game: Long?) = filterArticles(all, query, game).map { it.id }.toSet()

    @Test
    fun `tanpa query dan filter menampilkan semua artikel (QL-11 reset)`() {
        assertEquals(setOf(1L, 2L, 3L, 4L, 5L), ids("", null))
    }

    @Test
    fun `search cocok sebagian dan tidak peduli huruf besar-kecil (QL-10)`() {
        assertEquals(setOf(1L, 4L), ids("WALKTHROUGH", null))
        assertEquals(setOf(2L), ids("mini med", null))
        assertEquals(emptySet<Long>(), ids("tidak ada", null))
    }

    @Test
    fun `spasi di pinggir query diabaikan dan query spasi saja dianggap kosong`() {
        assertEquals(setOf(2L), ids("  medal  ", null))
        assertEquals(setOf(1L, 2L, 3L, 4L, 5L), ids("   ", null))
    }

    @Test
    fun `filter game hanya menampilkan artikel game itu (QL-11)`() {
        assertEquals(setOf(1L, 2L, 3L), ids("", 1L))
        assertEquals(setOf(4L, 5L), ids("", 2L))
    }

    @Test
    fun `filter dan search dikombinasikan (QL-11)`() {
        assertEquals(setOf(4L), ids("walkthrough", 2L))
        assertEquals(setOf(1L), ids("disc", 1L))
        assertEquals(emptySet<Long>(), ids("mini medal", 2L))
    }

    @Test
    fun `opsi filter menghitung artikel per game dari seluruh artikel`() {
        val options = gameFilterOptions(PreviewData.games, all)
        assertEquals(listOf("Dragon Quest VII" to 3, "Breath Of Fire III" to 2), options.map { it.name to it.count })
    }
}
