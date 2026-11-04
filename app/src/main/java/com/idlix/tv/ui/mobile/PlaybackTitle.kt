package com.idlix.tv.ui.mobile

/**
 * Display human-readable titles in the player. A raw source release filename
 * is preserved separately in "Info sumber", never used as control text.
 */
internal data class PlaybackTitle(val name: String, val episodeLabel: String?, val quality: String?)

internal fun readablePlaybackTitle(raw: String, isEpisode: Boolean): PlaybackTitle {
    val quality = Regex("""(?i)\b(2160p|1080p|720p|480p|4k)\b""")
        .find(raw)?.value?.uppercase()
    val original = raw.trim()
    if (original.isBlank()) return PlaybackTitle("Sedang diputar", null, quality)

    // Release filenames commonly use dots/underscores as spaces.
    var clean = original
        .replace(Regex("""\[[^\]]*]"""), " ")
        .replace(Regex("""(?i)\.(mkv|mp4|m4v|avi|ts)$"""), "")
        .replace(Regex("""[._]+"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    val numberedEpisode = Regex("""(?i)\bS(\d{1,2})[ -]*E(\d{1,3})\b""").find(clean)
    var episode: String? = if (isEpisode && numberedEpisode != null) {
        val seasonNumber = numberedEpisode.groupValues[1].toIntOrNull()
        val episodeNumber = numberedEpisode.groupValues[2].toIntOrNull()
        if (seasonNumber != null && episodeNumber != null)
            "S${seasonNumber} · E${episodeNumber}" else null
    } else null
    if (numberedEpisode != null) {
        clean = clean.replace(numberedEpisode.value, " ")
    }

    // Everything after a release/codec tag is technical metadata, not a title.
    clean = clean.replace(
        Regex("""(?i)\b(?:2160p|1080p|720p|480p|4k|WEB[ -]?DL|WEB[ -]?Rip|BluRay|BRRip|HDRip|DVDRip|DSNP|NF|AMZN|DDP5|DDP2|DDP|EAC3|HEVC|H[ .-]?26[45]|x26[45]|AAC|DTS)\b.*$"""),
        ""
    ).replace(Regex("""\([^)]*(?:web[ -]?rip|bluray|hevc|x26[45]|aac|1080p|720p|2160p)[^)]*\)""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\s+"""), " ")
        .trim(' ', '-', '_', '.', '·')

    if (isEpisode && episode == null) {
        val trailingEpisode = Regex("""(?i)(?:\s*[-–:]\s*|\s+)(?:ep(?:isode)?\.?\s*)?(\d{1,3})\s*$""")
            .find(clean)
        if (trailingEpisode != null) {
            val number = trailingEpisode.groupValues[1].toIntOrNull()
            if (number != null && number in 1..999) {
                episode = "Episode $number"
                clean = clean.substring(0, trailingEpisode.range.first).trim()
            }
        }
    }

    return PlaybackTitle(clean.ifBlank { original }, episode, quality)
}
