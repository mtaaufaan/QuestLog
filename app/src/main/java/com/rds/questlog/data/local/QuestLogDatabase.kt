package com.rds.questlog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rds.questlog.data.local.dao.AppConfigDao
import com.rds.questlog.data.local.dao.ArticleDao
import com.rds.questlog.data.local.dao.CheckpointDao
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
    version = 3,
    exportSchema = true,
)
abstract class QuestLogDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun articleDao(): ArticleDao
    abstract fun sourcePageDao(): SourcePageDao
    abstract fun contentNodeDao(): ContentNodeDao
    abstract fun imageDao(): ImageDao
    abstract fun checkpointDao(): CheckpointDao
    abstract fun appConfigDao(): AppConfigDao
}

/** v1 → v2: alasan gagal per halaman sumber (ditampilkan di Scrape Error Dialog). */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE source_pages ADD COLUMN failure_reason TEXT")
    }
}

/**
 * v2 -> v3: kolom articles.read_mode (mode baca per artikel) dan checkpoints.anchor_node_id / fallback_order menjadi
 * nullable agar posisi baca otomatis bisa disimpan tanpa checkpoint manual. SQLite tidak bisa mengubah nullability
 * kolom, jadi tabel dibuat ulang dan datanya disalin.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE articles ADD COLUMN read_mode TEXT")
        db.execSQL(
            "CREATE TABLE checkpoints_new (" +
                "article_id INTEGER NOT NULL, anchor_node_id INTEGER, fallback_order INTEGER, " +
                "last_visited_node_id INTEGER, last_read_at INTEGER, read_progress REAL, " +
                "updated_at INTEGER NOT NULL, PRIMARY KEY(article_id), " +
                "FOREIGN KEY(article_id) REFERENCES articles(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(anchor_node_id) REFERENCES content_nodes(id) ON UPDATE NO ACTION ON DELETE NO ACTION, " +
                "FOREIGN KEY(last_visited_node_id) REFERENCES content_nodes(id) " +
                "ON UPDATE NO ACTION ON DELETE NO ACTION)",
        )
        db.execSQL(
            "INSERT INTO checkpoints_new SELECT article_id, anchor_node_id, fallback_order, last_visited_node_id, " +
                "last_read_at, read_progress, updated_at FROM checkpoints",
        )
        db.execSQL("DROP TABLE checkpoints")
        db.execSQL("ALTER TABLE checkpoints_new RENAME TO checkpoints")
    }
}
