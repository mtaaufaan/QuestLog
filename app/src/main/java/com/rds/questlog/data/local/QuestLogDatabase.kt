package com.rds.questlog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rds.questlog.data.local.dao.AppConfigDao
import com.rds.questlog.data.local.dao.ArticleDao
import com.rds.questlog.data.local.dao.ArticleNoteDao
import com.rds.questlog.data.local.dao.BinderDao
import com.rds.questlog.data.local.dao.BinderMemberDao
import com.rds.questlog.data.local.dao.BookmarkDao
import com.rds.questlog.data.local.dao.CheckpointDao
import com.rds.questlog.data.local.dao.ContentNodeDao
import com.rds.questlog.data.local.dao.GameDao
import com.rds.questlog.data.local.dao.GameProgressDao
import com.rds.questlog.data.local.dao.ImageDao
import com.rds.questlog.data.local.dao.PageLayoutDao
import com.rds.questlog.data.local.dao.SourcePageDao
import com.rds.questlog.data.local.entity.AppConfigEntity
import com.rds.questlog.data.local.entity.ArticleBinderEntity
import com.rds.questlog.data.local.entity.ArticleEntity
import com.rds.questlog.data.local.entity.ArticleNoteEntity
import com.rds.questlog.data.local.entity.BinderCategoryEntity
import com.rds.questlog.data.local.entity.BookmarkEntity
import com.rds.questlog.data.local.entity.CheckpointEntity
import com.rds.questlog.data.local.entity.ContentNodeEntity
import com.rds.questlog.data.local.entity.GameEntity
import com.rds.questlog.data.local.entity.GameProgressEntity
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
        BookmarkEntity::class,
        ArticleNoteEntity::class,
        GameProgressEntity::class,
        BinderCategoryEntity::class,
        ArticleBinderEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
abstract class QuestLogDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun articleDao(): ArticleDao
    abstract fun sourcePageDao(): SourcePageDao

    abstract fun pageLayoutDao(): PageLayoutDao
    abstract fun contentNodeDao(): ContentNodeDao
    abstract fun imageDao(): ImageDao
    abstract fun checkpointDao(): CheckpointDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun articleNoteDao(): ArticleNoteDao
    abstract fun gameProgressDao(): GameProgressDao
    abstract fun binderDao(): BinderDao
    abstract fun binderMemberDao(): BinderMemberDao
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

/**
 * v3 -> v4 (v1.0): HANYA menambah tabel `bookmarks` (banyak per artikel) dan `article_notes` (satu per artikel).
 * Tidak ada tabel atau kolom MVP yang diubah atau dihapus, jadi seluruh data lama utuh. `bookmarks.node_id` tanpa
 * CASCADE: dipindahkan manual saat halaman dihapus atau diunduh ulang. Checklist ditunda dari v1 (tidak dibuat).
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS bookmarks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, article_id INTEGER NOT NULL, " +
                "node_id INTEGER NOT NULL, fallback_order INTEGER NOT NULL, label TEXT NOT NULL, " +
                "snippet TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, " +
                "FOREIGN KEY(article_id) REFERENCES articles(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(node_id) REFERENCES content_nodes(id) ON UPDATE NO ACTION ON DELETE NO ACTION)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmarks_article ON bookmarks (article_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmarks_node ON bookmarks (node_id)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS article_notes (" +
                "article_id INTEGER NOT NULL, content TEXT NOT NULL DEFAULT '', updated_at INTEGER NOT NULL, " +
                "PRIMARY KEY(article_id), " +
                "FOREIGN KEY(article_id) REFERENCES articles(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
    }
}

/**
 * v4 -> v5 (v1.1): HANYA menambah tabel `game_progress` (artikel utama per game, tanpa anchor), `binder_categories`
 * (kategori per game; nama unik tanpa peduli huruf besar-kecil) dan `article_binder` (relasi M:N). Tidak ada tabel atau
 * kolom lama yang diubah atau dihapus, jadi seluruh data v1.0 utuh. Semua kunci asing CASCADE: hapus game menghapus
 * penanda artikel utama dan kategorinya, hapus artikel utama menghapus barisnya, hapus kategori hanya mencabut relasi.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS game_progress (" +
                "game_id INTEGER NOT NULL, article_id INTEGER NOT NULL, PRIMARY KEY(game_id), " +
                "FOREIGN KEY(game_id) REFERENCES games(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(article_id) REFERENCES articles(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_game_progress_article ON game_progress (article_id)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS binder_categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, game_id INTEGER NOT NULL, " +
                "name TEXT NOT NULL COLLATE NOCASE, position INTEGER NOT NULL, created_at INTEGER NOT NULL, " +
                "FOREIGN KEY(game_id) REFERENCES games(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_binder_categories_game ON binder_categories (game_id)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_binder_categories_game_name ON binder_categories (game_id, name)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS article_binder (" +
                "article_id INTEGER NOT NULL, category_id INTEGER NOT NULL, PRIMARY KEY(article_id, category_id), " +
                "FOREIGN KEY(article_id) REFERENCES articles(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(category_id) REFERENCES binder_categories(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_article_binder_category ON article_binder (category_id)")
    }
}

/**
 * v5 -> v6 (v1.1, QL-38): HANYA menambah kolom `games.logo_path` (nullable; path logo kustom relatif ke `filesDir`).
 * Tidak ada tabel atau kolom lama yang diubah, jadi seluruh data utuh; game lama tampil dengan inisial.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE games ADD COLUMN logo_path TEXT")
    }
}

/**
 * v6 -> v7 (v1.2, QL-38): HANYA menambah kolom `games.logo_source_path` (gambar asli logo) dan
 * `logo_crop_zoom` / `logo_crop_x` / `logo_crop_y` (posisi crop terakhir), semuanya nullable. Tidak ada tabel atau
 * kolom lama yang diubah, jadi seluruh data utuh; logo hasil v6 tetap tampil (tanpa gambar asli).
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE games ADD COLUMN logo_source_path TEXT")
        db.execSQL("ALTER TABLE games ADD COLUMN logo_crop_zoom REAL")
        db.execSQL("ALTER TABLE games ADD COLUMN logo_crop_x REAL")
        db.execSQL("ALTER TABLE games ADD COLUMN logo_crop_y REAL")
    }
}
