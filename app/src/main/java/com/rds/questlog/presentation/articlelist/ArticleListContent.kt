package com.rds.questlog.presentation.articlelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameUiModel
import com.rds.questlog.presentation.preview.PreviewData
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Isi layar S1 tanpa state sendiri; props dan callback mengikuti component-contract.md §1.
 * [articles] adalah SELURUH artikel: search ([query]) dan filter ([filterGameId]) diterapkan di sini
 * (seperti prototipe), jumlah "walkthrough" di subjudul tetap menghitung semuanya.
 */
@Composable
fun ArticleListContent(
    games: List<GameUiModel>,
    articles: List<ArticleUiModel>,
    isPremium: Boolean,
    query: String,
    filterGameId: Long?,
    snackbar: String?,
    onQuery: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onArticleTap: (ArticleUiModel) -> Unit,
    onResume: (ArticleUiModel) -> Unit,
    onOpenActions: (ArticleUiModel) -> Unit,
    onAdd: () -> Unit,
    onOpenUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = remember(articles, query, filterGameId) { filterArticles(articles, query, filterGameId) }
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ArticleListHeader(
                games = games,
                totalArticles = articles.size,
                visibleArticles = visible.size,
                isPremium = isPremium,
                query = query,
                filterGameId = filterGameId,
                onQuery = onQuery,
                onOpenFilter = onOpenFilter,
                onOpenUnlock = onOpenUnlock,
            )
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 110.dp)) {
                items(visible, key = { it.id }) { article ->
                    ArticleRow(
                        article = article,
                        onTap = { onArticleTap(article) },
                        onResume = { onResume(article) },
                        onOpenActions = { onOpenActions(article) },
                    )
                }
                if (visible.isEmpty()) {
                    item { EmptyState(hasArticles = articles.isNotEmpty()) }
                }
            }
        }
        AddFab(onAdd, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 28.dp))
        ArticleListSnackbar(snackbar, Modifier.align(Alignment.BottomStart).navigationBarsPadding())
    }
}

@Composable
private fun ArticleListHeader(
    games: List<GameUiModel>,
    totalArticles: Int,
    visibleArticles: Int,
    isPremium: Boolean,
    query: String,
    filterGameId: Long?,
    onQuery: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onOpenUnlock: () -> Unit,
) {
    val c = QuestLogTheme.colors
    Column(
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = TextStyle(
                        fontFamily = CormorantGaramond,
                        fontWeight = FontWeight.Medium,
                        fontSize = 38.sp,
                        lineHeight = 38.sp,
                        letterSpacing = (-0.02).em,
                    ),
                )
                Text(
                    text = stringResource(R.string.article_list_subtitle, totalArticles),
                    color = c.textSecondary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontStyle = FontStyle.Italic),
                )
            }
            TierBadge(isPremium, games.size, onOpenUnlock)
        }
        SearchField(query, onQuery)
        val allGamesLabel = stringResource(R.string.article_list_filter_all)
        FilterRow(
            label = games.firstOrNull { it.id == filterGameId }?.name ?: allGamesLabel,
            active = filterGameId != null,
            count = visibleArticles,
            onClick = onOpenFilter,
        )
    }
}

@Composable
private fun TierBadge(isPremium: Boolean, gamesUsed: Int, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier = Modifier
            .height(30.dp)
            .clip(shape)
            .background(if (pressed) c.accentTint else Color.Transparent)
            .border(1.dp, if (isPremium) c.accentLine else c.accentLine.copy(alpha = 0.55f), shape)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!isPremium) {
            Icon(QlIcons.Lock, contentDescription = null, tint = c.accentText, modifier = Modifier.size(12.dp))
        }
        Text(
            text = if (isPremium) {
                stringResource(R.string.article_list_tier_unlimited)
            } else {
                stringResource(R.string.article_list_tier_free, gamesUsed)
            },
            color = c.accentText,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
        )
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    val c = QuestLogTheme.colors
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground,
        ),
        cursorBrush = SolidColor(c.accentLine),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f), RoundedCornerShape(4.dp))
                    .padding(start = 12.dp, end = if (query.isEmpty()) 12.dp else 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(QlIcons.Search, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.article_list_search_hint),
                            color = c.iconMuted,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                        )
                    }
                    inner()
                }
                if (query.isNotEmpty()) {
                    ClearButton { onQuery("") }
                }
            }
        },
    )
}

