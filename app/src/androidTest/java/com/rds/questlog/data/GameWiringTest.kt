package com.rds.questlog.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rds.questlog.domain.model.GameTarget
import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.ScrapedNode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** QL-36/QL-28/QL-30: tabel v1.1 dan cascade, daftar game, hapus game, dan pencarian lintas game. */
@RunWith(AndroidJUnit4::class)
class GameWiringTest {

    private lateinit var env: TestEnvironment
    private var gameA = 0L
    private var gameB = 0L
    private var a1 = 0L
    private var a2 = 0L
    private var b1 = 0L

    @Before
    fun setUp() = runBlocking {
        env = TestEnvironment()
        gameA = env.games.findOrCreate("Dragon Quest VII")
        gameB = env.games.findOrCreate("Breath Of Fire III")
        a1 = article(gameA, "Walkthrough Lengkap")
        a2 = article(gameA, "Lokasi Mini Medal")
        b1 = article(gameB, "Walkthrough Utama")
    }

    @After
    fun tearDown() = env.close()

    private suspend fun article(game: Long, title: String): Long {
        val id = env.articles.insertArticle(game, title, listOf("https://x/$title"))
        val page = env.db.pageLayoutDao().getByArticle(id).first().id
        env.pages.saveScrapedPage(id, page, List(2) { ScrapedNode(NodeType.P, "$title-${it + 1}") }, emptyList())
        return id
    }

    private fun sql(statement: String) = env.db.openHelper.writableDatabase.execSQL(statement)

    private fun count(table: String) = env.count("SELECT COUNT(*) FROM $table")

    private fun nodeOf(articleId: Long) = env.count("SELECT MIN(id) FROM content_nodes WHERE article_id = $articleId")

    private fun seedGameData(game: Long, article: Long) {
        sql("INSERT INTO game_progress (game_id, article_id) VALUES ($game, $article)")
        sql(
            "INSERT INTO binder_categories (id, game_id, name, position, created_at) " +
                "VALUES ($game, $game, 'Utama', 1, 1)",
        )
        sql("INSERT INTO article_binder (article_id, category_id) VALUES ($article, $game)")
        sql(
            "INSERT INTO bookmarks (article_id, node_id, fallback_order, label, created_at) " +
                "VALUES ($article, ${nodeOf(article)}, 1, 'Boss', 1)",
        )
        sql("INSERT INTO article_notes (article_id, content, updated_at) VALUES ($article, 'catatan', 1)")
    }

    @Test
    fun hapusGameMenghapusArtikelPenandaUtamaKategoriBookmarkDanCatatanTanpaMenyentuhGameLain() = runBlocking {
        seedGameData(gameA, a1)
        seedGameData(gameB, b1)

        env.games.deleteGame(gameA)

        assertEquals(listOf("Breath Of Fire III"), env.games.observeGames().first().map { it.name })
        assertEquals(1, count("articles"))
        assertEquals(1, count("game_progress"))
        assertEquals(1, count("binder_categories"))
        assertEquals(1, count("article_binder"))
        assertEquals(1, count("bookmarks"))
        assertEquals(1, count("article_notes"))
        assertEquals(2, count("content_nodes"))
    }

    @Test
    fun hapusArtikelUtamaMenghapusBarisnyaDanHapusKategoriHanyaMencabutRelasi() = runBlocking {
        seedGameData(gameA, a1)
        sql("INSERT INTO article_binder (article_id, category_id) VALUES ($a2, $gameA)")

        sql("DELETE FROM binder_categories WHERE id = $gameA")
        assertEquals(0, count("article_binder"))
        assertEquals(3, count("articles"))
        assertEquals(1, count("game_progress"))

        env.articles.deleteArticle(a1)
        assertEquals(0, count("game_progress"))
        assertEquals(1, env.games.observeGames().first().count { it.id == gameA })
    }

    @Test
    fun hapusArtikelTerakhirMenghapusGameDanDataTurunannya() = runBlocking {
        seedGameData(gameB, b1)

        env.articles.deleteArticle(b1)

        assertEquals(listOf(gameA), env.games.observeGames().first().map { it.id })
        assertEquals(0, env.count("SELECT COUNT(*) FROM games WHERE id = $gameB"))
        assertEquals(0, count("game_progress"))
        assertEquals(0, count("binder_categories"))
    }

    @Test
    fun pindahGameMencabutPenandaUtamaDanKategoriGameAsal() = runBlocking {
        seedGameData(gameA, a1)
        seedGameData(gameB, b1)

        env.management.updateDetails(a1, "Walkthrough Lengkap", GameTarget.Existing(gameB))

        assertEquals(0, env.count("SELECT COUNT(*) FROM game_progress WHERE game_id = $gameA"))
        assertEquals(1, env.count("SELECT COUNT(*) FROM game_progress WHERE game_id = $gameB"))
        // Kategori game asal tidak berlaku di game baru; relasi artikel b1 ke kategori game B tetap.
        assertEquals(1, count("article_binder"))
        assertEquals(1, env.count("SELECT COUNT(*) FROM article_binder WHERE article_id = $b1"))
    }

    @Test
    fun daftarGameMemuatJumlahArtikelDanArtikelTerakhirDibacaDanTidakMenampilkanGameKosong() = runBlocking {
        val empty = env.games.findOrCreate("Game Kosong")
        env.checkpoints.saveLastPosition(a2, nodeOf(a2).toLong())

        val games = env.games.observeGames().first()

        assertEquals(listOf("Dragon Quest VII", "Breath Of Fire III"), games.map { it.name })
        assertEquals(listOf(2, 1), games.map { it.articleCount })
        assertEquals("Lokasi Mini Medal", games.first().lastArticleTitle)
        assertTrue(games.first().lastReadAt != null)
        assertEquals(null, games.last().lastArticleTitle)
        // Game kosong peninggalan tidak dihitung ke batas tier, dan dipakai ulang bila namanya dipakai lagi.
        assertEquals(2, env.games.count())
        assertEquals(empty, env.games.findOrCreate("game kosong"))
    }

    @Test
    fun pencarianLintasGameTidakPedulikanHurufMencocokkanSebagianDanMemuatNamaGame() = runBlocking {
        val hits = env.articles.observeArticles("WALK", null).first()

        assertEquals(setOf("Walkthrough Lengkap", "Walkthrough Utama"), hits.map { it.title }.toSet())
        assertEquals(setOf("Dragon Quest VII", "Breath Of Fire III"), hits.map { it.gameName }.toSet())
        assertTrue(env.articles.observeArticles("tidak ada", null).first().isEmpty())
        assertEquals(3, env.articles.observeArticles("", null).first().size)
    }
}
