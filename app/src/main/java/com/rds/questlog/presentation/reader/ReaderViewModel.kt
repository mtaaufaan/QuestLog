package com.rds.questlog.presentation.reader

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rds.questlog.R
import com.rds.questlog.domain.model.DisplayPreferences
import com.rds.questlog.domain.model.ReadMode as DomainReadMode
import com.rds.questlog.domain.usecase.article.RetryFailedPagesUseCase
import com.rds.questlog.domain.usecase.prefs.ObserveDisplayPreferencesUseCase
import com.rds.questlog.domain.usecase.prefs.SetDarkModeUseCase
import com.rds.questlog.domain.usecase.prefs.SetFontSizeUseCase
import com.rds.questlog.domain.usecase.reader.GetArticleDetailUseCase
import com.rds.questlog.domain.usecase.reader.SaveLastPositionUseCase
import com.rds.questlog.domain.usecase.reader.SetCheckpointUseCase
import com.rds.questlog.domain.usecase.reader.SetReadModeUseCase
import com.rds.questlog.presentation.articlelist.ArticleUiMapper
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.ContentNodeUi
import com.rds.questlog.presentation.model.ReadMode
import com.rds.questlog.presentation.model.ReaderViewState
import com.rds.questlog.presentation.navigation.Reader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Use case aksi tulis Reader dikelompokkan agar konstruktor ViewModel tetap ringkas. */
class ReaderActions @Inject constructor(
    val setCheckpoint: SetCheckpointUseCase,
    val saveLastPosition: SaveLastPositionUseCase,
    val setReadMode: SetReadModeUseCase,
    val retryFailed: RetryFailedPagesUseCase,
    val setFontSize: SetFontSizeUseCase,
    val setDarkMode: SetDarkModeUseCase,
)

data class ReaderUiState(
    val viewState: ReaderViewState = ReaderViewState.LOADING,
    val article: ArticleUiModel? = null,
    val content: List<List<ContentNodeUi>> = emptyList(),
    /** Node tujuan auto-scroll saat dibuka (posisi terakhir atau checkpoint); null = mulai dari atas. */
    val resumeNodeId: Long? = null,
    val fontSize: Int = DisplayPreferences.DEFAULT_FONT_SIZE_SP,
    val darkMode: Boolean = false,
    @StringRes val snackbar: Int? = null,
)

/**
 * S3: menyajikan artikel + konten dari database dan meneruskan aksi pengguna ke use case (checkpoint, posisi baca,
 * mode baca, retry, preferensi tampilan). Tidak menyentuh Room/DataStore langsung.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getArticleDetail: GetArticleDetailUseCase,
    observeDisplayPreferences: ObserveDisplayPreferencesUseCase,
    private val actions: ReaderActions,
    private val mapper: ArticleUiMapper,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Reader>()
    private val articleId = route.articleId

    /** "last" atau "checkpoint": dari mana auto-scroll dimulai (menentukan teks snackbar di Reader). */
    val resumeFrom: String = route.resumeFrom

    private val reloads = MutableStateFlow(0)
    private val snackbar = MutableStateFlow<Int?>(null)
    private var snackbarJob: Job? = null

    private val screen: Flow<ReaderUiState> = reloads.flatMapLatest {
        getArticleDetail(articleId)
            .map { detail ->
                val content = detail?.toContentPages().orEmpty()
                ReaderUiState(
                    viewState = detail.toViewState(),
                    article = detail?.let { mapper.toUi(it.article).withContent(content) },
                    content = content,
                    resumeNodeId = detail?.resumeTarget(resumeFrom),
                )
            }
            .catch { emit(ReaderUiState(viewState = ReaderViewState.DB_ERROR)) }
    }

    val uiState: StateFlow<ReaderUiState> = combine(
        screen,
        observeDisplayPreferences().catch { emit(DisplayPreferences()) },
        snackbar,
    ) { state, prefs, message ->
        state.copy(fontSize = prefs.fontSizeSp, darkMode = prefs.darkMode, snackbar = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ReaderUiState())

    /** Memuat ulang data (tombol "Coba Lagi" di state db_error). */
    fun reload() {
        reloads.value++
    }

    /** Menjadikan [nodeId] checkpoint manual (menimpa yang lama). */
    fun setCheckpoint(nodeId: Long) = launchWrite { actions.setCheckpoint(articleId, nodeId) }

    /** Menyimpan posisi baca terakhir saat Reader ditinggalkan; terpisah dari checkpoint manual. */
    fun saveLastPosition(nodeId: Long) = launchWrite { actions.saveLastPosition(articleId, nodeId) }

    fun setReadMode(mode: ReadMode) = launchWrite {
        actions.setReadMode(articleId, if (mode == ReadMode.PAGED) DomainReadMode.PAGED else DomainReadMode.SEAMLESS)
    }

    /** Mengulang hanya halaman FAILED artikel ini; halaman yang sudah selesai tidak disentuh. */
    fun retryFailed() = launchWrite { actions.retryFailed(articleId).map { } }

    fun setFontSize(sizeSp: Int) = launchWrite { actions.setFontSize(sizeSp) }

    fun setDarkMode(enabled: Boolean) = launchWrite { actions.setDarkMode(enabled) }

    private fun launchWrite(block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            block().onFailure { showSnackbar(R.string.reader_snackbar_save_failed) }
        }
    }

    private fun showSnackbar(@StringRes message: Int) {
        snackbarJob?.cancel()
        snackbar.value = message
        snackbarJob = viewModelScope.launch {
            delay(SNACKBAR_MS)
            snackbar.value = null
        }
    }

    private companion object {
        const val SNACKBAR_MS = 2_600L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
