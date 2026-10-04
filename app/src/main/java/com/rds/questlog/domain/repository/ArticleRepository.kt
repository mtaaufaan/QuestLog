package com.rds.questlog.domain.repository

import com.rds.questlog.domain.model.Article
import kotlinx.coroutines.flow.Flow

interface ArticleRepository {

    /**
     * Stream artikel terbaru dulu. Search judul ([query], kosong = semua) dan filter [gameId] (null = semua game)
     * dijalankan oleh database, bukan di memori.
     */
    fun observeArticles(query: String, gameId: Long?): Flow<List<Article>>

    /** Membuat artikel di [gameId] beserta halaman sumbernya (urutan = urutan [urls]); mengembalikan id artikel. */
    suspend fun insertArticle(gameId: Long, title: String, urls: List<String>): Long

    /**
     * Menambahkan [urls] sebagai halaman baru di akhir artikel (append-only): urutan dan slot display_order
     * dilanjutkan dari nilai terbesar yang ada, sehingga checkpoint lama tetap valid. Artikel kembali SCRAPING.
     */
    suspend fun appendPages(articleId: Long, urls: List<String>)

    /** Jumlah artikel milik [gameId] (untuk batas tier gratis). */
    suspend fun countForGame(gameId: Long): Int

    /** Subset [urls] yang sudah tersimpan sebagai halaman sumber di artikel mana pun. */
    suspend fun findStoredUrls(urls: List<String>): Set<String>

    /** Menghapus artikel beserta seluruh halaman, konten, checkpoint, dan file gambar yang tak dipakai lagi. */
    suspend fun deleteArticle(articleId: Long)

    /** Mengembalikan halaman FAILED milik artikel ke PENDING; mengembalikan jumlah halaman yang direset. */
    suspend fun retryFailedPages(articleId: Long): Int
}
