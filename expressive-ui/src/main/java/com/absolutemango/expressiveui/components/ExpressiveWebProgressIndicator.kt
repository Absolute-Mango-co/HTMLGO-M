package com.absolutemango.expressiveui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveWebProgressIndicator(
    progress: Float,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    size: Dp = 36.8.dp
) {
    var isVisible by remember { mutableStateOf(false) }

    val shouldBeVisible = isLoading && progress < 1.0f

    LaunchedEffect(shouldBeVisible) {
        if (shouldBeVisible) {
            delay(120L)
            isVisible = true
            delay(8000L)
            isVisible = false
        } else {
            if (isVisible) {
                delay(180L)
            }
            isVisible = false
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "WebProgressSmooth"
    )

    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 10.dp, end = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.82f),
            exit = fadeOut(tween(250, easing = FastOutSlowInEasing)) + scaleOut(targetScale = 0.82f)
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .shadow(
                        elevation = 3.dp,
                        shape = CircleShape,
                        spotColor = Color.Black.copy(alpha = 0.25f),
                        ambientColor = Color.Black.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(size),
                    color = color,
                    trackColor = trackColor
                )
            }
        }
    }
}
