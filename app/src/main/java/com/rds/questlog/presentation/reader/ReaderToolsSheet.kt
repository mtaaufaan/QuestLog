package com.rds.questlog.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.model.ReaderToolAction
import com.rds.questlog.presentation.theme.QuestLogTheme

// Emas gelap tetap untuk ikon aksi utama, sama dengan ikon checkpoint di Article Actions Sheet.

/**
 * Alat baca (P17, component-contract.md §16a): Artikel utama game · Catatan (gembok bila gratis) · Tampilan.
 * Tambah/daftar bookmark tidak di sini; keduanya ada di chip bookmark pada baris mode baca.
 */
@Composable
fun ReaderToolsSheet(
    premium: PremiumUiState,
    snippet: String,
    hasGameProgress: Boolean,
    isGameProgressHere: Boolean,
    hasNote: Boolean,
    onAction: (ReaderToolAction) -> Unit,
    onClose: () -> Unit,
) {
    val c = QuestLogTheme.colors
    QlBottomSheet(onDismiss = onClose) {
        Column(Modifier.navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 22.dp)) {
            Column(
                Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.reader_tools_kicker).uppercase(),
                    color = c.accentText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.26.sp,
                    ),
                )
                Text(
                    text = stringResource(R.string.reader_tools_position, snippet),
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, fontSize = 13.5.sp),
                )
            }
            HorizontalDivider(color = c.divider)
            Column(Modifier.padding(top = 6.dp)) {
                ToolRow(
                    icon = QlIcons.Star,
                    iconColor = QuestLogTheme.colors.accentLine,
                    label = stringResource(
                        if (isGameProgressHere) R.string.reader_tools_main_here else R.string.reader_tools_main_set,
                    ),
                    hint = stringResource(
                        when {
                            isGameProgressHere -> R.string.reader_tools_main_here_hint
                            hasGameProgress -> R.string.reader_tools_main_replace_hint
                            else -> R.string.reader_tools_main_first_hint
                        },
                    ),
                    minHeight = 56,
                ) { onAction(ReaderToolAction.PROGRESS) }
                Separator()
                ToolRow(
                    icon = QlIcons.Note,
                    iconColor = c.textSecondary,
                    label = stringResource(R.string.reader_tools_note),
                    hint = stringResource(
                        if (hasNote) R.string.reader_tools_note_has else R.string.reader_tools_note_none,
                    ),
                    locked = premium == PremiumUiState.FREE,
                ) { onAction(ReaderToolAction.NOTE) }
                Separator()
                ToolRow(
                    icon = QlIcons.Sliders,
                    iconColor = c.textSecondary,
                    label = stringResource(R.string.reader_tools_display),
                    hint = stringResource(R.string.reader_tools_display_hint),
                ) { onAction(ReaderToolAction.DISPLAY) }
            }
            if (premium == PremiumUiState.LOST) {
                Text(
                    text = stringResource(R.string.reader_tools_read_only),
                    color = c.textTertiary,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun Separator() = HorizontalDivider(
    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    color = QuestLogTheme.colors.divider,
)

@Composable
private fun ToolRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    hint: String,
    minHeight: Int = 52,
    locked: Boolean = false,
    onClick: () -> Unit,
) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (pressed) c.hover else Color.Transparent)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            )
            Text(hint, color = c.textTertiary, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp))
        }
        if (locked) {
            Icon(
                QlIcons.Lock,
                contentDescription = stringResource(R.string.reader_locked),
                tint = c.accentText,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun ToolsPreview(
    premium: PremiumUiState,
    here: Boolean = false,
    hasProgress: Boolean = false,
    hasNote: Boolean = false,
) {
    QuestLogTheme {
        ReaderToolsSheet(premium, "Reruntuhan Barat", hasProgress, here, hasNote, onAction = {}, onClose = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 460)
@Composable
private fun ReaderToolsFreePreview() = ToolsPreview(PremiumUiState.FREE)

@Preview(showBackground = true, widthDp = 390, heightDp = 460)
@Composable
private fun ReaderToolsPremiumPreview() = ToolsPreview(PremiumUiState.PREMIUM, hasProgress = true, hasNote = true)

@Preview(showBackground = true, widthDp = 390, heightDp = 460)
@Composable
private fun ReaderToolsLostPreview() = ToolsPreview(PremiumUiState.LOST, here = true, hasNote = true)
