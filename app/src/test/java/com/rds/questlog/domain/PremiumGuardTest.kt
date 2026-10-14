package com.rds.questlog.domain

import com.rds.questlog.domain.error.QuestLogError.PremiumError
import com.rds.questlog.domain.model.FeatureAccess
import com.rds.questlog.domain.model.PremiumFeature
import com.rds.questlog.domain.model.PremiumState
import com.rds.questlog.domain.model.PurchaseQuery
import com.rds.questlog.domain.usecase.premium.PremiumGuard
import com.rds.questlog.domain.usecase.premium.SyncPremiumStatusUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** QL-26: tiga keadaan guard fitur premium (gratis, premium, premium hilang) dan verifikasi yang gagal. */
class PremiumGuardTest {

    private val config = FakeAppConfigRepository(premium = false)
    private val guard = PremiumGuard(config)

    private fun sync(query: PurchaseQuery) = runBlocking {
        SyncPremiumStatusUseCase(
            FakeBillingService(query),
            config,
        )()
    }

    private fun state() = runBlocking { guard.state().first() }

    private fun access(feature: PremiumFeature) = runBlocking { guard.access(feature).first() }

    private fun writeError(feature: PremiumFeature): Throwable? =
        runBlocking { runCatching { guard.requireWrite(feature) }.exceptionOrNull() }

    @Test
    fun `pengguna yang belum pernah premium terkunci dan ajakan upgrade, bukan hanya-baca`() {
        assertEquals(PremiumState.FREE, state())
        PremiumFeature.entries.forEach { feature ->
            assertEquals(FeatureAccess.LOCKED, access(feature))
            assertEquals(PremiumError.Locked(feature), writeError(feature))
        }
    }

    @Test
    fun `premium membuka seluruh fitur`() {
        sync(PurchaseQuery.Owned("tok"))

        assertEquals(PremiumState.PREMIUM, state())
        PremiumFeature.entries.forEach { feature ->
            assertEquals(FeatureAccess.FULL, access(feature))
            assertNull(writeError(feature))
        }
    }

    @Test
    fun `premium hilang setelah Billing mengonfirmasi tidak ada pembelian membuat data lama hanya-baca`() {
        sync(PurchaseQuery.Owned("tok"))

        sync(PurchaseQuery.NotOwned)

        assertEquals(PremiumState.LOST, state())
        listOf(PremiumFeature.BOOKMARK, PremiumFeature.NOTE, PremiumFeature.GAME_BINDER).forEach { feature ->
            assertEquals(FeatureAccess.READ_ONLY, access(feature))
            assertEquals(PremiumError.ReadOnly(feature), writeError(feature))
        }
        // Auto-detect tidak punya data lama untuk dibaca: tetap terkunci biasa.
        assertEquals(FeatureAccess.LOCKED, access(PremiumFeature.AUTO_DETECT))
        assertEquals(PremiumError.Locked(PremiumFeature.AUTO_DETECT), writeError(PremiumFeature.AUTO_DETECT))
    }

    @Test
    fun `verifikasi gagal atau offline mempertahankan status terakhir`() {
        sync(PurchaseQuery.Owned("tok"))
        sync(PurchaseQuery.Unavailable("offline"))
        assertEquals(PremiumState.PREMIUM, state())
        assertEquals(FeatureAccess.FULL, access(PremiumFeature.BOOKMARK))

        sync(PurchaseQuery.NotOwned)
        sync(PurchaseQuery.Unavailable("Play tidak tersedia"))
        assertEquals(PremiumState.LOST, state())
    }

    @Test
    fun `pengguna gratis yang diperiksa tanpa pembelian tetap gratis, bukan hilang`() {
        sync(PurchaseQuery.NotOwned)
        sync(PurchaseQuery.Unavailable("offline"))

        assertEquals(PremiumState.FREE, state())
    }

    @Test
    fun `premium kembali memulihkan akses penuh`() {
        sync(PurchaseQuery.Owned("tok"))
        sync(PurchaseQuery.NotOwned)

        sync(PurchaseQuery.Owned("tok-baru"))

        assertEquals(PremiumState.PREMIUM, state())
        assertEquals(FeatureAccess.FULL, access(PremiumFeature.NOTE))
        assertNull(writeError(PremiumFeature.NOTE))
    }

    @Test
    fun `status premium lama sebelum penanda ada tetap terdeteksi hilang saat turun`() {
        // Pengguna premium sejak sebelum v1: is_premium sudah '1' tanpa penanda was_premium.
        val legacy = FakeAppConfigRepository(premium = true)
        val legacyGuard = PremiumGuard(legacy)

        runBlocking { SyncPremiumStatusUseCase(FakeBillingService(PurchaseQuery.NotOwned), legacy)() }

        assertEquals(PremiumState.LOST, runBlocking { legacyGuard.state().first() })
    }
}
