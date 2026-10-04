package com.rds.questlog.presentation.addarticle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.components.QlRadio
import com.rds.questlog.presentation.components.QlTextField
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameUiModel
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QuestLogTheme

@Composable
internal fun GameField(
    form: AddArticleForm,
    games: List<GameUiModel>,
    articles: List<ArticleUiModel>,
    isPremium: Boolean,
    error: GameError?,
    onInput: (String) -> Unit,
    onPick: (GameUiModel) -> Unit,
    onCreate: () -> Unit,
    onOpenUnlock: () -> Unit,
) {
    val c = QuestLogTheme.colors
    val picked = form.gameId != null || form.newGame
    val exact = exactGame(games, form.gameInput)
    val matches = matchingGames(games, form.gameInput)
    // Daftar dibuka selama user mengetik dan belum memilih; dropdown inline agar ikut scroll dan tidak menutupi tombol.
    val showList = !picked && form.gameInput.isNotBlank()
    val errorText = error?.let { stringResource(it.messageRes()) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(stringResource(R.string.add_article_label_game))
        QlTextField(
            value = form.gameInput,
            onValueChange = onInput,
            placeholder = stringResource(R.string.add_article_game_placeholder),
            errorText = errorText,
        )
        if (showList) {
            val shape = RoundedCornerShape(6.dp)
            Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, c.divider, shape).background(c.surface)) {
                matches.forEach { game ->
                    GameOption(game, isPremium, articles.count { it.gameId == game.id }) { onPick(game) }
                    HorizontalDivider(color = c.divider)
                }
                if (exact == null) {
                    CreateGameOption(
                        normalizeGameName(form.gameInput),
                        canAddGame(games, isPremium),
                        onCreate,
                        onOpenUnlock,
                    )
                }
            }
        }
        val hint = when {
            form.newGame -> stringResource(R.string.add_article_game_new_hint)
            form.gameId != null ->
                stringResource(R.string.add_article_game_selected_hint, articles.count { it.gameId == form.gameId })
            else -> stringResource(R.string.add_article_game_empty_hint)
        }
        if (error == null) Text(hint, color = c.textTertiary, style = MaterialTheme.typography.bodySmall)
    }
}

private fun GameError.messageRes(): Int = when (this) {
    GameError.EMPTY -> R.string.add_article_error_game_empty
    GameError.NOT_SELECTED -> R.string.add_article_error_game_not_selected
    GameError.TOO_LONG -> R.string.add_article_error_game_too_long
}

@Composable
private fun CreateGameOption(name: String, canCreate: Boolean, onCreate: () -> Unit, onOpenUnlock: () -> Unit) {
    val c = QuestLogTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .alpha(if (canCreate) 1f else 0.6f)
            .clickable(enabled = canCreate, role = Role.Button, onClick = onCreate)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(QlIcons.PlusSmall, null, tint = c.accentText, modifier = Modifier.size(16.dp))
            Text(
                stringResource(R.string.add_article_create_game, name),
                color = c.accentText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
        }
        if (!canCreate) GameLimitHint(onOpenUnlock)
    }
}

@Composable
private fun GameLimitHint(onOpenUnlock: () -> Unit) {
    val c = QuestLogTheme.colors
    Row(Modifier.padding(top = 4.dp, start = 24.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.add_article_game_limit_inline),
            color = c.textSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            stringResource(R.string.add_article_upgrade),
            color = c.accentText,
            modifier = Modifier.clickable(role = Role.Button, onClick = onOpenUnlock),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
            ),
        )
    }
}

@Composable
private fun GameOption(game: GameUiModel, isPremium: Boolean, count: Int, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(game.name, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = if (isPremium) {
                stringResource(R.string.add_article_game_count_premium, count)
            } else {
                stringResource(R.string.add_article_game_count_free, count)
            },
            color = c.textTertiary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
internal fun AppendTargetSection(
    articles: List<ArticleUiModel>,
    games: List<GameUiModel>,
    targetId: Long?,
    hasError: Boolean,
    onPick: (Long) -> Unit,
) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(6.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(stringResource(R.string.add_article_label_pick_article))
        appendTargets(articles).forEach { article ->
            val selected = article.id == targetId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(if (selected) c.accentTint else Color.Transparent)
                    .border(1.dp, if (selected) c.accentLine else c.divider, shape)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(article.id) })
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QlRadio(selected)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = (games.firstOrNull { it.id == article.gameId }?.name ?: article.gameName).uppercase(),
                        color = c.accentText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 1.05.sp),
                    )
                    Text(
                        text = article.title,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = CormorantGaramond,
                            fontSize = 18.sp,
                        ),
                    )
                }
                Text(
                    stringResource(R.string.add_article_target_pages, article.totalPageCount),
                    color = c.textTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (hasError) {
            Text(
                stringResource(R.string.add_article_error_target),
                color = c.danger,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            )
        }
    }
}

