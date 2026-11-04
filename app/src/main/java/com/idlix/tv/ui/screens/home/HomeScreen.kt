package com.idlix.tv.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.idlix.tv.R
import com.idlix.tv.data.model.MovieItem
import com.idlix.tv.data.repository.SiteCatalogReferences
import com.idlix.tv.ui.components.TvContinueWatchingRow
import com.idlix.tv.ui.components.TvCatalogGateway
import com.idlix.tv.ui.components.TvHeroBillboard
import com.idlix.tv.ui.components.TvLoadingSpinner
import com.idlix.tv.ui.components.TvSwimlaneRow
import com.idlix.tv.ui.theme.NetflixBlack
import com.idlix.tv.ui.theme.NetflixWhite

@Composable
private fun TvNetworkOriginalsRow(onBrowse: (String, String, String) -> Unit) {
    val featuredNetworks = SiteCatalogReferences.networkProviders.take(5)
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Network Originals",
                color = NetflixWhite,
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Button(onClick = { onBrowse("network", "", "all") }) {
                Text("Lihat semua")
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        ) {
            rowItems(featuredNetworks, key = { it.slug }) { network ->
                Button(onClick = { onBrowse("network", network.slug, "all") }) {
                    Text(network.name)
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    onNavigateToBrowse: (section: String, value: String, type: String) -> Unit,
    onNavigateToDetail: (contentType: String, slug: String) -> Unit,
    onNavigateToPlayer: (contentType: String, contentId: String, title: String, poster: String?, backdrop: String?, slug: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val heroFocusRequester = remember { FocusRequester() }

    // Refresh continue watching list on screen entrance / return
    LaunchedEffect(Unit) {
        viewModel.refreshContinueWatching()
    }

    // Direct initial focus to Hero billboard content as soon as loaded
    LaunchedEffect(uiState.isLoading, uiState.heroMovie) {
        if (!uiState.isLoading && uiState.heroMovie != null) {
            try {
                heroFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        if (uiState.isLoading) {
            TvLoadingSpinner(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.error != null) {
            Text(
                text = uiState.error ?: "",
                color = NetflixWhite,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Dynamic Hero Billboard
                item {
                    TvHeroBillboard(
                        movie = uiState.heroMovie,
                        playButtonModifier = Modifier.focusRequester(heroFocusRequester),
                        onPlayClick = {
                            uiState.heroMovie?.let {
                                if (it.contentType == "tv_series") {
                                    onNavigateToDetail(it.contentType, it.slug)
                                } else {
                                    onNavigateToPlayer(
                                        it.contentType,
                                        it.id,
                                        it.title,
                                        it.fullPosterUrl,
                                        it.fullBackdropUrl,
                                        it.slug
                                    )
                                }
                            }
                        },
                        onDetailsClick = {
                            uiState.heroMovie?.let {
                                onNavigateToDetail(it.contentType, it.slug)
                            }
                        }
                    )
                }

                // 2. Bar Paling Pertama: Lanjutkan Menonton (hanya muncul jika ada histori yang belum kelar)
                if (uiState.continueWatching.isNotEmpty()) {
                    item {
                        TvContinueWatchingRow(
                            title = stringResource(R.string.continue_watching),
                            items = uiState.continueWatching,
                            onItemClick = { item ->
                                onNavigateToPlayer(
                                    item.contentType,
                                    item.contentId,
                                    item.title,
                                    item.posterUrl,
                                    item.backdropUrl,
                                    item.slug
                                )
                            }
                        )
                    }
                }
                item { TvNetworkOriginalsRow(onBrowse = onNavigateToBrowse) }

                // 3. Swimlane: Sedang Tren Sekarang
                item {
                    TvSwimlaneRow(
                        title = stringResource(R.string.trending_now),
                        items = uiState.trending,
                        onItemClick = { onNavigateToDetail(it.contentType, it.slug) }
                    )
                }

                if (uiState.trendingCountry.isNotEmpty()) {
                    item {
                        TvSwimlaneRow(
                            title = "Trending Indonesia",
                            items = uiState.trendingCountry,
                            onItemClick = { onNavigateToDetail(it.contentType, it.slug) }
                        )
                    }
                }
                if (uiState.networkPicks.isNotEmpty()) {
                    item {
                        TvSwimlaneRow(
                            title = "Pilihan Network",
                            items = uiState.networkPicks,
                            onItemClick = { onNavigateToDetail(it.contentType, it.slug) }
                        )
                    }
                }

                // 4. Swimlane: Film Terpopuler
                item {
                    TvSwimlaneRow(
                        title = stringResource(R.string.top_10_movies),
                        items = uiState.popularMovies,
                        onItemClick = { onNavigateToDetail(it.contentType, it.slug) }
                    )
                }

                // 5. Swimlane: Serial TV Pilihan
                item {
                    val seriesAsMovies = remember(uiState.popularSeries) {
                        uiState.popularSeries.map { s ->
                            MovieItem(
                                id = s.id,
                                title = s.title,
                                slug = s.slug,
                                posterPath = s.posterPath,
                                backdropPath = s.backdropPath,
                                voteAverage = s.voteAverage,
                                contentType = "tv_series"
                            )
                        }
                    }
                    TvSwimlaneRow(
                        title = stringResource(R.string.popular_series),
                        items = seriesAsMovies,
                        onItemClick = { onNavigateToDetail(it.contentType, it.slug) }
                    )
                }

                // Recently added rows use the existing catalog endpoints, like the web homepage.
                if (uiState.latestMovies.isNotEmpty()) {
                    item {
                        TvSwimlaneRow(
                            title = stringResource(R.string.latest_movies),
                            items = uiState.latestMovies,
                            onItemClick = { onNavigateToDetail(it.contentType, it.slug) }
                        )
                    }
                }

                if (uiState.latestSeries.isNotEmpty()) {
                    item {
                        TvSwimlaneRow(
                            title = stringResource(R.string.on_the_air_series),
                            items = uiState.latestSeries,
                            onItemClick = { onNavigateToDetail(it.contentType, it.slug) }
                        )
                    }
                }

                // Additional genre row: Action
                if (uiState.actionMovies.isNotEmpty()) {
                    item {
                        TvSwimlaneRow(
                            title = stringResource(R.string.movies_action),
                            items = uiState.actionMovies,
                            onItemClick = { onNavigateToDetail(it.contentType, it.slug) }
                        )
                    }
                }
                // Extra editorial rails are fetched progressively after initial
                // paint, rather than filling every tab with duplicate filters.
                items(uiState.extraShelves, key = { it.title }) { shelf ->
                    TvSwimlaneRow(
                        title = shelf.title,
                        items = shelf.items,
                        onItemClick = { onNavigateToDetail(it.contentType, it.slug) }
                    )
                }
            }
        }
    }
}
