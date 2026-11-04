package com.idlix.tv.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.idlix.tv.data.model.Genre
import com.idlix.tv.data.model.MovieItem
import com.idlix.tv.data.repository.IdlixRepository
import com.idlix.tv.data.repository.CinemetaCollectionSource
import com.idlix.tv.data.repository.SiteCatalogReferences
import com.idlix.tv.data.repository.SiteNetworkSource
import com.idlix.tv.ui.components.NetworkBrandLogo
import com.idlix.tv.data.local.SavedCollectionsStore
import com.idlix.tv.ui.screens.browse.choicesFor
import com.idlix.tv.ui.screens.browse.catalogSections
import com.idlix.tv.ui.screens.browse.BrowseChoice
import com.idlix.tv.ui.screens.browse.sortedItems
import com.idlix.tv.ui.screens.history.ContinueWatchingViewModel
import com.idlix.tv.ui.screens.home.HomeViewModel
import com.idlix.tv.ui.screens.movies.MoviesViewModel
import com.idlix.tv.ui.screens.search.SearchViewModel
import com.idlix.tv.ui.screens.series.SeriesViewModel
import com.idlix.tv.ui.theme.NetflixRed
import com.idlix.tv.ui.theme.NetflixWhite
import com.idlix.tv.ui.theme.NetflixBlack
import com.idlix.tv.ui.theme.NetflixDarkCard
import com.idlix.tv.ui.theme.NetflixLightGray

typealias MobilePlay = (String, String, String, String?, String?, String?, Int?, Int?) -> Unit

@Composable
fun MobileHomeScreen(
    onBrowse: OpenBrowse,
    onDetail: (String, String) -> Unit,
    onPlay: MobilePlay,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    if (state.isLoading && state.heroMovie == null) {
        MobileLoading()
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NetflixBlack),
        contentPadding = PaddingValues(bottom = 136.dp),
        verticalArrangement = Arrangement.spacedBy(23.dp)
    ) {
        item {
            MobileHero(
                item = state.heroMovie,
                onDetails = { state.heroMovie?.let { onDetail(it.contentType, it.slug) } },
                onPlay = {
                    state.heroMovie?.let {
                        if (it.contentType == "tv_series") onDetail(it.contentType, it.slug)
                        else onPlay(it.contentType, it.id, it.title, it.fullPosterUrl, it.fullBackdropUrl, it.slug, null, null)
                    }
                }
            )
        }
        if (state.error != null) item {
            MobileEmpty(state.error.orEmpty(), onRetry = { viewModel.loadHomeData() })
        }
        if (state.continueWatching.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Lanjutkan Menonton", Modifier.padding(horizontal = 18.dp), color = NetflixWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.continueWatching, key = { it.contentId }) { history ->
                        MobilePosterCard(
                            title = history.title,
                            poster = history.resolvedThumbnail,
                            rating = null,
                            progressLabel = if (history.progressPercent > 0) "${history.progressPercent}%" else null,
                            onClick = {
                                onPlay(
                                    history.contentType, history.contentId, history.title,
                                    history.posterUrl, history.backdropUrl, history.slug,
                                    history.seasonNumber, history.episodeNumber
                                )
                            }
                        )
                    }
                }
            }
        }
        item { MobileMovieRow("Sedang Tren", state.trending, onItemClick = { onDetail(it.contentType, it.slug) }) }
        item { MobileNetworkOriginalsRow(onBrowse = onBrowse) }
        if (state.trendingCountry.isNotEmpty()) item {
            MobileMovieRow("Trending Indonesia", state.trendingCountry) {
                onDetail(it.contentType, it.slug)
            }
        }
        if (state.networkPicks.isNotEmpty()) item {
            MobileMovieRow("Pilihan Network", state.networkPicks) {
                onDetail(it.contentType, it.slug)
            }
        }
        item { MobileMovieRow("Film Populer", state.popularMovies, onItemClick = { onDetail(it.contentType, it.slug) }) }
        item { MobileMovieRow("Serial Pilihan", state.popularSeries.map { it.toMovieItem() }, onItemClick = { onDetail(it.contentType, it.slug) }) }
        item { MobileMovieRow("Film Terbaru", state.latestMovies, onItemClick = { onDetail(it.contentType, it.slug) }) }
        item { MobileMovieRow("Serial Terbaru", state.latestSeries, onItemClick = { onDetail(it.contentType, it.slug) }) }
        item { MobileMovieRow("Aksi & Petualangan", state.actionMovies, onItemClick = { onDetail(it.contentType, it.slug) }) }
        items(state.extraShelves, key = { it.title }) { shelf ->
            MobileMovieRow(shelf.title, shelf.items) { onDetail(it.contentType, it.slug) }
        }
    }
}

