package com.rds.questlog.domain.repository

import com.rds.questlog.domain.model.LogoCrop

/** Logo kustom per game (QL-38); berkas di penyimpanan internal, path dan posisi crop di tabel `games`. */
interface GameLogoRepository {

    /**
     * Menyimpan logo game [gameId]: bila [source] (URI konten dari Photo Picker) ada, gambarnya menjadi gambar asli
     * baru (sisi terpanjang maks. 2048 px); bila null, gambar asli yang tersimpan dipakai ulang (atur ulang posisi).
     * Area di dalam bingkai [crop] dipotong menjadi 256x256 WebP. Melempar `ImageError` (InvalidImage / TooLarge /
     * IoError); logo lama baru diganti setelah yang baru berhasil ditulis.
     */
    suspend fun save(gameId: Long, source: String?, crop: LogoCrop)

    /** Mencabut logo game [gameId]; berkasnya disimpan sementara agar bisa [restore] (Urungkan). */
    suspend fun remove(gameId: Long)

    /** Memulihkan logo (berkas asli dan posisi crop) yang baru dicabut oleh [remove]; melempar `ImageError.IoError`. */
    suspend fun restore(gameId: Long)
}
