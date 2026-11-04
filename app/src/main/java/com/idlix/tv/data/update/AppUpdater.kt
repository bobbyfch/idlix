package com.idlix.tv.data.update

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.idlix.tv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class GithubRelease(
    val version: String,
    val apkUrl: String,
    val apkSize: Long,
    val sha256: String?,
    val versionCode: Long? = null
)

/**
 * The GitHub releases API is the only metadata source for app updates.
 * All installs are explicit: Android's package installer always owns approval.
 * The APK is cached privately and granted to the installer via FileProvider.
 */
object AppUpdater {
    private const val LATEST_API = "https://api.github.com/repos/bobbyfch/idlix/releases/latest"
    private const val MAX_APK_BYTES = 120L * 1024 * 1024
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** Returns 1 if remote is newer, 0 if equal, -1 if older. */
    fun compareVersions(remote: String, local: String): Int {
        fun parts(version: String): List<Long> {
            val normalized = version.trim().removePrefix("v")
            require(Regex("^\\d+\\.\\d+\\.\\d+$").matches(normalized)) {
                "Versi release tidak dikenali."
            }
            return normalized.split(".").map { it.toLong() }
        }
        val newer = parts(remote)
        val current = parts(local)
        for (i in 0..2) {
            if (newer[i] != current[i]) return newer[i].compareTo(current[i])
        }
        return 0
    }

    fun isNewer(release: GithubRelease, installedVersionCode: Int, installedVersionName: String): Boolean {
        val remoteCode = release.versionCode
        return if (remoteCode != null) remoteCode > installedVersionCode.toLong()
        else compareVersions(release.version, installedVersionName) > 0
    }

    fun parseRelease(json: String): GithubRelease {
        val obj = JSONObject(json)
        require(!obj.optBoolean("draft") && !obj.optBoolean("prerelease")) {
            "Release belum final."
        }
        val tag = obj.getString("tag_name")
        val version = tag.trim().removePrefix("v")
        compareVersions(version, version) // validate release version syntax
        val versionCode = Regex("""<!--\s*IDLIX_VERSION_CODE=(\d+)\s*-->""")
            .find(obj.optString("body"))
            ?.groupValues?.getOrNull(1)
            ?.toLongOrNull()
        val name = "IDLIX-$version.apk"
        val assets = obj.getJSONArray("assets")
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name") != name) continue
            val url = asset.optString("browser_download_url")
            val expectedPrefix =
                "https://github.com/bobbyfch/idlix/releases/download/$tag/"
            require(url == expectedPrefix + name) { "Alamat APK bukan dari repo IDLIX." }
            val size = asset.optLong("size", -1L)
            require(size in 1..MAX_APK_BYTES) { "Ukuran APK tidak valid." }
            val digest = asset.optString("digest").takeIf { it.startsWith("sha256:") }
                ?.removePrefix("sha256:")
                ?.takeIf { it.matches(Regex("[0-9a-fA-F]{64}")) }
            return GithubRelease(version, url, size, digest, versionCode)
        }
        error("APK resmi untuk versi $version belum tersedia.")
    }

    suspend fun checkLatest(): GithubRelease = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(LATEST_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "IDLIX-Android/${BuildConfig.VERSION_NAME}")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException(
                    if (response.code == 403) "Batas akses GitHub tercapai. Coba lagi nanti."
                    else "Gagal memeriksa pembaruan (HTTP ${response.code})."
                )
            }
            parseRelease(response.body?.string() ?: throw IOException("Respons GitHub kosong."))
        }
    }

    suspend fun download(
        context: Context,
        release: GithubRelease,
        onProgress: suspend (Int) -> Unit
    ): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val partial = File(dir, "idlix-update.part")
        val target = File(dir, "idlix-update.apk")
        partial.delete()
        target.delete()
        try {
            val req = Request.Builder().url(release.apkUrl)
                .header("User-Agent", "IDLIX-Android/${BuildConfig.VERSION_NAME}")
                .build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unduhan APK gagal (HTTP ${response.code}).")
                }
                val input = response.body?.byteStream() ?: throw IOException("File APK kosong.")
                val digest = MessageDigest.getInstance("SHA-256")
                var total = 0L
                var reported = -1
                input.use { source ->
                    partial.outputStream().buffered().use { destination ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val n = source.read(buffer)
                            if (n < 0) break
                            total += n
                            if (total > MAX_APK_BYTES) throw IOException("APK melebihi batas ukuran.")
                            destination.write(buffer, 0, n)
                            digest.update(buffer, 0, n)
                            val percent = ((total * 100) / release.apkSize)
                                .coerceIn(0, 100).toInt()
                            if (percent >= reported + 5) {
                                reported = percent
                                withContext(Dispatchers.Main) { onProgress(percent) }
                            }
                        }
                    }
                }
                if (total != release.apkSize) {
                    throw IOException("Ukuran APK tidak cocok dengan release GitHub.")
                }
                val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
                if (release.sha256 != null &&
                    !actualSha.equals(release.sha256, ignoreCase = true)
                ) {
                    throw IOException("Checksum APK tidak cocok. Instalasi dibatalkan.")
                }
                partial.inputStream().use { stream ->
                    val header = ByteArray(4)
                    if (stream.read(header) != 4 ||
                        !header.contentEquals(byteArrayOf(0x50, 0x4b, 0x03, 0x04))
                    ) throw IOException("Unduhan bukan paket APK yang valid.")
                }
                if (!partial.renameTo(target)) {
                    throw IOException("Gagal menyimpan paket pembaruan.")
                }
            }
            withContext(Dispatchers.Main) { onProgress(100) }
            FileProvider.getUriForFile(
                context, "${context.packageName}.updates", target
            )
        } finally {
            partial.delete()
        }
    }

    fun openInstaller(context: Context, uri: Uri) {
        val install = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply {
                clipData = ClipData.newUri(context.contentResolver, "IDLIX update", uri)
            }
        context.startActivity(install)
    }
}
