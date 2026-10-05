package com.rds.questlog.presentation.preview

import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.ContentNodeType
import com.rds.questlog.presentation.model.ContentNodeUi
import com.rds.questlog.presentation.model.PageStatus
import com.rds.questlog.presentation.model.PageUiModel
import com.rds.questlog.presentation.model.TableCellUi

/**
 * Artikel dummy untuk Reader (sumber: Reader.dc.html): 5 halaman, campuran tipe blok, halaman ke-4 FAILED
 * (menguji banner partial dan placeholder "gagal dimuat"). Id node mengikuti prototipe: checkpoint 302, posisi 204.
 */
object ReaderPreviewData {

    val article = ArticleUiModel(
        id = 1,
        gameId = 1,
        gameName = "Dragon Quest VII",
        title = "Walkthrough Lengkap — Disc 1",
        status = ArticleStatus.READY,
        pages = (1..PAGE_COUNT).map { i ->
            PageUiModel(
                url = "https://gamefaqs.gamespot.com/ps/dq7/faqs/walkthrough?page=$i",
                status = if (i == FAILED_PAGE) PageStatus.FAILED else PageStatus.DONE,
                reason = if (i == FAILED_PAGE) "Timeout — server tidak merespons" else null,
            )
        },
        checkpointNodeId = 302,
        lastNodeId = 204,
    )

    /** Blok per halaman (indeks = urutan halaman); halaman gagal kosong. */
    val content: List<List<ContentNodeUi>> = listOf(
        listOf(
            node(101, ContentNodeType.H1),
            node(
                102,
                ContentNodeType.P,
                "Panduan ini mengikuti urutan cerita Disc 1, dari Pulau Estard hingga fragmen batu keempat. " +
                    "Setiap bagian mencantumkan peti, Mini Medal, dan sidequest yang mudah terlewat.",
            ),
            node(103, ContentNodeType.H2, "Sebelum Mulai"),
            node(
                104,
                ContentNodeType.LI,
                "Simpan di gereja setiap kali masuk desa baru — game ini tidak punya autosave.",
            ),
            node(
                105,
                ContentNodeType.LI,
                "Bicaralah dengan semua NPC dua kali; beberapa memberi petunjuk lokasi fragmen.",
            ),
            node(
                106,
                ContentNodeType.LI,
                "Lemari, tong, dan guci bisa diperiksa. Banyak Mini Medal tersembunyi di sana.",
            ),
            node(107, ContentNodeType.H2, "Kastil Estard"),
            node(
                108,
                ContentNodeType.P,
                "Mulai dari kamar pangeran di lantai tiga. Turun ke dapur dan periksa tong di pojok kiri untuk " +
                    "mendapat Herb pertama. Di halaman belakang ada sumur tua; masuk ke dalamnya untuk menemukan " +
                    "50 Gold dan jalan pintas ke luar kastil.",
            ),
        ),
        listOf(
            node(201, ContentNodeType.H2, "Reruntuhan Barat"),
            node(
                202,
                ContentNodeType.P,
                "Reruntuhan ini adalah dungeon pertama. Musuhnya lemah, tetapi jalurnya berputar. Ikuti peta di " +
                    "bawah dan ambil jalur kiri pada percabangan pertama.",
            ),
            node(203, ContentNodeType.PRE, MAP_ART),
            node(
                204,
                ContentNodeType.P,
                "Di altar, letakkan fragmen biru untuk membuka pintu batu. Jika pintu tidak bergerak, pastikan " +
                    "semua anggota party berdiri di atas lingkaran.",
            ),
            node(205, ContentNodeType.LI, "Peti A — Leather Hat"),
            node(206, ContentNodeType.LI, "Peti B — Mini Medal #1"),
            node(207, ContentNodeType.LI, "Guci dekat tangga — Seed of Wisdom"),
        ),
        listOf(
            node(301, ContentNodeType.H2, "Desa Fishbel"),
            node(
                302,
                ContentNodeType.P,
                "Setelah melewati portal, Anda tiba di desa nelayan yang diselimuti kabut. Warga tidak mau bicara " +
                    "sampai Anda menemui kepala desa di rumah paling timur.",
            ),
            node(303, ContentNodeType.IMG, "Peta Desa Fishbel — lokasi peti dan Mini Medal"),
            node(
                304,
                ContentNodeType.P,
                "Kapal di dermaga baru bisa dipakai setelah event malam hari. Tidur di penginapan untuk memicu " +
                    "event tersebut.",
            ),
            ContentNodeUi(
                id = 305,
                type = ContentNodeType.TABLE,
                table = listOf(
                    listOf(
                        TableCellUi("Item", isHeader = true),
                        TableCellUi("Lokasi", isHeader = true),
                        TableCellUi("Catatan", isHeader = true),
                    ),
                    listOf(TableCellUi("Mini Medal"), TableCellUi("Sumur timur"), TableCellUi("Periksa dua kali")),
                    listOf(TableCellUi("Seed of Life"), TableCellUi("Lemari kepala desa"), TableCellUi("—")),
                    listOf(TableCellUi("Fishing Rod"), TableCellUi("Dermaga"), TableCellUi("Setelah event")),
                ),
            ),
        ),
        emptyList(),
        listOf(
            node(501, ContentNodeType.H2, "Boss: Golem Pasir"),
            node(
                502,
                ContentNodeType.P,
                "HP sekitar 480. Golem menyerang dua kali per giliran saat HP-nya di bawah 50%. Disarankan " +
                    "level party minimal 9.",
            ),
            node(503, ContentNodeType.H3, "Strategi"),
            node(504, ContentNodeType.LI, "Giliran 1: naikkan pertahanan dengan Upper, lalu serang biasa."),
            node(
                505,
                ContentNodeType.LI,
                "Sembuhkan saat HP anggota di bawah 20 — serangan gandanya bisa mencapai 18.",
            ),
            node(506, ContentNodeType.LI, "Jangan pakai sihir api; Golem tahan api."),
            node(
                507,
                ContentNodeType.P,
                "Setelah menang, kembali ke Estard dan letakkan fragmen di altar untuk membuka pulau berikutnya.",
            ),
        ),
    )

    private const val PAGE_COUNT = 5
    private const val FAILED_PAGE = 4

    private const val MAP_ART = "+-------+     +-------+\n| Peti  |-----| Altar |\n+---+---+     +---+---+\n" +
        "    |             |\n+---+---+     +---+---+\n| Masuk |     |Tangga |\n+-------+     +-------+"

    private fun node(id: Long, type: ContentNodeType, text: String = "") = ContentNodeUi(id, type, text)
}
