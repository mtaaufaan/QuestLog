package com.rds.questlog.domain

import com.rds.questlog.domain.error.QuestLogError.PremiumError
import com.rds.questlog.domain.error.QuestLogError.ValidationError
import com.rds.questlog.domain.model.Article
import com.rds.questlog.domain.model.Binder
import com.rds.questlog.domain.model.BinderCategory
import com.rds.questlog.domain.model.ContinueGameResult
import com.rds.questlog.domain.model.MAX_CATEGORY_NAME
import com.rds.questlog.domain.model.PremiumFeature
import com.rds.questlog.domain.model.PurchaseQuery
import com.rds.questlog.domain.model.SourcePage
import com.rds.questlog.domain.model.SourcePageStatus
import com.rds.questlog.domain.repository.BinderRepository
import com.rds.questlog.domain.repository.GameProgressRepository
import com.rds.questlog.domain.usecase.binder.AssignCategoriesUseCase
import com.rds.questlog.domain.usecase.binder.ContinueGameUseCase
import com.rds.questlog.domain.usecase.binder.CreateCategoryUseCase
import com.rds.questlog.domain.usecase.binder.DeleteCategoryUseCase
import com.rds.questlog.domain.usecase.binder.ObserveBinderUseCase
import com.rds.questlog.domain.usecase.binder.RenameCategoryUseCase
import com.rds.questlog.domain.usecase.binder.SetArticleCategoryUseCase
import com.rds.questlog.domain.usecase.binder.SetMainArticleUseCase
import com.rds.questlog.domain.usecase.premium.PremiumGuard
import com.rds.questlog.domain.usecase.premium.SyncPremiumStatusUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeBinderRepository : BinderRepository {
    val categories = MutableStateFlow<List<BinderCategory>>(emptyList())
    val members = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())
    private var nextId = 1L

    override fun observe(gameId: Long): Flow<Binder> = categories.map { all ->
        val mine = all.filter { it.gameId == gameId }
        Binder(mine, members.value.mapValues { (_, ids) -> ids.filter { id -> mine.any { it.id == id } }.toSet() })
    }

    override suspend fun findByName(gameId: Long, name: String) =
        categories.value.firstOrNull { it.gameId == gameId && it.name.equals(name, ignoreCase = true) }

    override suspend fun category(id: Long) = categories.value.firstOrNull { it.id == id }

    override suspend fun createCategory(gameId: Long, name: String): Long {
        val id = nextId++
        categories.value = categories.value + BinderCategory(id, gameId, name, categories.value.size + 1)
        return id
    }

    override suspend fun renameCategory(id: Long, name: String) {
        categories.value = categories.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun deleteCategory(id: Long) {
        categories.value = categories.value.filterNot { it.id == id }
        members.value = members.value.mapValues { (_, ids) -> ids - id }
    }

    override suspend fun setMembership(articleId: Long, categoryId: Long, member: Boolean) {
        val current = members.value[articleId].orEmpty()
        members.value = members.value + (articleId to if (member) current + categoryId else current - categoryId)
    }

    override suspend fun assign(articleId: Long, categoryIds: Set<Long>, newNames: List<String>) {
        val created = newNames.map { name -> findByName(1, name)?.id ?: createCategory(1, name) }
        members.value = members.value + (articleId to categoryIds + created)
    }
}

private class FakeGameProgressRepository(private val gameOf: Map<Long, Long> = mapOf(10L to 1L, 11L to 1L, 20L to 2L)) :
    GameProgressRepository {
    val main = mutableMapOf<Long, Long>()
    override suspend fun setMainArticle(articleId: Long) {
        main[gameOf.getValue(articleId)] = articleId
    }

    override suspend fun mainArticleId(gameId: Long): Long? = main[gameId]
}

/** QL-31/32/33: artikel utama, Lanjutkan Game, dan kategori Binder lewat PremiumGuard pada tiga keadaan. */
class BinderUseCasesTest {

