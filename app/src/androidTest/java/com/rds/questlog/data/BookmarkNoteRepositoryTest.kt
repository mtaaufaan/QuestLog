package com.rds.questlog.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rds.questlog.data.repository.BookmarkRepositoryImpl
import com.rds.questlog.data.repository.NoteRepositoryImpl
import com.rds.questlog.domain.model.GameTarget
import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.ScrapedNode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** QL-23/QL-24: bookmark dan catatan nyata di Room — daftar per game, banyak per artikel, catatan tidak ikut pindah game. */
@RunWith(AndroidJUnit4::class)
class BookmarkNoteRepositoryTest {

    private lateinit var env: TestEnvironment
    private lateinit var bookmarks: BookmarkRepositoryImpl
    private lateinit var notes: NoteRepositoryImpl
    private var gameA = 0L
    private var gameB = 0L
    private var first = 0L
    private var second = 0L

    @Before
    fun setUp() = runBlocking {
        env = TestEnvironment()
        bookmarks = BookmarkRepositoryImpl(env.db, env.db.bookmarkDao(), env.db.contentNodeDao())
        notes = NoteRepositoryImpl(env.db.articleNoteDao())
        gameA = env.games.findOrCreate("Game A")
        gameB = env.games.findOrCreate("Game B")
        first = article(gameA, "Artikel 1")
        second = article(gameA, "Artikel 2")
    }

    @After
    fun tearDown() = env.close()

    private suspend fun article(game: Long, title: String): Long {
        val id = env.articles.insertArticle(game, title, listOf("https://x/$title"))
        val page = env.db.pageLayoutDao().getByArticle(id).first().id
        env.pages.saveScrapedPage(id, page, List(3) { ScrapedNode(NodeType.P, "$title-${it + 1}") }, emptyList())
        return id
    }

    private fun nodeId(articleId: Long, index: Int) =
        runBlocking { env.articles.observeContent(articleId).first() }[index].id

    @Test
    fun daftarPerGameDikelompokkanPerArtikelDanBanyakBookmarkTidakSalingMenimpa() = runBlocking {
        bookmarks.add(first, nodeId(first, 0), "Boss", "s1")
        bookmarks.add(first, nodeId(first, 0), "Boss kedua di elemen sama", "s1")
        bookmarks.add(second, nodeId(second, 2), "Peti", "s2")
        val other = article(gameB, "Artikel B")
        bookmarks.add(other, nodeId(other, 1), "Game lain", "s3")

        val listA = bookmarks.observeByGame(gameA).first()

        // Artikel terbaru dulu (Artikel 2), lalu urutan dibuat di dalam artikel.
        assertEquals(listOf("Peti", "Boss", "Boss kedua di elemen sama"), listA.map { it.label })
        assertEquals(listOf("Artikel 2", "Artikel 1", "Artikel 1"), listA.map { it.articleTitle })
        assertEquals(listOf("Game lain"), bookmarks.observeByGame(gameB).first().map { it.label })
    }

    @Test
    fun ubahLabelHapusDanNodeBukanMilikArtikelDitolak() = runBlocking {
        val id = bookmarks.add(first, nodeId(first, 1), "Lama", "s")

        bookmarks.updateLabel(id, "Baru")
        assertEquals("Baru", bookmarks.observeByGame(gameA).first().single().label)

        val rejected = runCatching { bookmarks.add(first, nodeId(second, 0), "Salah", "s") }
        assertTrue(rejected.isFailure)

        bookmarks.delete(id)
        assertTrue(bookmarks.observeByGame(gameA).first().isEmpty())
    }

    @Test
    fun catatanSatuPerArtikelDanTidakBerubahSaatArtikelPindahGame() = runBlocking {
        assertEquals("", notes.observe(first).first())

        notes.save(first, "versi 1")
        notes.save(first, "versi 2")
        assertEquals("versi 2", notes.observe(first).first())
        assertEquals(1, env.count("SELECT COUNT(*) FROM article_notes"))

        env.management.updateDetails(first, "Artikel 1", GameTarget.Existing(gameB))

        assertEquals("versi 2", notes.observe(first).first())
        notes.delete(first)
        assertEquals("", notes.observe(first).first())
        assertEquals(0, env.count("SELECT COUNT(*) FROM article_notes"))
    }

    @Test
    fun indikatorDiDaftarArtikelMengikutiBookmarkDanCatatan() = runBlocking {
        bookmarks.add(first, nodeId(first, 0), "A", "")
        bookmarks.add(first, nodeId(first, 1), "B", "")
        notes.save(second, "ada isi")

        val rows = env.articles.observeArticles("", null).first().associateBy { it.id }

        assertEquals(2, rows.getValue(first).bookmarkCount)
        assertFalse(rows.getValue(first).hasNote)
        assertEquals(0, rows.getValue(second).bookmarkCount)
        assertTrue(rows.getValue(second).hasNote)
    }
}
