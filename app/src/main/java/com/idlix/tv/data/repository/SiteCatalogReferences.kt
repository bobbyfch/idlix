package com.idlix.tv.data.repository

import java.util.Locale

/**
 * Fallback index of verified collection landing pages (3 November 2026).
 * The homepage editorial rail is unrelated to the curated /collection page.
 * Do not infer collection membership from fuzzy title searches.
 */
object SiteCatalogReferences {
    // Fallback for the true /collection index (not the homepage title rail).
    val collections = listOf(
        SiteCollection("Marvel Cinematic Universe", "marvel-cinematic-universe", 56),
        SiteCollection("Toy Story", "toy-story", 5),
        SiteCollection("Wizarding World", "wizarding-world", 11),
        SiteCollection("Insidious", "insidious", 6),
        SiteCollection("Despicable Me & Minions", "despicable-me-and-minions", 7),
        SiteCollection("The Conjuring Universe", "conjuring-universe", 10),
        SiteCollection("Demon Slayer: Kimetsu no Yaiba", "demon-slayer-kimetsu-no-yaiba", 3),
        SiteCollection("365 Days", "365-days", 3),
        SiteCollection("Star Wars", "star-wars", 28),
        SiteCollection("Ready or Not", "ready-or-not", 2),
        SiteCollection("Fast & Furious", "fast-and-furious", 12),
        SiteCollection("Mortal Kombat", "mortal-kombat", 8)
    )
    val collectionTitles: List<String> get() = collections.map { it.title }

    val networkTitleSeeds: Map<String, List<String>> = mapOf(
        "netflix" to listOf(
            "The Night Agent", "Black Mirror", "The Witcher",
            "D.P.", "Gyeongseong Creature", "Formula 1: Drive to Survive"
        ),
        "hbo" to listOf(
            "House of the Dragon", "The Last of Us", "Succession",
            "The White Lotus", "Game of Thrones"
        ),
        "prime-video" to listOf(
            "The Boys", "Fallout", "Reacher", "Invincible", "The Rings of Power"
        ),
        "disney-plus" to listOf(
            "The Mandalorian", "Loki", "Andor", "Ahsoka", "WandaVision"
        ),
        "apple-tv-plus" to listOf(
            "Severance", "Silo", "Ted Lasso", "Foundation", "Slow Horses"
        )
    )

    fun networkAliases(provider: String): List<String> = when (provider.lowercase(Locale.ROOT)) {
        "netflix" -> listOf("Netflix")
        "hbo" -> listOf("HBO", "HBO Max", "Max")
        "prime-video" -> listOf("Amazon Prime Video", "Prime Video", "Amazon Video")
        "disney-plus" -> listOf("Disney+", "Disney Plus", "Disney")
        "apple-tv-plus" -> listOf("Apple TV+", "Apple TV Plus", "Apple")
        else -> listOf(provider)
    }

    fun normalizedTitle(title: String): String = title.lowercase(Locale.ROOT)
        .replace("&", "and")
        .replace(Regex("[^a-z0-9]"), "")

    fun isExactTitle(candidate: String, target: String): Boolean =
        normalizedTitle(candidate) == normalizedTitle(target)
}
