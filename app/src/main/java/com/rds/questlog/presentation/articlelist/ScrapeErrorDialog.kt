package com.rds.questlog.presentation.articlelist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlDangerGhostButton
import com.rds.questlog.presentation.components.QlDialog
import com.rds.questlog.presentation.components.QlGhostButton
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.components.QlOutlineButton
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.PageStatus
import com.rds.questlog.presentation.preview.PreviewData
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QlMonoStyle
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Daftar halaman yang gagal beserta alasannya. [onRetry] mengulang SEMUA halaman FAILED;
 * [onDelete] membuka konfirmasi hapus (bukan menghapus langsung).
 */
@Composable
fun ScrapeErrorDialog(article: ArticleUiModel, onRetry: () -> Unit, onDelete: () -> Unit, onClose: () -> Unit) {
    val c = QuestLogTheme.colors
    val failed = article.pages.withIndex().filter { it.value.status == PageStatus.FAILED }
    val allFailed = failed.size == article.pages.size
    QlDialog(onDismiss = onClose) {
        Row(
            modifier = Modifier.padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(c.danger, CircleShape))
            Text(
                text = stringResource(
                    if (allFailed) R.string.scrape_error_kicker_all else R.string.scrape_error_kicker_partial,
                ).uppercase(),
                color = c.danger,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.26.sp,
                ),
            )
        }
        Text(
            text = article.title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = CormorantGaramond,
                fontSize = 24.sp,
                lineHeight = 28.sp,
            ),
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Text(
            text = if (allFailed) {
                stringResource(R.string.scrape_error_body_all)
            } else {
                stringResource(R.string.scrape_error_body_partial, failed.size, article.pages.size)
            },
            color = c.textSecondary,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
            modifier = Modifier.padding(bottom = 14.dp),
        )
        HorizontalDivider(color = c.divider)
        failed.forEach { (index, page) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.scrape_error_page, index + 1),
                    color = c.textSecondary,
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    modifier = Modifier.width(44.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = page.url,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                        style = QlMonoStyle.copy(fontSize = 11.5.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    page.reason?.let {
                        Text(it, color = c.danger, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp))
                    }
                }
            }
            HorizontalDivider(color = c.divider)
        }
        Text(
            text = stringResource(R.string.scrape_error_note),
            color = c.textTertiary,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QlDangerGhostButton(stringResource(R.string.scrape_error_delete), onDelete)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                QlGhostButton(stringResource(R.string.scrape_error_close), onClose)
                QlOutlineButton(stringResource(R.string.scrape_error_retry), onRetry, icon = QlIcons.Refresh)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 600)
@Composable
private fun ScrapeErrorDialogPreview() {
    QuestLogTheme {
        ScrapeErrorDialog(PreviewData.articles.first { it.id == 5L }, onRetry = {}, onDelete = {}, onClose = {})
    }
}
