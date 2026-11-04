package com.idlix.tv.data.repository

import org.jsoup.nodes.Element
import java.net.URI

/**
 * Reads poster URLs from the image and lazy-loading formats used by IDLIX pages.
 * Relative URLs are resolved against the page Jsoup parsed, just like a browser.
 */
internal object SiteImageUrl {
    private const val ROOT = "https://z2.idlixku.com/"
    private val sourceSetAttributes = setOf("data-lazy-srcset", "data-srcset", "srcset")
    private val imageAttributes = listOf(
        "data-src",
        "data-lazy-src",
        "data-original",
        "data-lazyload",
        "data-image",
        "data-poster",
        "data-bg",
        "data-background",
        "data-lazy-srcset",
        "data-srcset",
        "srcset",
        "src"
    )
    private val cssImageUrl = Regex("""url\(\s*(['"]?)(.*?)\1\s*\)""", RegexOption.IGNORE_CASE)
    private val placeholderImage = Regex(
        """(?:placeholder|no[-_]?image|transparent|loading|spacer)(?:[._-]|$)""",
        RegexOption.IGNORE_CASE
    )

    fun from(container: Element): String? {
        var scope = container
        repeat(6) {
            findWithin(scope)?.let { return it }

            val parent = scope.parent() ?: return null
            // Stop at a shared list/grid so a neighbouring title cannot donate its poster.
            if (containsMultipleContentItems(parent)) return null
            scope = parent
        }
        return null
    }

    private fun findWithin(scope: Element): String? {
        val elements = listOf(scope) +
            scope.select("img, picture source, [data-bg], [data-background], [style]")
        for (element in elements) {
            for (attribute in imageAttributes) {
                val raw = element.attr(attribute).trim()
                if (raw.isBlank()) continue
                val candidates = if (attribute in sourceSetAttributes) {
                    raw.split(',').mapNotNull { candidate ->
                        candidate.trim().split(Regex("\\s+"))
                            .firstOrNull()?.takeIf { it.isNotBlank() }
                    }
                } else {
                    listOf(raw)
                }
                for (candidate in candidates) {
                    val resolved = resolve(candidate, element.baseUri()) ?: continue
                    if (!placeholderImage.containsMatchIn(URI(resolved).path.orEmpty())) return resolved
                }
            }
            for (match in cssImageUrl.findAll(element.attr("style"))) {
                val candidate = match.groupValues.getOrNull(2)?.trim().orEmpty()
                val resolved = resolve(candidate, element.baseUri()) ?: continue
                if (!placeholderImage.containsMatchIn(URI(resolved).path.orEmpty())) return resolved
            }
        }
        return null
    }

    private fun containsMultipleContentItems(scope: Element): Boolean {
        val contentPath = Regex("""/(?:movie|series)/[a-z0-9-]+/?$""", RegexOption.IGNORE_CASE)
        val paths = LinkedHashSet<String>()
        for (link in scope.select("a[href]")) {
            val href = link.absUrl("href").substringBefore('?').trimEnd('/')
            if (href.isBlank()) continue
            val path = runCatching { URI(href).path.orEmpty() }.getOrDefault("").trimEnd('/')
            if (contentPath.matches(path)) {
                paths.add(path.lowercase(java.util.Locale.ROOT))
                if (paths.size > 1) return true
            }
        }
        return false
    }

    private fun resolve(raw: String, baseUri: String): String? {
        val candidate = raw.trim().trim('"', '\'')
        if (
            candidate.isBlank() ||
            candidate.startsWith("data:", ignoreCase = true) ||
            candidate.startsWith("blob:", ignoreCase = true) ||
            candidate.startsWith("javascript:", ignoreCase = true) ||
            candidate.startsWith("about:", ignoreCase = true)
        ) return null

        return runCatching {
            val normalized = candidate.replace(" ", "%20")
            val parsed = URI(normalized)
            val base = runCatching { URI(baseUri.ifBlank { ROOT }) }.getOrElse { URI(ROOT) }
            val absolute = when {
                normalized.startsWith("//") -> URI("https:$normalized")
                parsed.isAbsolute -> parsed
                else -> base.resolve(parsed)
            }
            val scheme = absolute.scheme?.lowercase()
            if (scheme != "https" && scheme != "http") return null
            if (absolute.host.isNullOrBlank()) return null
            absolute.toASCIIString()
        }.getOrNull()
    }
}
