package com.rds.questlog.presentation.binder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BinderDummyStoreTest {

    private val store = BinderDummyStore()

    @Test
    fun `kategori milik satu game dan nama diurut menurut dibuat`() {
        store.create(1, "Utama")
        store.create(2, "Lain")
        store.create(1, "Koleksi")

        assertEquals(listOf("Utama", "Koleksi"), store.categoriesOf(1).map { it.name })
        assertEquals(listOf("Lain"), store.categoriesOf(2).map { it.name })
    }

    @Test
    fun `satu artikel boleh masuk beberapa kategori dan dikeluarkan satu per satu`() {
        val a = store.create(1, "Utama")
        val b = store.create(1, "Koleksi")

        store.setMembership(10, a, true)
        store.setMembership(10, b, true)
        assertEquals(listOf("Utama", "Koleksi"), store.categoryNamesOf(10, 1))

        store.setMembership(10, a, false)
        assertEquals(listOf("Koleksi"), store.categoryNamesOf(10, 1))
    }

    @Test
    fun `ganti nama mempertahankan penetapan dan hapus kategori hanya mencabut relasinya`() {
        val a = store.create(1, "Utama")
        store.setMembership(10, a, true)

        store.rename(a, "Jalur Utama")
        assertEquals(listOf("Jalur Utama"), store.categoryNamesOf(10, 1))

        store.delete(a)
        assertEquals(emptyList<String>(), store.categoryNamesOf(10, 1))
        assertEquals(emptyList<BinderCategoryUi>(), store.categoriesOf(1))
        // Artikel tidak ikut terhapus: penetapannya hanya kosong.
        assertEquals(emptySet<Long>(), store.assignments.getValue(10))
    }

    @Test
    fun `assign membuat kategori baru, memakai ulang nama yang sudah ada, dan mengganti penetapan`() {
        val existing = store.create(1, "Koleksi")
        store.setMembership(10, existing, true)

        store.assign(10, 1, CategoryAssignment(emptySet(), listOf("Boss", "koleksi")))

        assertEquals(listOf("Koleksi", "Boss"), store.categoryNamesOf(10, 1))
        assertEquals(2, store.categoriesOf(1).size)
    }

    @Test
    fun `artikel utama satu per game dan yang baru menimpa yang lama`() {
        store.setMainArticle(1, MainArticleRef(5, "Lama"))
        store.setMainArticle(2, MainArticleRef(8, "Lain"))
        store.setMainArticle(1, MainArticleRef(6, "Baru"))

        assertEquals(MainArticleRef(6, "Baru"), store.mainArticles[1])
        assertEquals(MainArticleRef(8, "Lain"), store.mainArticles[2])
        assertNull(store.mainArticles[3])
    }
}