@Composable
private fun MobileNetworkOriginalsRow(onBrowse: OpenBrowse) {
    val featuredNetworks = SiteCatalogReferences.networkProviders.take(5)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("Network Originals", color = NetflixWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { onBrowse("network", "", "all") }) {
                Text("Lihat semua", color = NetflixWhite, fontSize = 12.sp)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(featuredNetworks, key = { it.slug }) { network ->
                MobileFilterChip(
                    label = network.name,
                    selected = false,
                    onClick = { onBrowse("network", network.slug, "all") },
                    leadingIcon = {
                        NetworkBrandLogo(
                            slug = network.slug,
                            label = network.name,
                            modifier = Modifier.size(18.dp),
                            compact = true
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun MobileMoviesScreen(
    onBrowse: OpenBrowse,
    onDetail: (String, String) -> Unit,
    onPlay: MobilePlay,
    viewModel: MoviesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    if (state.isLoading && state.heroMovie == null) { MobileLoading(); return }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NetflixBlack),
        contentPadding = PaddingValues(bottom = 136.dp),
        verticalArrangement = Arrangement.spacedBy(23.dp)
    ) {
        item {
            MobileHero(
                state.heroMovie,
                onDetails = { state.heroMovie?.let { onDetail("movie", it.slug) } },
                onPlay = { state.heroMovie?.let { onPlay("movie", it.id, it.title, it.fullPosterUrl, it.fullBackdropUrl, it.slug, null, null) } }
            )
        }
        if (state.error != null) item { MobileEmpty(state.error.orEmpty(), onRetry = { viewModel.loadMoviesData() }) }
        item { MobileMovieRow("Terpopuler", state.popularMovies) { onDetail("movie", it.slug) } }
        item { MobileMovieRow("Terbaru", state.latestMovies) { onDetail("movie", it.slug) } }
        item { MobileMovieRow("Aksi", state.actionMovies) { onDetail("movie", it.slug) } }
        item { MobileMovieRow("Komedi", state.comedyMovies) { onDetail("movie", it.slug) } }
        item { MobileMovieRow("Horor", state.horrorMovies) { onDetail("movie", it.slug) } }
        item { MobileMovieRow("Sci-Fi", state.sciFiMovies) { onDetail("movie", it.slug) } }
        items(state.extraShelves, key = { it.title }) { shelf ->
            MobileMovieRow(shelf.title, shelf.items) { onDetail(it.contentType, it.slug) }
        }
    }
}

@Composable
fun MobileSeriesScreen(
    onBrowse: OpenBrowse,
    onDetail: (String, String) -> Unit,
    viewModel: SeriesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    if (state.isLoading && state.heroSeries == null) { MobileLoading(); return }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NetflixBlack),
        contentPadding = PaddingValues(bottom = 136.dp),
        verticalArrangement = Arrangement.spacedBy(23.dp)
    ) {
        item {
            MobileHero(
                state.heroSeries,
                onDetails = { state.heroSeries?.let { onDetail("tv_series", it.slug) } },
                onPlay = { state.heroSeries?.let { onDetail("tv_series", it.slug) } }
            )
        }
        if (state.error != null) item { MobileEmpty(state.error.orEmpty(), onRetry = { viewModel.loadSeriesData() }) }
        item { MobileMovieRow("Serial Populer", state.popularSeries) { onDetail("tv_series", it.slug) } }
        item { MobileMovieRow("Serial Terbaru", state.latestSeries) { onDetail("tv_series", it.slug) } }
        item { MobileMovieRow("Drama", state.dramaSeries) { onDetail("tv_series", it.slug) } }
        item { MobileMovieRow("Aksi", state.actionSeries) { onDetail("tv_series", it.slug) } }
        item { MobileMovieRow("Komedi", state.comedySeries) { onDetail("tv_series", it.slug) } }
        items(state.extraShelves, key = { it.title }) { shelf ->
            MobileMovieRow(shelf.title, shelf.items) { onDetail(it.contentType, it.slug) }
        }
    }
}

@Composable
fun MobileSearchScreen(
    onDetail: (String, String) -> Unit,
    onCollection: (String) -> Unit = {},
    initialQuery: String = "",
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val savedCollections by SavedCollectionsStore.saved.collectAsState()
    var collectionFilter by rememberSaveable { mutableStateOf("all") }
    val siteCollectionChoices = state.collections
        .filter { collectionFilter != "world" && (collectionFilter != "saved" || it.slug in savedCollections) }
        .sortedByDescending { it.slug in savedCollections }
    val curatedCollectionChoices = CinemetaCollectionSource.collections
        .filter { collectionFilter != "idlix" && (collectionFilter != "saved" || it.slug in savedCollections) }
        .sortedByDescending { it.slug in savedCollections }
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) viewModel.onQueryChange(initialQuery)
    }
    LaunchedEffect(state.query.isBlank()) {
        if (state.query.isBlank()) viewModel.loadCollections()
    }
    var filterType by rememberSaveable { mutableStateOf("all") }
    var sort by rememberSaveable { mutableStateOf("relevance") }
    val visibleResults = remember(state.results, filterType, sort) {
        val filtered = state.results.filter { item ->
            when (filterType) {
                "movie" -> item.contentType.equals("movie", true)
                "series" -> item.contentType.equals("tv_series", true) ||
                    item.contentType.equals("series", true)
                else -> true
            }
        }
        when (sort) {
            "rating" -> filtered.sortedByDescending { it.voteAverage?.toDoubleOrNull() ?: 0.0 }
            "az" -> filtered.sortedBy { it.title.lowercase() }
            "newest" -> filtered.sortedByDescending { it.releaseDate ?: it.firstAirDate.orEmpty() }
            else -> filtered
        }
    }
    Column(
        Modifier.fillMaxSize().background(NetflixBlack)
            .statusBarsPadding().padding(top = 80.dp)
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            label = { Text("Cari film atau serial") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
        if (state.query.isBlank()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(listOf("all" to "Semua", "saved" to "Tersimpan", "idlix" to "IDLIX", "world" to "Kurasi luar")) { (id, label) ->
                    MobileFilterChip(label = label, selected = collectionFilter == id, onClick = { collectionFilter = id })
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(listOf("all" to "Semua", "movie" to "Film", "series" to "Serial")) { (id, name) ->
                    MobileFilterChip(label = name, selected = filterType == id, onClick = { filterType = id })
                }
                items(listOf("relevance" to "Relevansi", "newest" to "Terbaru", "rating" to "Rating", "az" to "A–Z")) { (id, name) ->
                    MobileFilterChip(label = name, selected = sort == id, onClick = { sort = id })
                }
            }
        }
        when {
            state.isLoading -> MobileLoading("Mencari judul…")
            state.error != null -> MobileEmpty(state.error.orEmpty())
            state.query.isBlank() -> LazyVerticalGrid(
                columns = GridCells.Adaptive(162.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 130.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (collectionFilter != "world") item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Koleksi IDLIX", color = NetflixWhite, fontWeight = FontWeight.Bold, fontSize = 23.sp)
                        Text(
                            if (state.collectionsFromWebsite) "Koleksi dari halaman IDLIX"
                            else "Koleksi referensi · situs belum bisa dijangkau",
                            color = NetflixLightGray, fontSize = 12.sp
                        )
                    }
                }
                if (collectionFilter != "world" && state.collectionsLoading && state.collections.isEmpty()) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        MobileLoading("Memuat koleksi IDLIX…")
                    }
                }
                gridItems(siteCollectionChoices, key = { it.slug }) { collection ->
                    MobilePosterCard(
                        title = collection.title,
                        poster = collection.imageUrl.orEmpty(),
                        rating = null,
                        quality = if (collection.slug in savedCollections) "Disimpan"
                            else collection.count?.let { it.toString() + " judul" },
                        onClick = { onCollection(collection.slug) },
                        fullWidth = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (collectionFilter != "idlix") item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Kurasi dunia", color = NetflixWhite, fontWeight = FontWeight.Bold, fontSize = 23.sp)
                        Text(
                            "Metadata Cinemeta · hanya judul yang cocok di katalog IDLIX dapat dibuka",
                            color = NetflixLightGray, fontSize = 12.sp
                        )
                    }
                }
                gridItems(curatedCollectionChoices, key = { it.slug }) { collection ->
                    MobileCuratedCollectionCard(
                        title = collection.title,
                        saved = collection.slug in savedCollections,
                        onClick = { onCollection(collection.slug) }
                    )
                }
                if (collectionFilter == "saved" && !state.collectionsLoading && siteCollectionChoices.isEmpty() && curatedCollectionChoices.isEmpty()) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        MobileEmpty("Belum ada koleksi tersimpan. Buka koleksi lalu pilih Simpan.")
                    }
                }
            }
            visibleResults.isEmpty() -> MobileEmpty("Belum menemukan judul yang cocok.")
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(162.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 130.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                gridItems(visibleResults, key = { "${it.contentType}:${it.id}" }) { item ->
                    MobilePosterCard(
                        title = item.title,
                        poster = item.fullPosterUrl,
                        rating = item.formattedRating,
                        quality = item.sourceBadge,
                        onClick = { onDetail(item.contentType, item.slug) },
                        fullWidth = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileCuratedCollectionCard(title: String, saved: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(215.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(NetflixDarkCard)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            if (saved) "TERSIMPAN  /  CINEMETA" else "CINEMETA  /  KURASI",
            color = NetflixLightGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(androidx.compose.ui.Alignment.TopStart)
        )
        Column(
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(title, color = NetflixWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
            Text("Lihat di IDLIX", color = NetflixLightGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun MobileExploreScreen(
    onDetail: (String, String) -> Unit,
    initialSection: String = "genre",
    initialValue: String = "",
    initialType: String = "all"
) {
    val repository = remember { IdlixRepository() }
    val savedCollections by SavedCollectionsStore.saved.collectAsState()
    var genres by remember { mutableStateOf<List<Genre>>(emptyList()) }
    var networks by remember { mutableStateOf<List<BrowseChoice>>(emptyList()) }
    var section by rememberSaveable(initialSection) { mutableStateOf(initialSection) }
    var value by rememberSaveable(initialSection, initialValue) {
        mutableStateOf(initialValue.ifBlank { choicesFor(initialSection).firstOrNull()?.value.orEmpty() })
    }
    var sort by rememberSaveable { mutableStateOf(if (initialSection == "collections") "source" else "newest") }
    var filterType by rememberSaveable(initialType) {
        mutableStateOf(initialType.takeIf { it in listOf("all", "movie", "series") } ?: "all")
    }
    var retryTick by remember { mutableIntStateOf(0) }
    var scanPages by rememberSaveable(section, value) { mutableIntStateOf(4) }
    var results by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository, section) {
        when (section) {
            "genre" -> genres = repository.getAvailableGenres()
            "network" -> networks = repository.getAvailableNetworks()
                .filter { it.slug.isNotBlank() && it.name.isNotBlank() }
                .map { BrowseChoice(it.name, it.slug) }
        }
    }
    LaunchedEffect(section, value, retryTick, scanPages) {
        loading = true
        error = null
        try {
            results = repository.browseCatalog(section, value, maxPages = scanPages)
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Katalog gagal dimuat"
            results = emptyList()
        } finally {
            loading = false
        }
    }

    val sections = catalogSections.map { it.value to it.label }
    val choices: List<Pair<String, String>> = if (section == "genre") {
        (genres.filter { it.slug.isNotBlank() }.map { BrowseChoice(it.name, it.slug) } +
            choicesFor("genre"))
            .distinctBy { it.value.lowercase() }
            .sortedBy { it.label }
            .map { it.value to it.label }
    } else if (section == "network") {
        (networks + choicesFor(section)).distinctBy { it.value.lowercase() }
            .map { it.value to it.label }
    } else {
        choicesFor(section).map { it.value to it.label }
    }
    val shown = remember(results, sort, filterType) { sortedItems(results, sort, filterType) }
    val externalCollection = section == "collections" && CinemetaCollectionSource.isExternalCollection(value)
    val moreExternal = externalCollection && scanPages < 24
    Column(
        Modifier.fillMaxSize().background(NetflixBlack)
            .statusBarsPadding().padding(top = 80.dp)
    ) {
        if (section == "collections") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 6.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Isi Koleksi", color = NetflixWhite,
                    fontWeight = FontWeight.Bold, fontSize = 25.sp)
                TextButton(onClick = { SavedCollectionsStore.toggle(initialValue) }) {
                    Text(
                        if (initialValue in savedCollections) "Tersimpan ✓" else "Simpan",
                        color = NetflixWhite, fontSize = 13.sp
                    )
                }
            }
            Text(
                CinemetaCollectionSource.collectionTitle(initialValue)
                    ?: com.idlix.tv.data.repository.SiteCatalogReferences.collections.firstOrNull { it.slug == initialValue }?.title
                    ?: initialValue.split('-').joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) },
                color = NetflixLightGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
            if (CinemetaCollectionSource.isExternalCollection(initialValue)) {
                Text(
                    "Kurasi dari Cinemeta · hanya judul yang cocok di IDLIX yang ditampilkan",
                    color = NetflixLightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                )
            }
        } else {
        MobileExploreControls(
            section = section,
            value = value,
            sort = sort,
            filterType = filterType,
            sections = sections,
            choices = choices,
            onSectionSelected = { selected ->
                section = selected
                value = choicesFor(selected).firstOrNull()?.value.orEmpty()
            },
            onChoiceSelected = { value = it },
            onSortSelected = { sort = it },
            onTypeSelected = { filterType = it }
        )

        }
        when {
            loading -> MobileLoading()
            error != null -> MobileEmpty(error.orEmpty(), onRetry = { retryTick++ })
            shown.isEmpty() -> MobileEmpty(
                (if (CinemetaCollectionSource.isExternalCollection(initialValue))
                    "Belum ada kecocokan judul dan tahun di IDLIX, atau sumber metadata sedang tidak tersedia."
                else if (section == "network") "Belum ada judul platform ini di katalog yang tersedia."
                else "Belum ada judul yang cocok dalam katalog yang termuat.") +
                    if (section != "collections" && scanPages < 48) " Coba telusuri lebih banyak." else "",
                onRetry = if (moreExternal) ({ scanPages = (scanPages + 4).coerceAtMost(24) })
                else if (section == "collections") ({ retryTick++ })
                else if (scanPages < 48) ({ scanPages = (scanPages + 4).coerceAtMost(48) })
                else null,
                actionLabel = if (moreExternal) "Cari 18 judul berikutnya"
                    else if (section == "collections") "Coba lagi" else "Muat judul lainnya"
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(162.dp),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 130.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                gridItems(shown, key = { "${it.contentType}:${it.id}" }) { item ->
                    MobilePosterCard(
                        title = item.title,
                        poster = item.fullPosterUrl,
                        rating = item.formattedRating,
                        quality = item.sourceBadge,
                        imageReferer = if (section == "network") SiteNetworkSource.networkPageUrl(value) else null,
                        onClick = { onDetail(item.contentType, item.slug) },
                        fullWidth = true
                    )
                }
                if (moreExternal || (section != "collections" && scanPages < 48)) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        androidx.compose.material3.TextButton(
                            onClick = { scanPages = (scanPages + 4).coerceAtMost(if (externalCollection) 24 else 48) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (externalCollection) "Cari 18 judul berikutnya" else "Muat lebih banyak judul", color = NetflixWhite) }
                    }
                }
            }
        }
    }
}

