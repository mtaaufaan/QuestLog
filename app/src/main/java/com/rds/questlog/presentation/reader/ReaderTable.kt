package com.rds.questlog.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.domain.model.InlineMarkup
import com.rds.questlog.presentation.model.ContentNodeUi
import com.rds.questlog.presentation.model.TableCellUi
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme
import kotlin.math.roundToInt

private const val TABLE_SCALE = 0.86f
private const val PREFERRED_CAP = 0.6f
private const val CELL_END_PADDING_DP = 6
private const val MIN_COLUMN_DP = 12

/**
 * Lebar kolom (px) seperti tata letak tabel otomatis di browser. [min] = lebar terkecil agar tidak ada kata yang
 * terpotong, [preferred] = lebar bila teksnya muat satu baris. Cukup ruang → kolom diperlebar sebanding lebar pilihan;
 * ruang di antara → tiap kolom di antara min dan pilihan; kurang dari total min → semua min (tabel digulir ke samping).
 */
fun distributeColumnWidths(min: List<Int>, preferred: List<Int>, available: Int): List<Int> {
    val sumMin = min.sum()
    val sumPreferred = preferred.sum()
    return when {
        min.isEmpty() -> emptyList()
        sumMin >= available -> min
        sumPreferred <= available -> {
            val extra = (available - sumPreferred).toLong()
            preferred.map { it + (extra * it / sumPreferred).toInt() }
        }
        else -> {
            val share = (available - sumMin).toFloat() / (sumPreferred - sumMin)
            min.indices.map { min[it] + ((preferred[it] - min[it]) * share).toInt() }
        }
    }
}

/** Jumlah kolom grid: baris terlebar menurut jumlah sel dengan colspan-nya. */
fun tableColumnCount(rows: List<List<TableCellUi>>): Int = rows.maxOfOrNull { row -> row.sumOf { it.colSpan } } ?: 0

/**
 * Tabel Reader: lebar kolom mengikuti isi (bukan dibagi rata) dan colspan dihormati, sehingga tabel lebar tetap
 * terbaca; bila tetap lebih lebar dari layar, tabel bisa digulir ke samping alih-alih memecah kata.
 */
@Composable
fun TableBlock(node: ContentNodeUi, fs: Int) {
    val c = QuestLogTheme.colors
    val cellSp = (fs * TABLE_SCALE).roundToInt()
    val style = TextStyle(fontFamily = Lora, fontSize = cellSp.sp, lineHeight = (cellSp * 1.4f).sp)
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp)) {
        val availablePx = with(density) { maxWidth.roundToPx() }
        val widths = remember(node.id, node.table, cellSp, availablePx) {
            val (min, preferred) = measureColumns(node.table, style, density, availablePx, measurer::widthOf)
            distributeColumnWidths(min, preferred, availablePx)
        }
        val totalDp = with(density) { widths.sum().toDp() }
        val scrolls = widths.sum() > availablePx
        Column(if (scrolls) Modifier.horizontalScroll(rememberScrollState()) else Modifier.fillMaxWidth()) {
            Row(Modifier.width(totalDp).height(1.dp).background(MaterialTheme.colorScheme.onBackground)) {}
            node.table.forEach { cells ->
                Row(
                    Modifier.width(totalDp).drawBehind {
                        drawLine(c.divider, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                    },
                ) {
                    var column = 0
                    cells.forEach { cell ->
                        val span = cell.colSpan.coerceAtMost((widths.size - column).coerceAtLeast(1))
                        val cellDp = with(density) { widths.drop(column).take(span).sum().toDp() }
                        column += span
                        Text(
                            text = rememberStyled(cell.text),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = if (cell.isHeader && span > 1) TextAlign.Center else TextAlign.Start,
                            modifier = Modifier.width(
                                cellDp,
                            ).padding(top = 8.dp, bottom = 8.dp, end = CELL_END_PADDING_DP.dp),
                            style = style.copy(
                                fontWeight = if (cell.isHeader) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.text.TextMeasurer.widthOf(text: String, style: TextStyle): Int =
    measure(text, style, softWrap = false, maxLines = 1).size.width

/** Pasangan (indeks kolom awal, sel) untuk satu baris; sel yang membentang menggeser kolom berikutnya. */
private fun List<TableCellUi>.withColumn(): List<Pair<Int, TableCellUi>> {
    var column = 0
    return map { cell -> (column to cell).also { column += cell.colSpan } }
}

/** Lebar minimum (kata terpanjang) dan pilihan (satu baris, dibatasi) tiap kolom; sel colspan tidak dihitung. */
private fun measureColumns(
    rows: List<List<TableCellUi>>,
    style: TextStyle,
    density: Density,
    availablePx: Int,
    widthOf: (String, TextStyle) -> Int,
): Pair<List<Int>, List<Int>> {
    val columns = tableColumnCount(rows)
    val pad = with(density) { CELL_END_PADDING_DP.dp.roundToPx() }
    val floor = with(density) { MIN_COLUMN_DP.dp.roundToPx() }
    val cap = (availablePx * PREFERRED_CAP).roundToInt()
    val min = MutableList(columns) { floor }
    val preferred = MutableList(columns) { floor }
    rows.flatMap { it.withColumn() }
        .map { (column, cell) -> column to cell.copy(text = InlineMarkup.strip(cell.text)) }
        .filter { (column, cell) -> cell.colSpan == 1 && column < columns && cell.text.isNotBlank() }
        .forEach { (column, cell) ->
            val font = if (cell.isHeader) style.copy(fontWeight = FontWeight.SemiBold) else style
            val longestWord = cell.text.split(' ').maxByOrNull { it.length }.orEmpty()
            min[column] = maxOf(min[column], widthOf(longestWord, font) + pad)
            preferred[column] = maxOf(preferred[column], widthOf(cell.text, font) + pad)
        }
    return min to preferred.mapIndexed { i, w -> w.coerceAtMost(maxOf(cap, min[i])) }
}
