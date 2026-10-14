package com.rds.questlog.presentation.binder

import com.rds.questlog.domain.model.normalizeCategoryName

/** Apakah [name] (sudah dinormalisasi) sama dengan salah satu [others] tanpa peduli huruf besar-kecil. */
fun isDuplicateCategory(name: String, others: List<String>): Boolean =
    name.isNotEmpty() && others.any { it.equals(name, ignoreCase = true) }

/** Hasil mengetik nama di kolom "Tambah" Assign Category Sheet: tidak ada petunjuk, sudah ada, atau mirip. */
sealed interface CategorySuggestion {
    data object None : CategorySuggestion
    data class Exists(val name: String) : CategorySuggestion
    data class Similar(val name: String) : CategorySuggestion
}

/** Petunjuk untuk [draft] terhadap [existing]: sama persis (tanpa peduli huruf) → Exists, berawalan sama → Similar. */
fun suggestCategory(draft: String, existing: List<String>): CategorySuggestion {
    val name = normalizeCategoryName(draft)
    val exact = existing.firstOrNull { it.equals(name, ignoreCase = true) }
    val similar = existing.firstOrNull { it.startsWith(name, ignoreCase = true) }
    return when {
        name.isEmpty() -> CategorySuggestion.None
        exact != null -> CategorySuggestion.Exists(exact)
        similar != null -> CategorySuggestion.Similar(similar)
        else -> CategorySuggestion.None
    }
}
