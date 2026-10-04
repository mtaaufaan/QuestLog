package com.rds.questlog.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.model.ReadMode
import com.rds.questlog.presentation.model.ReaderViewState
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme

private const val DISABLED_ALPHA = 0.3f

@Composable
fun ReaderHeader(
    gameName: String,
    title: String,
    hasCheckpoint: Boolean,
    actionsEnabled: Boolean,
    onBack: () -> Unit,
    onCheckpoint: () -> Unit,
    onSettings: () -> Unit,
) {
    val c = QuestLogTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(start = 8.dp, end = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconAction(
            QlIcons.ArrowLeft,
            stringResource(R.string.reader_back),
            MaterialTheme.colorScheme.onBackground,
            22,
            onClick = onBack,
        )
        Column(Modifier.weight(1f).padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = gameName.uppercase(),
                color = c.accentText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontFamily = Lora,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                ),
            )
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
            )
        }
        IconAction(
            icon = if (hasCheckpoint) QlIcons.BookmarkSolid else QlIcons.Bookmark,
            description = stringResource(R.string.reader_checkpoint),
            tint = c.accentLine,
            size = 21,
            enabled = actionsEnabled,
            onClick = onCheckpoint,
        )
        IconAction(
            QlIcons.Sliders,
            stringResource(R.string.reader_settings),
            MaterialTheme.colorScheme.onBackground,
            20,
            onClick = onSettings,
        )
    }
}

@Composable
private fun IconAction(
    icon: ImageVector,
    description: String,
    tint: Color,
    size: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(CircleShape)
            .background(if (pressed) QuestLogTheme.colors.hover else Color.Transparent)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size.dp))
    }
}

/** Garis progres baca setinggi 2dp di bawah header. [percent] 0–100. */
@Composable
fun ReaderProgress(percent: Int) {
    val c = QuestLogTheme.colors
    Box(Modifier.fillMaxWidth().height(2.dp).background(c.divider)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(percent.coerceIn(0, 100) / 100f).background(c.accentLine))
    }
}

/** Toggle Seamless / Per Halaman beserta label posisi di kanannya. */
@Composable
fun ReaderModeBar(mode: ReadMode, positionLabel: String, onModeChange: (ReadMode) -> Unit) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(4.dp)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.clip(shape).border(1.dp, c.accentLine, shape)) {
                ModeTab(
                    stringResource(R.string.reader_mode_seamless),
                    mode == ReadMode.SEAMLESS,
                ) { onModeChange(ReadMode.SEAMLESS) }
                Box(Modifier.width(1.dp).height(30.dp).background(c.accentLine))
                ModeTab(
                    stringResource(R.string.reader_mode_paged),
                    mode == ReadMode.PAGED,
                ) { onModeChange(ReadMode.PAGED) }
            }
            Text(
                text = positionLabel,
                color = c.textSecondary,
                style = TextStyle(fontFamily = Lora, fontSize = 12.5.sp, fontFeatureSettings = "tnum"),
            )
        }
        Divider()
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    Box(
        modifier = Modifier
            .height(30.dp)
            .background(if (selected) c.accentTint else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = c.accentText,
            style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
        )
    }
}

/** Banner artikel parsial: jumlah halaman gagal + "Coba Lagi" (spinner selama [retrying]). */
@Composable
fun PartialBanner(failedCount: Int, retrying: Boolean, onRetry: () -> Unit) {
    val c = QuestLogTheme.colors
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().background(c.dangerTint).padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).background(c.danger, CircleShape))
            Text(
                text = stringResource(R.string.reader_partial_text, failedCount),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                style = TextStyle(fontFamily = Lora, fontSize = 13.sp, lineHeight = 18.sp),
            )
            val shape = RoundedCornerShape(4.dp)
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .alpha(if (retrying) 0.6f else 1f)
                    .clip(shape)
                    .border(1.dp, c.accentLine, shape)
                    .clickable(enabled = !retrying, role = Role.Button, onClick = onRetry)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (retrying) {
                    CircularProgressIndicator(
                        Modifier.size(11.dp),
                        color = c.accentLine,
                        strokeWidth = 1.5.dp,
                    )
                }
                Text(
                    stringResource(if (retrying) R.string.reader_retrying else R.string.reader_retry),
                    color = c.accentText,
                    style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                )
            }
        }
        Divider()
    }
}

