package com.rds.questlog.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlDialog
import com.rds.questlog.presentation.components.QlGhostButton
import com.rds.questlog.presentation.components.QlOutlineButton
import com.rds.questlog.presentation.components.QlTextField
import com.rds.questlog.presentation.model.BookmarkLabelMode
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Bookmark Label Dialog (P12a): label wajib (maks [MAX_BOOKMARK_LABEL]), cuplikan teks posisi, chip saran.
 * [recent] = label terbaru pengguna (terbaru dulu) untuk chip ↺.
 */
@Composable
fun BookmarkLabelDialog(
    mode: BookmarkLabelMode,
    snippet: String,
    initialLabel: String,
    recent: List<String>,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = QuestLogTheme.colors
    var label by rememberSaveable { mutableStateOf(initialLabel) }
    var showError by rememberSaveable { mutableStateOf(false) }
    val submit = {
        if (label.isBlank()) showError = true else onSave(label.trim())
    }
    QlDialog(onDismiss = onDismiss) {
        Text(
            stringResource(
                if (mode == BookmarkLabelMode.ADD) R.string.bookmark_label_add else R.string.bookmark_label_edit,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            style = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.Medium, fontSize = 24.sp),
        )
        if (snippet.isNotEmpty()) {
            Text(
                "“$snippet”",
                color = c.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .background(c.hover)
                    .drawBehind { drawRect(c.accentLine, size = Size(2.dp.toPx(), size.height)) }
                    .padding(start = 12.dp, top = 8.dp, end = 10.dp, bottom = 8.dp),
                style = TextStyle(
                    fontFamily = Lora,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                ),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.bookmark_label_field).uppercase(),
                color = c.textSecondary,
                style = TextStyle(
                    fontFamily = Lora,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.9.sp,
                ),
            )
            Text(
                "${label.length}/$MAX_BOOKMARK_LABEL",
                color = c.textTertiary,
                style = TextStyle(fontFamily = Lora, fontSize = 11.5.sp, fontFeatureSettings = "tnum"),
            )
        }
        QlTextField(
            value = label,
            onValueChange = {
                label = it.take(MAX_BOOKMARK_LABEL)
                showError = false
            },
            errorText = if (showError) stringResource(R.string.bookmark_label_error) else null,
        )
        LabelSuggestionChips(recent, label, Modifier.padding(top = 12.dp)) {
            label = it
            showError = false
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            QlGhostButton(stringResource(R.string.bookmark_label_cancel), onDismiss, Modifier.height(44.dp))
            QlOutlineButton(stringResource(R.string.bookmark_label_save), submit, Modifier.height(44.dp))
        }
    }
}

/** Chip saran label (terbaru + preset); mengetuk chip memanggil [onLabel] dengan isian baru. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabelSuggestionChips(
    recent: List<String>,
    label: String,
    modifier: Modifier = Modifier,
    onLabel: (String) -> Unit,
) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labelSuggestions(recent, label).forEach { s ->
            SuggestionChip(s) { onLabel(applySuggestion(s, label, recent)) }
        }
    }
}

@Composable
private fun SuggestionChip(suggestion: LabelSuggestion, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(15.dp)
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(shape)
            .background(if (suggestion.selected) c.accentTint else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.dp, if (suggestion.selected) c.accentLine else c.divider, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            (if (suggestion.isRecent) "↺ " else "") + suggestion.text,
            color = if (suggestion.selected) c.accentText else c.textSecondary,
            style = TextStyle(fontFamily = Lora, fontSize = 12.5.sp),
        )
    }
}

@Composable
private fun DialogPreview(mode: BookmarkLabelMode, label: String) {
    QuestLogTheme {
        BookmarkLabelDialog(
            mode = mode,
            snippet = "Peti di ruang bawah tanah barat berisi Mini Medal pertama.",
            initialLabel = label,
            recent = listOf("Boss Dharma", "Peti kunci"),
            onSave = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 640)
@Composable
private fun BookmarkLabelAddPreview() = DialogPreview(BookmarkLabelMode.ADD, "")

@Preview(showBackground = true, widthDp = 390, heightDp = 640)
@Composable
private fun BookmarkLabelEditPreview() = DialogPreview(BookmarkLabelMode.EDIT, "Boss Dharma")
