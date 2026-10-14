package com.rds.questlog.presentation.binder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.components.QlToast
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme

/** Artikel di Binder: [categoryIds] = kategori game ini tempat artikel berada (boleh lebih dari satu). */
data class BinderArticleUi(val id: Long, val title: String, val categoryIds: Set<Long>)

/** Aksi pengguna di S4; satu objek agar [BinderContent] tidak membawa delapan callback terpisah. */
@Suppress("LongParameterList") // satu callback per aksi
class BinderActions(
    val onBack: () -> Unit,
    val onCreate: () -> Unit,
    val onRename: (Long) -> Unit,
    val onDelete: (Long) -> Unit,
    val onTogglePicker: (Long) -> Unit,
    val onSetMember: (articleId: Long, categoryId: Long, member: Boolean) -> Unit,
    val onArticleTap: (Long) -> Unit,
)

/**
 * Isi layar S4 Game Binder (component-contract.md §20) tanpa state sendiri. Setiap kategori menampilkan nama, jumlah,
 * ＋ (picker artikel), ganti nama, dan hapus; artikel boleh masuk beberapa kategori. "Tanpa kategori" otomatis dan
 * tidak bisa diubah. [readOnly] (premium hilang) menyembunyikan semua aksi ubah dan menampilkan banner.
 */
@Composable
fun BinderContent(
    gameName: String,
    categories: List<BinderCategoryUi>,
    articles: List<BinderArticleUi>,
    readOnly: Boolean,
    pickerCategoryId: Long?,
    snackbar: String?,
    actions: BinderActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            BinderHeader(gameName, actions.onBack)
            if (readOnly) ReadOnlyBanner()
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 110.dp)) {
                categories.forEach { category ->
                    val members = articles.filter { category.id in it.categoryIds }
                    item(key = "category-${category.id}") {
                        CategorySection(
                            category = category,
                            members = members,
                            allArticles = articles,
                            editable = !readOnly,
                            pickerOpen = pickerCategoryId == category.id && !readOnly,
                            actions = actions,
                        )
                    }
                }
                item(key = "uncategorized") {
                    UncategorizedSection(articles.filter { it.categoryIds.isEmpty() }, actions.onArticleTap)
                }
                item(key = "note") {
                    Text(
                        text = stringResource(R.string.binder_note),
                        color = QuestLogTheme.colors.textTertiary,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
                        style = TextStyle(
                            fontFamily = Lora,
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp,
                        ),
                    )
                }
            }
        }
        if (!readOnly) {
            CreateFab(
                actions.onCreate,
                Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 28.dp),
            )
        }
        QlToast(snackbar, Modifier.align(Alignment.BottomStart).navigationBarsPadding())
    }
}

