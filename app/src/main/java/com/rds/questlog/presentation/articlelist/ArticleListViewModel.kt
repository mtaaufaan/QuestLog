package com.rds.questlog.presentation.articlelist

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rds.questlog.R
import com.rds.questlog.presentation.preview.PreviewData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel sementara untuk Sprint 1 Tahap 1: sumber data = [PreviewData], tanpa Room/use case.
 * Tahap 2 mengganti sumber data dengan use case; bentuk [ArticleListUiState] dan fungsi publik dipertahankan.
 */
@HiltViewModel
class ArticleListViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(
        ArticleListUiState(games = PreviewData.games, articles = PreviewData.articles),
    )
    val uiState: StateFlow<ArticleListUiState> = _uiState.asStateFlow()

    private var snackbarJob: Job? = null

    fun onQueryChange(query: String) = _uiState.update { it.copy(query = query) }

    fun onSelectFilter(gameId: Long?) = _uiState.update { it.copy(selectedGameFilter = gameId) }

    fun onScrapingArticleTapped() = showSnackbar(R.string.article_list_snackbar_scraping)

    fun deleteArticle(articleId: Long) {
        _uiState.update { it.withoutArticle(articleId) }
        showSnackbar(R.string.article_list_snackbar_deleted)
    }

    fun retryFailedPages(articleId: Long) {
        _uiState.update { it.withFailedPagesRetried(articleId) }
        showSnackbar(R.string.article_list_snackbar_retrying)
    }

    private fun showSnackbar(@StringRes message: Int) {
        snackbarJob?.cancel()
        _uiState.update { it.copy(snackbar = message) }
        snackbarJob = viewModelScope.launch {
            delay(SNACKBAR_DURATION_MS)
            _uiState.update { it.copy(snackbar = null) }
        }
    }

    private companion object {
        const val SNACKBAR_DURATION_MS = 2_600L
    }
}
