package com.rds.questlog.presentation.articlelist

import com.rds.questlog.domain.model.Article
import com.rds.questlog.domain.model.Game
import com.rds.questlog.domain.model.ReadMode as DomainReadMode
import com.rds.questlog.domain.model.ScrapingStatus
import com.rds.questlog.domain.model.SourcePage
import com.rds.questlog.domain.model.SourcePageStatus
import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameUiModel
import com.rds.questlog.presentation.model.PageStatus
import com.rds.questlog.presentation.model.PageUiModel
import com.rds.questlog.presentation.model.ReadMode
import javax.inject.Inject

/** Domain → model UI (component-contract.md §0); tidak ada logika bisnis di sini. */
class ArticleUiMapper @Inject constructor(private val failures: PageFailureFormatter) {

    fun toUi(game: Game) = GameUiModel(id = game.id, name = game.name, articleCount = game.articleCount)

    fun toUi(article: Article) = ArticleUiModel(
        id = article.id,
        gameId = article.gameId,
        gameName = article.gameName,
        title = article.title,
        status = when (article.status) {
            ScrapingStatus.SCRAPING -> ArticleStatus.SCRAPING
            ScrapingStatus.READY -> ArticleStatus.READY
            ScrapingStatus.ERROR -> ArticleStatus.ERROR
        },
        pages = article.pages.map(::toUi),
        checkpointNodeId = article.checkpointNodeId,
        lastNodeId = article.lastVisitedNodeId,
        // Label "dibaca 2 jam lalu" diisi saat Reader (Sprint 3) mulai menulis checkpoints.last_read_at.
        lastRead = null,
        readMode = article.readMode?.let { if (it == DomainReadMode.PAGED) ReadMode.PAGED else ReadMode.SEAMLESS },
    )

    private fun toUi(page: SourcePage) = PageUiModel(
        url = page.url,
        status = when (page.status) {
            SourcePageStatus.PENDING, SourcePageStatus.IN_PROGRESS -> PageStatus.PENDING
            SourcePageStatus.COMPLETED -> PageStatus.DONE
            SourcePageStatus.FAILED -> PageStatus.FAILED
        },
        reason = page.failure?.let(failures::format),
    )
}
