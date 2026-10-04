package com.rds.questlog.presentation.articlelist

import androidx.annotation.StringRes
import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameUiModel
import com.rds.questlog.presentation.model.PageStatus

data class ArticleListUiState(
    val games: List<GameUiModel> = emptyList(),
    val articles: List<ArticleUiModel> = emptyList(),
    /** null = "Semua Game". */
    val selectedGameFilter: Long? = null,
    val query: String = "",
    val isPremium: Boolean = false,
    val isLoading: Boolean = false,
    /** Pesan toast yang sedang tampil; dikosongkan ViewModel setelah ±2,6 detik. */
    @StringRes val snackbar: Int? = null,
)

fun ArticleListUiState.articleById(id: Long): ArticleUiModel? = articles.firstOrNull { it.id == id }

fun ArticleListUiState.withoutArticle(id: Long): ArticleListUiState =
    copy(articles = articles.filterNot { it.id == id })

/** Retry mengulang SEMUA halaman FAILED (FAILED → PENDING) dan membuat artikel kembali SCRAPING. */
fun ArticleListUiState.withFailedPagesRetried(id: Long): ArticleListUiState = copy(
    articles = articles.map { article ->
        if (article.id != id) {
            article
        } else {
            article.copy(
                status = ArticleStatus.SCRAPING,
                pages = article.pages.map {
                    if (it.status == PageStatus.FAILED) it.copy(status = PageStatus.PENDING, reason = null) else it
                },
            )
        }
    },
)
