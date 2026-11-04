package com.idlix.tv.data.repository

import com.idlix.tv.data.model.MovieItem
import java.util.Locale

private fun normalizedLabel(text: String): String =
    text.lowercase(Locale.ROOT).replace("+", "plus").replace(Regex("[^a-z0-9]"), "")

/** Match only proven catalog metadata; never substitute a title search for a country/year filter. */
internal fun matchesCatalogFilter(item: MovieItem, section: String, value: String): Boolean {
    if (value.isBlank() && section != "shorts") return true
    val target = normalizedLabel(value)
    return when (section) {
        "genre" -> item.genres.isEmpty() || item.genres.any { genre ->
            normalizedLabel(genre.slug) == target || normalizedLabel(genre.name) == target
        }
        "year" -> {
            item.releaseDate?.trim()?.take(4) == value ||
                item.slug.endsWith("-$value", ignoreCase = true)
        }
        "country" -> {
            val expected = Locale("", value.uppercase(Locale.ROOT))
                .getDisplayCountry(Locale.ENGLISH)
            val aliases = mapOf(
                "KR" to listOf("South Korea", "Korea Selatan", "Republic of Korea", "Korea"),
                "US" to listOf("United States", "USA", "America", "Amerika Serikat"),
                "GB" to listOf("United Kingdom", "UK", "Britain", "England"),
                "CN" to listOf("China", "People's Republic of China"),
                "TW" to listOf("Taiwan"),
                "ID" to listOf("Indonesia")
            )
            val fields = item.country.orEmpty()
                .split(Regex("[,;/|]"))
                .map(::normalizedLabel)
            val acceptable = (listOf(value, expected) + aliases[value.uppercase(Locale.ROOT)].orEmpty())
                .map(::normalizedLabel)
            fields.any { field -> field.isNotEmpty() && field in acceptable }
        }
        "network" -> {
            val networkText = normalizedLabel(item.networkMetadata)
            networkText.isNotBlank() &&
                SiteCatalogReferences.networkAliases(value).any {
                    networkText.contains(normalizedLabel(it))
                }
        }
        "shorts" -> item.runtime?.let { it in 1..50 } == true
        "collections" -> true
        else -> true
    }
}
