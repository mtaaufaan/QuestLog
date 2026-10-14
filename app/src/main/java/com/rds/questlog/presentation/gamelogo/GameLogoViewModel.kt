package com.rds.questlog.presentation.gamelogo

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rds.questlog.R
import com.rds.questlog.domain.error.QuestLogError.ImageError
import com.rds.questlog.domain.usecase.game.RemoveGameLogoUseCase
import com.rds.questlog.domain.usecase.game.RestoreGameLogoUseCase
import com.rds.questlog.domain.usecase.game.SetGameLogoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Pesan hasil; [undoGameId] terisi = ada aksi Urungkan untuk game itu. */
data class LogoMessage(@StringRes val res: Int, val undoGameId: Long? = null)

data class GameLogoUiState(val busyGameId: Long? = null, val message: LogoMessage? = null)

/** Use case logo game dikelompokkan agar konstruktor ViewModel tetap ringkas. */
class GameLogoUseCases @Inject constructor(
    val set: SetGameLogoUseCase,
    val remove: RemoveGameLogoUseCase,
    val restore: RestoreGameLogoUseCase,
)

/**
 * Logo game di S0/S1: memasang, mencabut, dan mengurungkan lewat use case; menyajikan spinner dan pesan. Data logo
 * sendiri sampai ke layar lewat daftar game yang diamati.
 */
@HiltViewModel
class GameLogoViewModel @Inject constructor(private val logos: GameLogoUseCases) : ViewModel() {

    private val _state = MutableStateFlow(GameLogoUiState())
    val state: StateFlow<GameLogoUiState> = _state

    private var messageJob: Job? = null

    /** Memasang gambar [uri] sebagai logo game [gameId]. */
    fun set(gameId: Long, uri: String) {
        _state.update { it.copy(busyGameId = gameId) }
        viewModelScope.launch {
            val result = logos.set(gameId, uri)
            _state.update { it.copy(busyGameId = null) }
            show(result.fold({ LogoMessage(R.string.game_logo_updated) }, { LogoMessage(errorMessage(it)) }))
        }
    }

    /** Mencabut logo game [gameId] (tanpa konfirmasi); snackbar menawarkan Urungkan. */
    fun remove(gameId: Long) {
        viewModelScope.launch {
            val result = logos.remove(gameId)
            show(
                result.fold(
                    { LogoMessage(R.string.game_logo_removed, undoGameId = gameId) },
                    { LogoMessage(R.string.game_logo_failed) },
                ),
            )
        }
    }

    /** Urungkan pencabutan logo game [gameId]. */
    fun undo(gameId: Long) {
        viewModelScope.launch {
            val result = logos.restore(gameId)
            show(
                result.fold(
                    { LogoMessage(R.string.game_logo_updated) },
                    { LogoMessage(R.string.game_logo_failed) },
                ),
            )
        }
    }

    private fun show(message: LogoMessage) {
        messageJob?.cancel()
        _state.update { it.copy(message = message) }
        messageJob = viewModelScope.launch {
            delay(if (message.undoGameId != null) UNDO_MS else MESSAGE_MS)
            _state.update { it.copy(message = null) }
        }
    }

    @StringRes
    private fun errorMessage(error: Throwable): Int = when (error) {
        ImageError.InvalidImage -> R.string.game_logo_invalid
        ImageError.TooLarge -> R.string.game_logo_too_large
        else -> R.string.game_logo_failed
    }

    private companion object {
        const val MESSAGE_MS = 2_600L
        const val UNDO_MS = 4_000L
    }
}
