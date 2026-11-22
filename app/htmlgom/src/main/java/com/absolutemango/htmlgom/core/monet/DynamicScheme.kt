package com.absolutemango.htmlgom.core.monet

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeContent
import com.google.android.material.color.utilities.SchemeExpressive
import com.google.android.material.color.utilities.SchemeFidelity
import com.google.android.material.color.utilities.SchemeFruitSalad
import com.google.android.material.color.utilities.SchemeMonochrome
import com.google.android.material.color.utilities.SchemeNeutral
import com.google.android.material.color.utilities.SchemeRainbow
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.SchemeVibrant

enum class M3SpecVersion(val displayName: String, val description: String) {
    SPEC_2023("Material 3 (2023)", "Baseline Material 3 balanced contrast and standard neutral tones"),
    SPEC_2025_EXPRESSIVE("Material 3 Expressive (2025)", "High-chroma expressive tones, elevated contrast & warm ambient surfaces")
}

private val mdc = MaterialDynamicColors()

fun DynamicScheme.getPrimary(): Int = mdc.primary().getArgb(this)
fun DynamicScheme.getSecondary(): Int = mdc.secondary().getArgb(this)
fun DynamicScheme.getTertiary(): Int = mdc.tertiary().getArgb(this)
fun DynamicScheme.getPrimaryContainer(): Int = mdc.primaryContainer().getArgb(this)

val DynamicScheme.primaryColor: Color get() = Color(getPrimary())
val DynamicScheme.secondaryColor: Color get() = Color(getSecondary())
val DynamicScheme.tertiaryColor: Color get() = Color(getTertiary())
val DynamicScheme.primaryContainerColor: Color get() = Color(getPrimaryContainer())

fun DynamicScheme.toColorScheme(
    isDark: Boolean = this.isDark,
    isAmoled: Boolean = false,
    specVersion: M3SpecVersion = M3SpecVersion.SPEC_2025_EXPRESSIVE
): ColorScheme {
    val bgArgb = mdc.background().getArgb(this)
    val surfaceArgb = mdc.surface().getArgb(this)
    val surfaceDimArgb = mdc.surfaceDim().getArgb(this)
    val cLowest = mdc.surfaceContainerLowest().getArgb(this)
    val cLow = mdc.surfaceContainerLow().getArgb(this)
    val c = mdc.surfaceContainer().getArgb(this)
    val cHigh = mdc.surfaceContainerHigh().getArgb(this)
    val cHighest = mdc.surfaceContainerHighest().getArgb(this)

    val surfaceBg = if (isAmoled && isDark) Color.Black else Color(surfaceArgb)

    return ColorScheme(
        primary = Color(mdc.primary().getArgb(this)),
        onPrimary = Color(mdc.onPrimary().getArgb(this)),
        primaryContainer = Color(mdc.primaryContainer().getArgb(this)),
        onPrimaryContainer = Color(mdc.onPrimaryContainer().getArgb(this)),
        inversePrimary = Color(mdc.inversePrimary().getArgb(this)),

        secondary = Color(mdc.secondary().getArgb(this)),
        onSecondary = Color(mdc.onSecondary().getArgb(this)),
        secondaryContainer = Color(mdc.secondaryContainer().getArgb(this)),
        onSecondaryContainer = Color(mdc.onSecondaryContainer().getArgb(this)),

        tertiary = Color(mdc.tertiary().getArgb(this)),
        onTertiary = Color(mdc.onTertiary().getArgb(this)),
        tertiaryContainer = Color(mdc.tertiaryContainer().getArgb(this)),
        onTertiaryContainer = Color(mdc.onTertiaryContainer().getArgb(this)),

        background = if (isAmoled && isDark) Color.Black else Color(bgArgb),
        onBackground = Color(mdc.onBackground().getArgb(this)),
        surface = surfaceBg,
        onSurface = Color(mdc.onSurface().getArgb(this)),
        surfaceVariant = Color(mdc.surfaceVariant().getArgb(this)),
        onSurfaceVariant = Color(mdc.onSurfaceVariant().getArgb(this)),
        surfaceTint = Color(mdc.surfaceTint().getArgb(this)),
        inverseSurface = Color(mdc.inverseSurface().getArgb(this)),
        inverseOnSurface = Color(mdc.inverseOnSurface().getArgb(this)),

        error = Color(mdc.error().getArgb(this)),
        onError = Color(mdc.onError().getArgb(this)),
        errorContainer = Color(mdc.errorContainer().getArgb(this)),
        onErrorContainer = Color(mdc.onErrorContainer().getArgb(this)),

        outline = Color(mdc.outline().getArgb(this)),
        outlineVariant = Color(mdc.outlineVariant().getArgb(this)),
        scrim = Color(mdc.scrim().getArgb(this)),

        surfaceBright = Color(mdc.surfaceBright().getArgb(this)),
        surfaceDim = if (isAmoled && isDark) Color.Black else Color(surfaceDimArgb),
        surfaceContainerLowest = if (isAmoled && isDark) Color.Black else Color(cLowest),
        surfaceContainerLow = if (isAmoled && isDark) Color(0xFF0A0A0A) else Color(cLow),
        surfaceContainer = if (isAmoled && isDark) Color(0xFF121212) else Color(c),
        surfaceContainerHigh = if (isAmoled && isDark) Color(0xFF1A1A1A) else Color(cHigh),
        surfaceContainerHighest = if (isAmoled && isDark) Color(0xFF222222) else Color(cHighest)
    )
}

