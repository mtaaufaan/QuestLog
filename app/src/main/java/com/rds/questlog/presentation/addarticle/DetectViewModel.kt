package com.rds.questlog.presentation.addarticle

import androidx.lifecycle.ViewModel
import com.rds.questlog.domain.error.QuestLogError.PremiumError
import com.rds.questlog.domain.usecase.article.DetectPaginationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Menjembatani tombol "Cari halaman lanjutan" di S2 ke [DetectPaginationUseCase] (OkHttp, lewat PremiumGuard). */
@HiltViewModel
class DetectViewModel @Inject constructor(private val detectPagination: DetectPaginationUseCase) : ViewModel() {

    /** Memeriksa [request]; kegagalan menjadi [DetectOutcome.Failed] dan tidak menghalangi simpan manual. */
    suspend fun detect(request: DetectRequest): DetectOutcome =
        detectPagination(request.base, request.taken, request.slots).fold(
            onSuccess = { DetectOutcome.Urls(it) },
            onFailure = { if (it is PremiumError) DetectOutcome.Locked else DetectOutcome.Failed },
        )
}
