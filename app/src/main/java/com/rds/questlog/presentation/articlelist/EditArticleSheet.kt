package com.rds.questlog.presentation.articlelist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.addarticle.FieldLabel
import com.rds.questlog.presentation.addarticle.exactGame
import com.rds.questlog.presentation.addarticle.matchingGames
import com.rds.questlog.presentation.addarticle.normalizeGameName
import com.rds.questlog.presentation.components.QlBottomSheet
import com.rds.questlog.presentation.components.QlGhostButton
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.components.QlOutlineButton
import com.rds.questlog.presentation.components.QlTextField
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameUiModel
import com.rds.questlog.presentation.preview.PreviewData
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Ubah judul dan/atau pindahkan artikel ke game lain (component-contract.md §12). [saving] dan [saveError]
 * dikendalikan pemanggil; limit tier gratis dihitung di sini dari [games] ([GameUiModel.articleCount]).
 */
@Composable
fun EditArticleSheet(
    article: ArticleUiModel,
    games: List<GameUiModel>,
    isPremium: Boolean,
    saving: Boolean,
    saveError: Boolean,
    onSave: (EditArticlePayload) -> Unit,
    onOpenUnlock: () -> Unit,
    onClose: () -> Unit,
) {
    var form by remember(article.id) { mutableStateOf(EditArticleForm.of(article, games)) }
    var titleError by remember(article.id) { mutableStateOf(false) }
    val limit = editLimit(article, games, form, isPremium)
    val canSave = !saving && form.hasGame && limit == null
    QlBottomSheet(onDismiss = onClose) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            EditHeader()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel(stringResource(R.string.edit_article_label_title))
                QlTextField(
                    value = form.title,
                    onValueChange = {
                        form = form.copy(title = it)
                        titleError = false
                    },
                    errorText = if (titleError) stringResource(R.string.edit_article_error_title) else null,
                )
            }
            EditGameField(article, games, isPremium, form, limit) { form = it }
            if (limit != null) LimitBox(limit, onOpenUnlock)
            if (saveError) {
                Text(
                    stringResource(R.string.edit_article_error_save),
                    color = QuestLogTheme.colors.danger,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                QlGhostButton(stringResource(R.string.edit_article_cancel), onClose)
                SaveButton(saving, canSave) {
                    if (form.title.isBlank()) {
                        titleError = true
                    } else {
                        onSave(form.toPayload())
                    }
                }
            }
        }
    }
}

private fun EditArticleForm.toPayload() = EditArticlePayload(
    title = title.trim(),
    gameId = if (newGame) null else gameId,
    newGameName = if (newGame) normalizeGameName(gameInput) else null,
)

@Composable
private fun EditHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.edit_article_kicker).uppercase(),
            color = QuestLogTheme.colors.accentText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.26.sp,
            ),
        )
        Text(
            text = stringResource(R.string.edit_article_title),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = CormorantGaramond,
                fontSize = 26.sp,
                lineHeight = 30.sp,
            ),
        )
    }
}

@Composable
private fun EditGameField(
    article: ArticleUiModel,
    games: List<GameUiModel>,
    isPremium: Boolean,
    form: EditArticleForm,
    limit: EditLimit?,
    onChange: (EditArticleForm) -> Unit,
) {
    val c = QuestLogTheme.colors
    val picked = form.hasGame
    val exact = exactGame(games, form.gameInput)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(stringResource(R.string.add_article_label_game))
        Box(contentAlignment = Alignment.CenterEnd) {
            QlTextField(
                value = form.gameInput,
                onValueChange = { onChange(form.copy(gameInput = it, gameId = null, newGame = false)) },
                placeholder = stringResource(R.string.add_article_game_placeholder),
            )
            if (picked) {
                Icon(
                    QlIcons.Check,
                    contentDescription = null,
                    tint = c.accentLine,
                    modifier = Modifier.padding(end = 12.dp).size(18.dp),
                )
            }
        }
        if (!picked) {
            GameDropdown(games, form.gameInput, isPremium, exact == null, onChange = onChange, form = form)
        }
        EditGameHint(article, games, form, limit)?.let {
            Text(it, color = c.textSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp))
        }
    }
}

