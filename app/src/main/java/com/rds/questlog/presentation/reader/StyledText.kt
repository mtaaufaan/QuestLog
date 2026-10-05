package com.rds.questlog.presentation.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.rds.questlog.domain.model.InlineMarkup
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Mengubah teks berpenanda gaya ([InlineMarkup]) menjadi [AnnotatedString]: tebal memakai SemiBold (satu-satunya
 * bobot tebal font Lora yang dimuat), miring memakai italic. Penanda yang tidak seimbang diperlakukan wajar: gaya yang
 * tidak ditutup berlaku sampai akhir, penutup tanpa pembuka diabaikan. [boldColor] (bila ditentukan) mewarnai teks
 * tebal; dipakai di mode gelap karena selisih bobot huruf saja kurang terlihat pada latar gelap.
 */
fun String.toStyledText(boldColor: Color = Color.Unspecified): AnnotatedString = buildAnnotatedString {
    val bold = SpanStyle(fontWeight = FontWeight.SemiBold, color = boldColor)
    var boldStart = -1
    var italicStart = -1
    for (char in this@toStyledText) {
        when (char) {
            InlineMarkup.BOLD_ON -> if (boldStart < 0) boldStart = length
            InlineMarkup.ITALIC_ON -> if (italicStart < 0) italicStart = length
            InlineMarkup.BOLD_OFF -> boldStart = close(boldStart, bold)
            InlineMarkup.ITALIC_OFF -> italicStart = close(italicStart, ITALIC)
            else -> append(char)
        }
    }
    close(boldStart, bold)
    close(italicStart, ITALIC)
}

/** Warna teks tebal: aksen emas di Reader gelap, tanpa warna khusus (hanya bobot) di Reader terang. */
@Composable
fun rememberBoldColor(): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < DARK_LUMINANCE
    return if (dark) QuestLogTheme.colors.accentText else Color.Unspecified
}

private const val DARK_LUMINANCE = 0.5f
private val ITALIC = SpanStyle(fontStyle = FontStyle.Italic)

/** Memberi [style] pada rentang [start] sampai akhir teks saat ini; mengembalikan -1 (gaya tertutup). */
private fun AnnotatedString.Builder.close(start: Int, style: SpanStyle): Int {
    if (start in 0 until length) addStyle(style, start, length)
    return -1
}

/** Teks berpenanda sebagai [AnnotatedString] yang diingat selama teks dan warna tebal tidak berubah. */
@Composable
fun rememberStyled(text: String): AnnotatedString {
    val boldColor = rememberBoldColor()
    return remember(text, boldColor) { text.toStyledText(boldColor) }
}
