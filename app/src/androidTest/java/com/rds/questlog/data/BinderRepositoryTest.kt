package com.rds.questlog.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rds.questlog.data.repository.BinderRepositoryImpl
import com.rds.questlog.data.repository.GameProgressRepositoryImpl
import com.rds.questlog.domain.model.GameTarget
import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.ScrapedNode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** QL-31/32/33: artikel utama dan kategori Binder di Room nyata, termasuk cascade dan pindah game. */
@RunWith(AndroidJUnit4::class)
class BinderRepositoryTest {

    private lateinit var env: TestEnvironment
    private lateinit var binder: BinderRepositoryImpl
    private lateinit var progress: GameProgressRepositoryImpl
    private var gameA = 0L
    private var gameB = 0L
    private var a1 = 0L
    private var a2 = 0L
    private var b1 = 0L

    @Before
    fun setUp() = runBlocking {
        env = TestEnvironment()
        binder = BinderRepositoryImpl(env.db, env.db.binderDao(), env.db.articleDao())
        progress = GameProgressRepositoryImpl(env.db, env.db.gameProgressDao(), env.db.articleDao())
        gameA = env.games.findOrCreate("Game A")
        gameB = env.games.findOrCreate("Game B")
        a1 = article(gameA, "Artikel 1")
        a2 = article(gameA, "Artikel 2")
        b1 = article(gameB, "Artikel B")
    }

    @After
    fun tearDown() = env.close()

    private suspend fun article(game: Long, title: String): Long {
        val id = env.articles.insertArticle(game, title, listOf("https://x/$title"))
        val page = env.db.pageLayoutDao().getByArticle(id).first().id
        env.pages.saveScrapedPage(id, page, List(2) { ScrapedNode(NodeType.P, "$title-${it + 1}") }, emptyList())
        return id
    }

    private fun article(id: Long) = runBlocking { env.articles.observeArticle(id).first()!! }

    @Test
    fun artikelUtamaSatuPerGameDanYangBaruMenimpaPenandaAdaDiDaftarDanArtikel() = runBlocking {
        progress.setMainArticle(a1)
        progress.setMainArticle(a2)
        progress.setMainArticle(b1)

        assertEquals(a2, progress.mainArticleId(gameA))
        assertEquals(b1, progress.mainArticleId(gameB))
        assertEquals(1, env.count("SELECT COUNT(*) FROM game_progress WHERE game_id = $gameA"))
        val games = env.games.observeGames().first().associateBy { it.id }
        assertEquals("Artikel 2", games.getValue(gameA).mainArticleTitle)
        assertEquals(a2, games.getValue(gameA).mainArticleId)
        assertTrue(article(a2).isMainArticle)
        assertEquals(false, article(a1).isMainArticle)
        assertEquals(a2, article(a1).gameMainArticleId)
    }

    @Test
    fun artikelUtamaTidakMengubahCheckpointDanPosisiTerakhir() = runBlocking {
        val node = env.articles.observeContent(a1).first().first().id
        env.checkpoints.setCheckpoint(a1, node)
        env.checkpoints.saveLastPosition(a1, node)

        progress.setMainArticle(a1)

        val after = article(a1)
        assertEquals(node, after.checkpointNodeId)
        assertEquals(node, after.lastVisitedNodeId)
    }

    @Test
    fun penandaDikosongkanBilaArtikelUtamaDihapusAtauDipindahGame() = runBlocking {
        progress.setMainArticle(a1)
        env.management.updateDetails(a1, "Artikel 1", GameTarget.Existing(gameB))
        assertNull(progress.mainArticleId(gameA))

        progress.setMainArticle(a2)
        env.articles.deleteArticle(a2)
        assertNull(progress.mainArticleId(gameA))
        assertEquals(0, env.count("SELECT COUNT(*) FROM game_progress WHERE game_id = $gameA"))
    }