@Composable
private fun GameDropdown(
    games: List<GameUiModel>,
    input: String,
    isPremium: Boolean,
    canCreate: Boolean,
    form: EditArticleForm,
    onChange: (EditArticleForm) -> Unit,
) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(4.dp)
    Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, c.divider, shape).background(c.surface)) {
        matchingGames(games, input).forEach { game ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) {
                        onChange(form.copy(gameInput = game.name, gameId = game.id, newGame = false))
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = game.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = if (isPremium) {
                        stringResource(R.string.add_article_game_count_premium, game.articleCount)
                    } else {
                        stringResource(R.string.add_article_game_count_free, game.articleCount)
                    },
                    color = c.textTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            HorizontalDivider(color = c.divider)
        }
        if (canCreate && input.isNotBlank()) {
            val name = normalizeGameName(input)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) {
                        onChange(form.copy(gameInput = name, gameId = null, newGame = true))
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(QlIcons.PlusSmall, null, tint = c.accentText, modifier = Modifier.size(16.dp))
                Text(
                    stringResource(R.string.add_article_create_game, name),
                    color = c.accentText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

/** Petunjuk di bawah field game (EditArticleSheet.dc.html); null bila game tidak berubah atau limit dilanggar. */
@Composable
private fun EditGameHint(
    article: ArticleUiModel,
    games: List<GameUiModel>,
    form: EditArticleForm,
    limit: EditLimit?,
): String? {
    val moved = form.newGame || (form.gameId != null && form.gameId != article.gameId)
    val oldEmpty = stringResource(R.string.edit_article_hint_old_empty)
    return when {
        limit != null || !moved -> null
        form.newGame -> stringResource(R.string.edit_article_hint_new) +
            if (oldGameBecomesEmpty(article, games)) " $oldEmpty" else ""
        oldGameBecomesEmpty(article, games) -> oldEmpty
        else -> stringResource(R.string.edit_article_hint_same)
    }
}

@Composable
private fun LimitBox(limit: EditLimit, onUpgrade: () -> Unit) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.accentTint)
            .border(1.dp, c.accentSoft, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(QlIcons.Lock, null, tint = c.accentText, modifier = Modifier.padding(top = 2.dp).size(16.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val message = when (limit) {
                EditLimit.GAME -> R.string.edit_article_limit_game
                EditLimit.ARTICLES -> R.string.edit_article_limit_articles
            }
            val style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp)
            Text(stringResource(message), color = c.accentText, style = style, modifier = Modifier.weight(1f, false))
            Text(
                text = stringResource(R.string.add_article_upgrade),
                color = c.accentText,
                modifier = Modifier.clickable(role = Role.Button, onClick = onUpgrade),
                style = style.copy(fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline),
            )
        }
    }
}

@Composable
private fun SaveButton(saving: Boolean, enabled: Boolean, onClick: () -> Unit) {
    QlOutlineButton(
        text = stringResource(R.string.edit_article_save),
        onClick = onClick,
        enabled = enabled || saving,
        modifier = Modifier.padding(horizontal = 6.dp),
        loading = saving,
    )
}

private val PreviewArticle = PreviewData.articles.first { it.id == 2L }

@Preview(showBackground = true, widthDp = 390, heightDp = 640)
@Composable
private fun EditArticleSheetPreview() {
    QuestLogTheme {
        EditArticleSheet(PreviewArticle, PreviewData.games, false, false, false, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 640)
@Composable
private fun EditArticleSheetSavingPreview() {
    QuestLogTheme {
        EditArticleSheet(PreviewArticle, PreviewData.games, true, saving = true, saveError = false, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 640)
@Composable
private fun EditArticleSheetErrorPreview() {
    QuestLogTheme {
        EditArticleSheet(PreviewArticle, PreviewData.games, true, saving = false, saveError = true, {}, {}, {})
    }
}
