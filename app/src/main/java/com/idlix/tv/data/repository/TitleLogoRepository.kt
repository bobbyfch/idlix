package com.idlix.tv.data.repository

import com.idlix.tv.data.model.titleLogoImageUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Prefer explicit IDLIX title art, then optional public IMDb-indexed metadata. */
object TitleLogoRepository {
    private data class CacheEntry(val url: String?, val createdAt: Long)
    private const val VALID_MS = 30 * 60_000L
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val client by lazy {
        OkHttpClient.Builder()
            .callTimeout(2300, TimeUnit.MILLISECONDS)
            .connectTimeout(1800, TimeUnit.MILLISECONDS)
            .readTimeout(1900, TimeUnit.MILLISECONDS)
            .followRedirects(false)
            .build()
    }
    private val imdbPattern = Regex("""tt\d{5,12}""")

    private fun key(type: String, slug: String): String =
        (if (type == "movie") "movie" else "series") + ":" + slug.lowercase()

    fun cached(type: String, slug: String?): String? {
        if (slug.isNullOrBlank()) return null
        return cache[key(type, slug)]?.takeIf {
            System.currentTimeMillis() - it.createdAt < VALID_MS
        }?.url
    }

    suspend fun resolve(
        type: String,
        slug: String?,
        explicitLogo: String?,
        imdbId: String?
    ): String? {
        val direct = titleLogoImageUrl(explicitLogo)
        if (direct != null) {
            if (!slug.isNullOrBlank()) cache[key(type, slug)] = CacheEntry(direct, System.currentTimeMillis())
            return direct
        }
        val cacheKey = slug?.takeIf(String::isNotBlank)?.let { key(type, it) }
        if (cacheKey != null) {
            val hit = cache[cacheKey]
            if (hit != null && System.currentTimeMillis() - hit.createdAt < VALID_MS) return hit.url
        }
        val imdb = imdbId?.takeIf { imdbPattern.matches(it) }
        val found = if (imdb == null) null else withContext(Dispatchers.IO) {
            try {
                val kind = if (type == "movie") "movie" else "series"
                val url = "https://v3-cinemeta.strem.io/meta/$kind/$imdb.json"
                client.newCall(Request.Builder().url(url).header("Accept", "application/json").build())
                    .execute().use { response ->
                        if (!response.isSuccessful) return@use null
                        val body = response.body?.string() ?: return@use null
                        parseCinemetaTitleLogo(body)
                    }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                null
            }
        }
        if (cacheKey != null) cache[cacheKey] = CacheEntry(found, System.currentTimeMillis())
        return found
    }
}

/**
 * A Stremio meta.logo is transparent title artwork, not its poster/background.
 * Reject unknown image hosts; malformed or unavailable metadata simply falls
 * back to the visible title.
 */
internal fun parseCinemetaTitleLogo(json: String): String? = try {
    val obj = Json.parseToJsonElement(json) as? JsonObject
    val meta = obj?.get("meta") as? JsonObject
    val image = (meta?.get("logo") as? JsonPrimitive)?.contentOrNull
    val parsed = image?.toHttpUrlOrNull()
    if (parsed?.isHttps == true && parsed.host in setOf(
        "image.tmdb.org", "images.metahub.space",
        "assets.fanart.tv", "i.imgur.com"
    )) image else null
} catch (_: Exception) {
    null
}
