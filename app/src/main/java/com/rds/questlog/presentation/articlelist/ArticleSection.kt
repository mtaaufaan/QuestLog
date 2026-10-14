package com.rds.questlog.presentation.articlelist

import com.rds.questlog.presentation.model.ArticleUiModel

/** Satu kelompok di Halaman Game; [title] null = tanpa header (daftar polos). */
data class ArticleSection(val title: String?, val articles: List<ArticleUiModel>)

/**
 * Daftar S1 per kategori Binder: hanya bila [enabled] (premium, tanpa pencarian) dan ada artikel berkategori. Urutan
 * kategori mengikuti kemunculan pertama, "Tanpa kategori" (title null pada [ArticleSection.title] untuk daftar polos
 * saja) selalu terakhir dan memakai [uncategorized] sebagai judul.
 */
fun groupArticles(articles: List<ArticleUiModel>, enabled: Boolean, uncategorized: String): List<ArticleSection> {
    if (!enabled || articles.none { it.categoryName != null }) return listOf(ArticleSection(null, articles))
    val byCategory = articles.filter { it.categoryName != null }.groupBy { it.categoryName }
    val sections = byCategory.map { (name, list) -> ArticleSection(name, list) }
    val rest = articles.filter { it.categoryName == null }
    return if (rest.isEmpty()) sections else sections + ArticleSection(uncategorized, rest)
}
