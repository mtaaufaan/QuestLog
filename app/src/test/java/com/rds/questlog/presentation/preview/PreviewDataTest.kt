package com.rds.questlog.presentation.preview

import com.rds.questlog.presentation.model.ArticleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewDataTest {

    @Test
    fun `dua game dan lima artikel dengan variasi status`() {
        assertEquals(2, PreviewData.games.size)
        assertEquals(5, PreviewData.articles.size)
        val byStatus = PreviewData.articles.groupingBy { it.status }.eachCount()
        assertEquals(1, byStatus[ArticleStatus.SCRAPING])
        assertEquals(3, byStatus[ArticleStatus.READY])
        assertEquals(1, byStatus[ArticleStatus.ERROR])
    }

    @Test
    fun `tepat satu artikel partial dan artikel ERROR gagal total`() {
        val partial = PreviewData.articles.filter { it.isPartial }
        assertEquals(listOf(2L), partial.map { it.id })
        assertEquals(1, partial.single().failedPageCount)

        val error = PreviewData.articles.single { it.status == ArticleStatus.ERROR }
        assertEquals(error.pages.size, error.failedPageCount)
        assertFalse(error.isPartial)
    }

    @Test
    fun `setiap artikel merujuk game yang ada dan checkpoint terpisah dari posisi terakhir`() {
        PreviewData.articles.forEach { a ->
            assertEquals(PreviewData.games.single { it.id == a.gameId }.name, a.gameName)
        }
        val withBoth = PreviewData.articles.single { it.id == 1L }
        assertEquals(302L, withBoth.checkpointNodeId)
        assertEquals(204L, withBoth.lastNodeId)
        assertTrue(PreviewData.articles.any { it.checkpointNodeId != null && it.lastNodeId == null })
    }
}