@Composable
fun MobileHistoryScreen(
    onPlay: MobilePlay,
    viewModel: ContinueWatchingViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { viewModel.loadHistory() }
    if (pendingDelete != null) {
        val isClearAll = pendingDelete == "__clear_all__"
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (isClearAll) "Hapus semua riwayat?" else "Hapus dari riwayat?") },
            text = { Text(if (isClearAll) "Seluruh progres lokal akan dihapus."
                          else "Progres tontonan ini akan dihapus dari perangkat.") },
            confirmButton = {
                TextButton(onClick = {
                    if (isClearAll) viewModel.clearAll()
                    else pendingDelete?.let(viewModel::removeItem)
                    pendingDelete = null
                }) { Text("Hapus", color = NetflixRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Batal") }
            }
        )
    }
    if (state.items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(NetflixBlack)
                .statusBarsPadding().padding(top = 82.dp)
        ) {
            MobileEmpty("Riwayat menontonmu masih kosong.")
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NetflixBlack)
            .statusBarsPadding().padding(top = 76.dp),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 134.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Lanjutkan Menonton", color = NetflixWhite, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold)
                TextButton(onClick = { pendingDelete = "__clear_all__" }) {
                    Text("Hapus Semua", color = NetflixRed)
                }
            }
        }
        items(state.items, key = { it.contentId }) { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MobilePosterCard(
                    title = item.title,
                    poster = item.resolvedThumbnail,
                    rating = null,
                    progressLabel = "${item.progressPercent}%",
                    onClick = {
                        onPlay(item.contentType, item.contentId, item.title,
                            item.posterUrl, item.backdropUrl, item.slug,
                            item.seasonNumber, item.episodeNumber)
                    }
                )
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = NetflixWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("${item.progressPercent}% ditonton", fontSize = 12.sp, color = NetflixLightGray)
                    IconButton(onClick = { pendingDelete = item.contentId }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus riwayat", tint = NetflixRed)
                    }
                }
            }
        }
    }
}
