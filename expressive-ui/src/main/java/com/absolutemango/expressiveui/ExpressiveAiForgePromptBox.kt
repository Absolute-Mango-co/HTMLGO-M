package com.absolutemango.expressiveui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect

/**
 * Rectangular Prompt Box for AI Forge adhering strictly to Blueprint Bricks 1 and 2.
 * Structured as a 100.dp fixed-height rectangular card container with a multiline text area at the top
 * and an integrated peeled capsule send button nestled in the bottom-right corner with spring touch feedback.
 * Back box is rendered with a smooth gradient fade dissolving the chat canvas into the dock.
 * Applies independent IME / navigation bar inset padding, consumes IME insets, and suppresses
 * window decor panning via a BringIntoView responder.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveAiForgePromptBox(
    prompt: String,
    onPromptChange: (String) -> Unit,
    canSend: Boolean,
    isGenerating: Boolean,
    onSend: () -> Unit,
    activeEditFileName: String? = null,
    onClearEditTarget: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    // Insets: Union of IME and navigationBars prevents double-counting navigation bar height
    val dockInsets = WindowInsets.ime.union(WindowInsets.navigationBars)

    // Suppress window decor panning by intercepting Compose bringIntoView requests
    val noOpBringIntoViewResponder = remember {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect = localRect
            override suspend fun bringChildIntoView(localRect: () -> Rect?) {
                // No-op: prevent propagation to ComposeView.requestRectangleOnScreen()
            }
        }
    }

    val surface = MaterialTheme.colorScheme.surface
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh

    val promptBlurStyle = remember(surface, surfaceContainerHigh) {
        HazeBlurStyle {
            blurRadius(20.dp)
            backgroundColor(surface)
            colorEffects(listOf(HazeColorEffect.tint(surfaceContainerHigh.copy(alpha = 0.70f))))
        }
    }

    // Bottom prompt dock with upward gradient fade replacing any solid back box
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        surface.copy(alpha = 0.20f),
                        surface.copy(alpha = 0.55f),
                        surface.copy(alpha = 0.88f),
                        surface.copy(alpha = 0.98f),
                        surface
                    )
                )
            )
            .windowInsetsPadding(dockInsets)
            .consumeWindowInsets(WindowInsets.ime)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
    ) {
        if (!activeEditFileName.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceContainerHigh.copy(alpha = 0.9f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Editing: $activeEditFileName",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (onClearEditTarget != null) {
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onClearEditTarget,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Detach file",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(18.dp))
                .then(
                    if (hazeState != null) {
                        Modifier.hazeBlur(
                            input = HazeInput.Sources(hazeState),
                            style = promptBlurStyle
                        )
                    } else {
                        Modifier.background(surfaceContainerHigh.copy(alpha = 0.85f))
                    }
                )
                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                .bringIntoViewResponder(noOpBringIntoViewResponder)
        ) {
            // Content container
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Upper: Multiline text entry region
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 2.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    if (prompt.isEmpty()) {
                        Text(
                            text = "Ask AI Forge to generate or modify HTML",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    BasicTextField(
                        value = prompt,
                        onValueChange = onPromptChange,
                        modifier = Modifier.fillMaxSize(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        enabled = !isGenerating
                    )
                }

                // Bottom-Right: Peeled capsule send button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (canSend) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        border = BorderStroke(
                            1.dp,
                            if (canSend) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(
                                enabled = canSend,
                                onClick = onSend
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Send",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (canSend) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Prompt",
                                modifier = Modifier.size(13.dp),
                                tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
