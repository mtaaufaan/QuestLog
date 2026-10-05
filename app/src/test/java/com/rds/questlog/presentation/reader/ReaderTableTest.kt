package com.rds.questlog.presentation.reader

import com.rds.questlog.presentation.model.TableCellUi
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTableTest {

    @Test
    fun `ruang lega - kolom diperlebar sebanding lebar pilihan dan total pas`() {
        val widths = distributeColumnWidths(min = listOf(20, 20), preferred = listOf(100, 300), available = 800)

        assertEquals(800.0, widths.sum().toDouble(), 1.0)
        assertEquals(widths[1] / widths[0].toDouble(), 3.0, 0.1)
    }

    @Test
    fun `ruang sedang - tiap kolom antara min dan pilihan, total pas`() {
        val min = listOf(40, 40, 40)
        val preferred = listOf(100, 100, 400)
        val widths = distributeColumnWidths(min, preferred, available = 360)

        assertEquals(360.0, widths.sum().toDouble(), 2.0)
        widths.indices.forEach { assert(widths[it] in min[it]..preferred[it]) }
    }

    @Test
    fun `ruang sempit - semua kolom di lebar minimum sehingga tabel digulir, kata tidak terpotong`() {
        val min = listOf(60, 50, 70)
        assertEquals(min, distributeColumnWidths(min, listOf(200, 200, 200), available = 150))
        assertEquals(emptyList<Int>(), distributeColumnWidths(emptyList(), emptyList(), 100))
    }

    @Test
    fun `jumlah kolom grid memperhitungkan colspan dan memakai baris terlebar`() {
        val rows = listOf(
            listOf(TableCellUi("Judul", colSpan = 12, isHeader = true)),
            List(12) { TableCellUi("c$it") },
            listOf(TableCellUi("a"), TableCellUi("b", colSpan = 3)),
        )
        assertEquals(12, tableColumnCount(rows))
        assertEquals(0, tableColumnCount(emptyList()))
    }
}
