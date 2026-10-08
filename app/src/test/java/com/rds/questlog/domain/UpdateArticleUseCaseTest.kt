package com.rds.questlog.domain

import com.rds.questlog.domain.error.QuestLogError.DatabaseError
import com.rds.questlog.domain.error.QuestLogError.TierError
import com.rds.questlog.domain.error.QuestLogError.ValidationError
import com.rds.questlog.domain.model.Article
import com.rds.questlog.domain.model.Game
import com.rds.questlog.domain.model.GameTarget
import com.rds.questlog.domain.model.SourcePageStatus
import com.rds.questlog.domain.usecase.article.UpdateArticleUseCase
import com.rds.questlog.domain.usecase.premium.ObservePremiumStatusUseCase
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** QL-18: ubah judul dan pindah game, termasuk batas tier gratis yang memperhitungkan game asal yang jadi kosong. */
class UpdateArticleUseCaseTest {

    private val dq = Game(id = 1, name = "Dragon Quest VII", createdAt = 1, articleCount = 2)
    private val bof = Game(id = 2, name = "Breath Of Fire III", createdAt = 2, articleCount = 1)

    private fun article(gameId: Long = 1, status: SourcePageStatus = SourcePageStatus.COMPLETED) = Article(
        id = 10,
        gameId = gameId,
        gameName = "x",
        title = "Lama",
        pages = listOf(page(1, status = status)),
        checkpointNodeId = null,
        lastVisitedNodeId = null,
        lastReadAt = null,
        createdAt = 0,
    )

    private class Setup(val articles: FakeArticleManagementRepository, val result: Result<Unit>)

    private fun update(
        title: String = " Baru ",
        gameId: Long? = 1,
        newGameName: String? = null,
        games: List<Game> = listOf(dq, bof),
        article: Article? = article(),
        premium: Boolean = false,
        failWith: Throwable? = null,
    ): Setup {
        val management = FakeArticleManagementRepository(failWith)
        val useCase = UpdateArticleUseCase(
            FakeArticleRepository(article = article),
            management,
            FakeGameRepository(games = games),
            ObservePremiumStatusUseCase(FakeAppConfigRepository(premium)),
        )
        return Setup(management, runBlocking { useCase(10, title, gameId, newGameName) })
    }

    @Test
    fun `ubah judul saja di-trim dan game tidak berubah`() {
        val s = update()

        assertTrue(s.result.isSuccess)
        assertEquals(Triple(10L, "Baru", GameTarget.Unchanged), s.articles.updates.single())
    }

    @Test
    fun `pindah ke game yang sudah ada`() {
        val s = update(gameId = 2)

        assertEquals(GameTarget.Existing(2), s.articles.updates.single().third)
    }

    @Test
    fun `nama game baru dibuat, tetapi nama yang sama dengan game lama memilih game itu`() {
        val baru = update(gameId = null, newGameName = " Zelda ", premium = true)
        val sama = update(gameId = null, newGameName = "breath of fire iii")

        assertEquals(GameTarget.New("Zelda"), baru.articles.updates.single().third)
        assertEquals(GameTarget.Existing(2), sama.articles.updates.single().third)
    }

    @Test
    fun `tier gratis menolak game tujuan yang sudah berisi 5 artikel`() {
        val full = bof.copy(articleCount = 5)

        val s = update(gameId = 2, games = listOf(dq, full))

        assertEquals(TierError.ArticleLimitReached, s.result.exceptionOrNull())
        assertTrue(s.articles.updates.isEmpty())
    }

    @Test
    fun `premium tidak dibatasi game penuh maupun jumlah game`() {
        val full = bof.copy(articleCount = 5)
        val ok = update(gameId = 2, games = listOf(dq, full), premium = true)
        val many = update(gameId = null, newGameName = "Zelda", games = listOf(dq, bof), premium = true)

        assertTrue(ok.result.isSuccess)
        assertTrue(many.result.isSuccess)
    }

    @Test
    fun `tier gratis menolak game baru bila sudah ada 2 game dan game asal tidak kosong`() {
        val s = update(gameId = null, newGameName = "Zelda") // game asal berisi 2 artikel

        assertEquals(TierError.GameLimitReached, s.result.exceptionOrNull())
        assertTrue(s.articles.updates.isEmpty())
    }

    @Test
    fun `artikel tunggal boleh pindah ke game baru walau sudah 2 game karena game asal dihapus`() {
        val s = update(gameId = null, newGameName = "Zelda", games = listOf(dq.copy(articleCount = 1), bof))

        assertTrue(s.result.isSuccess)
        assertEquals(GameTarget.New("Zelda"), s.articles.updates.single().third)
    }

    @Test
    fun `validasi judul dan game`() {
        val cases = listOf(
            update(title = "  ") to ValidationError.EmptyTitle,
            update(gameId = null, newGameName = null) to ValidationError.EmptyGameName,
            update(gameId = null, newGameName = "   ") to ValidationError.EmptyGameName,
            update(gameId = null, newGameName = "x".repeat(101)) to ValidationError.GameNameTooLong,
        )

        cases.forEach { (s, error) ->
            assertEquals(error, s.result.exceptionOrNull())
            assertTrue(s.articles.updates.isEmpty())
        }
    }

    @Test
    fun `gameId dan newGameName sekaligus ditolak sebagai kegagalan tulis`() {
        val s = update(gameId = 2, newGameName = "Zelda")

        assertTrue(s.result.exceptionOrNull() is DatabaseError.WriteFailed)
        assertTrue(s.articles.updates.isEmpty())
    }

    @Test
    fun `artikel yang sedang diproses tetap boleh diubah`() {
        val s = update(article = article(status = SourcePageStatus.PENDING))

        assertTrue(s.result.isSuccess)
    }

    @Test
    fun `artikel tidak ditemukan dan kegagalan database menjadi WriteFailed`() {
        val missing = update(article = null)
        val broken = update(failWith = IOException("disk penuh"))

        assertTrue(missing.result.exceptionOrNull() is DatabaseError.WriteFailed)
        assertTrue(broken.result.exceptionOrNull() is DatabaseError.WriteFailed)
    }
}
