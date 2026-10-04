package com.rds.questlog.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.model.ScrapedImage
import com.rds.questlog.domain.model.ScrapedNode
import com.rds.questlog.domain.model.ScrapingStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Search/filter (QL-10/11) sebagai query nyata ke Room, status turunan, retry, dan hapus berantai. */
@RunWith(AndroidJUnit4::class)
class ArticleQueriesTest {

    private lateinit var env: TestEnvironment
    private var dragonQuest = 0L
    private var breathOfFire = 0L

    @Before
    fun setUp() = runBlocking {
        env = TestEnvironment()
        dragonQuest = env.games.findOrCreate("Dragon Quest VII")
        breathOfFire = env.games.findOrCreate("Breath Of Fire III")
        env.articles.insertArticle(dragonQuest, "Walkthrough Lengkap — Disc 1", listOf("https://a/1"))
        env.articles.insertArticle(dragonQuest, "Lokasi Semua Mini Medal", listOf("https://a/2"))
        env.articles.insertArticle(breathOfFire, "Walkthrough Utama", listOf("https://a/3"))
        env.articles.insertArticle(breathOfFire, "Skill 100% & Master_List", listOf("https://a/4"))
        Unit
    }

    @After
    fun tearDown() = env.close()

    private fun titles(query: String, gameId: Long?) =
        runBlocking { env.articles.observeArticles(query, gameId).first() }.map { it.title }

    @Test
    fun searchJudulTidakPedulikanHurufBesarDanCocokSebagian() {
        assertEquals(setOf("Walkthrough Lengkap — Disc 1", "Walkthrough Utama"), titles("WALK", null).toSet())
        assertEquals(listOf("Lokasi Semua Mini Medal"), titles("mini med", null))
        assertTrue(titles("tidak ada", null).isEmpty())
    }

    @Test
    fun karakterKhususLikeDicocokkanApaAdanya() {
        assertEquals(listOf("Skill 100% & Master_List"), titles("100%", null))
        assertEquals(listOf("Skill 100% & Master_List"), titles("r_L", null))
        // "%" sendirian tidak boleh menjadi wildcard yang cocok dengan semua artikel.
        assertEquals(listOf("Skill 100% & Master_List"), titles("%", null))
    }

    @Test
    fun filterGameDanGabunganDenganSearch() {
        assertEquals(2, titles("", dragonQuest).size)
        assertEquals(2, titles("", breathOfFire).size)
        assertEquals(listOf("Walkthrough Utama"), titles("walk", breathOfFire))
        assertTrue(titles("mini medal", breathOfFire).isEmpty())
        assertEquals(4, titles("", null).size)
    }

    @Test
    fun artikelTerbaruTampilPertama() {
        assertEquals("Skill 100% & Master_List", titles("", null).first())
    }

    @Test
    fun jumlahArtikelPerGameUntukFilterSheet() = runBlocking {
        val games = env.games.observeGames().first()
        assertEquals(
            listOf("Dragon Quest VII" to 2, "Breath Of Fire III" to 2),
            games.map { it.name to it.articleCount },
        )
        assertEquals(dragonQuest, env.games.findOrCreate("dragon quest vii"))
        assertEquals(2, env.games.observeGames().first().size)
    }

    @Test
    fun statusTurunanDariHalamanDanRetryMengulangSemuaYangGagal() = runBlocking {
        val id = env.articles.insertArticle(
            dragonQuest,
            "Tiga halaman",
            listOf("https://b/1", "https://b/2", "https://b/3"),
        )
        fun article() = runBlocking { env.articles.observeArticles("Tiga", null).first().single() }
        assertEquals(ScrapingStatus.SCRAPING, article().status)

        val pageIds = article().pages.map { it.id }
        env.pages.saveScrapedPage(id, pageIds[0], listOf(ScrapedNode(NodeType.P, "isi")), emptyList())
        env.pages.markFailed(pageIds[1], PageFailure.Http(404))
        env.pages.markFailed(pageIds[2], PageFailure.Timeout)
        env.pages.updateScrapingDone(id)

        assertEquals(ScrapingStatus.READY, article().status) // partial: ada yang COMPLETED
        assertEquals(listOf(PageFailure.Http(404), PageFailure.Timeout), article().pages.mapNotNull { it.failure })
        assertEquals(1, env.count("SELECT is_scraping_done FROM articles WHERE id = $id"))

        assertEquals(2, env.articles.retryFailedPages(id))
        assertEquals(ScrapingStatus.SCRAPING, article().status)
        assertTrue(article().pages.all { it.failure == null })
        assertEquals(0, env.count("SELECT is_scraping_done FROM articles WHERE id = $id"))
        assertEquals(0, env.articles.retryFailedPages(id)) // tidak ada lagi yang gagal
    }

