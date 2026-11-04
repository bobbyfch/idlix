package com.idlix.tv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.idlix.tv.data.api.ApiClient
import com.idlix.tv.data.local.TvPreferencesStore
import kotlinx.coroutines.Dispatchers

class IdlixTvApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        appContext = this
        TvPreferencesStore.initialize(this)
        // Fail-safe: if a platform renderer throws from the optional blur node,
        // the next launch uses the lightweight Compose material instead.
        // Delegate to Android's original crash handler so logcat remains intact.
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val isHazeCrash = generateSequence(throwable) { it.cause }.any { cause ->
                cause.stackTrace.any { frame ->
                    frame.className.startsWith("dev.chrisbanes.haze.")
                }
            }
            if (isHazeCrash) {
                getSharedPreferences("idlix_tv_preferences", MODE_PRIVATE)
                    .edit()
                    .putBoolean("mobile_glass_enabled", false)
                    .commit()
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(ApiClient.okHttpClient)
            .allowRgb565(true)
            .respectCacheHeaders(false)
            .decoderDispatcher(Dispatchers.IO)
            .fetcherDispatcher(Dispatchers.IO)
            .transformationDispatcher(Dispatchers.IO)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024) // 150 MB
                    .build()
            }
            .crossfade(false)
            .build()
    }

    companion object {
        lateinit var appContext: Application
            private set
    }
}
