package com.rds.questlog.presentation.reader

import android.content.res.Resources
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.displaysettings.DEFAULT_FONT_SIZE
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.ContentNodeUi
import com.rds.questlog.presentation.model.PageStatus
import com.rds.questlog.presentation.model.ReadMode
import com.rds.questlog.presentation.model.ReaderViewState
import com.rds.questlog.presentation.preview.ReaderPreviewData
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme
import kotlinx.coroutines.delay

private const val TOAST_MS = 2_400L
private const val FULL = 100

/** Pemicu auto-scroll: [nodeId] tujuan dan pesan snackbar (null = tanpa pesan, mis. saat ganti mode). */
private data class Jump(val nodeId: Long, val messageRes: Int?)

private data class Toast(val text: String, val serial: Int)

/**
 * Layar baca (component-contract.md §3) dengan 5 [viewState]. State baca (mode, halaman, posisi scroll) dipegang di
 * sini; [article]/[content] selalu data terbaru dari database, sedangkan penyimpanan checkpoint, posisi terakhir,
 * mode baca, dan retry dilakukan pemanggil lewat callback. Auto-scroll ke [resumeNodeId] hanya sekali, saat konten
 * pertama kali tampil. [content] = blok per halaman sumber (kosong untuk halaman gagal).
 */
@Composable
fun ReaderContent(
    viewState: ReaderViewState,
    fontSize: Int,
    darkMode: Boolean,
    article: ArticleUiModel,
    content: List<List<ContentNodeUi>>,
    resumeNodeId: Long?,
    resumeFrom: String,
    snackbar: String?,
    onBack: (Long?) -> Unit,
    onLeave: (Long?) -> Unit,
    onOpenSettings: () -> Unit,
    onSetCheckpoint: (Long) -> Unit,
    onModeChange: (ReadMode) -> Unit,
    onRetryFailed: () -> Unit,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QuestLogTheme(darkTheme = darkMode) {
        val success = viewState == ReaderViewState.SUCCESS
        val state = rememberReaderState(article, content, resumeNodeId, resumeFrom, success)
        val currentOnLeave by rememberUpdatedState(onLeave)
        DisposableEffect(Unit) { onDispose { state.lastNodeId?.let { currentOnLeave(it) } } }
        val goBack = { onBack(state.currentNodeId() ?: state.lastNodeId) }
        BackHandler(onBack = goBack)

        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                ReaderHeader(
                    gameName = article.gameName,
                    title = article.title,
                    hasCheckpoint = state.checkpoint != null,
                    actionsEnabled = success,
                    onBack = goBack,
                    onCheckpoint = { state.setCheckpoint(onSetCheckpoint) },
                    onSettings = onOpenSettings,
                )
                ReaderProgress(if (success) state.percent else 0)
                when (viewState) {
                    ReaderViewState.SUCCESS -> SuccessBody(state, fontSize, article, onModeChange, onRetryFailed)
                    ReaderViewState.LOADING -> ReaderLoading()
                    ReaderViewState.DB_ERROR -> ReaderMessage(viewState, onRetryLoad)
                    else -> ReaderMessage(viewState, goBack)
                }
            }
            val message = snackbar ?: state.toast?.text
            if (message != null) {
                ReaderSnackbar(
                    text = message,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = if (state.paged) 80.dp else 24.dp),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SuccessBody(
    state: ReaderState,
    fontSize: Int,
    article: ArticleUiModel,
    onModeChange: (ReadMode) -> Unit,
    onRetryFailed: () -> Unit,
) {
    // Halaman belum selesai = gagal atau sedang diulang; spinner tampil selama ada yang diproses ulang.
    val unfinished = article.pages.count { it.status != PageStatus.DONE }
    val retrying = article.pages.any { it.status == PageStatus.PENDING }
    ReaderModeBar(
        mode = state.mode,
        positionLabel = if (state.paged) {
            stringResource(R.string.reader_position_page, pageNumber(article, state.pageIndex), article.pages.size)
        } else {
            stringResource(R.string.reader_position_percent, state.percent)
        },
        onModeChange = { state.changeMode(it, onModeChange) },
    )
    if (unfinished > 0) PartialBanner(unfinished, retrying, onRetryFailed)
    LazyColumn(
        state = state.listState,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 20.dp,
            bottom = 48.dp,
        ),
    ) {
        itemsIndexed(state.rows, key = { index, row -> row.nodeId?.let { "n$it" } ?: "r$index" }) { _, row ->
            ReaderRowItem(
                row,
                fontSize,
                article.title,
                isCheckpoint = row.nodeId != null && row.nodeId == state.checkpoint,
            )
        }
        if (!state.paged || state.pageIndex >= state.okCount - 1) {
            item(key = "end") {
                Text(
                    text = stringResource(R.string.reader_end),
                    color = QuestLogTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 36.dp),
                    style = TextStyle(fontFamily = Lora, fontSize = 13.sp, fontStyle = FontStyle.Italic),
                )
            }
        }
    }
    if (state.paged) {
        PagedFooter(
            indicator = stringResource(
                R.string.reader_page_indicator,
                pageNumber(article, state.pageIndex),
                article.pages.size,
            ),
            canPrev = state.pageIndex > 0,
            canNext = state.pageIndex < state.okCount - 1,
            onPrev = { state.goToPage(state.pageIndex - 1) },
            onNext = { state.goToPage(state.pageIndex + 1) },
        )
    }
}

