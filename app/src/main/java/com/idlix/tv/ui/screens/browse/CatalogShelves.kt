package com.idlix.tv.ui.screens.browse

import com.idlix.tv.data.model.MovieItem

/**
 * Keep the same movie from appearing in every shelf just because multiple API
 * categories share it. Genre discovery is unaffected; only editorial rails
 * are de-duplicated, in their display order.
 */
data class CatalogShelf(
    val title: String,
    val items: List<MovieItem>
)

/** Duplicate-free list for a single, semantically labelled editorial row. */
internal fun shelfItems(items: List<MovieItem>): List<MovieItem> =
    items.filter { it.id.isNotBlank() && it.slug.isNotBlank() }
        .distinctBy { "${it.contentType.lowercase()}:${it.id}" }

internal fun distinctCatalogShelves(shelves: List<List<MovieItem>>): List<List<MovieItem>> {
    val displayed = HashSet<String>()
    return shelves.map { shelf ->
        shelf.filter { item ->
            val identity = item.id.ifBlank { item.slug.ifBlank { item.title } }
            identity.isNotBlank() && displayed.add("${item.contentType.lowercase()}:${identity.lowercase()}")
        }
    }
}
