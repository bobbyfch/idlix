package com.idlix.tv.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.idlix.tv.BuildConfig
import com.idlix.tv.data.update.AppUpdater
import com.idlix.tv.data.update.GithubRelease
import kotlinx.coroutines.launch

/**
 * Explicit GitHub release check / download / OS-controlled installer.
 * This does not install silently, request storage access, or use Device Admin.
 */
@Composable
fun UpdateChecker(tv: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var release by remember { mutableStateOf<GithubRelease?>(null) }
    var readyToInstall by remember { mutableStateOf<Uri?>(null) }
    var progress by remember { mutableIntStateOf(-1) }
    var message by remember { mutableStateOf("Periksa update dari GitHub Release resmi.") }

    fun launchInstaller(uri: Uri) {
        try {
            AppUpdater.openInstaller(context, uri)
            message = "Konfirmasikan pemasangan melalui installer Android."
        } catch (e: Exception) {
            message = "Installer tidak tersedia: ${e.localizedMessage ?: "buka APK secara manual"}"
        }
    }

    val unknownSourcesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val uri = readyToInstall
        if (uri != null && context.packageManager.canRequestPackageInstalls()) {
            launchInstaller(uri)
        } else {
            message = "Izin instalasi belum diberikan. Pilih Pasang Update lagi setelah diizinkan."
        }
    }

    fun installOrRequestPermission(uri: Uri) {
        readyToInstall = uri
        if (!context.packageManager.canRequestPackageInstalls()) {
            message = "Android perlu izin 'Instal aplikasi tidak dikenal' untuk IDLIX."
            try {
                unknownSourcesLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:${context.packageName}"))
                )
            } catch (e: Exception) {
                message = "Buka Pengaturan Android lalu izinkan pemasangan APK oleh IDLIX."
            }
        } else {
            launchInstaller(uri)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (tv) {
            androidx.tv.material3.Text(
                "Pembaruan IDLIX", color = androidx.compose.ui.graphics.Color.White
            )
            androidx.tv.material3.Text(
                "Terpasang: v${BuildConfig.VERSION_NAME}",
                color = androidx.compose.ui.graphics.Color.LightGray
            )
            androidx.tv.material3.Text(
                message, color = androidx.compose.ui.graphics.Color.LightGray
            )
        } else {
            androidx.compose.material3.Text(
                "Pembaruan IDLIX",
                color = androidx.compose.ui.graphics.Color.White
            )
            androidx.compose.material3.Text(
                "Terpasang: v${BuildConfig.VERSION_NAME}",
                color = androidx.compose.ui.graphics.Color.LightGray
            )
            androidx.compose.material3.Text(
                message, color = androidx.compose.ui.graphics.Color.LightGray
            )
        }

        if (tv) {
            androidx.tv.material3.Button(
                onClick = {
                    busy = true
                    message = "Memeriksa GitHub..."
                    scope.launch {
                        try {
                            val latest = AppUpdater.checkLatest()
                            release = if (
                                AppUpdater.isNewer(
                                    latest, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME
                                )
                            ) latest else null
                            readyToInstall = null
                            progress = -1
                            message = if (release != null) {
                                "Versi ${latest.version} tersedia · ${latest.apkSize / 1024 / 1024} MB."
                            } else "IDLIX sudah versi terbaru (v${BuildConfig.VERSION_NAME})."
                        } catch (e: Exception) {
                            message = e.localizedMessage ?: "Tidak bisa memeriksa pembaruan."
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                androidx.tv.material3.Text("Check for Update")
            }
        } else {
            androidx.compose.material3.Button(
                onClick = {
                    busy = true
                    message = "Memeriksa GitHub..."
                    scope.launch {
                        try {
                            val latest = AppUpdater.checkLatest()
                            release = if (
                                AppUpdater.isNewer(
                                    latest, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME
                                )
                            ) latest else null
                            readyToInstall = null
                            progress = -1
                            message = if (release != null) {
                                "Versi ${latest.version} tersedia · ${latest.apkSize / 1024 / 1024} MB."
                            } else "IDLIX sudah versi terbaru (v${BuildConfig.VERSION_NAME})."
                        } catch (e: Exception) {
                            message = e.localizedMessage ?: "Tidak bisa memeriksa pembaruan."
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy
            ) { androidx.compose.material3.Text("Check for Update") }
        }

        val version = release
        if (version != null && !busy) {
            val action: () -> Unit = {
                val cached = readyToInstall
                if (cached != null) {
                    installOrRequestPermission(cached)
                } else {
                    busy = true
                    progress = 0
                    message = "Mengunduh v${version.version} dari GitHub..."
                    scope.launch {
                        try {
                            val uri = AppUpdater.download(context, version) { value ->
                                progress = value
                                message = "Mengunduh IDLIX v${version.version}: $value%"
                            }
                            progress = 100
                            installOrRequestPermission(uri)
                        } catch (e: Exception) {
                            message = e.localizedMessage ?: "Unduhan gagal."
                            progress = -1
                        } finally {
                            busy = false
                        }
                    }
                }
            }
            if (tv) {
                androidx.tv.material3.Button(onClick = action) {
                    androidx.tv.material3.Text(
                        if (readyToInstall != null) "Pasang Update"
                        else "Download & Update v${version.version}"
                    )
                }
            } else {
                androidx.compose.material3.Button(onClick = action) {
                    androidx.compose.material3.Text(
                        if (readyToInstall != null) "Pasang Update"
                        else "Download & Update v${version.version}"
                    )
                }
            }
        }
        if (busy && progress >= 0) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
