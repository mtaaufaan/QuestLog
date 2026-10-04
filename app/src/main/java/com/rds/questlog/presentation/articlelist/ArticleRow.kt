package com.rds.questlog.presentation.articlelist

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlBadge
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Satu baris artikel di S1. Tap = [onTap], tekan-tahan atau titik tiga = [onOpenActions],
 * tombol "Lanjutkan Baca" = [onResume] (hanya READY dengan checkpoint).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ArticleRow(
    article: ArticleUiModel,
    onTap: () -> Unit,
    onResume: () -> Unit,
    onOpenActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val isReady = article.status == ArticleStatus.READY

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (article.status == ArticleStatus.SCRAPING) 0.85f else 1f)
            .background(if (pressed) c.pressed else Color.Transparent)
            .combinedClickable(
                interactionSource = source,
                indication = null,
                onLongClickLabel = stringResource(R.string.article_list_actions),
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenActions()
                },
                onClick = onTap,
            )
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    c.divider,
                    Offset(0f, size.height - stroke / 2),
                    Offset(size.width, size.height - stroke / 2),
                    stroke,
                )
            }
            .height(IntrinsicSize.Min)
            .padding(start = 20.dp, top = 16.dp, end = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = article.gameName.uppercase(),
                color = c.accentText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.26.sp,
                ),
            )
            Text(
                text = article.title,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = CormorantGaramond,
                    fontSize = 21.sp,
                    lineHeight = 25.sp,
                ),
            )
            when (article.status) {
                ArticleStatus.READY -> FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = readyMeta(article),
                        color = c.textSecondary,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 17.sp),
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    if (article.isPartial) {
                        QlBadge(
                            stringResource(R.string.article_list_partial, article.failedPageCount),
                            modifier = Modifier.align(Alignment.CenterVertically),
                            color = c.danger,
                        )
                    }
                }
                ArticleStatus.SCRAPING -> ScrapingProgress(article)
                ArticleStatus.ERROR -> StatusLine(
                    dotColor = c.danger,
                    text = stringResource(R.string.article_list_error_row),
                    textColor = c.danger,
                )
            }
            if (isReady && article.checkpointNodeId != null) {
                ResumeButton(onResume, Modifier.padding(top = 5.dp).align(Alignment.Start))
            }
        }
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            MoreButton(onOpenActions)
            if (isReady) {
                Icon(
                    QlIcons.ChevronRight,
                    contentDescription = null,
                    tint = c.iconMuted,
                    modifier = Modifier.padding(bottom = 6.dp).size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun readyMeta(article: ArticleUiModel): String {
    val pages = if (article.failedPageCount > 0) {
        "${article.donePageCount}/${article.totalPageCount}"
    } else {
        article.totalPageCount.toString()
    }
    val lastRead = article.lastRead ?: stringResource(R.string.article_list_meta_unread)
    return stringResource(R.string.article_list_meta, pages, lastRead)
}

@Composable
private fun StatusLine(dotColor: Color, text: String, textColor: Color, dotAlpha: Float = 1f) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).alpha(dotAlpha).background(dotColor, CircleShape))
        Text(text, color = textColor, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp))
    }
}

/** Titik berdenyut (qlPulse 1,2 dtk), label "Memproses… n/total", dan bar progres 2dp. */
@Composable
private fun ScrapingProgress(article: ArticleUiModel) {
    val c = QuestLogTheme.colors
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    val total = article.totalPageCount.coerceAtLeast(1)
    val fraction by animateFloatAsState(article.handledPageCount / total.toFloat(), tween(500), label = "progress")
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        StatusLine(
            dotColor = c.accentLine,
            text = stringResource(R.string.article_list_progress, article.handledPageCount, article.totalPageCount),
            textColor = c.accentText,
            dotAlpha = pulse,
        )
        Box(
            Modifier
                .widthIn(max = 220.dp)
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(c.track),
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(c.accentLine))
        }
    }
}

@Composable
private fun ResumeButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(shape)
            .background(if (pressed) c.accentTintActive else Color.Transparent)
            .border(1.dp, c.accentLine, shape)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(QlIcons.BookmarkFilled, contentDescription = null, modifier = Modifier.size(14.dp))
        Text(
            text = stringResource(R.string.article_list_resume),
            color = c.accentText,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MoreButton(onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (pressed) c.pressed else Color.Transparent)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            QlIcons.MoreVert,
            contentDescription = stringResource(R.string.article_list_actions),
            tint = c.textSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}
