package com.rds.questlog.domain.scraper

interface ScrapeScheduler {

    /** Menjadwalkan scraping halaman PENDING milik [articleId] di latar belakang (berlanjut walau app di-minimize). */
    fun schedule(articleId: Long)
}
