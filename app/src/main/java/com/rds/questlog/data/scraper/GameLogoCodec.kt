package com.rds.questlog.data.scraper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.os.Build
import com.rds.questlog.domain.model.LogoCrop
import com.rds.questlog.domain.model.sourceRect
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Mengolah gambar pengguna menjadi gambar asli logo (maks. 2048 px, tegak) dan logo 256x256 WebP. */
object GameLogoCodec {

    const val LOGO_PX = 256
    const val SOURCE_MAX_PX = 2048
    const val LOGO_QUALITY = 85
    const val SOURCE_QUALITY = 90

    /** Bitmap tegak (orientasi EXIF diterapkan), sisi terpanjang maks. [SOURCE_MAX_PX]; null bila bukan gambar. */
    fun decodeSource(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val longest = max(bounds.outWidth, bounds.outHeight)
        // Kecilkan saat decode (pangkat 2) agar foto besar tidak memenuhi memori; sisanya dipaskan di bawah.
        val sample = Integer.highestOneBit((longest / SOURCE_MAX_PX).coerceAtLeast(1))
        val decoded = bounds.takeIf { longest > 0 }?.let {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        return decoded?.let { fit(rotate(it, exifOrientation(bytes))) }
    }

    /** Logo [LOGO_PX] x [LOGO_PX]: area gambar di dalam bingkai [crop] persis. */
    fun render(source: Bitmap, crop: LogoCrop): Bitmap {
        val area = crop.sourceRect(source.width, source.height)
        val scale = LOGO_PX / area.size
        val matrix = Matrix().apply {
            postTranslate(-area.left, -area.top)
            postScale(scale, scale)
        }
        val logo = Bitmap.createBitmap(LOGO_PX, LOGO_PX, Bitmap.Config.ARGB_8888)
        Canvas(logo).drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return logo
    }

    fun toWebp(bitmap: Bitmap, quality: Int): ByteArray = ByteArrayOutputStream().use { out ->
        @Suppress("DEPRECATION") // WEBP_LOSSY baru ada di API 30; minSdk 26.
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
        bitmap.compress(format, quality, out)
        out.toByteArray()
    }

    private fun fit(bitmap: Bitmap): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= SOURCE_MAX_PX) return bitmap
        val factor = SOURCE_MAX_PX.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * factor).toInt().coerceAtLeast(1),
            (bitmap.height * factor).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun exifOrientation(bytes: ByteArray): Int = runCatching {
        ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun rotate(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> matrix.apply {
                postRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> matrix.apply {
                postRotate(270f)
                postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
