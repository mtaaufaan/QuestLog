package com.rds.questlog.domain

import com.rds.questlog.domain.error.QuestLogError.DetectionFailed
import com.rds.questlog.domain.error.QuestLogError.PremiumError
import com.rds.questlog.domain.error.QuestLogError.ValidationError
import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.model.PremiumFeature
import com.rds.questlog.domain.model.PurchaseQuery
import com.rds.questlog.domain.scraper.PaginationDetector
import com.rds.questlog.domain.scraper.PaginationResult
import com.rds.questlog.domain.usecase.article.DetectPaginationUseCase
import com.rds.questlog.domain.usecase.premium.PremiumGuard
import com.rds.questlog.domain.usecase.premium.SyncPremiumStatusUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeDetector(private val result: PaginationResult) : PaginationDetector {
    val calls = mutableListOf<String>()
    override suspend fun detect(url: String): PaginationResult {
        calls += url
        return result
    }
}

/** QL-34: aturan usulan halaman lanjutan, guard premium, dan kegagalan. */
class DetectPaginationUseCaseTest {

    private val config = FakeAppConfigRepository(premium = false)
    private val guard = PremiumGuard(config)
    private val base = "https://site.com/guide?page=1"

    private fun page(n: Int) = "https://site.com/guide?page=$n"

    private fun links(vararg urls: String) = FakeDetector(PaginationResult.Links(urls.toList()))

    private fun premium() = runBlocking {
        SyncPremiumStatusUseCase(FakeBillingService(PurchaseQuery.Owned("tok")), config)()
    }

    private fun detect(detector: FakeDetector, taken: Set<String> = emptySet(), slots: Int = 9, url: String = base) =
        runBlocking { DetectPaginationUseCase(guard, detector)(url, taken, slots) }

    @Test
    fun `gratis dan premium hilang ditolak Locked tanpa mengambil halaman`() {
        val detector = links(page(2))

        assertEquals(PremiumError.Locked(PremiumFeature.AUTO_DETECT), detect(detector).exceptionOrNull())

        premium()
        runBlocking { SyncPremiumStatusUseCase(FakeBillingService(PurchaseQuery.NotOwned), config)() }
        assertEquals(PremiumError.Locked(PremiumFeature.AUTO_DETECT), detect(detector).exceptionOrNull())
        assertTrue(detector.calls.isEmpty())
    }

    @Test
    fun `premium mengambil hanya URL pertama dan mengembalikan usulan berurutan`() {
        premium()
        val detector = links(page(2), page(3), page(4))

        val result = detect(detector).getOrThrow()

        assertEquals(listOf(page(2), page(3), page(4)), result)
        assertEquals(listOf(base), detector.calls)
    }

    @Test
    fun `hanya https dan domain sama, www dianggap sama, fragmen dibuang`() {
        premium()
        val detector = links(
            "http://site.com/guide?page=2",
            "https://lain.com/guide?page=3",
            "https://sub.site.com/guide?page=4",
            "https://www.site.com/guide?page=5",
            "https://SITE.com/guide?page=6#isi",
            "ftp://site.com/x",
            "bukan url",
        )

        assertEquals(
            listOf("https://www.site.com/guide?page=5", "https://SITE.com/guide?page=6"),
            detect(detector).getOrThrow(),
        )
    }

    @Test
    fun `duplikat, URL sendiri, dan URL yang sudah ada di form atau artikel dibuang`() {
        premium()
        val detector = links(base, page(2), page(2), page(3), page(4))

        val result = detect(detector, taken = setOf(page(3), "https://site.com/guide?page=4"))

        assertEquals(listOf(page(2)), result.getOrThrow())
    }

    @Test
    fun `usulan dibatasi sisa slot dan urutan deteksi dipertahankan`() {
        premium()
        val detector = links(page(5), page(2), page(9), page(3), page(4))

        assertEquals(listOf(page(5), page(2), page(9)), detect(detector, slots = 3).getOrThrow())
    }

    @Test
    fun `tanpa slot tidak ada pemeriksaan dan hasilnya kosong`() {
        premium()
        val detector = links(page(2))

        assertEquals(emptyList<String>(), detect(detector, slots = 0).getOrThrow())
        assertTrue(detector.calls.isEmpty())
    }

    @Test
    fun `URL sumber yang bukan https ditolak sebelum mengambil halaman`() {
        premium()
        val detector = links(page(2))

        assertEquals(ValidationError.InvalidUrl, detect(detector, url = "http://site.com/guide").exceptionOrNull())
        assertEquals(ValidationError.InvalidUrl, detect(detector, url = "bukan url").exceptionOrNull())
        assertTrue(detector.calls.isEmpty())
    }

    @Test
    fun `gagal mengambil halaman menjadi DetectionFailed, bukan crash`() {
        premium()
        val detector = FakeDetector(PaginationResult.Failure(PageFailure.Http(500)))

        assertEquals(DetectionFailed(PageFailure.Http(500)), detect(detector).exceptionOrNull())
        assertEquals(
            DetectionFailed(PageFailure.Timeout),
            detect(FakeDetector(PaginationResult.Failure(PageFailure.Timeout))).exceptionOrNull(),
        )
    }

    @Test
    fun `halaman tanpa tautan lanjutan menghasilkan daftar kosong, bukan kegagalan`() {
        premium()

        assertEquals(emptyList<String>(), detect(links()).getOrThrow())
    }
}
