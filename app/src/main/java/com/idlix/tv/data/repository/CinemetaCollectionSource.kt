package com.idlix.tv.data.repository

import com.idlix.tv.data.model.SearchResultItem
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Cinemeta provides public *metadata*, not an IDLIX title or a playable link.
 * Only exact year/title/type matches from the IDLIX search API may open a detail.
 *
 * All catalog paths are explicitly whitelisted. No arbitrary remote URL from
 * the UI is fetched, and nothing in this object resolves video streams.
 */
internal object CinemetaCollectionSource {
    data class ExternalTitle(
        val imdbId: String,
        val title: String,
        val year: String,
        val type: String,
        val posterUrl: String? = null
    )

    private data class Catalog(
        val title: String,
        val slug: String,
        val contentType: String,
        val catalogId: String,
        val genre: String? = null
    )

    // The current-year lists follow the calendar without hard-coded annual maintenance.
    private val currentYear = java.time.Year.now().value.toString()
    private val catalogs = listOf(
        Catalog("Film Populer Dunia", "cinemeta-film-populer", "movie", "top"),
        Catalog("Film Rating Tertinggi", "cinemeta-film-terbaik", "movie", "imdbRating"),
        Catalog("Crime & Heist", "cinemeta-film-crime", "movie", "top", "Crime"),
        Catalog("Sci-Fi & Dunia Lain", "cinemeta-film-scifi", "movie", "top", "Sci-Fi"),
        Catalog("Thriller Pilihan", "cinemeta-film-thriller", "movie", "top", "Thriller"),
        Catalog("Horor Pilihan", "cinemeta-film-horror", "movie", "top", "Horror"),
        Catalog("Animasi & Keluarga", "cinemeta-film-animation", "movie", "top", "Animation"),
        Catalog("Film Fantasi", "cinemeta-film-fantasy", "movie", "top", "Fantasy"),
        Catalog("Film Komedi", "cinemeta-film-comedy", "movie", "top", "Comedy"),
        Catalog("Film Dokumenter", "cinemeta-film-documentary", "movie", "top", "Documentary"),
        Catalog("Film Rilis $currentYear", "cinemeta-film-tahun-ini", "movie", "year", currentYear),
        Catalog("Serial Populer Dunia", "cinemeta-serial-populer", "series", "top"),
        Catalog("Serial Rating Tertinggi", "cinemeta-serial-terbaik", "series", "imdbRating"),
        Catalog("Serial Crime", "cinemeta-serial-crime", "series", "top", "Crime"),
        Catalog("Serial Animasi", "cinemeta-serial-animation", "series", "top", "Animation"),
        Catalog("Serial Drama", "cinemeta-serial-drama", "series", "top", "Drama"),
        Catalog("Serial Misteri", "cinemeta-serial-mystery", "series", "top", "Mystery"),
        Catalog("Serial Sci-Fi", "cinemeta-serial-scifi", "series", "top", "Sci-Fi"),
        Catalog("Serial Rilis $currentYear", "cinemeta-serial-tahun-ini", "series", "year", currentYear)
    )

    val collections: List<SiteCollection> = catalogs.map { SiteCollection(it.title, it.slug) }

    fun isExternalCollection(slug: String): Boolean = catalogs.any { it.slug == slug }

    fun collectionTitle(slug: String): String? = catalogs.firstOrNull { it.slug == slug }?.title

    private const val TTL_MS = 30 * 60_000L
    private const val PROVIDER_PAGE_SIZE = 100
    const val MAX_DISCOVERY_TITLES = 108
    private val cache = ConcurrentHashMap<String, Pair<Long, List<ExternalTitle>>>()
    private val client = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val imdbIdPattern = Regex("""tt\d{5,12}""")
    private val yearPattern = Regex("""(?:18|19|20)\d{2}""")

    private fun JsonObject.string(key: String): String? =
        (get(key) as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }

