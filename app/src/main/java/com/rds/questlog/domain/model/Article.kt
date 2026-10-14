package com.rds.questlog.domain.model

enum class ScrapingStatus { SCRAPING, READY, ERROR }

data class Article(
    val id: Long,
    val gameId: Long,
    val gameName: String,
    val title: String,
    val pages: List<SourcePage>,
    /** Penanda manual (checkpoints.anchor_node_id). */
    val checkpointNodeId: Long?,
    /** Posisi baca terakhir otomatis (checkpoints.last_visited_node_id). */
    val lastVisitedNodeId: Long?,
    val lastReadAt: Long?,
    val createdAt: Long,
    /** display_order anchor checkpoint; cadangan bila node anchor sudah tidak ada. */
    val checkpointFallbackOrder: Int? = null,
    /** Mode baca terakhir artikel ini; null = belum pernah dipilih (anggap SEAMLESS). */
    val readMode: ReadMode? = null,
    /** Jumlah bookmark artikel ini (indikator di daftar artikel). */
    val bookmarkCount: Int = 0,
    /** Artikel punya catatan umum yang tidak kosong. */
    val hasNote: Boolean = false,
) {
    val status: ScrapingStatus get() = pages.toScrapingStatus()
}

private val UNFINISHED = setOf(SourcePageStatus.PENDING, SourcePageStatus.IN_PROGRESS)

/**
 * Status artikel diturunkan dari halaman sumbernya (tidak disimpan):
 * masih ada PENDING/IN_PROGRESS → SCRAPING; semua FAILED → ERROR; selain itu READY (partial bila ada FAILED).
 */
fun List<SourcePage>.toScrapingStatus(): ScrapingStatus = when {
    any { it.status in UNFINISHED } -> ScrapingStatus.SCRAPING
    isNotEmpty() && all { it.status == SourcePageStatus.FAILED } -> ScrapingStatus.ERROR
    else -> ScrapingStatus.READY
}
