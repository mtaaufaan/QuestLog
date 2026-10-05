package com.rds.questlog.data.scraper

import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
import com.rds.questlog.domain.model.InlineMarkup

private val WHITESPACE = Regex("\\s+")
private val EMPTY_SPAN = Regex("\\s*|\\s*")
private val BOLD_STYLE = Regex("font-weight\\s*:\\s*(bold|bolder|[6-9]00)")
private val ITALIC_STYLE = Regex("font-style\\s*:\\s*italic")
private val BOLD_TAGS = setOf("b", "strong")
private val ITALIC_TAGS = setOf("i", "em", "cite")
private val INLINE_TAGS = setOf(
    "a", "span", "b", "strong", "i", "em", "u", "code", "small", "sup", "sub", "abbr", "mark", "cite", "q", "kbd",
)

/**
 * Teks elemen dengan gaya inline dipertahankan sebagai penanda [InlineMarkup]: `<b>`/`<strong>` dan gaya CSS
 * font-weight tebal menjadi tebal, `<i>`/`<em>`/`<cite>` dan font-style italic menjadi miring. Spasi dirapikan seperti
 * `text()`; elemen blok dipisahkan spasi. Gaya yang hanya berasal dari kelas CSS eksternal tidak terdeteksi.
 */
internal fun Element.inlineText(): String {
    val out = StringBuilder()
    appendInline(this, out)
    return out.toString().replace(WHITESPACE, " ").replace(EMPTY_SPAN, " ").replace(WHITESPACE, " ").trim()
}

private fun appendInline(node: Node, out: StringBuilder) {
    when (node) {
        is TextNode -> out.append(node.text())
        is Element -> appendElement(node, out)
    }
}

private fun appendElement(element: Element, out: StringBuilder) {
    val tag = element.tagName().lowercase()
    val separated = tag !in INLINE_TAGS
    val style = element.attr("style").lowercase()
    val bold = tag in BOLD_TAGS || BOLD_STYLE.containsMatchIn(style)
    val italic = tag in ITALIC_TAGS || ITALIC_STYLE.containsMatchIn(style)
    if (separated) out.append(' ')
    if (bold) out.append(InlineMarkup.BOLD_ON)
    if (italic) out.append(InlineMarkup.ITALIC_ON)
    element.childNodes().forEach { appendInline(it, out) }
    if (italic) out.append(InlineMarkup.ITALIC_OFF)
    if (bold) out.append(InlineMarkup.BOLD_OFF)
    if (separated) out.append(' ')
}
