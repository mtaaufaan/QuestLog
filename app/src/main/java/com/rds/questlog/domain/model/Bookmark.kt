package com.rds.questlog.domain.model

/** Panjang maksimum label bookmark (SRS-FR-19). */
const val MAX_BOOKMARK_LABEL = 60

/** Cuplikan teks anchor yang disimpan bersama bookmark. */
const val MAX_BOOKMARK_SNIPPET = 200

/** Bookmark (v1.0, premium): anchor ke node konten [nodeId] milik [articleId]; [articleTitle] untuk pengelompokan. */
data class Bookmark(
    val id: Long,
    val articleId: Long,
    val articleTitle: String,
    val nodeId: Long,
    val label: String,
    val snippet: String,
    val createdAt: Long,
)
