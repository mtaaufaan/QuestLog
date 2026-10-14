package com.rds.questlog.presentation.share

import androidx.lifecycle.ViewModel
import com.rds.questlog.domain.usecase.article.ParseSharedUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Hasil menerima teks dari menu "Bagikan": URL valid pertama, atau tidak ada URL yang bisa dipakai. */
sealed interface ShareEvent {
    data class Url(val url: String) : ShareEvent

    data object NoUrl : ShareEvent
}

/**
 * Menjembatani Share Intent (diterima Activity) ke navigasi (QL-21). Event bertahan di ViewModel sampai
 * [consume]; Activity yang dibuat ulang (rotasi) tidak memproses intent yang sama dua kali.
 */
@HiltViewModel
class ShareViewModel @Inject constructor() : ViewModel() {

    private val parse = ParseSharedUrlUseCase()
    private val _event = MutableStateFlow<ShareEvent?>(null)
    val event: StateFlow<ShareEvent?> = _event

    /** Dipanggil Activity dengan teks `EXTRA_TEXT` dari intent ACTION_SEND. */
    fun onSharedText(text: String?) {
        _event.value = parse(text)?.let(ShareEvent::Url) ?: ShareEvent.NoUrl
    }

    /** Event sudah ditangani navigasi. */
    fun consume() {
        _event.value = null
    }
}
