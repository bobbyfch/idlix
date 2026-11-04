package com.idlix.tv.ui.screens.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.idlix.tv.R
import com.idlix.tv.data.local.SubtitleAppearance
import com.idlix.tv.data.local.VideoResizeMode
import com.idlix.tv.data.local.TvPreferencesStore
import com.idlix.tv.data.model.SubdlSubtitle
import com.idlix.tv.data.model.SubtitleTrack
import com.idlix.tv.ui.components.TvLoadingSpinner
import com.idlix.tv.ui.components.glassSurface
import com.idlix.tv.ui.theme.NetflixLightGray
import com.idlix.tv.ui.theme.NetflixRed

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSubtitlePickerModal(
    isVisible: Boolean,
    availableSubtitles: List<SubtitleTrack>,
    selectedSubtitle: SubtitleTrack?,
    isSubtitleEnabled: Boolean,
    videoHeights: List<Int>,
    selectedHeight: Int,
    onVideoHeightSelected: (Int) -> Unit,
    subdlSubtitles: List<SubdlSubtitle>,
    isSearchingSubdl: Boolean,
    onSelectTrack: (SubtitleTrack?) -> Unit,
    onSelectSubdl: (SubdlSubtitle) -> Unit,
    onSearchSubdlClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val preferences by TvPreferencesStore.preferences.collectAsState()
    val context = LocalContext.current

    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            try {
                firstItemFocusRequester.requestFocus()
            } catch (e: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xC9081424))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Slide-out Drawer Panel from Right side
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(420.dp)
                .clickable(enabled = false) {}
                .glassSurface(
                    preferences.glassIntensity,
                    RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp)
                )
                .padding(24.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ClosedCaption,
                        contentDescription = null,
                        tint = NetflixRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Subtitle & Kualitas",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isSearchingSubdl) {
                    TvLoadingSpinner(modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.subtitle_appearance),
                color = NetflixLightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val styleChoices = listOf(
                    SubtitleAppearance.SYSTEM to R.string.subtitle_appearance_system,
                    SubtitleAppearance.OUTLINE to R.string.subtitle_appearance_outline,
                    SubtitleAppearance.SHADOW to R.string.subtitle_appearance_shadow,
                    SubtitleAppearance.SOFT_BOX to R.string.subtitle_appearance_box
                )
                styleChoices.forEach { (appearance, titleRes) ->
                    val selected = preferences.subtitleAppearance == appearance
                    Button(
                        onClick = { TvPreferencesStore.setSubtitleAppearance(context, appearance) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.colors(
                            containerColor = if (selected) NetflixRed else Color(0xFF25262B),
                            contentColor = Color.White,
                            focusedContainerColor = Color.White,
                            focusedContentColor = Color.Black
                        ),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(10.dp)),
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = stringResource(titleRes),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.video_resize_mode),
                color = NetflixLightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(7.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                listOf(
                    VideoResizeMode.FIT to R.string.video_resize_fit,
                    VideoResizeMode.ZOOM to R.string.video_resize_zoom,
                    VideoResizeMode.FILL to R.string.video_resize_stretch
                ).forEach { (mode, label) ->
                    val selected = preferences.videoResizeMode == mode
                    Button(
                        onClick = { TvPreferencesStore.setVideoResizeMode(context, mode) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.colors(
                            containerColor = if (selected) NetflixRed else Color(0xFF25262B),
                            contentColor = Color.White,
                            focusedContainerColor = Color.White,
                            focusedContentColor = Color.Black
                        ),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(10.dp)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 7.dp)
                    ) {
                        Text(stringResource(label), fontSize = 11.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Kualitas video",
                color = NetflixLightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(7.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = PaddingValues(end = 12.dp)
            ) {
                rowItems(listOf(0) + videoHeights, key = { it }) { height ->
                    Button(
                        onClick = { onVideoHeightSelected(height) },
                        colors = ButtonDefaults.colors(
                            containerColor = if (selectedHeight == height) NetflixRed else Color(0xFF25262B),
                            contentColor = Color.White,
                            focusedContainerColor = Color.White,
                            focusedContentColor = Color.Black
                        ),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(11.dp)),
                        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp)
                    ) {
                        Text(if (height == 0) "Otomatis" else "${height}p", fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Turn Off Subtitles
                item {
                    val isOffSelected = !isSubtitleEnabled || selectedSubtitle == null
                    SubtitleOptionCard(
                        title = stringResource(R.string.subtitles_off),
                        subtitle = null,
                        badge = null,
                        isSelected = isOffSelected,
                        onClick = { onSelectTrack(null) },
                        modifier = Modifier.focusRequester(firstItemFocusRequester)
                    )
                }

                // 2. Default & Stream Subtitles
                if (availableSubtitles.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.default_subtitle),
                            color = NetflixLightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }

                    items(availableSubtitles) { track ->
                        val isSelected = isSubtitleEnabled && selectedSubtitle?.path == track.path
                        SubtitleOptionCard(
                            title = track.label.ifBlank { track.lang.uppercase() },
                            subtitle = null,
                            badge = track.lang.uppercase(),
                            isSelected = isSelected,
                            onClick = { onSelectTrack(track) }
                        )
                    }
                }

                // 3. SubDL.com Subtitles
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.subdl_subtitles_header),
                            color = NetflixLightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (!isSearchingSubdl) {
                            Button(
                                onClick = onSearchSubdlClick,
                                colors = ButtonDefaults.colors(
                                    containerColor = Color.Transparent,
                                    contentColor = NetflixRed
                                ),
                                shape = ButtonDefaults.shape(shape = RoundedCornerShape(4.dp)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = stringResource(R.string.search_subdl), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                if (isSearchingSubdl && subdlSubtitles.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.searching_subdl),
                            color = NetflixLightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else if (subdlSubtitles.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_subdl_found),
                            color = NetflixLightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(subdlSubtitles) { sub ->
                        val isIndo = sub.language.equals("ID", ignoreCase = true) || sub.lang.equals("indonesian", ignoreCase = true)
                        val badgeText = if (isIndo) "ID" else (sub.language ?: "EN")
                        val authorText = sub.author?.let { "by $it" } ?: ""
                        val releaseText = sub.releaseName ?: sub.name ?: ""

                        SubtitleOptionCard(
                            title = releaseText,
                            subtitle = authorText.ifBlank { null },
                            badge = badgeText,
                            isSelected = false,
                            onClick = { onSelectSubdl(sub) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SubtitleOptionCard(
    title: String,
    subtitle: String?,
    badge: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = CardDefaults.colors(
            containerColor = if (isSelected) NetflixRed.copy(alpha = 0.25f) else Color(0xFF25262B),
            focusedContainerColor = NetflixRed
        ),
        scale = CardDefaults.scale(focusedScale = 1.04f),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White)
            ),
            border = if (isSelected) {
                Border(border = BorderStroke(1.dp, NetflixRed))
            } else {
                Border.None
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (badge == "ID") NetflixRed else Color(0x66FFFFFF),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = NetflixLightGray,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(NetflixRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
