package com.idlix.tv.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteCollectionSourceTest {
    @Test fun readsTheCollectionIndexNotTheHomepageEditorialStrip() {
        val html = """
            <section><h2>Collections</h2>
              <a href="/movie/the-concubine"><h3>The Concubine</h3></a>
            </section>
            <main><h1>Koleksi</h1>
              <a href="/collection/marvel-cinematic-universe"><h3>Marvel Cinematic Universe</h3><p>56 judul 2008 - 2026</p></a>
              <a href="/collection/toy-story"><h3>Toy Story</h3><p>5 judul 1995 - 2026</p></a>
              <a href="/collection/toy-story">Toy Story 5 judul</a>
            </main>
        """.trimIndent()
        val collections = SiteCollectionSource.parseCollections(html)
        assertEquals(listOf("Marvel Cinematic Universe", "Toy Story"), collections.map { it.title })
        assertEquals(listOf(56, 5), collections.map { it.count })
        assertEquals(listOf("marvel-cinematic-universe", "toy-story"), collections.map { it.slug })
    }

    @Test fun detailKeepsMovieAndSeriesInSourceOrder() {
        val html = """
            <a href="/collection/toy-story">Koleksi</a>
            <section><h2>Isi koleksi</h2>
              <a href="/movie/iron-man-2008"><h3>Iron Man (2008)</h3></a>
              <a href="/series/wandavision-2021"><h3>WandaVision (2021)</h3></a>
            </section>
        """.trimIndent()
        val items = SiteCollectionSource.parseItems(html)
        assertEquals(listOf("iron-man-2008", "wandavision-2021"), items.map { it.slug })
        assertEquals(listOf("movie", "tv_series"), items.map { it.contentType })
        assertEquals(listOf("2008", "2021"), items.map { it.year })
        assertTrue(SiteCollectionSource.parseCollections("<h2>Trending Now</h2>").isEmpty())
    }
}
