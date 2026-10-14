package com.rds.questlog.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.rds.questlog.data.local.entity.ArticleBinderEntity
import com.rds.questlog.data.local.entity.ArticleEntity
import com.rds.questlog.data.local.entity.ArticleNoteEntity
import com.rds.questlog.data.local.entity.BinderCategoryEntity
import com.rds.questlog.data.local.entity.BookmarkEntity
import com.rds.questlog.data.local.entity.CheckpointEntity
import com.rds.questlog.data.local.entity.GameEntity
import com.rds.questlog.data.local.entity.GameProgressEntity
import com.rds.questlog.data.local.entity.SourcePageEntity

/** Artikel beserta game, halaman sumber, dan checkpoint-nya; urutan [pages] belum terjamin. */
data class ArticleWithDetails(
    @Embedded val article: ArticleEntity,
    @Relation(parentColumn = "game_id", entityColumn = "id") val game: GameEntity,
    @Relation(parentColumn = "id", entityColumn = "article_id") val pages: List<SourcePageEntity>,
    @Relation(parentColumn = "id", entityColumn = "article_id") val checkpoint: CheckpointEntity?,
    @Relation(parentColumn = "id", entityColumn = "article_id") val bookmarks: List<BookmarkEntity>,
    @Relation(parentColumn = "id", entityColumn = "article_id") val note: ArticleNoteEntity?,
    @Relation(parentColumn = "game_id", entityColumn = "game_id") val gameProgress: GameProgressEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ArticleBinderEntity::class,
            parentColumn = "article_id",
            entityColumn = "category_id",
        ),
    )
    val categories: List<BinderCategoryEntity>,
)
