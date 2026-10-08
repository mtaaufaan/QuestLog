package com.rds.questlog.presentation.reader

import com.rds.questlog.domain.model.ArticleDetail
import com.rds.questlog.domain.model.ContentNode
import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.SourcePageStatus
import com.rds.questlog.domain.model.resolveAnchor
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.ContentNodeType
import com.rds.questlog.presentation.model.ContentNodeUi
import com.rds.questlog.presentation.model.ReaderViewState
import com.rds.questlog.presentation.model.TableCellUi

/** Blok konten dikelompokkan per halaman sumber menurut urutan halaman artikel (indeks = urutan halaman). */
fun ArticleDetail.toContentPages(): List<List<ContentNodeUi>> {
    val byPage = nodes.groupBy { it.sourcePageId }
    return article.pages.map { page -> byPage[page.id].orEmpty().map(ContentNode::toUi) }
}

/** Menandai halaman yang masih punya konten tersimpan, termasuk yang sedang atau gagal diunduh ulang. */
fun ArticleUiModel.withContent(content: List<List<ContentNodeUi>>) = copy(
    pages = pages.mapIndexed { i, page -> page.copy(hasContent = content.getOrNull(i).orEmpty().isNotEmpty()) },
)

fun ContentNode.toUi() = ContentNodeUi(
    id = id,
    type = when (type) {
        NodeType.H1 -> ContentNodeType.H1
        NodeType.H2 -> ContentNodeType.H2
        NodeType.H3 -> ContentNodeType.H3
        // p_cont hanya beda granularitas checkpoint; tampilannya sama dengan paragraf.
        NodeType.P, NodeType.P_CONT -> ContentNodeType.P
        NodeType.IMG -> ContentNodeType.IMG
        NodeType.PRE -> ContentNodeType.PRE
        NodeType.TABLE -> ContentNodeType.TABLE
        NodeType.LI -> ContentNodeType.LI
    },
    text = text,
    table = tableRows.map { row -> row.map { TableCellUi(it.text, it.colSpan, it.isHeader) } },
    imagePath = imagePath,
)

/**
 * State layar dari detail artikel: tidak ada artikel -> NOT_FOUND; belum ada konten tetapi masih ada halaman yang
 * diproses -> LOADING; tidak ada konten sama sekali -> EMPTY; selain itu SUCCESS.
 */
fun ArticleDetail?.toViewState(): ReaderViewState = when {
    this == null -> ReaderViewState.NOT_FOUND
    nodes.isNotEmpty() -> ReaderViewState.SUCCESS
    article.pages.any { it.status == SourcePageStatus.PENDING || it.status == SourcePageStatus.IN_PROGRESS } ->
        ReaderViewState.LOADING
    else -> ReaderViewState.EMPTY
}

/**
 * Node tujuan auto-scroll saat Reader dibuka. resumeFrom "checkpoint" -> anchor checkpoint manual (dengan cadangan
 * display_order bila anchor hilang), selain itu posisi baca terakhir. Checkpoint kosong jatuh ke posisi terakhir.
 */
fun ArticleDetail.resumeTarget(resumeFrom: String): Long? {
    val article = article
    val checkpoint = nodes.resolveAnchor(article.checkpointNodeId, article.checkpointFallbackOrder)
    val last = nodes.resolveAnchor(article.lastVisitedNodeId, null)
    return if (resumeFrom == "checkpoint") checkpoint ?: last else last
}
