package com.idlix.tv.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.idlix.tv.data.local.AppColorMode
import com.idlix.tv.data.local.TvPreferencesStore

/** One palette source shared by Android TV, mobile and tablet. */
data class ResolvedAppearance(val scheme: ColorScheme, val light: Boolean)

@Composable
fun resolveIdlixAppearance(): ResolvedAppearance {
    val context = LocalContext.current
    val preferences by TvPreferencesStore.preferences.collectAsState()
    val systemDark = isSystemInDarkTheme()
    // Material You is a separate opt-in. When enabled the SYSTEM decides
    // light/dark and wallpaper colors; the fixed theme is preserved underneath.
    val mode = if (preferences.materialYouEnabled) {
        if (systemDark) AppColorMode.DARK else AppColorMode.LIGHT
    } else preferences.colorMode
    val light = mode == AppColorMode.LIGHT

    if (preferences.materialYouEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return ResolvedAppearance(
            if (systemDark) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context),
            light = !systemDark
        )
    }

    val scheme = when (mode) {
        AppColorMode.LIGHT -> lightColorScheme(
            primary = Color(0xFF6650A4),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEADDFF),
            onPrimaryContainer = Color(0xFF21005D),
            secondary = Color(0xFF625B71),
            onSecondary = Color.White,
            background = Color(0xFFF7F6FA),
            onBackground = Color(0xFF211F25),
            surface = Color(0xFFFFFBFF),
            onSurface = Color(0xFF211F25),
            surfaceVariant = Color(0xFFE7E1EC),
            onSurfaceVariant = Color(0xFF49454F)
        )
        AppColorMode.DARK -> darkColorScheme(
            primary = Color(0xFFD0BCFF),
            onPrimary = Color(0xFF381E72),
            primaryContainer = Color(0xFF4F378B),
            onPrimaryContainer = Color(0xFFEADDFF),
            secondary = Color(0xFFCCC2DC),
            onSecondary = Color(0xFF332D41),
            background = Color(0xFF101014),
            onBackground = Color.White,
            surface = Color(0xFF201F24),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF302D36),
            onSurfaceVariant = Color(0xFFCAC4D0)
        )
        AppColorMode.OLED -> darkColorScheme(
            primary = Color(0xFFE50924),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF821124),
            onPrimaryContainer = Color(0xFFFFD9DF),
            secondary = Color(0xFFFFA9B5),
            onSecondary = Color(0xFF43000B),
            background = Color.Black,
            onBackground = Color.White,
            surface = Color(0xFF141317),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF282126),
            onSurfaceVariant = Color(0xFFD3C4C9)
        )
    }
    return ResolvedAppearance(scheme, light)
}