@Composable
private fun BinderHeader(gameName: String, onBack: () -> Unit) {
    val c = QuestLogTheme.colors
    Column {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, top = 6.dp, end = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                QlIcons.ArrowLeft,
                stringResource(R.string.binder_back),
                MaterialTheme.colorScheme.onBackground,
                44,
                22,
                onBack,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.binder_title).uppercase(),
                    color = c.accentText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.12.em,
                    ),
                )
                Text(
                    text = gameName,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    style = TextStyle(
                        fontFamily = CormorantGaramond,
                        fontWeight = FontWeight.Medium,
                        fontSize = 28.sp,
                        lineHeight = 30.sp,
                    ),
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun ReadOnlyBanner() {
    val c = QuestLogTheme.colors
    Row(
        Modifier.fillMaxWidth().background(c.accentTint).padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(QlIcons.Lock, contentDescription = null, tint = c.accentText, modifier = Modifier.size(14.dp))
        Text(
            text = stringResource(R.string.binder_read_only),
            color = MaterialTheme.colorScheme.onBackground,
            style = TextStyle(fontFamily = Lora, fontSize = 12.5.sp, lineHeight = 18.sp),
        )
    }
}

@Composable
private fun CategorySection(
    category: BinderCategoryUi,
    members: List<BinderArticleUi>,
    allArticles: List<BinderArticleUi>,
    editable: Boolean,
    pickerOpen: Boolean,
    actions: BinderActions,
) {
    val c = QuestLogTheme.colors
    Column {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, top = 14.dp, end = 8.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(category.name, members.size, MaterialTheme.colorScheme.onBackground, Modifier.weight(1f))
            if (editable) {
                IconButton(QlIcons.Plus, stringResource(R.string.binder_add_articles), c.accentText, 40, 17) {
                    actions.onTogglePicker(category.id)
                }
                IconButton(QlIcons.Pencil, stringResource(R.string.binder_rename), c.textSecondary, 40, 16) {
                    actions.onRename(category.id)
                }
                IconButton(QlIcons.Trash, stringResource(R.string.binder_delete), c.danger, 40, 16) {
                    actions.onDelete(category.id)
                }
            }
        }
        if (pickerOpen) {
            ArticlePicker(category, allArticles, actions.onSetMember) { actions.onTogglePicker(category.id) }
        }
        members.forEach { article ->
            ArticleRowItem(article, others = article.categoryIds.size - 1, removable = editable, actions.onArticleTap) {
                actions.onSetMember(article.id, category.id, false)
            }
        }
        if (members.isEmpty()) EmptyCategory()
        HorizontalDivider(Modifier.padding(top = 8.dp), color = c.divider)
    }
}

@Composable
private fun SectionTitle(name: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    val c = QuestLogTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
        Text(
            text = name,
            color = color,
            modifier = Modifier.weight(1f, fill = false),
            style = TextStyle(
                fontFamily = CormorantGaramond,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 25.sp,
            ),
        )
        Text(
            text = stringResource(R.string.article_list_count, count),
            color = c.textTertiary,
            style = TextStyle(fontFamily = Lora, fontSize = 12.5.sp, fontFeatureSettings = "tnum"),
        )
    }
}

@Composable
private fun UncategorizedSection(members: List<BinderArticleUi>, onArticleTap: (Long) -> Unit) {
    val c = QuestLogTheme.colors
    Column {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, top = 14.dp, end = 8.dp, bottom = 6.dp)) {
            SectionTitle(stringResource(R.string.article_list_uncategorized), members.size, c.textSecondary)
        }
        members.forEach { ArticleRowItem(it, others = 0, removable = false, onArticleTap) {} }
        if (members.isEmpty()) EmptyCategory()
        HorizontalDivider(Modifier.padding(top = 8.dp), color = c.divider)
    }
}

@Composable
private fun EmptyCategory() {
    Text(
        text = stringResource(R.string.binder_empty_category),
        color = QuestLogTheme.colors.textTertiary,
        modifier = Modifier.padding(start = 34.dp, top = 6.dp, end = 20.dp, bottom = 8.dp),
        style = TextStyle(fontFamily = Lora, fontStyle = FontStyle.Italic, fontSize = 13.sp),
    )
}

@Composable
private fun ArticleRowItem(
    article: BinderArticleUi,
    others: Int,
    removable: Boolean,
    onTap: (Long) -> Unit,
    onRemove: () -> Unit,
) {
    val c = QuestLogTheme.colors
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).heightIn(min = 46.dp).clickable(role = Role.Button) { onTap(article.id) },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(4.dp).background(c.accentLine, CircleShape))
            Text(
                text = article.title,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                style = TextStyle(fontFamily = Lora, fontSize = 15.sp, lineHeight = 20.sp),
            )
            if (others > 0) {
                Text(
                    text = stringResource(R.string.binder_other_categories, others),
                    color = c.textTertiary,
                    style = TextStyle(fontFamily = Lora, fontSize = 11.5.sp),
                )
            }
        }
        if (removable) {
            IconButton(
                QlIcons.Close,
                stringResource(R.string.binder_remove_from_category),
                c.iconMuted,
                40,
                14,
                onRemove,
            )
        }
    }
}

