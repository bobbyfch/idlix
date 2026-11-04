package com.idlix.tv.data.repository

import com.idlix.tv.data.model.SearchResultItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.time.Year
import org.junit.Test

class CinemetaCollectionSourceTest {
    @Test fun parsesOnlyValidYearTypeAndImdbItems() {
        val json = """{"metas":[
          {"id":"tt0133093","name":"The Matrix","releaseInfo":"1999","type":"movie","poster":"https://images.example/poster.jpg"},
          {"id":"tt0133093","name":"Duplicate Matrix","releaseInfo":"1999","type":"movie"},
          {"id":"tt0000001","name":"Wrong type","releaseInfo":"2000","type":"series"},
          {"id":"not-an-imdb-id","name":"Invalid","releaseInfo":"2010","type":"movie"},
          {"id":"tt0000002","name":"No year","type":"movie"}
        ]}"""
        val items = CinemetaCollectionSource.parseCatalog(json, "movie")
        assertEquals(1, items.size)
        assertEquals("tt0133093", items.first().imdbId)
        assertEquals("1999", items.first().year)
    }

    @Test fun onlyExactTypeTitleAndYearMayOpenIdlix() {
        val movie = CinemetaCollectionSource.ExternalTitle("tt0133093", "The Matrix", "1999", "movie")
        val exact = SearchResultItem(id = "real-id", slug = "the-matrix-1999",
            title = "The Matrix (1999)", contentType = "movie", releaseDate = "1999-03-31")
        assertTrue(CinemetaCollectionSource.matchesIdlixTitle(movie, exact))
        assertFalse(CinemetaCollectionSource.matchesIdlixTitle(movie, exact.copy(releaseDate = "2021-01-01")))
        assertFalse(CinemetaCollectionSource.matchesIdlixTitle(movie, exact.copy(title = "The Matrix Reloaded")))
        assertFalse(CinemetaCollectionSource.matchesIdlixTitle(movie, exact.copy(contentType = "tv_series")))
        assertFalse(CinemetaCollectionSource.matchesIdlixTitle(movie, exact.copy(slug = "")))
        assertFalse(CinemetaCollectionSource.matchesIdlixTitle(movie, exact.copy(releaseDate = null)))
    }

    @Test fun catalogPaginationUsesSkipOffsetAndKeepsExternalNavigationWhitelisted() {
        assertEquals(
            "https://v3-cinemeta.strem.io/catalog/movie/top/skip=100.json",
            CinemetaCollectionSource.catalogUrlFor("cinemeta-film-populer", 100)
        )
        assertEquals(
            "https://v3-cinemeta.strem.io/catalog/movie/top/genre=Fantasy&skip=100.json",
            CinemetaCollectionSource.catalogUrlFor("cinemeta-film-fantasy", 100)
        )
        assertEquals(null, CinemetaCollectionSource.catalogUrlFor("https://untrusted.example", 100))
    }

    @Test fun yearAndGenreCatalogPathsFollowTheOfficialCinemetaManifest() {
        val current = Year.now().value
        assertEquals(
            "https://v3-cinemeta.strem.io/catalog/movie/year/genre=$current.json",
            CinemetaCollectionSource.catalogUrlFor("cinemeta-film-tahun-ini")
        )
        assertEquals(
            "https://v3-cinemeta.strem.io/catalog/series/year/genre=$current.json",
            CinemetaCollectionSource.catalogUrlFor("cinemeta-serial-tahun-ini")
        )
        assertEquals(
            "https://v3-cinemeta.strem.io/catalog/movie/top/genre=Fantasy.json",
            CinemetaCollectionSource.catalogUrlFor("cinemeta-film-fantasy")
        )
        assertEquals(null, CinemetaCollectionSource.catalogUrlFor("https://untrusted.example"))
    }

    @Test fun supportsSeriesAndWhitelistedCollectionsOnly() {
        val series = CinemetaCollectionSource.ExternalTitle("tt0944947", "Game of Thrones", "2011", "series")
        val idlix = SearchResultItem(id = "id", slug = "game-of-thrones-2011",
            title = "Game of Thrones", contentType = "tv_series", firstAirDate = "2011-04-17")
        assertTrue(CinemetaCollectionSource.matchesIdlixTitle(series, idlix))
        assertFalse(CinemetaCollectionSource.isExternalCollection("unknown-url"))
        assertTrue(CinemetaCollectionSource.isExternalCollection("cinemeta-film-crime"))
        assertTrue(CinemetaCollectionSource.collections.size >= 19)
    }
}
