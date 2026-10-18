package com.rds.questlog.domain

import com.rds.questlog.domain.error.QuestLogError.PremiumError
import com.rds.questlog.domain.model.LOGO_FRAME
import com.rds.questlog.domain.model.LogoCrop
import com.rds.questlog.domain.model.PremiumFeature
import com.rds.questlog.domain.model.clamped
import com.rds.questlog.domain.model.sourceRect
import com.rds.questlog.domain.repository.GameLogoRepository
import com.rds.questlog.domain.usecase.game.RemoveGameLogoUseCase
import com.rds.questlog.domain.usecase.game.RestoreGameLogoUseCase
import com.rds.questlog.domain.usecase.game.SaveGameLogoUseCase
import com.rds.questlog.domain.usecase.premium.PremiumGuard
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeGameLogoRepository : GameLogoRepository {
    val calls = mutableListOf<String>()
    override suspend fun save(gameId: Long, source: String?, crop: LogoCrop) {
        calls += "save:$gameId:$source:${crop.zoom}"
    }

    override suspend fun remove(gameId: Long) {
        calls += "remove:$gameId"
    }

    override suspend fun restore(gameId: Long) {
        calls += "restore:$gameId"
    }
}

/** QL-38: aturan crop logo (bingkai selalu tertutup gambar) dan guard premium pada use case logo. */
class GameLogoTest {

    private val delta = 0.001f

    @Test
    fun `tanpa zoom dan geser area sumber adalah persegi tengah gambar`() {
        val rect = LogoCrop().sourceRect(400, 200)

        assertEquals(100f, rect.left, delta)
        assertEquals(0f, rect.top, delta)
        assertEquals(200f, rect.size, delta)
    }

    @Test
    fun `geser ke kanan menggeser area sumber ke kiri sebanding satuan bingkai`() {
        // 400x200: 1 satuan bingkai = 200 / 264 px; geser 66 satuan = 50 px.
        val rect = LogoCrop(1f, 66f, 0f).sourceRect(400, 200)

        assertEquals(50f, rect.left, delta)
        assertEquals(200f, rect.size, delta)
    }

    @Test
    fun `zoom memperkecil area sumber dan geser dibatasi agar tetap di dalam gambar`() {
        val rect = LogoCrop(2f, 99_999f, -99_999f).sourceRect(400, 200)

        assertEquals(100f, rect.size, delta)
        assertEquals(0f, rect.left, delta)
        assertEquals(100f, rect.top, delta)
    }

    @Test
    fun `clamp membatasi zoom dan geser menurut perbandingan gambar`() {
        val tall = LogoCrop(9f, 5_000f, 5_000f).clamped(aspect = 0.5f)

        assertEquals(4f, tall.zoom, delta)
        // Gambar tinggi 1:2 pada zoom 4: lebar 4 bingkai, tinggi 8 bingkai.
        assertEquals((LOGO_FRAME * 4 - LOGO_FRAME) / 2f, tall.x, delta)
        assertEquals((LOGO_FRAME * 8 - LOGO_FRAME) / 2f, tall.y, delta)
        assertEquals(LogoCrop(1f, 0f, 0f), LogoCrop(0.2f, 80f, 80f).clamped(aspect = 1f))
    }

    @Test
    fun `area sumber selalu berada di dalam gambar untuk sembarang crop`() {
        val crops = listOf(LogoCrop(1f, 500f, 0f), LogoCrop(3.3f, -400f, 250f), LogoCrop(4f, 1_000f, 1_000f))
        for ((w, h) in listOf(400 to 200, 200 to 400, 300 to 300)) {
            for (crop in crops) {
                val r = crop.sourceRect(w, h)
                assertTrue(r.left >= -delta && r.top >= -delta)
                assertTrue(r.left + r.size <= w + delta && r.top + r.size <= h + delta)
            }
        }
    }

    private val config = FakeAppConfigRepository(premium = false)
    private val repo = FakeGameLogoRepository()
    private val guard = PremiumGuard(config)
    private val save = SaveGameLogoUseCase(guard, repo)
    private val remove = RemoveGameLogoUseCase(guard, repo)
    private val restore = RestoreGameLogoUseCase(guard, repo)

    @Test
    fun `gratis ditolak terkunci dan tidak menyentuh repository`() = runBlocking {
        val error = save(1, "content://x", LogoCrop()).exceptionOrNull()

        assertEquals(PremiumError.Locked(PremiumFeature.GAME_LOGO), error)
        assertTrue(repo.calls.isEmpty())
    }

    @Test
    fun `premium menyimpan menghapus dan memulihkan lewat repository`() = runBlocking {
        config.setPremium(true, "tok", 1)

        save(1, "content://x", LogoCrop(2f)).getOrThrow()
        save(1, null, LogoCrop()).getOrThrow()
        remove(1).getOrThrow()
        restore(1).getOrThrow()

        assertEquals(listOf("save:1:content://x:2.0", "save:1:null:1.0", "remove:1", "restore:1"), repo.calls)
    }

    @Test
    fun `premium hilang menjadi hanya-baca untuk simpan hapus dan urungkan`() = runBlocking {
        config.setPremium(true, "tok", 1)
        config.setPremium(false, null, 2)

        assertEquals(PremiumError.ReadOnly(PremiumFeature.GAME_LOGO), save(1, null, LogoCrop()).exceptionOrNull())
        assertEquals(PremiumError.ReadOnly(PremiumFeature.GAME_LOGO), remove(1).exceptionOrNull())
        assertEquals(PremiumError.ReadOnly(PremiumFeature.GAME_LOGO), restore(1).exceptionOrNull())
        assertTrue(repo.calls.isEmpty())
    }
}
