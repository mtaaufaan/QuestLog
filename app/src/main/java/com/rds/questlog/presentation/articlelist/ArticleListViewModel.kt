package com.rds.questlog.presentation.articlelist

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rds.questlog.R
import com.rds.questlog.domain.error.QuestLogError.ArticleError
import com.rds.questlog.domain.usecase.article.DeleteArticleUseCase
import com.rds.questlog.domain.usecase.article.DeletePageUseCase
import com.rds.questlog.domain.usecase.article.GetArticlesUseCase
import com.rds.questlog.domain.usecase.article.RefreshArticleUseCase
import com.rds.questlog.domain.usecase.article.RefreshPageUseCase
import com.rds.questlog.domain.usecase.article.ReorderPagesUseCase
import com.rds.questlog.domain.usecase.article.RetryFailedPagesUseCase
import com.rds.questlog.domain.usecase.article.UpdateArticleUseCase
import com.rds.questlog.domain.usecase.game.GetGamesUseCase
import com.rds.questlog.domain.usecase.premium.ObservePremiumStatusUseCase
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.ScrapeJob
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

/**
 * S1: hanya mengamati Flow dari use case dan meneruskan aksi pengguna; tidak menyentuh Room/OkHttp.
 * Search dan filter game dikirim sebagai parameter query sehingga disaring database, bukan di memori.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
@Suppress("LongParameterList") // satu use case per aksi S1; digabung hanya menambah lapisan
class ArticleListViewModel @Inject constructor(
    getArticles: GetArticlesUseCase,
    getGames: GetGamesUseCase,
    observePremiumStatus: ObservePremiumStatusUseCase,
    private val deleteArticleUseCase: DeleteArticleUseCase,
    private val retryFailedPagesUseCase: RetryFailedPagesUseCase,
    private val updateArticleUseCase: UpdateArticleUseCase,
    private val reorderPagesUseCase: ReorderPagesUseCase,
    private val deletePageUseCase: DeletePageUseCase,
    private val refreshArticleUseCase: RefreshArticleUseCase,
    private val refreshPageUseCase: RefreshPageUseCase,
    private val mapper: ArticleUiMapper,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val gameFilter = MutableStateFlow<Long?>(null)
    private val snackbar = MutableStateFlow<SnackbarMessage?>(null)
    private val edit = MutableStateFlow(EditState())
    private var snackbarJob: Job? = null

    private val articles: Flow<List<ArticleUiModel>> = combine(query, gameFilter) { q, game -> q to game }
        .flatMapLatest { (q, game) ->
            getArticles(q, game)
                .map { list -> list.map(mapper::toUi) }
                .catch {
                    showSnackbar(R.string.article_list_snackbar_read_failed)
                    emit(emptyList())
                }
        }

    val uiState: StateFlow<ArticleListUiState> = combine(
        query,
        gameFilter,
        articles,
        getGames().map { list -> list.map(mapper::toUi) }.catch { emit(emptyList()) },
        observePremiumStatus().catch { emit(false) },
    ) { q, game, articleList, games, isPremium ->
        ArticleListUiState(
            games = games,
            articles = articleList,
            selectedGameFilter = game,
            query = q,
            isPremium = isPremium,
        )
    }.combine(snackbar) { state, message -> state.copy(snackbar = message?.res, snackbarArg = message?.arg) }
        .combine(edit) { state, e -> state.copy(isSavingEdit = e.saving, editSaveError = e.error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ArticleListUiState(isLoading = true))

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun onSelectFilter(gameId: Long?) {
        gameFilter.value = gameId
    }

    init {
        // Game yang jadi kosong (artikelnya dipindah) ikut hilang; filter yang menunjuknya kembali ke "Semua Game".
        viewModelScope.launch {
            getGames().catch { emit(emptyList()) }.collect { games ->
                val selected = gameFilter.value
                if (selected != null && games.none { it.id == selected }) gameFilter.value = null
            }
        }
    }

    fun onScrapingArticleTapped() = showSnackbar(R.string.article_list_snackbar_scraping)

    /**
     * Menyimpan hasil Edit Article Sheet. [onSaved] dipanggil hanya bila sukses (pemanggil menutup popup);
     * bila gagal sheet tetap terbuka dengan pesan error.
     */
    fun updateArticle(articleId: Long, payload: EditArticlePayload, onSaved: () -> Unit) {
        if (edit.value.saving) return
        edit.value = EditState(saving = true)
        viewModelScope.launch {
            updateArticleUseCase(articleId, payload.title, payload.gameId, payload.newGameName)
                .onSuccess {
                    edit.value = EditState()
                    showSnackbar(R.string.article_list_snackbar_updated)
                    onSaved()
                }
                .onFailure { edit.value = EditState(error = true) }
        }
    }

    /** Menggeser halaman ke [index] + [dir] (−1 naik, +1 turun); langsung tersimpan, Reader ikut berubah. */
    fun movePage(article: ArticleUiModel, index: Int, dir: Int) {
        val ids = article.pages.map { it.id }.toMutableList()
        if (index !in ids.indices || index + dir !in ids.indices) return
        ids.add(index + dir, ids.removeAt(index))
        viewModelScope.launch {
            reorderPagesUseCase(article.id, ids)
                .onFailure { showSnackbar(R.string.article_list_snackbar_reorder_failed) }
        }
    }

    /** Menghapus halaman [pageId] setelah konfirmasi; pesan menyesuaikan penyebab bila ditolak. */
    fun deletePage(articleId: Long, pageId: Long) {
        viewModelScope.launch {
            deletePageUseCase(articleId, pageId)
                .onSuccess { showSnackbar(R.string.article_list_snackbar_page_deleted) }
                .onFailure { showSnackbar(it.toPageDeleteMessage()) }
        }
    }

    /** Mengunduh ulang semua halaman [article]; [onStarted] menerima pekerjaan untuk notifikasi progres. */
    fun refreshArticle(article: ArticleUiModel, onStarted: (ScrapeJob) -> Unit) {
        viewModelScope.launch {
            refreshArticleUseCase(article.id)
                .onSuccess {
                    showSnackbar(R.string.article_list_snackbar_refresh_all)
                    onStarted(ScrapeJob(article.id, skipPages = 0, pageIds = article.pages.map { it.id }.toSet()))
                }
                .onFailure { showSnackbar(it.toRefreshMessage()) }
        }
    }

    /** Mengunduh ulang halaman ke-[index] (tombol ⟳ di Page Manager). */
    fun refreshPage(article: ArticleUiModel, index: Int, onStarted: (ScrapeJob) -> Unit) {
        val page = article.pages.getOrNull(index) ?: return
        viewModelScope.launch {
            refreshPageUseCase(article.id, page.id)
                .onSuccess {
                    showSnackbar(R.string.article_list_snackbar_refresh_page, index + 1)
                    onStarted(ScrapeJob(article.id, skipPages = 0, pageIds = setOf(page.id)))
                }
                .onFailure { showSnackbar(it.toRefreshMessage()) }
        }
    }

    /** Sheet ditutup tanpa menyimpan: hapus sisa status error. */
    fun onEditDismissed() {
        edit.value = EditState()
    }

    fun deleteArticle(articleId: Long) {
        viewModelScope.launch {
            deleteArticleUseCase(articleId)
                .onSuccess { showSnackbar(R.string.article_list_snackbar_deleted) }
                .onFailure { showSnackbar(R.string.article_list_snackbar_delete_failed) }
        }
    }

    fun retryFailedPages(articleId: Long) {
        viewModelScope.launch {
            retryFailedPagesUseCase(articleId)
                .onSuccess { showSnackbar(R.string.article_list_snackbar_retrying) }
                .onFailure { showSnackbar(R.string.article_list_snackbar_retry_failed) }
        }
    }

    private fun showSnackbar(@StringRes message: Int, arg: Int? = null) {
        snackbarJob?.cancel()
        snackbar.value = SnackbarMessage(message, arg)
        snackbarJob = viewModelScope.launch {
            delay(SNACKBAR_DURATION_MS)
            snackbar.value = null
        }
    }

    @StringRes
    private fun Throwable.toRefreshMessage(): Int = when (this) {
        ArticleError.Busy -> R.string.article_list_snackbar_scraping
        else -> R.string.article_list_snackbar_refresh_failed
    }

    @StringRes
    private fun Throwable.toPageDeleteMessage(): Int = when (this) {
        ArticleError.LastPage -> R.string.article_list_snackbar_last_page
        ArticleError.Busy -> R.string.article_list_snackbar_page_busy
        else -> R.string.article_list_snackbar_page_delete_failed
    }

    private data class SnackbarMessage(@StringRes val res: Int, val arg: Int? = null)

    private data class EditState(val saving: Boolean = false, val error: Boolean = false)

    private companion object {
        const val SNACKBAR_DURATION_MS = 2_600L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
