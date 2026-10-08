package com.rds.questlog.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rds.questlog.domain.error.QuestLogError.ArticleError
import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.model.ScrapedNode
import com.rds.questlog.domain.model.SourcePageStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** QL-19 di Room nyata: hapus halaman (urutan FK), urut ulang slot, dan checkpoint yang tetap valid. */
@RunWith(AndroidJUnit4::class)
class ManagePagesDataTest {

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
            val nodes = List(2) { n -> ScrapedNode(NodeType.P, "h${i + 1}-${n + 1}") }
            env.pages.saveScrapedPage(articleId, id, nodes, emptyList())
        }
    }

    @After
    fun tearDown() = env.close()

    private fun texts() = runBlocking { env.articles.observeContent(articleId).first() }.map { it.text }

    private fun orders() = runBlocking { env.articles.observeContent(articleId).first() }.map { it.displayOrder }

    private fun nodeId(text: String) =
        runBlocking { env.articles.observeContent(articleId).first() }.first { it.text == text }.id

    private fun checkpoint() = runBlocking { env.db.checkpointDao().get(articleId) }

    private fun pageOrders() =
        runBlocking { env.db.pageLayoutDao().getByArticle(articleId) }.map { it.pageOrder to it.id }

    private fun failureOf(block: suspend () -> Unit): Throwable? =
        runBlocking { runCatching { block() }.exceptionOrNull() }

    @Test
    fun hapusHalamanTengahMemindahkanCheckpointKeNodeTerakhirHalamanSebelumnya() = runBlocking {
        env.checkpoints.setCheckpoint(articleId, nodeId("h2-1"))
        env.checkpoints.saveLastPosition(articleId, nodeId("h2-2"))

        env.management.deletePage(articleId, pageIds[1])

        assertEquals(listOf("h1-1", "h1-2", "h3-1", "h3-2"), texts())
        // Halaman 3 naik ke slot 2: urutan tetap rapat dan berurutan.
        assertEquals(listOf(1, 2, 1001, 1002), orders())
        assertEquals(listOf(1 to pageIds[0], 2 to pageIds[2]), pageOrders())
        val cp = checkpoint()!!
        assertEquals(nodeId("h1-2"), cp.anchorNodeId)
        assertEquals(2, cp.fallbackOrder)
        assertEquals(nodeId("h1-2"), cp.lastVisitedNodeId)
    }

    @Test
    fun hapusHalamanPertamaMemindahkanCheckpointKeNodePertamaHalamanBerikutnya() = runBlocking {
        env.checkpoints.setCheckpoint(articleId, nodeId("h1-2"))

        env.management.deletePage(articleId, pageIds[0])

        assertEquals(listOf("h2-1", "h2-2", "h3-1", "h3-2"), texts())
        assertEquals(listOf(1, 2, 1001, 1002), orders())
        assertEquals(nodeId("h2-1"), checkpoint()!!.anchorNodeId)
        assertEquals(1, checkpoint()!!.fallbackOrder)
    }

    @Test
    fun hapusHalamanTerakhirMemindahkanCheckpointKeHalamanSebelumnya() = runBlocking {
        env.checkpoints.setCheckpoint(articleId, nodeId("h3-2"))

        env.management.deletePage(articleId, pageIds[2])

        assertEquals(nodeId("h2-2"), checkpoint()!!.anchorNodeId)
        assertEquals(1002, checkpoint()!!.fallbackOrder)
    }

    @Test
    fun checkpointDiLuarHalamanYangDihapusTidakBerubahTetapFallbackMengikutiGeseran() = runBlocking {
        env.checkpoints.setCheckpoint(articleId, nodeId("h3-1"))

        env.management.deletePage(articleId, pageIds[0])

        assertEquals(nodeId("h3-1"), checkpoint()!!.anchorNodeId)
        assertEquals(1001, checkpoint()!!.fallbackOrder)
    }

    @Test
    fun halamanTerakhirTidakBolehDihapusDanDataTidakBerubah() = runBlocking {
        env.management.deletePage(articleId, pageIds[0])
        env.management.deletePage(articleId, pageIds[1])
        val before = texts()

        val error = failureOf { env.management.deletePage(articleId, pageIds[2]) }

        assertEquals(ArticleError.LastPage, error)
        assertEquals(before, texts())
        assertEquals(1, pageOrders().size)
    }

    @Test
    fun halamanYangSedangDiunduhDitolak() = runBlocking {
        env.db.sourcePageDao().setStatus(pageIds[1], SourcePageStatus.IN_PROGRESS.name, null)

        val error = failureOf { env.management.deletePage(articleId, pageIds[1]) }

        assertEquals(ArticleError.Busy, error)
        assertEquals(3, pageOrders().size)
    }

    @Test
    fun halamanLainAtauSudahTerhapusDitolak() = runBlocking {
        val error = failureOf { env.management.deletePage(articleId, 9_999) }

        assertEquals(ArticleError.PageNotFound, error)
    }

    @Test
    fun halamanGagalTanpaKontenBolehDihapusDanStatusArtikelDihitungUlang() = runBlocking {
        env.db.contentNodeDao().deleteByPage(pageIds[1])
        env.pages.markFailed(pageIds[1], PageFailure.Timeout)
        val before = env.articles.observeArticle(articleId).first()!!
        assertEquals(1, before.pages.count { it.status == SourcePageStatus.FAILED })

        env.management.deletePage(articleId, pageIds[1])

        val article = env.articles.observeArticle(articleId).first()!!
        assertEquals(2, article.pages.size)
        assertTrue(article.pages.none { it.status == SourcePageStatus.FAILED })
        assertEquals(listOf("h1-1", "h1-2", "h3-1", "h3-2"), texts())
    }

    @Test
    fun hapusHalamanSatuSatunyaYangPunyaKontenMelepasCheckpoint() = runBlocking {
        // Halaman 1 dan 3 kosong (gagal); hanya halaman 2 yang punya konten.
        env.db.contentNodeDao().deleteByPage(pageIds[0])
        env.db.contentNodeDao().deleteByPage(pageIds[2])
        env.checkpoints.setCheckpoint(articleId, nodeId("h2-1"))
        env.checkpoints.saveLastPosition(articleId, nodeId("h2-2"))

        env.management.deletePage(articleId, pageIds[1])

        assertNull(checkpoint()!!.anchorNodeId)
        assertNull(checkpoint()!!.fallbackOrder)
        assertNull(checkpoint()!!.lastVisitedNodeId)
    }

    @Test
    fun urutUlangMenggeserKontenDanCheckpointTetapMenunjukElemenSama() = runBlocking {
        env.checkpoints.setCheckpoint(articleId, nodeId("h3-1"))
        val anchor = nodeId("h3-1")

        env.management.reorderPages(articleId, listOf(pageIds[2], pageIds[0], pageIds[1]))

        assertEquals(listOf("h3-1", "h3-2", "h1-1", "h1-2", "h2-1", "h2-2"), texts())
        assertEquals(listOf(1, 2, 1001, 1002, 2001, 2002), orders())
        assertEquals(listOf(1 to pageIds[2], 2 to pageIds[0], 3 to pageIds[1]), pageOrders())
        assertEquals(anchor, checkpoint()!!.anchorNodeId)
        assertEquals(1, checkpoint()!!.fallbackOrder)
        val article = env.articles.observeArticle(articleId).first()!!
        assertEquals(listOf(pageIds[2], pageIds[0], pageIds[1]), article.pages.map { it.id })
    }

    @Test
    fun urutUlangDenganDaftarYangTidakCocokDitolakTanpaMengubahApaPun() = runBlocking {
        val before = texts()
        listOf(listOf(pageIds[0]), listOf(pageIds[0], pageIds[0], pageIds[1]), pageIds + 9_999L).forEach { ids ->
            val error = failureOf { env.management.reorderPages(articleId, ids) }

            assertEquals(ArticleError.PageNotFound, error)
        }

        assertEquals(before, texts())
        assertEquals(listOf(1 to pageIds[0], 2 to pageIds[1], 3 to pageIds[2]), pageOrders())
    }

    @Test
    fun halamanBaruDitambahSetelahUrutUlangTetapDiAkhirDenganSlotBerlanjut() = runBlocking {
        env.management.reorderPages(articleId, listOf(pageIds[1], pageIds[0], pageIds[2]))

        env.articles.appendPages(articleId, listOf("https://a/4"))

        val pages = env.db.pageLayoutDao().getByArticle(articleId)
        assertEquals(4, pages.last().pageOrder)
        assertEquals(3001, pages.last().orderStart)
    }
}
