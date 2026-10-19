package com.rds.questlog.presentation.gamelist

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlAddFab
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.components.QlOutlineButton
import com.rds.questlog.presentation.components.QlSearchField
import com.rds.questlog.presentation.components.QlToast
import com.rds.questlog.presentation.gamelogo.GameLogoControls
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameUiModel
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.preview.PreviewData
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Isi layar S0 Game List (home) tanpa state sendiri (component-contract.md §0). Bila [query] terisi daftar berubah
 * menjadi hasil pencarian artikel lintas game ([results]); empty state awal bila belum ada game.
 */
@Composable
fun GameListContent(
    games: List<GameUiModel>,
    results: List<ArticleUiModel>,
    totalArticles: Int,
    premium: PremiumUiState,
    query: String,
    snackbar: String?,
    actions: GameListActions,
    modifier: Modifier = Modifier,
    logo: GameLogoControls = GameLogoControls.None,
) {
    val searching = query.isNotBlank()
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            GameListHeader(games.size, totalArticles, premium, query, searching, results.size, actions)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 110.dp)) {
                when {
                    searching && results.isEmpty() -> item { NoResults(query.trim()) }
                    searching -> items(results, key = { it.id }) { ResultRow(it) { actions.onResultTap(it) } }
                    games.isEmpty() -> item { EmptyState(actions.onAdd) }
                    else -> items(games, key = { it.id }) { game -> GameRow(game, premium, actions, logo) }
                }
            }
        }
        QlAddFab(
            actions.onAdd,
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 28.dp),
        )
        QlToast(snackbar, Modifier.align(Alignment.BottomStart).navigationBarsPadding())
    }
}

@Composable
private fun GameListHeader(
    gameCount: Int,
    totalArticles: Int,
    premium: PremiumUiState,
    query: String,
    searching: Boolean,
    resultCount: Int,
    actions: GameListActions,
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
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Maskot pixel-art tanpa kotak; filter None menjaga piksel tetap tajam.
                Image(
                    bitmap = ImageBitmap.imageResource(R.drawable.questlog_mascot),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None,
                )
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
                        text = stringResource(R.string.game_list_subtitle, totalArticles, gameCount),
                        color = c.textSecondary,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontStyle = FontStyle.Italic),
                    )
                }
            }
            TierBadge(premium, gameCount, actions.onOpenUnlock)
        }
        QlSearchField(query, stringResource(R.string.game_list_search_hint), actions.onQuery)
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val label = if (searching) R.string.game_list_section_results else R.string.game_list_section_games
                Text(
                    text = stringResource(label).uppercase(),
                    color = c.textSecondary,
                    style = labelStyle(),
                )
                Text(
                    text = if (searching) {
                        stringResource(R.string.article_list_count, resultCount)
                    } else {
                        stringResource(R.string.game_list_count_games, gameCount)
                    }.uppercase(),
                    color = c.textSecondary,
                    style = labelStyle().copy(fontFeatureSettings = "tnum"),
                )
            }
            HorizontalDivider(
                Modifier.padding(top = 10.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun labelStyle() = MaterialTheme.typography.labelSmall.copy(
    fontSize = 11.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.1.em,
)

/** Badge tier: "n/2 game" + gembok bila gratis, "Unlimited" bila premium, "Premium nonaktif" bila hilang. */
@Composable
private fun TierBadge(premium: PremiumUiState, gameCount: Int, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier = Modifier
            .height(30.dp)
            .clip(shape)
            .background(if (pressed) c.accentTint else Color.Transparent)
            .border(
                1.dp,
                if (premium == PremiumUiState.PREMIUM) c.accentLine else c.accentLine.copy(alpha = 0.55f),
                shape,
            )
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (premium == PremiumUiState.FREE) {
            Icon(QlIcons.Lock, contentDescription = null, tint = c.accentText, modifier = Modifier.size(12.dp))
        }
        Text(
            text = when (premium) {
                PremiumUiState.PREMIUM -> stringResource(R.string.article_list_tier_unlimited)
                PremiumUiState.LOST -> stringResource(R.string.game_list_tier_lost)
                PremiumUiState.FREE -> stringResource(R.string.article_list_tier_free, gameCount)
            },
            color = c.accentText,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
        )
    }
}

@Composable
private fun NoResults(query: String) {
    CenteredMessage(
        title = stringResource(R.string.game_list_no_results_title),
        body = stringResource(R.string.game_list_no_results_body, query),
        titleSize = 24,
        verticalPadding = 64,
    )
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    CenteredMessage(
        title = stringResource(R.string.game_list_empty_title),
        body = stringResource(R.string.game_list_empty_body),
        titleSize = 26,
        verticalPadding = 72,
    ) {
        QlOutlineButton(
            stringResource(R.string.game_list_empty_action),
            onAdd,
            Modifier.padding(top = 10.dp).height(46.dp),
        )
    }
}

@Composable
private fun CenteredMessage(
    title: String,
    body: String,
    titleSize: Int,
    verticalPadding: Int,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = verticalPadding.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = CormorantGaramond,
                fontWeight = FontWeight.Medium,
                fontSize = titleSize.sp,
                lineHeight = (titleSize + 4).sp,
            ),
        )
        Text(
            text = body,
            color = QuestLogTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
        )
        action()
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GameListFreePreview() = Preview(PremiumUiState.FREE)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GameListPremiumPreview() = Preview(PremiumUiState.PREMIUM)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GameListLostPreview() = Preview(PremiumUiState.LOST)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GameListManyGamesPreview() = Preview(PremiumUiState.PREMIUM, games = PreviewData.manyGames)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GameListSearchPreview() = Preview(
    PremiumUiState.PREMIUM,
    query = "walk",
    snackbar = "Artikel utama masih diproses",
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GameListNoResultsPreview() = Preview(PremiumUiState.FREE, query = "zzz")

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GameListEmptyPreview() = Preview(PremiumUiState.FREE, games = emptyList())

@Composable
private fun Preview(
    premium: PremiumUiState,
    games: List<GameUiModel> = PreviewData.games,
    query: String = "",
    snackbar: String? = null,
) {
    QuestLogTheme {
        GameListContent(
            games = games,
            results = if (query.isBlank()) {
                emptyList()
            } else {
                PreviewData.articles.filter {
                    it.title.contains(query, ignoreCase = true)
                }
            },
            totalArticles = games.sumOf { it.articleCount },
            premium = premium,
            query = query,
            snackbar = snackbar,
            actions = GameListActions({}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}
