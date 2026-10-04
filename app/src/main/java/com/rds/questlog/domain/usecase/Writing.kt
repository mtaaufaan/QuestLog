package com.rds.questlog.domain.usecase

import com.rds.questlog.domain.error.QuestLogError
import kotlin.coroutines.cancellation.CancellationException

/**
 * Menjalankan operasi tulis dan membungkus kegagalannya ke [QuestLogError] (error_state_model.md §8):
 * `QuestLogError` diteruskan apa adanya, error lain menjadi `DatabaseError.WriteFailed`.
 * Pembatalan coroutine tidak ditelan.
 */
@Suppress("TooGenericExceptionCaught")
internal inline fun <T> writing(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: QuestLogError) {
    Result.failure(e)
} catch (e: Exception) {
    Result.failure(QuestLogError.DatabaseError.WriteFailed(e))
}
