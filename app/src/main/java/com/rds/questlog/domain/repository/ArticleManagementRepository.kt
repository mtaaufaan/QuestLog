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
}
