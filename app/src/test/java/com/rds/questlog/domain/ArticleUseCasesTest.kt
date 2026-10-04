package com.rds.questlog.domain

import com.rds.questlog.domain.error.QuestLogError
import com.rds.questlog.domain.error.QuestLogError.ValidationError
import com.rds.questlog.domain.usecase.article.DeleteArticleUseCase
import com.rds.questlog.domain.usecase.article.RetryFailedPagesUseCase
import com.rds.questlog.domain.usecase.article.SaveArticleUseCase
import com.rds.questlog.domain.usecase.premium.ObservePremiumStatusUseCase
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleUseCasesTest {

    private val url = "https://example.com/a"

    private fun save(
        game: String = "Dragon Quest VII",
        title: String = "Walkthrough",
        urls: List<String> = listOf(url),
        scheduler: FakeScheduler = FakeScheduler(),
        articles: FakeArticleRepository = FakeArticleRepository(),
        games: FakeGameRepository = FakeGameRepository(),
        premium: Boolean = false,
    ) = runBlocking {
        SaveArticleUseCase(games, articles, scheduler, ObservePremiumStatusUseCase(FakeBillingService(premium)))(
            game,
            title,
            urls,
        )
    }

    @Test
    fun `simpan artikel membuat game, artikel, dan menjadwalkan scraping dengan input di-trim`() {
        val scheduler = FakeScheduler()
        val articles = FakeArticleRepository()
        val games = FakeGameRepository()

        val result =
            save(
                game = "  Dragon Quest VII ",
                title = " Walkthrough ",
                urls = listOf(" $url "),
                scheduler = scheduler,
                articles = articles,
                games = games,
            )

        assertEquals(42L, result.getOrThrow())
        assertEquals(listOf("Dragon Quest VII"), games.created)
        assertEquals(Triple(7L, "Walkthrough", listOf(url)), articles.inserted.single())
        assertEquals(listOf(42L), scheduler.scheduled)
    }

    @Test
    fun `validasi input menghasilkan ValidationError yang tepat dan tidak menyimpan apa pun`() {
        val cases = listOf(
            save(game = " ") to ValidationError.EmptyGameName,
            save(game = "x".repeat(101)) to ValidationError.GameNameTooLong,
            save(title = "") to ValidationError.EmptyTitle,
            save(urls = emptyList()) to ValidationError.NoUrlsProvided,
            save(urls = List(11) { "https://example.com/$it" }) to ValidationError.TooManyUrls,
            save(urls = listOf("http://example.com")) to ValidationError.InvalidUrl,
            save(urls = listOf("example.com")) to ValidationError.InvalidUrl,
        )
        cases.forEach { (result, expected) -> assertEquals(expected, result.exceptionOrNull()) }
    }

    @Test
    fun `batas tepat 10 URL dan 100 karakter nama game diterima`() {
        assertTrue(save(urls = List(10) { "https://example.com/$it" }).isSuccess)
        assertTrue(save(game = "x".repeat(100)).isSuccess)
    }

    @Test
    fun `kegagalan database saat simpan dibungkus menjadi WriteFailed dan tidak menjadwalkan`() {
        val scheduler = FakeScheduler()
        val result = save(scheduler = scheduler, articles = FakeArticleRepository(failWith = IOException("disk penuh")))

        assertTrue(result.exceptionOrNull() is QuestLogError.DatabaseError.WriteFailed)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `hapus artikel sukses dan gagal`() {
        val repository = FakeArticleRepository()
        assertTrue(runBlocking { DeleteArticleUseCase(repository)(5) }.isSuccess)
        assertEquals(listOf(5L), repository.deleted)

        val failing = FakeArticleRepository(failWith = IllegalStateException("db"))
        val failure = runBlocking { DeleteArticleUseCase(failing)(5) }.exceptionOrNull()
        assertTrue(failure is QuestLogError.DatabaseError.WriteFailed)
    }

    @Test
    fun `retry menjadwalkan scraping hanya bila ada halaman yang direset`() {
        val scheduler = FakeScheduler()
        assertEquals(
            2,
            runBlocking {
                RetryFailedPagesUseCase(FakeArticleRepository(resetCount = 2), scheduler)(9)
            }.getOrThrow(),
        )
        assertEquals(listOf(9L), scheduler.scheduled)

        val idle = FakeScheduler()
        assertEquals(
            0,
            runBlocking { RetryFailedPagesUseCase(FakeArticleRepository(resetCount = 0), idle)(9) }.getOrThrow(),
        )
        assertTrue(idle.scheduled.isEmpty())
    }
}
