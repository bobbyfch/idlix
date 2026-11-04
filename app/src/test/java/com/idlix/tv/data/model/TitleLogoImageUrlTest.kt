package com.idlix.tv.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TitleLogoImageUrlTest {
    @Test fun relativeTmdbLogoIsScaled() {
        assertEquals("https://image.tmdb.org/t/p/w500/sample.png", titleLogoImageUrl("/sample.png"))
        assertEquals("https://image.tmdb.org/t/p/w500/logo.png", titleLogoImageUrl("/logo.svg"))
    }
    @Test fun absoluteLogoIsPreserved() {
        assertEquals("https://cdn.example.org/logo.png", titleLogoImageUrl("https://cdn.example.org/logo.png"))
    }
    @Test fun invalidLogoUsesTypographyFallback() {
        assertNull(titleLogoImageUrl(null))
        assertNull(titleLogoImageUrl(""))
        assertNull(titleLogoImageUrl("null"))
        assertNull(titleLogoImageUrl("http://example.com/logo.png"))
    }
    @Test fun movieAndSeriesExposeTitleArtwork() {
        val movie = MovieDetail(title = "Example", logoPath = "/original.png")
        val series = SeriesDetail(title = "Series", logoPath = "/series.png")
        assertEquals("https://image.tmdb.org/t/p/w500/original.png", movie.titleLogoUrl)
        assertEquals("https://image.tmdb.org/t/p/w500/series.png", series.titleLogoUrl)
    }
}
