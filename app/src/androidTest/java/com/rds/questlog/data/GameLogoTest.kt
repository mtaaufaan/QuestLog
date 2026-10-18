package com.rds.questlog.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rds.questlog.domain.error.QuestLogError.ImageError
import com.rds.questlog.domain.model.LogoCrop
import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.ScrapedNode
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Logo kustom game (QL-38): crop persis, gambar asli, Urungkan, dan pembersihan berkas saat game hilang. */
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

    /** Gambar [width] x [height]: separuh kiri merah, separuh kanan biru. */
    private fun twoToneSource(width: Int, height: Int): String {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawRect(0f, 0f, width / 2f, height.toFloat(), Paint().apply { color = Color.RED })
        canvas.drawRect(width / 2f, 0f, width.toFloat(), height.toFloat(), Paint().apply { color = Color.BLUE })
        val out = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return fileSource(out.toByteArray())
    }

    private fun fileSource(bytes: ByteArray): String {
        val file = File(env.context.cacheDir, "logo-src-${System.nanoTime()}").also { temp += it }
        file.writeBytes(bytes)
        return Uri.fromFile(file).toString()
    }

    private suspend fun stored() = env.games.observeGames().first().single { it.id == game }

    private fun logoBitmap(): Bitmap = BitmapFactory.decodeFile(env.imageStore.logoFile(game).path)

    private fun isReddish(pixel: Int) = Color.red(pixel) > 180 && Color.blue(pixel) < 90

    private fun isBluish(pixel: Int) = Color.blue(pixel) > 180 && Color.red(pixel) < 90

    @Test
    fun logoPersegi256DariTengahGambarDanMetadataTersimpan() = runBlocking {
        env.logos.save(game, twoToneSource(400, 200), LogoCrop())

        val logo = logoBitmap()
        assertEquals(256, logo.width)
        assertEquals(256, logo.height)
        // Persegi tengah 200x200 dari 400x200: separuh kiri merah, separuh kanan biru.
        assertTrue(isReddish(logo.getPixel(40, 128)))
        assertTrue(isBluish(logo.getPixel(216, 128)))
        val row = stored()
        assertEquals("game_logos/$game.webp", row.logoPath)
        assertEquals("game_logos/${game}_src.webp", row.logoSourcePath)
        assertEquals(LogoCrop(), row.logoCrop)
    }

    @Test
    fun gesekDanZoomMenentukanAreaYangDisimpanPersisDanDibatasiDiTepi() = runBlocking {
        // Geser 66 satuan = 50 px: persegi 50..250 -> 75% kiri merah, 25% kanan biru (batas di x = 192).
        env.logos.save(game, twoToneSource(400, 200), LogoCrop(1f, 66f, 0f))
        val shifted = logoBitmap()
        assertTrue(isReddish(shifted.getPixel(100, 128)))
        assertTrue(isReddish(shifted.getPixel(180, 128)))
        assertTrue(isBluish(shifted.getPixel(206, 128)))

        // Geser jauh melewati tepi: dibatasi, hanya area merah (0..200 px) yang masuk bingkai.
        env.logos.save(game, null, LogoCrop(1f, 5_000f, 0f))
        val clamped = logoBitmap()
        assertTrue(isReddish(clamped.getPixel(10, 128)))
        assertTrue(isReddish(clamped.getPixel(245, 128)))
        assertEquals(132f, stored().logoCrop!!.x, 0.01f)
    }

    @Test
    fun aturUlangPosisiTanpaUnggahUlangMemakaiGambarAsliTersimpan() = runBlocking {
        env.logos.save(game, twoToneSource(400, 200), LogoCrop())
        val sourceBefore = env.imageStore.logoSourceFile(game).readBytes()

        env.logos.save(game, null, LogoCrop(2f, 0f, 0f))

        assertTrue(sourceBefore.contentEquals(env.imageStore.logoSourceFile(game).readBytes()))
        assertEquals(2f, stored().logoCrop!!.zoom, 0.001f)
        // Zoom 2x di tengah 400x200: area 100x100 di tengah, setengahnya merah dan setengahnya biru.
        assertTrue(isReddish(logoBitmap().getPixel(60, 128)))
        assertTrue(isBluish(logoBitmap().getPixel(200, 128)))
    }

    @Test
    fun gambarAsliDikecilkanKe2048TanpaMengubahPerbandingan() = runBlocking {
        env.logos.save(game, twoToneSource(3000, 1500), LogoCrop())

        val source = BitmapFactory.decodeFile(env.imageStore.logoSourceFile(game).path)
        assertEquals(2048, maxOf(source.width, source.height))
        assertEquals(2, source.width / source.height)
    }

    @Test
    fun berkasBukanGambarDitolakDanLogoLamaTetapUtuh() = runBlocking {
        env.logos.save(game, twoToneSource(300, 300), LogoCrop())
        val before = env.imageStore.logoFile(game).readBytes()

        val error = runCatching { env.logos.save(game, fileSource("bukan gambar".toByteArray()), LogoCrop()) }
            .exceptionOrNull()

        assertEquals(ImageError.InvalidImage, error)
        assertTrue(before.contentEquals(env.imageStore.logoFile(game).readBytes()))
    }

    @Test
    fun berkasLebihDari10MbDitolakSebelumDecode() = runBlocking {
        val error = runCatching { env.logos.save(game, fileSource(ByteArray(10 * 1024 * 1024 + 1)), LogoCrop()) }
            .exceptionOrNull()

        assertEquals(ImageError.TooLarge, error)
        assertNull(stored().logoPath)
        assertFalse(env.imageStore.logoFile(game).exists())
    }

    @Test
    fun hapusLogoMencabutPathDanKeduaBerkasLaluUrungkanMemulihkanDenganPosisi() = runBlocking {
        env.logos.save(game, twoToneSource(400, 200), LogoCrop(1.5f, 20f, 0f))

        env.logos.remove(game)
        assertNull(stored().logoPath)
        assertNull(stored().logoSourcePath)
        assertFalse(env.imageStore.logoFile(game).exists())
        assertFalse(env.imageStore.logoSourceFile(game).exists())

        env.logos.restore(game)
        val restored = stored()
        assertEquals("game_logos/$game.webp", restored.logoPath)
        assertEquals("game_logos/${game}_src.webp", restored.logoSourcePath)
        assertEquals(1.5f, restored.logoCrop!!.zoom, 0.001f)
        assertTrue(env.imageStore.logoFile(game).exists() && env.imageStore.logoSourceFile(game).exists())
    }

    @Test
    fun menggantiLogoMenghapusSalinanUrungkanLama() = runBlocking {
        env.logos.save(game, twoToneSource(300, 300), LogoCrop())
        env.logos.remove(game)
        assertTrue(env.imageStore.removedLogoFile(game).exists())

        env.logos.save(game, twoToneSource(300, 300), LogoCrop())

        assertFalse(env.imageStore.removedLogoFile(game).exists())
        assertFalse(env.imageStore.removedLogoSourceFile(game).exists())
        assertNotNull(stored().logoPath)
    }

    @Test
    fun hapusGameMenghapusSemuaBerkasLogo() = runBlocking {
        env.logos.save(game, twoToneSource(300, 300), LogoCrop())
        env.logos.remove(game)
        env.logos.restore(game)

        env.games.deleteGame(game)

        assertFalse(env.imageStore.logoFile(game).exists())
        assertFalse(env.imageStore.logoSourceFile(game).exists())
    }

    @Test
    fun hapusArtikelTerakhirMenghapusGameKosongDanBerkasLogonya() = runBlocking {
        env.logos.save(game, twoToneSource(300, 300), LogoCrop())

        env.articles.deleteArticle(article)

        assertEquals(0, env.count("SELECT COUNT(*) FROM games WHERE id = $game"))
        assertFalse(env.imageStore.logoFile(game).exists())
        assertFalse(env.imageStore.logoSourceFile(game).exists())
    }
}
