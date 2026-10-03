package com.rds.questlog.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object ArticleList

@Serializable
data class AddArticle(val mode: String = "new", val targetArticleId: Long? = null)

@Serializable
data class Reader(val articleId: Long, val resumeFrom: String = "last")
