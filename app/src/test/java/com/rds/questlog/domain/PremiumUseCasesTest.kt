package com.rds.questlog.domain

import com.rds.questlog.domain.error.QuestLogError
import com.rds.questlog.domain.error.QuestLogError.TierError
import com.rds.questlog.domain.model.PurchaseQuery
import com.rds.questlog.domain.model.PurchaseResult
import com.rds.questlog.domain.model.UnlockOutcome
import com.rds.questlog.domain.usecase.article.SaveArticleUseCase
import com.rds.questlog.domain.usecase.premium.ObservePremiumStatusUseCase
import com.rds.questlog.domain.usecase.premium.PurchaseUnlimitedUseCase
import com.rds.questlog.domain.usecase.premium.RestorePurchasesUseCase
import com.rds.questlog.domain.usecase.premium.SyncPremiumStatusUseCase
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumUseCasesTest {

    private fun sync(billing: FakeBillingService, config: FakeAppConfigRepository) =
        runBlocking { SyncPremiumStatusUseCase(billing, config)() }

    @Test
    fun `saat dibuka, pembelian aktif disimpan sebagai premium beserta token dan waktu verifikasi`() {
        val config = FakeAppConfigRepository(premium = false)

        assertTrue(sync(FakeBillingService(PurchaseQuery.Owned("tok-1")), config).isSuccess)

        assertTrue(runBlocking { ObservePremiumStatusUseCase(config)().first() })
        assertEquals("tok-1", config.token)
        assertTrue(config.verifiedAt!! > 0)
    }

    @Test
    fun `tidak ada pembelian membuat status kembali gratis dan token dihapus`() {
        val config = FakeAppConfigRepository(premium = true).also { it.token = "lama" }

        sync(FakeBillingService(PurchaseQuery.NotOwned), config)

        assertFalse(runBlocking { config.observeIsPremium().first() })
        assertNull(config.token)
    }

    @Test
    fun `toko tidak terjangkau tidak mengubah cache sehingga pembeli tidak turun ke gratis saat offline`() {
        val config = FakeAppConfigRepository(premium = true)

        assertTrue(sync(FakeBillingService(PurchaseQuery.Unavailable("offline")), config).isSuccess)

        assertTrue(runBlocking { config.observeIsPremium().first() })
        assertEquals(0, config.writes)
    }

    @Test
    fun `pembelian berhasil menyimpan premium, dibatalkan atau gagal tidak mengubah apa pun`() {
        val config = FakeAppConfigRepository()
        fun buy(result: PurchaseResult) =
            runBlocking { PurchaseUnlimitedUseCase(FakeBillingService(purchase = result), config)() }

        assertEquals(UnlockOutcome.CANCELLED, buy(PurchaseResult.Cancelled).getOrThrow())
        assertEquals(UnlockOutcome.UNAVAILABLE, buy(PurchaseResult.Failure("x")).getOrThrow())
        assertEquals(0, config.writes)

        assertEquals(UnlockOutcome.ACTIVATED, buy(PurchaseResult.Success("tok-2")).getOrThrow())
        assertTrue(runBlocking { config.observeIsPremium().first() })
        assertEquals("tok-2", config.token)
    }

    @Test
    fun `restore menemukan pembelian dan menampilkan sukses, jika tidak ada hasilnya NOT_FOUND`() {
        val found = FakeAppConfigRepository()
        val billing = FakeBillingService(PurchaseQuery.Owned("tok-3"))
        assertEquals(UnlockOutcome.ACTIVATED, runBlocking { RestorePurchasesUseCase(billing, found)() }.getOrThrow())
        assertEquals(1, billing.queries)
        assertTrue(runBlocking { found.observeIsPremium().first() })

        val none = FakeAppConfigRepository()
        val result = runBlocking { RestorePurchasesUseCase(FakeBillingService(PurchaseQuery.NotOwned), none)() }
        assertEquals(UnlockOutcome.NOT_FOUND, result.getOrThrow())
        assertEquals(0, none.writes)

        val offline =
            runBlocking { RestorePurchasesUseCase(FakeBillingService(PurchaseQuery.Unavailable("x")), none)() }
        assertEquals(UnlockOutcome.UNAVAILABLE, offline.getOrThrow())
    }

    @Test
    fun `kegagalan menulis app_config dibungkus WriteFailed`() {
        val config = FakeAppConfigRepository(failWith = IOException("disk"))
        val failure = sync(FakeBillingService(PurchaseQuery.Owned("t")), config).exceptionOrNull()
        assertTrue(failure is QuestLogError.DatabaseError.WriteFailed)
    }

    @Test
    fun `batas tier gratis hilang segera setelah pembelian tanpa membuat ulang use case`() {
        val config = FakeAppConfigRepository(premium = false)
        val games = FakeGameRepository(existing = mapOf("A" to 1L), gameCount = 2)
        val save =
            SaveArticleUseCase(games, FakeArticleRepository(), FakeScheduler(), ObservePremiumStatusUseCase(config))
        fun trySave() = runBlocking { save("Game Baru", "Judul", listOf("https://example.com/a")) }

        assertEquals(TierError.GameLimitReached, trySave().exceptionOrNull())

        runBlocking { PurchaseUnlimitedUseCase(FakeBillingService(purchase = PurchaseResult.Success("t")), config)() }

        assertTrue(trySave().isSuccess)
    }
}
