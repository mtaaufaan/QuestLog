package com.rds.questlog.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object ArticleList

@Serializable
data class AddArticle(val mode: String = "new", val targetArticleId: Long? = null, val sharedUrl: String? = null)

@Serializable
data class Reader(val articleId: Long, val resumeFrom: String = "last", val startNodeId: Long = 0L)
