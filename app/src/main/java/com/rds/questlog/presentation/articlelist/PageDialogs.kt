package com.rds.questlog.presentation.articlelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlDangerButton
import com.rds.questlog.presentation.components.QlDialog
import com.rds.questlog.presentation.components.QlGhostButton
import com.rds.questlog.presentation.components.QlOutlineButton
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QlMonoStyle
import com.rds.questlog.presentation.theme.QuestLogTheme

/** Konfirmasi hapus satu halaman; tampil di atas Page Manager Sheet (component-contract.md §14). */
@Composable
fun DeletePageDialog(pageNumber: Int, pageUrl: String, onConfirm: () -> Unit, onClose: () -> Unit) {
    QlDialog(onDismiss = onClose, maxWidth = 320.dp) {
        DialogTitle(stringResource(R.string.delete_page_title, pageNumber), bottomPadding = 10.dp)
        Text(
            text = pageUrl.removePrefix("https://").removePrefix("www."),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            style = QlMonoStyle.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        DialogBody(stringResource(R.string.delete_page_body))
        DialogButtons {
            QlGhostButton(stringResource(R.string.delete_article_cancel), onClose)
            QlDangerButton(stringResource(R.string.delete_article_confirm), onConfirm)
        }
    }
}

/** Konfirmasi unduh ulang seluruh halaman satu artikel (component-contract.md §15). */
@Composable
fun RefreshArticleDialog(articleTitle: String, pageCount: Int, onConfirm: () -> Unit, onClose: () -> Unit) {
    QlDialog(onDismiss = onClose, maxWidth = 320.dp) {
        DialogTitle(stringResource(R.string.refresh_article_title), bottomPadding = 6.dp)
        Text(
            text = stringResource(R.string.refresh_article_meta, articleTitle, pageCount),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, lineHeight = 20.sp),
            modifier = Modifier.padding(bottom = 10.dp),
        )
        DialogBody(stringResource(R.string.refresh_article_body))
        DialogButtons {
            QlGhostButton(stringResource(R.string.delete_article_cancel), onClose)
            QlOutlineButton(stringResource(R.string.refresh_article_confirm), onConfirm)
        }
    }
}

@Composable
private fun DialogTitle(text: String, bottomPadding: Dp) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = CormorantGaramond,
            fontSize = 24.sp,
            lineHeight = 28.sp,
        ),
        modifier = Modifier.padding(bottom = bottomPadding),
    )
}

@Composable
private fun DialogBody(text: String) {
    Text(
        text = text,
        color = QuestLogTheme.colors.textSecondary,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp, fontWeight = FontWeight.Normal),
    )
}

@Composable
private fun DialogButtons(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) { content() }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 480)
@Composable
private fun DeletePageDialogPreview() {
    QuestLogTheme {
        DeletePageDialog(3, "https://dragonquest.fandom.com/wiki/Mini_Medal_(DQVII)?section=3", {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 480)
@Composable
private fun RefreshArticleDialogPreview() {
    QuestLogTheme {
        RefreshArticleDialog("Walkthrough Lengkap — Disc 1", 5, {}, {})
    }
}
