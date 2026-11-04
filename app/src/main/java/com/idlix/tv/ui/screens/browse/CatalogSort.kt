package com.idlix.tv.ui.screens.browse

import com.idlix.tv.data.model.MovieItem

/** Deterministic, device-local sorting and type filtering of the loaded catalog. */
internal fun sortedItems(
    items: List<MovieItem>,
    sort: String,
    type: String
): List<MovieItem> {
    val visible = items.filter {
        when (type) {
            "movie" -> it.contentType.equals("movie", true)
            "series" -> it.contentType.equals("tv_series", true) ||
                it.contentType.equals("series", true)
            else -> true
        }
    }
    return when (sort) {
        "source" -> visible
        "popular" -> visible.sortedWith(
            compareByDescending<MovieItem> { it.viewCount }.thenBy { it.title.lowercase() })
        "rating" -> visible.sortedWith(
            compareByDescending<MovieItem> { it.voteAverage?.toDoubleOrNull() ?: 0.0 }
                .thenBy { it.title.lowercase() })
        "az" -> visible.sortedBy { it.title.lowercase() }
        "oldest" -> visible.sortedWith(
            compareBy<MovieItem> { it.releaseDate.isNullOrBlank() }
                .thenBy { it.releaseDate.orEmpty() }.thenBy { it.title.lowercase() })
        else -> visible.sortedWith(
            compareByDescending<MovieItem> { it.releaseDate.orEmpty() }
                .thenBy { it.title.lowercase() })
    }
}

