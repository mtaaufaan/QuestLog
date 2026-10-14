package com.rds.questlog.presentation.articlelist

import com.rds.questlog.presentation.model.ArticleUiModel

/** Satu kelompok di Halaman Game; [title] null = tanpa header (daftar polos). */
data class ArticleSection(val title: String?, val articles: List<ArticleUiModel>)

/**
 * Daftar S1 per kategori Binder: hanya bila [enabled] (premium, tanpa pencarian) dan ada artikel berkategori. Urutan
 * kategori mengikuti kemunculan pertama; artikel yang masuk beberapa kategori tampil di tiap kategorinya. Artikel
 * tanpa kategori masuk kelompok terakhir berjudul [uncategorized].
 */
fun groupArticles(articles: List<ArticleUiModel>, enabled: Boolean, uncategorized: String): List<ArticleSection> {
    if (!enabled || articles.all { it.categories.isEmpty() }) return listOf(ArticleSection(null, articles))
    val names = articles.flatMap { it.categories }.distinct()
    val sections = names.map { name -> ArticleSection(name, articles.filter { name in it.categories }) }
    val rest = articles.filter { it.categories.isEmpty() }
    return if (rest.isEmpty()) sections else sections + ArticleSection(uncategorized, rest)
}
