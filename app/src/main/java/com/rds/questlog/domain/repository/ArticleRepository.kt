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

    /** Menghapus artikel beserta seluruh halaman, konten, checkpoint, dan file gambar yang tak dipakai lagi. */
    suspend fun deleteArticle(articleId: Long)

    /** Mengembalikan halaman FAILED milik artikel ke PENDING; mengembalikan jumlah halaman yang direset. */
    suspend fun retryFailedPages(articleId: Long): Int
}
