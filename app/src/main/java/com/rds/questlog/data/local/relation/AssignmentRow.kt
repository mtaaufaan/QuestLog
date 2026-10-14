package com.rds.questlog.data.local.relation

import androidx.room.ColumnInfo

/** Satu penetapan artikel ke kategori Binder. */
data class AssignmentRow(
    @ColumnInfo(name = "article_id") val articleId: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
)
