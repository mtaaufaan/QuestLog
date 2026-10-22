package com.rds.questlog.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.rds.questlog.domain.model.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Token Mode Baca (design.md 3.2): keempat mode lengkap, berbeda, dan lolos kontras WCAG AA untuk teks. */
class ReaderColorsTest {

    private fun contrast(foreground: Color, background: Color): Float {
        val fg = foreground.compositeOver(background).luminance()
        val bg = background.luminance()
        return (maxOf(fg, bg) + 0.05f) / (minOf(fg, bg) + 0.05f)
    }

    @Test
    fun `setiap mode punya token sendiri dan latar yang berbeda`() {
        val backgrounds = ReaderTheme.entries.map { readerColorsFor(it).bg }

        assertEquals(4, backgrounds.distinct().size)
        assertEquals(Color(0xFFF3F2F2), readerColorsFor(ReaderTheme.LIGHT).bg)
        assertEquals(Color(0xFF1D1C1A), readerColorsFor(ReaderTheme.DARK).bg)
        assertEquals(Color(0xFFFBF0D9), readerColorsFor(ReaderTheme.SEPIA).bg)
        assertEquals(Color(0xFFE4E4E6), readerColorsFor(ReaderTheme.MUTED).bg)
    }

    @Test
    fun `teks primer sekunder dan aksen lolos kontras 4,5 di semua mode`() {
        for (theme in ReaderTheme.entries) {
            val c = readerColorsFor(theme)
            assertTrue("$theme fg", contrast(c.fg, c.bg) >= 4.5f)
            assertTrue("$theme muted", contrast(c.muted, c.bg) >= 4.5f)
            assertTrue("$theme accent", contrast(c.accent, c.bg) >= 4.5f)
            assertTrue("$theme snackbar", contrast(c.snackFg, c.snackBg) >= 4.5f)
        }
    }

    @Test
    fun `muted memakai dua aksen dan hanya yang teks lolos kontras`() {
        val c = readerColorsFor(ReaderTheme.MUTED)

        assertNotEquals(c.accent, c.accentStroke)
        assertTrue(contrast(c.accent, c.bg) >= 4.5f)
        assertTrue(contrast(c.accentStroke, c.bg) < 4.5f)
    }
}
