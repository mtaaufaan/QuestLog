package com.rds.questlog.domain.repository

import com.rds.questlog.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {

    /** Stream semua bookmark di [gameId]: artikel terbaru dulu, lalu urutan dibuat. Emit ulang tiap data berubah. */
    fun observeByGame(gameId: Long): Flow<List<Bookmark>>

    /** Membuat bookmark di [nodeId] milik [articleId]; gagal bila node bukan milik artikel. Mengembalikan id baru. */
    suspend fun add(articleId: Long, nodeId: Long, label: String, snippet: String): Long

    /** Mengganti label bookmark [id]; posisi (anchor) tidak berubah. */
    suspend fun updateLabel(id: Long, label: String)

    /** Menghapus bookmark [id] saja; bookmark lain dan checkpoint tidak tersentuh. */
    suspend fun delete(id: Long)
}