    private val config = FakeAppConfigRepository(premium = false)
    private val guard = PremiumGuard(config)
    private val repo = FakeBinderRepository()

    private val create = CreateCategoryUseCase(guard, repo)
    private val rename = RenameCategoryUseCase(guard, repo)
    private val delete = DeleteCategoryUseCase(guard, repo)
    private val setMember = SetArticleCategoryUseCase(guard, repo)
    private val assign = AssignCategoriesUseCase(guard, repo)

    private fun sync(query: PurchaseQuery) = runBlocking {
        SyncPremiumStatusUseCase(
            FakeBillingService(query),
            config,
        )()
    }

    private fun goPremium() = sync(PurchaseQuery.Owned("tok"))

    private fun losePremium() {
        goPremium()
        sync(PurchaseQuery.NotOwned)
    }

    private fun names() = repo.categories.value.map { it.name }

    @Test
    fun `nama kategori dinormalisasi dan tidak ada batas jumlah kategori`() = runBlocking {
        goPremium()

        create(1, "  sidequest   penting ").getOrThrow()
        repeat(40) { create(1, "Kategori $it").getOrThrow() }

        assertEquals("Sidequest Penting", names().first())
        assertEquals(41, names().size)
    }

    @Test
    fun `nama kosong, kepanjangan, atau duplikat tanpa peduli huruf ditolak tanpa mengubah data`() = runBlocking {
        goPremium()
        create(1, "Koleksi").getOrThrow()

        assertEquals(ValidationError.EmptyCategoryName, create(1, "   ").exceptionOrNull())
        assertEquals(
            ValidationError.CategoryNameTooLong,
            create(1, "x".repeat(MAX_CATEGORY_NAME + 1)).exceptionOrNull(),
        )
        assertEquals(ValidationError.DuplicateCategory("Koleksi"), create(1, " koleksi ").exceptionOrNull())

        assertEquals(listOf("Koleksi"), names())
        // Nama yang sama boleh dipakai di game lain, dan tepat 100 karakter masih sah.
        assertTrue(create(2, "Koleksi").isSuccess)
        assertTrue(create(1, "A".repeat(MAX_CATEGORY_NAME)).isSuccess)
    }

    @Test
    fun `ganti nama mengecualikan nama sendiri tetapi menolak nama kategori lain`() = runBlocking {
        goPremium()
        val a = create(1, "Utama").getOrThrow()
        val b = create(1, "Koleksi").getOrThrow()

        assertTrue(rename(a, "utama").isSuccess)
        // Pesan memakai nama yang diketik (sudah dinormalisasi), sama dengan pesan di Category Name Sheet.
        assertEquals(ValidationError.DuplicateCategory("KOLEKSI"), rename(a, "KOLEKSI").exceptionOrNull())
        assertEquals(ValidationError.EmptyCategoryName, rename(b, " ").exceptionOrNull())
        assertEquals(ValidationError.CategoryNotFound, rename(99, "Baru").exceptionOrNull())

        assertEquals(listOf("Utama", "Koleksi"), names())
    }

    @Test
    fun `hapus kategori hanya mencabut relasi, artikel tetap ada`() = runBlocking {
        goPremium()
        val a = create(1, "Utama").getOrThrow()
        setMember(10, a, true).getOrThrow()

        delete(a).getOrThrow()

        assertEquals(emptyList<String>(), names())
        assertEquals(emptySet<Long>(), repo.members.value.getValue(10))
    }

    @Test
    fun `satu artikel boleh masuk beberapa kategori dan kategori baru dibuat dari sheet tanpa duplikat`() =
        runBlocking {
            goPremium()
            val existing = create(1, "Koleksi").getOrThrow()

            assign(10, setOf(existing), listOf("boss", "Boss", " koleksi")).getOrThrow()

            assertEquals(listOf("Koleksi", "Boss"), names())
            assertEquals(2, repo.members.value.getValue(10).size)
            assertEquals(ValidationError.EmptyCategoryName, assign(10, emptySet(), listOf("  ")).exceptionOrNull())
        }

