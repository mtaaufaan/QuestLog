package com.rds.questlog.domain.model

/** Tipe `content_nodes.node_type` (database_schema.md); [dbValue] = nilai yang disimpan. */
enum class NodeType(val dbValue: String) {
    H1("h1"),
    H2("h2"),
    H3("h3"),
    P("p"),
    P_CONT("p_cont"),
    IMG("img"),
    PRE("pre"),
    TABLE("table"),
    LI("li"),
}

/** Satu blok hasil ekstraksi; [metadataJson] mengikuti skema (img: path+alt, table: html). */
data class ScrapedNode(
    val type: NodeType,
    val text: String? = null,
    val metadataJson: String? = null,
)

/** Gambar yang sudah diunduh dan dikompres ke penyimpanan lokal. */
data class ScrapedImage(
    val sourceUrl: String,
    val filename: String,
    val filePath: String,
    val fileSize: Long,
)

object ScrapeLimits {
    /** Slot display_order per halaman sumber (database_schema.md: order_start..order_end). */
    const val MAX_NODES_PER_PAGE = 1000
}
