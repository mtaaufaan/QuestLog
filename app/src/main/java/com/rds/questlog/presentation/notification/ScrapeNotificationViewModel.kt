package com.rds.questlog.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rds.questlog.domain.model.Article
import com.rds.questlog.domain.model.SourcePageStatus
import com.rds.questlog.domain.usecase.article.GetArticlesUseCase
import com.rds.questlog.presentation.model.ScrapeJob
import com.rds.questlog.presentation.model.ScrapeNotificationKind
import com.rds.questlog.presentation.model.ScrapeNotificationUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch

/**
 * Menurunkan Scrape Notification dari status halaman di database (yang diperbarui worker), bukan dari WorkManager:
 * progres = halaman selesai/gagal, lalu varian akhir DONE / PARTIAL / ERROR. Satu notifikasi aktif pada satu waktu.
 */
@HiltViewModel
class ScrapeNotificationViewModel @Inject constructor(
    private val getArticles: GetArticlesUseCase,
) : ViewModel() {

    private val _notification = MutableStateFlow<ScrapeNotificationUi?>(null)
    val notification: StateFlow<ScrapeNotificationUi?> = _notification

    private var tracking: Job? = null

    /** Mulai memantau [job]; menggantikan notifikasi sebelumnya. Varian akhir tampil sebentar lalu disembunyikan. */
    fun track(job: ScrapeJob) {
        tracking?.cancel()
        tracking = viewModelScope.launch {
            getArticles("", null)
                .map { list -> list.firstOrNull { it.id == job.articleId } }
                .takeWhile { it != null }
                .map { it!!.toNotification(job.skipPages) }
                .transformWhile {
                    emit(it)
                    it.kind == ScrapeNotificationKind.PROGRESS
                }
                .catch { }
                .collect { _notification.value = it }
            delay(FINAL_VISIBLE_MS)
            _notification.value = null
        }
    }

    /** Menutup notifikasi (mis. setelah di-tap). */
    fun dismiss() {
        tracking?.cancel()
        _notification.value = null
    }

    private companion object {
        const val FINAL_VISIBLE_MS = 5_000L
    }
}

/**
 * Notifikasi untuk halaman artikel dengan urutan di atas [skipPages] (halaman lama pada mode Lengkapi diabaikan):
 * masih ada yang diproses → PROGRESS; semua gagal → ERROR; sebagian gagal → PARTIAL; selain itu DONE.
 */
fun Article.toNotification(skipPages: Int = 0): ScrapeNotificationUi {
    val mine = pages.filter { it.order > skipPages }
    val failed = mine.count { it.status == SourcePageStatus.FAILED }
    val handled = mine.count { it.status == SourcePageStatus.COMPLETED || it.status == SourcePageStatus.FAILED }
    val kind = when {
        mine.any { it.status == SourcePageStatus.PENDING || it.status == SourcePageStatus.IN_PROGRESS } ->
            ScrapeNotificationKind.PROGRESS
        mine.isNotEmpty() && failed == mine.size -> ScrapeNotificationKind.ERROR
        failed > 0 -> ScrapeNotificationKind.PARTIAL
        else -> ScrapeNotificationKind.DONE
    }
    return ScrapeNotificationUi(kind, title, handled, mine.size, failed, id)
}
