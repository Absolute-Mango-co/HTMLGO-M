package com.absolutemango.htmlgom.commonMain.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.absolutemango.htmlgom.core.AgslPerformanceGovernor
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

@Composable
fun LiquidContainer(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(32.dp),
    forceDarkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isGovernorEnabled by AgslPerformanceGovernor.isGovernorEnabled
    val enableComplexShaders = remember(context, isGovernorEnabled) { AgslPerformanceGovernor.shouldEnableComplexShaders(context) }
    val isThrottled by AgslPerformanceGovernor.isThermalThrottled
    val isBypassed = isGovernorEnabled && (!enableComplexShaders || isThrottled)

    val isLightTheme = !forceDarkMode && !isSystemInDarkTheme()
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f)
        else Color(0xFF121212).copy(0.4f)

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    if (!isBypassed) {
                        colorControls(
                            brightness = if (isLightTheme) 0.2f else 0f,
                            saturation = 1.5f
                        )
                        vibrancy()
                        blur(if (isLightTheme) 16f.dp.toPx() else 8f.dp.toPx())
                        lens(24f.dp.toPx(), 48f.dp.toPx(), depthEffect = true)
                    } else {
                        blur(2f.dp.toPx())
                    }
                },
                highlight = {
                    if (isLightTheme) Highlight.Plain else Highlight.Ambient.copy(alpha = 0.3f)
                },
                onDrawSurface = {
                    drawRect(containerColor)
                }
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (isLightTheme) Color.White.copy(0.4f) else Color.White.copy(0.15f)
                ),
                shape
            )
            .clip(shape)
    ) {
        content()
    }
}
