package com.rds.questlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rds.questlog.data.local.entity.CheckpointEntity

@Dao
interface CheckpointDao {

    @Query("SELECT * FROM checkpoints WHERE article_id = :articleId")
    suspend fun get(articleId: Long): CheckpointEntity?

    /** Satu baris per artikel (article_id adalah primary key), jadi REPLACE sama dengan upsert. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checkpoint: CheckpointEntity)

    /** Memindahkan anchor yang menunjuk node halaman [pageId] ke [nodeId] (null = checkpoint manual dilepas). */
    @Query(
        "UPDATE checkpoints SET anchor_node_id = :nodeId, fallback_order = :order " +
            "WHERE article_id = :articleId AND anchor_node_id IN " +
            "(SELECT id FROM content_nodes WHERE source_page_id = :pageId)",
    )
    suspend fun moveAnchor(articleId: Long, pageId: Long, nodeId: Long?, order: Int?)

    /** Memindahkan posisi baca terakhir yang menunjuk node halaman [pageId] ke [nodeId] (null = dilepas). */
    @Query(
        "UPDATE checkpoints SET last_visited_node_id = :nodeId WHERE article_id = :articleId AND " +
            "last_visited_node_id IN (SELECT id FROM content_nodes WHERE source_page_id = :pageId)",
    )
    suspend fun moveLastVisited(articleId: Long, pageId: Long, nodeId: Long?)

    /** Menghitung ulang fallback_order dari posisi anchor sekarang (setelah display_order bergeser). */
    @Query(
        "UPDATE checkpoints SET fallback_order = " +
            "(SELECT display_order FROM content_nodes WHERE id = checkpoints.anchor_node_id) " +
            "WHERE article_id = :articleId AND anchor_node_id IS NOT NULL",
    )
    suspend fun refreshFallback(articleId: Long)
}
