package com.rds.questlog.presentation.reader

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rds.questlog.R
import com.rds.questlog.domain.error.QuestLogError.PremiumError
import com.rds.questlog.domain.model.Bookmark
import com.rds.questlog.domain.model.DisplayPreferences
import com.rds.questlog.domain.model.ReadMode as DomainReadMode
import com.rds.questlog.domain.usecase.article.RetryFailedPagesUseCase
import com.rds.questlog.domain.usecase.binder.SetMainArticleUseCase
import com.rds.questlog.domain.usecase.bookmark.AddBookmarkUseCase
import com.rds.questlog.domain.usecase.bookmark.DeleteBookmarkUseCase
import com.rds.questlog.domain.usecase.bookmark.ObserveGameBookmarksUseCase
import com.rds.questlog.domain.usecase.bookmark.UpdateBookmarkLabelUseCase
import com.rds.questlog.domain.usecase.note.DeleteNoteUseCase
import com.rds.questlog.domain.usecase.note.ObserveNoteUseCase
import com.rds.questlog.domain.usecase.note.SaveNoteUseCase
import com.rds.questlog.domain.usecase.prefs.ConsumeReaderTipUseCase
import com.rds.questlog.domain.usecase.prefs.ObserveDisplayPreferencesUseCase
import com.rds.questlog.domain.usecase.prefs.SetDarkModeUseCase
import com.rds.questlog.domain.usecase.prefs.SetFontSizeUseCase
import com.rds.questlog.domain.usecase.premium.PremiumGuard
import com.rds.questlog.domain.usecase.reader.GetArticleDetailUseCase
import com.rds.questlog.domain.usecase.reader.SaveLastPositionUseCase
import com.rds.questlog.domain.usecase.reader.SetCheckpointUseCase
import com.rds.questlog.domain.usecase.reader.SetReadModeUseCase
import com.rds.questlog.presentation.articlelist.ArticleUiMapper
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.BookmarkUi
import com.rds.questlog.presentation.model.ContentNodeUi
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.model.ReadMode
import com.rds.questlog.presentation.model.ReaderViewState
import com.rds.questlog.presentation.navigation.Reader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Isi artikel untuk Pratinjau Bookmark (P20): [article] null = belum ada / tidak ditemukan. */
data class PeekData(
    val article: ArticleUiModel?,
    val content: List<List<ContentNodeUi>>,
    val viewState: ReaderViewState,
)

/** Use case aksi tulis Reader dikelompokkan agar konstruktor ViewModel tetap ringkas. */
class ReaderActions @Inject constructor(
    val setCheckpoint: SetCheckpointUseCase,
    val saveLastPosition: SaveLastPositionUseCase,
    val setReadMode: SetReadModeUseCase,
    val retryFailed: RetryFailedPagesUseCase,
    val setFontSize: SetFontSizeUseCase,
    val setDarkMode: SetDarkModeUseCase,
)

/** Use case bookmark Reader. */
class BookmarkActions @Inject constructor(
    val observe: ObserveGameBookmarksUseCase,
    val add: AddBookmarkUseCase,
    val rename: UpdateBookmarkLabelUseCase,
    val delete: DeleteBookmarkUseCase,
)

/** Use case catatan Reader. */
class NoteActions @Inject constructor(
    val observe: ObserveNoteUseCase,
    val save: SaveNoteUseCase,
    val delete: DeleteNoteUseCase,
)

/** Alat baca premium: bookmark, catatan, dan status premium untuk mengunci UI. */
class ReaderToolsActions @Inject constructor(
    val bookmarks: BookmarkActions,
    val notes: NoteActions,
    val premium: PremiumGuard,
    val mainArticle: SetMainArticleUseCase,
    val readerTip: ConsumeReaderTipUseCase,
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
    val premium: PremiumUiState = PremiumUiState.FREE,
)

/**
 * S3: menyajikan artikel + konten dari database dan meneruskan aksi pengguna ke use case (checkpoint, posisi baca,
 * mode baca, retry, preferensi tampilan). Tidak menyentuh Room/DataStore langsung.
 */
