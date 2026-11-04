package com.idlix.tv.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Explore : Screen("explore")
    object Movies : Screen("movies")
    object Series : Screen("series")
    object Shorts : Screen("shorts")
    object Collections : Screen("collections")
    object Browse : Screen("browse/{section}?value={value}&type={type}") {
        fun createRoute(section: String, value: String = "", type: String = "all"): String {
            fun enc(s: String) = Uri.encode(s)
            return "browse/${enc(section)}?value=${enc(value)}&type=${enc(type)}"
        }
    }
    object ActorSearch : Screen("actor/{name}") {
        fun createRoute(name: String): String =
            "actor/" + Uri.encode(name)
    }
    object Search : Screen("search")
    object ContinueWatching : Screen("continue_watching")
    object Settings : Screen("settings")

    object Detail : Screen("detail/{contentType}/{slug}") {
        fun createRoute(contentType: String, slug: String): String =
            "detail/$contentType/$slug"
    }

    object Player : Screen("player/{contentType}/{contentId}/{title}?poster={poster}&backdrop={backdrop}&slug={slug}&season={season}&episode={episode}") {
        fun createRoute(
            contentType: String,
            contentId: String,
            title: String,
            poster: String? = null,
            backdrop: String? = null,
            slug: String? = null,
            season: Int? = null,
            episode: Int? = null
        ): String {
            val encodedTitle = Uri.encode(title)
            val encodedPoster = Uri.encode(poster.orEmpty())
            val encodedBackdrop = Uri.encode(backdrop.orEmpty())
            val encodedSlug = Uri.encode(slug.orEmpty())
            val seasonParam = season?.toString() ?: ""
            val episodeParam = episode?.toString() ?: ""
            return "player/${Uri.encode(contentType)}/${Uri.encode(contentId)}/$encodedTitle?poster=$encodedPoster&backdrop=$encodedBackdrop&slug=$encodedSlug&season=$seasonParam&episode=$episodeParam"
        }
    }
}
