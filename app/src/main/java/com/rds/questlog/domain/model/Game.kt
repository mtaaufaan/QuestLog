package com.rds.questlog.domain.model

data class Game(
    val id: Long,
    val name: String,
    val createdAt: Long,
    /** Jumlah artikel milik game ini (untuk S0 Game List dan badge tier). */
    val articleCount: Int = 0,
    /** Judul artikel yang terakhir dibaca di game ini dan kapan (epoch ms); null bila belum ada yang dibaca. */
    val lastArticleTitle: String? = null,
    val lastReadAt: Long? = null,
)
