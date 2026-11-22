package com.absolutemango.htmlgom

import androidx.compose.ui.graphics.Color
import com.absolutemango.htmlgom.core.monet.DynamicSchemeFactory
import com.absolutemango.htmlgom.core.monet.M3SpecVersion
import com.absolutemango.htmlgom.core.monet.WallpaperColorExtractor
import com.absolutemango.htmlgom.core.monet.toColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MonetThemeEngineTest {

    @Test
    fun testDefaultSeedIsM3BlueNotRed() {
        // DEFAULT_SEED_ARGB should be baseline Material 3 Blue (#0061A4), never red
        val hex = WallpaperColorExtractor.formatHex(WallpaperColorExtractor.DEFAULT_SEED_ARGB)
        assertEquals("#0061A4", hex)
    }

    @Test
    fun testDynamicSchemeFactoryGeneratesSchemesFromMonetSeed() {
        val testMonetSeed = Color(0xFF1B6EF3) // Test system monet blue

        for ((styleName, _) in DynamicSchemeFactory.ALL_STYLES) {
            val scheme = DynamicSchemeFactory.createScheme(
                styleName = styleName,
                seedColor = testMonetSeed,
                isDark = false,
                specVersion = M3SpecVersion.SPEC_2025_EXPRESSIVE
            )
            val colorScheme = scheme.toColorScheme(isDark = false, isAmoled = false)
            assertNotNull("ColorScheme should not be null for $styleName", colorScheme)
            assertNotNull(colorScheme.primary)
            assertNotNull(colorScheme.secondary)
            assertNotNull(colorScheme.tertiary)
        }
    }

    @Test
    fun testHexFormatHelper() {
        assertEquals("#FF0000", WallpaperColorExtractor.formatHex(0xFFFF0000.toInt()))
        assertEquals("#00FF00", WallpaperColorExtractor.formatHex(0xFF00FF00.toInt()))
        assertEquals("#0061A4", WallpaperColorExtractor.formatHex(0xFF0061A4.toInt()))
    }
}
