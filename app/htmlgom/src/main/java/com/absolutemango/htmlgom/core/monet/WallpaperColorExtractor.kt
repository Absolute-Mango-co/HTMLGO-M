package com.absolutemango.htmlgom.core.monet

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat

/**
 * Extracts the primary system Monet seed color.
 * Prioritizes native Android 12+ (API 31+) Monet system resources (system_accent1_500 and
 * dynamicLightColorScheme/dynamicDarkColorScheme) to adjust faithfully to the user's
 * system-wide Monet theme.
 * Falls back to WallpaperManager.getWallpaperColors(FLAG_SYSTEM) (API 27+) on pre-Android 12
 * devices, and finally to DEFAULT_SEED_ARGB.
 */
object WallpaperColorExtractor {
    const val DEFAULT_SEED_ARGB: Int = 0xFF0061A4.toInt()

    fun getPrimaryWallpaperSeedArgb(context: Context): Int {
        // Priority 1: Android 12+ (API 31+) Native System Monet
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Check canonical Monet primary accent resource (tone 500)
            try {
                val accentArgb = ContextCompat.getColor(context, android.R.color.system_accent1_500)
                if (accentArgb != 0) {
                    return accentArgb
                }
            } catch (_: Throwable) {}

            // Check dynamic light color scheme primary
            try {
                val lightScheme = dynamicLightColorScheme(context)
                val argb = lightScheme.primary.toArgb()
                if (argb != 0) {
                    return argb
                }
            } catch (_: Throwable) {}

            // Check dynamic dark color scheme primary
            try {
                val darkScheme = dynamicDarkColorScheme(context)
                val argb = darkScheme.primary.toArgb()
                if (argb != 0) {
                    return argb
                }
            } catch (_: Throwable) {}
        }

        // Priority 2: WallpaperManager system colors (API 27+) for pre-Android 12 devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                val colors: WallpaperColors? = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                val primary = colors?.primaryColor
                if (primary != null) {
                    val argb = primary.toArgb()
                    if (argb != 0) {
                        return argb
                    }
                }
            } catch (_: Throwable) {}
        }

        return DEFAULT_SEED_ARGB
    }

    fun getPrimaryWallpaperSeed(context: Context): Color {
        return Color(getPrimaryWallpaperSeedArgb(context))
    }

    fun formatHex(colorArgb: Int): String {
        return String.format("#%06X", (0xFFFFFF and colorArgb))
    }
}