    @Test
    fun semuaHalamanGagalBerartiError() = runBlocking {
        val id = env.articles.insertArticle(dragonQuest, "Gagal total", listOf("https://c/1", "https://c/2"))
        env.articles.observeArticles("Gagal", null).first().single().pages.forEach {
            env.pages.markFailed(it.id, PageFailure.ConnectionFailed)
        }
        env.pages.updateScrapingDone(id)
        assertEquals(ScrapingStatus.ERROR, env.articles.observeArticles("Gagal", null).first().single().status)
    }

    @Test
    fun hapusArtikelMenghapusHalamanKontenCheckpointDanFileGambar() = runBlocking {
        val id = env.articles.insertArticle(dragonQuest, "Akan dihapus", listOf("https://d/1"))
        val pageId = env.articles.observeArticles("dihapus", null).first().single().pages.single().id
        val image = fakeImage("hapus-uji-1.webp", "https://img/1")
        env.pages.saveScrapedPage(
            id,
            pageId,
            listOf(ScrapedNode(NodeType.H1, "Judul"), ScrapedNode(NodeType.P, "isi")),
            listOf(image),
        )
        val sql = env.db.openHelper.writableDatabase
        sql.execSQL(
            "INSERT INTO checkpoints (article_id, anchor_node_id, fallback_order, updated_at) " +
                "SELECT $id, MIN(id), 1, 0 FROM content_nodes WHERE article_id = $id",
        )
        assertEquals(2, env.count("SELECT COUNT(*) FROM content_nodes WHERE article_id = $id"))
        assertTrue(env.imageStore.file(image.filename).exists())

        env.articles.deleteArticle(id)

        assertEquals(0, env.count("SELECT COUNT(*) FROM articles WHERE id = $id"))
        assertEquals(0, env.count("SELECT COUNT(*) FROM source_pages WHERE article_id = $id"))
        assertEquals(0, env.count("SELECT COUNT(*) FROM content_nodes WHERE article_id = $id"))
        assertEquals(0, env.count("SELECT COUNT(*) FROM checkpoints WHERE article_id = $id"))
        assertEquals(0, env.count("SELECT COUNT(*) FROM images WHERE article_id = $id"))
        assertFalse(env.imageStore.file(image.filename).exists())
    }

    @Test
    fun fileGambarBersamaBaruDihapusSetelahArtikelTerakhirDihapus() = runBlocking {
        val shared = fakeImage("hapus-uji-bersama.webp", "https://img/shared")
        val first = env.articles.insertArticle(dragonQuest, "Pertama", listOf("https://e/1"))
        val second = env.articles.insertArticle(dragonQuest, "Kedua", listOf("https://e/2"))
        listOf(first to "Pertama", second to "Kedua").forEach { (articleId, title) ->
            val pageId = env.articles.observeArticles(title, null).first().single().pages.single().id
            env.pages.saveScrapedPage(articleId, pageId, listOf(ScrapedNode(NodeType.IMG, null, "{}")), listOf(shared))
        }

        env.articles.deleteArticle(first)
        assertTrue("file masih dipakai artikel kedua", env.imageStore.file(shared.filename).exists())

        env.articles.deleteArticle(second)
        assertFalse(env.imageStore.file(shared.filename).exists())
    }

    private fun fakeImage(filename: String, url: String): ScrapedImage {
        val file = env.imageStore.fileForWriting(filename)
        file.writeText("gambar")
        return ScrapedImage(url, filename, file.absolutePath, file.length())
    }
}
