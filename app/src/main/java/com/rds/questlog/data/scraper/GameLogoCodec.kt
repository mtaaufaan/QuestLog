package com.rds.questlog.data.scraper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.min

/** Mengubah gambar pengguna menjadi logo: potong persegi dari tengah, 256x256 px, WebP (kualitas ±85). */
object GameLogoCodec {

    const val SIZE_PX = 256
    private const val QUALITY = 85

    /** WebP hasil, atau null bila [bytes] bukan gambar yang bisa didecode. */
    fun encode(bytes: ByteArray): ByteArray? = decodeSquare(bytes)?.let(::toWebp)

    /** Decode lalu potong persegi dari tengah dan skala ke [SIZE_PX]; null bila bukan gambar. */
    private fun decodeSquare(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        // Kecilkan saat decode (pangkat 2, sisi terpendek tetap >= SIZE_PX) agar foto besar tidak memenuhi memori.
        val sample = Integer.highestOneBit((min(bounds.outWidth, bounds.outHeight) / SIZE_PX).coerceAtLeast(1))
        val decoded = bounds.takeIf { it.outWidth > 0 && it.outHeight > 0 }?.let {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        return decoded?.let {
            val side = min(it.width, it.height)
            val square = Bitmap.createBitmap(it, (it.width - side) / 2, (it.height - side) / 2, side, side)
            Bitmap.createScaledBitmap(square, SIZE_PX, SIZE_PX, true)
        }
    }

    private fun toWebp(logo: Bitmap): ByteArray = ByteArrayOutputStream().use { out ->
        @Suppress("DEPRECATION") // WEBP_LOSSY baru ada di API 30; minSdk 26.
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
        logo.compress(format, QUALITY, out)
        out.toByteArray()
    }
}
