package com.rds.questlog.presentation.addarticle

import com.rds.questlog.presentation.model.AddArticlePayload
import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameUiModel
import com.rds.questlog.presentation.model.ScrapeMode

private const val FREE_GAME_LIMIT = 2
private const val FREE_ARTICLES_PER_GAME = 5
private val WHITESPACE = Regex("\\s+")
private val VALID_URL = Regex("^https://\\S+\\.\\S+")

/** Merapikan nama game baru: spasi dirapikan dan huruf pertama tiap kata dikapitalkan. */
fun normalizeGameName(raw: String): String = raw.trim().replace(
    WHITESPACE,
    " ",
).split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

/** Game untuk dropdown autocomplete: namanya memuat [input], atau diawali kata pertamanya; input kosong = semua. */
fun matchingGames(games: List<GameUiModel>, input: String): List<GameUiModel> {
    val query = input.trim().lowercase()
    if (query.isEmpty()) return games
    val firstWord = query.substringBefore(' ')
    return games.filter { it.name.lowercase().let { name -> name.contains(query) || name.startsWith(firstWord) } }
}

fun exactGame(games: List<GameUiModel>, input: String): GameUiModel? =
    games.firstOrNull { it.name.equals(normalizeGameName(input), ignoreCase = true) }

/** Batas tier gratis: maks. 2 game. */
fun canAddGame(games: List<GameUiModel>, isPremium: Boolean): Boolean = isPremium || games.size < FREE_GAME_LIMIT

/** Batas tier gratis: maks. 5 artikel per game; hanya relevan di mode "Buat artikel baru". */
fun articleLimitReached(form: AddArticleForm, articles: List<ArticleUiModel>, isPremium: Boolean): Boolean =
    form.mode == ScrapeMode.NEW && !isPremium && form.gameId != null &&
        articles.count { it.gameId == form.gameId } >= FREE_ARTICLES_PER_GAME

fun gameLimitReached(form: AddArticleForm, games: List<GameUiModel>, isPremium: Boolean): Boolean =
    form.mode == ScrapeMode.NEW && form.newGame && !canAddGame(games, isPremium)

/** Artikel yang bisa dilengkapi: hanya yang READY. */
fun appendTargets(articles: List<ArticleUiModel>): List<ArticleUiModel> =
    articles.filter { it.status == ArticleStatus.READY }

fun targetArticle(form: AddArticleForm, articles: List<ArticleUiModel>): ArticleUiModel? =
    if (form.mode == ScrapeMode.APPEND) articles.firstOrNull { it.id == form.targetId } else null

/** Nomor halaman pertama untuk URL baru: mode Lengkapi melanjutkan setelah halaman artikel target (append-only). */
fun urlPageOffset(target: ArticleUiModel?): Int = target?.pages?.size ?: 0

sealed interface UrlError {
    data object NoUrls : UrlError
    data object Empty : UrlError
    data object InvalidFormat : UrlError
    data class DuplicateOf(val page: Int) : UrlError
    data class ExistsInArticle(val page: Int) : UrlError

    /** Sudah tersimpan di artikel lain. */
    data object StoredElsewhere : UrlError
}

/** Konteks pembanding URL: halaman artikel target ([existing]), semua URL tersimpan ([stored]), dan nomor awal. */
class UrlScope(val existing: List<String> = emptyList(), val stored: Set<String> = emptySet(), val pageOffset: Int = 0)

/** Semua URL halaman sumber yang sudah tersimpan di artikel mana pun. */
fun storedUrls(articles: List<ArticleUiModel>): Set<String> = articles.flatMap { a -> a.pages.map { it.url } }.toSet()

fun urlScope(form: AddArticleForm, articles: List<ArticleUiModel>): UrlScope {
    val target = targetArticle(form, articles)
    return UrlScope(target?.pages?.map { it.url }.orEmpty(), storedUrls(articles), urlPageOffset(target))
}

/**
 * Error per baris URL (kunci = indeks baris). Format dan duplikat dicek langsung saat mengetik; "tidak boleh kosong"
 * dan "minimal 1 URL" hanya bila [forSave]. URL yang sudah tersimpan (artikel target atau lain) juga ditolak.
 */
fun urlErrors(urls: List<String>, scope: UrlScope, forSave: Boolean): Map<Int, UrlError> {
    val trimmed = urls.map { it.trim() }
    val errors = mutableMapOf<Int, UrlError>()
    if (forSave && trimmed.none { it.isNotEmpty() }) errors[0] = UrlError.NoUrls
    trimmed.forEachIndexed { index, url ->
        errors.putIfAbsent(
            index,
            urlErrorFor(index, trimmed, scope, forSave) ?: return@forEachIndexed,
        )
    }
    return errors
}

private fun urlErrorFor(index: Int, all: List<String>, scope: UrlScope, forSave: Boolean): UrlError? {
    val url = all[index]
    return when {
        url.isEmpty() -> if (forSave && all.any { it.isNotEmpty() }) UrlError.Empty else null
        !VALID_URL.matches(url) -> UrlError.InvalidFormat
        all.indexOf(url) < index -> UrlError.DuplicateOf(scope.pageOffset + all.indexOf(url) + 1)
        url in scope.existing -> UrlError.ExistsInArticle(scope.existing.indexOf(url) + 1)
        url in scope.stored -> UrlError.StoredElsewhere
        else -> null
    }
}

enum class GameError { EMPTY, NOT_SELECTED, TOO_LONG }

data class FormErrors(
    val game: GameError? = null,
    val title: Boolean = false,
    val target: Boolean = false,
    val urls: Map<Int, UrlError> = emptyMap(),
) {
    val isValid: Boolean get() = game == null && !title && !target && urls.isEmpty()
}

/** Validasi saat tombol simpan ditekan (aturan dari `AddArticle.dc.html`). */
fun validate(form: AddArticleForm, articles: List<ArticleUiModel>): FormErrors {
    val target = targetArticle(form, articles)
    val isNew = form.mode == ScrapeMode.NEW
    val game = when {
        !isNew -> null
        form.gameInput.length > MAX_GAME_NAME -> GameError.TOO_LONG
        form.gameId == null && !form.newGame ->
            if (form.gameInput.isNotBlank()) GameError.NOT_SELECTED else GameError.EMPTY
        else -> null
    }
    return FormErrors(
        game = game,
        title = isNew && form.title.isBlank(),
        target = !isNew && target == null,
        urls = urlErrors(form.urls, urlScope(form, articles), forSave = true),
    )
}

fun buildPayload(form: AddArticleForm, articles: List<ArticleUiModel>) = AddArticlePayload(
    mode = form.mode,
    gameId = form.gameId,
    newGameName = if (form.newGame) normalizeGameName(form.gameInput) else null,
    title = form.title.trim(),
    urls = form.urls.map { it.trim() },
    targetArticleId = targetArticle(form, articles)?.id,
)
