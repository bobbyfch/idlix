package com.idlix.tv.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.idlix.tv.data.model.MovieItem
import com.idlix.tv.data.repository.IdlixRepository
import com.idlix.tv.data.repository.TitleLogoRepository
import com.idlix.tv.ui.components.MovieTitleArtwork
import kotlinx.coroutines.CancellationException
import com.idlix.tv.ui.theme.NetflixRed
import com.idlix.tv.ui.theme.NetflixDarkCard
import com.idlix.tv.ui.theme.NetflixLightGray
import com.idlix.tv.ui.theme.NetflixWhite
import com.idlix.tv.ui.theme.NetflixDarkGray

private val SurfaceCard get() = NetflixDarkCard
private val TextSecondary get() = NetflixLightGray
private val RatingGold = Color(0xFFFFDA79)

/** One high-contrast selection style shared by discover, search and settings. */
@Composable
internal fun MobileFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = leadingIcon,
        label = {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        },
        shape = RoundedCornerShape(13.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

/**
 * Native mobile poster: no invisible Material LocalContentColor inheritance.
 * Quality is only shown when returned by the API; never infer resolution.
 */
@Composable
internal fun MobilePosterCard(
    title: String,
    poster: String,
    rating: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false,
    quality: String? = null,
    progressLabel: String? = null,
    imageReferer: String? = null
) {
    val context = LocalContext.current
    val imageRequest = remember(poster, imageReferer) {
        ImageRequest.Builder(context)
            .data(poster)
            .crossfade(false)
            .apply {
                imageReferer?.takeIf { it.isNotBlank() }?.let { setHeader("Referer", it) }
            }
            .build()
    }

    Card(
        modifier = if (fullWidth) modifier.fillMaxWidth() else modifier.width(146.dp),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceCard,
            contentColor = NetflixWhite
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(NetflixDarkGray)
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                val displayQuality = quality?.trim()?.takeIf {
                    it.isNotEmpty() && it != "N/A"
                }
                if (displayQuality != null) {
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(7.dp)
                            .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(7.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text(
                            displayQuality.uppercase(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                if (!progressLabel.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(7.dp)
                            .background(Color.Black.copy(alpha = 0.80f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            progressLabel,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
                val displayRating = rating?.takeIf { it.isNotBlank() && it != "N/A" }
                if (displayRating != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(7.dp)
                            .background(Color.Black.copy(alpha = 0.80f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = RatingGold,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            displayRating,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                color = NetflixWhite,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun MobileMovieRow(
    title: String,
    items: List<MovieItem>,
    onItemClick: (MovieItem) -> Unit
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            color = NetflixWhite,
            modifier = Modifier.padding(horizontal = 18.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { "${it.contentType}:${it.id}" }) { item ->
                MobilePosterCard(
                    title = item.title,
                    poster = item.fullPosterUrl,
                    rating = item.formattedRating,
                    quality = item.sourceBadge,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
internal fun MobileHero(
    item: MovieItem?,
    onDetails: () -> Unit,
    onPlay: () -> Unit
) {
    if (item == null) return
    val titleLogo = remember(item.slug, item.contentType) { mutableStateOf<String?>(null) }
    LaunchedEffect(item.slug, item.contentType) {
        titleLogo.value = try {
            val repository = IdlixRepository()
            if (item.contentType.contains("series", ignoreCase = true)) {
                val detail = repository.getSeriesDetail(item.slug)
                TitleLogoRepository.resolve("series", item.slug, detail?.titleLogoUrl, detail?.imdbId)
            } else {
                val detail = repository.getMovieDetail(item.slug)
                TitleLogoRepository.resolve("movie", item.slug, detail?.titleLogoUrl, detail?.imdbId)
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            null
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(415.dp)
            .background(Color(0xFF151518))
    ) {
        AsyncImage(
            model = item.fullBackdropUrl.ifBlank { item.fullPosterUrl },
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = 0.32f),
                    0.30f to Color.Black.copy(alpha = 0.16f),
                    0.55f to Color.Black.copy(alpha = 0.48f),
                    0.86f to Color.Black.copy(alpha = 0.92f),
                    1f to Color.Black
                )
            )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 29.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MovieTitleArtwork(
                title = item.title,
                logoUrl = titleLogo.value,
                modifier = Modifier.fillMaxWidth().height(76.dp),
                textSize = 28.sp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val releaseYear = item.releaseDate?.take(4)?.takeIf { it.length == 4 }
                if (releaseYear != null) {
                    Text(releaseYear, color = TextSecondary, fontSize = 12.sp)
                }
                Text(
                    if (item.contentType == "tv_series") "Serial" else "Film",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                item.sourceBadge?.let {
                    Text(it.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (item.formattedRating != "N/A") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = RatingGold, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(item.formattedRating, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NetflixRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(13.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (item.contentType == "tv_series") "Lihat Episode" else "Putar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onDetails,
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.54f),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 19.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(5.dp))
                    Text("Detail", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun MobileLoading(message: String = "Memuat katalog…") {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NetflixRed)
            Spacer(Modifier.height(14.dp))
            Text(message, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
internal fun MobileEmpty(message: String, onRetry: (() -> Unit)? = null, actionLabel: String = "Coba lagi") {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(message, color = TextSecondary, fontSize = 14.sp)
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text(actionLabel, color = NetflixRed) }
        }
    }
}
