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
)
