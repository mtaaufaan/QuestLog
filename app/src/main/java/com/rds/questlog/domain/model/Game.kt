package com.rds.questlog.domain.model

data class Game(
    val id: Long,
    val name: String,
    val createdAt: Long,
    /** Jumlah artikel milik game ini (untuk S0 Game List dan badge tier). */
    val articleCount: Int = 0,
    /** Judul artikel yang terakhir dibaca di game ini dan kapan (epoch ms); null bila belum ada yang dibaca. */
    val lastArticleTitle: String? = null,
    val lastReadAt: Long? = null,
    /** Artikel utama game (tombol Lanjutkan Game) dan judulnya; null bila belum ada. */
    val mainArticleId: Long? = null,
    val mainArticleTitle: String? = null,
    /** Path logo kustom relatif ke `filesDir`; null = belum ada (UI menampilkan inisial). */
    val logoPath: String? = null,
    /** Gambar asli logo (sisi terpanjang maks. 2048 px) dan posisi crop terakhir; null = belum ada. */
    val logoSourcePath: String? = null,
    val logoCrop: LogoCrop? = null,
)
