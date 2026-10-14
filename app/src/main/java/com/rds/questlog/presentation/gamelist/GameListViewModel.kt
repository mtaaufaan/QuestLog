package com.rds.questlog.presentation.gamelist

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rds.questlog.R
import com.rds.questlog.domain.usecase.article.GetArticlesUseCase
import com.rds.questlog.domain.usecase.game.DeleteGameUseCase
import com.rds.questlog.domain.usecase.game.GetGamesUseCase
import com.rds.questlog.domain.usecase.premium.PremiumGuard
import com.rds.questlog.presentation.articlelist.ArticleUiMapper
import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameUiModel
import com.rds.questlog.presentation.model.PremiumUiState
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GameListUiState(
    val games: List<GameUiModel> = emptyList(),
    /** Hasil pencarian judul artikel lintas game; kosong bila [query] kosong. */
    val results: List<ArticleUiModel> = emptyList(),
    val totalArticles: Int = 0,
    val query: String = "",
    val premium: PremiumUiState = PremiumUiState.FREE,
    @StringRes val snackbar: Int? = null,
)

/**
 * S0 Game List: mengamati game, jumlah artikel, dan hasil pencarian lintas game dari use case yang sudah ada.
 * Artikel utama (Lanjutkan Game) dan Binder menyusul di Sprint 9.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GameListViewModel @Inject constructor(
    getGames: GetGamesUseCase,
    getArticles: GetArticlesUseCase,
    premiumGuard: PremiumGuard,
    private val deleteGameUseCase: DeleteGameUseCase,
    private val mapper: ArticleUiMapper,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val snackbar = MutableStateFlow<Int?>(null)
    private var snackbarJob: Job? = null

    private val everything: Flow<List<ArticleUiModel>> = getArticles("", null)
        .map { list -> list.map(mapper::toUi) }
        .catch { emit(emptyList()) }

    private val results: Flow<List<ArticleUiModel>> = query.flatMapLatest { q ->
        if (q.isBlank()) {
            flowOf(emptyList())
        } else {
            getArticles(q, null).map { list -> list.map(mapper::toUi) }.catch { emit(emptyList()) }
        }
    }

    private val games: Flow<List<GameUiModel>> = combine(
        getGames().map { list -> list.map(mapper::toUi) }.catch { emit(emptyList()) },
        everything,
    ) { list, all ->
        list.map { game ->
            game.copy(scrapingCount = all.count { it.gameId == game.id && it.status == ArticleStatus.SCRAPING })
        }
    }

    private val premium: Flow<PremiumUiState> = premiumGuard.state()
        .map { PremiumUiState.valueOf(it.name) }
        .catch { emit(PremiumUiState.FREE) }

    val uiState: StateFlow<GameListUiState> = combine(query, games, results, premium) { q, gameList, found, tier ->
        GameListUiState(
            games = gameList,
            results = found,
            totalArticles = gameList.sumOf { it.articleCount },
            query = q,
            premium = tier,
        )
    }.combine(snackbar) { state, message -> state.copy(snackbar = message) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GameListUiState())

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun onScrapingArticleTapped() = showSnackbar(R.string.article_list_snackbar_scraping)

    /** Menghapus game beserta seluruh artikel dan data turunannya setelah konfirmasi di Delete Game Dialog. */
    fun deleteGame(gameId: Long) {
        viewModelScope.launch {
            deleteGameUseCase(gameId)
                .onSuccess { showSnackbar(R.string.game_list_snackbar_deleted) }
                .onFailure { showSnackbar(R.string.game_list_snackbar_delete_failed) }
        }
    }

    /** Game Binder belum dibangun (Sprint 9); ketukan pengguna premium hanya memberi tahu. */
    fun onBinderTapped() = showSnackbar(R.string.binder_coming_soon)

    private fun showSnackbar(@StringRes message: Int) {
        snackbarJob?.cancel()
        snackbar.value = message
        snackbarJob = viewModelScope.launch {
            delay(SNACKBAR_MS)
            snackbar.value = null
        }
    }

    private companion object {
        const val SNACKBAR_MS = 2_600L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
