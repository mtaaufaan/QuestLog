package com.rds.questlog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rds.questlog.data.local.entity.AppConfigEntity
import com.rds.questlog.data.local.entity.ArticleEntity
import com.rds.questlog.data.local.entity.CheckpointEntity
import com.rds.questlog.data.local.entity.ContentNodeEntity
import com.rds.questlog.data.local.entity.GameEntity
import com.rds.questlog.data.local.entity.ImageEntity
import com.rds.questlog.data.local.entity.SourcePageEntity

/** Seluruh tabel dibuat sekaligus agar tidak perlu migrasi bertahap per sprint. DAO ditambah per sprint fitur. */
@Database(
    entities = [
        GameEntity::class,
        ArticleEntity::class,
        SourcePageEntity::class,
        ContentNodeEntity::class,
        CheckpointEntity::class,
        ImageEntity::class,
        AppConfigEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class QuestLogDatabase : RoomDatabase()
