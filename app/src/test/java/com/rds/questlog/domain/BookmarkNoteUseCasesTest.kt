package com.rds.questlog.domain

import com.rds.questlog.domain.error.QuestLogError.PremiumError
import com.rds.questlog.domain.error.QuestLogError.ValidationError
import com.rds.questlog.domain.model.Bookmark
import com.rds.questlog.domain.model.MAX_BOOKMARK_LABEL
import com.rds.questlog.domain.model.MAX_BOOKMARK_SNIPPET
import com.rds.questlog.domain.model.PremiumFeature
import com.rds.questlog.domain.model.PurchaseQuery
import com.rds.questlog.domain.repository.BookmarkRepository
import com.rds.questlog.domain.repository.NoteRepository
import com.rds.questlog.domain.usecase.bookmark.AddBookmarkUseCase
import com.rds.questlog.domain.usecase.bookmark.DeleteBookmarkUseCase
import com.rds.questlog.domain.usecase.bookmark.ObserveGameBookmarksUseCase
import com.rds.questlog.domain.usecase.bookmark.UpdateBookmarkLabelUseCase
import com.rds.questlog.domain.usecase.note.DeleteNoteUseCase
import com.rds.questlog.domain.usecase.note.ObserveNoteUseCase
import com.rds.questlog.domain.usecase.note.SaveNoteUseCase
import com.rds.questlog.domain.usecase.premium.PremiumGuard
import com.rds.questlog.domain.usecase.premium.SyncPremiumStatusUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeBookmarkRepository : BookmarkRepository {
    val items = MutableStateFlow<List<Bookmark>>(emptyList())
    private var nextId = 1L

    override fun observeByGame(gameId: Long): Flow<List<Bookmark>> = items
    override suspend fun add(articleId: Long, nodeId: Long, label: String, snippet: String): Long {
        val id = nextId++
        items.value = items.value + Bookmark(id, articleId, "Artikel $articleId", nodeId, label, snippet, id)
        return id
    }

    override suspend fun updateLabel(id: Long, label: String) {
        items.value = items.value.map { if (it.id == id) it.copy(label = label) else it }
    }

    override suspend fun delete(id: Long) {
        items.value = items.value.filterNot { it.id == id }
    }
}

private class FakeNoteRepository : NoteRepository {
    val notes = MutableStateFlow<Map<Long, String>>(emptyMap())
    override fun observe(articleId: Long): Flow<String> = notes.map { it[articleId].orEmpty() }
    override suspend fun save(articleId: Long, content: String) {
        notes.value = notes.value + (articleId to content)
    }

    override suspend fun delete(articleId: Long) {
        notes.value = notes.value - articleId
    }
}

/** QL-23/QL-24: CRUD bookmark dan catatan lewat PremiumGuard pada tiga keadaan (gratis, premium, premium hilang). */
class BookmarkNoteUseCasesTest {

    private val config = FakeAppConfigRepository(premium = false)
    private val guard = PremiumGuard(config)
    private val bookmarks = FakeBookmarkRepository()
    private val notes = FakeNoteRepository()

    private val add = AddBookmarkUseCase(guard, bookmarks)
    private val rename = UpdateBookmarkLabelUseCase(guard, bookmarks)
    private val delete = DeleteBookmarkUseCase(guard, bookmarks)
    private val saveNote = SaveNoteUseCase(guard, notes)
    private val deleteNote = DeleteNoteUseCase(guard, notes)

    private fun sync(query: PurchaseQuery) = runBlocking {
        SyncPremiumStatusUseCase(
            FakeBillingService(query),
            config,
        )()
    }

    private fun list() = runBlocking { ObserveGameBookmarksUseCase(bookmarks)(1).first() }

    private fun note(articleId: Long = 1) = runBlocking { ObserveNoteUseCase(notes)(articleId).first() }

    private fun goPremium() = sync(PurchaseQuery.Owned("tok"))

    private fun losePremium() {
        goPremium()
        sync(PurchaseQuery.NotOwned)
    }

    @Test
    fun `premium menambah banyak bookmark per artikel dengan label dipangkas dan cuplikan dibatasi`() = runBlocking {
        goPremium()

        add(1, 10, "  Boss Dharma  ", "x".repeat(MAX_BOOKMARK_SNIPPET + 50)).getOrThrow()
        add(1, 10, "Elemen sama, bookmark kedua", "").getOrThrow()
        add(2, 20, "Peti", "teks").getOrThrow()

        val saved = list()
        assertEquals(listOf("Boss Dharma", "Elemen sama, bookmark kedua", "Peti"), saved.map { it.label })
        assertEquals(MAX_BOOKMARK_SNIPPET, saved.first().snippet.length)
        assertEquals(listOf(10L, 10L, 20L), saved.map { it.nodeId })
    }

