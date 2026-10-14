package com.rds.questlog.presentation.gamelist

import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameUiModel

/** Aksi pengguna di S0; satu objek agar [GameListContent] tidak membawa belasan callback terpisah. */
@Suppress("LongParameterList") // satu callback per aksi
class GameListActions(
    val onQuery: (String) -> Unit,
    val onGameTap: (GameUiModel) -> Unit,
    val onGameLongPress: (GameUiModel) -> Unit,
    val onResume: (GameUiModel) -> Unit,
    val onOpenBinder: (GameUiModel) -> Unit,
    val onResultTap: (ArticleUiModel) -> Unit,
    val onAdd: () -> Unit,
    val onOpenUnlock: () -> Unit,
)
