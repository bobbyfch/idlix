package com.idlix.tv.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MovieListResponse(
    val data: List<MovieItem> = emptyList(),
    val pagination: Pagination? = null
)

@Serializable
data class MovieItem(
    val id: String = "",
    val title: String = "",
    val slug: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val voteAverage: String? = null,
    val viewCount: Long = 0,
    val quality: String? = null,
    val country: String? = null,
    val network: JsonElement? = null,
    val networks: JsonElement? = null,
    val collection: JsonElement? = null,
    val collections: JsonElement? = null,
    val runtime: Int? = null,
    val originalLanguage: String? = null,
    val contentType: String = "movie",
    val genres: List<Genre> = emptyList(),
    val hasVideo: Boolean = true
) {
    val fullPosterUrl: String
        get() = mediaImageUrl(posterPath, "w342")

    val fullBackdropUrl: String
        get() = mediaImageUrl(backdropPath, "w1280")

    val formattedRating: String
        get() = voteAverage?.toDoubleOrNull()?.takeIf { it > 0.0 && it <= 10.0 }
            ?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "N/A"

    val sourceBadge: String? get() = releaseSourceBadge(quality, title, slug)

    val networkMetadata: String
        get() = listOfNotNull(network, networks).joinToString(" ").lowercase()

    val collectionMetadata: String
        get() = listOfNotNull(collection, collections).joinToString(" ").lowercase()
}

@Serializable
data class MovieDetail(
    val id: String = "",
    val tmdbId: Long? = null,
    val imdbId: String? = null,
    val title: String = "",
    val slug: String = "",
    val originalTitle: String? = null,
    val overview: String? = null,
    val tagline: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val logoPath: String? = null,
    val logoUrl: String? = null,
    val releaseDate: String? = null,
    val duration: Int? = null,
    val runtime: Int? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val voteAverage: String? = null,
    val viewCount: Long = 0,
    val quality: String? = null,
    val country: String? = null,
    val genres: List<Genre> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    val trailerUrl: String? = null
) {
    val fullPosterUrl: String
        get() = mediaImageUrl(posterPath, "w342")

    val fullBackdropUrl: String
        get() = mediaImageUrl(backdropPath, "w1280")

    val sourceBadge: String? get() = releaseSourceBadge(quality, title, slug)
    val titleLogoUrl: String? get() = titleLogoImageUrl(logoPath ?: logoUrl)
}

@Serializable
data class Genre(
    val id: String = "",
    val name: String = "",
    val slug: String = ""
)

@Serializable
data class CastMember(
    val id: String = "",
    val name: String = "",
    val character: String? = null,
    val profilePath: String? = null
) {
    val fullProfileUrl: String
        get() = mediaImageUrl(profilePath, "w185")
}

@Serializable
data class Pagination(
    val page: Int = 1,
    val limit: Int = 20,
    val total: Long = 0,
    val totalPages: Int = 1
)

@Serializable
data class GenreListResponse(
    val data: List<Genre> = emptyList()
)
