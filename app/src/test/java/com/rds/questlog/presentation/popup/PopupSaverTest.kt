package com.rds.questlog.presentation.popup

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PopupSaverTest {

    private val all = listOf(
        Popup.ScrapeMode,
        Popup.ArticleActions(7),
        Popup.DeleteArticle(8),
        Popup.ScrapeError(9),
        Popup.Unlock,
        Popup.GameFilter,
        Popup.DisplaySettings,
        Popup.EditArticle(10),
        Popup.ManagePages(11),
        Popup.DeletePage(11, 42),
        Popup.RefreshArticle(12),
    )

    private fun save(popup: Popup?): Any? = with(PopupSaver) { SaverScope { true }.save(popup) }

    @Test
    fun `setiap popup bisa disimpan lalu dipulihkan persis sama`() {
        all.forEach { popup -> assertEquals(popup, PopupSaver.restore(save(popup)!!)) }
    }

    @Test
    fun `tanpa popup tidak ada yang disimpan`() {
        assertNull(save(null))
    }

    @Test
    fun `data rusak atau kode tidak dikenal tidak memulihkan popup`() {
        assertNull(PopupSaver.restore(arrayListOf(99L, 1L)))
        assertNull(PopupSaver.restore(arrayListOf(ARTICLE_ACTIONS_CODE)))
        assertNull(PopupSaver.restore(arrayListOf(9L, 1L)))
        assertNull(PopupSaver.restore("bukan daftar"))
    }

    private companion object {
        const val ARTICLE_ACTIONS_CODE = 1L
    }
}
