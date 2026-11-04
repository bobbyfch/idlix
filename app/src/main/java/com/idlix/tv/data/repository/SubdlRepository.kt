package com.idlix.tv.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.idlix.tv.IdlixTvApp
import com.idlix.tv.data.api.ApiClient
import com.idlix.tv.data.model.SubdlResponse
import com.idlix.tv.data.model.SubdlSubtitle
import com.idlix.tv.data.model.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.net.URLEncoder
import java.util.zip.ZipInputStream

object SubdlRepository {
    private const val TAG = "SubdlRepository"
    private const val PREF_KEY_SUBDL_API_KEY = "custom_subdl_api_key"
    const val DEFAULT_API_KEY = "subdl__VuGBV3ExlH8tW68l4wVEIsk6Mfnaxq1_FiONFN8Yg0"
    private const val BASE_URL = "https://api.subdl.com/api/v1/subtitles"

    @Volatile
    private var currentApiKey: String? = null

    fun getApiKey(): String {
        currentApiKey?.let { return it }
        val saved = try {
            val prefs = IdlixTvApp.appContext.getSharedPreferences("idlix_config", Context.MODE_PRIVATE)
            prefs.getString(PREF_KEY_SUBDL_API_KEY, null)
        } catch (_: Exception) {
            null
        }
        val key = if (!saved.isNullOrBlank()) saved else DEFAULT_API_KEY
        currentApiKey = key
        return key
    }

    fun setApiKey(newKey: String) {
        val trimmed = newKey.trim()
        currentApiKey = trimmed
        try {
            val prefs = IdlixTvApp.appContext.getSharedPreferences("idlix_config", Context.MODE_PRIVATE)
            prefs.edit().putString(PREF_KEY_SUBDL_API_KEY, trimmed).apply()
        } catch (_: Exception) {}
    }

    suspend fun searchAllSubtitles(
        title: String,
        type: String = "movie",
        tmdbId: Long? = null,
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        languages: String = "ID,EN"
    ): List<SubdlSubtitle> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val queryParams = StringBuilder("api_key=$apiKey&languages=$languages")
            if (tmdbId != null && tmdbId > 0) {
                queryParams.append("&tmdb_id=$tmdbId")
            } else if (!imdbId.isNullOrBlank()) {
                queryParams.append("&imdb_id=$imdbId")
            } else {
                val cleanTitle = title.replace(Regex("\\(\\d{4}\\)"), "").trim()
                val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
                queryParams.append("&film_name=$encoded")
            }

            if (type == "tv" || season != null) {
                queryParams.append("&type=tv")
                if (season != null) queryParams.append("&season_number=$season")
                if (episode != null) queryParams.append("&episode_number=$episode")
            } else {
                queryParams.append("&type=movie")
            }

            val requestUrl = "$BASE_URL?$queryParams"
            val request = Request.Builder()
                .url(requestUrl)
                .header("User-Agent", "Mozilla/5.0 (Android TV; Idlix)")
                .build()

            ApiClient.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "SubDL request failed with code: ${response.code}")
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: return@withContext emptyList()
                val subdlRes = ApiClient.json.decodeFromString<SubdlResponse>(body)
                if (!subdlRes.status || subdlRes.subtitles.isEmpty()) {
                    Log.d(TAG, "No SubDL subtitles found for $title")
                    return@withContext emptyList()
                }
                subdlRes.subtitles
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search SubDL subtitles: ${e.message}")
            emptyList()
        }
    }

    suspend fun downloadSubtitle(
        sub: SubdlSubtitle,
        context: Context
    ): SubtitleTrack? = withContext(Dispatchers.IO) {
        try {
            val zipUrl = sub.fullDownloadUrl
            val prefix = subtitleCachePrefix(sub.language, System.currentTimeMillis())
            val extractedFile = downloadAndExtractSrt(zipUrl, context, prefix) ?: return@withContext null

            val isIndo = sub.language.equals("ID", ignoreCase = true) || sub.lang.equals("indonesian", ignoreCase = true)
            val langCode = if (isIndo) "id" else (sub.language?.lowercase() ?: "en")
            val releaseTag = sub.author?.let { " ($it)" } ?: ""
            val label = if (isIndo) "Indonesian [SubDL]$releaseTag" else "English [SubDL]$releaseTag"

            SubtitleTrack(
                lang = langCode,
                label = label,
                path = Uri.fromFile(extractedFile).toString()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download SubDL subtitle: ${e.message}")
            null
        }
    }

    suspend fun fetchIndonesianSubtitle(
        title: String,
        type: String = "movie",
        tmdbId: Long? = null,
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        context: Context
    ): SubtitleTrack? = withContext(Dispatchers.IO) {
        val allSubs = searchAllSubtitles(title, type, tmdbId, imdbId, season, episode, languages = "ID")
        val chosen = allSubs.firstOrNull {
            it.language.equals("ID", ignoreCase = true) || it.lang.equals("indonesian", ignoreCase = true)
        } ?: allSubs.firstOrNull() ?: return@withContext null

        downloadSubtitle(chosen, context)
    }

    private fun downloadAndExtractSrt(zipUrl: String, context: Context, filePrefix: String): File? {
        // Subtitle ZIPs are untrusted. Reject insecure origins and cap decompression.
        if (!zipUrl.startsWith("https://", ignoreCase = true)) return null
        return try {
            val req = Request.Builder()
                .url(zipUrl)
                .header("User-Agent", "Mozilla/5.0 (Android TV; Idlix)")
                .build()

            ApiClient.okHttpClient.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.byteStream() ?: return null
                ZipInputStream(body).use { zip ->
                    val directory = File(context.cacheDir, "subtitles").apply { mkdirs() }
                    var entry = zip.nextEntry
                    var inspected = 0
                    while (entry != null && inspected++ < 40) {
                        val name = entry.name.lowercase(Locale.ROOT)
                        val extension = when {
                            !entry.isDirectory && name.endsWith(".vtt") -> ".vtt"
                            !entry.isDirectory && name.endsWith(".srt") -> ".srt"
                            else -> null
                        }
                        if (extension != null) {
                            val outFile = File(directory, "$filePrefix$extension")
                            try {
                                FileOutputStream(outFile).use { output ->
                                    val bytes = ByteArray(8192)
                                    var written = 0L
                                    while (true) {
                                        val count = zip.read(bytes)
                                        if (count < 0) break
                                        written += count
                                        if (written > 5L * 1024 * 1024) {
                                            throw IOException("Subtitle file exceeds safe size limit")
                                        }
                                        if (count > 0) output.write(bytes, 0, count)
                                    }
                                }
                            } catch (e: Exception) {
                                outFile.delete() // never leave oversized partial subtitles
                                throw e
                            }
                            return outFile
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Subtitle archive could not be safely opened: ${e.javaClass.simpleName}")
            null
        }
    }
}

/** Shared by the downloader and tests: never allow remote metadata into file paths. */
internal fun subtitleCachePrefix(language: String?, timestamp: Long): String {
    val safeLanguage = language.orEmpty().lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9_-]"), "").take(12).ifBlank { "id" }
    return "subdl_${safeLanguage}_${timestamp.coerceAtLeast(0L)}"
}