@Composable
internal fun UrlSection(
    form: AddArticleForm,
    target: ArticleUiModel?,
    errors: Map<Int, UrlError>,
    onChange: (Int, String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val c = QuestLogTheme.colors
    val isAppend = form.mode == com.rds.questlog.presentation.model.ScrapeMode.APPEND
    val offset = urlPageOffset(target)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FieldLabel(
                stringResource(if (isAppend) R.string.add_article_urls_append else R.string.add_article_urls_new),
            )
            Text(
                stringResource(R.string.add_article_url_count, form.urls.size),
                color = c.textTertiary,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
        }
        if (isAppend) {
            Text(
                text = if (target != null) {
                    stringResource(R.string.add_article_append_note_target, target.totalPageCount)
                } else {
                    stringResource(R.string.add_article_append_note_none)
                },
                color = c.textSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        form.urls.forEachIndexed { index, url ->
            UrlRow(
                page = offset + index + 1,
                url = url,
                error = errors[index],
                canUp = index > 0,
                canDown = index < form.urls.lastIndex,
                onChange = { onChange(index, it) },
                onRemove = { onRemove(index) },
                onMove = { onMove(index, it) },
            )
        }
        if (form.urls.size < MAX_URLS) AddUrlButton(onAdd)
        Text(
            stringResource(R.string.add_article_paste_hint),
            color = c.textTertiary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun UrlRow(
    page: Int,
    url: String,
    error: UrlError?,
    canUp: Boolean,
    canDown: Boolean,
    onChange: (String) -> Unit,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit,
) {
    val c = QuestLogTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.add_article_url_page, page),
                color = c.textTertiary,
                modifier = Modifier.width(44.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
            QlTextField(
                value = url,
                onValueChange = onChange,
                modifier = Modifier.weight(1f),
                placeholder = stringResource(R.string.add_article_url_placeholder),
                isError = error != null,
                height = 44.dp,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.5.sp),
            )
            Column {
                MoveButton(QlIcons.ChevronUp, R.string.add_article_url_up, canUp) { onMove(-1) }
                MoveButton(QlIcons.ChevronDownSmall, R.string.add_article_url_down, canDown) { onMove(1) }
            }
            Box(
                Modifier.size(width = 36.dp, height = 44.dp).clickable(role = Role.Button, onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    QlIcons.Close,
                    contentDescription = stringResource(R.string.add_article_url_remove),
                    tint = c.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (error != null) {
            Text(
                text = urlErrorText(error),
                color = c.danger,
                modifier = Modifier.padding(start = 52.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            )
        }
    }
}

@Composable
private fun urlErrorText(error: UrlError): String = when (error) {
    UrlError.NoUrls -> stringResource(R.string.add_article_error_url_none)
    UrlError.Empty -> stringResource(R.string.add_article_error_url_empty)
    UrlError.InvalidFormat -> stringResource(R.string.add_article_error_url_invalid)
    is UrlError.DuplicateOf -> stringResource(R.string.add_article_error_url_duplicate, error.page)
    is UrlError.ExistsInArticle -> stringResource(R.string.add_article_error_url_exists, error.page)
}

@Composable
private fun MoveButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(width = 28.dp, height = 22.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            stringResource(description),
            tint = QuestLogTheme.colors.textSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun AddUrlButton(onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val stroke = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    c.accentSoft,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(8f),
                    style = stroke,
                )
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(QlIcons.PlusSmall, null, tint = c.accentText, modifier = Modifier.size(16.dp))
        Text(
            stringResource(R.string.add_article_add_url),
            color = c.accentText,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
