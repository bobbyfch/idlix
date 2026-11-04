package com.idlix.tv.data.model

import java.util.Locale

/** Source type (WEB-DL/BluRay) is not the same thing as resolution (1080p/720p). */
private val sourceToken = Regex(
    """(?i)(?<![a-z0-9])(?:web[ ._-]?dl|web[ ._-]?rip|blu[ ._-]?ray|bd[ ._-]?rip|br[ ._-]?rip|b[dr]remux|hdtv|hdcam|camrip|cam|dvdrip|hdrip|remux)(?![a-z0-9])"""
)

/** Only classify source labels from explicit server metadata or tagged release names. */
fun releaseSourceBadge(quality: String?, title: String? = null, slug: String? = null): String? {
    for ((index, raw) in listOfNotNull(quality, title, slug).withIndex()) {
        val clean = raw.trim()
        if (clean.isEmpty() || clean.equals("N/A", ignoreCase = true) ||
            clean.equals("null", ignoreCase = true)) continue
        val token = sourceToken.find(clean)?.value?.uppercase(Locale.ROOT)
            ?.filter { it.isLetterOrDigit() }
        if (token != null) {
            return when (token) {
                "WEBDL" -> "WEB-DL"
                "WEBRIP" -> "WEBRip"
                "BLURAY", "BDRIP", "BRRIP", "BDREMUX", "BRREMUX", "REMUX" -> "BluRay"
                "DVDRIP" -> "DVDRip"
                "HDRIP" -> "HDRip"
                "HDTV" -> "HDTV"
                "HDCAM", "CAMRIP", "CAM" -> "CAM"
                else -> null
            }
        }
        // Preserve explicit resolution tiers without incorrectly labelling them BluRay/WEB-DL.
        if (index == 0 && clean.matches(Regex("""(?i)(HD|SD|FULL HD|FHD|UHD|4K|2160P|1080P|720P|480P)"""))) {
            return clean.uppercase(Locale.ROOT)
        }
    }
    return null
}
