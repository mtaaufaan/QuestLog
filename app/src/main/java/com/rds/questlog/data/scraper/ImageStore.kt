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
}
