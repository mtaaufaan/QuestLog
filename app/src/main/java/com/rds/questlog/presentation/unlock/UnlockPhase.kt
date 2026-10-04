package com.rds.questlog.presentation.unlock

/** Tahap proses di dalam sheet: menunggu, membeli, memulihkan pembelian, atau sudah aktif. */
enum class UnlockPhase { IDLE, PROCESSING, RESTORING, DONE }
