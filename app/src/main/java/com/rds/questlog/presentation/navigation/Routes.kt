package com.rds.questlog.presentation.navigation

import kotlinx.serialization.Serializable

/** S0 Game List (home). */
@Serializable
object GameList

/** S1 Halaman Game: artikel milik satu game. */
@Serializable
data class ArticleList(val gameId: Long)

@Serializable
data class AddArticle(val mode: String = "new", val targetArticleId: Long? = null, val sharedUrl: String? = null)

@Serializable
data class Reader(val articleId: Long, val resumeFrom: String = "last", val startNodeId: Long = 0L)
