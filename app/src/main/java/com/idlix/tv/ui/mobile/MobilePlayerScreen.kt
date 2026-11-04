package com.idlix.tv.ui.mobile

import android.content.Context
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.idlix.tv.ui.screens.player.availableVideoHeights
import com.idlix.tv.ui.screens.player.selectVideoHeight
import com.idlix.tv.ui.screens.player.safeForwardSeekPosition
import com.idlix.tv.ui.screens.player.toMedia3ResizeMode
import android.content.res.Configuration
import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.ui.PlayerView
import com.idlix.tv.data.local.SubtitleAppearance
import com.idlix.tv.data.local.TvPreferencesStore
import com.idlix.tv.data.model.ResolvedPlayback
import com.idlix.tv.ui.components.MovieTitleArtwork
import androidx.compose.ui.text.style.TextAlign
import com.idlix.tv.data.model.SubtitleTrack
import com.idlix.tv.ui.screens.player.PlayerViewModel
import com.idlix.tv.ui.screens.player.QueryPreservingDataSourceFactory
import com.idlix.tv.ui.screens.player.applyTvSubtitleAppearance
import com.idlix.tv.ui.theme.NetflixRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.ArrayList

@OptIn(UnstableApi::class)
private fun mobileMediaSource(
    context: Context,
    stream: ResolvedPlayback,
    track: SubtitleTrack?,
    factory: QueryPreservingDataSourceFactory
): MediaSource {
    val video = HlsMediaSource.Factory(factory).createMediaSource(
        MediaItem.Builder()
            .setUri(stream.hlsMasterUrl)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()
    )
    if (track == null || track.path.isBlank()) return video
    val uri = Uri.parse(track.path)
    val subtitleConfiguration = MediaItem.SubtitleConfiguration.Builder(uri)
        .setMimeType(if (track.path.endsWith(".srt", ignoreCase = true)) MimeTypes.APPLICATION_SUBRIP else MimeTypes.TEXT_VTT)
        .setLanguage(track.lang)
        .setLabel(track.label)
        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
        .build()
    val subtitleFactory = if (uri.scheme == "file" || uri.scheme == null)
        DefaultDataSource.Factory(context)
    else factory
    val subtitleSource = SingleSampleMediaSource.Factory(subtitleFactory)
        .createMediaSource(subtitleConfiguration, C.TIME_UNSET)
    return MergingMediaSource(video, subtitleSource)
}

/**
 * Full-screen when landscape, native touch theatre when portrait.
 * The only player engine is the existing ExoPlayer + HLS resolver; this
 * composable replaces the stock Media3 controller with compact touch UI.
 */
