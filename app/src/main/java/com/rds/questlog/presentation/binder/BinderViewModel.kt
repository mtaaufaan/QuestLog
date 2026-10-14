package com.rds.questlog.presentation.binder

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rds.questlog.domain.usecase.article.GetArticlesUseCase
import com.rds.questlog.domain.usecase.game.GetGamesUseCase
import com.rds.questlog.domain.usecase.premium.PremiumGuard
import com.rds.questlog.presentation.articlelist.ArticleUiMapper
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.navigation.Binder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BinderUiState(
    val gameName: String = "",
    val articles: List<ArticleUiModel> = emptyList(),
    val premium: PremiumUiState = PremiumUiState.FREE,
    @StringRes val snackbar: Int? = null,
)

/**
 * S4 Game Binder: artikel game ini dan status premium dari use case yang sudah ada. Kategori dan penetapannya masih
 * dari [BinderDummy] (tahap UI Sprint 9); UseCase dan Room menyusul di tahap wiring.
 */
@HiltViewModel
class BinderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getArticles: GetArticlesUseCase,
    getGames: GetGamesUseCase,
    premiumGuard: PremiumGuard,
    private val mapper: ArticleUiMapper,
) : ViewModel() {

    val gameId: Long = savedStateHandle.toRoute<Binder>().gameId

    private val snackbar = MutableStateFlow<Int?>(null)
    private var snackbarJob: Job? = null

    val uiState: StateFlow<BinderUiState> = combine(
        getGames().map { games -> games.firstOrNull { it.id == gameId }?.name.orEmpty() }.catch { emit("") },
        getArticles("", gameId).map { list -> list.map(mapper::toUi) }.catch { emit(emptyList()) },
        premiumGuard.state().map { PremiumUiState.valueOf(it.name) }.catch { emit(PremiumUiState.FREE) },
        snackbar,
    ) { name, articles, premium, message -> BinderUiState(name, articles, premium, message) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BinderUiState())

    /** Umpan balik singkat (mis. "Kategori dihapus — artikel tetap ada"). */
    fun showMessage(@StringRes message: Int) {
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
