package com.rds.questlog.presentation.addarticle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlDialog
import com.rds.questlog.presentation.components.QlGhostButton
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.components.QlOutlineButton
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QuestLogTheme

/** Tombol "Tempel" di samping hitungan URL (AddArticle.dc.html v1): isi daftar dari clipboard hanya saat diketuk. */
@Composable
internal fun PasteButton(onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(shape)
            .background(if (pressed) c.accentTintActive else Color.Transparent)
            .border(1.dp, c.accentLine, shape)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(QlIcons.Clipboard, contentDescription = null, tint = c.accentText, modifier = Modifier.size(14.dp))
        Text(
            stringResource(R.string.add_article_paste),
            color = c.accentText,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
        )
    }
}

/** Penanda bahwa URL di Hal. 1 berasal dari "Bagikan" di browser (QL-21). */
@Composable
internal fun SharedUrlBanner() {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.accentTint)
            .border(1.dp, c.accentLine.copy(alpha = 0.5f), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(QlIcons.Share, contentDescription = null, tint = c.accentText, modifier = Modifier.size(14.dp))
        Text(
            stringResource(R.string.add_article_shared_banner),
            color = c.accentText,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
        )
    }
}

/**
 * URL dari "Bagikan" yang tiba saat S2 sudah terbuka (QL-21): daftar masih kosong → langsung mengisi Hal. 1;
 * sudah berisi → [onNeedsConfirm] (tidak menimpa tanpa konfirmasi). [onHandled] dipanggil sekali setelah diproses.
 */
@Composable
internal fun IncomingSharedUrl(
    url: String?,
    hasFilledUrls: Boolean,
    onApply: (String) -> Unit,
    onNeedsConfirm: (String) -> Unit,
    onHandled: () -> Unit,
) {
    LaunchedEffect(url) {
        val incoming = url ?: return@LaunchedEffect
        if (hasFilledUrls) onNeedsConfirm(incoming) else onApply(incoming)
        onHandled()
    }
}

/** Konfirmasi sebelum URL dari "Bagikan" menggantikan URL yang sudah diisi di S2 (QL-21). */
@Composable
internal fun ReplaceUrlsDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    QlDialog(onDismiss = onDismiss, maxWidth = 320.dp) {
        Text(
            stringResource(R.string.add_article_replace_title),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = CormorantGaramond, fontSize = 24.sp),
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Text(
            stringResource(R.string.add_article_replace_body),
            color = QuestLogTheme.colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            QlGhostButton(stringResource(R.string.add_article_replace_cancel), onDismiss)
            QlOutlineButton(stringResource(R.string.add_article_replace_confirm), onConfirm)
        }
    }
}

/** Pesan inline hasil mengetuk Tempel: sukses berwarna sekunder, gagal berwarna bahaya. */
@Composable
internal fun PasteMessage(outcome: PasteOutcome) {
    val c = QuestLogTheme.colors
    val (text, color) = when (outcome) {
        is PasteOutcome.Added -> stringResource(R.string.add_article_paste_done, outcome.count) to c.textSecondary
        PasteOutcome.NoUrls -> stringResource(R.string.add_article_paste_invalid) to c.danger
        PasteOutcome.NothingNew -> stringResource(R.string.add_article_paste_nothing_new) to c.danger
    }
    Text(text, color = color, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp))
}

private fun urls(count: Int) = List(count) { "https://gamefaqs.gamespot.com/ps/dq7/faqs/walkthrough?page=${it + 1}" }

@Composable
private fun UrlSectionPreview(form: AddArticleForm, outcome: PasteOutcome? = null, shared: Boolean = false) {
    QuestLogTheme {
        Column(Modifier.background(MaterialTheme.colorScheme.background).padding(20.dp)) {
            UrlSection(
                form = form,
                target = null,
                errors = emptyMap(),
                onChange = { _, _ -> },
                onAdd = {},
                onRemove = {},
                onMove = { _, _ -> },
                showSharedBanner = shared,
                pasteOutcome = outcome,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun UrlSectionNormalPreview() = UrlSectionPreview(AddArticleForm())

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun UrlSectionSharedPreview() = UrlSectionPreview(AddArticleForm(urls = urls(1)), shared = true)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun UrlSectionClipboardInvalidPreview() = UrlSectionPreview(AddArticleForm(), PasteOutcome.NoUrls)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun UrlSectionNothingNewPreview() = UrlSectionPreview(AddArticleForm(urls = urls(2)), PasteOutcome.NothingNew)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun UrlSectionPasteMultiPreview() = UrlSectionPreview(AddArticleForm(urls = urls(3)), PasteOutcome.Added(3))

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun UrlSectionPasteMaxPreview() =
    UrlSectionPreview(AddArticleForm(urls = urls(MAX_URLS)), PasteOutcome.Added(MAX_URLS - 2))
