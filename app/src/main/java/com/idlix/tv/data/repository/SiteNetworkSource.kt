package com.idlix.tv.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class SiteNetwork(
    val name: String,
    val slug: String
)

data class SiteNetworkTitle(
    val title: String,
    val slug: String,
    val contentType: String,
    val year: String? = null,
    val posterUrl: String? = null
)

/**
 * Reads the provider index and each provider's own IDLIX listing. This keeps
 * Network results tied to the page that owns them instead of guessing from
 * sparse API metadata.
 */
object SiteNetworkSource {
    private const val ROOT = "https://z2.idlixku.com"
    private const val INDEX = "$ROOT/network"
    private const val INDEX_TTL_MS = 20 * 60_000L
    private const val DETAIL_TTL_MS = 10 * 60_000L
    private val slugPattern = Regex("^[a-z0-9-]{1,100}$")
    private val providerPath = Regex("/network/([a-z0-9-]+)/?$", RegexOption.IGNORE_CASE)
    private val itemPath = Regex("/(movie|series)/([a-z0-9-]+)/?$", RegexOption.IGNORE_CASE)
    private val yearPattern = Regex("\\b(19|20)\\d{2}\\b")

    private val client = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    @Volatile private var cachedNetworks: Pair<Long, List<SiteNetwork>>? = null
    private val itemCache = ConcurrentHashMap<String, Pair<Long, List<SiteNetworkTitle>>>()

    private fun imageOf(anchor: Element): String? {
        val img = anchor.selectFirst("img") ?: return null
        val raw = listOf("data-src", "data-lazy-src", "data-original", "src")
            .firstNotNullOfOrNull { key ->
                img.attr(key).takeIf { it.isNotBlank() && !it.startsWith("data:") }
            } ?: return null
        return when {
            raw.startsWith("https://") || raw.startsWith("http://") -> raw
            raw.startsWith("//") -> "https:" + raw
            raw.startsWith("/") -> ROOT + raw
            else -> null
        }
    }

    fun parseNetworks(html: String): List<SiteNetwork> {
        val doc = Jsoup.parse(html, INDEX)
        return doc.select("a[href]").mapNotNull { anchor ->
            val href = anchor.absUrl("href").substringBefore('?').trimEnd('/')
            if (!href.startsWith("$ROOT/network/")) return@mapNotNull null
            val slug = providerPath.find(href.removePrefix(ROOT))?.groupValues?.get(1)
                ?: return@mapNotNull null
            val rawName = anchor.selectFirst("h2,h3,h4,h5")?.text()?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: anchor.text().trim()
            val name = rawName
                .replace(" Movies & Series", "", ignoreCase = true)
                .replace(" Movie & Series", "", ignoreCase = true)
                .trim()
                .takeIf { it.length in 1..80 }
                ?: slug.split('-').joinToString(" ") { word ->
                    word.replaceFirstChar { it.titlecase(Locale.ROOT) }
                }
            SiteNetwork(name = name, slug = slug.lowercase(Locale.ROOT))
        }.distinctBy { it.slug }.take(80)
    }

    fun parseItems(html: String): List<SiteNetworkTitle> {
        val doc = Jsoup.parse(html, ROOT)
        return doc.select("a[href]").mapNotNull { anchor ->
            val href = anchor.absUrl("href").substringBefore('?').trimEnd('/')
            if (!href.startsWith("$ROOT/")) return@mapNotNull null
            val match = itemPath.find(href.removePrefix(ROOT)) ?: return@mapNotNull null
            val slug = match.groupValues[2]
            val contentType = if (match.groupValues[1].equals("series", true)) "tv_series" else "movie"
            val title = anchor.selectFirst("h2,h3,h4,h5")?.text()?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: anchor.selectFirst("img[alt]")?.attr("alt")?.trim()
                    ?.takeIf { it.isNotBlank() }
                ?: anchor.attr("title").trim().takeIf { it.isNotBlank() }
                ?: slug.replace(Regex("-\\d{4}$"), "")
                    .split('-').joinToString(" ") { word ->
                        word.replaceFirstChar { it.titlecase(Locale.ROOT) }
                    }
            val year = yearPattern.find(anchor.text())?.value
                ?: slug.substringAfterLast('-').takeIf { it.matches(Regex("[0-9]{4}")) }
            SiteNetworkTitle(
                title = title,
                slug = slug,
                contentType = contentType,
                year = year,
                posterUrl = imageOf(anchor)
            )
        }.distinctBy { it.contentType + ":" + it.slug }.take(200)
    }

    private fun fetchPage(url: String): String? {
        val request = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (Android; IDLIX)")
            .header("Accept", "text/html")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null
            else response.body?.string()?.takeIf { it.length <= 3_000_000 }
        }
    }

    suspend fun getNetworks(): List<SiteNetwork> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cachedNetworks?.takeIf { now - it.first < INDEX_TTL_MS }?.let { return@withContext it.second }

        val live = try {
            fetchPage(INDEX)?.let(::parseNetworks).orEmpty()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            emptyList()
        }
        val networks = (live + SiteCatalogReferences.networkProviders)
            .distinctBy { it.slug }
            .take(80)
        cachedNetworks = System.currentTimeMillis() to networks
        networks
    }

    suspend fun getNetworkItems(slug: String): List<SiteNetworkTitle> = withContext(Dispatchers.IO) {
        val normalized = slug.lowercase(Locale.ROOT)
        if (!slugPattern.matches(normalized)) return@withContext emptyList()

        val now = System.currentTimeMillis()
        itemCache[normalized]
            ?.takeIf { now - it.first in 0 until DETAIL_TTL_MS }
            ?.let { return@withContext it.second }

        val items = try {
            fetchPage("$ROOT/network/$normalized")?.let(::parseItems).orEmpty()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            emptyList()
        }
        if (items.isNotEmpty()) {
            if (itemCache.size >= 48) itemCache.clear()
            itemCache[normalized] = System.currentTimeMillis() to items
        }
        items
    }
}
