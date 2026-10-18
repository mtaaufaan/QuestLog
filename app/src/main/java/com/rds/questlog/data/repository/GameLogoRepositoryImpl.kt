package com.rds.questlog.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.rds.questlog.data.local.dao.GameDao
import com.rds.questlog.data.scraper.GameLogoCodec
import com.rds.questlog.data.scraper.ImageStore
import com.rds.questlog.domain.error.QuestLogError.ImageError
import com.rds.questlog.domain.model.LogoCrop
import com.rds.questlog.domain.model.clamped
import com.rds.questlog.domain.repository.GameLogoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameLogoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameDao: GameDao,
    private val imageStore: ImageStore,
) : GameLogoRepository {

    override suspend fun save(gameId: Long, source: String?, crop: LogoCrop) = withContext(Dispatchers.IO) {
        val bitmap = if (source != null) decodeNew(source) else loadStored(gameId)
        val applied = crop.clamped(bitmap.width.toFloat() / bitmap.height)
        try {
            if (source != null) {
                writeAtomically(imageStore.logoSourceFile(gameId), GameLogoCodec.toWebp(bitmap, SOURCE_QUALITY))
            }
            val logo = GameLogoCodec.toWebp(GameLogoCodec.render(bitmap, applied), LOGO_QUALITY)
            writeAtomically(imageStore.logoFile(gameId), logo)
            imageStore.deleteRemovedLogos(gameId)
            val hasSource = imageStore.logoSourceFile(gameId).exists()
            gameDao.setLogoFiles(
                gameId,
                imageStore.logoPath(gameId),
                imageStore.logoSourcePath(gameId).takeIf { hasSource },
            )
            gameDao.setLogoCrop(gameId, applied.zoom, applied.x, applied.y)
        } catch (e: IOException) {
            throw ImageError.IoError.also { it.initCause(e) }
        }
    }

    override suspend fun remove(gameId: Long) = withContext(Dispatchers.IO) {
        imageStore.deleteRemovedLogos(gameId)
        // ponytail: salinan Urungkan bertahan sampai logo diubah lagi atau game dihapus; maks. dua berkas per game.
        val logo = imageStore.logoFile(gameId)
        if (logo.exists() && !logo.renameTo(imageStore.removedLogoFile(gameId))) throw ImageError.IoError
        val source = imageStore.logoSourceFile(gameId)
        if (source.exists() && !source.renameTo(imageStore.removedLogoSourceFile(gameId))) throw ImageError.IoError
        gameDao.setLogoFiles(gameId, null, null)
    }

    override suspend fun restore(gameId: Long) = withContext(Dispatchers.IO) {
        val removedLogo = imageStore.removedLogoFile(gameId)
        if (!removedLogo.exists() || !removedLogo.renameTo(imageStore.logoFile(gameId))) throw ImageError.IoError
        imageStore.removedLogoSourceFile(gameId).takeIf { it.exists() }?.renameTo(imageStore.logoSourceFile(gameId))
        val hasSource = imageStore.logoSourceFile(gameId).exists()
        gameDao.setLogoFiles(
            gameId,
            imageStore.logoPath(gameId),
            imageStore.logoSourcePath(gameId).takeIf { hasSource },
        )
    }

    /** Gambar baru dari Photo Picker: dibaca maks. 10 MB, didecode tegak dan dipaskan ke 2048 px. */
    private fun decodeNew(source: String): Bitmap =
        GameLogoCodec.decodeSource(readCapped(Uri.parse(source))) ?: throw ImageError.InvalidImage

    /** Gambar asli tersimpan; logo hasil v6 (tanpa gambar asli) dipakai sebagai gantinya. */
    private fun loadStored(gameId: Long): Bitmap {
        val file = imageStore.logoSourceFile(gameId).takeIf { it.exists() } ?: imageStore.logoFile(gameId)
        return BitmapFactory.decodeFile(file.path) ?: throw ImageError.IoError
    }

    /** Menulis ke berkas sementara lalu menggantikan [target]: berkas lama tetap utuh bila penulisan gagal. */
    private fun writeAtomically(target: File, bytes: ByteArray) {
        val temp = File(target.path + ".tmp")
        temp.writeBytes(bytes)
        if (!temp.renameTo(target)) {
            target.delete()
            if (!temp.renameTo(target)) throw IOException("Gagal menulis ${target.name}")
        }
    }

    /** Membaca sumber maks. [MAX_BYTES]; lebih besar -> `TooLarge` sebelum decode. */
    private fun readCapped(uri: Uri): ByteArray {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readUpTo(MAX_BYTES + 1) }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        } ?: throw ImageError.IoError
        if (bytes.size > MAX_BYTES) throw ImageError.TooLarge
        return bytes
    }

    /** Membaca paling banyak [limit] byte (satu lebih dari batas cukup untuk mendeteksi kelebihan). */
    private fun InputStream.readUpTo(limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        var total = 0
        while (total < limit) {
            val read = read(buffer, 0, minOf(buffer.size, limit - total))
            if (read < 0) break
            out.write(buffer, 0, read)
            total += read
        }
        return out.toByteArray()
    }

    private companion object {
        const val MAX_BYTES = 10 * 1024 * 1024
        const val LOGO_QUALITY = GameLogoCodec.LOGO_QUALITY
        const val SOURCE_QUALITY = GameLogoCodec.SOURCE_QUALITY
        const val BUFFER_BYTES = 16 * 1024
    }
}
