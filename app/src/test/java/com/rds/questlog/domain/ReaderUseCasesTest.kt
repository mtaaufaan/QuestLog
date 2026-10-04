package com.rds.questlog.domain

import com.rds.questlog.domain.error.QuestLogError
import com.rds.questlog.domain.model.ContentNode
import com.rds.questlog.domain.model.DisplayPreferences
import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.ReadMode
import com.rds.questlog.domain.model.resolveAnchor
import com.rds.questlog.domain.repository.CheckpointRepository
import com.rds.questlog.domain.repository.UserPreferencesRepository
import com.rds.questlog.domain.usecase.prefs.SetFontSizeUseCase
import com.rds.questlog.domain.usecase.reader.SaveLastPositionUseCase
import com.rds.questlog.domain.usecase.reader.SetCheckpointUseCase
import com.rds.questlog.domain.usecase.reader.SetReadModeUseCase
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeCheckpointRepository(private val failWith: Throwable? = null) : CheckpointRepository {
    val checkpoints = mutableListOf<Pair<Long, Long>>()
    val positions = mutableListOf<Pair<Long, Long>>()
    override suspend fun setCheckpoint(articleId: Long, nodeId: Long) {
        failWith?.let { throw it }
        checkpoints += articleId to nodeId
    }
    override suspend fun saveLastPosition(articleId: Long, nodeId: Long) {
        failWith?.let { throw it }
        positions += articleId to nodeId
    }
}

private class FakePrefsRepository : UserPreferencesRepository {
    var fontSize: Int? = null
    override fun observeDisplayPreferences(): Flow<DisplayPreferences> = flowOf(DisplayPreferences())
    override suspend fun setFontSize(sizeSp: Int) {
        fontSize = sizeSp
    }
    override suspend fun setDarkMode(enabled: Boolean) = Unit
}

class ReaderUseCasesTest {

    private fun node(id: Long, order: Int) = ContentNode(id, sourcePageId = 1, type = NodeType.P, displayOrder = order)

    private val nodes = listOf(node(10, 1), node(11, 2), node(30, 1001), node(31, 1002))

    @Test
    fun `anchor yang masih ada dipakai apa adanya`() {
        assertEquals(11L, nodes.resolveAnchor(11, fallbackOrder = 2))
    }

    @Test
    fun `anchor yang hilang jatuh ke node terdekat menurut display_order, lalu node terakhir`() {
        assertEquals(30L, nodes.resolveAnchor(999, fallbackOrder = 500))
        assertEquals(31L, nodes.resolveAnchor(999, fallbackOrder = 9_999))
        assertEquals(31L, nodes.resolveAnchor(999, fallbackOrder = null))
    }

    @Test
    fun `tanpa anchor atau tanpa konten hasilnya null dan tidak error`() {
        assertNull(nodes.resolveAnchor(null, 1))
        assertNull(emptyList<ContentNode>().resolveAnchor(5, 1))
    }

    @Test
    fun `checkpoint dan posisi terakhir ditulis ke jalur yang terpisah`() {
        val repo = FakeCheckpointRepository()

        assertTrue(runBlocking { SetCheckpointUseCase(repo)(1, 11) }.isSuccess)
        assertTrue(runBlocking { SaveLastPositionUseCase(repo)(1, 30) }.isSuccess)

        assertEquals(listOf(1L to 11L), repo.checkpoints)
        assertEquals(listOf(1L to 30L), repo.positions)
    }

    @Test
    fun `kegagalan tulis dibungkus WriteFailed dan tidak melempar`() {
        val repo = FakeCheckpointRepository(failWith = IOException("disk"))
        val failure = runBlocking { SetCheckpointUseCase(repo)(1, 11) }.exceptionOrNull()
        assertTrue(failure is QuestLogError.DatabaseError.WriteFailed)
    }

    @Test
    fun `mode baca disimpan per artikel`() {
        val articles = FakeArticleRepository()
        runBlocking { SetReadModeUseCase(articles)(7, ReadMode.PAGED) }
        assertEquals(ReadMode.PAGED, articles.readModes[7L])
    }

    @Test
    fun `ukuran font dibatasi 12-24 dan dibulatkan ke kelipatan 2`() {
        val prefs = FakePrefsRepository()
        val set = SetFontSizeUseCase(prefs)
        listOf(5 to 12, 13 to 12, 17 to 16, 24 to 24, 99 to 24).forEach { (input, expected) ->
            runBlocking { set(input) }
            assertEquals("input $input", expected, prefs.fontSize)
        }
    }
}
