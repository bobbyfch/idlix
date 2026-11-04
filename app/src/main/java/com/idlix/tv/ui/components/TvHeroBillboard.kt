package com.idlix.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Border
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.idlix.tv.R
import com.idlix.tv.data.model.MovieItem
import com.idlix.tv.data.repository.IdlixRepository
import com.idlix.tv.data.repository.TitleLogoRepository
import kotlinx.coroutines.CancellationException
import com.idlix.tv.ui.theme.NetflixGold

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvHeroBillboard(
    movie: MovieItem?,
    modifier: Modifier = Modifier,
    playButtonModifier: Modifier = Modifier,
    playLabel: String? = null,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    if (movie == null) return
    // The photo stays cinematic while its lower edge blends into the selected
    // page theme. Use dark scrim on the text region for reliable contrast.
    val heroSurface = MaterialTheme.colorScheme.background
    val heroBottomBrush = Brush.verticalGradient(
        colors = listOf(Color.Transparent, heroSurface.copy(alpha = 0.38f), heroSurface),
        startY = 80f
    )
    val heroLeftBrush = Brush.horizontalGradient(
        colors = listOf(
            heroSurface.copy(alpha = 0.96f),
            heroSurface.copy(alpha = 0.72f),
            Color.Transparent
        )
    )

    val context = LocalContext.current
    val titleLogo = remember(movie.slug, movie.contentType) { mutableStateOf<String?>(null) }
    LaunchedEffect(movie.slug, movie.contentType) {
        titleLogo.value = try {
            val repository = IdlixRepository()
            if (movie.contentType.contains("series", ignoreCase = true)) {
                val detail = repository.getSeriesDetail(movie.slug)
                TitleLogoRepository.resolve("series", movie.slug, detail?.titleLogoUrl, detail?.imdbId)
            } else {
                val detail = repository.getMovieDetail(movie.slug)
                TitleLogoRepository.resolve("movie", movie.slug, detail?.titleLogoUrl, detail?.imdbId)
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            null
        }
    }
    val backdropUrl = movie.fullBackdropUrl
    val backdropRequest = remember(backdropUrl) {
        ImageRequest.Builder(context)
            .data(backdropUrl)
            .size(1280, 720)
            .precision(Precision.INEXACT)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        // Dynamic Backdrop
        AsyncImage(
            model = backdropRequest,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Bottom gradient fade to black
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(heroBottomBrush)
        )

        // Left gradient fade ONLY on text area (saves 70% fillrate)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(540.dp)
                .background(heroLeftBrush)
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(480.dp)
                .padding(start = 24.dp, top = 24.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Title
            MovieTitleArtwork(
                title = movie.title,
                logoUrl = titleLogo.value,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                textSize = 28.sp,
                textColor = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Metadata Badges (Rating, Year, Quality)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (movie.formattedRating != "N/A") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = NetflixGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${movie.formattedRating} / 10",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                movie.releaseDate?.take(4)?.let { year ->
                    Text(
                        text = year,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                movie.quality?.let { q ->
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.84f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = q,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPlayClick,
                    modifier = playButtonModifier,
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        focusedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = ButtonDefaults.border(
                        focusedBorder = Border(
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        )
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(16.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = playLabel ?: stringResource(R.string.play),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = playLabel ?: stringResource(R.string.play),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Button(
                    onClick = onDetailsClick,
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.84f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        focusedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = ButtonDefaults.border(
                        focusedBorder = Border(
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        )
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(16.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.more_info),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.more_info),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
