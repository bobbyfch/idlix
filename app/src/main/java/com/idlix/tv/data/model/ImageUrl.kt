package com.idlix.tv.data.model

/** Resolve both TMDB relative paths and already-absolute CDN URLs safely. */
internal fun mediaImageUrl(path: String?, size: String): String {
    val value = path?.trim().orEmpty()
    if (value.isEmpty()) return ""
    if (value.startsWith("https://", true) || value.startsWith("http://", true)) return value
    if (value.startsWith("//")) return "https:$value"
    return "https://image.tmdb.org/t/p/$size/${value.trimStart('/')}"
}
