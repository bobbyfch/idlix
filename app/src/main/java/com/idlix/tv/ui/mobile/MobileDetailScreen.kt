package com.idlix.tv.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.idlix.tv.data.model.Season
import com.idlix.tv.ui.components.MovieTitleArtwork
import com.idlix.tv.ui.screens.detail.DetailViewModel
import com.idlix.tv.ui.theme.NetflixRed
import com.idlix.tv.ui.theme.NetflixBlack
import com.idlix.tv.ui.theme.NetflixWhite
import com.idlix.tv.ui.theme.NetflixLightGray
import com.idlix.tv.ui.theme.NetflixDarkCard
import com.idlix.tv.ui.theme.IdlixPalette

@Composable
fun MobileDetailScreen(
    contentType: String,
    slug: String,
    onBack: () -> Unit,
    onCastClick: (String) -> Unit,
    onPlay: MobilePlay,
    viewModel: DetailViewModel = viewModel()
) {
    LaunchedEffect(contentType, slug) { viewModel.loadDetail(contentType, slug) }
    val state by viewModel.uiState.collectAsState()
    if (state.isLoading) {
        Column(Modifier.fillMaxSize()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = NetflixWhite)
            }
            MobileLoading("Memuat detail…")
        }
        return
    }
    if (state.error != null) {
        Column(Modifier.fillMaxSize()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = NetflixWhite)
            }
            MobileEmpty(state.error.orEmpty(), onRetry = { viewModel.loadDetail(contentType, slug) })
        }
        return
    }

    val movie = state.movie
    val series = state.series
    val title = movie?.title ?: series?.title.orEmpty()
    val backdrop = movie?.fullBackdropUrl ?: series?.fullBackdropUrl.orEmpty()
    val poster = movie?.fullPosterUrl ?: series?.fullPosterUrl.orEmpty()
    val cast = movie?.cast ?: series?.cast.orEmpty()
    val overview = movie?.overview ?: series?.overview.orEmpty()
    val year = movie?.releaseDate ?: series?.firstAirDate
    val genres = (movie?.genres ?: series?.genres.orEmpty()).joinToString(" · ") { it.name }
    val ratingValue = (movie?.voteAverage ?: series?.voteAverage)
        ?.toDoubleOrNull()?.takeIf { it > 0.0 && it <= 10.0 }
    val durationMinutes = movie?.runtime ?: movie?.duration
    val durationLabel = durationMinutes?.takeIf { it in 1..600 }?.let { mins ->
        if (mins >= 60) "${mins / 60}j ${mins % 60}m" else "${mins}m"
    }
    val details = listOfNotNull(
        durationLabel,
        series?.numberOfSeasons?.takeIf { it > 0 }?.let { "${it} musim" },
        series?.numberOfEpisodes?.takeIf { it > 0 }?.let { "${it} episode" }
    )
    val seasons = series?.seasons.orEmpty().ifEmpty {
        series?.defaultSeason?.let { listOf(Season(seasonNumber = it.seasonNumber, name = it.name)) }.orEmpty()
    }
    val firstEpisode = state.episodes.firstOrNull()
    val selectedSeasonNumber = state.selectedSeason?.seasonNumber ?: series?.defaultSeason?.seasonNumber ?: 1

    fun startPlayback() {
        if (movie != null) {
            onPlay("movie", movie.id, movie.title, movie.fullPosterUrl, movie.fullBackdropUrl, movie.slug, null, null)
        } else if (series != null && firstEpisode != null && !state.isSeasonLoading) {
            onPlay(
                "episode", firstEpisode.id, "${series.title} - ${firstEpisode.name}",
                firstEpisode.fullStillUrl, series.fullBackdropUrl, series.slug,
                selectedSeasonNumber, firstEpisode.episodeNumber
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NetflixBlack),
        contentPadding = PaddingValues(bottom = 38.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(415.dp).background(Color(0xFF181818))
            ) {
                AsyncImage(
                    model = backdrop.ifBlank { poster },
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.46f),
                            0.35f to Color.Black.copy(alpha = 0.10f),
                            0.66f to Color.Black.copy(alpha = 0.50f),
                            1f to Color.Black
                        )
                    )
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart).statusBarsPadding()
                        .padding(14.dp)
                        .background(Color.Black.copy(alpha = 0.50f), CircleShape)
                        .border(0.8.dp, Color.White.copy(alpha = 0.23f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
                Column(
                    modifier = Modifier.align(Alignment.BottomStart)
                        .fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MovieTitleArtwork(
                        title = title,
                        logoUrl = state.logoUrl,
                        modifier = Modifier.width(295.dp).height(104.dp),
                        textSize = 29.sp,
                        alignment = Alignment.CenterStart
                    )
                    Text(
                        listOfNotNull(year?.take(4), movie?.sourceBadge ?: series?.sourceBadge)
                            .filter(String::isNotBlank).joinToString(" · "),
                        color = Color(0xFFDFDFE3), fontSize = 12.sp
                    )
                    Button(
                        onClick = ::startPlayback,
                        enabled = movie != null || (firstEpisode != null && !state.isSeasonLoading),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NetflixRed, contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (movie != null) "Putar Film" else "Putar Episode",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    listOfNotNull(
                        ratingValue?.let { "★ " + String.format(java.util.Locale.US, "%.1f", it) },
                        *details.toTypedArray()
                    ).joinToString("  ·  ").ifBlank { "Detail tayangan" },
                    color = if (ratingValue != null) (if (IdlixPalette.isLight) Color(0xFF966000) else Color(0xFFFFD68A)) else NetflixLightGray,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp
                )
                if (genres.isNotBlank()) Text(
                    genres, color = NetflixLightGray, fontSize = 13.sp,
                    maxLines = 3, lineHeight = 20.sp
                )
                if (overview.isNotBlank()) {
                    Text("Sinopsis", color = NetflixWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(overview, color = NetflixWhite, fontSize = 14.sp, lineHeight = 23.sp)
                }
                if (cast.isNotEmpty()) Text(
                    "Ketuk pemeran untuk mencari judul lainnya",
                    color = NetflixLightGray,
                    fontSize = 12.sp
                )
            }
        }

        if (cast.isNotEmpty()) {
            item {
                MobileCastRow(cast = cast, onCastClick = onCastClick)
            }
        }

        if (series != null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Season & Episode", color = NetflixWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (seasons.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(seasons, key = { it.seasonNumber }) { season ->
                                MobileFilterChip(
                                    label = season.name.ifBlank { "Season ${season.seasonNumber}" },
                                    selected = selectedSeasonNumber == season.seasonNumber,
                                    onClick = { viewModel.selectSeason(slug, season) }
                                )
                            }
                        }
                    }
                }
            }
            if (state.isSeasonLoading) item {
                MobileEmpty("Memuat episode season…")
            } else if (state.seasonError != null) item {
                MobileEmpty(state.seasonError.orEmpty(), onRetry = {
                    state.selectedSeason?.let { viewModel.selectSeason(slug, it) }
                })
            } else if (state.episodes.isEmpty()) item {
                MobileEmpty("Episode belum tersedia.")
            } else {
                items(state.episodes, key = { it.id }) { episode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Card(
                            onClick = {
                                onPlay(
                                    "episode", episode.id, "${series.title} - ${episode.name}",
                                    episode.fullStillUrl, series.fullBackdropUrl, series.slug,
                                    selectedSeasonNumber, episode.episodeNumber
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = NetflixDarkCard,
                                contentColor = NetflixWhite
                            ),
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = episode.fullStillUrl,
                                    contentDescription = episode.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.width(108.dp).height(72.dp).background(Color.DarkGray)
                                )
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(
                                        "${episode.episodeNumber}. ${episode.name}",
                                        color = NetflixWhite,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!episode.overview.isNullOrBlank()) {
                                        Text(
                                            episode.overview.orEmpty(),
                                            color = NetflixLightGray,
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Putar",
                                    tint = NetflixWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
