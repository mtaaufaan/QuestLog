package com.rds.questlog.presentation.articlelist

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.rds.questlog.presentation.components.GameLogo
import com.rds.questlog.presentation.components.QlAddFab
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.components.QlSearchField
import com.rds.questlog.presentation.components.QlToast
import com.rds.questlog.presentation.gamelogo.GameLogoControls
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.preview.PreviewData
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Isi layar S1 Halaman Game tanpa state sendiri (component-contract.md §1). [articles] = hasil yang SUDAH disaring
 * database menurut [query] untuk game ini; [totalArticles] = jumlah seluruh artikel game (subjudul dan pilihan empty
 * state). [premium] menentukan ikon Binder (gembok saat gratis, tersembunyi saat premium hilang) dan pengelompokan
 * kategori (hanya premium).
 */
@Composable
fun ArticleListContent(
    gameName: String,
    articles: List<ArticleUiModel>,
    totalArticles: Int,
    premium: PremiumUiState,
    query: String,
    snackbar: String?,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onOpenBinder: () -> Unit,
    onArticleTap: (ArticleUiModel) -> Unit,
    onResume: (ArticleUiModel) -> Unit,
    onOpenActions: (ArticleUiModel) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    gameId: Long = 0,
    logoPath: String? = null,
    logo: GameLogoControls = GameLogoControls.None,
) {
    val uncategorized = stringResource(R.string.article_list_uncategorized)
    val sections = remember(articles, premium, query, uncategorized) {
        groupArticles(articles, enabled = premium == PremiumUiState.PREMIUM && query.isBlank(), uncategorized)
    }
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            GamePageHeader(
                gameName = gameName,
                logo = GameLogoSlot(gameId, logoPath, logo),
                subtitle = gameSubtitle(totalArticles, sections),
                premium = premium,
                query = query,
                onBack = onBack,
                onQuery = onQuery,
                onOpenBinder = onOpenBinder,
            )
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 110.dp)) {
                sections.forEach { section ->
                    section.title?.let { title ->
                        item(key = "section-$title") {
                            SectionHeader(title, section.articles.size, muted = title == uncategorized)
                        }
                    }
                    items(section.articles, key = { "${section.title}-${it.id}" }) { article ->
                        ArticleRow(
                            article = article,
                            onTap = { onArticleTap(article) },
                            onResume = { onResume(article) },
                            onOpenActions = { onOpenActions(article) },
                        )
                    }
                }
                if (articles.isEmpty()) {
                    item { EmptyState(hasArticles = query.isNotBlank() || totalArticles > 0) }
                }
            }
        }
        QlAddFab(
            onAdd,
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 28.dp),
        )
        QlToast(snackbar, Modifier.align(Alignment.BottomStart).navigationBarsPadding())
    }
}

/** Logo game di header S1: id, path, dan kendali ketuk/spinner. */
private class GameLogoSlot(val gameId: Long, val path: String?, val controls: GameLogoControls)

@Composable
private fun gameSubtitle(totalArticles: Int, sections: List<ArticleSection>): String {
    val uncategorized = stringResource(R.string.article_list_uncategorized)
    val categories = sections.count { it.title != null && it.title != uncategorized }
    return if (categories > 0) {
        stringResource(R.string.article_list_subtitle_categories, totalArticles, categories)
    } else {
        stringResource(R.string.article_list_subtitle_game, totalArticles)
    }
}

