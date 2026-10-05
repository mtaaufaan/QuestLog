package com.rds.questlog.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rds.questlog.data.local.entity.ContentNodeEntity
import com.rds.questlog.data.prefs.DataStoreUserPreferencesRepository
import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.ReadMode
import com.rds.questlog.domain.model.TableCell
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** QL-4/6/7/8/16: konten dari Room, checkpoint manual vs posisi otomatis, mode per artikel, preferensi DataStore. */
@RunWith(AndroidJUnit4::class)
class ReaderDataTest {

    private lateinit var env: TestEnvironment
    private var articleId = 0L
    private var pageId = 0L

    @Before
    fun setUp() = runBlocking {
        env = TestEnvironment()
        val gameId = env.games.findOrCreate("Game")
        articleId = env.articles.insertArticle(gameId, "A", listOf("https://x.com/1", "https://x.com/2"))
        pageId = env.db.sourcePageDao().getPending(articleId).first().id
    }

    @After
    fun tearDown() = env.close()

    private fun insertNodes(vararg rows: ContentNodeEntity) = runBlocking {
        env.db.contentNodeDao().insertAll(rows.toList())
    }

    private fun node(order: Int, type: String = "p", text: String? = "t$order", meta: String? = null) =
        ContentNodeEntity(
            articleId = articleId,
            sourcePageId = pageId,
            nodeType = type,
            displayOrder = order,
            textContent = text,
            metadataJson = meta,
        )

    private fun checkpoint() = runBlocking { env.db.checkpointDao().get(articleId) }

    private fun ids() = env.strings("SELECT id FROM content_nodes ORDER BY display_order").map { it.toLong() }

    private companion object {
        const val TABLE_META = "{\"html\":\"<table><tr><th>Item</th><th>Lokasi</th></tr>" +
            "<tr><td>Herb</td><td>Desa</td></tr></table>\"}"
    }

    @Test(timeout = 30_000)
    fun kontenDibacaBerurutanDenganGambarDanTabelTerurai() = runBlocking {
        insertNodes(
            node(3, "p"),
            node(1, "h1", "Judul"),
            node(2, "img", "Peta", """{"path":"/data/peta.webp","alt":"Peta"}"""),
            node(
                4,
                "table",
                null,
                TABLE_META,
            ),
        )

        val nodes = env.articles.observeContent(articleId).first()

        assertEquals(listOf(NodeType.H1, NodeType.IMG, NodeType.P, NodeType.TABLE), nodes.map { it.type })
        assertEquals("/data/peta.webp", nodes[1].imagePath)
        assertEquals(
            listOf(
                listOf(TableCell("Item", isHeader = true), TableCell("Lokasi", isHeader = true)),
                listOf(TableCell("Herb"), TableCell("Desa")),
            ),
            nodes[3].tableRows,
        )
    }

    @Test(timeout = 30_000)
    fun posisiOtomatisTidakMembuatCheckpointManualDanSebaliknya() = runBlocking {
        insertNodes(node(1), node(2), node(3))
        val (a, b, c) = ids()

        env.checkpoints.saveLastPosition(articleId, b)
        assertNull(checkpoint()?.anchorNodeId)
        assertEquals(b, checkpoint()?.lastVisitedNodeId)
        assertNull(env.articles.observeArticle(articleId).first()?.checkpointNodeId)

        env.checkpoints.setCheckpoint(articleId, c)
        assertEquals(c, checkpoint()?.anchorNodeId)
        assertEquals(3, checkpoint()?.fallbackOrder)
        assertEquals(b, checkpoint()?.lastVisitedNodeId)

        // Checkpoint baru menimpa yang lama; hanya satu baris per artikel.
        env.checkpoints.setCheckpoint(articleId, a)
        assertEquals(a, env.articles.observeArticle(articleId).first()?.checkpointNodeId)
        assertEquals(1, env.count("SELECT COUNT(*) FROM checkpoints WHERE article_id = $articleId"))
    }

    @Test(timeout = 30_000)
    fun nodeBukanMilikArtikelDitolak() = runBlocking {
        runCatching { env.checkpoints.setCheckpoint(articleId, 9_999) }.also { assertTrue(it.isFailure) }
        assertNull(checkpoint())
    }

    @Test(timeout = 30_000)
    fun modeBacaTersimpanPerArtikel() = runBlocking {
        assertNull(env.articles.observeArticle(articleId).first()?.readMode)
        env.articles.setReadMode(articleId, ReadMode.PAGED)
        assertEquals(ReadMode.PAGED, env.articles.observeArticle(articleId).first()?.readMode)
    }

    @Test(timeout = 30_000)
    fun preferensiTampilanBertahanAntarPembacaan() = runBlocking<Unit> {
        val file = File(env.context.cacheDir, "prefs-test-${System.nanoTime()}.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repo = DataStoreUserPreferencesRepository(PreferenceDataStoreFactory.create(scope = firstScope) { file })
        assertEquals(16, repo.observeDisplayPreferences().first().fontSizeSp)

        repo.setFontSize(20)
        repo.setDarkMode(true)
        // Satu file hanya boleh punya satu DataStore aktif: tutup yang pertama sebelum "membuka ulang aplikasi".
        firstScope.coroutineContext[Job]!!.cancelAndJoin()

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val prefs = DataStoreUserPreferencesRepository(PreferenceDataStoreFactory.create(scope = secondScope) { file })
            .observeDisplayPreferences().first()
        assertEquals(20, prefs.fontSizeSp)
        assertTrue(prefs.darkMode)
        secondScope.cancel()
        file.delete()
    }
}
