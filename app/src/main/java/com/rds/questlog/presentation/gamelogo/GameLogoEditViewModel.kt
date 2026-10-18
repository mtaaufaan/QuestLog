package com.rds.questlog.presentation.gamelogo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rds.questlog.domain.usecase.game.GetGamesUseCase
import com.rds.questlog.presentation.articlelist.ArticleUiMapper
import com.rds.questlog.presentation.model.GameUiModel
import com.rds.questlog.presentation.navigation.GameLogoEdit
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Game yang logonya diatur di P19 (logo, gambar asli, dan posisi crop tersimpan); null = belum dimuat / tidak ada. */
@HiltViewModel
class GameLogoEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getGames: GetGamesUseCase,
    mapper: ArticleUiMapper,
) : ViewModel() {

    val gameId: Long = savedStateHandle.toRoute<GameLogoEdit>().gameId

    val game: StateFlow<GameUiModel?> = getGames()
        .map { games -> games.firstOrNull { it.id == gameId }?.let(mapper::toUi) }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
