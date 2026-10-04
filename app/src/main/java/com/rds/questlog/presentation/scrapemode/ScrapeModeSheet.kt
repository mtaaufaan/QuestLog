package com.rds.questlog.presentation.scrapemode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.rds.questlog.presentation.components.QlGhostButton
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.model.ScrapeMode
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Pilihan eksplisit sebelum menyimpan walkthrough (QL-14): "Buat artikel baru" atau "Lengkapi artikel yang ada".
 * Opsi kedua nonaktif bila [hasArticles] false (belum ada artikel yang bisa dilengkapi).
 */
@Composable
fun ScrapeModeSheet(hasArticles: Boolean = true, onPick: (ScrapeMode) -> Unit, onClose: () -> Unit) {
    val c = QuestLogTheme.colors
    QlBottomSheet(onDismiss = onClose) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 26.dp)) {
            Column(
                Modifier.padding(start = 4.dp, end = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.scrape_mode_kicker).uppercase(),
                    color = c.accentText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.26.sp,
                    ),
                )
                Text(
                    text = stringResource(R.string.scrape_mode_title),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = CormorantGaramond,
                        fontSize = 26.sp,
                        lineHeight = 30.sp,
                    ),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeOption(
                    icon = QlIcons.FilePlus,
                    title = stringResource(R.string.scrape_mode_new_title),
                    body = stringResource(R.string.scrape_mode_new_body),
                    onClick = { onPick(ScrapeMode.NEW) },
                )
                ModeOption(
                    icon = QlIcons.BookPlus,
                    title = stringResource(R.string.scrape_mode_append_title),
                    body = stringResource(R.string.scrape_mode_append_body),
                    enabled = hasArticles,
                    onClick = { onPick(ScrapeMode.APPEND) },
                )
            }
            QlGhostButton(
                text = stringResource(R.string.scrape_mode_cancel),
                onClick = onClose,
                modifier = Modifier.padding(top = 14.dp).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ModeOption(icon: ImageVector, title: String, body: String, enabled: Boolean = true, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(if (pressed && enabled) c.accentTint else Color.Transparent)
            .border(1.dp, if (pressed && enabled) c.accentLine else c.divider, shape)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFFA06F24),
            modifier = Modifier.padding(top = 2.dp).size(22.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                color = c.textSecondary,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun ScrapeModeSheetPreview() {
    QuestLogTheme { ScrapeModeSheet(hasArticles = true, onPick = {}, onClose = {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun ScrapeModeSheetNoArticlesPreview() {
    QuestLogTheme { ScrapeModeSheet(hasArticles = false, onPick = {}, onClose = {}) }
}
