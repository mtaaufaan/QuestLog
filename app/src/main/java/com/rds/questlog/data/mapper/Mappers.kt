package com.rds.questlog.data.mapper

import com.rds.questlog.data.local.entity.SourcePageEntity
import com.rds.questlog.data.local.relation.ArticleWithDetails
import com.rds.questlog.data.local.relation.GameWithCount
import com.rds.questlog.domain.model.Article
import com.rds.questlog.domain.model.Game
import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.model.ReadMode
import com.rds.questlog.domain.model.SourcePage
import com.rds.questlog.domain.model.SourcePageStatus

private const val HTTP_PREFIX = "HTTP:"

/** Kode yang disimpan di `source_pages.failure_reason`. */
internal fun PageFailure.toCode(): String = when (this) {
    is PageFailure.Http -> "$HTTP_PREFIX$code"
    PageFailure.Timeout -> "TIMEOUT"
    PageFailure.ConnectionFailed -> "CONNECTION"
    PageFailure.EmptyContent -> "EMPTY"
    PageFailure.TooLong -> "TOO_LONG"
    PageFailure.Unknown -> "UNKNOWN"
}

internal fun String.toPageFailure(): PageFailure = when {
    startsWith(HTTP_PREFIX) -> PageFailure.Http(removePrefix(HTTP_PREFIX).toIntOrNull() ?: 0)
    this == "TIMEOUT" -> PageFailure.Timeout
    this == "CONNECTION" -> PageFailure.ConnectionFailed
    this == "EMPTY" -> PageFailure.EmptyContent
    this == "TOO_LONG" -> PageFailure.TooLong
    else -> PageFailure.Unknown
}

internal fun SourcePageEntity.toDomain() = SourcePage(
    id = id,
    url = sourceUrl,
    order = pageOrder,
    status = SourcePageStatus.entries.firstOrNull { it.name == status } ?: SourcePageStatus.PENDING,
    failure = failureReason?.toPageFailure(),
)

internal fun ArticleWithDetails.toDomain() = Article(
    id = article.id,
    gameId = article.gameId,
    gameName = game.title,
    title = article.title,
    pages = pages.sortedBy { it.pageOrder }.map { it.toDomain() },
    checkpointNodeId = checkpoint?.anchorNodeId,
    lastVisitedNodeId = checkpoint?.lastVisitedNodeId,
    lastReadAt = checkpoint?.lastReadAt,
    createdAt = article.createdAt,
    checkpointFallbackOrder = checkpoint?.fallbackOrder,
    readMode = article.readMode?.let { name -> ReadMode.entries.firstOrNull { it.name == name } },
)

internal fun GameWithCount.toDomain() = Game(id = id, name = name, createdAt = createdAt, articleCount = articleCount)
