package com.rds.questlog.domain

import com.rds.questlog.domain.error.QuestLogError.TierError
import com.rds.questlog.domain.error.QuestLogError.ValidationError
import com.rds.questlog.domain.usecase.article.AppendPagesUseCase
import com.rds.questlog.domain.usecase.article.SaveArticleUseCase
import com.rds.questlog.domain.usecase.premium.ObservePremiumStatusUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Sprint2UseCasesTest {

    private val a = "https://example.com/a"
    private val b = "https://example.com/b"

    private fun save(
        game: String = "Dragon Quest VII",
        urls: List<String> = listOf(a),
        articles: FakeArticleRepository = FakeArticleRepository(),
        games: FakeGameRepository = FakeGameRepository(),
        premium: Boolean = false,
        scheduler: FakeScheduler = FakeScheduler(),
    ) = runBlocking {
        SaveArticleUseCase(games, articles, scheduler, ObservePremiumStatusUseCase(FakeBillingService(premium)))(
            game,
            "Judul",
            urls,
        )
    }

    private fun append(
        urls: List<String>,
        articles: FakeArticleRepository = FakeArticleRepository(),
        scheduler: FakeScheduler = FakeScheduler(),
    ) = runBlocking { AppendPagesUseCase(articles, scheduler)(5, urls) }

    @Test
    fun `URL duplikat dalam input ditolak sebelum menyimpan atau menjadwalkan`() {
        val scheduler = FakeScheduler()
        val articles = FakeArticleRepository()
        val result = save(urls = listOf(a, b, a), articles = articles, scheduler = scheduler)

        assertEquals(ValidationError.DuplicateUrl(a), result.exceptionOrNull())
        assertTrue(articles.inserted.isEmpty() && scheduler.scheduled.isEmpty())
    }

    @Test
    fun `URL yang sudah tersimpan di artikel mana pun ditolak di kedua mode`() {
        val articles = FakeArticleRepository(stored = setOf(b))
        val scheduler = FakeScheduler()

        assertEquals(
            ValidationError.UrlAlreadySaved(b),
            save(urls = listOf(a, b), articles = articles, scheduler = scheduler).exceptionOrNull(),
        )
        assertEquals(
            ValidationError.UrlAlreadySaved(b),
            append(listOf(b), articles, scheduler).exceptionOrNull(),
        )
        assertTrue(articles.inserted.isEmpty() && articles.appended.isEmpty() && scheduler.scheduled.isEmpty())
    }

    @Test
    fun `pengguna gratis tidak bisa membuat game ketiga tetapi game yang ada tetap bisa dipakai`() {
        val full = FakeGameRepository(existing = mapOf("Dragon Quest VII" to 1L), gameCount = 2)

        assertEquals(TierError.GameLimitReached, save(game = "Baru", games = full).exceptionOrNull())
        assertTrue(save(game = "Dragon Quest VII", games = full).isSuccess)
        assertTrue(save(game = "Baru", games = full, premium = true).isSuccess)
    }

    @Test
    fun `pengguna gratis dibatasi lima artikel per game, premium tidak`() {
        val games = FakeGameRepository(existing = mapOf("Dragon Quest VII" to 1L), gameCount = 1)
        val scheduler = FakeScheduler()

        assertEquals(
            TierError.ArticleLimitReached,
            save(games = games, articles = FakeArticleRepository(articleCount = 5), scheduler = scheduler)
                .exceptionOrNull(),
        )
        assertTrue(scheduler.scheduled.isEmpty())
        assertTrue(save(games = games, articles = FakeArticleRepository(articleCount = 4)).isSuccess)
        assertTrue(save(games = games, articles = FakeArticleRepository(articleCount = 50), premium = true).isSuccess)
    }

    @Test
    fun `lengkapi menambah halaman lalu menjadwalkan scraping artikel target`() {
        val articles = FakeArticleRepository()
        val scheduler = FakeScheduler()

        assertTrue(append(listOf(" $a ", b), articles, scheduler).isSuccess)
        assertEquals(listOf(5L to listOf(a, b)), articles.appended)
        assertEquals(listOf(5L), scheduler.scheduled)
    }

    @Test
    fun `lengkapi memvalidasi bentuk dan jumlah URL`() {
        assertEquals(ValidationError.NoUrlsProvided, append(emptyList()).exceptionOrNull())
        assertEquals(ValidationError.InvalidUrl, append(listOf("http://x.com")).exceptionOrNull())
        assertEquals(ValidationError.TooManyUrls, append(List(11) { "https://x.com/$it" }).exceptionOrNull())
        assertEquals(ValidationError.DuplicateUrl(a), append(listOf(a, a)).exceptionOrNull())
    }
}