    @Test
    fun `gratis ditolak Locked dan premium hilang ReadOnly, data tidak berubah, lalu kembali bisa diubah`() =
        runBlocking {
            val feature = PremiumFeature.GAME_BINDER
            assertEquals(PremiumError.Locked(feature), create(1, "Utama").exceptionOrNull())
            assertEquals(PremiumError.Locked(feature), assign(10, emptySet(), listOf("Baru")).exceptionOrNull())
            assertTrue(names().isEmpty())

            goPremium()
            val id = create(1, "Utama").getOrThrow()
            setMember(10, id, true).getOrThrow()

            losePremium()
            assertEquals(PremiumError.ReadOnly(feature), create(1, "Baru").exceptionOrNull())
            assertEquals(PremiumError.ReadOnly(feature), rename(id, "Ganti").exceptionOrNull())
            assertEquals(PremiumError.ReadOnly(feature), delete(id).exceptionOrNull())
            assertEquals(PremiumError.ReadOnly(feature), setMember(11, id, true).exceptionOrNull())
            assertEquals(PremiumError.ReadOnly(feature), assign(10, emptySet(), emptyList()).exceptionOrNull())
            // Membaca tetap jalan dan data utuh.
            val binder = ObserveBinderUseCase(repo)(1).first()
            assertEquals(listOf("Utama"), binder.categories.map { it.name })
            assertEquals(setOf(id), binder.assignments.getValue(10))

            goPremium()
            assertTrue(rename(id, "Jalur Utama").isSuccess)
            assertEquals(setOf(id), ObserveBinderUseCase(repo)(1).first().assignments.getValue(10))
        }

    // ---------- QL-31 ----------

    private fun article(id: Long, vararg statuses: SourcePageStatus) = Article(
        id = id,
        gameId = 1,
        gameName = "G",
        title = "A$id",
        pages = statuses.mapIndexed { i, s -> SourcePage(i + 1L, "https://x/$i", i + 1, s) },
        checkpointNodeId = null,
        lastVisitedNodeId = null,
        lastReadAt = null,
        createdAt = 0,
    )

    @Test
    fun `artikel utama satu per game dan yang baru menimpa yang lama`() = runBlocking {
        val progress = FakeGameProgressRepository()
        val set = SetMainArticleUseCase(progress)

        set(10).getOrThrow()
        set(20).getOrThrow()
        set(11).getOrThrow()

        assertEquals(11L, progress.main[1])
        assertEquals(20L, progress.main[2])
    }

    @Test
    fun `Lanjutkan Game membuka artikel utama bila sudah siap, termasuk sebagian gagal`() = runBlocking {
        val progress = FakeGameProgressRepository().apply { main[1] = 10 }
        val ready = FakeArticleRepository(article = article(10, SourcePageStatus.COMPLETED, SourcePageStatus.FAILED))

        assertEquals(ContinueGameResult.Open(10), ContinueGameUseCase(progress, ready)(1))
    }

    @Test
    fun `Lanjutkan Game masih diproses bila artikel utama SCRAPING`() = runBlocking {
        val progress = FakeGameProgressRepository().apply { main[1] = 10 }
        val scraping =
            FakeArticleRepository(article = article(10, SourcePageStatus.COMPLETED, SourcePageStatus.PENDING))

        assertEquals(ContinueGameResult.StillScraping, ContinueGameUseCase(progress, scraping)(1))
    }

    @Test
    fun `Lanjutkan Game sudah dihapus bila penanda atau artikelnya tidak ada`() = runBlocking {
        val withMark = FakeGameProgressRepository().apply { main[1] = 10 }
        val noMark = FakeGameProgressRepository()

        assertEquals(
            ContinueGameResult.ArticleGone,
            ContinueGameUseCase(withMark, FakeArticleRepository(article = null))(1),
        )
        assertEquals(
            ContinueGameResult.ArticleGone,
            ContinueGameUseCase(noMark, FakeArticleRepository(article = article(10)))(1),
        )
        assertNull(noMark.main[1])
    }
}
