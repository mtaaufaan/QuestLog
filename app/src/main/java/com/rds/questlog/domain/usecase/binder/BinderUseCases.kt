package com.rds.questlog.domain.usecase.binder

import com.rds.questlog.domain.error.QuestLogError.ValidationError
import com.rds.questlog.domain.model.Binder
import com.rds.questlog.domain.model.MAX_CATEGORY_NAME
import com.rds.questlog.domain.model.PremiumFeature
import com.rds.questlog.domain.model.normalizeCategoryName
import com.rds.questlog.domain.repository.BinderRepository
import com.rds.questlog.domain.usecase.premium.PremiumGuard
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Nama dinormalisasi lalu divalidasi: wajib diisi dan maksimal 100 karakter. */
internal fun validCategoryName(input: String): String {
    val name = normalizeCategoryName(input)
    if (name.isEmpty()) throw ValidationError.EmptyCategoryName
    if (name.length > MAX_CATEGORY_NAME) throw ValidationError.CategoryNameTooLong
    return name
}

/** Kategori dan penetapan artikel satu game; selalu terbaca (juga saat premium hilang). */
class ObserveBinderUseCase @Inject constructor(private val repository: BinderRepository) {
    /** Emit ulang tiap kategori atau penetapannya berubah. */
    /** Emit ulang tiap kategori atau penetapannya berubah. */
    operator fun invoke(gameId: Long): Flow<Binder> = repository.observe(gameId)
}

/**
 * Membuat kategori di [gameId] (QL-32, premium). Nama dinormalisasi (trim, spasi dirapikan, huruf awal kata kapital),
 * wajib, ≤ 100 karakter, dan unik per game tanpa peduli huruf (`DuplicateCategory`). Tidak ada batas jumlah.
 */
class CreateCategoryUseCase @Inject constructor(
    private val guard: PremiumGuard,
    private val repository: BinderRepository,
) {
    /** Mengembalikan id kategori baru, atau kegagalan (guard premium, nama tidak valid, duplikat). */
    /** Mengembalikan id kategori baru, atau kegagalan (guard premium, nama tidak valid, duplikat). */
    suspend operator fun invoke(gameId: Long, name: String): Result<Long> = writing {
        guard.requireWrite(PremiumFeature.GAME_BINDER)
        val valid = validCategoryName(name)
        if (repository.findByName(gameId, valid) != null) throw ValidationError.DuplicateCategory(valid)
        repository.createCategory(gameId, valid)
    }
}

/** Mengganti nama kategori; aturan seperti membuat, dengan namanya sendiri dikecualikan dari pemeriksaan duplikat. */
class RenameCategoryUseCase @Inject constructor(
    private val guard: PremiumGuard,
    private val repository: BinderRepository,
) {
    /** Mengganti nama kategori [categoryId]; kategori yang sudah terhapus -> `CategoryNotFound`. */
    /** Mengganti nama kategori [categoryId]; kategori yang sudah terhapus -> `CategoryNotFound`. */
    suspend operator fun invoke(categoryId: Long, name: String): Result<Unit> = writing {
        guard.requireWrite(PremiumFeature.GAME_BINDER)
        val category = repository.category(categoryId) ?: throw ValidationError.CategoryNotFound
        val valid = validCategoryName(name)
        val clash = repository.findByName(category.gameId, valid)
        if (clash != null && clash.id != categoryId) throw ValidationError.DuplicateCategory(valid)
        repository.renameCategory(categoryId, valid)
    }
}

/** Menghapus kategori; artikelnya tidak terhapus dan yang tak punya kategori lain masuk "Tanpa kategori". */
class DeleteCategoryUseCase @Inject constructor(
    private val guard: PremiumGuard,
    private val repository: BinderRepository,
) {
    /** Menghapus kategori [categoryId] setelah guard premium. */
    /** Menghapus kategori [categoryId] setelah guard premium. */
    suspend operator fun invoke(categoryId: Long): Result<Unit> = writing {
        guard.requireWrite(PremiumFeature.GAME_BINDER)
        repository.deleteCategory(categoryId)
    }
}

/** Memasukkan atau mengeluarkan satu artikel dari satu kategori (picker di Binder). */
class SetArticleCategoryUseCase @Inject constructor(
    private val guard: PremiumGuard,
    private val repository: BinderRepository,
) {
    /** [member] true = masukkan, false = keluarkan; kategori harus milik game artikel itu. */
    /** [member] true = masukkan, false = keluarkan; kategori harus milik game artikel itu. */
    suspend operator fun invoke(articleId: Long, categoryId: Long, member: Boolean): Result<Unit> = writing {
        guard.requireWrite(PremiumFeature.GAME_BINDER)
        repository.setMembership(articleId, categoryId, member)
    }
}

/**
 * Menetapkan kategori satu artikel dari Assign Category Sheet (QL-33, premium): kategori terpilih ditambah kategori
 * baru bernama [newNames] (dinormalisasi dan divalidasi seperti membuat kategori; nama yang sudah ada dipakai ulang).
 * Satu artikel boleh masuk beberapa kategori.
 */
class AssignCategoriesUseCase @Inject constructor(
    private val guard: PremiumGuard,
    private val repository: BinderRepository,
) {
    /** Menetapkan kategori [articleId] persis sebesar [categoryIds] ditambah [newNames]. */
    suspend operator fun invoke(articleId: Long, categoryIds: Set<Long>, newNames: List<String>): Result<Unit> =
        writing {
            guard.requireWrite(PremiumFeature.GAME_BINDER)
            repository.assign(articleId, categoryIds, newNames.map(::validCategoryName).distinctBy { it.lowercase() })
        }
}
