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
 * (tech-stack.md §7). URL yang sama menghasilkan file yang sama sehingga otomatis ter-dedup.
 */
class ImageDownloader @Inject constructor(
    private val client: OkHttpClient,
    private val store: ImageStore,
) {

    /** Mengembalikan gambar tersimpan, atau null bila gagal diunduh/didekode (mis. SVG); kegagalan tidak melempar. */
    suspend fun download(url: String): ScrapedImage? = withContext(Dispatchers.IO) {
        val filename = md5(url) + ".webp"
        val file = store.fileForWriting(filename)
        if ((!file.exists() || file.length() == 0L) && !fetchAndSave(url, file)) return@withContext null
        ScrapedImage(sourceUrl = url, filename = filename, filePath = file.absolutePath, fileSize = file.length())
    }

    private fun fetchAndSave(url: String, file: File): Boolean = try {
        val request = Request.Builder().url(url).header("User-Agent", BROWSER_USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            val bytes = if (response.isSuccessful) response.body?.bytes() else null
            val bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            if (bitmap == null) {
                false
            } else {
                compressAndSave(bitmap, file)
                true
            }
        }
    } catch (_: IOException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }

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
        const val MAX_DIMENSION = 1200
        const val LARGE_IMAGE_SIDE = 1000
    }
}