    @Test
    fun kategoriUnikPerGameTanpaPedulikanHurufDanTanpaBatasJumlah() = runBlocking {
        val id = binder.createCategory(gameA, "Koleksi")
        repeat(50) { binder.createCategory(gameA, "Kategori $it") }

        assertEquals(id, binder.findByName(gameA, "KOLEKSI")?.id)
        assertNull(binder.findByName(gameB, "Koleksi"))
        assertTrue(runCatching { binder.createCategory(gameA, "koleksi") }.isFailure)
        assertEquals(51, binder.observe(gameA).first().categories.size)
        // Urutan tampil mengikuti urutan dibuat.
        assertEquals("Koleksi", binder.observe(gameA).first().categories.first().name)
    }

    @Test
    fun artikelBolehMasukBeberapaKategoriDanHapusKategoriHanyaMencabutRelasi() = runBlocking {
        val utama = binder.createCategory(gameA, "Utama")
        val koleksi = binder.createCategory(gameA, "Koleksi")

        binder.assign(a1, setOf(utama), listOf("Boss"))
        binder.setMembership(a1, koleksi, true)
        binder.setMembership(a2, koleksi, true)

        val data = binder.observe(gameA).first()
        assertEquals(3, data.assignments.getValue(a1).size)
        assertEquals(setOf(koleksi), data.assignments.getValue(a2))
        assertEquals(listOf("Boss", "Koleksi", "Utama"), article(a1).categoryNames.sorted())

        binder.deleteCategory(koleksi)

        assertEquals(2, binder.observe(gameA).first().assignments.getValue(a1).size)
        assertTrue(binder.observe(gameA).first().assignments[a2].isNullOrEmpty())
        assertEquals(3, env.count("SELECT COUNT(*) FROM articles"))
    }

    @Test
    fun assignMenggantiPenetapanDanMemakaiUlangNamaYangSudahAda() = runBlocking {
        val utama = binder.createCategory(gameA, "Utama")
        binder.assign(a1, setOf(utama), emptyList())

        binder.assign(a1, emptySet(), listOf("utama"))

        assertEquals(1, binder.observe(gameA).first().categories.size)
        assertEquals(setOf(utama), binder.observe(gameA).first().assignments.getValue(a1))
    }

    @Test
    fun kategoriGameLainDitolakDiPenetapan() = runBlocking {
        val other = binder.createCategory(gameB, "Milik B")

        assertTrue(runCatching { binder.setMembership(a1, other, true) }.isFailure)
        binder.assign(a1, setOf(other), emptyList())

        assertTrue(binder.observe(gameA).first().assignments[a1].isNullOrEmpty())
        assertEquals(0, env.count("SELECT COUNT(*) FROM article_binder"))
    }

    @Test
    fun pindahGameMencabutKategoriTanpaMenghapusKategoriGameAsal() = runBlocking {
        val utama = binder.createCategory(gameA, "Utama")
        binder.setMembership(a1, utama, true)

        env.management.updateDetails(a1, "Artikel 1", GameTarget.Existing(gameB))

        assertEquals(emptyList<String>(), article(a1).categoryNames)
        assertEquals(listOf("Utama"), binder.observe(gameA).first().categories.map { it.name })
        assertEquals(0, env.count("SELECT COUNT(*) FROM article_binder"))
    }

    @Test
    fun hapusGameMenghapusKategoriDanRelasinya() = runBlocking {
        val utama = binder.createCategory(gameA, "Utama")
        binder.setMembership(a1, utama, true)
        progress.setMainArticle(a1)

        env.games.deleteGame(gameA)

        assertEquals(0, env.count("SELECT COUNT(*) FROM binder_categories"))
        assertEquals(0, env.count("SELECT COUNT(*) FROM article_binder"))
        assertEquals(0, env.count("SELECT COUNT(*) FROM game_progress"))
        assertEquals(1, env.count("SELECT COUNT(*) FROM articles"))
    }
}
