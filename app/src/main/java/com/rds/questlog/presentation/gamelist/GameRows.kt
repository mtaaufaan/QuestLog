package com.rds.questlog.presentation.gamelist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.GameLogo
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.gamelogo.GameLogoControls
import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameUiModel
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QuestLogTheme

/** Baris game: tap = Halaman Game, tekan-tahan = Delete Game Dialog. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GameRow(game: GameUiModel, premium: PremiumUiState, actions: GameListActions, logo: GameLogoControls) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (pressed) c.pressed else Color.Transparent)
            .combinedClickable(
                interactionSource = source,
                indication = null,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    actions.onGameLongPress(game)
                },
                onClick = { actions.onGameTap(game) },
            )
            .rowDivider(c.divider)
            .padding(start = 20.dp, top = 18.dp, end = 8.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameLogo(
                    name = game.name,
                    logoPath = game.logoPath,
                    size = 52.dp,
                    cornerRadius = 6.dp,
                    onClick = { logo.onTap(game.id, game.logoPath != null) },
                    busy = logo.busyGameId == game.id,
                )
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = game.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = TextStyle(
                            fontFamily = CormorantGaramond,
                            fontWeight = FontWeight.Medium,
                            fontSize = 26.sp,
                            lineHeight = 29.sp,
                            letterSpacing = (-0.01).em,
                        ),
                    )
                    Text(text = gameMeta(game), color = c.textSecondary, style = metaStyle())
                }
            }
            if (game.lastTitle != null && game.lastWhen != null) {
                Text(
                    text = stringResource(R.string.game_list_last, game.lastTitle, game.lastWhen),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = metaStyle().copy(fontStyle = FontStyle.Italic),
                )
            }
            game.mainArticleTitle?.let { ResumeGameButton(it) { actions.onResume(game) } }
        }
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (premium != PremiumUiState.LOST) {
                BinderButton(locked = premium == PremiumUiState.FREE) { actions.onOpenBinder(game) }
            }
            Icon(
                QlIcons.ChevronRight,
                contentDescription = null,
                tint = c.iconMuted,
                modifier = Modifier.padding(bottom = 4.dp).size(18.dp),
            )
        }
    }
}

@Composable
private fun metaStyle() = MaterialTheme.typography.bodySmall.copy(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontFeatureSettings = "tnum",
)

@Composable
private fun gameMeta(game: GameUiModel): String = if (game.scrapingCount > 0) {
    stringResource(R.string.game_list_meta_scraping, game.articleCount, game.scrapingCount)
} else {
    stringResource(R.string.game_list_meta, game.articleCount)
}

private fun Modifier.rowDivider(color: Color) = drawBehind {
    val stroke = 1.dp.toPx()
    drawLine(color, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
}

@Composable
private fun ResumeGameButton(mainTitle: String, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = Modifier
            .padding(top = 6.dp)
            .height(34.dp)
            .clip(shape)
            .border(1.dp, c.accentLine, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(QlIcons.Star, contentDescription = null, tint = Color(0xFFA06F24), modifier = Modifier.size(14.dp))
        Text(
            text = stringResource(R.string.game_list_resume),
            color = c.accentText,
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
        )
        Text(
            text = stringResource(R.string.game_list_resume_main, mainTitle),
            color = c.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
        )
    }
}

/** Ikon Game Binder (gembok kecil bila gratis). */
@Composable
private fun BinderButton(locked: Boolean, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            QlIcons.Binder,
            contentDescription = stringResource(R.string.game_list_binder),
            tint = c.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        if (locked) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 6.dp)
                    .size(15.dp)
                    .background(MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(QlIcons.Lock, contentDescription = null, tint = c.accentText, modifier = Modifier.size(10.dp))
            }
        }
    }
}

@Composable
internal fun ResultRow(article: ArticleUiModel, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .rowDivider(c.divider)
            .padding(start = 20.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = article.gameName.uppercase(),
                color = c.accentText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.12.em,
                ),
            )
            Text(
                text = article.title,
                color = MaterialTheme.colorScheme.onBackground,
                style = TextStyle(
                    fontFamily = CormorantGaramond,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 19.sp,
                    lineHeight = 23.sp,
                ),
            )
            val (status, color) = when (article.status) {
                ArticleStatus.READY -> stringResource(R.string.game_list_result_ready) to c.textSecondary
                ArticleStatus.SCRAPING -> stringResource(R.string.game_list_result_scraping) to c.accentText
                ArticleStatus.ERROR -> stringResource(R.string.article_list_error_row) to c.danger
            }
            Text(text = status, color = color, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp))
        }
        Icon(QlIcons.ChevronRight, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(18.dp))
    }
}
