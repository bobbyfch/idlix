package com.idlix.tv.ui.screens.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as columnItems
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.idlix.tv.data.model.MovieItem
import com.idlix.tv.data.local.TvPreferencesStore
import com.idlix.tv.ui.components.glassSurface
import com.idlix.tv.data.repository.IdlixRepository
import com.idlix.tv.data.repository.CinemetaCollectionSource
import com.idlix.tv.data.local.SavedCollectionsStore
import com.idlix.tv.ui.components.TvLoadingSpinner
import com.idlix.tv.ui.components.TvMovieCard
import com.idlix.tv.ui.components.NetworkBrandLogo
import com.idlix.tv.data.repository.SiteNetworkSource
import com.idlix.tv.ui.theme.NetflixBlack
import com.idlix.tv.ui.theme.NetflixDarkCard
import com.idlix.tv.ui.theme.NetflixLightGray
import com.idlix.tv.ui.theme.NetflixWhite
import com.idlix.tv.ui.theme.NetflixDarkGray
import com.idlix.tv.ui.theme.NetflixRed
import com.idlix.tv.ui.theme.IdlixPalette

data class BrowseChoice(val label: String, val value: String)

/** Shared with mobile Explore; one canonical section taxonomy. */
val catalogSections = listOf(
    BrowseChoice("Genre", "genre"),
    BrowseChoice("Negara", "country"),
    BrowseChoice("Tahun", "year"),
    BrowseChoice("Network", "network"),
    BrowseChoice("Shorts", "shorts")
)

fun choicesFor(section: String): List<BrowseChoice> = when (section) {
    "genre" -> listOf(
        "Action" to "action", "Adventure" to "adventure", "Animation" to "animation",
        "Comedy" to "comedy", "Crime" to "crime", "Documentary" to "documentary",
        "Drama" to "drama", "Family" to "family", "Fantasy" to "fantasy",
        "History" to "history", "Horror" to "horror", "Kids" to "kids",
        "Music" to "music", "Mystery" to "mystery", "Reality" to "reality",
        "Romance" to "romance", "Science Fiction" to "science-fiction",
        "Soap" to "soap", "Talk" to "talk", "Thriller" to "thriller",
        "TV Movie" to "tv-movie", "War" to "war", "Western" to "western"
    ).map { BrowseChoice(it.first, it.second) }
    "country" -> listOf(
        "Indonesia" to "ID", "Korea Selatan" to "KR", "Jepang" to "JP",
        "Amerika Serikat" to "US", "Inggris" to "GB", "China" to "CN",
        "Thailand" to "TH", "India" to "IN", "Taiwan" to "TW",
        "Malaysia" to "MY", "Prancis" to "FR", "Jerman" to "DE",
        "Australia" to "AU", "Kanada" to "CA", "Argentina" to "AR",
        "Austria" to "AT", "Belgia" to "BE", "Brasil" to "BR",
        "Bulgaria" to "BG", "Kamboja" to "KH", "Chile" to "CL",
        "Kolombia" to "CO", "Kuba" to "CU", "Siprus" to "CY",
        "Ceko" to "CZ", "Denmark" to "DK", "Mesir" to "EG",
        "Finlandia" to "FI", "Georgia" to "GE", "Yunani" to "GR",
        "Hong Kong" to "HK", "Hungaria" to "HU", "Islandia" to "IS",
        "Iran" to "IR", "Irak" to "IQ", "Irlandia" to "IE",
        "Israel" to "IL", "Italia" to "IT", "Meksiko" to "MX",
        "Maroko" to "MA", "Belanda" to "NL", "Selandia Baru" to "NZ",
        "Nigeria" to "NG", "Norwegia" to "NO", "Pakistan" to "PK",
        "Filipina" to "PH", "Polandia" to "PL", "Portugal" to "PT",
        "Rumania" to "RO", "Rusia" to "RU", "Arab Saudi" to "SA",
        "Singapura" to "SG", "Afrika Selatan" to "ZA",
        "Spanyol" to "ES", "Swedia" to "SE", "Swiss" to "CH",
        "Turki" to "TR", "Ukraina" to "UA", "Vietnam" to "VN",
        "Republik Dominika" to "DO", "Ekuador" to "EC",
        "El Salvador" to "SV", "Estonia" to "EE", "Guatemala" to "GT",
        "Yordania" to "JO", "Kazakhstan" to "KZ", "Kenya" to "KE",
        "Kuwait" to "KW", "Latvia" to "LV", "Lebanon" to "LB",
        "Lituania" to "LT", "Luksemburg" to "LU", "Nepal" to "NP",
        "Peru" to "PE", "Serbia" to "RS", "Slovakia" to "SK",
        "Slovenia" to "SI", "Uni Emirat Arab" to "AE",
        "Uruguay" to "UY"
    ).map { BrowseChoice(it.first, it.second) }
    "year" -> (java.time.Year.now().value downTo 1940).map { BrowseChoice(it.toString(), it.toString()) }
    // Verified choices from the public IDLIX /network page (Nov 2026).
    // Availability is based on real provider metadata, not label inference.
    "network" -> listOf(
        "Netflix" to "netflix", "HBO / Max" to "hbo",
        "Prime Video" to "prime-video", "Disney+" to "disney-plus",
        "Apple TV+" to "apple-tv-plus", "Tencent Video" to "tencent-video",
        "tvN" to "tvn", "iQIYI" to "iqiyi",
        "SBS" to "sbs", "MBC" to "mbc",
        "TV Tokyo" to "tv-tokyo", "KBS" to "kbs",
        "JTBC" to "jtbc", "Fuji TV" to "fuji-tv",
        "Youku" to "youku", "NBC" to "nbc",
        "TVING" to "tving", "Paramount+" to "paramount-plus",
        "Mango TV" to "mango-tv", "BBC" to "bbc",
        "Wavve" to "wavve", "FOX" to "fox",
        "Peacock" to "peacock", "CBS" to "cbs",
        "YouTube" to "youtube", "bilibili" to "bilibili",
        "Showtime" to "showtime", "Cartoon Network" to "cartoon-network",
        "STARZ" to "starz", "Sky" to "sky",
        "Channel 4" to "channel-4", "Crunchyroll" to "crunchyroll"
    ).map { BrowseChoice(it.first, it.second) }
    else -> emptyList()
}

