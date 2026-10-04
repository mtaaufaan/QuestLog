package com.rds.questlog.data.scraper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.rds.questlog.domain.model.ScrapedImage
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Mengunduh gambar, mengecilkan (maks 1200px) dan mengompres ke WebP di `filesDir/images/{md5(url)}.webp`
 * (tech-stack.md §7). SVG tidak bisa didekode Bitmap, jadi disimpan apa adanya sebagai `{md5(url)}.svg` (dirender
 * Coil). URL yang sama menghasilkan file yang sama sehingga otomatis ter-dedup.
 */
class ImageDownloader @Inject constructor(
    private val client: OkHttpClient,
    private val store: ImageStore,
) {

    /** Mengembalikan gambar tersimpan, atau null bila gagal diunduh/didekode; kegagalan tidak melempar. */
    suspend fun download(url: String): ScrapedImage? = withContext(Dispatchers.IO) {
        cached(url) ?: fetchBytes(url)?.let { save(url, it) }
    }

    /**
     * Menyimpan [bytes] hasil unduhan [url] (raster -> WebP terkompresi, SVG -> apa adanya); null bila bukan gambar
     * yang bisa dibaca. Dipakai juga saat URL halaman itu sendiri adalah gambar.
     */
    fun save(url: String, bytes: ByteArray): ScrapedImage? {
        val svg = isSvg(bytes)
        val file = store.fileForWriting(md5(url) + if (svg) SVG_EXT else WEBP_EXT)
        val saved = if (svg) {
            file.writeBytes(bytes)
            true
        } else {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { compressAndSave(it, file) } != null
        }
        return if (saved) describe(url, file) else null
    }

    private fun cached(url: String): ScrapedImage? = listOf(WEBP_EXT, SVG_EXT)
        .map { store.file(md5(url) + it) }
        .firstOrNull { it.exists() && it.length() > 0 }
        ?.let { describe(url, it) }

    private fun describe(url: String, file: File) =
        ScrapedImage(sourceUrl = url, filename = file.name, filePath = file.absolutePath, fileSize = file.length())

    private fun fetchBytes(url: String): ByteArray? = try {
        val request = Request.Builder().url(url).header("User-Agent", BROWSER_USER_AGENT).build()
        client.newCall(
            request,
        ).execute().use { response -> if (response.isSuccessful) response.body?.bytes() else null }
    } catch (_: IOException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun isSvg(bytes: ByteArray): Boolean =
        bytes.copyOf(minOf(bytes.size, SVG_SNIFF_BYTES)).decodeToString().contains("<svg", ignoreCase = true)

    private fun compressAndSave(bitmap: Bitmap, file: File) {
        val scaled = if (bitmap.width > MAX_DIMENSION || bitmap.height > MAX_DIMENSION) {
            val ratio = minOf(MAX_DIMENSION.toFloat() / bitmap.width, MAX_DIMENSION.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }
        // Peta/diagram besar butuh detail lebih.
        val quality = if (scaled.width > LARGE_IMAGE_SIDE && scaled.height > LARGE_IMAGE_SIDE) 90 else 75
        file.outputStream().use { scaled.compress(webpFormat(), quality, it) }
    }

    @Suppress("DEPRECATION")
    private fun webpFormat() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Bitmap.CompressFormat.WEBP_LOSSY
    } else {
        Bitmap.CompressFormat.WEBP
    }

    private fun md5(text: String): String =
        MessageDigest.getInstance("MD5").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val WEBP_EXT = ".webp"
        const val SVG_EXT = ".svg"
        const val SVG_SNIFF_BYTES = 2_048
        const val MAX_DIMENSION = 1200
        const val LARGE_IMAGE_SIDE = 1000
    }
}
