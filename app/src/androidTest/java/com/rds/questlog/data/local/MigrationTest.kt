package com.rds.questlog.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Memastikan data v1 selamat saat naik ke v2 (kolom `source_pages.failure_reason`). */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), QuestLogDatabase::class.java)

    @Test
    fun migrate1To2MempertahankanDataDanMenambahKolomAlasanGagal() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL("INSERT INTO games (id, title, created_at) VALUES (1, 'Dragon Quest VII', 0)")
            execSQL("INSERT INTO articles (id, game_id, title, created_at) VALUES (1, 1, 'Walkthrough', 0)")
            execSQL(
                "INSERT INTO source_pages (id, article_id, source_url, page_order, status) " +
                    "VALUES (1, 1, 'https://example.com/1', 1, 'FAILED')",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)

        migrated.query("SELECT status, failure_reason FROM source_pages WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("FAILED", cursor.getString(0))
            assertTrue(cursor.isNull(1))
        }
        migrated.execSQL("UPDATE source_pages SET failure_reason = 'HTTP:404' WHERE id = 1")
        migrated.query("SELECT failure_reason FROM source_pages WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("HTTP:404", it.getString(0))
        }
    }

    @Test
    fun migrate2To3MempertahankanCheckpointDanMengizinkanPosisiTanpaAnchor() {
        helper.createDatabase(DB_NAME, 2).apply {
            execSQL("INSERT INTO games (id, title, created_at) VALUES (1, 'G', 0)")
            execSQL("INSERT INTO articles (id, game_id, title, created_at) VALUES (1, 1, 'A', 0)")
            execSQL("INSERT INTO articles (id, game_id, title, created_at) VALUES (2, 1, 'B', 0)")
            execSQL(
                "INSERT INTO source_pages (id, article_id, source_url, page_order, status) " +
                    "VALUES (1, 1, 'https://example.com/1', 1, 'COMPLETED')",
            )
            execSQL(
                "INSERT INTO content_nodes (id, article_id, source_page_id, node_type, display_order) " +
                    "VALUES (5, 1, 1, 'p', 7)",
            )
            execSQL(
                "INSERT INTO checkpoints " +
                    "(article_id, anchor_node_id, fallback_order, last_visited_node_id, updated_at) " +
                    "VALUES (1, 5, 7, 5, 100)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 3, true, MIGRATION_1_2, MIGRATION_2_3)

        migrated.query(
            "SELECT anchor_node_id, fallback_order, last_visited_node_id FROM checkpoints WHERE article_id = 1",
        ).use {
            assertTrue(it.moveToFirst())
            assertEquals(5, it.getInt(0))
            assertEquals(7, it.getInt(1))
            assertEquals(5, it.getInt(2))
        }
        migrated.query("SELECT read_mode FROM articles WHERE id = 1").use {
            it.moveToFirst()
            assertTrue(it.isNull(0))
        }
        // Setelah migrasi, baris tanpa anchor (hanya posisi baca otomatis) diperbolehkan.
        migrated.execSQL(
            "INSERT INTO checkpoints (article_id, last_visited_node_id, updated_at) VALUES (2, NULL, 200)",
        )
    }

    @Test
    fun migrate3To4HanyaMenambahTabelBookmarkDanCatatanDanDataMvpUtuh() {
        helper.createDatabase(DB_NAME, 3).apply {
            execSQL("INSERT INTO games (id, title, created_at) VALUES (1, 'Dragon Quest VII', 0)")
            execSQL(
                "INSERT INTO articles (id, game_id, title, created_at, read_mode) " +
                    "VALUES (1, 1, 'Walkthrough', 5, 'PAGED')",
            )
            execSQL(
                "INSERT INTO source_pages (id, article_id, source_url, page_order, status, order_start, order_end) " +
                    "VALUES (1, 1, 'https://example.com/1', 1, 'COMPLETED', 1, 1000)",
            )
            execSQL(
                "INSERT INTO content_nodes (id, article_id, source_page_id, node_type, display_order, text_content) " +
                    "VALUES (5, 1, 1, 'p', 7, 'Paragraf')",
            )
            execSQL(
                "INSERT INTO checkpoints " +
                    "(article_id, anchor_node_id, fallback_order, last_visited_node_id, updated_at) " +
                    "VALUES (1, 5, 7, 5, 100)",
            )
            execSQL("INSERT INTO app_config (`key`, value) VALUES ('is_premium', '1')")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 4, true, MIGRATION_3_4)

        // Data MVP utuh.
        assertEquals("Dragon Quest VII", migrated.text("SELECT title FROM games WHERE id = 1"))
        assertEquals("PAGED", migrated.text("SELECT read_mode FROM articles WHERE id = 1"))
        assertEquals("Paragraf", migrated.text("SELECT text_content FROM content_nodes WHERE id = 5"))
        assertEquals("5", migrated.text("SELECT anchor_node_id FROM checkpoints WHERE article_id = 1"))
        assertEquals("1", migrated.text("SELECT value FROM app_config WHERE `key` = 'is_premium'"))
        // Tabel baru ada dan kosong, lengkap dengan indeksnya.
        assertEquals("0", migrated.text("SELECT COUNT(*) FROM bookmarks"))
        assertEquals("0", migrated.text("SELECT COUNT(*) FROM article_notes"))
        assertEquals(
            "idx_bookmarks_article,idx_bookmarks_node",
            migrated.text(
                "SELECT group_concat(name) FROM (SELECT name FROM sqlite_master " +
                    "WHERE type = 'index' AND tbl_name = 'bookmarks' AND name LIKE 'idx_%' ORDER BY name)",
            ),
        )
        // Tidak ada tabel checklist (ditunda dari v1).
        assertEquals("0", migrated.text("SELECT COUNT(*) FROM sqlite_master WHERE name LIKE 'checklist%'"))

        // Bookmark dan catatan bisa dibuat, dan hapus artikel ikut menghapusnya (CASCADE) tanpa menyentuh game.
        migrated.execSQL("PRAGMA foreign_keys = ON")
        migrated.execSQL(
            "INSERT INTO bookmarks (article_id, node_id, fallback_order, label, created_at) " +
                "VALUES (1, 5, 7, 'Boss', 1)",
        )
        migrated.execSQL("INSERT INTO article_notes (article_id, content, updated_at) VALUES (1, 'catatan', 1)")
        assertEquals("", migrated.text("SELECT snippet FROM bookmarks WHERE article_id = 1"))
        migrated.execSQL("DELETE FROM articles WHERE id = 1")
        assertEquals("0", migrated.text("SELECT COUNT(*) FROM bookmarks"))
        assertEquals("0", migrated.text("SELECT COUNT(*) FROM article_notes"))
        assertEquals("1", migrated.text("SELECT COUNT(*) FROM games"))
    }

    @Test
    fun migrate4To5HanyaMenambahTabelV11DanDataV10Utuh() {
        helper.createDatabase(DB_NAME, 4).apply {
            execSQL("INSERT INTO games (id, title, created_at) VALUES (1, 'Dragon Quest VII', 0)")
            execSQL("INSERT INTO games (id, title, created_at) VALUES (2, 'Game Kosong Lama', 0)")
            execSQL("INSERT INTO articles (id, game_id, title, created_at) VALUES (1, 1, 'Walkthrough', 5)")
            execSQL(
                "INSERT INTO source_pages (id, article_id, source_url, page_order, status, order_start, order_end) " +
                    "VALUES (1, 1, 'https://example.com/1', 1, 'COMPLETED', 1, 1000)",
            )
            execSQL(
                "INSERT INTO content_nodes (id, article_id, source_page_id, node_type, display_order, text_content) " +
                    "VALUES (5, 1, 1, 'p', 7, 'Paragraf')",
            )
            execSQL(
                "INSERT INTO checkpoints " +
                    "(article_id, anchor_node_id, fallback_order, last_visited_node_id, updated_at) " +
                    "VALUES (1, 5, 7, 5, 100)",
            )
            execSQL(
                "INSERT INTO bookmarks (article_id, node_id, fallback_order, label, snippet, created_at) " +
                    "VALUES (1, 5, 7, 'Boss', 'cuplikan', 1)",
            )
            execSQL("INSERT INTO article_notes (article_id, content, updated_at) VALUES (1, 'catatan', 1)")
            execSQL("INSERT INTO app_config (`key`, value) VALUES ('is_premium', '1')")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 5, true, MIGRATION_4_5)

        // Data v1.0 utuh (termasuk game kosong lama: migrasi tidak menghapus apa pun).
        assertEquals("2", migrated.text("SELECT COUNT(*) FROM games"))
        assertEquals("Walkthrough", migrated.text("SELECT title FROM articles WHERE id = 1"))
        assertEquals("Paragraf", migrated.text("SELECT text_content FROM content_nodes WHERE id = 5"))
        assertEquals("5", migrated.text("SELECT anchor_node_id FROM checkpoints WHERE article_id = 1"))
        assertEquals("Boss", migrated.text("SELECT label FROM bookmarks WHERE article_id = 1"))
        assertEquals("catatan", migrated.text("SELECT content FROM article_notes WHERE article_id = 1"))
        assertEquals("1", migrated.text("SELECT value FROM app_config WHERE `key` = 'is_premium'"))
        // Tabel baru ada, kosong, lengkap dengan indeksnya.
        listOf("game_progress", "binder_categories", "article_binder").forEach {
            assertEquals("0", migrated.text("SELECT COUNT(*) FROM $it"))
        }
        assertEquals(
            "idx_article_binder_category,idx_binder_categories_game,idx_binder_categories_game_name," +
                "idx_game_progress_article",
            migrated.text(
                "SELECT group_concat(name) FROM (SELECT name FROM sqlite_master " +
                    "WHERE type = 'index' AND name LIKE 'idx_%' AND tbl_name IN " +
                    "('game_progress', 'binder_categories', 'article_binder') ORDER BY name)",
            ),
        )

        migrated.execSQL("PRAGMA foreign_keys = ON")
        migrated.execSQL("INSERT INTO game_progress (game_id, article_id) VALUES (1, 1)")
        migrated.execSQL(
            "INSERT INTO binder_categories (id, game_id, name, position, created_at) VALUES (1, 1, 'Koleksi', 1, 1)",
        )
        migrated.execSQL("INSERT INTO article_binder (article_id, category_id) VALUES (1, 1)")
        // Nama kategori unik per game tanpa peduli huruf besar-kecil.
        val duplicate = runCatching {
            migrated.execSQL(
                "INSERT INTO binder_categories (game_id, name, position, created_at) VALUES (1, 'KOLEKSI', 2, 2)",
            )
        }
        assertTrue(duplicate.isFailure)
        migrated.execSQL(
            "INSERT INTO binder_categories (game_id, name, position, created_at) VALUES (2, 'koleksi', 1, 2)",
        )
        // Hanya satu artikel utama per game.
        assertTrue(
            runCatching { migrated.execSQL("INSERT INTO game_progress (game_id, article_id) VALUES (1, 1)") }.isFailure,
        )
    }

    @Test
    fun dataMvpV3NaikLangsungKeV5MelaluiDuaMigrasiDanBisaDipakaiFiturV1() {
        helper.createDatabase(DB_NAME, 3).apply {
            execSQL("INSERT INTO games (id, title, created_at) VALUES (1, 'Dragon Quest VII', 0)")
            execSQL(
                "INSERT INTO articles (id, game_id, title, created_at, read_mode) " +
                    "VALUES (1, 1, 'Walkthrough', 5, 'PAGED')",
            )
            execSQL(
                "INSERT INTO source_pages (id, article_id, source_url, page_order, status, order_start, order_end) " +
                    "VALUES (1, 1, 'https://example.com/1', 1, 'COMPLETED', 1, 1000)",
            )
            execSQL(
                "INSERT INTO content_nodes (id, article_id, source_page_id, node_type, display_order, text_content) " +
                    "VALUES (5, 1, 1, 'p', 7, 'Paragraf')",
            )
            execSQL(
                "INSERT INTO checkpoints " +
                    "(article_id, anchor_node_id, fallback_order, last_visited_node_id, updated_at) " +
                    "VALUES (1, 5, 7, 5, 100)",
            )
            execSQL("INSERT INTO app_config (`key`, value) VALUES ('is_premium', '1')")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 5, true, MIGRATION_3_4, MIGRATION_4_5)

        assertEquals("Dragon Quest VII", migrated.text("SELECT title FROM games WHERE id = 1"))
        assertEquals("PAGED", migrated.text("SELECT read_mode FROM articles WHERE id = 1"))
        assertEquals("Paragraf", migrated.text("SELECT text_content FROM content_nodes WHERE id = 5"))
        assertEquals("5", migrated.text("SELECT anchor_node_id FROM checkpoints WHERE article_id = 1"))
        assertEquals("1", migrated.text("SELECT value FROM app_config WHERE `key` = 'is_premium'"))
        listOf("bookmarks", "article_notes", "game_progress", "binder_categories", "article_binder").forEach {
            assertEquals("0", migrated.text("SELECT COUNT(*) FROM $it"))
        }
        // Fitur v1.0 dan v1.1 langsung bisa dipakai di atas data MVP, dan hapus game membersihkan semuanya.
        migrated.execSQL("PRAGMA foreign_keys = ON")
        migrated.execSQL(
            "INSERT INTO bookmarks (article_id, node_id, fallback_order, label, created_at) " +
                "VALUES (1, 5, 7, 'Boss', 1)",
        )
        migrated.execSQL("INSERT INTO article_notes (article_id, content, updated_at) VALUES (1, 'catatan', 1)")
        migrated.execSQL("INSERT INTO game_progress (game_id, article_id) VALUES (1, 1)")
        migrated.execSQL(
            "INSERT INTO binder_categories (id, game_id, name, position, created_at) VALUES (1, 1, 'Utama', 1, 1)",
        )
        migrated.execSQL("INSERT INTO article_binder (article_id, category_id) VALUES (1, 1)")
        migrated.execSQL("DELETE FROM games WHERE id = 1")
        listOf(
            "articles",
            "bookmarks",
            "article_notes",
            "game_progress",
            "binder_categories",
            "article_binder",
        ).forEach {
            assertEquals("0", migrated.text("SELECT COUNT(*) FROM $it"))
        }
    }

    @Test
    fun migrate5To6HanyaMenambahKolomLogoDanDataLamaUtuh() {
        helper.createDatabase(DB_NAME, 5).apply {
            execSQL("INSERT INTO games (id, title, created_at) VALUES (1, 'Dragon Quest VII', 0)")
            execSQL("INSERT INTO articles (id, game_id, title, created_at) VALUES (1, 1, 'Walkthrough', 0)")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 6, true, MIGRATION_5_6)

        assertEquals("Dragon Quest VII", migrated.text("SELECT title FROM games WHERE id = 1"))
        assertEquals("Walkthrough", migrated.text("SELECT title FROM articles WHERE id = 1"))
        migrated.query("SELECT logo_path FROM games WHERE id = 1").use {
            it.moveToFirst()
            assertTrue(it.isNull(0))
        }
        migrated.execSQL("UPDATE games SET logo_path = 'game_logos/1.webp' WHERE id = 1")
        assertEquals("game_logos/1.webp", migrated.text("SELECT logo_path FROM games WHERE id = 1"))
    }

    @Test
    fun migrate6To7HanyaMenambahKolomGambarAsliDanPosisiCropDanLogoV6Utuh() {
        helper.createDatabase(DB_NAME, 6).apply {
            execSQL(
                "INSERT INTO games (id, title, created_at, logo_path) " +
                    "VALUES (1, 'Dragon Quest VII', 0, 'game_logos/1.webp')",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 7, true, MIGRATION_6_7)

        assertEquals("game_logos/1.webp", migrated.text("SELECT logo_path FROM games WHERE id = 1"))
        migrated.query(
            "SELECT logo_source_path, logo_crop_zoom, logo_crop_x, logo_crop_y FROM games WHERE id = 1",
        ).use {
            it.moveToFirst()
            for (column in 0..3) assertTrue(it.isNull(column))
        }
        migrated.execSQL(
            "UPDATE games SET logo_source_path = 'game_logos/1_src.webp', logo_crop_zoom = 2.5, " +
                "logo_crop_x = -12.5, logo_crop_y = 7 WHERE id = 1",
        )
        assertEquals("2.5", migrated.text("SELECT logo_crop_zoom FROM games WHERE id = 1"))
    }

    @Test
    fun migrasiPenuhDariSkemaPertamaSampaiV7Valid() {
        helper.createDatabase(DB_NAME, 1).close()

        helper.runMigrationsAndValidate(
            DB_NAME,
            7,
            true,
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
        )
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.text(sql: String): String = query(sql).use {
        it.moveToFirst()
        it.getString(0)
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
