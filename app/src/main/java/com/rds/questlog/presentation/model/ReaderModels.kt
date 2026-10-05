package com.rds.questlog.presentation.model

/** 5 state layar Reader (component-contract.md §3, screen_flow.md §3 S3). */
enum class ReaderViewState { SUCCESS, LOADING, NOT_FOUND, EMPTY, DB_ERROR }

/** Tipe blok konten yang dirender Reader; pemisah halaman dan halaman gagal diturunkan dari status halaman. */
enum class ContentNodeType { H1, H2, H3, P, LI, PRE, IMG, TABLE }

/**
 * Satu blok konten. [text] = isi (alt gambar untuk IMG); untuk H1 yang kosong Reader memakai judul artikel.
 * [table] (baris berisi sel) hanya dipakai TABLE.
 */
/** Sel tabel Reader: [colSpan] kolom lebarnya, [isHeader] = sel judul (tebal). */
data class TableCellUi(val text: String, val colSpan: Int = 1, val isHeader: Boolean = false)

data class ContentNodeUi(
    val id: Long,
    val type: ContentNodeType,
    val text: String = "",
    val table: List<List<TableCellUi>> = emptyList(),
    /** File gambar lokal untuk IMG; null = tampilkan placeholder. */
    val imagePath: String? = null,
)
