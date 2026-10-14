package com.rds.questlog.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rds.questlog.data.local.entity.ArticleNoteEntity
import com.rds.questlog.data.local.entity.BookmarkEntity
import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.model.ScrapedNode
import com.rds.questlog.domain.scraper.ScraperEngine
import com.rds.questlog.domain.scraper.ScrapingResult
import com.rds.questlog.domain.usecase.article.ScrapeArticleUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** QL-35: bookmark ikut cascade hapus artikel dan dipulihkan seperti checkpoint saat halaman dihapus/diunduh ulang. */
@RunWith(AndroidJUnit4::class)
class BookmarkAnchorTest {

    private lateinit var env: TestEnvironment
    private var articleId = 0L
    private lateinit var pageIds: List<Long>

    @Before
    fun setUp() = runBlocking {
        env = TestEnvironment()
        val game = env.games.findOrCreate("Game")
        articleId = env.articles.insertArticle(game, "A", listOf("https://a/1", "https://a/2", "https://a/3"))
        pageIds = env.db.pageLayoutDao().getByArticle(articleId).map { it.id }
        pageIds.forEachIndexed { i, id ->
            val nodes = List(3) { n -> ScrapedNode(NodeType.P, "h${i + 1}-${n + 1}") }
            env.pages.saveScrapedPage(articleId, id, nodes, emptyList())
        }
    }

    @After
    fun tearDown() = env.close()

    private fun nodeId(text: String) =
        runBlocking { env.articles.observeContent(articleId).first() }.first { it.text == text }.id

    private fun mark(text: String, label: String = text): Long = runBlocking {
        val node = env.articles.observeContent(articleId).first().first { it.text == text }
        env.db.bookmarkDao().insert(
            BookmarkEntity(
                articleId = articleId,
                nodeId = node.id,
                fallbackOrder = node.displayOrder,
                label = label,
                snippet = text,
                createdAt = 1,
            ),
        )
    }

    private fun bookmarks() = runBlocking { env.db.bookmarkDao().getByArticle(articleId) }

    private fun failureOf(block: suspend () -> Unit): Throwable? =
        runBlocking { runCatching { block() }.exceptionOrNull() }

    private class FakeScraper(private val results: Map<String, ScrapingResult>) : ScraperEngine {
        override suspend fun scrape(url: String): ScrapingResult = results.getValue(url)
    }

    private fun scrape(vararg results: Pair<String, ScrapingResult>) =
        runBlocking { ScrapeArticleUseCase(env.pages, FakeScraper(results.toMap()))(articleId) }

    @Test
    fun banyakBookmarkPerArtikelDanHapusArtikelMenghapusBookmarkDanCatatan() = runBlocking {
        mark("h1-1")
        mark("h2-2")
        mark("h2-2", label = "Bookmark kedua di elemen sama")
        env.db.articleNoteDao().upsert(ArticleNoteEntity(articleId = articleId, content = "catatan", updatedAt = 1))
        assertEquals(3, bookmarks().size)

        env.articles.deleteArticle(articleId)

        assertEquals(0, env.count("SELECT COUNT(*) FROM bookmarks"))
        assertEquals(0, env.count("SELECT COUNT(*) FROM article_notes"))
        assertEquals(0, env.count("SELECT COUNT(*) FROM content_nodes"))
    }

    @Test
    fun hapusHalamanMemindahkanBookmarkKeNodeTerdekatSepertiCheckpoint() = runBlocking {
        mark("h2-1")
        mark("h2-3")
        mark("h3-1")

        env.management.deletePage(articleId, pageIds[1])

        val moved = bookmarks()
        assertEquals(3, moved.size)
        assertEquals(
            listOf("h1-3", "h1-3", "h3-1"),
            moved.map { b ->
                runBlocking { env.articles.observeContent(articleId).first() }.first { it.id == b.nodeId }.text
            },
        )
        // Halaman 3 naik ke slot 2: fallback ikut dihitung ulang dari posisi node sekarang.
        assertEquals(listOf(3, 3, 1001), moved.map { it.fallbackOrder })
    }

    @Test
    fun hapusHalamanPertamaMemindahkanBookmarkKeHalamanBerikutnya() = runBlocking {
        mark("h1-2")

        env.management.deletePage(articleId, pageIds[0])

        assertEquals(nodeId("h2-1"), bookmarks().single().nodeId)
    }

    @Test
    fun hapusHalamanSatuSatunyaYangPunyaKontenMenghapusBookmarkTanpaError() = runBlocking {
        env.db.contentNodeDao().deleteByPage(pageIds[0])
        env.db.contentNodeDao().deleteByPage(pageIds[2])
        mark("h2-1")

        env.management.deletePage(articleId, pageIds[1])

        assertEquals(0, bookmarks().size)
    }

    @Test
    fun urutUlangMenjagaElemenBookmarkDanMenghitungUlangFallback() = runBlocking {
        mark("h3-2")
        val anchor = nodeId("h3-2")

        env.management.reorderPages(articleId, listOf(pageIds[2], pageIds[0], pageIds[1]))

        val b = bookmarks().single()
        assertEquals(anchor, b.nodeId)
        assertEquals(2, b.fallbackOrder)
    }

    @Test
    fun unduhUlangMemulihkanBookmarkByTeksSamaLaluPosisiTerdekat() = runBlocking {
        mark("h1-3", label = "teks sama")
        mark("h1-2", label = "teks berubah")
        env.management.markPagesForRefresh(articleId, pageIds[0])
        val fresh = listOf(
            ScrapedNode(NodeType.P, "sisipan"),
            ScrapedNode(NodeType.P, "h1-1"),
            ScrapedNode(NodeType.P, "h1-2-DIUBAH"),
            ScrapedNode(NodeType.P, "h1-3"),
        )

        scrape("https://a/1" to ScrapingResult.Success(fresh, emptyList()))

        val byLabel = bookmarks().associate { it.label to it }
        assertEquals(nodeId("h1-3"), byLabel.getValue("teks sama").nodeId)
        assertEquals(4, byLabel.getValue("teks sama").fallbackOrder)
        // Teks "h1-2" sudah tidak ada: jatuh ke node dengan display_order terdekat di halaman yang sama (posisi 2).
        assertEquals(nodeId("h1-1"), byLabel.getValue("teks berubah").nodeId)
        assertEquals(2, env.count("SELECT COUNT(*) FROM bookmarks"))
    }

    @Test
    fun unduhUlangGagalMempertahankanBookmarkDanNodeLama() = runBlocking {
        mark("h2-2")
        val before = bookmarks().single()
        env.management.markPagesForRefresh(articleId, pageIds[1])

        scrape("https://a/2" to ScrapingResult.Failure(PageFailure.Timeout))

        assertEquals(before.nodeId, bookmarks().single().nodeId)
    }

    @Test
    fun unduhUlangBookmarkDiHalamanLainTidakTersentuh() = runBlocking {
        mark("h3-1")
        val before = bookmarks().single()
        env.management.markPagesForRefresh(articleId, pageIds[0])

        scrape("https://a/1" to ScrapingResult.Success(listOf(ScrapedNode(NodeType.P, "baru")), emptyList()))

        assertEquals(before.nodeId, bookmarks().single().nodeId)
        assertNull(failureOf { env.db.bookmarkDao().getByArticle(articleId) })
    }
}
