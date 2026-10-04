package com.rds.questlog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rds.questlog.domain.usecase.premium.SyncPremiumStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Tingkat aplikasi: setiap aplikasi dibuka, status premium diverifikasi ulang ke toko lalu di-cache di app_config. */
@HiltViewModel
class AppViewModel @Inject constructor(private val syncPremiumStatus: SyncPremiumStatusUseCase) : ViewModel() {

    private var verified = false

    /** Memverifikasi pembelian sekali per peluncuran (tidak diulang saat layar diputar). */
    fun verifyPremium() {
        if (verified) return
        verified = true
        viewModelScope.launch { syncPremiumStatus() }
    }
}
