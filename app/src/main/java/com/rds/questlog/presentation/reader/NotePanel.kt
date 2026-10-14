package com.rds.questlog.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlBottomSheet
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.preview.ReaderPreviewData
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme
import kotlinx.coroutines.delay

private const val SHEET_HEIGHT = 0.92f
private const val AUTOSAVE_MS = 1_000L

/**
 * Simpan otomatis catatan: menunggu [debounceMs] setelah mengetik berhenti, lalu memanggil [save]; mengembalikan teks
 * yang berhasil disimpan (atau [saved] bila gagal, sehingga status tetap "Menyimpan…" dan dicoba lagi di ketikan
 * berikutnya). Dibatalkan otomatis bila teks berubah lagi.
 */
suspend fun autosaveNote(text: String, saved: String, debounceMs: Long, save: suspend (String) -> Boolean): String {
    if (text == saved) return saved
    delay(debounceMs)
    return if (save(text)) text else saved
}

/**
 * Note Panel (P13): satu catatan umum per artikel. Simpan otomatis [AUTOSAVE_MS] setelah mengetik berhenti lewat
 * [onSave] (status "Menyimpan…" → "Tersimpan" menunggu hasilnya) dan sisa ketikan disimpan lewat [onFlush] saat panel
 * ditutup. [readOnly] = premium tidak aktif.
 */
@Composable
fun NotePanel(
    articleTitle: String,
    initialText: String,
    readOnly: Boolean,
    onSave: suspend (String) -> Boolean,
    onFlush: (String) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initialText) }
    var saved by rememberSaveable { mutableStateOf(initialText) }
    val latest by rememberUpdatedState(text)
    val savedNow by rememberUpdatedState(saved)
    val currentFlush by rememberUpdatedState(onFlush)

    LaunchedEffect(text) {
        if (!readOnly) saved = autosaveNote(text, saved, AUTOSAVE_MS, onSave)
    }
    // Simpan sisa ketikan yang belum sempat tersimpan saat panel ditutup.
    DisposableEffect(Unit) { onDispose { if (!readOnly && latest != savedNow) currentFlush(latest) } }

    QlBottomSheet(onDismiss = onClose) {
        Column(Modifier.fillMaxHeight(SHEET_HEIGHT).navigationBarsPadding().imePadding()) {
            NoteHeader(articleTitle, saving = text != saved, canDelete = !readOnly && text.isNotEmpty()) {
                text = ""
                saved = ""
                onDelete()
            }
            if (readOnly) ReadOnlyLine()
            NoteField(text, readOnly, { text = it }, Modifier.weight(1f))
            HorizontalDivider(color = QuestLogTheme.colors.divider)
            NoteFooter(text.length)
        }
    }
}

@Composable
private fun NoteHeader(title: String, saving: Boolean, canDelete: Boolean, onDelete: () -> Unit) {
    val c = QuestLogTheme.colors
    Column {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.note_kicker).uppercase(),
                    color = c.accentText,
                    style = TextStyle(
                        fontFamily = Lora,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.26.sp,
                    ),
                )
                Text(
                    title,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.Medium, fontSize = 20.sp),
                )
            }
            Text(
                stringResource(if (saving) R.string.note_saving else R.string.note_saved),
                color = if (saving) c.accentText else c.textTertiary,
                style = TextStyle(fontFamily = Lora, fontSize = 12.sp),
            )
            Box(
                Modifier
                    .size(44.dp)
                    .alpha(if (canDelete) 1f else 0.35f)
                    .clickable(enabled = canDelete, role = Role.Button, onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    QlIcons.Trash,
                    contentDescription = stringResource(R.string.note_delete),
                    tint = c.danger,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ReadOnlyLine() {
    val c = QuestLogTheme.colors
    Row(
        Modifier.fillMaxWidth().background(c.accentTint).padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(QlIcons.Lock, contentDescription = null, tint = c.accentText, modifier = Modifier.size(14.dp))
        Text(
            stringResource(R.string.note_read_only),
            color = MaterialTheme.colorScheme.onSurface,
            style = TextStyle(fontFamily = Lora, fontSize = 12.5.sp),
        )
    }
}

@Composable
private fun NoteField(text: String, readOnly: Boolean, onChange: (String) -> Unit, modifier: Modifier) {
    val c = QuestLogTheme.colors
    val style = TextStyle(
        fontFamily = Lora,
        fontSize = 16.sp,
        lineHeight = 27.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Box(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
        BasicTextField(
            value = text,
            onValueChange = onChange,
            readOnly = readOnly,
            textStyle = style,
            cursorBrush = SolidColor(c.accentLine),
            modifier = Modifier.fillMaxSize(),
        )
        if (text.isEmpty()) {
            Text(stringResource(R.string.note_placeholder), color = c.iconMuted, style = style)
        }
    }
}

@Composable
private fun NoteFooter(chars: Int) {
    val c = QuestLogTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val style = TextStyle(fontFamily = Lora, fontSize = 11.5.sp)
        Text(stringResource(R.string.note_footer), color = c.textTertiary, style = style)
        Text(stringResource(R.string.note_chars, chars), color = c.textTertiary, style = style)
    }
}

@Composable
private fun NotePreview(text: String, readOnly: Boolean = false) {
    QuestLogTheme {
        NotePanel("Walkthrough Lengkap — Disc 1", text, readOnly, { true }, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun NoteEmptyPreview() = NotePreview("")

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun NoteFilledPreview() = NotePreview(ReaderPreviewData.NOTE)

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun NoteReadOnlyPreview() = NotePreview(ReaderPreviewData.NOTE, readOnly = true)
