package com.rds.questlog.presentation.binder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Kategori Binder milik satu game. */
data class BinderCategoryUi(val id: Long, val gameId: Long, val name: String)

/** Judul artikel utama sebuah game (untuk tombol "Lanjutkan Game · {judul}"). */
data class MainArticleRef(val articleId: Long, val title: String)

/** Pilihan Assign Category Sheet: id kategori yang dipilih dan nama kategori baru yang dibuat di sheet. */
data class CategoryAssignment(val categoryIds: Set<Long>, val newNames: List<String>)

/**
 * Penyimpanan SEMENTARA di memori untuk kategori Binder, penetapan artikel, dan artikel utama game (Sprint 9
 * Tahap 1, belum ke Room). Dibagi S0, S1, S4, dan Reader agar tampilannya konsisten; hilang saat proses mati.
 * Diganti UseCase di tahap wiring.
 */
class BinderDummyStore {
    var categories by mutableStateOf(emptyList<BinderCategoryUi>())
        private set

    /** articleId -> id kategori. */
    var assignments by mutableStateOf(emptyMap<Long, Set<Long>>())
        private set

    /** gameId -> artikel utama. */
    var mainArticles by mutableStateOf(emptyMap<Long, MainArticleRef>())
        private set

    private var nextId = 0L

    fun categoriesOf(gameId: Long): List<BinderCategoryUi> = categories.filter { it.gameId == gameId }

    fun categoryNamesOf(articleId: Long, gameId: Long): List<String> {
        val ids = assignments[articleId].orEmpty()
        return categoriesOf(gameId).filter { it.id in ids }.map { it.name }
    }

    fun create(gameId: Long, name: String): Long {
        val id = ++nextId
        categories = categories + BinderCategoryUi(id, gameId, name)
        return id
    }

    fun rename(id: Long, name: String) {
        categories = categories.map { if (it.id == id) it.copy(name = name) else it }
    }

    /** Menghapus kategori; artikelnya tidak ikut terhapus, hanya relasinya dicabut. */
    fun delete(id: Long) {
        categories = categories.filterNot { it.id == id }
        assignments = assignments.mapValues { (_, ids) -> ids - id }
    }

    fun setMembership(articleId: Long, categoryId: Long, member: Boolean) {
        val current = assignments[articleId].orEmpty()
        assignments = assignments + (articleId to if (member) current + categoryId else current - categoryId)
    }

    /** Menetapkan kategori [articleId] persis sebesar [assignment]; nama baru dibuat, yang sudah ada dipakai ulang. */
    fun assign(articleId: Long, gameId: Long, assignment: CategoryAssignment) {
        val created = assignment.newNames.map { name ->
            categoriesOf(gameId).firstOrNull { it.name.equals(name, ignoreCase = true) }?.id ?: create(gameId, name)
        }
        assignments = assignments + (articleId to assignment.categoryIds + created)
    }

    fun setMainArticle(gameId: Long, ref: MainArticleRef) {
        mainArticles = mainArticles + (gameId to ref)
    }
}

/** Instans tunggal selama tahap UI; lihat [BinderDummyStore]. */
val BinderDummy = BinderDummyStore()
