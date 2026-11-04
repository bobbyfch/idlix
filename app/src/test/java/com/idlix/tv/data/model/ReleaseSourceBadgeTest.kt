package com.idlix.tv.data.model

import com.idlix.tv.data.api.ApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseSourceBadgeTest {
    @Test fun canonicalizesExplicitSourceMetadata() {
        assertEquals("WEB-DL", releaseSourceBadge("WEBDL"))
        assertEquals("WEB-DL", releaseSourceBadge("WEB.DL 1080p"))
        assertEquals("BluRay", releaseSourceBadge("Blu-ray"))
        assertEquals("BluRay", releaseSourceBadge("BDRip"))
        assertEquals("WEBRip", releaseSourceBadge("WEB-RIP"))
        assertEquals("CAM", releaseSourceBadge("HDCAM"))
    }

    @Test fun doesNotInventSourcesFromResolution() {
        assertNull(releaseSourceBadge(null, "Movie 2026", "movie-2026"))
        assertNull(releaseSourceBadge("unknown", "TV Series", "tv-series"))
        assertEquals("1080P", releaseSourceBadge("1080p"))
    }

    @Test fun findsTechnicalSourceTagsInReleaseNames() {
        assertEquals("WEB-DL", releaseSourceBadge(null, "Example.Movie.2026.WEB-DL.1080p"))
        assertEquals("BluRay", releaseSourceBadge(null, "Example Movie", "example-movie-bluray"))
    }

    @Test fun seriesToMovieItemRetainsQuality() {
        val series = SeriesItem(title = "Example", quality = "BLURAY", slug = "example")
        assertEquals("BluRay", series.toMovieItem().sourceBadge)
    }

    @Test fun decodesSourceForSeriesAndMovies() {
        val series = ApiClient.json.decodeFromString<SeriesListResponse>(
            """{"data":[{"id":"1","title":"Episode","slug":"episode","quality":"WEB-DL"}]}"""
        )
        assertEquals("WEB-DL", series.data.single().toMovieItem().sourceBadge)
        val films = ApiClient.json.decodeFromString<MovieListResponse>(
            """{"data":[{"id":"2","title":"Film","slug":"film","quality":"BluRay"}]}"""
        )
        assertEquals("BluRay", films.data.single().sourceBadge)
    }

    @Test fun searchItemsKeepExplicitSource() {
        val search = ApiClient.json.decodeFromString<SearchResponse>(
            """{"results":[{"id":"3","title":"Example","contentType":"tv_series","quality":"WEBDL"}]}"""
        )
        assertEquals("WEB-DL", search.results.single().sourceBadge)
    }
}
