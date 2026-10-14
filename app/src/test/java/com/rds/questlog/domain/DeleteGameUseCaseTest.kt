package com.rds.questlog.domain

import com.rds.questlog.domain.error.QuestLogError
import com.rds.questlog.domain.usecase.game.DeleteGameUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteGameUseCaseTest {

    @Test
    fun `menghapus game lewat repository`() = runBlocking {
        val repository = FakeGameRepository()

        assertTrue(DeleteGameUseCase(repository)(4).isSuccess)

        assertEquals(listOf(4L), repository.deletedGames)
    }

    @Test
    fun `kegagalan dibungkus WriteFailed dan tidak dilempar`() = runBlocking {
        val result = DeleteGameUseCase(FakeGameRepository(failWith = IllegalStateException("disk")))(4)

        assertTrue(result.exceptionOrNull() is QuestLogError.DatabaseError.WriteFailed)
    }
}
