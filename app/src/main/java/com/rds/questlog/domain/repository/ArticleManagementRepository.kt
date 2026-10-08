package com.rds.questlog.domain.repository

import com.rds.questlog.domain.model.GameTarget

/** Operasi pengelolaan artikel yang sudah tersimpan (Sprint 5); dipisah dari [ArticleRepository] agar tetap ramping. */
interface ArticleManagementRepository {

    /**
     * Mengubah judul dan/atau memindahkan artikel ke [target] dalam SATU transaksi: judul, game baru (bila perlu),
     * pemindahan, dan penghapusan game asal bila jadi kosong berhasil bersama atau tidak sama sekali.
     * Halaman, konten, dan checkpoint tidak disentuh.
     */
    suspend fun updateDetails(articleId: Long, title: String, target: GameTarget)

    /**
     * QL-19. Menghapus satu halaman beserta node-nya dalam SATU transaksi. Urutan wajib karena FK tanpa cascade:
     * checkpoint (anchor dan posisi baca) yang menunjuk halaman itu dipindah ke node terdekat (halaman sebelumnya,
     * lalu sesudahnya; dilepas bila tidak ada konten lain) → hapus node → hapus halaman → rapatkan nomor halaman
     * dan slot display_order. Status artikel diturunkan dari halaman sehingga ikut terhitung ulang.
     * @throws com.rds.questlog.domain.error.QuestLogError.ArticleError.LastPage halaman terakhir artikel
     * @throws com.rds.questlog.domain.error.QuestLogError.ArticleError.Busy halaman sedang diunduh
     * @throws com.rds.questlog.domain.error.QuestLogError.ArticleError.PageNotFound halaman bukan milik artikel
     */
    suspend fun deletePage(articleId: Long, pageId: Long)

    /**
     * QL-19. Menyimpan urutan halaman baru dalam satu transaksi: nomor halaman dan slot display_order (kelipatan
     * 1000) dinomori ulang, node ikut bergeser, dan fallback_order checkpoint dihitung ulang. Anchor berbasis id
     * node sehingga checkpoint tetap menunjuk elemen yang sama.
     * @param orderedPageIds seluruh id halaman artikel, berurutan sesuai urutan baru
     * @throws com.rds.questlog.domain.error.QuestLogError.ArticleError.PageNotFound bila daftar tidak cocok
     */
    suspend fun reorderPages(articleId: Long, orderedPageIds: List<Long>)
}
