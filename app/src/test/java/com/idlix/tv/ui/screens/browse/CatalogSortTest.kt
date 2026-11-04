package com.idlix.tv.ui.screens.browse

import com.idlix.tv.data.model.MovieItem
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogSortTest {
    private val sample = listOf(
        MovieItem(id = "a", title = "Zebra", viewCount = 90, voteAverage = "7.0",
            releaseDate = "2023-01-05", contentType = "movie"),
        MovieItem(id = "b", title = "Alpha", viewCount = 20, voteAverage = "9.1",
            releaseDate = "2026-03-10", contentType = "tv_series"),
        MovieItem(id = "c", title = "Bravo", viewCount = 50, voteAverage = "8.2",
            releaseDate = "2020-06-12", contentType = "movie")
    )

    @Test fun newestSort() {
        assertEquals(listOf("b", "a", "c"), sortedItems(sample, "newest", "all").map { it.id })
    }

    @Test fun oldestSort() {
        assertEquals(listOf("c", "a", "b"), sortedItems(sample, "oldest", "all").map { it.id })
    }

    @Test fun popularitySort() {
        assertEquals(listOf("a", "c", "b"), sortedItems(sample, "popular", "all").map { it.id })
    }

    @Test fun ratingSort() {
        assertEquals(listOf("b", "c", "a"), sortedItems(sample, "rating", "all").map { it.id })
    }

    @Test fun alphabeticalSort() {
        assertEquals(listOf("b", "c", "a"), sortedItems(sample, "az", "all").map { it.id })
    }

    @Test fun collectionSourceOrderRemainsUnchanged() {
        assertEquals(listOf("a", "b", "c"),
            sortedItems(sample, "source", "all").map { it.id })
    }

    @Test fun filterByType() {
        assertEquals(listOf("a", "c"), sortedItems(sample, "newest", "movie").map { it.id })
        assertEquals(listOf("b"), sortedItems(sample, "newest", "series").map { it.id })
    }
}
