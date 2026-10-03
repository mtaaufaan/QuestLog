package com.rds.questlog.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Memastikan seluruh skema (7 tabel, 2 index, FK cascade) terbentuk dan bisa diisi. */
@RunWith(AndroidJUnit4::class)
class QuestLogDatabaseTest {

    private lateinit var db: QuestLogDatabase
    private lateinit var sql: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, QuestLogDatabase::class.java).build()
        sql = db.openHelper.writableDatabase
    }

    @After
    fun tearDown() = db.close()

    private fun count(query: String): Int = sql.query(query).use {
        it.moveToFirst()
        it.getInt(0)
    }

    @Test
    fun semuaTabelDanIndexTerbentuk() {
        val tables = setOf("games", "articles", "source_pages", "content_nodes", "checkpoints", "images", "app_config")
        sql.query("SELECT name FROM sqlite_master WHERE type='table'").use { c ->
            val found = generateSequence { if (c.moveToNext()) c.getString(0) else null }.toSet()
            assertTrue("tabel hilang: ${tables - found}", found.containsAll(tables))
        }
        sql.query("SELECT name FROM sqlite_master WHERE type='index'").use { c ->
            val found = generateSequence { if (c.moveToNext()) c.getString(0) else null }.toSet()
            assertTrue(found.contains("idx_content_nodes_article_order"))
            assertTrue(found.contains("idx_content_nodes_source_page"))
        }
    }

    @Test
    fun insertDummyDanCascadeDelete() {
        sql.execSQL("INSERT INTO games (id, title, created_at) VALUES (1, 'Dragon Quest VII', 0)")
        sql.execSQL("INSERT INTO articles (id, game_id, title, created_at) VALUES (1, 1, 'Walkthrough', 0)")
        sql.execSQL(
            "INSERT INTO source_pages (id, article_id, source_url, page_order, order_start, order_end) " +
                "VALUES (1, 1, 'https://example.com/1', 1, 1, 1000)",
        )
        sql.execSQL(
            "INSERT INTO content_nodes (id, article_id, source_page_id, node_type, display_order, text_content) " +
                "VALUES (1, 1, 1, 'h1', 1, 'Judul')",
        )
        sql.execSQL(
            "INSERT INTO checkpoints (article_id, anchor_node_id, fallback_order, updated_at) VALUES (1, 1, 1, 0)",
        )
        sql.execSQL("INSERT INTO app_config (key, value) VALUES ('is_premium', '0')")

        assertEquals(
            "PENDING",
            sql.query("SELECT status FROM source_pages").use {
                it.moveToFirst()
                it.getString(0)
            },
        )
        assertEquals(0, count("SELECT is_scraping_done FROM articles"))

        // Menghapus game harus menghapus artikel, checkpoint, dan content_nodes lewat cascade.
        sql.execSQL("DELETE FROM games WHERE id = 1")
        assertEquals(0, count("SELECT COUNT(*) FROM articles"))
        assertEquals(0, count("SELECT COUNT(*) FROM content_nodes"))
        assertEquals(0, count("SELECT COUNT(*) FROM checkpoints"))
        assertEquals(1, count("SELECT COUNT(*) FROM app_config"))
    }
}
