package com.rds.questlog.data.scraper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Lokasi file gambar hasil scraping: `filesDir/images/{md5(url)}.webp` (tech-stack.md §7). */
@Singleton
class ImageStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val directory: File get() = File(context.filesDir, "images")

    fun file(filename: String): File = File(directory, filename)

    /** Membuat folder bila perlu, lalu mengembalikan [file]. */
    fun fileForWriting(filename: String): File {
        directory.mkdirs()
        return file(filename)
    }

    fun delete(filename: String) {
        file(filename).delete()
    }

    private val logoDirectory: File get() = File(context.filesDir, LOGO_DIR).also { it.mkdirs() }

    /** Logo kustom game: `filesDir/game_logos/{gameId}.webp` (256x256); folder dibuat bila perlu. */
    fun logoFile(gameId: Long): File = File(logoDirectory, "$gameId.webp")

    /** Gambar asli logo (sisi terpanjang maks. 2048 px) agar posisi crop bisa diatur ulang tanpa unggah ulang. */
    fun logoSourceFile(gameId: Long): File = File(logoDirectory, "${gameId}_src.webp")

    /** Salinan sementara logo yang baru dicabut (untuk Urungkan) di samping berkas aslinya. */
    fun removedLogoFile(gameId: Long): File = File(logoDirectory, "$gameId.removed.webp")

    fun removedLogoSourceFile(gameId: Long): File = File(logoDirectory, "${gameId}_src.removed.webp")

    /** Path logo / gambar asli [gameId] yang disimpan di database (relatif ke `filesDir`). */
    fun logoPath(gameId: Long): String = "$LOGO_DIR/$gameId.webp"

    fun logoSourcePath(gameId: Long): String = "$LOGO_DIR/${gameId}_src.webp"

    /** Menghapus salinan Urungkan (logo diganti atau dibuat baru). */
    fun deleteRemovedLogos(gameId: Long) {
        removedLogoFile(gameId).delete()
        removedLogoSourceFile(gameId).delete()
    }

    /** Menghapus semua berkas logo game [gameId] (game dihapus). */
    fun deleteLogos(gameId: Long) {
        logoFile(gameId).delete()
        logoSourceFile(gameId).delete()
        deleteRemovedLogos(gameId)
    }

    private companion object {
        const val LOGO_DIR = "game_logos"
    }
}
