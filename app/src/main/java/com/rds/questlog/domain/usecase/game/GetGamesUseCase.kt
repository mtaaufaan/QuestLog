package com.rds.questlog.domain.usecase.game

import com.rds.questlog.domain.model.Game
import com.rds.questlog.domain.repository.GameRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetGamesUseCase @Inject constructor(private val repository: GameRepository) {

    /** Stream semua game beserta jumlah artikelnya, untuk Game Filter Sheet dan badge "X/2 game". */
    operator fun invoke(): Flow<List<Game>> = repository.observeGames()
}
