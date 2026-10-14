package com.rds.questlog.presentation.binder

import com.rds.questlog.domain.model.normalizeCategoryName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryRulesTest {

    @Test
    fun `nama dinormalisasi - trim, spasi ganda dirapikan, huruf awal tiap kata kapital`() {
        assertEquals("Sidequest Penting", normalizeCategoryName("  sidequest   penting "))
        assertEquals("Koleksi", normalizeCategoryName("koleksi"))
        assertEquals("Boss DQ", normalizeCategoryName("boss dQ"))
        assertEquals("", normalizeCategoryName("   "))
    }

    @Test
    fun `duplikat tanpa peduli huruf besar-kecil dan kosong bukan duplikat`() {
        val others = listOf("Walkthrough Utama", "Koleksi")

        assertTrue(isDuplicateCategory("koleksi", others))
        assertTrue(isDuplicateCategory("WALKTHROUGH UTAMA", others))
        assertFalse(isDuplicateCategory("Boss", others))
        assertFalse(isDuplicateCategory("", others))
    }

    @Test
    fun `saran sama persis berarti sudah ada, berawalan sama berarti mirip, selain itu tidak ada`() {
        val existing = listOf("Walkthrough Utama", "Koleksi", "Boss")

        assertEquals(CategorySuggestion.Exists("Koleksi"), suggestCategory("  koleksi ", existing))
        assertEquals(CategorySuggestion.Similar("Boss"), suggestCategory("bo", existing))
        assertEquals(CategorySuggestion.None, suggestCategory("Item", existing))
        assertEquals(CategorySuggestion.None, suggestCategory("   ", existing))
    }
}
