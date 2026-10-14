package com.rds.questlog.presentation.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlGhostButton
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.components.QlOutlineButton
import com.rds.questlog.presentation.components.QlTextField
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme

/** Lebar popover melebihi node sebesar ini di tiap sisi. */
private val POPOVER_BLEED = 6.dp
private const val ENTER_MS = 180

/** Menempel tepat di bawah node (kiri sejajar node dikurangi [bleedPx]); membalik ke atas bila tak muat di bawah. */
private class BelowNodeProvider(private val bleedPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.left - bleedPx).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val below = anchorBounds.bottom
        val y = if (below + popupContentSize.height <= windowSize.height) {
            below
        } else {
            (anchorBounds.top - popupContentSize.height).coerceAtLeast(0)
        }
        return IntOffset(x, y)
    }
}

/**
 * Popover bookmark inline (non-modal, menempel di bawah node yang ditahan). Isi mengikuti [premium]: gratis = teks
 * terkunci + Unlock, premium hilang = hanya-baca, premium = chip saran + label + Simpan. [nodeWidth] = lebar node.
 */
@Composable
fun BookmarkPopover(
    premium: PremiumUiState,
    nodeWidth: Dp,
    recent: List<String>,
    onSave: (String) -> Unit,
    onUnlock: () -> Unit,
    onDismiss: () -> Unit,
) {
    val bleedPx = with(androidx.compose.ui.platform.LocalDensity.current) { POPOVER_BLEED.roundToPx() }
    Popup(
        popupPositionProvider = remember(bleedPx) { BelowNodeProvider(bleedPx) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn(tween(ENTER_MS)) + slideInVertically(tween(ENTER_MS)) { it / 4 },
        ) {
            PopoverCard(premium, nodeWidth + POPOVER_BLEED * 2, recent, onSave, onUnlock, onDismiss)
        }
    }
}

@Composable
private fun PopoverCard(
    premium: PremiumUiState,
    width: Dp,
    recent: List<String>,
    onSave: (String) -> Unit,
    onUnlock: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(6.dp)
    var label by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val submit = { if (label.isBlank()) showError = true else onSave(label.trim()) }
    Column(
        Modifier
            .width(width)
            .shadow(4.dp, shape)
            .background(c.surface, shape)
            .border(1.dp, c.accentLine, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(QlIcons.Bookmark, null, tint = c.accentLine, modifier = Modifier.size(14.dp))
            Text(
                stringResource(R.string.bookmark_popover_kicker).uppercase(),
                color = c.accentText,
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    fontFamily = Lora,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.1.sp,
                ),
            )
            if (premium == PremiumUiState.PREMIUM) {
                Text(
                    "${label.length}/$MAX_BOOKMARK_LABEL",
                    color = c.textTertiary,
                    style = TextStyle(fontFamily = Lora, fontSize = 11.sp, fontFeatureSettings = "tnum"),
                )
            }
        }
        if (premium == PremiumUiState.PREMIUM) {
            LabelSuggestionChips(recent, label) {
                label = it
                showError = false
            }
            QlTextField(
                value = label,
                onValueChange = {
                    label = it.take(MAX_BOOKMARK_LABEL)
                    showError = false
                },
                placeholder = stringResource(R.string.bookmark_popover_placeholder),
                errorText = if (showError) stringResource(R.string.bookmark_label_error) else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            PopoverButtons {
                QlGhostButton(stringResource(R.string.bookmark_label_cancel), onDismiss, Modifier.height(44.dp))
                QlOutlineButton(stringResource(R.string.bookmark_label_save), submit, Modifier.height(44.dp))
            }
        } else {
            Text(
                stringResource(
                    if (premium == PremiumUiState.FREE) {
                        R.string.bookmark_popover_locked
                    } else {
                        R.string.bookmark_popover_lost
                    },
                ),
                color = c.textSecondary,
                style = TextStyle(fontFamily = Lora, fontSize = 13.sp, lineHeight = 19.sp),
            )
            PopoverButtons {
                QlGhostButton(stringResource(R.string.bookmark_popover_close), onDismiss, Modifier.height(44.dp))
                if (premium == PremiumUiState.FREE) {
                    QlOutlineButton(stringResource(R.string.bookmark_popover_unlock), onUnlock, Modifier.height(44.dp))
                }
            }
        }
    }
}

@Composable
private fun PopoverButtons(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}
