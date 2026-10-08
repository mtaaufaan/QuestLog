package com.rds.questlog.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rds.questlog.domain.model.GameTarget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** QL-18 di Room nyata: ubah judul, pindah game, dan game kosong dihapus otomatis dalam satu transaksi. */
@RunWith(AndroidJUnit4::class)
class UpdateArticleDetailsTest {

    private lateinit var env: TestEnvironment
    private var dq = 0L
    private var bof = 0L
    private var solo = 0L
    private var pair = 0L

    @Before
    fun setUp() = runBlocking {
        env = TestEnvironment()
        dq = env.games.findOrCreate("Dragon Quest VII")
        bof = env.games.findOrCreate("Breath Of Fire III")
        solo = env.articles.insertArticle(dq, "Sendirian", listOf("https://a/1", "https://a/2"))
        pair = env.articles.insertArticle(bof, "Berdua A", listOf("https://a/3"))
        env.articles.insertArticle(bof, "Berdua B", listOf("https://a/4"))
        Unit
    }

    @After
    fun tearDown() = env.close()

    private fun games() = runBlocking { env.games.observeGames().first() }.associate { it.name to it.articleCount }

    @Test
    fun judulBerubahTanpaMenyentuhHalaman() = runBlocking {
        env.management.updateDetails(solo, "Judul Baru", GameTarget.Unchanged)

        val article = env.articles.observeArticle(solo).first()!!
        assertEquals("Judul Baru", article.title)
        assertEquals(dq, article.gameId)
        assertEquals(2, article.pages.size)
    }

    @Test
    fun pindahKeGameLainMenghapusGameAsalYangKosong() = runBlocking {
        env.management.updateDetails(solo, "Sendirian", GameTarget.Existing(bof))

        assertEquals(bof, env.articles.observeArticle(solo).first()!!.gameId)
        assertEquals(mapOf("Breath Of Fire III" to 3), games())
    }

    @Test
    fun pindahKeGameBaruMembuatGameDanMenghapusAsalYangKosong() = runBlocking {
        env.management.updateDetails(solo, "Sendirian", GameTarget.New("Zelda"))

        assertEquals(mapOf("Breath Of Fire III" to 2, "Zelda" to 1), games())
    }

    @Test
    fun gameAsalYangMasihBerisiTidakDihapus() = runBlocking {
        env.management.updateDetails(pair, "Berdua A", GameTarget.Existing(dq))

        assertEquals(mapOf("Dragon Quest VII" to 2, "Breath Of Fire III" to 1), games())
    }

    @Test
    fun namaGameBaruYangSudahAdaMemakaiGameItu() = runBlocking {
        env.management.updateDetails(solo, "Sendirian", GameTarget.New("breath of fire iii"))

        assertEquals(mapOf("Breath Of Fire III" to 3), games())
    }

    @Test
    fun deleteIfEmptyHanyaMenghapusGameKosong() = runBlocking {
        val kosong = env.games.findOrCreate("Kosong")

        env.games.deleteIfEmpty(kosong)
        env.games.deleteIfEmpty(dq)

        assertNull(env.games.findIdByName("Kosong"))
        assertEquals(dq, env.games.findIdByName("Dragon Quest VII"))
    }
}
