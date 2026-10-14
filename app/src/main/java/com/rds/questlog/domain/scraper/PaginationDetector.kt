package com.rds.questlog.domain.scraper

import com.rds.questlog.domain.model.PageFailure

sealed interface PaginationResult {
    /** Tautan lanjutan mentah (URL absolut) sesuai urutan terdeteksi; belum disaring domain/https/duplikat. */
    data class Links(val urls: List<String>) : PaginationResult
    data class Failure(val failure: PageFailure) : PaginationResult
}

/**
 * Mencari tautan halaman lanjutan di satu halaman web (QL-34). Hanya mengambil halaman [url] itu sendiri; halaman
 * hasil deteksi tidak pernah diambil. Tidak pernah melempar: kegagalan dikembalikan sebagai [PaginationResult.Failure].
 */
interface PaginationDetector {
    suspend fun detect(url: String): PaginationResult
}
