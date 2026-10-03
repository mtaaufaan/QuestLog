package com.rds.questlog.presentation.articlelist

import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.PageStatus
import com.rds.questlog.presentation.preview.PreviewData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleListUiStateTest {

    private val state = ArticleListUiState(games = PreviewData.games, articles = PreviewData.articles)

    @Test
    fun `menghapus artikel hanya membuang artikel itu`() {
        val after = state.withoutArticle(5L)
        assertEquals(listOf(3L, 1L, 2L, 4L), after.articles.map { it.id })
        assertNull(after.articleById(5L))
    }

    @Test
    fun `retry mengubah halaman FAILED menjadi PENDING dan artikel kembali SCRAPING`() {
        val before = state.articleById(2L)!!
        val after = state.withFailedPagesRetried(2L).articleById(2L)!!

        assertEquals(ArticleStatus.SCRAPING, after.status)
        assertEquals(0, after.failedPageCount)
        assertEquals(1, after.pages.count { it.status == PageStatus.PENDING })
        assertTrue(after.pages.all { it.reason == null })
        // Halaman DONE tidak disentuh.
        assertEquals(before.donePageCount, after.donePageCount)
    }

    @Test
    fun `retry artikel lain tidak mengubah artikel yang tidak dipilih`() {
        val after = state.withFailedPagesRetried(5L)
        assertEquals(state.articleById(2L), after.articleById(2L))
        assertEquals(ArticleStatus.SCRAPING, after.articleById(5L)!!.status)
        assertEquals(2, after.articleById(5L)!!.pages.count { it.status == PageStatus.PENDING })
    }
}
