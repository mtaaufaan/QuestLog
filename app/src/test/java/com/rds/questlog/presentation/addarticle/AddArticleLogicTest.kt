package com.rds.questlog.presentation.addarticle

import com.rds.questlog.presentation.model.ScrapeMode
import com.rds.questlog.presentation.preview.PreviewData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddArticleLogicTest {
    private val games = PreviewData.games
    private val articles = PreviewData.articles

    @Test
    fun normalizeGameName_capitalizesAndTrims() {
        assertEquals("Elden Ring", normalizeGameName("  elden   ring "))
    }

    @Test
    fun matchingGames_emptyReturnsAll_andFiltersByName() {
        assertEquals(games, matchingGames(games, ""))
        val first = games.first().name
        assertTrue(matchingGames(games, first.lowercase()).any { it.name == first })
    }

    @Test
    fun canAddGame_freeLimitTwo() {
        assertFalse(canAddGame(games, isPremium = false))
        assertTrue(canAddGame(games, isPremium = true))
        assertTrue(canAddGame(games.take(1), isPremium = false))
    }

    @Test
    fun paste_multiUrlSplitsIntoRows_capped() {
        val pasted = (1..12).joinToString("\n") { "https://a.com/$it" }
        val form = AddArticleForm().withUrlChanged(0, pasted)
        assertEquals(MAX_URLS, form.urls.size)
        assertEquals("https://a.com/1", form.urls.first())
    }

    @Test
    fun urlOps_addRemoveMove() {
        var form = AddArticleForm().withUrlChanged(0, "https://a.com").withUrlAdded().withUrlChanged(1, "https://b.com")
        form = form.withUrlMoved(1, -1)
        assertEquals(listOf("https://b.com", "https://a.com"), form.urls)
        assertEquals(form, form.withUrlMoved(0, -1))
        assertEquals(listOf("https://a.com"), form.withUrlRemoved(0).urls)
        assertEquals(listOf(""), form.withUrlRemoved(0).withUrlRemoved(0).urls)
    }

    @Test
    fun urlErrors_formatAndDuplicate() {
        val errors = urlErrors(listOf("https://a.com", "http://x.com", "https://a.com", ""), emptyList(), 0, false)
        assertNull(errors[0])
        assertEquals(UrlError.InvalidFormat, errors[1])
        assertEquals(UrlError.DuplicateOf(1), errors[2])
        assertNull(errors[3])
        assertEquals(UrlError.Empty, urlErrors(listOf("https://a.com", ""), emptyList(), 0, true)[1])
        assertEquals(UrlError.NoUrls, urlErrors(listOf(""), emptyList(), 0, true)[0])
    }

    @Test
    fun validate_newModeRequiresGameTitleAndUrl() {
        val errors = validate(AddArticleForm(), articles)
        assertEquals(GameError.EMPTY, errors.game)
        assertTrue(errors.title)
        assertFalse(errors.isValid)
        val ok = AddArticleForm()
            .withPickedGame(games.first().id, games.first().name)
            .withTitle("Judul")
            .withUrlChanged(0, "https://a.com/x")
        assertTrue(validate(ok, articles).isValid)
        assertEquals("Judul", buildPayload(ok, articles).title)
    }

    @Test
    fun scrapeMode_fromRoute() {
        assertEquals(ScrapeMode.APPEND, ScrapeMode.fromRoute("append"))
        assertEquals(ScrapeMode.NEW, ScrapeMode.fromRoute("apa-saja"))
    }
}
