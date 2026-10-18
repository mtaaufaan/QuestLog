package com.rds.questlog.presentation.reader

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlDangerButton
import com.rds.questlog.presentation.components.QlGhostButton
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.model.BookmarkUi
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme

private const val TINT_MS = 200
private val MENU_MIN_WIDTH = 200.dp

/** Aksi menu penanda bookmark; [readOnly] (premium hilang) = menu tampil tetapi kedua aksi nonaktif. */
class MarkerTools(
    val readOnly: Boolean,
    val onRename: (BookmarkUi) -> Unit,
    val onDelete: (BookmarkUi) -> Unit,
)

private enum class MarkerMenu { NONE, ACTIONS, CONFIRM }

/**
 * Penanda bookmark inline ("🔖 label ———") di atas node bertanda. Tekan-tahan membuka menu Ubah label / Hapus
 * (hapus meminta konfirmasi inline); kejadiannya tidak diteruskan ke tekan-tahan paragraf di bawahnya.
 */
@Composable
internal fun BookmarkMarker(bookmark: BookmarkUi, tools: MarkerTools) {
    val c = QuestLogTheme.colors
    var menu by remember { mutableStateOf(MarkerMenu.NONE) }
    val haptic = LocalHapticFeedback.current
    val tint by animateColorAsState(
        if (menu != MarkerMenu.NONE) c.accentTint else Color.Transparent,
        tween(TINT_MS),
        label = "markerTint",
    )
    Box(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(tint)
                .pointerInput(bookmark.id) {
                    detectTapGestures(onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menu = MarkerMenu.ACTIONS
                    })
                }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(QlIcons.Bookmark, null, tint = c.accentLine, modifier = Modifier.size(12.dp))
            Text(
                bookmark.label,
                color = c.accentText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 240.dp),
                style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
            )
            Box(Modifier.weight(1f).height(1.dp).alpha(0.6f).background(c.accentLine))
        }
        // Jangkar nol-ukuran di kanan-bawah penanda: menu terbuka di sudut itu.
        Box(Modifier.align(Alignment.BottomEnd)) {
            DropdownMenu(
                expanded = menu != MarkerMenu.NONE,
                onDismissRequest = { menu = MarkerMenu.NONE },
                modifier = Modifier.widthIn(min = MENU_MIN_WIDTH),
            ) {
                if (menu == MarkerMenu.CONFIRM) {
                    ConfirmDelete(
                        bookmark,
                        onCancel = { menu = MarkerMenu.NONE },
                        onConfirm = {
                            menu = MarkerMenu.NONE
                            tools.onDelete(bookmark)
                        },
                    )
                } else {
                    MenuActions(
                        tools.readOnly,
                        onRename = {
                            menu = MarkerMenu.NONE
                            tools.onRename(bookmark)
                        },
                        onDelete = { menu = MarkerMenu.CONFIRM },
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuActions(readOnly: Boolean, onRename: () -> Unit, onDelete: () -> Unit) {
    val c = QuestLogTheme.colors
    if (readOnly) {
        Text(
            stringResource(R.string.reader_marker_read_only),
            color = c.textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = TextStyle(fontFamily = Lora, fontSize = 12.sp),
        )
    }
    DropdownMenuItem(
        text = { Text(stringResource(R.string.reader_marker_edit), style = TextStyle(fontFamily = Lora)) },
        onClick = onRename,
        enabled = !readOnly,
        leadingIcon = { Icon(QlIcons.Pencil, null, tint = c.textSecondary, modifier = Modifier.size(18.dp)) },
    )
    DropdownMenuItem(
        text = {
            Text(stringResource(R.string.reader_marker_delete), color = c.danger, style = TextStyle(fontFamily = Lora))
        },
        onClick = onDelete,
        enabled = !readOnly,
        leadingIcon = { Icon(QlIcons.Trash, null, tint = c.danger, modifier = Modifier.size(18.dp)) },
    )
}

@Composable
private fun ConfirmDelete(bookmark: BookmarkUi, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.reader_marker_confirm, bookmark.label),
            color = QuestLogTheme.colors.textSecondary,
            style = TextStyle(fontFamily = Lora, fontSize = 13.sp, lineHeight = 18.sp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            QlGhostButton(stringResource(R.string.bookmark_label_cancel), onCancel, Modifier.height(44.dp))
            QlDangerButton(stringResource(R.string.reader_marker_confirm_delete), onConfirm, Modifier.height(44.dp))
        }
    }
}
