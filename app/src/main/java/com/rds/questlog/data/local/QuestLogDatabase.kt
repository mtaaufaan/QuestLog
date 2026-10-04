package com.rds.questlog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rds.questlog.data.local.dao.ArticleDao
import com.rds.questlog.data.local.dao.ContentNodeDao
import com.rds.questlog.data.local.dao.GameDao
import com.rds.questlog.data.local.dao.ImageDao
import com.rds.questlog.data.local.dao.SourcePageDao
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
    version = 2,
    exportSchema = true,
)
abstract class QuestLogDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun articleDao(): ArticleDao
    abstract fun sourcePageDao(): SourcePageDao
    abstract fun contentNodeDao(): ContentNodeDao
    abstract fun imageDao(): ImageDao
}

/** v1 → v2: alasan gagal per halaman sumber (ditampilkan di Scrape Error Dialog). */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE source_pages ADD COLUMN failure_reason TEXT")
    }
}
