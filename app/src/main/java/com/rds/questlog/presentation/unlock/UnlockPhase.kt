package com.rds.questlog.presentation.unlock

import androidx.annotation.StringRes
import com.rds.questlog.R

/** Tahap proses di dalam sheet: menunggu, membeli, memulihkan pembelian, atau sudah aktif. */
enum class UnlockPhase { IDLE, PROCESSING, RESTORING, DONE }

/** Pesan di bawah catatan sheet setelah Unlock/Restore tidak berujung aktif. */
enum class UnlockMessage(@StringRes val text: Int, val isError: Boolean) {
    NOT_FOUND(R.string.unlock_msg_not_found, isError = false),
    UNAVAILABLE(R.string.unlock_msg_unavailable, isError = true),
    FAILED(R.string.unlock_msg_failed, isError = true),
}
