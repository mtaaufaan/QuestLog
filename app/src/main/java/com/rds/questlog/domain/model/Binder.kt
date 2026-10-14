package com.rds.questlog.domain.model

/** Panjang maksimum nama kategori Binder (sama dengan nama game). */
const val MAX_CATEGORY_NAME = 100

private val WORD_START = Regex("(^|\\s)(\\S)")
private val SPACES = Regex("\\s+")

/** Nama kategori dinormalisasi: spasi di ujung dibuang, spasi ganda dirapikan, huruf awal tiap kata kapital. */
fun normalizeCategoryName(input: String): String =
    input.trim().replace(SPACES, " ").replace(WORD_START) { it.groupValues[1] + it.groupValues[2].uppercase() }

/** Kategori Game Binder (premium) milik satu game; [position] = urutan tampil. */
data class BinderCategory(val id: Long, val gameId: Long, val name: String, val position: Int)

/** Kategori satu game beserta penetapan artikelnya ([assignments]: id artikel -> id kategori). */
data class Binder(val categories: List<BinderCategory>, val assignments: Map<Long, Set<Long>>)

/** Hasil menekan Lanjutkan Game pada satu game. */
sealed interface ContinueGameResult {
    /** Buka artikel utama (di checkpoint-nya; tanpa checkpoint Reader memakai posisi terakhir dibaca). */
    data class Open(val articleId: Long) : ContinueGameResult

    /** Artikel utama masih diunduh: tampilkan "Artikel utama masih diproses". */
    data object StillScraping : ContinueGameResult

    /** Penanda ada tetapi artikelnya sudah tidak ada: tampilkan "Artikel utama sudah dihapus". */
    data object ArticleGone : ContinueGameResult
}
