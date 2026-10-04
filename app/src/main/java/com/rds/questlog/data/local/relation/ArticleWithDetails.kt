package com.rds.questlog.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.rds.questlog.data.local.entity.ArticleEntity
import com.rds.questlog.data.local.entity.CheckpointEntity
import com.rds.questlog.data.local.entity.GameEntity
import com.rds.questlog.data.local.entity.SourcePageEntity

/** Artikel beserta game, halaman sumber, dan checkpoint-nya; urutan [pages] belum terjamin. */
data class ArticleWithDetails(
    @Embedded val article: ArticleEntity,
    @Relation(parentColumn = "game_id", entityColumn = "id") val game: GameEntity,
    @Relation(parentColumn = "id", entityColumn = "article_id") val pages: List<SourcePageEntity>,
    @Relation(parentColumn = "id", entityColumn = "article_id") val checkpoint: CheckpointEntity?,
)
