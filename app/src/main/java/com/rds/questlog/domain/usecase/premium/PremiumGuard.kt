package com.rds.questlog.domain.usecase.premium

import com.rds.questlog.domain.error.QuestLogError.PremiumError
import com.rds.questlog.domain.model.FeatureAccess
import com.rds.questlog.domain.model.PremiumFeature
import com.rds.questlog.domain.model.PremiumState
import com.rds.questlog.domain.repository.AppConfigRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Guard fitur premium v1 (QL-26, SRS-FR-16). Satu-satunya tempat yang memutuskan boleh tidaknya fitur premium
 * (Bookmark, Catatan, Game Binder, Auto-detect) dipakai; UI dan UseCase fitur bertanya ke sini, tidak membaca
 * `is_premium` langsung. Tiga keadaan: premium → semua tersedia; pernah premium tetapi sekarang tidak (lost) →
 * data lama terbaca tetapi ubah dikunci; belum pernah premium → terkunci. Verifikasi Billing yang gagal tidak
 * mengubah apa pun (status terakhir dipertahankan oleh [SyncPremiumStatusUseCase]).
 */
class PremiumGuard @Inject constructor(private val appConfig: AppConfigRepository) {

    /** Stream keadaan premium; emit ulang saat berubah sehingga UI ikut terkunci/terbuka tanpa restart. */
    fun state(): Flow<PremiumState> = appConfig.observePremiumState()

    /** Stream akses ke [feature]; dasar ikon gembok, banner hanya-baca, dan visibilitas Binder. */
    fun access(feature: PremiumFeature): Flow<FeatureAccess> = state().map { accessFor(it, feature) }

    /**
     * Memastikan [feature] boleh diubah (membuat, mengubah, menghapus). Gagal dengan [PremiumError.Locked] (gratis,
     * ajak upgrade) atau [PremiumError.ReadOnly] (premium hilang). Dipanggil UseCase fitur SEBELUM menulis apa pun,
     * sehingga tidak ada data yang berubah saat ditolak.
     */
    suspend fun requireWrite(feature: PremiumFeature) {
        when (accessFor(state().first(), feature)) {
            FeatureAccess.FULL -> Unit
            FeatureAccess.READ_ONLY -> throw PremiumError.ReadOnly(feature)
            FeatureAccess.LOCKED -> throw PremiumError.Locked(feature)
        }
    }

    private fun accessFor(state: PremiumState, feature: PremiumFeature): FeatureAccess = when (state) {
        PremiumState.PREMIUM -> FeatureAccess.FULL
        PremiumState.LOST -> if (feature.hasUserData) FeatureAccess.READ_ONLY else FeatureAccess.LOCKED
        PremiumState.FREE -> FeatureAccess.LOCKED
    }
}
