package com.rds.questlog.presentation.articlelist

import androidx.annotation.StringRes
import com.rds.questlog.presentation.binder.BinderCategoryUi
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameUiModel
import com.rds.questlog.presentation.model.PremiumUiState

data class ArticleListUiState(
    val games: List<GameUiModel> = emptyList(),
    /** Artikel yang sudah disaring database menurut [query] dan [selectedGameFilter]. */
    val articles: List<ArticleUiModel> = emptyList(),
    /** Game yang sedang dibuka (Halaman Game). */
    val selectedGameFilter: Long? = null,
    val query: String = "",
    val premium: PremiumUiState = PremiumUiState.FREE,
    /** Kategori Binder game ini dan penetapan artikelnya (id artikel -> id kategori). */
    val categories: List<BinderCategoryUi> = emptyList(),
    val assignments: Map<Long, Set<Long>> = emptyMap(),
    val isLoading: Boolean = false,
    /** Pesan toast yang sedang tampil; dikosongkan ViewModel setelah ±2,6 detik. */
    @StringRes val snackbar: Int? = null,
    /** Argumen tunggal untuk pesan [snackbar] yang berformat (mis. nomor halaman). */
    val snackbarArg: Int? = null,
    /** Edit Article Sheet sedang menyimpan (spinner di tombol Simpan). */
    val isSavingEdit: Boolean = false,
    /** Penyimpanan Edit Article Sheet gagal: teks "Gagal menyimpan. Coba lagi." di sheet. */
    val editSaveError: Boolean = false,
) {
    val isPremium: Boolean get() = premium == PremiumUiState.PREMIUM
}

fun ArticleListUiState.articleById(id: Long): ArticleUiModel? = articles.firstOrNull { it.id == id }
