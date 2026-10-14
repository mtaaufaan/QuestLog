package com.rds.questlog.presentation.binder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.domain.model.MAX_CATEGORY_NAME
import com.rds.questlog.domain.model.normalizeCategoryName
import com.rds.questlog.presentation.components.QlBottomSheet
import com.rds.questlog.presentation.components.QlGhostButton
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.components.QlOutlineButton
import com.rds.questlog.presentation.components.QlTextField
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme

/** Kategori di dalam sheet: [id] negatif = baru dibuat di sheet dan belum tersimpan. */
private data class Chip(val id: Long, val name: String)

/**
 * Assign Category Sheet (P18): menetapkan kategori Binder pada satu artikel lewat chip multi-pilih. Kolom "Tambah"
 * membuat kategori baru (dinormalisasi) atau mencentang yang sudah ada ("sudah ada — akan dicentang"), dengan
 * petunjuk "Mirip: …". Simpan mengirim id terpilih dan nama kategori baru.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignCategorySheet(
    articleTitle: String,
    gameName: String,
    categories: List<BinderCategoryUi>,
    selectedIds: Set<Long>,
    onSave: (CategoryAssignment) -> Unit,
    onClose: () -> Unit,
    initialDraft: String = "",
) {
    val c = QuestLogTheme.colors
    var chips by remember { mutableStateOf(categories.map { Chip(it.id, it.name) }) }
    var selected by remember { mutableStateOf(selectedIds) }
    var draft by remember { mutableStateOf(initialDraft) }
    var nextTempId by remember { mutableStateOf(-1L) }
    val normalized = normalizeCategoryName(draft)
    val suggestion = suggestCategory(draft, chips.map { it.name })

    fun addDraft() {
        if (normalized.isEmpty()) return
        val existing = chips.firstOrNull { it.name.equals(normalized, ignoreCase = true) }
        if (existing != null) {
            selected = selected + existing.id
        } else {
            val chip = Chip(nextTempId--, normalized)
            chips = chips + chip
            selected = selected + chip.id
        }
        draft = ""
    }

    QlBottomSheet(onDismiss = onClose) {
        Column(
            Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.assign_category_kicker).uppercase(),
                    color = c.accentText,
                    style = TextStyle(
                        fontFamily = Lora,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.26.sp,
                    ),
                )
                Text(
                    text = articleTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = TextStyle(
                        fontFamily = CormorantGaramond,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                    ),
                )
                Text(
                    text = stringResource(R.string.assign_category_hint, gameName),
                    color = c.textSecondary,
                    style = TextStyle(fontFamily = Lora, fontSize = 13.sp),
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEach { chip ->
                    CategoryChip(chip.name, chip.id in selected) {
                        selected = if (chip.id in selected) selected - chip.id else selected + chip.id
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QlTextField(
                        value = draft,
                        onValueChange = { draft = it.take(MAX_CATEGORY_NAME) },
                        modifier = Modifier.weight(1f),
                        placeholder = stringResource(R.string.assign_category_placeholder),
                        height = 44.dp,
                    )
                    AddButton(enabled = normalized.isNotEmpty(), onClick = ::addDraft)
                }
                when (suggestion) {
                    is CategorySuggestion.Exists ->
                        Hint(stringResource(R.string.assign_category_exists, suggestion.name))
                    is CategorySuggestion.Similar ->
                        Hint(stringResource(R.string.assign_category_similar, suggestion.name))
                    CategorySuggestion.None -> Unit
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                QlGhostButton(stringResource(R.string.category_name_cancel), onClose, Modifier.height(46.dp))
                QlOutlineButton(
                    text = stringResource(R.string.category_name_save),
                    onClick = {
                        onSave(
                            CategoryAssignment(
                                categoryIds = selected.filter { it > 0 }.toSet(),
                                newNames = chips.filter { it.id < 0 && it.id in selected }.map { it.name },
                            ),
                        )
                    },
                    modifier = Modifier.height(46.dp),
                )
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        color = QuestLogTheme.colors.textSecondary,
        style = TextStyle(fontFamily = Lora, fontSize = 12.5.sp),
    )
}

@Composable
private fun CategoryChip(name: String, on: Boolean, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(shape)
            .background(if (on) c.accentTint else Color.Transparent)
            .border(1.dp, if (on) c.accentLine else c.divider, shape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(start = 10.dp, end = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
            if (on) Icon(QlIcons.Check, contentDescription = null, tint = c.accentText, modifier = Modifier.size(13.dp))
        }
        Text(
            text = name,
            color = if (on) c.accentText else c.textSecondary,
            style = TextStyle(fontFamily = Lora, fontSize = 14.sp),
        )
    }
}

/** Tombol "Tambah" bergaris putus-putus; nonaktif bila kolom kosong. */
@Composable
private fun AddButton(enabled: Boolean, onClick: () -> Unit) {
    val line = QuestLogTheme.colors.accentLine
    Box(
        modifier = Modifier
            .height(44.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .drawBehind {
                drawRoundRect(
                    color = line,
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
                )
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.assign_category_add),
            color = QuestLogTheme.colors.accentText,
            style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
        )
    }
}

private val SampleCategories = listOf(
    BinderCategoryUi(1, 1, "Walkthrough Utama"),
    BinderCategoryUi(2, 1, "Koleksi"),
    BinderCategoryUi(3, 1, "Boss"),
)

@Composable
private fun SheetPreview(
    categories: List<BinderCategoryUi> = SampleCategories,
    selected: Set<Long> = setOf(2L),
    draft: String = "",
) {
    QuestLogTheme {
        AssignCategorySheet(
            articleTitle = "Lokasi Semua Mini Medal",
            gameName = "Dragon Quest VII",
            categories = categories,
            selectedIds = selected,
            onSave = {},
            onClose = {},
            initialDraft = draft,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun AssignCategoryPreview() = SheetPreview()

@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun AssignCategoryNoneSelectedPreview() = SheetPreview(selected = emptySet())

@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun AssignCategoryExistsPreview() = SheetPreview(draft = "  koleksi ")

@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun AssignCategorySimilarPreview() = SheetPreview(draft = "bo")

@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun AssignCategoryEmptyPreview() = SheetPreview(categories = emptyList(), selected = emptySet())
