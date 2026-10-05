package com.rds.questlog.presentation.reader

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.rds.questlog.domain.model.InlineMarkup
import org.junit.Assert.assertEquals
import org.junit.Test

class StyledTextTest {

    private val boldOn = InlineMarkup.BOLD_ON
    private val boldOff = InlineMarkup.BOLD_OFF
    private val italicOn = InlineMarkup.ITALIC_ON
    private val italicOff = InlineMarkup.ITALIC_OFF

    @Test
    fun `tebal dan miring menjadi span pada rentang yang benar tanpa karakter penanda`() {
        val styled = "Ambil ${boldOn}herb$boldOff dan ${italicOn}simpan$italicOff.".toStyledText()

        assertEquals("Ambil herb dan simpan.", styled.text)
        val bold = styled.spanStyles.single { it.item.fontWeight == FontWeight.SemiBold }
        assertEquals("herb", styled.text.substring(bold.start, bold.end))
        val italic = styled.spanStyles.single { it.item.fontStyle == FontStyle.Italic }
        assertEquals("simpan", styled.text.substring(italic.start, italic.end))
    }

    @Test
    fun `penanda tidak seimbang tidak merusak teks`() {
        val unclosed = "Awal ${boldOn}tebal sampai akhir".toStyledText()
        assertEquals("Awal tebal sampai akhir", unclosed.text)
        assertEquals("tebal sampai akhir", unclosed.text.substring(unclosed.spanStyles.single().start))

        assertEquals("Hanya penutup", "Hanya penutup$boldOff".toStyledText().text)
        assertEquals("Teks biasa * tanpa gaya", "Teks biasa * tanpa gaya".toStyledText().text)
        assertEquals(0, "Teks biasa".toStyledText().spanStyles.size)
    }
}
