package com.rds.questlog.presentation.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rds.questlog.domain.usecase.game.GetGamesUseCase
import com.rds.questlog.domain.usecase.premium.ObservePremiumStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Jumlah game (bukan artikel) dan status premium untuk Unlock Sheet; hanya membaca lewat use case. */
@HiltViewModel
class UnlockViewModel @Inject constructor(
    getGames: GetGamesUseCase,
    observePremiumStatus: ObservePremiumStatusUseCase,
) : ViewModel() {

    val gamesUsed: StateFlow<Int> = getGames().map { it.size }.catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    val isPremium: StateFlow<Boolean> = observePremiumStatus().catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