object DynamicSchemeFactory {
    fun createScheme(
        styleName: String,
        seedColor: Color,
        isDark: Boolean = false,
        specVersion: M3SpecVersion = M3SpecVersion.SPEC_2025_EXPRESSIVE
    ): DynamicScheme {
        return createSchemeByArgb(
            styleName = styleName,
            seedArgb = seedColor.toArgb(),
            isDark = isDark,
            contrastLevel = 0.0,
            specVersion = specVersion
        )
    }

    fun createSchemeByArgb(
        styleName: String,
        seedArgb: Int,
        isDark: Boolean,
        contrastLevel: Double = 0.0,
        specVersion: M3SpecVersion = M3SpecVersion.SPEC_2025_EXPRESSIVE
    ): DynamicScheme {
        val hct = Hct.fromInt(seedArgb)
        val contrast = if (specVersion == M3SpecVersion.SPEC_2025_EXPRESSIVE) contrastLevel + 0.05 else contrastLevel
        return when (styleName) {
            "Tonal Spot", "Dynamic Monet", "System Default" -> SchemeTonalSpot(hct, isDark, contrast)
            "Neutral" -> SchemeNeutral(hct, isDark, contrast)
            "Vibrant" -> SchemeVibrant(hct, isDark, contrast)
            "Expressive" -> SchemeExpressive(hct, isDark, contrast)
            "Fidelity" -> SchemeFidelity(hct, isDark, contrast)
            "Content" -> SchemeContent(hct, isDark, contrast)
            "Monochrome" -> SchemeMonochrome(hct, isDark, contrast)
            "Rainbow" -> SchemeRainbow(hct, isDark, contrast)
            "Fruit Salad" -> SchemeFruitSalad(hct, isDark, contrast)
            else -> SchemeTonalSpot(hct, isDark, contrast)
        }
    }

    val ALL_STYLES = listOf(
        "Dynamic Monet" to "Native Android 12+ System Monet Accent",
        "Tonal Spot" to "Default balanced Material 3 tones",
        "Neutral" to "Muted, subtle grayscale-leaning accents",
        "Vibrant" to "High saturation vivid primary and tertiary colors",
        "Expressive" to "Playful contrasting hues rotated across color wheel",
        "Fidelity" to "Authentic match to system wallpaper saturation",
        "Content" to "Harmonious content-driven tonal alignment",
        "Monochrome" to "Pure high-contrast grayscale with zero chroma",
        "Rainbow" to "Full spectrum multi-tone gradient accents",
        "Fruit Salad" to "Fresh split-complementary vibrant citrus accents"
    )
}
