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

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
