package com.rds.questlog.data.scraper

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import javax.inject.Inject

/**
 * Mengenali tautan halaman lanjutan di HTML (QL-34). Jenis tautan yang dikenali (ditetapkan Architect):
 *  1. `rel="next"` pada `<link>` atau `<a>`;
 *  2. daftar nomor halaman: tautan bertuliskan angka lebih besar dari halaman sekarang, atau tautan ber-pola halaman
 *     (`?page=n`, `&p=n`, `/page/n`) di jalur yang sama dengan halaman sekarang; terurut menaik menurut nomornya;
 *  3. tautan teks "Next", "Berikutnya", "Selanjutnya", "Lanjut", "›", "»".
 * Hasil berurutan: (1), lalu (2), lalu (3), tanpa duplikat, URL absolut tanpa fragmen. Penyaringan https, domain, dan
 * duplikat dilakukan use case; kelas ini murni (tanpa jaringan) sehingga mudah diuji dengan fixture HTML.
 */
class PaginationLinkParser @Inject constructor() {

    fun parse(html: String, baseUrl: String): List<String> {
        val document = Ksoup.parse(html, baseUrl)
        val current = currentPage(baseUrl)
        val relNext = document.select("link[rel~=(?i)next][href], a[rel~=(?i)next][href]").map { absolute(it) }
        val numbered = document.select("a[href]").mapNotNull { numberedLink(it, baseUrl, current) }
            .sortedBy { it.second }.map { it.first }
        val textNext = document.select("a[href]").filter { NEXT_TEXT.matches(it.text().trim()) }.map { absolute(it) }
        val self = baseUrl.substringBefore('#')
        return (relNext + numbered + textNext).filter { it.isNotEmpty() }.map { it.substringBefore('#') }
            .filter { it != self }.distinct()
    }

    private fun absolute(element: Element): String = element.absUrl("href")

    /** Pasangan (URL, nomor halaman) bila [anchor] adalah tautan nomor halaman setelah [current]; selain itu null. */
    private fun numberedLink(anchor: Element, baseUrl: String, current: Int): Pair<String, Int>? {
        val url = absolute(anchor).substringBefore('#')
        val number = pageNumber(anchor, url, baseUrl)
        val usable = url.isNotEmpty() && url != baseUrl.substringBefore('#')
        return if (usable && number != null && number > current) url to number else null
    }

    /** Nomor dari pola URL di jalur yang sama dengan halaman sekarang, atau dari teks tautan berupa angka. */
    private fun pageNumber(anchor: Element, url: String, baseUrl: String): Int? =
        pageNumberOf(url)?.takeIf { samePath(url, baseUrl) }
            ?: NUMBER_TEXT.matchEntire(anchor.text().trim())?.value?.toIntOrNull()

    private fun currentPage(url: String): Int = pageNumberOf(url) ?: 1

    private fun pageNumberOf(url: String): Int? =
        (PAGE_PARAM.find(url) ?: PAGE_PATH.find(url))?.groupValues?.get(1)?.toIntOrNull()

    /** Jalur (tanpa query/fragmen dan tanpa segmen `/page/n`) sama, sehingga hanya nomor halaman yang berbeda. */
    private fun samePath(a: String, b: String): Boolean = pathKey(a) == pathKey(b)

    private fun pathKey(url: String): String =
        url.substringBefore('#').substringBefore('?').replace(PAGE_PATH, "").trimEnd('/')

    private companion object {
        val NUMBER_TEXT = Regex("\\d{1,3}")
        val NEXT_TEXT = Regex("(?i)(next|next page|next ›|next »|berikutnya|selanjutnya|lanjut|lanjutkan|›|»|>|>>)")
        val PAGE_PARAM = Regex("[?&](?:page|paged|p|pg)=(\\d+)", RegexOption.IGNORE_CASE)
        val PAGE_PATH = Regex("/page/(\\d+)(?=/|$|\\?)", RegexOption.IGNORE_CASE)
    }
}
