package com.rds.questlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rds.questlog.data.local.entity.ImageEntity

@Dao
interface ImageDao {

    @Insert
    suspend fun insertAll(images: List<ImageEntity>)

    @Query("SELECT DISTINCT filename FROM images WHERE article_id = :articleId")
    suspend fun filenamesOf(articleId: Long): List<String>

    /** File fisik hanya boleh dihapus bila tidak ada lagi baris yang merujuk filename ini (dedup antar artikel). */
    @Query("SELECT COUNT(*) FROM images WHERE filename = :filename")
    suspend fun countByFilename(filename: String): Int
}
