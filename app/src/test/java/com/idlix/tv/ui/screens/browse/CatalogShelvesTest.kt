package com.idlix.tv.ui.screens.browse

import com.idlix.tv.data.model.MovieItem
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogShelvesTest {
    private fun item(id: String, type: String = "movie") =
        MovieItem(id = id, title = "Title $id", contentType = type)

    @Test fun avoidsRepeatingSameFilmInLaterRails() {
        val shelves = distinctCatalogShelves(
            listOf(
                listOf(item("1"), item("2")),
                listOf(item("2"), item("3"), item("1")),
                listOf(item("1"), item("3"), item("4"))
            )
        )
        assertEquals(listOf("1", "2"), shelves[0].map { it.id })
        assertEquals(listOf("3"), shelves[1].map { it.id })
        assertEquals(listOf("4"), shelves[2].map { it.id })
    }

    @Test fun treatsMovieAndSeriesIdsAsDifferent() {
        val shelves = distinctCatalogShelves(
            listOf(listOf(item("42")), listOf(item("42", "tv_series")))
        )
        assertEquals(1, shelves[0].size)
        assertEquals(1, shelves[1].size)
    }

    @Test fun emptyShelvesRemainEmpty() {
        assertEquals(listOf(emptyList<MovieItem>()), distinctCatalogShelves(listOf(emptyList())))
    }
}