@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getArticleDetail: GetArticleDetailUseCase,
    observeDisplayPreferences: ObserveDisplayPreferencesUseCase,
    private val actions: ReaderActions,
    private val tools: ReaderToolsActions,
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
                    resumeNodeId = detail?.resumeTarget(resumeFrom, route.startNodeId),
                )
            }
            .catch { emit(ReaderUiState(viewState = ReaderViewState.DB_ERROR)) }
    }

    private val premium: Flow<PremiumUiState> = tools.premium.state()
        .map { PremiumUiState.valueOf(it.name) }
        .catch { emit(PremiumUiState.FREE) }

    val uiState: StateFlow<ReaderUiState> = combine(
        screen,
        observeDisplayPreferences().catch { emit(DisplayPreferences()) },
        snackbar,
        premium,
    ) { state, prefs, message, premiumState ->
        state.copy(fontSize = prefs.fontSizeSp, darkMode = prefs.darkMode, snackbar = message, premium = premiumState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ReaderUiState())

    /** Bookmark semua artikel di game artikel ini (dikelompokkan per artikel oleh UI); selalu terbaca. */
    val bookmarks: StateFlow<List<BookmarkUi>> = uiState.map { it.article?.gameId }.distinctUntilChanged()
        .flatMapLatest { gameId ->
            if (gameId == null) {
                flowOf(emptyList())
            } else {
                tools.bookmarks.observe(gameId).map { list -> list.map(::toUi) }.catch { emit(emptyList()) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** Isi catatan artikel; null = belum dimuat (panel baru dibuka setelah terisi agar tidak menimpa catatan). */
    val note: StateFlow<String?> = tools.notes.observe(articleId).map<String, String?> { it }
        .catch { emit("") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val unlockEvents = Channel<Unit>(Channel.CONFLATED)

    /** Pengguna gratis menulis lewat UseCase: UI membuka Unlock Sheet (data tidak berubah). */
    val unlockRequests: Flow<Unit> = unlockEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            delay(TIP_DELAY_MS)
            if (tools.readerTip()) showSnackbar(R.string.reader_tip_bookmark)
        }
    }

    /** Memuat ulang data (tombol "Coba Lagi" di state db_error). */
    fun reload() {
        reloads.value++
    }

    /** Menjadikan [nodeId] checkpoint manual (menimpa yang lama); [forArticle] = artikel P20 bila beda. */
    fun setCheckpoint(nodeId: Long, forArticle: Long = articleId) =
        launchWrite { actions.setCheckpoint(forArticle, nodeId) }

    /** Menyimpan posisi baca terakhir saat Reader ditinggalkan; terpisah dari checkpoint manual. */
    fun saveLastPosition(nodeId: Long) = launchWrite { actions.saveLastPosition(articleId, nodeId) }

    fun setReadMode(mode: ReadMode) = launchWrite {
        actions.setReadMode(articleId, if (mode == ReadMode.PAGED) DomainReadMode.PAGED else DomainReadMode.SEAMLESS)
    }

    /** Mengulang hanya halaman FAILED artikel ini; halaman yang sudah selesai tidak disentuh. */
    fun retryFailed() = launchWrite { actions.retryFailed(articleId).map { } }

    /** Menambah bookmark di [nodeId]; [forArticle] = artikel Pratinjau Bookmark bila beda dari artikel Reader. */
    fun addBookmark(nodeId: Long, label: String, snippet: String, forArticle: Long = articleId) =
        launchWrite { tools.bookmarks.add(forArticle, nodeId, label, snippet).map { } }

    /** Isi artikel [peekArticleId] untuk Pratinjau Bookmark (P20); tidak menyentuh state atau posisi baca Reader. */
    fun observePeek(peekArticleId: Long): Flow<PeekData> = getArticleDetail(peekArticleId)
        .map { detail ->
            val content = detail?.toContentPages().orEmpty()
            PeekData(detail?.let { mapper.toUi(it.article).withContent(content) }, content, detail.toViewState())
        }
        .catch { emit(PeekData(null, emptyList(), ReaderViewState.DB_ERROR)) }

    fun renameBookmark(id: Long, label: String) = launchWrite { tools.bookmarks.rename(id, label) }

    fun deleteBookmark(id: Long) = launchWrite { tools.bookmarks.delete(id) }

    /** Menyimpan catatan (autosave); true bila tersimpan, supaya panel baru menampilkan "Tersimpan" setelahnya. */
    suspend fun saveNote(content: String): Boolean = tools.notes.save(articleId, content).onFailure(::report).isSuccess

    /** Menyimpan sisa ketikan saat panel catatan ditutup. */
    fun flushNote(content: String) {
        viewModelScope.launch { saveNote(content) }
    }

    /** Menjadikan artikel ini artikel utama game-nya (gratis); yang lama tertimpa. */
    fun setMainArticle() {
        viewModelScope.launch {
            tools.mainArticle(articleId)
                .onSuccess { showSnackbar(R.string.reader_snackbar_main_set) }
                .onFailure(::report)
        }
    }

    fun deleteNote() = launchWrite { tools.notes.delete(articleId) }

    fun setFontSize(sizeSp: Int) = launchWrite { actions.setFontSize(sizeSp) }

    fun setDarkMode(enabled: Boolean) = launchWrite { actions.setDarkMode(enabled) }

    private fun launchWrite(block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            block().onFailure(::report)
        }
    }

    /** Gratis -> Unlock Sheet; premium hilang -> pesan hanya-baca; selain itu gagal simpan biasa. */
    private fun report(error: Throwable) {
        when (error) {
            is PremiumError.Locked -> unlockEvents.trySend(Unit)
            is PremiumError.ReadOnly -> showSnackbar(R.string.reader_snackbar_read_only)
            else -> showSnackbar(R.string.reader_snackbar_save_failed)
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
        const val TIP_DELAY_MS = 1_800L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private fun toUi(bookmark: Bookmark) = BookmarkUi(
    id = bookmark.id,
    articleId = bookmark.articleId,
    articleTitle = bookmark.articleTitle,
    label = bookmark.label,
    snippet = bookmark.snippet,
    nodeId = bookmark.nodeId,
)