@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobilePlayerScreen(
    contentType: String,
    contentId: String,
    title: String,
    posterUrl: String?,
    backdropUrl: String?,
    slug: String?,
    seasonNumber: Int?,
    episodeNumber: Int?,
    onBack: () -> Unit,
    tabletMode: Boolean = false,
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val preferences by TvPreferencesStore.preferences.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val rawTitle = state.resolvedStream?.title?.ifBlank { title } ?: title
    // Prefer the human-facing title selected in Detail instead of the raw CDN
    // release filename. Keep rawTitle exclusively in the optional source info.
    val displayTitle = remember(title, rawTitle, contentType) {
        readablePlaybackTitle(title.ifBlank { rawTitle }, contentType == "episode")
    }
    var settingsVisible by remember { mutableStateOf(false) }
    var settingsTab by remember { mutableIntStateOf(0) }
    var fileInfoVisible by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionCount by remember { mutableIntStateOf(0) }
    var currentMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }

    fun openSettings(tab: Int) {
        settingsTab = tab
        settingsVisible = true
        controlsVisible = true
        interactionCount++
    }

    // Enter cinema fullscreen immediately for ANY content opened on a phone.
    // It is independent of the handset's Auto Rotate toggle, and restores the
    // previous preference on exit (never changes Android TV orientation).
    DisposableEffect(context, tabletMode) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        if (activity != null && !tabletMode) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            if (activity != null && !tabletMode && originalOrientation != null) {
                activity.requestedOrientation = originalOrientation
            }
        }
    }

    // A cinema player is immersive in landscape; portrait retains the user's
    // status-bar preference. Restore the browser system bars when leaving.
    DisposableEffect(isLandscape, context, preferences.mobileStatusBarEnabled) {
        val activity = context as? Activity
        val insets = activity?.let {
            WindowCompat.getInsetsController(it.window, it.window.decorView)
        }
        if (insets != null) {
            if (isLandscape) {
                insets.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                insets.show(WindowInsetsCompat.Type.navigationBars())
                if (preferences.mobileStatusBarEnabled)
                    insets.show(WindowInsetsCompat.Type.statusBars())
                else insets.hide(WindowInsetsCompat.Type.statusBars())
            }
            insets.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            insets?.show(WindowInsetsCompat.Type.navigationBars())
            if (preferences.mobileStatusBarEnabled)
                insets?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    LaunchedEffect(contentType, contentId) {
        viewModel.loadStream(
            contentType, contentId, title, posterUrl, backdropUrl, slug, seasonNumber, episodeNumber
        )
    }

    LaunchedEffect(controlsVisible, isPlaying, interactionCount, settingsVisible) {
        if (controlsVisible && isPlaying && !settingsVisible) {
            delay(4300L)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(if (!isLandscape && preferences.mobileStatusBarEnabled) Modifier.statusBarsPadding() else Modifier)
            .then(if (!isLandscape) Modifier.navigationBarsPadding() else Modifier)
    ) {
        when {
            state.isLoading -> Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MovieTitleArtwork(
                    title = displayTitle.name,
                    logoUrl = state.logoUrl,
                    modifier = Modifier
                        .width(if (isLandscape) 380.dp else 270.dp)
                        .height(if (isLandscape) 128.dp else 105.dp),
                    textSize = if (isLandscape) 34.sp else 26.sp,
                    alignment = Alignment.Center,
                    textAlign = TextAlign.Center,
                    breathing = true
                )
                Spacer(Modifier.height(17.dp))
                Text(
                    "Menyiapkan pemutaran",
                    color = Color(0xFFD5D5DC),
                    fontSize = 13.sp
                )
                if (state.countdownSeconds > 0) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "${state.countdownSeconds} detik",
                        color = Color(0xFF9999A7),
                        fontSize = 12.sp
                    )
                }
            }
            state.error != null -> Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MobileEmpty(state.error.orEmpty(), onRetry = {
                    viewModel.loadStream(
                        contentType, contentId, title, posterUrl, backdropUrl, slug,
                        seasonNumber, episodeNumber
                    )
                })
            }
            state.resolvedStream?.hlsMasterUrl.isNullOrBlank() ->
                MobileEmpty("Sumber video belum tersedia.")
            else -> {
                val stream = state.resolvedStream!!
                val playingContentId = state.playingContentId
                var videoHeights by remember(stream.hlsMasterUrl) {
                    mutableStateOf<List<Int>>(emptyList())
                }
                var selectedVideoHeight by remember(stream.hlsMasterUrl) { mutableIntStateOf(0) }
                val factory = remember(stream.hlsMasterUrl) {
                    QueryPreservingDataSourceFactory(
                        DefaultHttpDataSource.Factory()
                            .setUserAgent("Mozilla/5.0 (Android; Mobile) AppleWebKit/537.36")
                            .setDefaultRequestProperties(
                                mapOf(
                                    "Referer" to "https://z2.idlixku.com/",
                                    "Origin" to "https://z2.idlixku.com"
                                )
                            ),
                        Uri.parse(stream.hlsMasterUrl).query.orEmpty()
                    )
                }
                val exoPlayer = remember(context, stream.hlsMasterUrl, playingContentId) {
                    val renderersFactory = object : DefaultRenderersFactory(context) {
                        override fun buildTextRenderers(
                            context: Context,
                            output: TextOutput,
                            outputLooper: Looper,
                            extensionRendererMode: Int,
                            out: ArrayList<Renderer>
                        ) {
                            val renderer = TextRenderer(output, outputLooper)
                            renderer.experimentalSetLegacyDecodingEnabled(true)
                            out.add(renderer)
                        }
                    }
                    ExoPlayer.Builder(context, renderersFactory).build().apply {
                        val track = stream.subtitles.firstOrNull { it.lang.equals("id", true) }
                            ?: stream.subtitles.firstOrNull()
                        setMediaSource(mobileMediaSource(context, stream, track, factory))
                        trackSelectionParameters = trackSelectionParameters.buildUpon()
                            .setPreferredTextLanguage("id")
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, track == null)
                            .build()
                        prepare()
                        if (state.initialPositionMs > 0L) seekTo(state.initialPositionMs)
                        playWhenReady = true
                    }
                }

                DisposableEffect(exoPlayer, playingContentId) {
                    onDispose {
                        try {
                            if (exoPlayer.duration > 0L) {
                                viewModel.saveProgressFor(
                                    playingContentId,
                                    exoPlayer.currentPosition,
                                    exoPlayer.duration
                                )
                            }
                        } catch (_: Exception) { }
                        exoPlayer.release()
                    }
                }
                LaunchedEffect(exoPlayer, playingContentId) {
                    var lastSavedPosition = -1L
                    while (isActive) {
                        currentMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                        durationMs = exoPlayer.duration.coerceAtLeast(0L)
                        isPlaying = exoPlayer.isPlaying
                        val heights = availableVideoHeights(exoPlayer)
                        if (videoHeights != heights) videoHeights = heights
                        if (selectedVideoHeight != 0 && selectedVideoHeight !in heights) {
                            selectedVideoHeight = 0
                        }
                        // Keep UI responsive at 800 ms, but don't rewrite history
                        // JSON/SharedPreferences each tick. Persist after 5 seconds
                        // of movement or a seek; DisposalEffect saves the final point.
                        if (durationMs > 0L &&
                            (lastSavedPosition < 0L || kotlin.math.abs(currentMs - lastSavedPosition) >= 5_000L)) {
                            viewModel.saveProgressFor(playingContentId, currentMs, durationMs)
                            lastSavedPosition = currentMs
                        }
                        delay(800L)
                    }
                }
                LaunchedEffect(exoPlayer, playbackSpeed) {
                    exoPlayer.setPlaybackSpeed(playbackSpeed)
                }

                fun switchSubtitle(track: SubtitleTrack?) {
                    val previousPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val shouldPlay = exoPlayer.playWhenReady
                    exoPlayer.setMediaSource(
                        mobileMediaSource(context, stream, track, factory),
                        previousPosition
                    )
                    exoPlayer.trackSelectionParameters =
                        exoPlayer.trackSelectionParameters.buildUpon()
                            .setPreferredTextLanguage(track?.lang ?: "id")
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, track == null)
                            .build()
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = shouldPlay
                    // Replacing a MediaSource changes track-group identity;
                    // reset stale manual overrides to adaptive mode.
                    selectVideoHeight(exoPlayer, 0)
                    selectedVideoHeight = 0
                    viewModel.selectSubtitleTrack(track)
                    settingsVisible = false
                    controlsVisible = true
                    interactionCount++
                }

                val nextEpisodeReady = durationMs > 120_000L &&
                    durationMs - currentMs in 0L..90_000L &&
                    state.nextEpisode != null && !state.isNextEpisodeDismissed

                @Composable
                fun VideoSurface(modifier: Modifier) {
                    Box(modifier.background(Color.Black)) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = false
                                    resizeMode = preferences.videoResizeMode.toMedia3ResizeMode()
                                    subtitleView?.applyTvSubtitleAppearance(
                                        preferences,
                                        controlsVisible = controlsVisible,
                                        nextEpisodeVisible = nextEpisodeReady
                                    )
                                }
                            },
                            update = { view ->
                                if (view.player !== exoPlayer) view.player = exoPlayer
                                view.resizeMode = preferences.videoResizeMode.toMedia3ResizeMode()
                                view.keepScreenOn = isPlaying
                                view.subtitleView?.applyTvSubtitleAppearance(
                                    preferences,
                                    controlsVisible = controlsVisible,
                                    nextEpisodeVisible = nextEpisodeReady
                                )
                            }
                        )
                        MobilePlayerControls(
                            title = displayTitle.name,
                            visible = controlsVisible,
                            isPlaying = isPlaying,
                            currentPositionMs = currentMs,
                            durationMs = durationMs,
                            isLandscape = isLandscape,
                            onToggleVisibility = {
                                controlsVisible = !controlsVisible
                                interactionCount++
                            },
                            onBack = onBack,
                            onOpenSettings = { openSettings(0) },
                            onOpenSubtitles = { openSettings(1) },
                            onOpenQuality = { openSettings(2) },
                            onPlayPause = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            onSeekBack = {
                                exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                            },
                            onSeekForward = {
                                exoPlayer.seekTo(
                                    safeForwardSeekPosition(
                                        exoPlayer.currentPosition, exoPlayer.duration
                                    )
                                )
                            },
                            onSeekTo = { exoPlayer.seekTo(it) },
                            onInteracted = { controlsVisible = true; interactionCount++ }
                        )
                    }
                }

                if (isLandscape) {
                    VideoSurface(Modifier.fillMaxSize())
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        VideoSurface(Modifier.fillMaxWidth().aspectRatio(16f / 9f))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 19.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                displayTitle.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 28.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            val descriptions = listOfNotNull(
                                displayTitle.episodeLabel,
                                displayTitle.quality,
                                if (playbackSpeed != 1f) "${playbackSpeed}×" else null
                            )
                            if (descriptions.isNotEmpty()) {
                                Text(
                                    descriptions.joinToString("  ·  "),
                                    color = Color(0xFFB4B4BD),
                                    fontSize = 13.sp
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { openSettings(0) },
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.09f),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.AspectRatio, contentDescription = null)
                                    Spacer(Modifier.width(7.dp))
                                    Text(when (preferences.videoResizeMode) {
                                        com.idlix.tv.data.local.VideoResizeMode.FIT -> "Fit"
                                        com.idlix.tv.data.local.VideoResizeMode.ZOOM -> "Zoom"
                                        com.idlix.tv.data.local.VideoResizeMode.FILL -> "Stretch"
                                        com.idlix.tv.data.local.VideoResizeMode.FIXED_WIDTH -> "Lebar"
                                        com.idlix.tv.data.local.VideoResizeMode.FIXED_HEIGHT -> "Tinggi"
                                    })
                                }
                                OutlinedButton(
                                    onClick = { openSettings(1) },
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.09f),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.ClosedCaption, contentDescription = null)
                                    Spacer(Modifier.width(7.dp))
                                    Text(if (state.isSubtitleEnabled) "Subtitle" else "CC Mati")
                                }
                            }

                            if (nextEpisodeReady) {
                                Button(
                                    onClick = { viewModel.playNextEpisode() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = NetflixRed,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Putar Episode Berikutnya")
                                }
                            }

                            TextButton(
                                onClick = { fileInfoVisible = !fileInfoVisible },
                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFFE8E8EF)
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null,
                                    modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(7.dp))
                                Text(if (fileInfoVisible) "Sembunyikan info sumber" else "Info sumber")
                            }
                            if (fileInfoVisible) {
                                Text(
                                    rawTitle,
                                    color = Color(0xFF9E9EA8),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                if (settingsVisible) {
                    MobilePlaybackSettingsSheet(
                        initialTab = settingsTab,
                        availableHeights = videoHeights,
                        selectedHeight = selectedVideoHeight,
                        onVideoQualitySelected = { height ->
                            if (selectVideoHeight(exoPlayer, height)) {
                                selectedVideoHeight = height
                                controlsVisible = true
                            }
                        },
                        selectedSubtitle = state.selectedSubtitle,
                        subtitleEnabled = state.isSubtitleEnabled,
                        availableSubtitles = stream.subtitles,
                        availableSubdl = state.subdlSubtitles,
                        searchingSubdl = state.isSearchingSubdl,
                        playbackSpeed = playbackSpeed,
                        onPlaybackSpeed = { speed ->
                            playbackSpeed = speed
                            controlsVisible = true
                        },
                        onSubtitleSelected = ::switchSubtitle,
                        onSubdlSelected = { sub ->
                            viewModel.selectSubdlSubtitle(sub) { newTrack ->
                                switchSubtitle(newTrack)
                            }
                        },
                        onSearchSubdl = { viewModel.searchSubdlSubtitles() },
                        onDismiss = { settingsVisible = false }
                    )
                }
            }
        }
        if (state.isLoading || state.error != null ||
            state.resolvedStream?.hlsMasterUrl.isNullOrBlank()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(7.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }
        }
    }
}
