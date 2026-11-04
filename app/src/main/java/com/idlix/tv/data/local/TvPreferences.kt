package com.idlix.tv.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppColorMode { LIGHT, DARK, OLED }

enum class VideoResizeMode {
    FIT,       // Entire source is visible; black bars when aspect ratio differs.
    FILL,      // Stretch to the viewport (distorts source aspect ratio).
    ZOOM,      // Cover the viewport; crop excess edges.
    FIXED_WIDTH,
    FIXED_HEIGHT
}

enum class SubtitlePosition(val bottomPaddingFraction: Float) {
    LOW(0.035f),
    STANDARD(0.085f),
    HIGH(0.16f)
}

enum class SubtitleSize(val scale: Float) {
    SMALL(0.86f),
    STANDARD(1f),
    LARGE(1.18f)
}

/** Applies only to subtitle rendering. No effect on stream selection/resolution. */
enum class SubtitleAppearance {
    SYSTEM,
    OUTLINE,
    SHADOW,
    SOFT_BOX
}

data class TvPreferences(
    val layoutMode: AppLayoutMode = AppLayoutMode.AUTO,
    val colorMode: AppColorMode = AppColorMode.OLED,
    val materialYouEnabled: Boolean = false,
    val glassIntensity: Int = 2,
    val subtitlePosition: SubtitlePosition = SubtitlePosition.STANDARD,
    val subtitleSize: SubtitleSize = SubtitleSize.STANDARD,
    val subtitleAppearance: SubtitleAppearance = SubtitleAppearance.OUTLINE,
    val videoResizeMode: VideoResizeMode = VideoResizeMode.FIT,
    val mobileGlassEnabled: Boolean = false,
    val mobileStatusBarEnabled: Boolean = true
)

/** Shared, persisted TV preferences used by Settings, navigation and the player. */
object TvPreferencesStore {
    private const val PREFS_NAME = "idlix_tv_preferences"
    private const val KEY_LAYOUT_MODE = "app_layout_mode"
    private const val KEY_COLOR_MODE = "app_color_mode"
    private const val KEY_MATERIAL_YOU = "material_you_enabled"
    private const val KEY_GLASS_INTENSITY = "glass_intensity"
    private const val KEY_SUBTITLE_POSITION = "subtitle_position"
    private const val KEY_SUBTITLE_SIZE = "subtitle_size"
    private const val KEY_SUBTITLE_APPEARANCE = "subtitle_appearance"
    private const val KEY_VIDEO_RESIZE_MODE = "video_resize_mode"
    private const val KEY_MOBILE_GLASS = "mobile_glass_enabled"
    private const val KEY_STATUS_BAR = "mobile_status_bar_enabled"
    private const val KEY_GLASS_MIGRATED = "mobile_glass_performance_migrated"

    private val mutablePreferences = MutableStateFlow(TvPreferences())
    val preferences: StateFlow<TvPreferences> = mutablePreferences.asStateFlow()

