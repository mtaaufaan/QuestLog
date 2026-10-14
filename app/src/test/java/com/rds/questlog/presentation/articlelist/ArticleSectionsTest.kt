package com.rds.questlog.presentation.articlelist

import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.ArticleUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleSectionsTest {

    private fun article(id: Long, category: String? = null) = ArticleUiModel(
        id = id,
        gameId = 1,
        gameName = "G",
        title = "A$id",
        status = ArticleStatus.READY,
        pages = emptyList(),
        categoryName = category,
    )

    private val articles = listOf(article(1, "Utama"), article(2), article(3, "Koleksi"), article(4, "Utama"))

    @Test
    fun `tidak dikelompokkan bila fitur mati meski ada kategori`() {
        val result = groupArticles(articles, enabled = false, uncategorized = "Tanpa kategori")

        assertEquals(listOf(ArticleSection(null, articles)), result)
    }

    @Test
    fun `tidak dikelompokkan bila tidak ada artikel berkategori`() {
        val plain = listOf(article(1), article(2))

        assertEquals(listOf(ArticleSection(null, plain)), groupArticles(plain, enabled = true, uncategorized = "X"))
    }

    @Test
    fun `kelompok mengikuti kemunculan pertama dan tanpa kategori selalu terakhir`() {
        val result = groupArticles(articles, enabled = true, uncategorized = "Tanpa kategori")

        assertEquals(listOf("Utama", "Koleksi", "Tanpa kategori"), result.map { it.title })
        assertEquals(listOf(listOf(1L, 4L), listOf(3L), listOf(2L)), result.map { s -> s.articles.map { it.id } })
    }

    @Test
    fun `semua berkategori tidak menambah kelompok tanpa kategori`() {
        val all = listOf(article(1, "A"), article(2, "B"))

        assertEquals(listOf("A", "B"), groupArticles(all, enabled = true, uncategorized = "Tanpa").map { it.title })
    }
}
