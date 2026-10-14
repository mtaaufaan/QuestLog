package com.rds.questlog.presentation.binder

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rds.questlog.R
import com.rds.questlog.domain.error.QuestLogError.PremiumError
import com.rds.questlog.domain.error.QuestLogError.ValidationError
import com.rds.questlog.domain.model.Binder
import com.rds.questlog.domain.usecase.article.GetArticlesUseCase
import com.rds.questlog.domain.usecase.binder.CreateCategoryUseCase
import com.rds.questlog.domain.usecase.binder.DeleteCategoryUseCase
import com.rds.questlog.domain.usecase.binder.ObserveBinderUseCase
import com.rds.questlog.domain.usecase.binder.RenameCategoryUseCase
import com.rds.questlog.domain.usecase.binder.SetArticleCategoryUseCase
import com.rds.questlog.domain.usecase.game.GetGamesUseCase
import com.rds.questlog.domain.usecase.premium.PremiumGuard
import com.rds.questlog.presentation.articlelist.ArticleUiMapper
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.navigation.Binder as BinderRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Use case Game Binder (S4) dikelompokkan agar konstruktor ViewModel tetap ringkas. */
class BinderUseCases @Inject constructor(
    val observe: ObserveBinderUseCase,
    val create: CreateCategoryUseCase,
    val rename: RenameCategoryUseCase,
    val delete: DeleteCategoryUseCase,
    val setMember: SetArticleCategoryUseCase,
)

/** Pesan singkat di S4; [arg] mengisi placeholder teks (mis. nama kategori yang duplikat). */
data class BinderMessage(@StringRes val res: Int, val arg: String? = null)

data class BinderUiState(
    val gameName: String = "",
    val articles: List<ArticleUiModel> = emptyList(),
    val categories: List<BinderCategoryUi> = emptyList(),
    /** id artikel -> id kategori. */
    val assignments: Map<Long, Set<Long>> = emptyMap(),
    val premium: PremiumUiState = PremiumUiState.FREE,
    val message: BinderMessage? = null,
)

/**
 * S4 Game Binder: artikel, kategori, dan penetapannya dari use case; semua penulisan lewat [PremiumGuard] di domain.
 * Gratis/premium hilang ditolak (pesan hanya-baca), data tidak berubah.
 */
@HiltViewModel
class BinderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getArticles: GetArticlesUseCase,
    getGames: GetGamesUseCase,
    premiumGuard: PremiumGuard,
    private val binder: BinderUseCases,
    private val mapper: ArticleUiMapper,
) : ViewModel() {

    val gameId: Long = savedStateHandle.toRoute<BinderRoute>().gameId

    private val message = MutableStateFlow<BinderMessage?>(null)
    private var messageJob: Job? = null

    private val content = combine(
        getGames().map { games -> games.firstOrNull { it.id == gameId }?.name.orEmpty() }.catch { emit("") },
        getArticles("", gameId).map { list -> list.map(mapper::toUi) }.catch { emit(emptyList()) },
        binder.observe(gameId).catch { emit(Binder(emptyList(), emptyMap())) },
    ) { name, articles, data -> Triple(name, articles, data) }

    val uiState: StateFlow<BinderUiState> = combine(
        content,
        premiumGuard.state().map { PremiumUiState.valueOf(it.name) }.catch { emit(PremiumUiState.FREE) },
        message,
    ) { (name, articles, data), premium, current ->
        BinderUiState(
            gameName = name,
            articles = articles,
            categories = data.categories.map { BinderCategoryUi(it.id, it.gameId, it.name) },
            assignments = data.assignments,
            premium = premium,
            message = current,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BinderUiState())

    fun createCategory(name: String) = launchWrite { binder.create(gameId, name).map { } }

    fun renameCategory(id: Long, name: String) = launchWrite { binder.rename(id, name) }

    /** Menghapus kategori; artikelnya tetap ada ("Kategori dihapus — artikel tetap ada"). */
    fun deleteCategory(id: Long) = launchWrite(onSuccess = R.string.binder_deleted) { binder.delete(id) }

    fun setMember(articleId: Long, categoryId: Long, member: Boolean) =
        launchWrite { binder.setMember(articleId, categoryId, member) }

    /** Umpan balik singkat (mis. artikel masih diproses). */
    fun showMessage(@StringRes res: Int, arg: String? = null) {
        messageJob?.cancel()
        message.value = BinderMessage(res, arg)
        messageJob = viewModelScope.launch {
            delay(MESSAGE_MS)
            message.value = null
        }
    }

    private fun launchWrite(@StringRes onSuccess: Int? = null, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            block()
                .onSuccess { onSuccess?.let { showMessage(it) } }
                .onFailure { error ->
                    when (error) {
                        is ValidationError.DuplicateCategory -> showMessage(
                            R.string.category_name_duplicate,
                            error.name,
                        )
                        is PremiumError -> showMessage(R.string.reader_snackbar_read_only)
                        else -> showMessage(R.string.reader_snackbar_save_failed)
                    }
                }
        }
    }

    private companion object {
        const val MESSAGE_MS = 2_600L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
