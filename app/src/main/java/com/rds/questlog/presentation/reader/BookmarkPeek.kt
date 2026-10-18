package com.rds.questlog.presentation.reader

import android.content.res.Resources
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.domain.model.InlineMarkup
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.BookmarkLabelMode
import com.rds.questlog.presentation.model.BookmarkUi
import com.rds.questlog.presentation.model.ContentNodeUi
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.model.ReaderViewState
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SLIDE_MS = 300
private const val TOAST_MS = 2_600L
private const val SNIPPET_LENGTH = 80
private val TOP_GAP = 72.dp
private val SHEET_RADIUS = 16.dp

/** Bookmark yang dipratinjau: artikel pemilik dan node tujuannya. */
data class PeekTarget(val articleId: Long, val nodeId: Long)

/** Penyimpan [PeekTarget]? untuk `rememberSaveable` (kosong = tidak ada pratinjau). */
val PeekTargetSaver = listSaver<PeekTarget?, Long>(
    save = { target -> target?.let { listOf(it.articleId, it.nodeId) }.orEmpty() },
    restore = { saved -> saved.takeIf { it.size == 2 }?.let { PeekTarget(it[0], it[1]) } },
)

/** Artikel Reader di balik pratinjau, dan pengaturan tampilan yang diikuti pratinjau. */
class PeekContext(
    val headArticleId: Long,
    val headTitle: String,
    val premium: PremiumUiState,
    val fontSize: Int,
    val darkMode: Boolean,
)

/** Aksi di P20; semuanya berlaku untuk artikel di P20 (kecuali [openFull] dan [moveHere] yang mengubah head). */
@Suppress("LongParameterList") // satu callback per aksi
class PeekActions(
    val addBookmark: (nodeId: Long, label: String, snippet: String) -> Unit,
    val renameBookmark: (id: Long, label: String) -> Unit,
    val deleteBookmark: (id: Long) -> Unit,
    val setCheckpoint: (nodeId: Long) -> Unit,
    val openFull: () -> Unit,
    val moveHere: () -> Unit,
    val onUnlock: () -> Unit,
)

/** State pratinjau: node popover aktif dan pesan singkat; implementasi [NodeBookmarkState] untuk daftar node. */
@Stable
private class PeekState(
    val listState: LazyListState,
    private val res: Resources,
    private val content: () -> List<List<ContentNodeUi>>,
    private val rows: () -> List<ReaderRow>,
) : NodeBookmarkState {
    override var popoverNodeId by mutableStateOf<Long?>(null)
    var toast by mutableStateOf<String?>(null)

    override fun snippetOf(id: Long): String =
        InlineMarkup.strip(content().asSequence().flatten().firstOrNull { it.id == id }?.text.orEmpty())
            .take(SNIPPET_LENGTH)

    override fun showToast(@StringRes id: Int, vararg args: Any) {
        toast = res.getString(id, *args)
    }

    fun contentPages(): List<List<ContentNodeUi>> = content()

    /** Menggulir daftar ke node [nodeId] (bila ada di daftar). */
    suspend fun scrollToNode(nodeId: Long?) {
        val index = rows().indexOfFirst { it.nodeId != null && it.nodeId == nodeId }
        if (index >= 0) listState.animateScrollToItem(index)
    }
}

/**
 * Pratinjau Bookmark (P20): sheet tinggi di atas Reader yang menampilkan artikel bookmark (Seamless, tersorot di node
 * bookmark) tanpa mengganti artikel Reader maupun posisi bacanya. Semua aksi tulis berlaku untuk artikel di sini.
 * [onClose] (✕, scrim, atau Back) kembali ke Reader di posisi semula.
 */
@Composable
fun BookmarkPeekSheet(
    target: PeekTarget,
    data: PeekData?,
    bookmarks: List<BookmarkUi>,
    context: PeekContext,
    actions: PeekActions,
    onClose: () -> Unit,
) {
    QuestLogTheme(darkTheme = context.darkMode) {
        BackHandler(onBack = onClose)
        val visible = remember { MutableTransitionState(false).apply { targetState = true } }
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(QuestLogTheme.colors.scrim)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onClose()
                    },
            )
            AnimatedVisibility(
                visibleState = visible,
                modifier = Modifier.fillMaxSize().padding(top = TOP_GAP),
                enter = slideInVertically(tween(SLIDE_MS)) { it },
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(topStart = SHEET_RADIUS, topEnd = SHEET_RADIUS),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 8.dp,
                ) {
                    PeekBody(target, data, bookmarks, context, actions, onClose)
                }
            }
        }
    }
}

