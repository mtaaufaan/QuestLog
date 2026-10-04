package com.rds.questlog.presentation.articlelist

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rds.questlog.R
import com.rds.questlog.domain.usecase.article.DeleteArticleUseCase
import com.rds.questlog.domain.usecase.article.GetArticlesUseCase
import com.rds.questlog.domain.usecase.article.RetryFailedPagesUseCase
import com.rds.questlog.domain.usecase.game.GetGamesUseCase
import com.rds.questlog.domain.usecase.premium.ObservePremiumStatusUseCase
import com.rds.questlog.presentation.model.ArticleUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * S1: hanya mengamati Flow dari use case dan meneruskan aksi pengguna; tidak menyentuh Room/OkHttp.
 * Search dan filter game dikirim sebagai parameter query sehingga disaring database, bukan di memori.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArticleListViewModel @Inject constructor(
    getArticles: GetArticlesUseCase,
    getGames: GetGamesUseCase,
    observePremiumStatus: ObservePremiumStatusUseCase,
    private val deleteArticleUseCase: DeleteArticleUseCase,
    private val retryFailedPagesUseCase: RetryFailedPagesUseCase,
    private val mapper: ArticleUiMapper,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val gameFilter = MutableStateFlow<Long?>(null)
    private val snackbar = MutableStateFlow<Int?>(null)
    private var snackbarJob: Job? = null

    private val articles: Flow<List<ArticleUiModel>> = combine(query, gameFilter) { q, game -> q to game }
        .flatMapLatest { (q, game) ->
            getArticles(q, game)
                .map { list -> list.map(mapper::toUi) }
                .catch {
                    showSnackbar(R.string.article_list_snackbar_read_failed)
                    emit(emptyList())
                }
        }

    val uiState: StateFlow<ArticleListUiState> = combine(
        query,
        gameFilter,
        articles,
        getGames().map { list -> list.map(mapper::toUi) }.catch { emit(emptyList()) },
        observePremiumStatus().catch { emit(false) },
    ) { q, game, articleList, games, isPremium ->
        ArticleListUiState(
            games = games,
            articles = articleList,
            selectedGameFilter = game,
            query = q,
            isPremium = isPremium,
        )
    }.combine(snackbar) { state, message -> state.copy(snackbar = message) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ArticleListUiState(isLoading = true))

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun onSelectFilter(gameId: Long?) {
        gameFilter.value = gameId
    }

    fun onScrapingArticleTapped() = showSnackbar(R.string.article_list_snackbar_scraping)

    fun deleteArticle(articleId: Long) {
        viewModelScope.launch {
            deleteArticleUseCase(articleId)
                .onSuccess { showSnackbar(R.string.article_list_snackbar_deleted) }
                .onFailure { showSnackbar(R.string.article_list_snackbar_delete_failed) }
        }
    }

    fun retryFailedPages(articleId: Long) {
        viewModelScope.launch {
            retryFailedPagesUseCase(articleId)
                .onSuccess { showSnackbar(R.string.article_list_snackbar_retrying) }
                .onFailure { showSnackbar(R.string.article_list_snackbar_retry_failed) }
        }
    }

    private fun showSnackbar(@StringRes message: Int) {
        snackbarJob?.cancel()
        snackbar.value = message
        snackbarJob = viewModelScope.launch {
            delay(SNACKBAR_DURATION_MS)
            snackbar.value = null
        }
    }

    private companion object {
        const val SNACKBAR_DURATION_MS = 2_600L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
