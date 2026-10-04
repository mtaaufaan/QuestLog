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

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
