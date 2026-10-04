package com.rds.questlog.presentation.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rds.questlog.domain.model.UnlockOutcome
import com.rds.questlog.domain.usecase.game.GetGamesUseCase
import com.rds.questlog.domain.usecase.premium.ObservePremiumStatusUseCase
import com.rds.questlog.domain.usecase.premium.PurchaseUnlimitedUseCase
import com.rds.questlog.domain.usecase.premium.RestorePurchasesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Unlock Sheet: jumlah game (bukan artikel) dan status premium dibaca lewat use case; Unlock/Restore dijalankan lewat
 * use case dan hasilnya dipetakan ke fase + pesan. Sukses tidak perlu pesan: [isPremium] (dari app_config) berubah
 * dan sheet menampilkan layar "Unlimited aktif".
 */
@HiltViewModel
class UnlockViewModel @Inject constructor(
    getGames: GetGamesUseCase,
    observePremiumStatus: ObservePremiumStatusUseCase,
    private val purchaseUnlimited: PurchaseUnlimitedUseCase,
    private val restorePurchases: RestorePurchasesUseCase,
) : ViewModel() {

    val gamesUsed: StateFlow<Int> = getGames().map { it.size }.catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    val isPremium: StateFlow<Boolean> = observePremiumStatus().catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    private val _phase = MutableStateFlow(UnlockPhase.IDLE)
    val phase: StateFlow<UnlockPhase> = _phase

    private val _message = MutableStateFlow<UnlockMessage?>(null)
    val message: StateFlow<UnlockMessage?> = _message

    /** Memulai alur pembelian Unlimited. */
    fun purchase() = run(UnlockPhase.PROCESSING) { purchaseUnlimited() }

    /** Restore Purchase: memeriksa ulang pembelian di toko. */
    fun restore() = run(UnlockPhase.RESTORING) { restorePurchases() }

    private fun run(phase: UnlockPhase, action: suspend () -> Result<UnlockOutcome>) {
        if (_phase.value != UnlockPhase.IDLE) return
        _message.value = null
        _phase.value = phase
        viewModelScope.launch {
            _message.value = action().fold(
                onSuccess = { outcome ->
                    when (outcome) {
                        UnlockOutcome.NOT_FOUND -> UnlockMessage.NOT_FOUND
                        UnlockOutcome.UNAVAILABLE -> UnlockMessage.UNAVAILABLE
                        UnlockOutcome.ACTIVATED, UnlockOutcome.CANCELLED -> null
                    }
                },
                onFailure = { UnlockMessage.FAILED },
            )
            _phase.value = UnlockPhase.IDLE
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
