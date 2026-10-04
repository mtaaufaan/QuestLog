package com.rds.questlog.presentation.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.model.ScrapeNotificationKind
import com.rds.questlog.presentation.model.ScrapeNotificationUi
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QuestLogTheme

// Kartu notifikasi meniru notifikasi sistem: selalu gelap dan memakai font sistem, tidak mengikuti tema app.
private val CardColor = Color(0xFF2D2B2B)
private val TitleColor = Color(0xFFF3F2F2)
private val BodyColor = Color(0xFFD7D3D3)
private val MetaColor = Color(0xFFBAB6B6)
private val AccentColor = Color(0xFFE1AD66)

/**
 * Banner notifikasi scraping (component-contract.md §8): [ScrapeNotificationKind.PROGRESS] dengan bar progres,
 * serta DONE, PARTIAL, dan ERROR. [onTap]: selesai/parsial → Reader, error → Scrape Error Dialog.
 */
@Composable
fun ScrapeNotification(
    kind: ScrapeNotificationKind,
    articleTitle: String,
    current: Int,
    total: Int,
    failedCount: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, text) = when (kind) {
        ScrapeNotificationKind.PROGRESS ->
            articleTitle to stringResource(R.string.notification_progress_text, current, total)
        ScrapeNotificationKind.DONE ->
            stringResource(R.string.notification_done_title, articleTitle) to
                stringResource(R.string.notification_done_text)
        ScrapeNotificationKind.PARTIAL ->
            stringResource(R.string.notification_partial_title, articleTitle) to
                stringResource(R.string.notification_partial_text, failedCount)
        ScrapeNotificationKind.ERROR ->
            stringResource(R.string.notification_error_title, articleTitle) to
                stringResource(R.string.notification_error_text)
    }
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, shape)
            .clip(shape)
            .background(CardColor)
            .clickable(role = Role.Button, onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(32.dp).border(1.dp, AccentColor, CircleShape), contentAlignment = Alignment.Center) {
            Text(
                text = "Q",
                color = AccentColor,
                textAlign = TextAlign.Center,
                style = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.notification_app_name),
                    color = MetaColor,
                    style = systemStyle(11.5f, FontWeight.Medium),
                )
                Text(
                    stringResource(R.string.notification_now),
                    color = MetaColor,
                    style = systemStyle(11.5f, FontWeight.Medium),
                )
            }
            Text(
                text = title,
                color = TitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = systemStyle(14f, FontWeight.SemiBold),
            )
            Text(
                text,
                color = BodyColor,
                style = systemStyle(13f, FontWeight.Normal).copy(fontFeatureSettings = "tnum"),
            )
            if (kind == ScrapeNotificationKind.PROGRESS) {
                ProgressBar(fraction = current / total.coerceAtLeast(1).toFloat(), Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.15f)),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(AccentColor))
    }
}

private fun systemStyle(sizeSp: Float, weight: FontWeight) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = sizeSp.sp,
    fontWeight = weight,
    lineHeight = (sizeSp * 1.3f).sp,
)

/**
 * Overlay di atas semua layar (independen dari `activePopup`): turun dari atas dengan animasi qlDrop (0,35 dtk).
 * [notification] null = disembunyikan; teks terakhir dipertahankan selama animasi keluar.
 */
@Composable
fun ScrapeNotificationHost(
    notification: ScrapeNotificationUi?,
    onTap: (ScrapeNotificationUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var last by remember { mutableStateOf(notification) }
    LaunchedEffect(notification) { if (notification != null) last = notification }
    AnimatedVisibility(
        visible = notification != null,
        modifier = modifier,
        enter = slideInVertically(tween(350)) { -it },
        exit = slideOutVertically(tween(250)) { -it } + fadeOut(tween(250)),
    ) {
        (notification ?: last)?.let { n ->
            ScrapeNotification(
                kind = n.kind,
                articleTitle = n.articleTitle,
                current = n.current,
                total = n.total,
                failedCount = n.failedCount,
                onTap = { onTap(n) },
                modifier = Modifier.statusBarsPadding().padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F2F2, widthDp = 390)
@Composable
private fun ScrapeNotificationVariantsPreview() {
    QuestLogTheme {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val title = "Panduan Class & Monster Heart"
            ScrapeNotification(
                ScrapeNotificationKind.PROGRESS,
                title,
                current = 2,
                total = 6,
                failedCount = 0,
                onTap = {},
            )
            ScrapeNotification(ScrapeNotificationKind.DONE, title, current = 6, total = 6, failedCount = 0, onTap = {})
            ScrapeNotification(
                ScrapeNotificationKind.PARTIAL,
                title,
                current = 6,
                total = 6,
                failedCount = 1,
                onTap = {},
            )
            ScrapeNotification(ScrapeNotificationKind.ERROR, title, current = 6, total = 6, failedCount = 6, onTap = {})
        }
    }
}
