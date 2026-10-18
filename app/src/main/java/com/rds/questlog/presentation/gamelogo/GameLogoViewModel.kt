package com.rds.questlog.presentation.gamelogo

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rds.questlog.R
import com.rds.questlog.domain.error.QuestLogError.ImageError
import com.rds.questlog.domain.error.QuestLogError.PremiumError
import com.rds.questlog.domain.model.LogoCrop
import com.rds.questlog.domain.usecase.game.RemoveGameLogoUseCase
import com.rds.questlog.domain.usecase.game.RestoreGameLogoUseCase
import com.rds.questlog.domain.usecase.game.SaveGameLogoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Pesan hasil; [undoGameId] terisi = ada aksi Urungkan untuk game itu. */
data class LogoMessage(@StringRes val res: Int, val undoGameId: Long? = null)

data class GameLogoUiState(val saving: Boolean = false, val message: LogoMessage? = null)

/** Use case logo game dikelompokkan agar konstruktor ViewModel tetap ringkas. */
class GameLogoUseCases @Inject constructor(
    val save: SaveGameLogoUseCase,
    val remove: RemoveGameLogoUseCase,
    val restore: RestoreGameLogoUseCase,
)

/**
 * Logo game lintas layar (P19 menyimpan/menghapus, S0/S1 menampilkan hasil dan Urungkan): dipegang di tingkat
 * Activity agar pesan hasil yang muncul setelah P19 ditutup tetap terbaca di layar asal. Data logo sendiri sampai ke
 * layar lewat daftar game yang diamati.
 */
@HiltViewModel
class GameLogoViewModel @Inject constructor(private val logos: GameLogoUseCases) : ViewModel() {

    private val _state = MutableStateFlow(GameLogoUiState())
    val state: StateFlow<GameLogoUiState> = _state

    private val doneEvents = Channel<Unit>(Channel.CONFLATED)

    /** Berhasil menyimpan/menghapus: P19 menutup dirinya. */
    val done: Flow<Unit> = doneEvents.receiveAsFlow()

    private var messageJob: Job? = null

    /** Menyimpan logo [gameId] dari bingkai [crop] pada [uri] (null = gambar asli tersimpan, atur ulang posisi). */
    fun save(gameId: Long, uri: String?, crop: LogoCrop) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = logos.save(gameId, uri, crop)
            _state.update { it.copy(saving = false) }
            show(result.fold({ LogoMessage(R.string.game_logo_updated) }, { LogoMessage(errorMessage(it)) }))
            if (result.isSuccess) doneEvents.trySend(Unit)
        }
    }

    /** Menghapus logo [gameId] (tanpa konfirmasi); layar asal menawarkan Urungkan. */
    fun remove(gameId: Long) {
        viewModelScope.launch {
            val result = logos.remove(gameId)
            show(
                result.fold(
                    { LogoMessage(R.string.game_logo_removed, undoGameId = gameId) },
                    { LogoMessage(errorMessage(it)) },
                ),
            )
            if (result.isSuccess) doneEvents.trySend(Unit)
        }
    }

    /** Urungkan penghapusan logo [gameId] (berkas asli dan posisi crop kembali). */
    fun undo(gameId: Long) {
        viewModelScope.launch {
            val result = logos.restore(gameId)
            show(result.fold({ LogoMessage(R.string.game_logo_restored) }, { LogoMessage(errorMessage(it)) }))
        }
    }

    /** Premium hilang: logo hanya bisa dilihat. */
    fun showReadOnly() = show(LogoMessage(R.string.reader_snackbar_read_only))

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
        is PremiumError -> R.string.reader_snackbar_read_only
        else -> R.string.game_logo_failed
    }

    private companion object {
        const val MESSAGE_MS = 2_600L
        const val UNDO_MS = 4_000L
    }
}
