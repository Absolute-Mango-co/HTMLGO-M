package com.absolutemango.htmlgom.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.absolutemango.htmlgom.core.monet.DynamicSchemeFactory
import com.absolutemango.htmlgom.core.monet.M3SpecVersion
import com.absolutemango.htmlgom.core.monet.WallpaperColorExtractor
import com.absolutemango.htmlgom.core.monet.toColorScheme

@Composable
fun HTMLGOMTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    extraBlack: Boolean = false,
    schemeName: String = "Dynamic Monet",
    specVersion: M3SpecVersion = M3SpecVersion.SPEC_2025_EXPRESSIVE,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val effectiveDark = darkTheme || extraBlack
    val colorScheme = remember(schemeName, effectiveDark, extraBlack, specVersion) {
        if (schemeName == "Dynamic Monet" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val sys = if (effectiveDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (extraBlack && effectiveDark) {
                sys.copy(
                    surface = Color.Black,
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerLow = Color(0xFF0A0A0A),
                    surfaceContainer = Color(0xFF121212),
                    surfaceContainerHigh = Color(0xFF1A1A1A),
                    surfaceContainerHighest = Color(0xFF222222)
                )
            } else {
                sys
            }
        } else {
            val seedColor = WallpaperColorExtractor.getPrimaryWallpaperSeed(context)
            val tokens = DynamicSchemeFactory.createScheme(schemeName, seedColor, isDark = effectiveDark, specVersion = specVersion)
            tokens.toColorScheme(isDark = effectiveDark, isAmoled = extraBlack, specVersion = specVersion)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
