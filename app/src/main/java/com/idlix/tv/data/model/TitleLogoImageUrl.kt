package com.idlix.tv.data.model

/** Film/series title artwork is a transparent logo; never a poster or backdrop. */
fun titleLogoImageUrl(path: String?): String? {
    val value = path?.trim()?.takeIf {
        it.isNotBlank() && !it.equals("null", true) && !it.equals("n/a", true)
    } ?: return null
    if (value.startsWith("http://", ignoreCase = true)) return null
    val image = if (value.endsWith(".svg", ignoreCase = true)) {
        value.dropLast(4) + ".png"
    } else value
    val url = mediaImageUrl(image, "w500")
    return url.takeIf { it.startsWith("https://", ignoreCase = true) }
}
