package com.rds.questlog.presentation.reader

import com.rds.questlog.domain.model.Article
import com.rds.questlog.domain.model.ArticleDetail
import com.rds.questlog.domain.model.ContentNode
import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.SourcePage
import com.rds.questlog.domain.model.SourcePageStatus
import com.rds.questlog.presentation.model.ContentNodeType
import com.rds.questlog.presentation.model.ReaderViewState
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderMappingTest {

    private fun page(id: Long, status: SourcePageStatus) = SourcePage(id, "https://x.com/$id", id.toInt(), status)

    private fun node(id: Long, page: Long, order: Int, type: NodeType = NodeType.P) =
        ContentNode(id, page, type, order, text = "t$id")

    private fun detail(
        pages: List<SourcePage>,
        nodes: List<ContentNode>,
        checkpoint: Long? = null,
        fallback: Int? = null,
        last: Long? = null,
    ) = ArticleDetail(
        Article(
            id = 1, gameId = 1, gameName = "G", title = "T", pages = pages, checkpointNodeId = checkpoint,
            lastVisitedNodeId = last, lastReadAt = null, createdAt = 0, checkpointFallbackOrder = fallback,
        ),
        nodes,
    )

    private val done = SourcePageStatus.COMPLETED

    @Test
    fun `konten dikelompokkan per halaman sesuai urutan halaman, halaman gagal kosong`() {
        val d = detail(
            pages = listOf(page(1, done), page(2, SourcePageStatus.FAILED), page(3, done)),
            nodes = listOf(node(10, 1, 1), node(11, 1, 2), node(30, 3, 2001)),
        )
        assertEquals(listOf(2, 0, 1), d.toContentPages().map { it.size })
    }

    @Test
    fun `p_cont tampil sebagai paragraf dan tabel dipecah menjadi header dan baris`() {
        val table = ContentNode(
            1,
            1,
            NodeType.TABLE,
            1,
            tableRows = listOf(listOf("Item", "Lokasi"), listOf("Herb", "Desa")),
        )
        assertEquals(ContentNodeType.P, node(2, 1, 2, NodeType.P_CONT).toUi().type)
        val ui = table.toUi()
        assertEquals(listOf("Item", "Lokasi"), ui.head)
        assertEquals(listOf(listOf("Herb", "Desa")), ui.rows)
    }

    @Test
    fun `view state mengikuti ada tidaknya artikel dan konten`() {
        assertEquals(ReaderViewState.NOT_FOUND, (null as ArticleDetail?).toViewState())
        assertEquals(ReaderViewState.SUCCESS, detail(listOf(page(1, done)), listOf(node(1, 1, 1))).toViewState())
        assertEquals(
            ReaderViewState.LOADING,
            detail(listOf(page(1, SourcePageStatus.PENDING)), emptyList()).toViewState(),
        )
        assertEquals(
            ReaderViewState.EMPTY,
            detail(listOf(page(1, SourcePageStatus.FAILED)), emptyList()).toViewState(),
        )
    }

    @Test
    fun `resume dari checkpoint memakai anchor dan cadangannya, resume biasa memakai posisi terakhir`() {
        val nodes = listOf(node(10, 1, 1), node(11, 1, 2), node(30, 1, 1001))
        val d = detail(listOf(page(1, done)), nodes, checkpoint = 999, fallback = 500, last = 11)

        assertEquals(30L, d.resumeTarget("checkpoint"))
        assertEquals(11L, d.resumeTarget("last"))
        assertEquals(11L, detail(listOf(page(1, done)), nodes, last = 11).resumeTarget("checkpoint"))
        assertEquals(null, detail(listOf(page(1, done)), nodes).resumeTarget("last"))
    }
}
