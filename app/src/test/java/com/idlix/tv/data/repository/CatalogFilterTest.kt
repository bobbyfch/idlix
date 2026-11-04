package com.idlix.tv.data.repository

import com.idlix.tv.data.model.Genre
import com.idlix.tv.data.model.MovieItem
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogFilterTest {
    @Test fun yearMatchesMetadataOrYearInSlug() {
        val film = MovieItem(id = "1", releaseDate = "2019-08-17", slug = "some-film")
        assertTrue(matchesCatalogFilter(film, "year", "2019"))
        assertFalse(matchesCatalogFilter(film, "year", "2020"))
        assertTrue(matchesCatalogFilter(MovieItem(slug = "triple-frontier-2019"), "year", "2019"))
    }

    @Test fun yearDoesNotMatchMerelyBecauseTitleContainsNumber() {
        val film = MovieItem(title = "2019 Memories", releaseDate = "2022-01-01")
        assertFalse(matchesCatalogFilter(film, "year", "2019"))
    }

    @Test fun countryAcceptsActualCodesAndAliases() {
        assertTrue(matchesCatalogFilter(MovieItem(country = "KR"), "country", "KR"))
        assertTrue(matchesCatalogFilter(MovieItem(country = "South Korea"), "country", "KR"))
        assertTrue(matchesCatalogFilter(MovieItem(country = "US, JP"), "country", "JP"))
        assertFalse(matchesCatalogFilter(MovieItem(country = "South Africa"), "country", "KR"))
        assertFalse(matchesCatalogFilter(MovieItem(country = null), "country", "ID"))
    }

    @Test fun networkUsesMetadataNotTitleGuessing() {
        val show = MovieItem(title = "Netflix Forever", network = JsonPrimitive("Disney+"))
        assertFalse(matchesCatalogFilter(show, "network", "netflix"))
        assertTrue(matchesCatalogFilter(show, "network", "disney-plus"))
    }

    @Test fun networkProviderAliasesMatchKnownMetadata() {
        assertTrue(matchesCatalogFilter(
            MovieItem(network = JsonPrimitive("Amazon Prime Video")), "network", "prime-video"
        ))
        assertTrue(matchesCatalogFilter(
            MovieItem(network = JsonPrimitive("HBO Max")), "network", "hbo"
        ))
        assertTrue(matchesCatalogFilter(
            MovieItem(network = JsonPrimitive("Apple TV+")), "network", "apple-tv-plus"
        ))
        assertFalse(matchesCatalogFilter(MovieItem(), "network", "hbo"))
    }

    @Test fun genresAndShortsAreGatedByTheirMetadata() {
        val comedy = MovieItem(genres = listOf(Genre(name = "Comedy", slug = "comedy")))
        assertTrue(matchesCatalogFilter(comedy, "genre", "comedy"))
        assertFalse(matchesCatalogFilter(comedy, "genre", "horror"))
        assertTrue(matchesCatalogFilter(MovieItem(runtime = 12), "shorts", ""))
        assertFalse(matchesCatalogFilter(MovieItem(runtime = 72), "shorts", ""))
    }
}
