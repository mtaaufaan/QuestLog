package com.rds.questlog.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rds.questlog.data.repository.AppConfigRepositoryImpl
import com.rds.questlog.domain.error.QuestLogError.TierError
import com.rds.questlog.domain.model.PurchaseQuery
import com.rds.questlog.domain.model.PurchaseResult
import com.rds.questlog.domain.repository.BillingService
import com.rds.questlog.domain.scraper.ScrapeScheduler
import com.rds.questlog.domain.usecase.article.SaveArticleUseCase
import com.rds.questlog.domain.usecase.premium.ObservePremiumStatusUseCase
import com.rds.questlog.domain.usecase.premium.SyncPremiumStatusUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** QL-12/13/17: status premium di tabel app_config nyata, dibaca sebagai Flow oleh use case penegak batas tier. */
@RunWith(AndroidJUnit4::class)
class PremiumAppConfigTest {

    private lateinit var env: TestEnvironment
    private lateinit var config: AppConfigRepositoryImpl

    private class StoreBilling(var query: PurchaseQuery) : BillingService {
        override suspend fun queryUnlimitedPurchase() = query
        override suspend fun purchaseUnlimited(): PurchaseResult = PurchaseResult.Cancelled
    }

    @Before
    fun setUp() {
        env = TestEnvironment()
        config = AppConfigRepositoryImpl(env.db, env.db.appConfigDao())
    }

    @After
    fun tearDown() = env.close()

    private fun rows() = env.strings("SELECT key || '=' || value FROM app_config ORDER BY key")

    @Test(timeout = 30_000)
    fun belumAdaBarisBerartiGratisLaluSyncMenulisTigaBaris() = runBlocking {
        assertFalse(config.observeIsPremium().first())

        val billing = StoreBilling(PurchaseQuery.Owned("tok-9"))
        assertTrue(SyncPremiumStatusUseCase(billing, config)().isSuccess)

        assertTrue(config.observeIsPremium().first())
        val saved = rows()
        assertTrue(saved.contains("is_premium=1"))
        assertTrue(saved.contains("purchase_token=tok-9"))
        assertTrue(saved.any { it.startsWith("purchase_verified_at=") && it.substringAfter('=').toLong() > 0 })
    }

    @Test(timeout = 30_000)
    fun refundMenurunkanKeGratisDanMenghapusToken() = runBlocking {
        val billing = StoreBilling(PurchaseQuery.Owned("tok"))
        val sync = SyncPremiumStatusUseCase(billing, config)
        sync()

        billing.query = PurchaseQuery.NotOwned
        sync()

        assertFalse(config.observeIsPremium().first())
        assertTrue(rows().contains("is_premium=0"))
        assertTrue(rows().none { it.startsWith("purchase_token=") })
    }

    @Test(timeout = 30_000)
    fun batasGameGratisHilangSetelahPembelianTanpaMembuatUlangUseCase() = runBlocking {
        val scheduler = object : ScrapeScheduler {
            override fun schedule(articleId: Long) = Unit
        }
        val save = SaveArticleUseCase(env.games, env.articles, scheduler, ObservePremiumStatusUseCase(config))
        env.games.findOrCreate("Game A")
        env.games.findOrCreate("Game B")

        assertEquals(TierError.GameLimitReached, save("Game C", "Judul", listOf("https://x.com/1")).exceptionOrNull())

        config.setPremium(true, "tok", System.currentTimeMillis())

        assertTrue(save("Game C", "Judul", listOf("https://x.com/1")).isSuccess)
        assertEquals(3, env.count("SELECT COUNT(*) FROM games"))
    }

    @Test(timeout = 30_000)
    fun artikelDanGameLamaTetapBisaDiaksesSaatLimitTercapai() = runBlocking {
        val gameId = env.games.findOrCreate("Game A")
        repeat(5) { env.articles.insertArticle(gameId, "Artikel $it", listOf("https://x.com/$it")) }

        val articles = env.articles.observeArticles("", gameId).first()

        assertEquals(5, articles.size)
    }
}
