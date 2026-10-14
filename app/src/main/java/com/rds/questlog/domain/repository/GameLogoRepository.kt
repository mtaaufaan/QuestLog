package com.rds.questlog.domain.repository

/** Logo kustom per game (QL-38); file di penyimpanan internal, path di kolom `games.logo_path`. */
interface GameLogoRepository {

    /**
     * Memasang gambar [source] (URI konten dari Photo Picker) sebagai logo game [gameId]: dipotong persegi dari
     * tengah, 256x256, WebP. Melempar `ImageError` (InvalidImage / TooLarge / IoError); logo lama baru diganti
     * setelah yang baru berhasil ditulis.
     */
    suspend fun set(gameId: Long, source: String)

    /** Mencabut logo game [gameId]; berkasnya disimpan sementara agar bisa [restore] (Urungkan). */
    suspend fun remove(gameId: Long)

    /** Memulihkan logo yang baru dicabut oleh [remove]; melempar `ImageError.IoError` bila sudah tidak ada. */
    suspend fun restore(gameId: Long)
}
