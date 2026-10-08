package com.rds.questlog.domain

import com.rds.questlog.domain.error.QuestLogError.ArticleError
import com.rds.questlog.domain.error.QuestLogError.DatabaseError
import com.rds.questlog.domain.usecase.article.DeletePageUseCase
import com.rds.questlog.domain.usecase.article.ReorderPagesUseCase
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** QL-19: use case hanya meneruskan ke repository dan membungkus kegagalan; aturan datanya diuji di instrumented. */
class ManagePagesUseCasesTest {

    @Test
    fun `hapus halaman diteruskan ke repository`() {
        val repo = FakeArticleManagementRepository()

        val result = runBlocking { DeletePageUseCase(repo)(articleId = 3, pageId = 9) }

        assertTrue(result.isSuccess)
        assertEquals(listOf(3L to 9L), repo.deletedPages)
    }

    @Test
    fun `urut ulang diteruskan dengan urutan persis seperti diberikan`() {
        val repo = FakeArticleManagementRepository()

        val result = runBlocking { ReorderPagesUseCase(repo)(articleId = 3, orderedPageIds = listOf(9, 7, 8)) }

        assertTrue(result.isSuccess)
        assertEquals(listOf(3L to listOf(9L, 7L, 8L)), repo.reordered)
    }

    @Test
    fun `ArticleError dari repository diteruskan apa adanya`() {
        listOf(ArticleError.LastPage, ArticleError.Busy, ArticleError.PageNotFound).forEach { error ->
            val result = runBlocking { DeletePageUseCase(FakeArticleManagementRepository(error))(1, 1) }

            assertEquals(error, result.exceptionOrNull())
        }
        val reorderRepo = FakeArticleManagementRepository(ArticleError.PageNotFound)
        val reorder = runBlocking { ReorderPagesUseCase(reorderRepo)(1, listOf(1)) }
        assertEquals(ArticleError.PageNotFound, reorder.exceptionOrNull())
    }

    @Test
    fun `kegagalan lain menjadi WriteFailed`() {
        val repo = FakeArticleManagementRepository(IOException("disk penuh"))

        val delete = runBlocking { DeletePageUseCase(repo)(1, 1) }
        val reorder = runBlocking { ReorderPagesUseCase(repo)(1, listOf(1)) }

        assertTrue(delete.exceptionOrNull() is DatabaseError.WriteFailed)
        assertTrue(reorder.exceptionOrNull() is DatabaseError.WriteFailed)
    }
}