@Composable
private fun PeekBody(
    target: PeekTarget,
    data: PeekData?,
    bookmarks: List<BookmarkUi>,
    context: PeekContext,
    actions: PeekActions,
    onClose: () -> Unit,
) {
    val article = data?.article
    val content = data?.content.orEmpty()
    val listState = rememberLazyListState()
    val res = LocalContext.current.resources
    val rows = remember(article, content) {
        if (article == null) emptyList() else readerRows(article, content, paged = false, pageIndex = 0)
    }
    val currentContent by rememberUpdatedState(content)
    val currentRows by rememberUpdatedState(rows)
    val state = remember { PeekState(listState, res, { currentContent }, { currentRows }) }
    val own = remember(bookmarks, target.articleId) { bookmarks.filter { it.articleId == target.articleId } }
    val highlight = article?.let { resolveNode(target.nodeId, it, content)?.nodeId }
    var renaming by remember { mutableStateOf<BookmarkUi?>(null) }
    val topNode by remember(rows) {
        derivedStateOf { (listState.firstVisibleItemIndex until rows.size).firstNotNullOfOrNull { rows[it].nodeId } }
    }
    LaunchedEffect(state.toast) {
        if (state.toast != null) {
            delay(TOAST_MS)
            state.toast = null
        }
    }
    LaunchedEffect(target, highlight, rows.isNotEmpty()) {
        val index = rows.indexOfFirst { it.nodeId != null && it.nodeId == highlight }
        if (index >= 0) listState.scrollToItem(index)
    }
    val source = NodeBookmarkSource(
        premium = context.premium,
        recentLabels = bookmarks.sortedByDescending { it.id }.map { it.label }.distinct(),
        bookmarksByNode = own.groupBy { it.nodeId },
        highlightNodeId = highlight,
        callbacks = NodeBookmarkCallbacks(
            onSave = actions.addBookmark,
            onUnlock = actions.onUnlock,
            onRename = { renaming = it },
            onDelete = { actions.deleteBookmark(it.id) },
        ),
    )
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            PeekHeader(target, article, own, context, actions, onClose)
            PeekToolbar(article, context, topNode, state, actions)
            HorizontalDivider(color = QuestLogTheme.colors.divider)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                PeekContent(data, article, rows, source, state, context, onClose)
            }
            Text(
                stringResource(R.string.peek_footer, context.headTitle),
                color = QuestLogTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                style = TextStyle(fontFamily = Lora, fontSize = 12.sp),
            )
        }
        state.toast?.let { ReaderSnackbar(it, Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp)) }
    }
    renaming?.let { bookmark ->
        BookmarkLabelDialog(
            mode = BookmarkLabelMode.EDIT,
            snippet = bookmark.snippet,
            initialLabel = bookmark.label,
            recent = source.recentLabels,
            onSave = { label ->
                actions.renameBookmark(bookmark.id, label)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
}

@Composable
private fun PeekContent(
    data: PeekData?,
    article: ArticleUiModel?,
    rows: List<ReaderRow>,
    source: NodeBookmarkSource,
    state: PeekState,
    context: PeekContext,
    onClose: () -> Unit,
) {
    when {
        data == null || data.viewState == ReaderViewState.LOADING -> ReaderLoading()
        article == null || data.viewState != ReaderViewState.SUCCESS -> ReaderMessage(data.viewState, onClose)
        else -> LazyColumn(
            state = state.listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
        ) {
            itemsIndexed(rows, key = { index, row -> row.nodeId?.let { "n$it" } ?: "r$index" }) { _, row ->
                ReaderRowItem(
                    row,
                    context.fontSize,
                    article.title,
                    isCheckpoint = row.nodeId != null && row.nodeId == article.checkpointNodeId,
                    bookmark = row.nodeId?.let { id -> nodeBookmarkFor(id, source, state) },
                )
            }
        }
    }
}

@Composable
private fun PeekHeader(
    target: PeekTarget,
    article: ArticleUiModel?,
    own: List<BookmarkUi>,
    context: PeekContext,
    actions: PeekActions,
    onClose: () -> Unit,
) {
    val c = QuestLogTheme.colors
    val sameArticle = target.articleId == context.headArticleId
    val bookmark = own.firstOrNull { it.nodeId == target.nodeId }
    val title = article?.title ?: bookmark?.articleTitle.orEmpty()
    val subtitle = stringResource(
        if (sameArticle) R.string.peek_subtitle_same else R.string.peek_subtitle,
        title,
        article?.gameName.orEmpty(),
    )
    Column(Modifier.fillMaxWidth().background(c.hover)) {
        Box(
            Modifier
                .padding(top = 10.dp)
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 4.dp)
                .background(c.textTertiary.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
        )
        Row(
            Modifier.fillMaxWidth().padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val closeDescription = stringResource(R.string.peek_close)
            Box(
                Modifier
                    .size(44.dp)
                    .semantics { contentDescription = closeDescription }
                    .clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    QlIcons.Close,
                    null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(QlIcons.Bookmark, null, tint = c.accentLine, modifier = Modifier.size(11.dp))
                    Text(
                        stringResource(R.string.peek_kicker).uppercase(),
                        color = c.accentText,
                        style = TextStyle(
                            fontFamily = Lora,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                        ),
                    )
                }
                Text(
                    bookmark?.label.orEmpty(),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontFamily = CormorantGaramond,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                    ),
                )
                Text(
                    subtitle,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = Lora, fontSize = 12.sp),
                )
            }
            OutlinedPill(
                stringResource(if (sameArticle) R.string.peek_move_here else R.string.peek_open_full),
                onClick = if (sameArticle) actions.moveHere else actions.openFull,
            )
        }
    }
}

