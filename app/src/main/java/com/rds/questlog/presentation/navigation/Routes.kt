package com.rds.questlog.presentation.navigation

import kotlinx.serialization.Serializable

/** S0 Game List (home). */
@Serializable
object GameList

/** S4 Game Binder (premium): kategori artikel satu game. */
@Serializable
data class Binder(val gameId: Long)

/** P19 Logo game (premium): pilih, geser/zoom, dan simpan logo satu game. */
@Serializable
data class GameLogoEdit(val gameId: Long)

/** S1 Halaman Game: artikel milik satu game. */
@Serializable
data class ArticleList(val gameId: Long)

@Serializable
data class AddArticle(
    val mode: String = "new",
    val targetArticleId: Long? = null,
    val sharedUrl: String? = null,
    /** Game yang terisi otomatis di S2 (dari Halaman Game); null = pilih sendiri. */
    val gameId: Long? = null,
)

@Serializable
data class Reader(val articleId: Long, val resumeFrom: String = "last", val startNodeId: Long = 0L)
