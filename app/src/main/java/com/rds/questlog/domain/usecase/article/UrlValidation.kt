package com.rds.questlog.domain.usecase.article

import com.rds.questlog.domain.error.QuestLogError.ValidationError
import com.rds.questlog.domain.repository.ArticleRepository

internal const val MAX_URLS = 10

/** Validasi bentuk URL (sebelum menyentuh database): jumlah 1-10, diawali https://, tanpa duplikat dalam input. */
internal fun validateUrls(urls: List<String>): ValidationError? = when {
    urls.isEmpty() -> ValidationError.NoUrlsProvided
    urls.size > MAX_URLS -> ValidationError.TooManyUrls
    urls.any { !it.startsWith("https://", ignoreCase = true) } -> ValidationError.InvalidUrl
    else -> urls.groupBy { it }.entries.firstOrNull { it.value.size > 1 }?.let { ValidationError.DuplicateUrl(it.key) }
}

/** Gagal bila salah satu [urls] sudah tersimpan di artikel mana pun; dijalankan sebelum scraping dijadwalkan. */
internal suspend fun ArticleRepository.requireNotStored(urls: List<String>) {
    findStoredUrls(urls).firstOrNull()?.let { throw ValidationError.UrlAlreadySaved(it) }
}
