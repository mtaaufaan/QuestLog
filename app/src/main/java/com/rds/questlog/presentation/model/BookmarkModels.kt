package com.rds.questlog.presentation.model

/** Keadaan premium untuk UI Reader (component-contract.md §4: `'free' | 'premium' | 'lost'`). */
enum class PremiumUiState { FREE, PREMIUM, LOST }

/** Satu bookmark di Bookmark Panel; [nodeId] = anchor elemen konten, [snippet] = cuplikan teksnya. */
data class BookmarkUi(
    val id: Long,
    val articleId: Long,
    val articleTitle: String,
    val label: String,
    val snippet: String,
    val nodeId: Long = 0,
)

/** Mode Bookmark Label Dialog: tambah bookmark baru atau ubah label yang ada. */
enum class BookmarkLabelMode { ADD, EDIT }

/** Aksi di Reader Tools Sheet (component-contract.md §16a). */
enum class ReaderToolAction { PROGRESS, NOTE, DISPLAY }
