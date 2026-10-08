package com.rds.questlog.presentation.articlelist

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlBottomSheet
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.components.QlOutlineButton
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.PageStatus
import com.rds.questlog.presentation.model.PageUiModel
import com.rds.questlog.presentation.preview.PreviewData
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QlMonoStyle
import com.rds.questlog.presentation.theme.QuestLogTheme

private val RefreshTint = Color(0xFFA06F24)

/**
 * Kelola halaman satu artikel: geser urutan, unduh ulang satu halaman, hapus (component-contract.md §13).
 * Semua callback memakai INDEKS baris; pemanggil memetakannya ke id halaman.
 */
@Composable
fun PageManagerSheet(
    articleTitle: String,
    pages: List<PageUiModel>,
    onMove: (index: Int, dir: Int) -> Unit,
    onRefreshPage: (index: Int) -> Unit,
    onDeletePage: (index: Int) -> Unit,
    onClose: () -> Unit,
) {
    val c = QuestLogTheme.colors
    val failed = pages.count { it.status == PageStatus.FAILED }
    QlBottomSheet(onDismiss = onClose) {
        Column(Modifier.padding(bottom = 18.dp)) {
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.page_manager_kicker).uppercase(),
                    color = c.accentText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.26.sp,
                    ),
                )
                Text(
                    text = articleTitle,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = CormorantGaramond,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                    ),
                )
                Text(
                    text = if (failed > 0) {
                        stringResource(R.string.page_manager_summary_failed, pages.size, failed)
                    } else {
                        stringResource(R.string.page_manager_summary, pages.size)
                    },
                    color = c.textSecondary,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontFeatureSettings = "tnum",
                    ),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground)
            LazyColumn(Modifier.weight(1f, fill = false)) {
                itemsIndexed(pages) { i, p ->
                    PageRow(
                        index = i,
                        page = p,
                        isLast = pages.size == 1,
                        canMoveUp = i > 0,
                        canMoveDown = i < pages.lastIndex,
                        onMove = onMove,
                        onRefresh = { onRefreshPage(i) },
                        onDelete = { onDeletePage(i) },
                    )
                }
            }
            Text(
                text = stringResource(
                    if (pages.size == 1) R.string.page_manager_footnote_last else R.string.page_manager_footnote,
                ),
                color = c.textTertiary,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 19.sp,
                ),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
            )
            QlOutlineButton(
                text = stringResource(R.string.page_manager_done),
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .heightIn(min = 46.dp),
            )
        }
    }
}

@Composable
private fun PageRow(
    index: Int,
    page: PageUiModel,
    isLast: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int, Int) -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = QuestLogTheme.colors
    val busy = page.status == PageStatus.PENDING
    Column {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = (index + 1).toString(),
                color = c.textSecondary,
                modifier = Modifier.width(28.dp),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = page.url.removePrefix("https://").removePrefix("www."),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = QlMonoStyle.copy(fontSize = 11.5.sp),
                )
                PageStatusLine(page)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    MoveButton(QlIcons.ChevronUp, R.string.page_manager_move_up, canMoveUp) { onMove(index, -1) }
                    MoveButton(QlIcons.ChevronDownSmall, R.string.page_manager_move_down, canMoveDown) {
                        onMove(index, 1)
                    }
                }
                RowIconButton(
                    icon = QlIcons.Refresh,
                    description = R.string.page_manager_refresh_page,
                    tint = RefreshTint,
                    disabledAlpha = 0.3f,
                    enabled = !busy,
                    onClick = onRefresh,
                )
                RowIconButton(
                    icon = QlIcons.Trash,
                    description = R.string.page_manager_delete_page,
                    tint = c.danger,
                    disabledAlpha = 0.25f,
                    enabled = !isLast && !busy,
                    onClick = onDelete,
                )
            }
        }
        HorizontalDivider(color = c.divider)
    }
}

@Composable
private fun PageStatusLine(page: PageUiModel) {
    val c = QuestLogTheme.colors
    val (label, color) = when (page.status) {
        PageStatus.PENDING -> stringResource(R.string.page_manager_status_refreshing) to c.accentText
        PageStatus.FAILED -> stringResource(
            R.string.page_manager_status_failed,
            page.reason ?: stringResource(R.string.page_manager_status_failed_unknown),
        ) to c.danger
        PageStatus.DONE -> stringResource(R.string.page_manager_status_saved) to c.textSecondary
    }
    val alpha = if (page.status == PageStatus.PENDING) {
        val transition = rememberInfiniteTransition(label = "pulse")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.2f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "pulse",
        ).value
    } else {
        1f
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).alpha(alpha).background(color, CircleShape))
        Text(label, color = color, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp), maxLines = 2)
    }
}

@Composable
private fun MoveButton(icon: ImageVector, description: Int, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 32.dp, height = 22.dp)
            .alpha(if (enabled) 1f else 0.25f)
            .clip(RoundedCornerShape(3.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = stringResource(description),
            tint = QuestLogTheme.colors.textSecondary,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun RowIconButton(
    icon: ImageVector,
    description: Int,
    tint: Color,
    disabledAlpha: Float,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 44.dp)
            .alpha(if (enabled) 1f else disabledAlpha)
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = stringResource(description), tint = tint, modifier = Modifier.size(17.dp))
    }
}

private val PreviewManaged: ArticleUiModel = PreviewData.articles.first { it.id == 2L }

@Preview(showBackground = true, widthDp = 390, heightDp = 720)
@Composable
private fun PageManagerSheetPreview() {
    QuestLogTheme {
        PageManagerSheet(PreviewManaged.title, PreviewManaged.pages, { _, _ -> }, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 480)
@Composable
private fun PageManagerSheetRefreshingPreview() {
    QuestLogTheme {
        val pages = PreviewManaged.pages.mapIndexed { i, p -> if (i == 0) p.copy(status = PageStatus.PENDING) else p }
        PageManagerSheet(PreviewManaged.title, pages, { _, _ -> }, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 360)
@Composable
private fun PageManagerSheetLastPagePreview() {
    QuestLogTheme {
        PageManagerSheet(PreviewManaged.title, PreviewManaged.pages.take(1), { _, _ -> }, {}, {}, {})
    }
}