/** Navigasi Sebelumnya/Berikutnya + indikator "Halaman n dari total" (khusus mode Per Halaman). */
@Composable
fun PagedFooter(indicator: String, canPrev: Boolean, canNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    val c = QuestLogTheme.colors
    Column(Modifier.navigationBarsPadding()) {
        Divider()
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavButton(
                stringResource(R.string.reader_prev),
                QlIcons.ChevronLeft,
                canPrev,
                iconFirst = true,
                onClick = onPrev,
            )
            Text(
                indicator,
                color = c.textSecondary,
                style = TextStyle(fontFamily = Lora, fontSize = 13.sp, fontFeatureSettings = "tnum"),
            )
            NavButton(
                stringResource(R.string.reader_next),
                QlIcons.ChevronRight,
                canNext,
                iconFirst = false,
                onClick = onNext,
            )
        }
    }
}

@Composable
private fun NavButton(label: String, icon: ImageVector, enabled: Boolean, iconFirst: Boolean, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    Row(
        modifier = Modifier
            .height(44.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconFirst) Icon(icon, null, tint = c.accentText, modifier = Modifier.size(18.dp))
        Text(
            label,
            color = c.accentText,
            style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
        )
        if (!iconFirst) Icon(icon, null, tint = c.accentText, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun ReaderLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(22.dp), color = QuestLogTheme.colors.accentLine, strokeWidth = 1.5.dp)
    }
}

/** State not_found / empty / db_error: ikon, judul, deskripsi, dan satu tombol aksi. */
@Composable
fun ReaderMessage(state: ReaderViewState, onAction: () -> Unit) {
    val c = QuestLogTheme.colors
    val spec = when (state) {
        ReaderViewState.NOT_FOUND -> MessageSpec(
            QlIcons.FileX,
            R.string.reader_not_found_title,
            R.string.reader_not_found_body,
            R.string.reader_back,
        )
        ReaderViewState.EMPTY -> MessageSpec(
            QlIcons.Inbox,
            R.string.reader_empty_title,
            R.string.reader_empty_body,
            R.string.reader_back,
        )
        else -> MessageSpec(
            QlIcons.AlertTriangle,
            R.string.reader_db_error_title,
            R.string.reader_db_error_body,
            R.string.reader_retry,
        )
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier.padding(bottom = 6.dp).size(64.dp).border(1.dp, c.divider, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                spec.icon,
                null,
                tint = if (state == ReaderViewState.DB_ERROR) c.danger else c.textSecondary,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            stringResource(spec.title),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = CormorantGaramond,
                fontWeight = FontWeight.Medium,
                fontSize = 26.sp,
                lineHeight = 30.sp,
            ),
        )
        Text(
            stringResource(spec.body),
            color = c.textSecondary,
            textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = Lora, fontSize = 14.sp, lineHeight = 22.sp),
        )
        val shape = RoundedCornerShape(4.dp)
        Box(
            modifier = Modifier
                .padding(top = 14.dp)
                .height(46.dp)
                .clip(shape)
                .border(1.dp, c.accentLine, shape)
                .clickable(role = Role.Button, onClick = onAction)
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(spec.action),
                color = c.accentText,
                style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
            )
        }
    }
}

private data class MessageSpec(val icon: ImageVector, val title: Int, val body: Int, val action: Int)

/** Snackbar persegi (radius 4) khusus Reader; warna mengikuti tema Reader (terang/gelap). */
@Composable
fun ReaderSnackbar(text: String, modifier: Modifier = Modifier) {
    val c = QuestLogTheme.colors
    Text(
        text = text,
        color = c.snackbarText,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(c.snackbar)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        style = TextStyle(fontFamily = Lora, fontSize = 13.5.sp, lineHeight = 19.sp),
    )
}

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(QuestLogTheme.colors.divider))
