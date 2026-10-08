package com.rds.questlog.data.repository

import com.rds.questlog.data.local.entity.ContentNodeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** QL-20: checkpoint dipulihkan ke teks yang sama, atau ke posisi terdekat bila teks berubah. */
class AnchorRestoreTest {

    private fun node(id: Long, order: Int, text: String?) = ContentNodeEntity(
        id = id,
        articleId = 1,
        sourcePageId = 1,
        nodeType = "p",
        displayOrder = order,
        textContent = text,
    )

    @Test
    fun `teks yang sama dipilih walau posisinya bergeser`() {
        val old = node(1, 5, "Pintu rahasia di lantai dua")
        val fresh = listOf(node(10, 1, "Awal"), node(11, 8, "Pintu rahasia di lantai dua"), node(12, 9, "Akhir"))

        assertEquals(11L, pickReplacement(old, fresh)?.id)
    }

    @Test
    fun `teks kembar memilih yang paling dekat dengan posisi lama`() {
        val old = node(1, 6, "Simpan permainan")
        val fresh = listOf(node(10, 1, "Simpan permainan"), node(11, 7, "Simpan permainan"))

        assertEquals(11L, pickReplacement(old, fresh)?.id)
    }

    @Test
    fun `teks yang berubah jatuh ke posisi terdekat`() {
        val old = node(1, 4, "Teks lama")
        val fresh = listOf(node(10, 1, "A"), node(11, 5, "B"), node(12, 9, "C"))

        assertEquals(11L, pickReplacement(old, fresh)?.id)
    }

    @Test
    fun `node tanpa teks seperti gambar memakai posisi terdekat`() {
        val old = node(1, 2, null)
        val fresh = listOf(node(10, 1, null), node(11, 3, null))

        assertEquals(10L, pickReplacement(old, fresh)?.id)
    }

    @Test
    fun `halaman baru tanpa node tidak punya pengganti`() {
        assertNull(pickReplacement(node(1, 1, "x"), emptyList()))
    }
}
