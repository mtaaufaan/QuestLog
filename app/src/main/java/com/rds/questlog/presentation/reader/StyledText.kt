package com.rds.questlog.presentation.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.rds.questlog.domain.model.InlineMarkup

/**
 * Mengubah teks berpenanda gaya ([InlineMarkup]) menjadi [AnnotatedString]: tebal memakai SemiBold (satu-satunya
 * bobot tebal font Lora yang dimuat), miring memakai italic. Penanda yang tidak seimbang diperlakukan wajar: gaya yang
 * tidak ditutup berlaku sampai akhir, penutup tanpa pembuka diabaikan.
 */
fun String.toStyledText(): AnnotatedString = buildAnnotatedString {
    var boldStart = -1
    var italicStart = -1
    for (char in this@toStyledText) {
        when (char) {
            InlineMarkup.BOLD_ON -> if (boldStart < 0) boldStart = length
            InlineMarkup.ITALIC_ON -> if (italicStart < 0) italicStart = length
            InlineMarkup.BOLD_OFF -> boldStart = close(boldStart, BOLD)
            InlineMarkup.ITALIC_OFF -> italicStart = close(italicStart, ITALIC)
            else -> append(char)
        }
    }
    close(boldStart, BOLD)
    close(italicStart, ITALIC)
}

private val BOLD = SpanStyle(fontWeight = FontWeight.SemiBold)
private val ITALIC = SpanStyle(fontStyle = FontStyle.Italic)

/** Memberi [style] pada rentang [start] sampai akhir teks saat ini; mengembalikan -1 (gaya tertutup). */
private fun AnnotatedString.Builder.close(start: Int, style: SpanStyle): Int {
    if (start in 0 until length) addStyle(style, start, length)
    return -1
}
