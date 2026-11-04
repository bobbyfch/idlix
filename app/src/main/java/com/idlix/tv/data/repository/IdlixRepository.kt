package com.idlix.tv.data.repository

import com.idlix.tv.IdlixTvApp
import com.idlix.tv.data.api.ApiClient
import com.idlix.tv.data.api.IdlixApiService
import com.idlix.tv.data.api.MajorplayApiService
import com.idlix.tv.data.model.ClaimRequest
import com.idlix.tv.data.model.EpisodeResponse
import com.idlix.tv.data.model.Genre
import com.idlix.tv.data.model.MovieDetail
import com.idlix.tv.data.model.MovieItem
import com.idlix.tv.data.model.MovieListResponse
import com.idlix.tv.data.model.PlayInfoResponse
import com.idlix.tv.data.model.RedeemRequest
import com.idlix.tv.data.model.ResolvedPlayback
import com.idlix.tv.data.model.SearchResultItem
import com.idlix.tv.data.model.SeriesDetail
import com.idlix.tv.data.model.SeriesItem
import com.idlix.tv.data.model.SeriesListResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class IdlixRepository(
    private val idlixApi: IdlixApiService = ApiClient.idlixApi,
    private val majorplayApi: MajorplayApiService = ApiClient.majorplayApi
) {

    /**
     * Initial homepage rails are reused between Home, Film, Serial and Explore.
     * A per-key Mutex coalesces concurrent requests without serializing other
     * genres. Failed/empty responses aren't cached, so retry stays possible.
     */
    private suspend fun <T> cachedFirstPage(
        key: String,
        request: suspend () -> List<T>
    ): List<T> = withContext(Dispatchers.IO) {
        val scopedKey = ApiClient.getBaseUrl() + "|" + key
        fun isFresh(time: Long) =
            System.currentTimeMillis() - time in 0 until FIRST_PAGE_TTL_MS

        firstPageCache[scopedKey]?.takeIf { isFresh(it.first) }?.let {
            @Suppress("UNCHECKED_CAST")
            return@withContext it.second as List<T>
        }

        val mutex = firstPageLocks.getOrPut(scopedKey) { Mutex() }
        mutex.lock()
        try {
            firstPageCache[scopedKey]?.takeIf { isFresh(it.first) }?.let {
                @Suppress("UNCHECKED_CAST")
                return@withContext it.second as List<T>
            }
            val items = try {
                request()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                emptyList()
            }
            if (items.isNotEmpty()) {
                if (firstPageCache.size >= 80) firstPageCache.clear()
                firstPageCache[scopedKey] = System.currentTimeMillis() to items
            }
            items
        } finally {
            mutex.unlock()
        }
    }

    suspend fun getTrendingMovies(): List<MovieItem> =
        cachedFirstPage("movies:popularity") {
            idlixApi.getMovies(page = 1, limit = 20, sort = "popularity").data
        }

    suspend fun getPopularMovies(): List<MovieItem> =
        cachedFirstPage("movies:default") {
            idlixApi.getMovies(page = 1, limit = 20).data
        }

    suspend fun getMoviesBySort(sort: String): List<MovieItem> =
        cachedFirstPage("movies:sort:$sort") {
            idlixApi.getMovies(page = 1, limit = 20, sort = sort).data
        }

    suspend fun getMoviesByGenre(genreSlug: String): List<MovieItem> =
        cachedFirstPage("movies:genre:${genreSlug.lowercase()}") {
            idlixApi.getMovies(page = 1, limit = 20, genre = genreSlug).data
        }

    suspend fun getPopularSeries(): List<SeriesItem> =
        cachedFirstPage("series:popularity") {
            idlixApi.getSeries(page = 1, limit = 20, sort = "popularity").data
        }

    suspend fun getSeriesBySort(sort: String): List<SeriesItem> =
        cachedFirstPage("series:sort:$sort") {
            idlixApi.getSeries(page = 1, limit = 20, sort = sort).data
        }

    suspend fun getSeriesByGenre(genreSlug: String): List<SeriesItem> =
        cachedFirstPage("series:genre:${genreSlug.lowercase()}") {
            idlixApi.getSeries(page = 1, limit = 20, genre = genreSlug).data
        }

    /** Existing catalog APIs only; never assume undocumented filter parameters exist. */
    suspend fun getAvailableGenres(): List<Genre> = withContext(Dispatchers.IO) {
        cachedFirstPage("genres") { idlixApi.getGenres().data }
    }

    /**
     * Paginated catalog discovery, shared by phone and TV.
     * The source only guarantees a genre filter; other filters must be verified
     * against item metadata. This uses multiple pages, never just page one.
     * Callers may request more pages when rare years/countries aren't seen yet.
     */
    suspend fun browseCatalog(
        section: String, value: String, maxPages: Int = 8
    ): List<MovieItem> = withContext(Dispatchers.IO) {
        fun SearchResultItem.asCatalogMovie() = MovieItem(
            id = id, title = title, slug = slug,
            posterPath = posterPath, backdropPath = backdropPath,
            releaseDate = releaseDate ?: firstAirDate,
            voteAverage = voteAverage, viewCount = viewCount,
            quality = quality, contentType = contentType,
            country = countryLabel,
            network = network, networks = networks,
            collection = collection, collections = collections,
            runtime = runtimeMinutes, originalLanguage = originalLanguage,
            genres = genres.map { name ->
                Genre(name = name, slug = name.lowercase()
                    .replace(Regex("[^a-z0-9]+"), "-").trim('-'))
            }
        )

        if (section == "collections") {
            if (CinemetaCollectionSource.isExternalCollection(value)) {
                // Metadata is only a discovery source. Return actual IDLIX API
                // results, never synthetic IMDb IDs/paths that cannot open.
                val requested = (18 * ((maxPages.coerceIn(4, 24) + 3) / 4))
                    .coerceAtMost(CinemetaCollectionSource.MAX_DISCOVERY_TITLES)
                val cacheKey = ApiClient.getBaseUrl() + "|cinemeta:" + value + ":" + requested
                val now = System.currentTimeMillis()
                curatedCollectionCache[cacheKey]
                    ?.takeIf { now - it.first in 0 until 30 * 60_000L }
                    ?.let { return@withContext it.second }

                val sourceTitles = CinemetaCollectionSource.getTitles(value, requested)
                val verified = mutableListOf<MovieItem>()
                // Three concurrent searches at most, on demand. Do not scan
                // dozens of catalogue pages merely to open an editorial list.
                for (batch in sourceTitles.chunked(3)) {
                    val found = kotlinx.coroutines.coroutineScope {
                        batch.map { title ->
                            async {
                                try {
                                    val matchKey = ApiClient.getBaseUrl() +
                                        "|cinemeta-title:" + title.type + ":" + title.imdbId
                                    val existing = curatedTitleCache[matchKey]?.takeIf {
                                        System.currentTimeMillis() - it.first in 0 until 5 * 60_000L
                                    }
                                    if (existing != null) {
                                        existing.second
                                    } else {
                                        val resolved = searchStrict(title.title)
                                            .firstOrNull { CinemetaCollectionSource.matchesIdlixTitle(title, it) }
                                            ?.asCatalogMovie()
                                            ?.let { item ->
                                                if (item.posterPath.isNullOrBlank() && title.posterUrl != null)
                                                    item.copy(posterPath = title.posterUrl)
                                                else item
                                            }
                                        if (curatedTitleCache.size >= 240) curatedTitleCache.clear()
                                        // Successful empty search is cached briefly as a negative match.
                                        curatedTitleCache[matchKey] = System.currentTimeMillis() to resolved
                                        resolved
                                    }
                                } catch (cancel: CancellationException) {
                                    throw cancel
                                } catch (_: Exception) {
                                    null
                                }
                            }
                        }.map { it.await() }
                    }
                    verified.addAll(found.filterNotNull())
                }
                val matches = verified.distinctBy { "${it.contentType}:${it.id}" }
                if (matches.isNotEmpty()) {
                    if (curatedCollectionCache.size >= 24) curatedCollectionCache.clear()
                    curatedCollectionCache[cacheKey] = System.currentTimeMillis() to matches
                }
                return@withContext matches
            }
            // The website's collection detail defines membership and order.
            if (value.isBlank() || value == "all") return@withContext emptyList()
            return@withContext SiteCollectionSource.getCollectionItems(value).map { item ->
                MovieItem(
                    id = item.slug,
                    title = item.title,
                    slug = item.slug,
                    contentType = item.contentType,
                    posterPath = item.posterUrl,
                    releaseDate = item.year?.plus("-01-01")
                )
            }
        }

        if (section == "network" && value.isNotBlank()) {
            val siteItems = SiteNetworkSource.getNetworkItems(value)
            if (siteItems.isNotEmpty()) {
                return@withContext siteItems.map { item ->
                    MovieItem(
                        id = item.slug,
                        title = item.title,
                        slug = item.slug,
                        contentType = item.contentType,
                        posterPath = item.posterUrl,
                        releaseDate = item.year?.plus("-01-01")
                    )
                }
            }
        }

        val genre = value.takeIf { section == "genre" && it.isNotBlank() }
        val items = linkedMapOf<String, MovieItem>()

        if (section == "network" && value.isNotBlank()) {
            // The public API often omits network metadata. Show exact matches
            // from a small, labelled provider-originals shortlist, without
            // turning broad fuzzy search into false provider classification.
            val titles = SiteCatalogReferences.networkTitleSeeds[value].orEmpty()
            for (batch in titles.chunked(4)) {
                val found = kotlinx.coroutines.coroutineScope {
                    batch.map { title ->
                        async {
                            search(title)
                                .filter { SiteCatalogReferences.isExactTitle(it.title, title) }
                                .map { it.asCatalogMovie() }
                        }
                    }.map { it.await() }.flatten()
                }
                found.forEach { items["${it.contentType}:${it.id}"] = it }
            }
        }
        // Keyword searches are supplementary, not replacements for year metadata.
        if (section == "year" && value.matches(Regex("""\d{4}"""))) {
            search(value).map { it.asCatalogMovie() }
                .filter { matchesCatalogFilter(it, section, value) }
                .forEach { items["${it.contentType}:${it.id}"] = it }
        }
        if (section == "shorts") {
            search("short").filter { it.isShort }.map { it.asCatalogMovie() }
                .forEach { items["${it.contentType}:${it.id}"] = it }
        }

        // Network has a separate originals search. Avoid scanning thousands of
        // unrelated titles on every provider switch on an Android TV.
        val pages = if (section == "network") maxPages.coerceIn(1, 4)
                    else maxPages.coerceIn(1, 48)
        val sortMode = "createdAt"
        var moviesEnd = false
        var seriesEnd = false
        // Page requests are paired, not an unbounded bulk download.
        for (page in 1..pages) {
            val (movies, shows) = kotlinx.coroutines.coroutineScope {
                val movieJob = async {
                    if (moviesEnd) null else try {
                        val key = "${genre.orEmpty()}:$page"
                        val cached = catalogMoviePages[key]
                        if (cached != null && System.currentTimeMillis() - cached.first < 5 * 60_000L) {
                            cached.second
                        } else {
                            idlixApi.getMovies(page = page, limit = 100, sort = sortMode, genre = genre).also {
                                if (catalogMoviePages.size > 120) catalogMoviePages.clear()
                                catalogMoviePages[key] = System.currentTimeMillis() to it
                            }
                        }
                    } catch (cancel: kotlinx.coroutines.CancellationException) {
                        throw cancel
                    } catch (_: Exception) { null }
                }
                val seriesJob = async {
                    if (seriesEnd) null else try {
                        val key = "${genre.orEmpty()}:$page"
                        val cached = catalogSeriesPages[key]
                        if (cached != null && System.currentTimeMillis() - cached.first < 5 * 60_000L) {
                            cached.second
                        } else {
                            idlixApi.getSeries(page = page, limit = 100, sort = sortMode, genre = genre).also {
                                if (catalogSeriesPages.size > 120) catalogSeriesPages.clear()
                                catalogSeriesPages[key] = System.currentTimeMillis() to it
                            }
                        }
                    } catch (cancel: kotlinx.coroutines.CancellationException) {
                        throw cancel
                    } catch (_: Exception) { null }
                }
                movieJob.await() to seriesJob.await()
            }
            val movieItems = movies?.data.orEmpty()
            val seriesItems = shows?.data.orEmpty().map { it.toMovieItem() }
            moviesEnd = moviesEnd || movieItems.isEmpty() ||
                (movies?.pagination?.totalPages?.let { page >= it } == true)
            seriesEnd = seriesEnd || seriesItems.isEmpty() ||
                (shows?.pagination?.totalPages?.let { page >= it } == true)

            (movieItems + seriesItems).forEach { item ->
                if (matchesCatalogFilter(item, section, value)) {
                    items["${item.contentType}:${item.id}"] = item
                }
            }
            // Respect the requested depth. Otherwise "Muat lebih banyak"
            // would stop at the same 64 results and never discover new years.
            if (moviesEnd && seriesEnd) break
        }
        items.values.toList()
    }

    suspend fun getMovieDetail(slug: String): MovieDetail? = withContext(Dispatchers.IO) {
        try {
            idlixApi.getMovieDetail(slug)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSeriesDetail(slug: String): SeriesDetail? = withContext(Dispatchers.IO) {
        try {
            idlixApi.getSeriesDetail(slug)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getEpisodeDetail(slug: String, season: Int, episode: Int): EpisodeResponse? = withContext(Dispatchers.IO) {
        try {
            idlixApi.getEpisodeDetail(slug, season, episode)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCollectionTitles(): CollectionListing = SiteCollectionSource.getCollections()

    suspend fun getAvailableNetworks(): List<SiteNetwork> = SiteNetworkSource.getNetworks()

    /** User-facing search distinguishes a failed request from zero matches. */
    suspend fun searchStrict(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) emptyList() else idlixApi.search(trimmed).results
    }

    suspend fun search(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        try {
            val res = idlixApi.search(trimmed)
            res.results
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("IdlixRepository", "Backend search error: ${e.message}")
            emptyList()
        }
    }

    private data class CachedPlayback(
        val playback: ResolvedPlayback,
        val timestamp: Long = System.currentTimeMillis()
    )

    companion object {
        // One cache shared by Home / Film / Serial VMs; avoids downloading the
        // same genre rail repeatedly when switching tabs. Only non-empty lists.
        private val curatedCollectionCache =
            ConcurrentHashMap<String, Pair<Long, List<MovieItem>>>()
        private val curatedTitleCache =
            ConcurrentHashMap<String, Pair<Long, MovieItem?>>()
        private const val FIRST_PAGE_TTL_MS = 5 * 60_000L
        private val firstPageCache = ConcurrentHashMap<String, Pair<Long, List<*>>>()
        private val firstPageLocks = ConcurrentHashMap<String, Mutex>()
        // Share paged catalog snapshots across year/country/network filters.
        // Re-scanning deeper pages no longer downloads already inspected pages.
        private val catalogMoviePages = ConcurrentHashMap<String, Pair<Long, MovieListResponse>>()
        private val catalogSeriesPages = ConcurrentHashMap<String, Pair<Long, SeriesListResponse>>()
        private val playbackCache = ConcurrentHashMap<String, CachedPlayback>()
        private val activePrewarms = ConcurrentHashMap<String, Deferred<Result<ResolvedPlayback>>>()
        private const val CACHE_TTL_MS = 60 * 60 * 1000L // 60 minutes
        private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    /**
     * Pre-warms the stream resolution in the background while the user is reading
     * details or looking at synopsis. When they click "Play", the stream is already unlocked!
     */
    fun prewarmStream(contentType: String, contentId: String, contentTitle: String) {
        if (contentId.isBlank()) return
        val cacheKey = "$contentType:$contentId"
        val cached = playbackCache[cacheKey]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TTL_MS) {
            return
        }

        // LAZY + putIfAbsent avoids duplicate resolvers and the race where a
        // coroutine finishes before it has been entered into activePrewarms.
        val pending = repoScope.async(start = CoroutineStart.LAZY) {
            try {
                val result = resolveStreamInternal(contentType, contentId, contentTitle, onCountdown = null)
                result.onSuccess { playbackCache[cacheKey] = CachedPlayback(it) }
                result
            } finally {
                activePrewarms.remove(cacheKey)
            }
        }
        if (activePrewarms.putIfAbsent(cacheKey, pending) == null) {
            pending.start()
        } else {
            pending.cancel()
        }
    }

    suspend fun resolveStream(
        contentType: String,
        contentId: String,
        contentTitle: String,
        onCountdown: ((Int) -> Unit)? = null
    ): Result<ResolvedPlayback> = withContext(Dispatchers.IO) {
        val cacheKey = "$contentType:$contentId"

        // 1. Instant hit: Already cached in memory
        val cached = playbackCache[cacheKey]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TTL_MS) {
            onCountdown?.invoke(0)
            return@withContext Result.success(cached.playback)
        }

        // 2. Pre-warming hit: Already resolving in background
        val ongoing = activePrewarms[cacheKey]
        if (ongoing != null) {
            try {
                val res = ongoing.await()
                if (res.isSuccess) {
                    onCountdown?.invoke(0)
                    return@withContext res
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) { }
        }

        // 3. Fallback to standard resolution
        val res = resolveStreamInternal(contentType, contentId, contentTitle, onCountdown)
        res.onSuccess { resolved ->
            playbackCache[cacheKey] = CachedPlayback(resolved)
        }
        res
    }

    private suspend fun resolveStreamInternal(
        contentType: String,
        contentId: String,
        contentTitle: String,
        onCountdown: ((Int) -> Unit)? = null
    ): Result<ResolvedPlayback> = withContext(Dispatchers.IO) {
        try {
            var targetType = contentType
            var targetId = contentId

            // If a TV series ID is passed instead of an episode ID, retrieve episode 1 of season 1
            if (contentType == "tv_series") {
                val series = getSeriesDetail(contentId)
                val firstEp = series?.defaultSeason?.episodes?.firstOrNull()
                    ?: series?.seasons?.firstOrNull()?.let { s ->
                        getEpisodeDetail(series.slug, s.seasonNumber, 1)?.episode
                    }

                if (firstEp != null) {
                    targetType = "episode"
                    targetId = firstEp.id
                } else {
                    return@withContext Result.failure(Exception("Tidak ada episode yang tersedia untuk serial ini."))
                }
            }

            // High-performance path: If no interactive countdown listener is needed, use backend one-shot resolver
            if (onCountdown == null) {
                try {
                    val resolved = idlixApi.resolvePlayback(
                        contentType = targetType,
                        contentId = targetId,
                        contentTitle = contentTitle
                    )
                    if (resolved.hlsMasterUrl.isNotBlank()) {
                        return@withContext Result.success(resolved)
                    }
                } catch (e: Exception) {
                    android.util.Log.d("IdlixRepository", "One-shot resolver fallback: ${e.message}")
                }
            }

            // Step 1: Get play-info (gateToken + unlockAt) from backend API
            val playInfo: PlayInfoResponse = if (targetType == "episode") {
                idlixApi.getEpisodePlayInfo(targetId)
            } else {
                idlixApi.getMoviePlayInfo(targetId)
            }

            if (playInfo.gateToken.isBlank()) {
                return@withContext Result.failure(Exception("Gagal memperoleh token pemutaran"))
            }

            // Step 2: Server-synchronized countdown
            val diffMs = if (playInfo.serverNow > 0 && playInfo.unlockAt > playInfo.serverNow) {
                playInfo.unlockAt - playInfo.serverNow
            } else {
                15000L
            }

            // Extract owner device ID from gateToken to construct valid session cookie
            val didMatch = try {
                val rawP0 = playInfo.gateToken.split(".")[0]
                val decodedBytes = try {
                    java.util.Base64.getUrlDecoder().decode(rawP0)
                } catch (e: Throwable) {
                    android.util.Base64.decode(rawP0, android.util.Base64.URL_SAFE)
                }
                val p0 = String(decodedBytes, Charsets.UTF_8)
                """"owner":"d:([^"]+)"""".toRegex().find(p0)?.groupValues?.get(1)
            } catch (e: Exception) {
                null
            }
            val didCookie = if (!didMatch.isNullOrBlank()) "did=$didMatch" else null

            var secondsLeft = (diffMs / 1000).toInt()
            while (secondsLeft > 1) {
                onCountdown?.invoke(secondsLeft)
                delay(1000)
                secondsLeft--
            }
            if (secondsLeft > 0) {
                onCountdown?.invoke(1)
            }

            // Step 3: Claim session with retry polling via Backend API
            var validClaimRes: com.idlix.tv.data.model.ClaimResponse? = null

            for (attempt in 1..15) {
                try {
                    val res = idlixApi.claimSession(
                        request = ClaimRequest(gateToken = playInfo.gateToken),
                        cookie = didCookie
                    )
                    if (res.kind == "pentos" && res.claim.isNotBlank()) {
                        validClaimRes = res
                        break
                    }
                } catch (_: Exception) {}
                delay(600)
            }

            onCountdown?.invoke(0)

            val claimRes = validClaimRes
            if (claimRes == null || claimRes.claim.isBlank()) {
                return@withContext Result.failure(Exception("Gagal mengklaim sesi pemutaran. Silakan coba lagi."))
            }

            // Step 4: Redeem HLS URL from majorplay CDN
            val redeemUrl = claimRes.redeemUrl.ifBlank { "https://e2e.majorplay.net/api/play" }
            val redeemRes = try {
                majorplayApi.redeemPlay(
                    url = redeemUrl,
                    request = RedeemRequest(claim = claimRes.claim, mode = "browser")
                )
            } catch (e: Exception) {
                null
            }

            if (redeemRes == null || redeemRes.url.isBlank()) {
                return@withContext Result.failure(Exception("Gagal mengambil master playlist HLS."))
            }

            val finalSubtitles = redeemRes.subtitles.toMutableList()
            val hasIndoSub = finalSubtitles.any { it.lang == "id" }

            // SubDL Third-party Subtitle Fallback:
            // If the content has no Indonesian subtitle (or no subtitles at all), fetch from subdl.com
            if (!hasIndoSub) {
                try {
                    val subdlSub = SubdlRepository.fetchIndonesianSubtitle(
                        title = contentTitle,
                        type = if (targetType == "episode") "tv" else "movie",
                        context = IdlixTvApp.appContext
                    )
                    if (subdlSub != null) {
                        finalSubtitles.add(subdlSub)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("IdlixRepository", "SubDL fallback failed: ${e.message}")
                }
            }

            Result.success(
                ResolvedPlayback(
                    title = claimRes.title.ifBlank { contentTitle },
                    hlsMasterUrl = redeemRes.url,
                    subtitles = finalSubtitles,
                    durationSec = claimRes.durationSec,
                    videoId = claimRes.videoId
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
