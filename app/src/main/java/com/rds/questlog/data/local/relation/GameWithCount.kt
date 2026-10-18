package com.rds.questlog.data.local.relation

/** Hasil agregasi `games LEFT JOIN articles` untuk S0 Game List dan badge tier. */
data class GameWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val articleCount: Int,
    /** Judul artikel yang terakhir dibaca di game ini; null bila belum ada yang dibaca. */
    val lastTitle: String? = null,
    val lastReadAt: Long? = null,
    /** Artikel utama game ini dan judulnya; null bila belum ada. */
    val mainArticleId: Long? = null,
    val mainArticleTitle: String? = null,
    val logoPath: String? = null,
    val logoSourcePath: String? = null,
    val logoCropZoom: Float? = null,
    val logoCropX: Float? = null,
    val logoCropY: Float? = null,
)
