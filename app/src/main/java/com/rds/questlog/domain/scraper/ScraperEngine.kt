package com.rds.questlog.domain.scraper

import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.model.ScrapedImage
import com.rds.questlog.domain.model.ScrapedNode

sealed interface ScrapingResult {
    data class Success(val nodes: List<ScrapedNode>, val images: List<ScrapedImage> = emptyList()) : ScrapingResult
    data class Failure(val failure: PageFailure) : ScrapingResult
}

interface ScraperEngine {

    /**
     * Mengunduh dan mengekstrak satu halaman. Tidak pernah melempar: kegagalan jaringan, HTTP, dan parsing
     * dikembalikan sebagai [ScrapingResult.Failure].
     */
    suspend fun scrape(url: String): ScrapingResult
}
