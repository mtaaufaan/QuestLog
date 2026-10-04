package com.rds.questlog.domain.repository

interface CheckpointRepository {

    /**
     * Menetapkan checkpoint manual artikel di [nodeId] (anchor ke node, bukan posisi scroll); menimpa checkpoint
     * lama dan tidak menyentuh posisi baca otomatis. Gagal bila [nodeId] bukan node milik artikel.
     */
    suspend fun setCheckpoint(articleId: Long, nodeId: Long)

    /** Menyimpan posisi baca otomatis (saat meninggalkan Reader); terpisah dari checkpoint manual. */
    suspend fun saveLastPosition(articleId: Long, nodeId: Long)
}