/** Picker artikel satu kategori: kotak centang per artikel game ini, ditutup dengan "Selesai". */
@Composable
private fun ArticlePicker(
    category: BinderCategoryUi,
    articles: List<BinderArticleUi>,
    onSetMember: (Long, Long, Boolean) -> Unit,
    onDone: () -> Unit,
) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(4.dp)
    Column(
        Modifier
            .padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 10.dp)
            .shadow(6.dp, shape)
            .background(MaterialTheme.colorScheme.background, shape)
            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f), shape),
    ) {
        Text(
            text = stringResource(R.string.binder_assign_to, category.name).uppercase(),
            color = c.textTertiary,
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 6.dp),
            style = TextStyle(
                fontFamily = Lora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp,
                letterSpacing = 1.sp,
            ),
        )
        articles.forEach { article ->
            val on = category.id in article.categoryIds
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable(role = Role.Checkbox) { onSetMember(article.id, category.id, !on) }
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(on)
                Text(
                    text = article.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = TextStyle(fontFamily = Lora, fontSize = 14.5.sp, lineHeight = 19.sp),
                )
            }
        }
        HorizontalDivider(color = c.divider)
        Box(
            Modifier.fillMaxWidth().height(42.dp).clickable(role = Role.Button, onClick = onDone),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.binder_done),
                color = c.accentText,
                style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
            )
        }
    }
}

@Composable
private fun Checkbox(on: Boolean) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(3.dp)
    Box(
        Modifier
            .size(18.dp)
            .background(if (on) c.accentLine else Color.Transparent, shape)
            .border(1.dp, if (on) c.accentLine else c.iconMuted, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (on) {
            Icon(
                QlIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun IconButton(icon: ImageVector, description: String, tint: Color, box: Int, size: Int, onClick: () -> Unit) {
    Box(
        Modifier.size(box.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size.dp))
    }
}

@Composable
private fun CreateFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = QuestLogTheme.colors
    Row(
        modifier = modifier
            .height(52.dp)
            .shadow(4.dp, RoundedCornerShape(26.dp), ambientColor = c.snackbar, spotColor = c.snackbar)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, c.accentLine, RoundedCornerShape(26.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(QlIcons.Plus, contentDescription = null, tint = c.accentText, modifier = Modifier.size(20.dp))
        Text(
            text = stringResource(R.string.binder_new_category),
            color = c.accentText,
            style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        )
    }
}

private val SampleCategories = listOf(BinderCategoryUi(1, 1, "Walkthrough Utama"), BinderCategoryUi(2, 1, "Koleksi"))
private val SampleArticles = listOf(
    BinderArticleUi(1, "Walkthrough Lengkap — Disc 1", setOf(1)),
    BinderArticleUi(2, "Lokasi Semua Mini Medal", setOf(2, 1)),
    BinderArticleUi(3, "Panduan Class & Monster Heart", emptySet()),
)

@Composable
private fun BinderPreview(
    categories: List<BinderCategoryUi> = SampleCategories,
    articles: List<BinderArticleUi> = SampleArticles,
    readOnly: Boolean = false,
    picker: Long? = null,
    snackbar: String? = null,
) {
    QuestLogTheme {
        BinderContent(
            gameName = "Dragon Quest VII",
            categories = categories,
            articles = articles,
            readOnly = readOnly,
            pickerCategoryId = picker,
            snackbar = snackbar,
            actions = BinderActions({}, {}, {}, {}, {}, { _, _, _ -> }, {}),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun BinderPremiumPreview() = BinderPreview()

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun BinderPickerPreview() = BinderPreview(picker = 2L)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun BinderLostPreview() = BinderPreview(readOnly = true)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun BinderEmptyCategoriesPreview() = BinderPreview(categories = emptyList())

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun BinderEmptyCategoryPreview() = BinderPreview(
    articles = SampleArticles.map {
        it.copy(categoryIds = emptySet())
    },
    snackbar = "Kategori dihapus — artikel tetap ada",
)
