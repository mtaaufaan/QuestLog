package com.rds.questlog.data.local.relation

/** Hasil agregasi `games LEFT JOIN articles`; dipakai Game Filter Sheet dan badge tier. */
data class GameWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val articleCount: Int,
)
