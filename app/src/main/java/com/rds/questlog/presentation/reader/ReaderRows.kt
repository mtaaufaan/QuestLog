package com.rds.questlog.presentation.reader

import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.ContentNodeUi
import com.rds.questlog.presentation.model.PageStatus
import com.rds.questlog.presentation.model.PageUiModel
import java.net.URI

/** Satu baris daftar baca: blok konten, pemisah antar-halaman, atau placeholder halaman yang gagal dimuat. */
sealed interface ReaderRow {
    data class Node(val node: ContentNodeUi) : ReaderRow
    data class Break(val page: Int, val host: String) : ReaderRow
    data class Missing(val page: Int) : ReaderRow
}

val ReaderRow.nodeId: Long? get() = (this as? ReaderRow.Node)?.node?.id

/** Hasil menerjemahkan id node ke posisinya: [pageIndex] = indeks di antara halaman yang berhasil (DONE). */
data class NodeHit(val nodeId: Long, val pageIndex: Int)

/** Halaman bisa dibaca bila selesai, atau masih menyimpan isi lamanya (unduh ulang berjalan atau gagal). */
private val PageUiModel.readable: Boolean get() = status == PageStatus.DONE || hasContent

/** Indeks (0-based) halaman artikel yang bisa dibaca. */
fun okPageIndexes(article: ArticleUiModel): List<Int> = article.pages.indices.filter { article.pages[it].readable }

/**
 * Baris untuk mode Seamless (semua halaman disambung pemisah; halaman gagal jadi placeholder) atau Per Halaman
 * (hanya halaman ke-[pageIndex] dari halaman yang berhasil).
 */
fun readerRows(
    article: ArticleUiModel,
    content: List<List<ContentNodeUi>>,
    paged: Boolean,
    pageIndex: Int,
): List<ReaderRow> {
    val ok = okPageIndexes(article)
    val pagedPage = ok.getOrNull(pageIndex.coerceIn(0, (ok.size - 1).coerceAtLeast(0)))
    if (paged) return pagedPage?.let { content.getOrNull(it).orEmpty().map(ReaderRow::Node) }.orEmpty()
    return article.pages.indices.flatMap { i ->
        val page = article.pages[i]
        buildList {
            if (!page.readable) {
                add(ReaderRow.Missing(i + 1))
            } else {
                if (i > 0) add(ReaderRow.Break(i + 1, hostOf(page.url)))
                content.getOrNull(i).orEmpty().forEach { add(ReaderRow.Node(it)) }
            }
        }
    }
}

/**
 * Menerjemahkan [id] ke node yang benar-benar ada: persis id itu, bila tidak ada node pertama dengan id lebih besar,
 * bila tidak ada node terakhir. Null bila tidak ada konten sama sekali.
 */
fun resolveNode(id: Long, article: ArticleUiModel, content: List<List<ContentNodeUi>>): NodeHit? {
    val all = okPageIndexes(article).flatMapIndexed { okIndex, page ->
        content.getOrNull(page).orEmpty().map { NodeHit(it.id, okIndex) }
    }
    return all.firstOrNull { it.nodeId == id } ?: all.firstOrNull { it.nodeId >= id } ?: all.lastOrNull()
}

/** Nomor halaman (1-based) dari halaman ke-[pageIndex] di antara halaman yang berhasil. */
fun pageNumber(article: ArticleUiModel, pageIndex: Int): Int {
    val ok = okPageIndexes(article)
    return (ok.getOrNull(pageIndex.coerceIn(0, (ok.size - 1).coerceAtLeast(0))) ?: 0) + 1
}

private fun hostOf(url: String): String = runCatching { URI(url).host.orEmpty().removePrefix("www.") }.getOrDefault("")
