package com.rds.questlog.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rds.questlog.domain.error.QuestLogError.ImageError
import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.ScrapedNode
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Logo kustom game (QL-38): pengolahan gambar, penyimpanan berkas, Urungkan, dan pembersihan saat game hilang. */
@RunWith(AndroidJUnit4::class)
class GameLogoTest {

    private lateinit var env: TestEnvironment
    private var game = 0L
    private var article = 0L
    private val temp = mutableListOf<File>()

    @Before
    fun setUp() = runBlocking {
        env = TestEnvironment()
        game = env.games.findOrCreate("Dragon Quest VII")
        article = env.articles.insertArticle(game, "Walkthrough", listOf("https://x/1"))
        val page = env.db.pageLayoutDao().getByArticle(article).first().id
        env.pages.saveScrapedPage(article, page, listOf(ScrapedNode(NodeType.P, "isi")), emptyList())
    }

    @After
    fun tearDown() {
        env.imageStore.deleteLogos(game)
        temp.forEach { it.delete() }
        env.close()
    }

    private fun source(width: Int, height: Int, color: Int = Color.RED): String {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        val out = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return fileSource(out.toByteArray())
    }

    private fun fileSource(bytes: ByteArray): String {
        val file = File(env.context.cacheDir, "logo-src-${System.nanoTime()}").also { temp += it }
        file.writeBytes(bytes)
        return Uri.fromFile(file).toString()
    }

    private suspend fun logoPath() = env.games.observeGames().first().single { it.id == game }.logoPath

    @Test
    fun logoDipotongPersegiDiubahKe256DanTerdaftarDiGame() = runBlocking {
        env.logos.set(game, source(400, 200))

        val path = logoPath()!!
        assertEquals("game_logos/$game.webp", path)
        val decoded = BitmapFactory.decodeFile(File(env.context.filesDir, path).path)
        assertEquals(256, decoded.width)
        assertEquals(256, decoded.height)
    }

    @Test
    fun berkasBukanGambarDitolakDanLogoLamaTetapUtuh() = runBlocking {
        env.logos.set(game, source(300, 300))
        val before = env.imageStore.logoFile(game).readBytes()

        val error = runCatching { env.logos.set(game, fileSource("bukan gambar".toByteArray())) }.exceptionOrNull()

        assertEquals(ImageError.InvalidImage, error)
        assertTrue(before.contentEquals(env.imageStore.logoFile(game).readBytes()))
    }

    @Test
    fun berkasLebihDari10MbDitolakSebelumDecode() = runBlocking {
        val error = runCatching { env.logos.set(game, fileSource(ByteArray(10 * 1024 * 1024 + 1))) }
            .exceptionOrNull()

        assertEquals(ImageError.TooLarge, error)
        assertNull(logoPath())
        assertFalse(env.imageStore.logoFile(game).exists())
    }

    @Test
    fun hapusLogoMencabutPathDanBerkasLaluUrungkanMemulihkan() = runBlocking {
        env.logos.set(game, source(300, 300))

        env.logos.remove(game)
        assertNull(logoPath())
        assertFalse(env.imageStore.logoFile(game).exists())

        env.logos.restore(game)
        assertEquals("game_logos/$game.webp", logoPath())
        assertTrue(env.imageStore.logoFile(game).exists())
    }

    @Test
    fun menggantiLogoMenghapusSalinanUrungkanLama() = runBlocking {
        env.logos.set(game, source(300, 300))
        env.logos.remove(game)
        assertTrue(env.imageStore.removedLogoFile(game).exists())

        env.logos.set(game, source(300, 300, Color.BLUE))

        assertFalse(env.imageStore.removedLogoFile(game).exists())
        assertTrue(env.imageStore.logoFile(game).exists())
    }

    @Test
    fun hapusGameMenghapusBerkasLogo() = runBlocking {
        env.logos.set(game, source(300, 300))

        env.games.deleteGame(game)

        assertFalse(env.imageStore.logoFile(game).exists())
    }

    @Test
    fun hapusArtikelTerakhirMenghapusGameKosongDanBerkasLogonya() = runBlocking {
        env.logos.set(game, source(300, 300))

        env.articles.deleteArticle(article)

        assertEquals(0, env.count("SELECT COUNT(*) FROM games WHERE id = $game"))
        assertFalse(env.imageStore.logoFile(game).exists())
    }
}
