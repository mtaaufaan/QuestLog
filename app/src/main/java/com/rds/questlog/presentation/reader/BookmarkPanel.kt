package com.rds.questlog.presentation.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlBottomSheet
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.model.BookmarkUi
import com.rds.questlog.presentation.preview.ReaderPreviewData
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme

private const val SHEET_HEIGHT = 0.86f
private const val DISABLED_ALPHA = 0.35f

/**
 * Bookmark Panel (P12): semua bookmark di game ini dikelompokkan per artikel, artikel yang sedang dibaca di atas.
 * [readOnly] (premium tidak aktif) menonaktifkan tambah/ubah/hapus; mengetuk baris untuk lompat tetap bisa.
 */
@Composable
fun BookmarkPanel(
    gameName: String,
    bookmarks: List<BookmarkUi>,
    currentArticleId: Long,
    readOnly: Boolean,
    onAddHere: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onJump: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val c = QuestLogTheme.colors
    val groups = bookmarks.groupBy { it.articleId }.entries
        .sortedByDescending { it.key == currentArticleId }
    QlBottomSheet(onDismiss = onClose) {
        Column(Modifier.fillMaxHeight(SHEET_HEIGHT).navigationBarsPadding()) {
            PanelHeader(gameName, bookmarks.size, readOnly, onAddHere)
            if (readOnly) ReadOnlyBanner()
            if (bookmarks.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    groups.forEach { (articleId, items) ->
                        item(key = "g$articleId") {
                            GroupTitle(items.first().articleTitle, articleId == currentArticleId)
                        }
                        items(items.size, key = { items[it].id }) { index ->
                            BookmarkRow(items[index], readOnly, onEdit, onDelete, onJump)
                            HorizontalDivider(Modifier.padding(start = 20.dp), color = c.divider)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(gameName: String, count: Int, readOnly: Boolean, onAddHere: () -> Unit) {
    val c = QuestLogTheme.colors
    Column {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.bookmark_panel_kicker, gameName).uppercase(),
                    color = c.accentText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontFamily = Lora,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.26.sp,
                    ),
                )
                Text(
                    text = stringResource(R.string.bookmark_panel_count, count),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.Medium, fontSize = 24.sp),
                )
            }
            val shape = RoundedCornerShape(4.dp)
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .alpha(if (readOnly) DISABLED_ALPHA else 1f)
                    .clip(shape)
                    .border(1.dp, c.accentLine, shape)
                    .clickable(enabled = !readOnly, role = Role.Button, onClick = onAddHere)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(QlIcons.Plus, contentDescription = null, tint = c.accentText, modifier = Modifier.size(15.dp))
                Text(
                    stringResource(R.string.bookmark_here),
                    color = c.accentText,
                    style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ReadOnlyBanner() {
    val c = QuestLogTheme.colors
    Row(
        Modifier.fillMaxWidth().background(c.accentTint).padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(QlIcons.Lock, contentDescription = null, tint = c.accentText, modifier = Modifier.size(14.dp))
        Text(
            stringResource(R.string.bookmark_read_only),
            color = MaterialTheme.colorScheme.onSurface,
            style = TextStyle(fontFamily = Lora, fontSize = 12.5.sp, lineHeight = 18.sp),
        )
    }
}

@Composable
private fun GroupTitle(title: String, reading: Boolean) {
    val c = QuestLogTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
            style = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        )
        if (reading) {
            Text(
                stringResource(R.string.bookmark_reading).uppercase(),
                color = c.accentText,
                style = TextStyle(
                    fontFamily = Lora,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                ),
            )
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: BookmarkUi,
    readOnly: Boolean,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onJump: (Long) -> Unit,
) {
    val c = QuestLogTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(
            role = Role.Button,
        ) { onJump(bookmark.id) }.padding(start = 20.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(QlIcons.BookmarkGold, contentDescription = null, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                bookmark.label,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
            )
            if (bookmark.snippet.isNotEmpty()) {
                Text(
                    bookmark.snippet,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = Lora, fontStyle = FontStyle.Italic, fontSize = 12.5.sp),
                )
            }
        }
        RowAction(QlIcons.Pencil, stringResource(R.string.bookmark_edit), c.textSecondary, !readOnly) {
            onEdit(bookmark.id)
        }
        RowAction(QlIcons.Trash, stringResource(R.string.bookmark_delete), c.danger, !readOnly) {
            onDelete(bookmark.id)
        }
    }
}

@Composable
private fun RowAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(width = 40.dp, height = 44.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EmptyState() {
    val c = QuestLogTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.bookmark_empty_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.Medium, fontSize = 22.sp),
        )
        Text(
            stringResource(R.string.bookmark_empty_body),
            color = c.textSecondary,
            textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = Lora, fontSize = 13.5.sp, lineHeight = 21.sp),
        )
    }
}

@Composable
private fun PanelPreview(items: List<BookmarkUi>, readOnly: Boolean = false) {
    QuestLogTheme {
        BookmarkPanel(
            gameName = "Dragon Quest VII",
            bookmarks = items,
            currentArticleId = 1,
            readOnly = readOnly,
            onAddHere = {},
            onEdit = {},
            onDelete = {},
            onJump = {},
            onClose = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 760)
@Composable
private fun BookmarkPanelFullPreview() = PanelPreview(ReaderPreviewData.bookmarks)

@Preview(showBackground = true, widthDp = 390, heightDp = 760)
@Composable
private fun BookmarkPanelEmptyPreview() = PanelPreview(emptyList())

@Preview(showBackground = true, widthDp = 390, heightDp = 760)
@Composable
private fun BookmarkPanelReadOnlyPreview() = PanelPreview(ReaderPreviewData.bookmarks, readOnly = true)
