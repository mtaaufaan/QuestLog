package com.rds.questlog.presentation.addarticle

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rds.questlog.R
import com.rds.questlog.domain.error.QuestLogError.TierError
import com.rds.questlog.domain.error.QuestLogError.ValidationError
import com.rds.questlog.domain.usecase.article.AppendPagesUseCase
import com.rds.questlog.domain.usecase.article.GetArticlesUseCase
import com.rds.questlog.domain.usecase.article.SaveArticleUseCase
import com.rds.questlog.domain.usecase.game.GetGamesUseCase
import com.rds.questlog.domain.usecase.premium.ObservePremiumStatusUseCase
import com.rds.questlog.presentation.articlelist.ArticleUiMapper
import com.rds.questlog.presentation.model.AddArticlePayload
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameUiModel
import com.rds.questlog.presentation.model.ScrapeJob
import com.rds.questlog.presentation.model.ScrapeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AddArticleUiState(
    val games: List<GameUiModel> = emptyList(),
    val articles: List<ArticleUiModel> = emptyList(),
    val isPremium: Boolean = false,
)

/**
 * S2: menyajikan game/artikel/status premium dari use case (untuk autocomplete dan batas tier) dan meneruskan
 * penyimpanan ke [SaveArticleUseCase] / [AppendPagesUseCase]. Validasi dan batas tier ditegakkan di domain; di sini
 * hanya hasilnya dipetakan menjadi pesan.
 */
@HiltViewModel
class AddArticleViewModel @Inject constructor(
    getGames: GetGamesUseCase,
    getArticles: GetArticlesUseCase,
    observePremiumStatus: ObservePremiumStatusUseCase,
    private val saveArticle: SaveArticleUseCase,
    private val appendPages: AppendPagesUseCase,
    private val mapper: ArticleUiMapper,
) : ViewModel() {

    val uiState: StateFlow<AddArticleUiState> = combine(
        getGames().map { list -> list.map(mapper::toUi) }.catch { emit(emptyList()) },
        getArticles("", null).map { list -> list.map(mapper::toUi) }.catch { emit(emptyList()) },
        observePremiumStatus().catch { emit(false) },
    ) { games, articles, isPremium -> AddArticleUiState(games, articles, isPremium) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AddArticleUiState())

    private val _submitError = MutableStateFlow<Int?>(null)

    /** Pesan gagal simpan dari domain (duplikat/limit/database); null bila belum ada. */
    val submitError: StateFlow<Int?> = _submitError

    /** Menyimpan [payload]; [onSaved] dipanggil hanya bila sukses dan scraping sudah dijadwalkan. */
    fun save(payload: AddArticlePayload, onSaved: (ScrapeJob) -> Unit) {
        _submitError.value = null
        viewModelScope.launch {
            val job = when (payload.mode) {
                ScrapeMode.NEW -> saveNew(payload)
                ScrapeMode.APPEND -> saveAppend(payload)
            }
            job.onSuccess(onSaved).onFailure { _submitError.value = it.toMessage() }
        }
    }

    private suspend fun saveNew(payload: AddArticlePayload): Result<ScrapeJob> {
        val name = payload.newGameName
            ?: uiState.value.games.firstOrNull { it.id == payload.gameId }?.name.orEmpty()
        return saveArticle(name, payload.title, payload.urls).map { ScrapeJob(it, skipPages = 0) }
    }

    private suspend fun saveAppend(payload: AddArticlePayload): Result<ScrapeJob> {
        val id = payload.targetArticleId ?: return Result.failure(ValidationError.NoUrlsProvided)
        val existing = uiState.value.articles.firstOrNull { it.id == id }?.totalPageCount ?: 0
        return appendPages(id, payload.urls).map { ScrapeJob(id, skipPages = existing) }
    }

    @StringRes
    private fun Throwable.toMessage(): Int = when (this) {
        TierError.GameLimitReached -> R.string.add_article_limit_game
        TierError.ArticleLimitReached -> R.string.add_article_limit_article
        is ValidationError.DuplicateUrl, is ValidationError.UrlAlreadySaved -> R.string.add_article_submit_duplicate
        else -> R.string.add_article_submit_failed
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
