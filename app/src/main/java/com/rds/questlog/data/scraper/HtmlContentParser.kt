package com.rds.questlog.data.scraper

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.TextNode
import com.rds.questlog.domain.model.NodeType
import javax.inject.Inject

/** Blok konten hasil ekstraksi, belum menyentuh jaringan/penyimpanan (gambar masih berupa URL). */
data class ParsedNode(
    val type: NodeType,
    val text: String? = null,
    val imageUrl: String? = null,
    val imageAlt: String? = null,
    val tableHtml: String? = null,
)

/**
 * Mengekstrak konten utama dari HTML: heading, paragraf, list, `<pre>`, tabel, dan gambar, dalam urutan dokumen.
 * Iklan, navigasi, komentar, pop-up, dan skrip dibuang; konten dicari di kontainer utama (artikel/wiki/#content),
 * dengan `<body>` sebagai cadangan. Tanpa dependensi Android sehingga dapat diuji di JVM.
 */
class HtmlContentParser @Inject constructor() {

    fun parse(html: String, baseUrl: String): List<ParsedNode> {
        val document = Ksoup.parse(html, baseUrl)
        removeNoise(document)
        val nodes = mutableListOf<ParsedNode>()
        collect(findContentRoot(document), nodes)
        return nodes
    }

    private fun removeNoise(document: Document) {
        document.select(NOISE_SELECTOR).remove()
        // <header> di dalam <article>/<main> memuat judul artikel; hanya header halaman yang dibuang.
        document.select("header")
            .filter { header -> header.parents().none { it.tagName().lowercase() in CONTENT_CONTAINERS } }
            .forEach { it.remove() }
    }

    private fun findContentRoot(document: Document): Element {
        for (selector in ROOT_SELECTORS) {
            val candidate = document.selectFirst(selector)
            if (candidate != null && candidate.text().length >= MIN_ROOT_TEXT) return candidate
        }
        return document.body()
    }

    /** Menelusuri anak [container]; teks lepas dan elemen inline dikumpulkan menjadi satu paragraf. */
    private fun collect(container: Element, out: MutableList<ParsedNode>) {
        val inline = StringBuilder()
        for (node in container.childNodes()) {
            when {
                node is TextNode -> inline.append(node.text())
                node is Element && node.isInline() -> inline.append(' ').append(node.text()).append(' ')
                node is Element -> {
                    emitParagraph(inline.toString(), out)
                    inline.clear()
                    handleBlock(node, out)
                }
            }
        }
        emitParagraph(inline.toString(), out)
    }

    private fun handleBlock(element: Element, out: MutableList<ParsedNode>) {
        when (element.tagName().lowercase()) {
            "h1" -> emitHeading(NodeType.H1, element, out)
            "h2" -> emitHeading(NodeType.H2, element, out)
            "h3", "h4", "h5", "h6" -> emitHeading(NodeType.H3, element, out)
            in PARAGRAPH_TAGS -> {
                emitParagraph(element.text(), out)
                element.select("img").forEach { emitImage(it, out) }
            }
            "ul", "ol" -> emitListItems(element, out)
            "pre" -> chunkPreformatted(element.wholeText()).forEach { out += ParsedNode(NodeType.PRE, it) }
            "table" -> emitTable(element, out)
            "img" -> emitImage(element, out)
            "hr", "br" -> Unit
            else -> collect(element, out)
        }
    }

    /** Item list tingkat atas; list bersarang ikut masuk ke teks `li` induknya sehingga tidak ganda. */
    private fun emitListItems(list: Element, out: MutableList<ParsedNode>) {
        for (item in list.children()) {
            if (!item.tagName().equals("li", ignoreCase = true)) continue
            val text = normalize(item.text())
            if (text.isNotEmpty()) out += ParsedNode(NodeType.LI, text)
        }
    }

    private fun emitTable(table: Element, out: MutableList<ParsedNode>) {
        if (table.text().isNotBlank()) out += ParsedNode(NodeType.TABLE, tableHtml = table.outerHtml())
    }

    private fun emitHeading(type: NodeType, element: Element, out: MutableList<ParsedNode>) {
        normalize(element.text()).takeIf { it.isNotEmpty() }?.let { out += ParsedNode(type, it) }
    }

    private fun emitParagraph(raw: String, out: MutableList<ParsedNode>) {
        val text = normalize(raw)
        if (text.isEmpty()) return
        splitParagraph(text).forEachIndexed { index, chunk ->
            out += ParsedNode(if (index == 0) NodeType.P else NodeType.P_CONT, chunk)
        }
    }

    private fun emitImage(element: Element, out: MutableList<ParsedNode>) {
        val src = element.absUrl("src").ifBlank { element.absUrl("data-src") }
        if (src.isBlank() || src.startsWith("data:")) return
        out += ParsedNode(NodeType.IMG, imageUrl = src, imageAlt = element.attr("alt").takeIf { it.isNotBlank() })
    }

    private fun Element.isInline(): Boolean = tagName().lowercase() in INLINE_TAGS && selectFirst("img") == null

    private fun normalize(text: String): String = text.replace(WHITESPACE, " ").trim()

    private companion object {
        const val MIN_ROOT_TEXT = 200
        val WHITESPACE = Regex("\\s+")
        val CONTENT_CONTAINERS = setOf("article", "main")
        val PARAGRAPH_TAGS = setOf("p", "dd", "dt", "figcaption", "summary")
        val INLINE_TAGS = setOf(
            "a", "span", "b", "strong", "i", "em", "u", "code", "small", "sup", "sub", "abbr", "mark", "cite", "q",
            "kbd",
        )
        val ROOT_SELECTORS = listOf(
            "#mw-content-text",
            "article",
            "main",
            "[role=main]",
            "#content",
            ".entry-content",
            ".post-content",
        )
        val NOISE_SELECTOR = listOf(
            "script", "style", "noscript", "template", "iframe", "svg", "canvas", "form", "button", "select", "dialog",
            "nav", "footer", "aside",
            "[role=navigation]", "[role=banner]", "[role=complementary]", "[role=contentinfo]", "[aria-hidden=true]",
            ".ad", ".ads", ".adsbygoogle", "[class*=advert]", "[id*=advert]", "[class^=ad-]", "[id^=ad-]",
            "#comments", ".comments", ".comment-list", "[class*=cookie]", "[id*=cookie]",
            ".popup", ".modal", ".newsletter", ".breadcrumb", ".breadcrumbs", ".sidebar", "#sidebar",
        ).joinToString(", ")
    }
}
