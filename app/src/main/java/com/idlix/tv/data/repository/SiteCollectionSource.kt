package com.idlix.tv.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class SiteCollection(
    val title: String,
    val slug: String,
    val count: Int? = null,
    val imageUrl: String? = null
)

data class CollectionListing(
    val collections: List<SiteCollection>,
    val fromWebsite: Boolean
) {
    val titles: List<String> get() = collections.map { it.title }
}

data class SiteCollectionItem(
    val title: String,
    val slug: String,
    val contentType: String,
    val year: String? = null,
    val posterUrl: String? = null
)

/**
 * /collection lists actual curated franchises; the homepage "Collections"
 * editorial rail contains individual titles and must not be used here.
 */
object SiteCollectionSource {
    private const val ROOT = "https://z2.idlixku.com"
    private const val INDEX = "$ROOT/collection"
    private const val TTL_MS = 20 * 60_000L
    private const val FALLBACK_TTL_MS = 2 * 60_000L
    private val slugPattern = Regex("^[a-z0-9-]{1,100}$")
    private val collectionPath = Regex("/collection/([a-z0-9-]+)/?$", RegexOption.IGNORE_CASE)
    private val itemPath = Regex("/(movie|series)/([a-z0-9-]+)/?$", RegexOption.IGNORE_CASE)
    private val countPattern = Regex("""(\d+)\s+judul""", RegexOption.IGNORE_CASE)
    private val yearPattern = Regex("""\((\d{4})\)""")

    private val client = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    @Volatile private var cached: Pair<Long, CollectionListing>? = null
    private val detailCache = ConcurrentHashMap<String, Pair<Long, List<SiteCollectionItem>>>()
    private const val DETAIL_TTL_MS = 10 * 60_000L

    fun parseCollections(html: String): List<SiteCollection> {
        val doc = Jsoup.parse(html, INDEX)
        return doc.select("a[href]").mapNotNull { anchor ->
            val href = anchor.absUrl("href")
            if (!href.startsWith(ROOT + "/collection/")) return@mapNotNull null
            val slug = collectionPath.find(href.substringBefore('?').trimEnd('/'))?.groupValues?.get(1)
                ?: return@mapNotNull null
            val rawText = anchor.text().trim()
            val title = anchor.selectFirst("h2,h3,h4,h5")?.text()?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: rawText.replace(Regex("""\s+\d+\s+judul\b.*$""", RegexOption.IGNORE_CASE), "").trim()
            if (title.length !in 2..120) return@mapNotNull null
            SiteCollection(
                title = title,
                slug = slug.lowercase(Locale.ROOT),
                count = countPattern.find(rawText)?.groupValues?.get(1)?.toIntOrNull(),
                imageUrl = SiteImageUrl.from(anchor)
            )
        }.distinctBy { it.slug }.take(100)
    }

    fun parseItems(html: String, baseUrl: String = ROOT): List<SiteCollectionItem> {
        val doc = Jsoup.parse(html, baseUrl)
        return doc.select("a[href]").mapNotNull { anchor ->
            val href = anchor.absUrl("href")
            if (!href.startsWith(ROOT + "/")) return@mapNotNull null
            val match = itemPath.find(href.substringBefore('?').trimEnd('/'))
                ?: return@mapNotNull null
            val type = if (match.groupValues[1].equals("series", true)) "tv_series" else "movie"
            val slug = match.groupValues[2]
            val name = anchor.selectFirst("h3,h4,h5")?.text()?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: anchor.selectFirst("img[alt]")?.attr("alt")?.trim()
                    ?.takeIf { it.isNotBlank() }
                ?: anchor.attr("title").trim().takeIf { it.isNotBlank() }
                ?: slug.replace(Regex("-\\d{4}$"), "")
                    .split('-').joinToString(" ") { part ->
                        part.replaceFirstChar { it.titlecase(Locale.ROOT) }
                    }
            val year = yearPattern.find(anchor.text())?.groupValues?.get(1)
                ?: Regex("""-(\d{4})$""").find(slug)?.groupValues?.get(1)
            SiteCollectionItem(name, slug, type, year, SiteImageUrl.from(anchor))
        }.distinctBy { it.contentType + ":" + it.slug }.take(200)
    }

    private fun fetchPage(url: String): String? {
        val request = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Referer", "$ROOT/")
            .header("Origin", ROOT)
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null
            else response.body?.string()?.takeIf { it.length <= 3_000_000 }
        }
    }

    suspend fun getCollections(): CollectionListing = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cached?.let { (time, listing) ->
            val lifetime = if (listing.fromWebsite) TTL_MS else FALLBACK_TTL_MS
            if (now - time < lifetime) return@withContext listing
        }
        val live = try { fetchPage(INDEX)?.let(::parseCollections).orEmpty() }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { emptyList() }
        val listing = if (live.isNotEmpty()) CollectionListing(live, true)
            else CollectionListing(SiteCatalogReferences.collections, false)
        cached = now to listing
        listing
    }

    /** Reopening a franchise never refetches the whole page during this session. */
    suspend fun getCollectionItems(slug: String): List<SiteCollectionItem> =
        withContext(Dispatchers.IO) {
            val normalized = slug.lowercase(Locale.ROOT)
            if (!slugPattern.matches(normalized)) return@withContext emptyList()
            val now = System.currentTimeMillis()
            detailCache[normalized]?.takeIf { now - it.first in 0 until DETAIL_TTL_MS }
                ?.let { return@withContext it.second }

            val pageUrl = ROOT + "/collection/" + normalized
            val items = try {
                fetchPage(pageUrl)?.let { parseItems(it, pageUrl) }.orEmpty()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                emptyList()
            }
            if (items.isNotEmpty()) {
                if (detailCache.size >= 48) detailCache.clear()
                detailCache[normalized] = System.currentTimeMillis() to items
            }
            items
        }

}
