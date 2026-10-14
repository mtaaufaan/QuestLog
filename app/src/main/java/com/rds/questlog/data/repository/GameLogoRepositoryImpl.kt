package com.rds.questlog.data.repository

import android.content.Context
import android.net.Uri
import com.rds.questlog.data.local.dao.GameDao
import com.rds.questlog.data.scraper.GameLogoCodec
import com.rds.questlog.data.scraper.ImageStore
import com.rds.questlog.domain.error.QuestLogError.ImageError
import com.rds.questlog.domain.repository.GameLogoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
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

    override suspend fun set(gameId: Long, source: String) = withContext(Dispatchers.IO) {
        val webp = GameLogoCodec.encode(readCapped(Uri.parse(source))) ?: throw ImageError.InvalidImage
        try {
            // Tulis ke berkas sementara dulu: logo lama tetap utuh bila penulisan gagal.
            val target = imageStore.logoFile(gameId)
            val temp = imageStore.removedLogoFile(gameId).resolveSibling("$gameId.tmp")
            temp.writeBytes(webp)
            if (!temp.renameTo(target)) {
                target.delete()
                if (!temp.renameTo(target)) throw IOException("Gagal mengganti logo")
            }
            imageStore.removedLogoFile(gameId).delete()
            gameDao.setLogo(gameId, imageStore.logoPath(gameId))
        } catch (e: IOException) {
            throw ImageError.IoError.also { it.initCause(e) }
        }
    }

    override suspend fun remove(gameId: Long) = withContext(Dispatchers.IO) {
        val logo = imageStore.logoFile(gameId)
        val kept = imageStore.removedLogoFile(gameId)
        kept.delete()
        // ponytail: salinan Urungkan bertahan sampai logo diubah lagi atau game dihapus; maks. satu berkas per game.
        if (logo.exists() && !logo.renameTo(kept)) throw ImageError.IoError
        gameDao.setLogo(gameId, null)
    }

    override suspend fun restore(gameId: Long) = withContext(Dispatchers.IO) {
        val kept = imageStore.removedLogoFile(gameId)
        if (!kept.exists() || !kept.renameTo(imageStore.logoFile(gameId))) throw ImageError.IoError
        gameDao.setLogo(gameId, imageStore.logoPath(gameId))
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
        const val BUFFER_BYTES = 16 * 1024
    }
}
