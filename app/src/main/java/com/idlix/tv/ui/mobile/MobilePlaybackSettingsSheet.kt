package com.idlix.tv.ui.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idlix.tv.data.local.SubtitleAppearance
import com.idlix.tv.data.local.SubtitlePosition
import com.idlix.tv.data.local.SubtitleSize
import com.idlix.tv.data.local.TvPreferencesStore
import com.idlix.tv.data.local.VideoResizeMode
import com.idlix.tv.data.model.SubdlSubtitle
import com.idlix.tv.data.model.SubtitleTrack

private val resizeOptions = listOf(
    Triple(VideoResizeMode.FIT, "Fit", "Video utuh, tanpa terpotong"),
    Triple(VideoResizeMode.ZOOM, "Fill / Zoom", "Memenuhi layar, tepi terpotong"),
    Triple(VideoResizeMode.FILL, "Stretch", "Paksa penuh, rasio gambar berubah"),
    Triple(VideoResizeMode.FIXED_WIDTH, "Lebar", "Sesuaikan berdasarkan lebar video"),
    Triple(VideoResizeMode.FIXED_HEIGHT, "Tinggi", "Sesuaikan berdasarkan tinggi video")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobilePlaybackSettingsSheet(
    initialTab: Int,
    availableHeights: List<Int>,
    selectedHeight: Int,
    onVideoQualitySelected: (Int) -> Unit,
    selectedSubtitle: SubtitleTrack?,
    subtitleEnabled: Boolean,
    availableSubtitles: List<SubtitleTrack>,
    availableSubdl: List<SubdlSubtitle>,
    searchingSubdl: Boolean,
    playbackSpeed: Float,
    onPlaybackSpeed: (Float) -> Unit,
    onSubtitleSelected: (SubtitleTrack?) -> Unit,
    onSubdlSelected: (SubdlSubtitle) -> Unit,
    onSearchSubdl: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val preferences by TvPreferencesStore.preferences.collectAsState()
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141416),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            androidx.compose.material3.BottomSheetDefaults.DragHandle(color = Color(0xFF77777B))
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFFE7E7EA))
                Text("Pengaturan pemutaran", fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf(0 to "Tampilan", 1 to "Subtitle", 2 to "Kualitas")) { (index, title) ->
                    MobileFilterChip(label = title, selected = tab == index, onClick = { tab = index })
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (tab == 0) {
                    item {
                        Text("Rasio & ukuran video", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Pilih bagaimana video menyesuaikan layar. Tidak mengubah resolusi sumber.",
                            color = Color(0xFFACACB2),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                    items(resizeOptions) { (mode, title, description) ->
                        val selected = preferences.videoResizeMode == mode
                        Card(
                            onClick = { TvPreferencesStore.setVideoResizeMode(context, mode) },
                            shape = RoundedCornerShape(14.dp),
                            border = if (selected) BorderStroke(1.dp, Color(0xFFEB6069)) else null,
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) Color(0xFF2C2830) else Color(0xFF222228),
                                contentColor = Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        description,
                                        fontSize = 12.sp,
                                        color = Color(0xFFB6B6BA),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (selected) {
                                    Icon(Icons.Default.Check, contentDescription = "Dipilih", tint = Color(0xFFFF6D76))
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(5.dp))
                        Text("Kecepatan", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(listOf(0.75f, 1f, 1.25f, 1.5f, 2f)) { speed ->
                                MobileFilterChip(
                                    label = "${speed}×",
                                    selected = playbackSpeed == speed,
                                    onClick = { onPlaybackSpeed(speed) }
                                )
                            }
                        }
                    }
                } else if (tab == 1) {
                    item {
                        Text("Bahasa subtitle", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                    item {
                        SheetOption(
                            text = "Nonaktif",
                            selected = !subtitleEnabled,
                            onClick = { onSubtitleSelected(null) }
                        )
                    }
                    items(availableSubtitles, key = { it.path }) { track ->
                        SheetOption(
                            text = "${track.label} · ${track.lang.uppercase()}",
                            selected = subtitleEnabled && selectedSubtitle?.path == track.path,
                            onClick = { onSubtitleSelected(track) }
                        )
                    }
                    item {
                        TextButton(
                            onClick = onSearchSubdl,
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFFFFB8BE)
                            )
                        ) {
                            Text(
                                if (searchingSubdl) "Mencari subtitle…" else "Cari subtitle tambahan (SubDL)",
                                color = Color(0xFFFFB8BE)
                            )
                        }
                    }
                    items(availableSubdl) { sub ->
                        SheetOption(
                            text = (sub.releaseName ?: sub.name ?: "Subtitle") + " · " + (sub.language ?: "EN"),
                            selected = false,
                            onClick = { onSubdlSelected(sub) }
                        )
                    }
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text("Gaya subtitle", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(SubtitleAppearance.values().toList()) { appearance ->
                                val label = when (appearance) {
                                    SubtitleAppearance.SYSTEM -> "Sistem"
                                    SubtitleAppearance.OUTLINE -> "Garis Tepi"
                                    SubtitleAppearance.SHADOW -> "Bayangan"
                                    SubtitleAppearance.SOFT_BOX -> "Kotak Lembut"
                                }
                                MobileFilterChip(
                                    label = label,
                                    selected = preferences.subtitleAppearance == appearance,
                                    onClick = { TvPreferencesStore.setSubtitleAppearance(context, appearance) }
                                )
                            }
                        }
                    }
                    item {
                        Text("Ukuran subtitle", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(SubtitleSize.values().toList()) { size ->
                                MobileFilterChip(
                                    label = when (size) {
                                        SubtitleSize.SMALL -> "Kecil"
                                        SubtitleSize.STANDARD -> "Normal"
                                        SubtitleSize.LARGE -> "Besar"
                                    },
                                    selected = preferences.subtitleSize == size,
                                    onClick = { TvPreferencesStore.setSubtitleSize(context, size) }
                                )
                            }
                        }
                    }
                    item {
                        Text("Posisi subtitle", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(SubtitlePosition.values().toList()) { position ->
                                MobileFilterChip(
                                    label = when (position) {
                                        SubtitlePosition.LOW -> "Bawah"
                                        SubtitlePosition.STANDARD -> "Normal"
                                        SubtitlePosition.HIGH -> "Atas"
                                    },
                                    selected = preferences.subtitlePosition == position,
                                    onClick = { TvPreferencesStore.setSubtitlePosition(context, position) }
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Text("Kualitas video", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Pilih resolusi yang disediakan video ini. Mode Otomatis menyesuaikan koneksi.",
                            color = Color(0xFFB8B8C1), fontSize = 12.sp, lineHeight = 18.sp
                        )
                    }
                    item {
                        SheetOption(
                            text = "Otomatis (disarankan)",
                            selected = selectedHeight == 0,
                            onClick = { onVideoQualitySelected(0) }
                        )
                    }
                    if (availableHeights.isEmpty()) {
                        item {
                            Text(
                                "Sumber ini belum menyediakan pilihan resolusi manual.",
                                color = Color(0xFFBCBCC6), fontSize = 13.sp
                            )
                        }
                    } else {
                        items(availableHeights, key = { it }) { height ->
                            SheetOption(
                                text = "${height}p",
                                selected = selectedHeight == height,
                                onClick = { onVideoQualitySelected(height) }
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(22.dp)) }
            }
        }
    }
}

@Composable
private fun SheetOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF2C2830) else Color(0xFF222228),
            contentColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (selected) Icon(Icons.Default.Check, contentDescription = "Aktif", tint = Color(0xFFFF6D76))
        }
    }
}
