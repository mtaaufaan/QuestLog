package com.rds.questlog.domain

import com.rds.questlog.domain.error.QuestLogError.ArticleError
import com.rds.questlog.domain.error.QuestLogError.DatabaseError
import com.rds.questlog.domain.model.Article
import com.rds.questlog.domain.model.SourcePageStatus
import com.rds.questlog.domain.model.SourcePageStatus.COMPLETED
import com.rds.questlog.domain.model.SourcePageStatus.FAILED
import com.rds.questlog.domain.model.SourcePageStatus.IN_PROGRESS
import com.rds.questlog.domain.model.SourcePageStatus.PENDING
import com.rds.questlog.domain.usecase.article.RefreshArticleUseCase
import com.rds.questlog.domain.usecase.article.RefreshPageUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** QL-20: unduh ulang menandai halaman lalu menjadwalkan worker; ditolak bila artikel masih diproses. */
class RefreshUseCasesTest {

    private fun article(vararg statuses: SourcePageStatus) = Article(
        id = 5,
        gameId = 1,
        gameName = "G",
        title = "T",
        pages = statuses.mapIndexed { i, s -> page(i + 1L, status = s) },
        checkpointNodeId = null,
        lastVisitedNodeId = null,
        lastReadAt = null,
        createdAt = 0,
    )

    private class Setup(val management: FakeArticleManagementRepository, val scheduler: FakeScheduler)

    private fun setup(article: Article?, failWith: Throwable? = null) =
        Setup(FakeArticleManagementRepository(failWith), FakeScheduler()) to FakeArticleRepository(article = article)

    @Test
    fun `unduh ulang artikel menandai semua halaman lalu menjadwalkan worker`() {
        val (s, articles) = setup(article(COMPLETED, FAILED))

        val result = runBlocking { RefreshArticleUseCase(articles, s.management, s.scheduler)(5) }

        assertTrue(result.isSuccess)
        assertEquals(listOf(5L to null), s.management.refreshed)
        assertEquals(listOf(5L), s.scheduler.scheduled)
    }

    @Test
    fun `unduh ulang satu halaman hanya menandai halaman itu`() {
        val (s, articles) = setup(article(COMPLETED, COMPLETED))

        val result = runBlocking { RefreshPageUseCase(articles, s.management, s.scheduler)(5, 2) }

        assertTrue(result.isSuccess)
        assertEquals(listOf(5L to 2L), s.management.refreshed)
        assertEquals(listOf(5L), s.scheduler.scheduled)
    }

    @Test
    fun `artikel yang sedang diproses ditolak dan tidak ada yang berubah atau dijadwalkan`() {
        listOf(arrayOf(COMPLETED, PENDING), arrayOf(IN_PROGRESS)).forEach { statuses ->
            val (s, articles) = setup(article(*statuses))

            val all = runBlocking { RefreshArticleUseCase(articles, s.management, s.scheduler)(5) }
            val one = runBlocking { RefreshPageUseCase(articles, s.management, s.scheduler)(5, 1) }

            assertEquals(ArticleError.Busy, all.exceptionOrNull())
            assertEquals(ArticleError.Busy, one.exceptionOrNull())
            assertTrue(s.management.refreshed.isEmpty())
            assertTrue(s.scheduler.scheduled.isEmpty())
        }
    }

    @Test
    fun `halaman tidak dikenal atau kegagalan database tidak menjadwalkan worker`() {
        val (s, articles) = setup(article(COMPLETED), failWith = ArticleError.PageNotFound)

        val result = runBlocking { RefreshPageUseCase(articles, s.management, s.scheduler)(5, 99) }

        assertEquals(ArticleError.PageNotFound, result.exceptionOrNull())
        assertTrue(s.scheduler.scheduled.isEmpty())
    }

    @Test
    fun `artikel tidak ditemukan menjadi WriteFailed`() {
        val (s, articles) = setup(null)

        val result = runBlocking { RefreshArticleUseCase(articles, s.management, s.scheduler)(5) }

        assertTrue(result.exceptionOrNull() is DatabaseError.WriteFailed)
        assertTrue(s.scheduler.scheduled.isEmpty())
    }
}
