package com.rds.questlog.domain.usecase.article

import com.rds.questlog.domain.error.QuestLogError.DetectionFailed
import com.rds.questlog.domain.error.QuestLogError.ValidationError
import com.rds.questlog.domain.model.PremiumFeature
import com.rds.questlog.domain.scraper.PaginationDetector
import com.rds.questlog.domain.scraper.PaginationResult
import com.rds.questlog.domain.usecase.premium.PremiumGuard
import com.rds.questlog.domain.usecase.writing
import java.net.URI
import javax.inject.Inject

/**
 * Auto-detect pagination (QL-34, SRS-FR-25, premium): mengusulkan halaman lanjutan dari [baseUrl] hanya saat
 * dipanggil dari tombol "Cari halaman lanjutan" (tidak pernah otomatis). Hanya halaman [baseUrl] yang diambil;
 * tidak ada halaman usulan yang diambil atau ditambahkan sebelum pengguna mengonfirmasi.
 *
 * Aturan usulan: hanya `https://` dan domain sama dengan [baseUrl] (tanpa awalan `www.`), tanpa fragmen, bukan
 * [baseUrl] sendiri, tanpa duplikat dan tanpa URL yang sudah ada di [taken] (URL di form, artikel target, atau artikel
 * lain), urutan mengikuti hasil deteksi, dan dibatasi [slots] (sisa slot dari 10 URL).
 *
 * Gratis atau premium hilang -> `PremiumError.Locked` (UI membuka Unlock Sheet). Gagal mengambil halaman ->
 * `DetectionFailed` (tidak menghalangi penyimpanan manual; hanya tidak ada usulan).
 */
class DetectPaginationUseCase @Inject constructor(
    private val guard: PremiumGuard,
    private val detector: PaginationDetector,
) {
    /** Mengembalikan URL usulan (bisa kosong), atau kegagalan. */
    suspend operator fun invoke(baseUrl: String, taken: Set<String>, slots: Int): Result<List<String>> = writing {
        guard.requireWrite(PremiumFeature.AUTO_DETECT)
        val base = parseHttps(baseUrl.trim()) ?: throw ValidationError.InvalidUrl
        if (slots <= 0) return@writing emptyList()
        when (val result = detector.detect(baseUrl.trim())) {
            is PaginationResult.Failure -> throw DetectionFailed(result.failure)
            is PaginationResult.Links -> suggestions(base, taken + baseUrl.trim(), result.urls, slots)
        }
    }

    private fun suggestions(base: URI, taken: Set<String>, links: List<String>, slots: Int): List<String> = links
        .map { it.substringBefore('#') }
        .filter { it !in taken }
        .filter { url -> parseHttps(url)?.let { sameDomain(base, it) } == true }
        .distinct()
        .take(slots)
}

private fun parseHttps(url: String): URI? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    return uri.takeIf { it.scheme.equals("https", ignoreCase = true) && !it.host.isNullOrEmpty() }
}

private fun sameDomain(a: URI, b: URI): Boolean =
    a.host.lowercase().removePrefix("www.") == b.host.lowercase().removePrefix("www.")
