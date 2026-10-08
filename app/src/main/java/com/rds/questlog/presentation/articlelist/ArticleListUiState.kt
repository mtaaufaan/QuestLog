package com.rds.questlog.presentation.articlelist

import androidx.annotation.StringRes
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameUiModel

data class ArticleListUiState(
    val games: List<GameUiModel> = emptyList(),
    /** Artikel yang sudah disaring database menurut [query] dan [selectedGameFilter]. */
    val articles: List<ArticleUiModel> = emptyList(),
    /** null = "Semua Game". */
    val selectedGameFilter: Long? = null,
    val query: String = "",
    val isPremium: Boolean = false,
    val isLoading: Boolean = false,
    /** Pesan toast yang sedang tampil; dikosongkan ViewModel setelah ±2,6 detik. */
    @StringRes val snackbar: Int? = null,
    /** Edit Article Sheet sedang menyimpan (spinner di tombol Simpan). */
    val isSavingEdit: Boolean = false,
    /** Penyimpanan Edit Article Sheet gagal: teks "Gagal menyimpan. Coba lagi." di sheet. */
    val editSaveError: Boolean = false,
)

fun ArticleListUiState.articleById(id: Long): ArticleUiModel? = articles.firstOrNull { it.id == id }
