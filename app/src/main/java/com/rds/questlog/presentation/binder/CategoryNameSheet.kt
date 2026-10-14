package com.rds.questlog.presentation.binder

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.rds.questlog.presentation.components.QlOutlineButton
import com.rds.questlog.presentation.components.QlTextField
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Category Name Sheet (P16): buat kategori baru atau ganti nama. [existingNames] = nama kategori game ini (saat
 * rename, nama sendiri dikecualikan dari pemeriksaan duplikat lewat [initialName]). Nama dinormalisasi; bila berbeda
 * dari isian tampil "Disimpan sebagai “…”". Duplikat (tanpa peduli huruf) atau kosong menonaktifkan tombol simpan.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryNameSheet(
    rename: Boolean,
    gameName: String,
    initialName: String,
    existingNames: List<String>,
    onConfirm: (String) -> Unit,
    onClose: () -> Unit,
) {
    val c = QuestLogTheme.colors
    var input by rememberSaveable { mutableStateOf(initialName) }
    val name = normalizeCategoryName(input)
    val others = existingNames.filterNot { rename && it.equals(initialName, ignoreCase = true) }
    val duplicate = isDuplicateCategory(name, others)
    QlBottomSheet(onDismiss = onClose) {
        Column(
            Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.binder_sheet_kicker, gameName).uppercase(),
                    color = c.accentText,
                    style = TextStyle(
                        fontFamily = Lora,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.26.sp,
                    ),
                )
                Text(
                    text = stringResource(if (rename) R.string.category_name_rename else R.string.category_name_create),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = TextStyle(
                        fontFamily = CormorantGaramond,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 26.sp,
                    ),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = stringResource(R.string.category_name_field).uppercase(),
                        color = c.textSecondary,
                        style = TextStyle(
                            fontFamily = Lora,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                        ),
                    )
                    Text(
                        text = "${input.length}/$MAX_CATEGORY_NAME",
                        color = c.textTertiary,
                        style = TextStyle(fontFamily = Lora, fontSize = 12.sp, fontFeatureSettings = "tnum"),
                    )
                }
                QlTextField(
                    value = input,
                    onValueChange = { input = it.take(MAX_CATEGORY_NAME) },
                    placeholder = stringResource(R.string.category_name_placeholder),
                    errorText = if (duplicate) stringResource(R.string.category_name_duplicate, name) else null,
                )
                if (!duplicate && name.isNotEmpty() && name != input) {
                    Text(
                        text = stringResource(R.string.category_name_saved_as, name),
                        color = c.textSecondary,
                        style = TextStyle(fontFamily = Lora, fontSize = 12.5.sp),
                    )
                }
            }
            if (others.isNotEmpty()) ExistingNames(others)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                QlGhostButton(stringResource(R.string.category_name_cancel), onClose, Modifier.height(46.dp))
                QlOutlineButton(
                    text = stringResource(
                        if (rename) R.string.category_name_save else R.string.category_name_create_action,
                    ),
                    onClick = { onConfirm(name) },
                    modifier = Modifier.height(46.dp),
                    enabled = name.isNotEmpty() && !duplicate,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExistingNames(names: List<String>) {
    val c = QuestLogTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.category_name_existing).uppercase(),
            color = c.textTertiary,
            style = TextStyle(
                fontFamily = Lora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
            ),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            names.forEach { name ->
                Text(
                    text = name,
                    color = c.textSecondary,
                    modifier = Modifier
                        .border(1.dp, c.divider, RoundedCornerShape(13.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    style = TextStyle(fontFamily = Lora, fontSize = 12.5.sp),
                )
            }
        }
    }
}

private val SampleNames = listOf("Walkthrough Utama", "Koleksi", "Boss")

@Composable
private fun SheetPreview(rename: Boolean, initial: String) {
    QuestLogTheme { CategoryNameSheet(rename, "Dragon Quest VII", initial, SampleNames, onConfirm = {}, onClose = {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun CategoryNameCreatePreview() = SheetPreview(rename = false, initial = "")

@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun CategoryNameNormalizedPreview() = SheetPreview(rename = false, initial = "  sidequest   penting ")

@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun CategoryNameDuplicatePreview() = SheetPreview(rename = false, initial = "koleksi")

@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun CategoryNameRenamePreview() = SheetPreview(rename = true, initial = "Koleksi")
