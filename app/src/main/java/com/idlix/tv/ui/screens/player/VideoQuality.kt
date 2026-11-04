package com.idlix.tv.ui.screens.player

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer

/** Only show HLS renditions that Media3 actually exposes and can decode. */
internal fun availableVideoHeights(player: ExoPlayer): List<Int> {
    return player.currentTracks.groups
        .asSequence()
        .filter { it.type == C.TRACK_TYPE_VIDEO }
        .flatMap { group ->
            (0 until group.length).asSequence()
                .filter { group.isTrackSupported(it) }
                .map { group.getTrackFormat(it).height }
        }
        .filter { it > 0 }
        .distinct()
        .sortedDescending()
        .toList()
}

/** Switch renditions in-place; never restart/re-resolve the stream. */
internal fun selectVideoHeight(player: ExoPlayer, desiredHeight: Int): Boolean {
    val builder = player.trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
    if (desiredHeight <= 0) {
        player.trackSelectionParameters = builder.build()
        return true
    }
    for (group in player.currentTracks.groups) {
        if (group.type != C.TRACK_TYPE_VIDEO) continue
        val match = (0 until group.length)
            .filter { group.isTrackSupported(it) && group.getTrackFormat(it).height == desiredHeight }
            .maxByOrNull { group.getTrackFormat(it).bitrate }
        if (match != null) {
            player.trackSelectionParameters = builder
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(match)))
                .build()
            return true
        }
    }
    return false
}
