package com.rds.questlog.presentation.preview

import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameUiModel
import com.rds.questlog.presentation.model.PageStatus
import com.rds.questlog.presentation.model.PageUiModel

/**
 * Data dummy bersama untuk @Preview dan layar sebelum data nyata tersedia.
 * Sumber: QuestLogApp.dc.html (state awal). Jangan membuat dummy sendiri di layar; tambah/ubah di sini.
 */
object PreviewData {

    /** [GameUiModel.articleCount] sama dengan jumlah artikel di [articles] (dijaga `PreviewDataTest`). */
    val games = listOf(
        GameUiModel(id = 1, name = "Dragon Quest VII", articleCount = 3),
        GameUiModel(id = 2, name = "Breath Of Fire III", articleCount = 2),
    )

    /** Urutan sama dengan prototipe: SCRAPING, READY (checkpoint), READY (partial), READY, ERROR. */
    val articles = listOf(
        article(
            id = 3,
            gameId = 1,
            title = "Panduan Class & Monster Heart",
            status = ArticleStatus.SCRAPING,
            pages = pages("https://dragonquest.fandom.com/wiki/DQVII_Vocations?part=", 6) {
                if (it < 2) PageStatus.DONE else PageStatus.PENDING
            },
        ),
        article(
            id = 1,
            gameId = 1,
            title = "Walkthrough Lengkap — Disc 1",
            status = ArticleStatus.READY,
            pages = pages("https://gamefaqs.gamespot.com/ps/dq7/faqs/walkthrough?page=", 5),
            checkpointNodeId = 302,
            lastNodeId = 204,
            lastRead = "dibaca 2 jam lalu",
        ),
        article(
            id = 2,
            gameId = 1,
            title = "Lokasi Semua Mini Medal",
            status = ArticleStatus.READY,
            pages = pages("https://dragonquest.fandom.com/wiki/Mini_Medal_(DQVII)?section=", 4) {
                if (it == 2) PageStatus.FAILED else PageStatus.DONE
            }.mapIndexed { i, page ->
                if (i == 2) page.copy(reason = "Timeout — server tidak merespons") else page
            },
            lastRead = "dibaca kemarin",
        ),
        article(
            id = 4,
            gameId = 2,
            title = "Walkthrough Utama",
            status = ArticleStatus.READY,
            pages = pages("https://gamefaqs.gamespot.com/ps/bof3/faqs/main?page=", 5),
            checkpointNodeId = 103,
            lastRead = "dibaca 3 hari lalu",
        ),
        article(
            id = 5,
            gameId = 2,
            title = "Master & Skill List",
            status = ArticleStatus.ERROR,
            pages = listOf(
                PageUiModel(
                    url = "https://gamefaqs.gamespot.com/ps/bof3/faqs/master-list",
                    status = PageStatus.FAILED,
                    reason = "HTTP 403 — butuh login",
                ),
                PageUiModel(
                    url = "https://gamefaqs.gamespot.com/ps/bof3/faqs/skill-list",
                    status = PageStatus.FAILED,
                    reason = "HTTP 404 — halaman tidak ditemukan",
                ),
            ),
        ),
    )

    private var nextPageId = 1L

    private fun pages(base: String, count: Int, status: (Int) -> PageStatus = { PageStatus.DONE }) =
        List(count) { PageUiModel(id = nextPageId++, url = base + (it + 1), status = status(it)) }

    private fun article(
        id: Long,
        gameId: Long,
        title: String,
        status: ArticleStatus,
        pages: List<PageUiModel>,
        checkpointNodeId: Long? = null,
        lastNodeId: Long? = null,
        lastRead: String? = null,
    ) = ArticleUiModel(
        id = id,
        gameId = gameId,
        gameName = games.first { it.id == gameId }.name,
        title = title,
        status = status,
        pages = pages,
        checkpointNodeId = checkpointNodeId,
        lastNodeId = lastNodeId,
        lastRead = lastRead,
    )
}