private fun titleFor(section: String): String = when (section) {
    "genre" -> "Jelajah Genre"
    "country" -> "Jelajah Negara"
    "year" -> "Jelajah Tahun"
    "network" -> "Jelajah Network"
    "shorts" -> "Shorts"
    else -> "Jelajah Katalog"
}

private val sortChoices = listOf(
    BrowseChoice("Urutan koleksi", "source"),
    BrowseChoice("Terbaru", "newest"),
    BrowseChoice("Terpopuler", "popular"),
    BrowseChoice("Rating", "rating"),
    BrowseChoice("Judul A–Z", "az"),
    BrowseChoice("Terlama", "oldest")
)

private val typeChoices = listOf(
    BrowseChoice("Semua", "all"),
    BrowseChoice("Film", "movie"),
    BrowseChoice("Serial TV", "series")
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CatalogBrowseScreen(
    section: String,
    onNavigateToDetail: (contentType: String, slug: String) -> Unit,
    onSelectSection: (String) -> Unit = {},
    initialValue: String = "",
    initialType: String = "all",
    modifier: Modifier = Modifier
) {
    val repository = remember { IdlixRepository() }
    val preferences by TvPreferencesStore.preferences.collectAsState()
    val savedCollections by SavedCollectionsStore.saved.collectAsState()
    var liveGenres by remember(section) { mutableStateOf<List<BrowseChoice>>(emptyList()) }
    var liveNetworks by remember(section) { mutableStateOf<List<BrowseChoice>>(emptyList()) }
    val fallbackChoices = remember(section) { choicesFor(section) }
    // The public web exposes 23 genres; keep the full reference taxonomy
    // even when /api/genres returns a partial list.
    val choices = when (section) {
        "genre" -> (liveGenres + fallbackChoices).distinctBy { it.value.lowercase() }
        "network" -> (liveNetworks + fallbackChoices).distinctBy { it.value.lowercase() }
        else -> fallbackChoices
    }
    var selected by remember(section, initialValue) {
        mutableStateOf(initialValue.ifBlank { choices.firstOrNull()?.value.orEmpty() })
    }
    var sort by remember(section) { mutableStateOf(if (section == "collections") "source" else "newest") }
    var contentType by remember(section, initialType) {
        mutableStateOf(initialType.takeIf { it in setOf("all", "movie", "series") } ?: "all")
    }
    var results by remember(section) { mutableStateOf<List<MovieItem>>(emptyList()) }
    var loading by remember(section) { mutableStateOf(section != "explore") }
    var retryKey by remember(section) { mutableStateOf(0) }
    var scanPages by remember(section, selected) { mutableStateOf(4) }
    val externalCollection = section == "collections" && CinemetaCollectionSource.isExternalCollection(initialValue)
    val moreExternal = externalCollection && scanPages < 24
    val exploreFocusRequester = remember(section) { FocusRequester() }

    LaunchedEffect(section) {
        if (section == "explore") {
            try { exploreFocusRequester.requestFocus() } catch (_: Exception) { }
        }
    }

    LaunchedEffect(section) {
        when (section) {
            "genre" -> liveGenres = repository.getAvailableGenres()
                .filter { it.slug.isNotBlank() && it.name.isNotBlank() }
                .distinctBy { it.slug }
                .sortedBy { it.name }
                .map { BrowseChoice(it.name, it.slug) }
            "network" -> liveNetworks = repository.getAvailableNetworks()
                .filter { it.slug.isNotBlank() && it.name.isNotBlank() }
                .map { BrowseChoice(it.name, it.slug) }
        }
    }
    LaunchedEffect(choices) {
        if (choices.isNotEmpty() && choices.none { it.value == selected }) {
            selected = choices.first().value
        }
    }
    LaunchedEffect(section, selected, retryKey, scanPages) {
        if (section != "explore") {
            loading = true
            results = emptyList()
            results = try {
                repository.browseCatalog(section, selected, maxPages = scanPages)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
            loading = false
        }
    }

    val displayItems = remember(results, sort, contentType) {
        sortedItems(results, sort, contentType)
    }

    Column(
        modifier = modifier.fillMaxSize().background(NetflixBlack)
            .padding(start = 28.dp, end = 32.dp, top = 24.dp, bottom = 22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (section == "collections")
                    CinemetaCollectionSource.collectionTitle(initialValue)
                        ?: com.idlix.tv.data.repository.SiteCatalogReferences.collections
                            .firstOrNull { it.slug == initialValue }?.title
                        ?: initialValue.replace('-', ' ').replaceFirstChar { it.uppercaseChar() }
                else if (section == "network")
                    choices.firstOrNull { it.value == selected }?.label?.plus(" Originals") ?: titleFor(section)
                else titleFor(section),
                color = NetflixWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold
            )
            if (section == "collections" && initialValue.isNotBlank()) {
                Button(
                    onClick = { SavedCollectionsStore.toggle(initialValue) },
                    colors = ButtonDefaults.colors(
                        containerColor = NetflixDarkCard,
                        contentColor = NetflixWhite,
                        focusedContainerColor = NetflixRed
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp))
                ) {
                    Text(if (initialValue in savedCollections) "Tersimpan ✓" else "Simpan Koleksi",
                        fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            when (section) {
                "explore" -> "Pilih kategori dengan tombol arah dan OK pada remote TV."
                "network" -> "Film dan serial dari halaman " +
                    (choices.firstOrNull { it.value == selected }?.label ?: "network") + " di IDLIX."
                "shorts" -> "Film pendek berdasarkan metadata durasi dan penanda dari katalog."
                "collections" -> if (CinemetaCollectionSource.isExternalCollection(initialValue))
                    "Kurasi Cinemeta · hanya hasil yang benar-benar cocok di IDLIX yang ditampilkan."
                    else "Judul sesuai urutan di halaman koleksi IDLIX."
                else -> "Katalog yang termuat dari sumber. Gunakan arah kiri/kanan untuk berpindah panel."
            },
            color = NetflixLightGray, fontSize = 15.sp
        )
        Spacer(Modifier.height(22.dp))
        if (section == "explore") {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                gridItems(catalogSections, key = { it.value }) { choice ->
                    Card(
                        onClick = { onSelectSection(choice.value) },
                        modifier = Modifier.fillMaxWidth().height(148.dp)
                            .then(if (choice.value == catalogSections.first().value) {
                                Modifier.focusRequester(exploreFocusRequester)
                            } else Modifier),
                        shape = CardDefaults.shape(shape = RoundedCornerShape(22.dp)),
                        colors = CardDefaults.colors(
                            containerColor = NetflixDarkCard,
                            focusedContainerColor = NetflixDarkGray
                        ),
                        scale = CardDefaults.scale(focusedScale = 1.045f)
                    ) {
                        Box(
                            Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(choice.label, color = NetflixWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                if (choices.isNotEmpty()) {
                    Column(
                        modifier = Modifier.width(242.dp).fillMaxHeight()
                            .glassSurface(preferences.glassIntensity, RoundedCornerShape(24.dp))
                            .padding(horizontal = 12.dp, vertical = 16.dp)
                    ) {
                        Text(
                            when (section) {
                                "network" -> "Pilih network"
                                else -> "Pilih kategori"
                            },
                            color = NetflixLightGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 14.dp, bottom = 10.dp)
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            columnItems(choices, key = { it.value }) { choice ->
                                val active = selected == choice.value
                                Button(
                                    onClick = { selected = choice.value },
                                    modifier = Modifier.fillMaxWidth().height(45.dp),
                                    colors = ButtonDefaults.colors(
                                        containerColor = if (active) NetflixWhite.copy(alpha = if (IdlixPalette.isLight) 0.10f else 0.20f) else Color.Transparent,
                                        contentColor = if (active) NetflixWhite else NetflixLightGray,
                                        focusedContainerColor = NetflixWhite.copy(alpha = if (IdlixPalette.isLight) 0.14f else 0.30f),
                                        focusedContentColor = NetflixWhite
                                    ),
                                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(14.dp)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    if (section == "network") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                                        ) {
                                            NetworkBrandLogo(
                                                slug = choice.value,
                                                label = choice.label,
                                                modifier = Modifier.width(50.dp).height(22.dp)
                                            )
                                            Text(
                                                choice.label,
                                                fontSize = 14.sp,
                                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        }
                                    } else {
                                        Text(
                                            choice.label,
                                            fontSize = 14.sp,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    Text("Urutkan", color = NetflixLightGray, fontSize = 13.sp)
                    Spacer(Modifier.height(7.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 15.dp, bottom = 6.dp)
                    ) {
                        rowItems(sortChoices, key = { it.value }) { choice ->
                            val active = sort == choice.value
                            Button(
                                onClick = { sort = choice.value },
                                colors = ButtonDefaults.colors(
                                    containerColor = if (active) NetflixWhite.copy(alpha = if (IdlixPalette.isLight) 0.10f else 0.20f) else NetflixDarkCard,
                                    contentColor = NetflixWhite,
                                    focusedContainerColor = NetflixWhite.copy(alpha = if (IdlixPalette.isLight) 0.16f else 0.32f),
                                    focusedContentColor = NetflixWhite
                                ),
                                shape = ButtonDefaults.shape(shape = RoundedCornerShape(14.dp)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp)
                            ) {
                                Text(choice.label, fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 15.dp, bottom = 8.dp)
                    ) {
                        rowItems(typeChoices, key = { it.value }) { choice ->
                            val active = contentType == choice.value
                            Button(
                                onClick = { contentType = choice.value },
                                colors = ButtonDefaults.colors(
                                    containerColor = if (active) NetflixRed.copy(alpha = 0.26f) else Color.Transparent,
                                    contentColor = if (active) NetflixWhite else NetflixLightGray,
                                    focusedContainerColor = NetflixRed,
                                    focusedContentColor = NetflixWhite
                                ),
                                shape = ButtonDefaults.shape(shape = RoundedCornerShape(13.dp)),
                                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 6.dp)
                            ) {
                                Text(choice.label, fontSize = 12.sp)
                            }
                        }
                    }
                    Text(
                        if (loading) "Memuat katalog..." else "${displayItems.size} judul ditemukan dalam katalog yang termuat",
                        color = NetflixLightGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )
                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            loading -> TvLoadingSpinner(modifier = Modifier.align(Alignment.Center))
                            displayItems.isEmpty() -> Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    if (CinemetaCollectionSource.isExternalCollection(initialValue))
                                        "Belum menemukan judul dengan nama, tahun dan tipe yang cocok di IDLIX."
                                    else "Belum ada hasil yang cocok. Pilih kategori atau filter lain.",
                                    color = NetflixLightGray,
                                    fontSize = 17.sp
                                )
                                Button(
                                    onClick = {
                                        if (moreExternal) scanPages = (scanPages + 4).coerceAtMost(24)
                                        else if (section == "collections") retryKey++
                                        else if (scanPages < 48) scanPages = (scanPages + 4).coerceAtMost(48)
                                        else retryKey++
                                    },
                                    colors = ButtonDefaults.colors(
                                        containerColor = NetflixDarkCard,
                                        contentColor = NetflixWhite,
                                        focusedContainerColor = NetflixRed
                                    ),
                                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(14.dp))
                                ) { Text(if (moreExternal) "Cari 18 judul berikutnya" else "Coba muat lagi", fontSize = 14.sp) }
                            }
                            else -> LazyVerticalGrid(
                                columns = GridCells.Adaptive(170.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                gridItems(displayItems, key = { "${it.contentType}-${it.id}-${it.slug}" }) { movie ->
                                    TvMovieCard(
                                        title = movie.title,
                                        posterUrl = movie.fullPosterUrl,
                                        rating = movie.formattedRating,
                                        quality = movie.sourceBadge,
                                        imageReferer = if (section == "network") SiteNetworkSource.networkPageUrl(selected) else null,
                                        onClick = { onNavigateToDetail(movie.contentType, movie.slug) }
                                    )
                                }
                                if (moreExternal) {
                                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                        Button(
                                            onClick = { scanPages = (scanPages + 4).coerceAtMost(24) },
                                            colors = ButtonDefaults.colors(
                                                containerColor = NetflixDarkCard,
                                                contentColor = NetflixWhite,
                                                focusedContainerColor = NetflixRed
                                            ),
                                            shape = ButtonDefaults.shape(shape = RoundedCornerShape(14.dp))
                                        ) { Text("Cari 18 judul berikutnya", fontSize = 14.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
