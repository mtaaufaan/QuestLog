package com.rds.questlog.domain.model

data class Game(
    val id: Long,
    val name: String,
    val createdAt: Long,
    /** Jumlah artikel milik game ini (untuk Game Filter Sheet dan badge tier). */
    val articleCount: Int = 0,
)
