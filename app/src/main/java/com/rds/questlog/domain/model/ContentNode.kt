package com.rds.questlog.domain.model

/** Mode baca per artikel (QL-5/QL-6). */
enum class ReadMode { SEAMLESS, PAGED }

/** Preferensi tampilan app-wide (bukan per artikel): ukuran teks 12-24sp dan mode gelap Reader. */
data class DisplayPreferences(
    val fontSizeSp: Int = DEFAULT_FONT_SIZE_SP,
    val darkMode: Boolean = false,
) {
    companion object {
        const val MIN_FONT_SIZE_SP = 12
        const val MAX_FONT_SIZE_SP = 24
        const val FONT_SIZE_STEP_SP = 2
        const val DEFAULT_FONT_SIZE_SP = 16
    }
}

/**
 * Satu blok konten tersimpan. [imagePath] (IMG) = file gambar lokal; [tableRows] (TABLE) = baris sel, baris pertama
 * dipakai sebagai header. [displayOrder] menentukan urutan baca dan menjadi cadangan anchor checkpoint.
 */
data class ContentNode(
    val id: Long,
    val sourcePageId: Long,
    val type: NodeType,
    val displayOrder: Int,
    val text: String = "",
    val imagePath: String? = null,
    val tableRows: List<List<String>> = emptyList(),
)

/** Artikel beserta seluruh kontennya, berurutan menurut [ContentNode.displayOrder]. */
data class ArticleDetail(val article: Article, val nodes: List<ContentNode>)

/**
 * Menerjemahkan anchor ke node yang benar-benar ada (checkpoint tidak boleh error bila anchor hilang):
 * persis [nodeId] bila masih ada; bila tidak, node pertama dengan displayOrder >= [fallbackOrder]; bila tidak ada
 * juga, node terakhir. Null bila [nodeId] null, atau tidak ada konten sama sekali.
 */
fun List<ContentNode>.resolveAnchor(nodeId: Long?, fallbackOrder: Int?): Long? {
    if (nodeId == null || isEmpty()) return null
    val exact = firstOrNull { it.id == nodeId }
    val nearest = fallbackOrder?.let { order -> firstOrNull { it.displayOrder >= order } }
    return (exact ?: nearest ?: last()).id
}
