package com.rds.questlog.presentation.articlelist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlBottomSheet
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.preview.PreviewData
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Aksi kontekstual satu artikel. "Lanjutkan dari checkpoint" hanya bila READY dan ada checkpoint;
 * "Buka artikel" hanya bila READY.
 */
@Composable
fun ArticleActionsSheet(article: ArticleUiModel, onAction: (ArticleAction) -> Unit, onClose: () -> Unit) {
    val c = QuestLogTheme.colors
    val isReady = article.status == ArticleStatus.READY
    val busy = article.status == ArticleStatus.SCRAPING
    QlBottomSheet(onDismiss = onClose) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 22.dp)) {
            Column(
                Modifier.padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = article.gameName.uppercase(),
                    color = c.accentText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.26.sp,
                    ),
                )
                Text(
                    text = article.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = CormorantGaramond,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                    ),
                )
                Text(
                    text = stringResource(R.string.article_actions_meta, article.donePageCount, article.totalPageCount),
                    color = c.textSecondary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                )
            }
            HorizontalDivider(color = c.divider)
            Column(Modifier.padding(top = 6.dp)) {
                if (isReady && article.checkpointNodeId != null) {
                    ActionRow(QlIcons.Bookmark, stringResource(R.string.article_actions_resume), Color(0xFFA06F24)) {
                        onAction(ArticleAction.RESUME)
                    }
                }
                if (isReady) {
                    ActionRow(QlIcons.BookOpen, stringResource(R.string.article_actions_open), c.textSecondary) {
                        onAction(ArticleAction.OPEN)
                    }
                }
                ActionRow(
                    icon = QlIcons.BookPlus,
                    label = stringResource(R.string.article_actions_append),
                    iconColor = c.textSecondary,
                    hint = stringResource(R.string.article_actions_append_hint),
                ) { onAction(ArticleAction.APPEND) }
                ActionRow(QlIcons.Pencil, stringResource(R.string.article_actions_edit), c.textSecondary) {
                    onAction(ArticleAction.EDIT)
                }
                ActionRow(
                    icon = QlIcons.ListBullets,
                    label = stringResource(R.string.article_actions_pages),
                    iconColor = c.textSecondary,
                    hint = stringResource(
                        if (busy) R.string.article_actions_pages_busy else R.string.article_actions_pages_hint,
                    ),
                    enabled = !busy,
                ) { onAction(ArticleAction.PAGES) }
                ActionRow(
                    icon = QlIcons.Refresh,
                    label = stringResource(R.string.article_actions_refresh),
                    iconColor = c.textSecondary,
                    enabled = !busy,
                ) { onAction(ArticleAction.REFRESH) }
                HorizontalDivider(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = c.divider)
                ActionRow(
                    icon = QlIcons.Trash,
                    label = stringResource(R.string.article_actions_delete),
                    iconColor = c.danger,
                    textColor = c.danger,
                    pressedColor = c.dangerTint,
                ) { onAction(ArticleAction.DELETE) }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    pressedColor: Color = QuestLogTheme.colors.hover,
    hint: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(4.dp))
            .background(if (pressed) pressedColor else Color.Transparent)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, color = textColor, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp))
            if (hint != null) {
                Text(hint, color = QuestLogTheme.colors.textTertiary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun ArticleActionsSheetPreview() {
    QuestLogTheme {
        ArticleActionsSheet(PreviewData.articles.first { it.id == 1L }, onAction = {}, onClose = {})
    }
}