    /** Be defensive: Cinemeta is an addon endpoint, not a versioned API contract. */
    fun parseCatalog(json: String, expectedType: String): List<ExternalTitle> {
        val root = Json.parseToJsonElement(json) as? JsonObject ?: return emptyList()
        val metas = root["metas"] as? JsonArray ?: return emptyList()
        return metas.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val imdbId = item.string("id") ?: return@mapNotNull null
            if (!imdbIdPattern.matches(imdbId)) return@mapNotNull null
            val title = item.string("name")?.takeIf { it.length in 2..150 }
                ?: return@mapNotNull null
            val type = item.string("type") ?: expectedType
            if (type != expectedType) return@mapNotNull null
            val year = yearPattern.find(
                item.string("releaseInfo") ?: item.string("year").orEmpty()
            )?.value ?: return@mapNotNull null
            val poster = item.string("poster")?.takeIf {
                it.startsWith("https://") && it.length <= 1000
            }
            ExternalTitle(imdbId, title, year, type, poster)
        }.distinctBy { it.imdbId }.take(PROVIDER_PAGE_SIZE)
    }

    private fun catalogUrl(catalog: Catalog, skip: Int = 0): String = buildString {
        append("https://v3-cinemeta.strem.io/catalog/")
        append(catalog.contentType)
        append("/")
        append(catalog.catalogId)
        if (catalog.genre != null || skip > 0) {
            append("/")
            if (catalog.genre != null) {
                append("genre=")
                append(catalog.genre)
            }
            if (skip > 0) {
                if (catalog.genre != null) append("&")
                append("skip=")
                append(skip)
            }
        }
        append(".json")
    }

    /** Observable for tests: catalog paths must come from the fixed manifest-compatible whitelist. */
    internal fun catalogUrlFor(slug: String, skip: Int = 0): String? =
        catalogs.firstOrNull { it.slug == slug }?.let { catalogUrl(it, skip) }

    /**
     * Collect only as many external candidates as the user requested.
     * A Stremio catalog page contains up to 100 entries; we retain the full
     * page in memory and reuse it across successive "more" actions.
     */
    suspend fun getTitles(slug: String, limit: Int = 18): List<ExternalTitle> =
        withContext(Dispatchers.IO) {
            val catalog = catalogs.firstOrNull { it.slug == slug }
                ?: return@withContext emptyList()
            val desired = limit.coerceIn(1, MAX_DISCOVERY_TITLES)
            val titles = mutableListOf<ExternalTitle>()
            for (skip in 0 until desired step PROVIDER_PAGE_SIZE) {
                val cacheKey = "$slug:$skip"
                val now = System.currentTimeMillis()
                val cached = cache[cacheKey]?.takeIf {
                    now - it.first in 0 until TTL_MS
                }?.second
                val page = cached ?: try {
                    val request = Request.Builder().url(catalogUrl(catalog, skip))
                        .header("Accept", "application/json")
                        .header("User-Agent", "IDLIX-Android/1.0")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) emptyList()
                        else response.body?.string()
                            ?.takeIf { it.length <= 2_000_000 }
                            ?.let { parseCatalog(it, catalog.contentType) }.orEmpty()
                    }.also { loaded ->
                        if (loaded.isNotEmpty()) {
                            if (cache.size >= 48) cache.clear()
                            cache[cacheKey] = System.currentTimeMillis() to loaded
                        }
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    emptyList()
                }
                if (page.isEmpty()) break
                titles.addAll(page)
                if (page.size < PROVIDER_PAGE_SIZE) break
            }
            titles.distinctBy { it.imdbId }.take(desired)
        }

    private fun normalizedTitle(title: String): String {
        val withoutYear = title.trim()
            .replace(Regex("""\s*\((?:18|19|20)\d{2}\)$"""), "")
            .replace(Regex("""\s*[-–]\s*(?:18|19|20)\d{2}$"""), "")
        return SiteCatalogReferences.normalizedTitle(withoutYear)
    }

    /**
     * A metadata hit is not a playable title. Reject same-name remakes and
     * movie-vs-series collisions; never navigate with a Cinemeta IMDb ID.
     */
    fun matchesIdlixTitle(source: ExternalTitle, candidate: SearchResultItem): Boolean {
        val typeMatches = when (source.type) {
            "movie" -> candidate.contentType == "movie"
            "series" -> candidate.contentType == "tv_series" ||
                candidate.contentType == "series"
            else -> false
        }
        if (!typeMatches || candidate.slug.isBlank() || candidate.id.isBlank()) return false
        if (candidate.releaseYear != source.year) return false
        val target = normalizedTitle(source.title)
        return target.isNotBlank() && (
            normalizedTitle(candidate.title) == target ||
                normalizedTitle(candidate.originalTitle.orEmpty()) == target
            )
    }
}
