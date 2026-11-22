package com.absolutemango.htmlgom

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.absolutemango.htmlgom.core.monet.M3SpecVersion
import com.absolutemango.htmlgom.core.monet.WallpaperColorExtractor
import com.absolutemango.htmlgom.core.monet.getPrimary
import com.absolutemango.htmlgom.core.monet.getSecondary
import com.absolutemango.htmlgom.core.monet.getTertiary
import com.absolutemango.htmlgom.core.monet.getPrimaryContainer
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeContent
import com.google.android.material.color.utilities.SchemeExpressive
import com.google.android.material.color.utilities.SchemeFidelity
import com.google.android.material.color.utilities.SchemeFruitSalad
import com.google.android.material.color.utilities.SchemeMonochrome
import com.google.android.material.color.utilities.SchemeNeutral
import com.google.android.material.color.utilities.SchemeRainbow
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.SchemeVibrant
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

data class DynamicSchemeCardItem(
    val name: String,
    val description: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val primaryContainer: Color
)

@Composable
fun ThemePageScreen(
    onBackClick: () -> Unit,
    activeSchemeName: String,
    activeSpecVersion: M3SpecVersion = M3SpecVersion.SPEC_2025_EXPRESSIVE,
    onSpecVersionChanged: (M3SpecVersion) -> Unit = {},
    onSchemeSelected: (String, Offset) -> Unit,
    backdrop: Backdrop = rememberLayerBackdrop()
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    var dropdownExpanded by remember { mutableStateOf(false) }

    // 1. Extract Seed Color from live wallpaper / system_accent1_500 in HCT color space
    val seedArgb = remember(isDark) { WallpaperColorExtractor.getPrimaryWallpaperSeedArgb(context) }
    val seedHex = remember(seedArgb) { WallpaperColorExtractor.formatHex(seedArgb) }
    val seedHct = remember(seedArgb) { Hct.fromInt(seedArgb) }

    // 2. Run MCU Scheme Algorithm and bind preview dots dynamically
    val schemes = remember(seedHct, activeSpecVersion, isDark) {
        val contrast = if (activeSpecVersion == M3SpecVersion.SPEC_2025_EXPRESSIVE) 0.05 else 0.0
        val systemScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else null

        listOf(
            // Dynamic Monet (Native OS)
            if (systemScheme != null) {
                DynamicSchemeCardItem(
                    name = "Dynamic Monet",
                    description = "Native Android 12+ System Monet Accent",
                    primary = systemScheme.primary,
                    secondary = systemScheme.secondary,
                    tertiary = systemScheme.tertiary,
                    primaryContainer = systemScheme.primaryContainer
                )
            } else {
                val s = SchemeTonalSpot(seedHct, isDark, contrast)
                DynamicSchemeCardItem("Dynamic Monet", "Native Android 12+ System Monet Accent", Color(s.getPrimary()), Color(s.getSecondary()), Color(s.getTertiary()), Color(s.getPrimaryContainer()))
            },
            // SchemeTonalSpot
            run {
                val s = SchemeTonalSpot(seedHct, isDark, contrast)
                DynamicSchemeCardItem("Tonal Spot", "Default balanced Material 3 tones", Color(s.getPrimary()), Color(s.getSecondary()), Color(s.getTertiary()), Color(s.getPrimaryContainer()))
            },
            // SchemeNeutral
            run {
                val s = SchemeNeutral(seedHct, isDark, contrast)
                DynamicSchemeCardItem("Neutral", "Subtle greys and slate tones", Color(s.getPrimary()), Color(s.getSecondary()), Color(s.getTertiary()), Color(s.getPrimaryContainer()))
            },
            // SchemeVibrant
            run {
                val s = SchemeVibrant(seedHct, isDark, contrast)
                DynamicSchemeCardItem("Vibrant", "High saturation vivid primary accents", Color(s.getPrimary()), Color(s.getSecondary()), Color(s.getTertiary()), Color(s.getPrimaryContainer()))
            },
            // SchemeExpressive
            run {
                val s = SchemeExpressive(seedHct, isDark, contrast)
                DynamicSchemeCardItem("Expressive", "Playful contrasting primary & tertiary", Color(s.getPrimary()), Color(s.getSecondary()), Color(s.getTertiary()), Color(s.getPrimaryContainer()))
            },
            // SchemeFidelity
            run {
                val s = SchemeFidelity(seedHct, isDark, contrast)
                DynamicSchemeCardItem("Fidelity", "Matches source wallpaper color fidelity", Color(s.getPrimary()), Color(s.getSecondary()), Color(s.getTertiary()), Color(s.getPrimaryContainer()))
            },
            // SchemeContent
            run {
                val s = SchemeContent(seedHct, isDark, contrast)
                DynamicSchemeCardItem("Content", "Content-driven dominant tones", Color(s.getPrimary()), Color(s.getSecondary()), Color(s.getTertiary()), Color(s.getPrimaryContainer()))
            },
            // SchemeMonochrome
            run {
                val s = SchemeMonochrome(seedHct, isDark, contrast)
                DynamicSchemeCardItem("Monochrome", "High contrast black, white & grey", Color(s.getPrimary()), Color(s.getSecondary()), Color(s.getTertiary()), Color(s.getPrimaryContainer()))
            },
            // SchemeRainbow
            run {
                val s = SchemeRainbow(seedHct, isDark, contrast)
                DynamicSchemeCardItem("Rainbow", "Full spectrum vibrant gradient accents", Color(s.getPrimary()), Color(s.getSecondary()), Color(s.getTertiary()), Color(s.getPrimaryContainer()))
            },
            // SchemeFruitSalad
            run {
                val s = SchemeFruitSalad(seedHct, isDark, contrast)
                DynamicSchemeCardItem("Fruit Salad", "Fresh citrus green & orange accents", Color(s.getPrimary()), Color(s.getSecondary()), Color(s.getTertiary()), Color(s.getPrimaryContainer()))
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dynamic Monet Themes",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "System Monet color engine • Seed: $seedHex",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(seedArgb))
                                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Palette Grid with Spec Version Dropdown Header
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Dropdown Card spanned across both columns
                item(span = { GridItemSpan(2) }) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ColorLens,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Theming Specification",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = activeSpecVersion.displayName,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Box {
                                    FilledTonalButton(
                                        onClick = { dropdownExpanded = true },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (activeSpecVersion == M3SpecVersion.SPEC_2025_EXPRESSIVE) "2025 Expressive" else "2023 Baseline",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select version",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    Text(
                                                        text = M3SpecVersion.SPEC_2025_EXPRESSIVE.displayName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        text = M3SpecVersion.SPEC_2025_EXPRESSIVE.description,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        lineHeight = 13.sp
                                                    )
                                                }
                                            },
                                            onClick = {
                                                dropdownExpanded = false
                                                onSpecVersionChanged(M3SpecVersion.SPEC_2025_EXPRESSIVE)
                                            },
                                            leadingIcon = if (activeSpecVersion == M3SpecVersion.SPEC_2025_EXPRESSIVE) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            } else null
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    Text(
                                                        text = M3SpecVersion.SPEC_2023.displayName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        text = M3SpecVersion.SPEC_2023.description,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        lineHeight = 13.sp
                                                    )
                                                }
                                            },
                                            onClick = {
                                                dropdownExpanded = false
                                                onSpecVersionChanged(M3SpecVersion.SPEC_2023)
                                            },
                                            leadingIcon = if (activeSpecVersion == M3SpecVersion.SPEC_2023) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Dynamic Scheme Cards
                items(schemes, key = { it.name }) { scheme ->
                    val isSelected = scheme.name == activeSchemeName
                    var cardCenter by remember { mutableStateOf(Offset.Zero) }

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                val pos = coordinates.positionInRoot()
                                val sz = coordinates.size
                                cardCenter = Offset(pos.x + sz.width / 2f, pos.y + sz.height / 2f)
                            }
                            .clickable {
                                onSchemeSelected(scheme.name, cardCenter)
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = scheme.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Text(
                                text = scheme.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )

                            // 4 Swatch Dynamic Palette Bubbles (primary, secondary, tertiary, primaryContainer)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(scheme.primary)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(scheme.secondary)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(scheme.tertiary)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(scheme.primaryContainer)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
