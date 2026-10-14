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

    private val logoDirectory: File get() = File(context.filesDir, LOGO_DIR)

    /** Logo kustom game: `filesDir/game_logos/{gameId}.webp`; folder dibuat bila perlu. */
    fun logoFile(gameId: Long): File = File(logoDirectory.also { it.mkdirs() }, "$gameId.webp")

    /** Salinan logo yang baru dicabut (untuk Urungkan) di samping berkas aslinya. */
    fun removedLogoFile(gameId: Long): File = File(logoDirectory.also { it.mkdirs() }, "$gameId.removed.webp")

    /** Path logo [gameId] yang disimpan di database (relatif ke `filesDir`). */
    fun logoPath(gameId: Long): String = "$LOGO_DIR/$gameId.webp"

    /** Menghapus logo game [gameId] beserta salinan Urungkan-nya (game dihapus, atau logo diganti). */
    fun deleteLogos(gameId: Long) {
        logoFile(gameId).delete()
        removedLogoFile(gameId).delete()
    }

    private companion object {
        const val LOGO_DIR = "game_logos"
    }
}