@Composable
private fun ClearButton(onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (pressed) c.hover else Color.Transparent)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            QlIcons.Close,
            contentDescription = stringResource(R.string.article_list_search_clear),
            tint = c.textSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun FilterRow(label: String, active: Boolean, count: Int, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape(17.dp)
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(shape)
                    .background(if (active || pressed) c.accentTint else Color.Transparent)
                    .border(
                        1.dp,
                        if (active) c.accentLine else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f),
                        shape,
                    )
                    .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
                    .padding(start = 12.dp, end = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                )
                Icon(
                    QlIcons.ChevronDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = stringResource(R.string.article_list_count, count).uppercase(),
                color = c.textSecondary,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.1.em,
                    fontFeatureSettings = "tnum",
                ),
            )
        }
        Column(Modifier.padding(top = 10.dp)) {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun EmptyState(hasArticles: Boolean) {
    val c = QuestLogTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(
                if (hasArticles) R.string.article_list_empty_nomatch_title else R.string.article_list_empty_none_title,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = CormorantGaramond,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                lineHeight = 29.sp,
            ),
        )
        Text(
            text = stringResource(
                if (hasArticles) R.string.article_list_empty_nomatch_body else R.string.article_list_empty_none_body,
            ),
            color = c.textSecondary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
        )
    }
}

@Composable
private fun AddFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(60.dp)
            .shadow(4.dp, CircleShape, ambientColor = c.snackbar, spotColor = c.snackbar)
            .clip(CircleShape)
            .background(if (pressed) c.accentTintActive else MaterialTheme.colorScheme.background)
            .border(1.dp, c.accentLine, CircleShape)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            QlIcons.Plus,
            contentDescription = stringResource(R.string.article_list_add),
            tint = Color(0xFFA06F24),
            modifier = Modifier.size(26.dp),
        )
    }
}

/** Toast: kiri 16dp, kanan 92dp (menyisakan ruang FAB), naik 12dp + fade 0,22 dtk (qlUp). */
@Composable
private fun ArticleListSnackbar(message: String?, modifier: Modifier = Modifier) {
    val c = QuestLogTheme.colors
    // Simpan teks terakhir agar tetap terbaca selama animasi keluar.
    var lastMessage by remember { mutableStateOf("") }
    LaunchedEffect(message) { if (message != null) lastMessage = message }
    val offsetPx = with(LocalDensity.current) { 12.dp.roundToPx() }
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { offsetPx },
        exit = fadeOut(tween(150)),
    ) {
        Box(
            Modifier
                .padding(start = 16.dp, end = 92.dp, bottom = 32.dp)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .background(c.snackbar, RoundedCornerShape(4.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = message ?: lastMessage,
                color = c.snackbarText,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ArticleListContentPreview() {
    PreviewContent(query = "", filterGameId = null, snackbar = null)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ArticleListContentFilteredPreview() {
    PreviewContent(
        query = "walk",
        filterGameId = 2L,
        snackbar = stringResource(R.string.article_list_snackbar_scraping),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ArticleListContentEmptyPreview() {
    QuestLogTheme {
        ArticleListContent(
            games = emptyList(),
            articles = emptyList(),
            isPremium = true,
            query = "",
            filterGameId = null,
            snackbar = null,
            onQuery = {},
            onOpenFilter = {},
            onArticleTap = {},
            onResume = {},
            onOpenActions = {},
            onAdd = {},
            onOpenUnlock = {},
        )
    }
}

@Composable
private fun PreviewContent(query: String, filterGameId: Long?, snackbar: String?) {
    QuestLogTheme {
        ArticleListContent(
            games = PreviewData.games,
            articles = PreviewData.articles,
            isPremium = false,
            query = query,
            filterGameId = filterGameId,
            snackbar = snackbar,
            onQuery = {},
            onOpenFilter = {},
            onArticleTap = {},
            onResume = {},
            onOpenActions = {},
            onAdd = {},
            onOpenUnlock = {},
        )
    }
}
