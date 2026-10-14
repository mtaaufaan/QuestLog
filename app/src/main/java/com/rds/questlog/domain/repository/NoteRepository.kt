package com.rds.questlog.domain.repository

import kotlinx.coroutines.flow.Flow

/** Catatan umum satu per artikel; tanpa baris berarti catatan kosong. */
interface NoteRepository {

    /** Stream isi catatan artikel ("" bila belum ada); emit ulang tiap disimpan. */
    fun observe(articleId: Long): Flow<String>

    /** Menyimpan (menimpa) catatan artikel. */
    suspend fun save(articleId: Long, content: String)

    /** Menghapus catatan artikel; sesudahnya [observe] mengembalikan "". */
    suspend fun delete(articleId: Long)
}
