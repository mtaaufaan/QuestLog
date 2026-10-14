package com.rds.questlog.presentation.binder

/** Kategori Binder milik satu game. */
data class BinderCategoryUi(val id: Long, val gameId: Long, val name: String)

/** Pilihan Assign Category Sheet: id kategori yang dipilih dan nama kategori baru yang dibuat di sheet. */
data class CategoryAssignment(val categoryIds: Set<Long>, val newNames: List<String>)