    @Synchronized
    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Previously this costly blur defaulted to ON. Migrate once to a
        // fast default for existing installs, without blocking later opt-in.
        if (!prefs.getBoolean(KEY_GLASS_MIGRATED, false)) {
            prefs.edit().putBoolean(KEY_MOBILE_GLASS, false)
                .putBoolean(KEY_GLASS_MIGRATED, true).apply()
        }
        val savedColorMode = prefs.getString(KEY_COLOR_MODE, null)
        mutablePreferences.value = TvPreferences(
            layoutMode = enumValueOrDefault(prefs.getString(KEY_LAYOUT_MODE, null), AppLayoutMode.AUTO),
            // Migrate previous five-mode preferences without losing the user's choice.
            colorMode = when (savedColorMode) {
                "IDLIX", "OLED" -> AppColorMode.OLED
                "MATERIAL", "DARK" -> AppColorMode.DARK
                "LIGHT" -> AppColorMode.LIGHT
                else -> AppColorMode.OLED
            },
            materialYouEnabled = prefs.getBoolean(
                KEY_MATERIAL_YOU, savedColorMode == "MATERIAL"
            ),
            glassIntensity = prefs.getInt(KEY_GLASS_INTENSITY, TvPreferences().glassIntensity).coerceIn(1, 3),
            subtitlePosition = enumValueOrDefault(
                prefs.getString(KEY_SUBTITLE_POSITION, null),
                SubtitlePosition.STANDARD
            ),
            subtitleSize = enumValueOrDefault(
                prefs.getString(KEY_SUBTITLE_SIZE, null),
                SubtitleSize.STANDARD
            ),
            subtitleAppearance = enumValueOrDefault(
                prefs.getString(KEY_SUBTITLE_APPEARANCE, null),
                SubtitleAppearance.OUTLINE
            ),
            videoResizeMode = enumValueOrDefault(
                prefs.getString(KEY_VIDEO_RESIZE_MODE, null),
                VideoResizeMode.FIT
            ),
            mobileGlassEnabled = prefs.getBoolean(KEY_MOBILE_GLASS, false),
            mobileStatusBarEnabled = prefs.getBoolean(KEY_STATUS_BAR, true)
        )
    }

    fun setLayoutMode(context: Context, value: AppLayoutMode) = update(context) {
        it.copy(layoutMode = value)
    }

    fun setColorMode(context: Context, value: AppColorMode) = update(context) {
        // Selecting a fixed appearance must actually apply it, not be hidden
        // behind a previously enabled dynamic wallpaper override.
        it.copy(colorMode = value, materialYouEnabled = false)
    }

    fun setMaterialYouEnabled(context: Context, enabled: Boolean) = update(context) {
        it.copy(materialYouEnabled = enabled)
    }

    fun setGlassIntensity(context: Context, value: Int) = update(context) {
        it.copy(glassIntensity = value.coerceIn(1, 3))
    }

    fun setSubtitlePosition(context: Context, value: SubtitlePosition) = update(context) {
        it.copy(subtitlePosition = value)
    }

    fun setSubtitleSize(context: Context, value: SubtitleSize) = update(context) {
        it.copy(subtitleSize = value)
    }

    fun setSubtitleAppearance(context: Context, value: SubtitleAppearance) = update(context) {
        it.copy(subtitleAppearance = value)
    }

    fun setVideoResizeMode(context: Context, value: VideoResizeMode) = update(context) {
        it.copy(videoResizeMode = value)
    }

    fun setMobileGlassEnabled(context: Context, enabled: Boolean) = update(context) {
        it.copy(mobileGlassEnabled = enabled)
    }

    fun setMobileStatusBarEnabled(context: Context, enabled: Boolean) = update(context) {
        it.copy(mobileStatusBarEnabled = enabled)
    }

    private fun update(context: Context, transform: (TvPreferences) -> TvPreferences) {
        val updated = transform(mutablePreferences.value)
        mutablePreferences.value = updated
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAYOUT_MODE, updated.layoutMode.name)
            .putString(KEY_COLOR_MODE, updated.colorMode.name)
            .putBoolean(KEY_MATERIAL_YOU, updated.materialYouEnabled)
            .putInt(KEY_GLASS_INTENSITY, updated.glassIntensity)
            .putString(KEY_SUBTITLE_POSITION, updated.subtitlePosition.name)
            .putString(KEY_SUBTITLE_SIZE, updated.subtitleSize.name)
            .putString(KEY_SUBTITLE_APPEARANCE, updated.subtitleAppearance.name)
            .putString(KEY_VIDEO_RESIZE_MODE, updated.videoResizeMode.name)
            .putBoolean(KEY_MOBILE_GLASS, updated.mobileGlassEnabled)
            .putBoolean(KEY_STATUS_BAR, updated.mobileStatusBarEnabled)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        value?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } } ?: default
}