    @Test
    fun `label kosong atau lebih dari 60 karakter ditolak tanpa mengubah data`() = runBlocking {
        goPremium()
        val id = add(1, 10, "Asli", "").getOrThrow()

        assertEquals(ValidationError.EmptyBookmarkLabel, add(1, 10, "   ", "").exceptionOrNull())
        assertEquals(
            ValidationError.BookmarkLabelTooLong,
            add(1, 10, "x".repeat(MAX_BOOKMARK_LABEL + 1), "").exceptionOrNull(),
        )
        assertEquals(ValidationError.EmptyBookmarkLabel, rename(id, "").exceptionOrNull())

        assertEquals(listOf("Asli"), list().map { it.label })
        assertTrue(add(1, 10, "x".repeat(MAX_BOOKMARK_LABEL), "").isSuccess)
    }

    @Test
    fun `ubah label dan hapus hanya memengaruhi bookmark itu`() = runBlocking {
        goPremium()
        val a = add(1, 10, "A", "").getOrThrow()
        add(1, 11, "B", "").getOrThrow()

        rename(a, "A baru").getOrThrow()
        assertEquals(listOf("A baru", "B"), list().map { it.label })

        delete(a).getOrThrow()
        assertEquals(listOf("B"), list().map { it.label })
    }

    @Test
    fun `pengguna gratis ditolak Locked dan data tidak berubah`() = runBlocking {
        assertEquals(PremiumError.Locked(PremiumFeature.BOOKMARK), add(1, 10, "A", "").exceptionOrNull())
        assertEquals(PremiumError.Locked(PremiumFeature.BOOKMARK), rename(1, "B").exceptionOrNull())
        assertEquals(PremiumError.Locked(PremiumFeature.BOOKMARK), delete(1).exceptionOrNull())
        assertEquals(PremiumError.Locked(PremiumFeature.NOTE), saveNote(1, "isi").exceptionOrNull())
        assertEquals(PremiumError.Locked(PremiumFeature.NOTE), deleteNote(1).exceptionOrNull())
        assertTrue(list().isEmpty())
        assertEquals("", note())
    }

    @Test
    fun `premium hilang tetap membaca tetapi menulis ditolak ReadOnly, lalu premium kembali tanpa kehilangan isi`() =
        runBlocking {
            goPremium()
            val id = add(1, 10, "Boss", "cuplikan").getOrThrow()
            saveNote(1, "Party: Kiefer").getOrThrow()

            losePremium()

            assertEquals(PremiumError.ReadOnly(PremiumFeature.BOOKMARK), add(1, 11, "Baru", "").exceptionOrNull())
            assertEquals(PremiumError.ReadOnly(PremiumFeature.BOOKMARK), rename(id, "Ubah").exceptionOrNull())
            assertEquals(PremiumError.ReadOnly(PremiumFeature.BOOKMARK), delete(id).exceptionOrNull())
            assertEquals(PremiumError.ReadOnly(PremiumFeature.NOTE), saveNote(1, "Ganti").exceptionOrNull())
            assertEquals(PremiumError.ReadOnly(PremiumFeature.NOTE), deleteNote(1).exceptionOrNull())
            assertEquals(listOf("Boss"), list().map { it.label })
            assertEquals("Party: Kiefer", note())

            goPremium()

            rename(id, "Boss Dharma").getOrThrow()
            assertEquals(listOf("Boss Dharma"), list().map { it.label })
            assertEquals("Party: Kiefer", note())
        }

    @Test
    fun `catatan satu per artikel, autosave menimpa, kosong menghapus`() = runBlocking {
        goPremium()

        saveNote(1, "satu").getOrThrow()
        saveNote(1, "satu dua").getOrThrow()
        saveNote(2, "artikel lain").getOrThrow()
        assertEquals("satu dua", note(1))
        assertEquals("artikel lain", note(2))

        saveNote(1, "").getOrThrow()
        assertEquals("", note(1))
        assertEquals(setOf(2L), notes.notes.value.keys)

        deleteNote(2).getOrThrow()
        assertTrue(notes.notes.value.isEmpty())
    }

    @Test
    fun `catatan panjang disimpan utuh`() = runBlocking {
        goPremium()
        val long = "baris catatan\n".repeat(20_000)

        saveNote(1, long).getOrThrow()

        assertEquals(long, note())
    }
}