@Composable
private fun OutlinedPill(text: String, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = c.accentText,
            maxLines = 1,
            modifier = Modifier.border(1.dp, c.accentLine, shape).padding(horizontal = 14.dp, vertical = 7.dp),
            style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
        )
    }
}

/** Baris alat: checkpoint (pasang/perbarui, ke checkpoint) dan tambah bookmark di node teratas yang terlihat. */
@Composable
private fun PeekToolbar(
    article: ArticleUiModel?,
    context: PeekContext,
    topNode: Long?,
    state: PeekState,
    actions: PeekActions,
) {
    val c = QuestLogTheme.colors
    val scope = rememberCoroutineScope()
    val hasCheckpoint = article?.checkpointNodeId != null
    val ready = article != null && topNode != null
    val saved = if (hasCheckpoint) {
        R.string.reader_snackbar_checkpoint_updated
    } else {
        R.string.reader_snackbar_checkpoint_saved
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ToolPill(
            icon = QlIcons.Anchor,
            text = stringResource(if (hasCheckpoint) R.string.peek_checkpoint_update else R.string.peek_checkpoint_set),
            tinted = hasCheckpoint,
            enabled = ready,
        ) {
            topNode?.let {
                actions.setCheckpoint(it)
                state.showToast(saved)
            }
        }
        val jumpDescription = stringResource(R.string.reader_jump_checkpoint)
        Box(
            Modifier
                .size(44.dp)
                .alpha(if (hasCheckpoint) 1f else 0.4f)
                .semantics { contentDescription = jumpDescription }
                .clickable(enabled = hasCheckpoint, role = Role.Button) {
                    val checkpoint = article?.checkpointNodeId
                    val node = checkpoint?.let { id -> resolveNode(id, article, state.contentPages())?.nodeId }
                    scope.launch { state.scrollToNode(node) }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(34.dp).border(1.dp, c.divider, CircleShape), contentAlignment = Alignment.Center) {
                Icon(QlIcons.AnchorJump, null, tint = c.accentLine, modifier = Modifier.size(18.dp))
            }
        }
        Box(Modifier.weight(1f))
        ToolPill(
            icon = QlIcons.Bookmark,
            text = stringResource(R.string.peek_add_bookmark),
            tinted = false,
            enabled = ready && context.premium != PremiumUiState.LOST,
            locked = context.premium == PremiumUiState.FREE,
            description = stringResource(R.string.peek_add_bookmark_description),
        ) { state.popoverNodeId = topNode }
    }
}

@Composable
private fun ToolPill(
    icon: ImageVector,
    text: String,
    tinted: Boolean,
    enabled: Boolean,
    locked: Boolean = false,
    description: String? = null,
    onClick: () -> Unit,
) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics { description?.let { contentDescription = it } }
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .height(34.dp)
                .background(if (tinted) c.accentTint else Color.Transparent, shape)
                .border(1.dp, c.accentLine, shape)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, null, tint = c.accentText, modifier = Modifier.size(15.dp))
            Text(
                text,
                color = c.accentText,
                style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
            )
            if (locked) Icon(QlIcons.Lock, null, tint = c.accentText, modifier = Modifier.size(11.dp))
        }
    }
}