/** State dan perilaku scroll Reader; dipisah dari tampilan agar [ReaderContent] tetap ringkas. */
@Stable
private class ReaderState(
    initialArticle: ArticleUiModel,
    initialContent: List<List<ContentNodeUi>>,
    val listState: LazyListState,
    private val res: Resources,
) {
    /** Selalu data terbaru dari database; state baca (mode, halaman, posisi) tetap bertahan saat data berubah. */
    var article by mutableStateOf(initialArticle)
    var content by mutableStateOf(initialContent)
    private var modeOverride by mutableStateOf<ReadMode?>(null)
    var pageIndex by mutableIntStateOf(0)
    var toast by mutableStateOf<Toast?>(null)
    var lastNodeId by mutableStateOf<Long?>(null)
    var jump by mutableStateOf<Jump?>(null)
    private var toastSerial = 0

    val mode get() = modeOverride ?: article.readMode ?: ReadMode.SEAMLESS
    val checkpoint get() = article.checkpointNodeId
    val paged get() = mode == ReadMode.PAGED
    val okCount get() = okPageIndexes(article).size
    val rows get() = readerRows(article, content, paged, pageIndex)

    val percent: Int by derivedStateOf {
        val info = listState.layoutInfo
        when {
            info.totalItemsCount <= 1 || !listState.canScrollForward -> FULL
            else -> (listState.firstVisibleItemIndex * FULL / (info.totalItemsCount - 1)).coerceIn(0, FULL)
        }
    }

    /** Node pertama yang terlihat di layar (posisi baca saat ini). */
    fun currentNodeId(): Long? {
        val rows = rows
        return (listState.firstVisibleItemIndex until rows.size).firstNotNullOfOrNull { rows[it].nodeId }
    }

    fun showToast(@StringRes id: Int, vararg args: Any) {
        toast = Toast(res.getString(id, *args), ++toastSerial)
    }

    fun changeMode(newMode: ReadMode, onModeChange: (ReadMode) -> Unit) {
        if (newMode == mode) return
        val keep = currentNodeId() ?: lastNodeId
        modeOverride = newMode
        onModeChange(newMode)
        keep?.let { jump = Jump(it, null) }
    }

    fun goToPage(index: Int) {
        pageIndex = index.coerceIn(0, (okCount - 1).coerceAtLeast(0))
        listState.requestScrollToItem(0)
    }

    fun setCheckpoint(onSetCheckpoint: (Long) -> Unit) {
        val id = currentNodeId() ?: return
        val message = if (checkpoint != null) {
            R.string.reader_snackbar_checkpoint_updated
        } else {
            R.string.reader_snackbar_checkpoint_saved
        }
        showToast(message)
        onSetCheckpoint(id)
    }

    /** Menjalankan [jump] yang tertunda: pindah halaman bila perlu, scroll ke node, lalu tampilkan pesan. */
    suspend fun applyJump() {
        val target = jump ?: return
        val hit = resolveNode(target.nodeId, article, content)
        when {
            hit == null -> jump = null
            // Ganti halaman dulu; efek dijalankan ulang setelah daftar baris halaman itu tersusun.
            paged && pageIndex != hit.pageIndex -> pageIndex = hit.pageIndex
            else -> {
                val index = rows.indexOfFirst { it.nodeId == hit.nodeId }
                if (index >= 0) listState.scrollToItem(index)
                target.messageRes?.let { id ->
                    val base = res.getString(id)
                    val nearest = hit.nodeId != target.nodeId
                    toast = Toast(
                        if (nearest) res.getString(R.string.reader_snackbar_nearest, base) else base,
                        ++toastSerial,
                    )
                }
                jump = null
            }
        }
    }
}

@Composable
private fun rememberReaderState(
    article: ArticleUiModel,
    content: List<List<ContentNodeUi>>,
    resumeNodeId: Long?,
    resumeFrom: String,
    active: Boolean,
): ReaderState {
    val listState = rememberLazyListState()
    val res = LocalContext.current.resources
    val state = remember(article.id) { ReaderState(article, content, listState, res) }
    SideEffect {
        state.article = article
        state.content = content
    }

    LaunchedEffect(state.toast) {
        if (state.toast != null) {
            delay(TOAST_MS)
            state.toast = null
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.currentNodeId() }.collect { id -> if (id != null) state.lastNodeId = id }
    }
    LaunchedEffect(state, active) {
        val target = resumeNodeId ?: article.lastNodeId
        if (active && target != null) {
            val message = if (resumeFrom == "checkpoint") {
                R.string.reader_snackbar_resume_checkpoint
            } else {
                R.string.reader_snackbar_resume_last
            }
            state.jump = Jump(target, message)
        }
    }
    LaunchedEffect(state.jump, state.mode, state.pageIndex) { state.applyJump() }
    return state
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReaderSuccessPreview() = ReaderPreview(ReaderViewState.SUCCESS, dark = false)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReaderSuccessDarkPreview() = ReaderPreview(ReaderViewState.SUCCESS, dark = true, fontSize = 20)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReaderLoadingPreview() = ReaderPreview(ReaderViewState.LOADING, dark = false)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReaderNotFoundPreview() = ReaderPreview(ReaderViewState.NOT_FOUND, dark = false)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReaderEmptyPreview() = ReaderPreview(ReaderViewState.EMPTY, dark = false)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReaderDbErrorPreview() = ReaderPreview(ReaderViewState.DB_ERROR, dark = true)

@Composable
private fun ReaderPreview(viewState: ReaderViewState, dark: Boolean, fontSize: Int = DEFAULT_FONT_SIZE) {
    ReaderContent(
        viewState = viewState,
        fontSize = fontSize,
        darkMode = dark,
        article = ReaderPreviewData.article,
        content = ReaderPreviewData.content,
        resumeNodeId = null,
        resumeFrom = "last",
        snackbar = null,
        onBack = {},
        onLeave = {},
        onOpenSettings = {},
        onSetCheckpoint = {},
        onModeChange = {},
        onRetryFailed = {},
        onRetryLoad = {},
    )
}