@Composable
private fun GamePageHeader(
    gameName: String,
    logo: GameLogoSlot,
    subtitle: String,
    premium: PremiumUiState,
    query: String,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onOpenBinder: () -> Unit,
) {
    val c = QuestLogTheme.colors
    Column(
        modifier = Modifier.padding(start = 8.dp, top = 6.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onBack)
            Text(
                text = stringResource(R.string.article_list_kicker).uppercase(),
                color = c.accentText,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.12.em,
                ),
            )
            if (premium != PremiumUiState.LOST) {
                BinderPill(
                    locked = premium == PremiumUiState.FREE,
                    onClick = onOpenBinder,
                )
            }
        }
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            GameLogo(
                name = gameName,
                logoPath = logo.path,
                size = 64.dp,
                cornerRadius = 8.dp,
                onClick = { logo.controls.onTap(logo.gameId, logo.path != null) },
                busy = logo.controls.busyGameId == logo.gameId,
            )
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = gameName,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = TextStyle(
                        fontFamily = CormorantGaramond,
                        fontWeight = FontWeight.Medium,
                        fontSize = 34.sp,
                        lineHeight = 36.sp,
                        letterSpacing = (-0.015).em,
                    ),
                )
                Text(
                    text = subtitle,
                    color = c.textSecondary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontStyle = FontStyle.Italic),
                )
            }
        }
        QlSearchField(
            query,
            stringResource(R.string.article_list_search_hint),
            onQuery,
            Modifier.padding(start = 12.dp),
        )
        HorizontalDivider(
            Modifier.padding(start = 12.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (pressed) c.hover else Color.Transparent)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            QlIcons.ArrowLeft,
            contentDescription = stringResource(R.string.article_list_back),
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Pil "Binder" (gembok bila gratis); tersembunyi saat premium hilang (pemanggil). */
@Composable
private fun BinderPill(locked: Boolean, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape(17.dp)
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(shape)
            .background(if (pressed) c.accentTint else Color.Transparent)
            .border(1.dp, if (locked) c.accentLine.copy(alpha = 0.55f) else c.accentLine, shape)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(QlIcons.Binder, contentDescription = null, tint = c.accentText, modifier = Modifier.size(15.dp))
        Text(
            text = stringResource(R.string.article_list_binder),
            color = c.accentText,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
        )
        if (locked) Icon(QlIcons.Lock, contentDescription = null, tint = c.accentText, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, muted: Boolean) {
    val c = QuestLogTheme.colors
    Column {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title.uppercase(),
                color = if (muted) c.textSecondary else c.accentText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.12.em,
                ),
            )
            Text(
                text = stringResource(R.string.article_list_count, count),
                color = c.textTertiary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontFeatureSettings = "tnum"),
            )
        }
        HorizontalDivider(thickness = 1.dp, color = c.divider)
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

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GamePageFreePreview() = PreviewContent(PremiumUiState.FREE)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GamePagePremiumPreview() = PreviewContent(PremiumUiState.PREMIUM)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GamePagePremiumCategoriesPreview() = PreviewContent(
    PremiumUiState.PREMIUM,
    articles = PreviewData.categorizedArticles,
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GamePageLostPreview() = PreviewContent(PremiumUiState.LOST)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GamePageSearchPreview() = PreviewContent(
    PremiumUiState.PREMIUM,
    query = "mini",
    snackbar = stringResource(R.string.article_list_snackbar_scraping),
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GamePageNoResultsPreview() = PreviewContent(PremiumUiState.PREMIUM, query = "zzz", articles = emptyList())

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GamePageEmptyPreview() = PreviewContent(PremiumUiState.FREE, articles = emptyList(), total = 0)

@Composable
private fun PreviewContent(
    premium: PremiumUiState,
    query: String = "",
    articles: List<ArticleUiModel> = PreviewData.articles.filter { it.gameId == 1L },
    total: Int = articles.size,
    snackbar: String? = null,
) {
    QuestLogTheme {
        ArticleListContent(
            gameName = "Dragon Quest VII",
            articles = articles.filter { it.title.contains(query, ignoreCase = true) },
            totalArticles = total,
            premium = premium,
            query = query,
            snackbar = snackbar,
            onBack = {},
            onQuery = {},
            onOpenBinder = {},
            onArticleTap = {},
            onResume = {},
            onOpenActions = {},
            onAdd = {},
        )
    }
}
