package com.absolutemango.htmlgom

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.absolutemango.htmlgom.commonMain.components.LiquidButton
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val onboardingBackdrop = rememberLayerBackdrop()
    val scrollState = rememberScrollState()

    // Green Cyber-Emerald Theme Palette
    val backgroundColor = Color(0xFF061109)
    val primaryGreen = Color(0xFF00E676)
    val lightMint = Color(0xFF69F0AE)

    var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // --- LAYER 1: BACKDROP RECORDING LAYER ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(onboardingBackdrop)
        ) {
            // Ambient Green Radial Glow Flare at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryGreen.copy(alpha = 0.16f),
                                primaryGreen.copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            radius = 650f
                        )
                    )
            )
        }

        // --- LAYER 2: INTERACTIVE CONTENT LAYER ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Natural Aspect-Ratio Video Showcase (Kept away from status bar & preserving default proportions)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(videoAspectRatio)
            ) {
                FlushLoopingVideoPlayer(
                    onAspectRatioDiscovered = { ratio ->
                        if (ratio > 0f) {
                            videoAspectRatio = ratio
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Multi-Stop Gradient Scrim dissolving the lower edge into the green background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.40f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    backgroundColor.copy(alpha = 0.45f),
                                    backgroundColor.copy(alpha = 0.85f),
                                    backgroundColor
                                )
                            )
                        )
                )
            }

            // 2. Middle Scrollable Introduction Section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Welcome to HTMLGOM",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "The Offline-First Web Platform & Engine",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryGreen,
                    letterSpacing = 0.2.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "HTMLGOM is your high-performance standalone environment for running, developing, and packaging local web apps, games, and tools directly on Android without internet dependencies.",
                    fontSize = 13.5.sp,
                    color = Color.White.copy(alpha = 0.72f),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Feature Highlights with Green Theming
                OnboardingFeatureRow(
                    icon = Icons.Outlined.Layers,
                    title = "Dual Rendering Engines",
                    description = "Freely toggle between hardware-accelerated System WebView and Mozilla GeckoView for maximum web standards fidelity.",
                    accentColor = primaryGreen
                )

                Spacer(modifier = Modifier.height(14.dp))

                OnboardingFeatureRow(
                    icon = Icons.Outlined.Code,
                    title = "Local Offline Workspace",
                    description = "Extract, edit, and launch offline HTML/CSS/JS bundles with zero tracking, external servers, or network overhead.",
                    accentColor = primaryGreen
                )

                Spacer(modifier = Modifier.height(14.dp))

                OnboardingFeatureRow(
                    icon = Icons.Outlined.Watch,
                    title = "Wear OS Companion Sync",
                    description = "Push web applications wirelessly to HTMLGO-W for standalone execution on your wrist.",
                    accentColor = primaryGreen
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. Pinned KYANT0 Liquid Glass Continue Action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                LiquidButton(
                    onClick = onContinue,
                    backdrop = onboardingBackdrop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    surfaceColor = primaryGreen.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = primaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingFeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.65f),
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun FlushLoopingVideoPlayer(
    onAspectRatioDiscovered: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isSurfaceReady by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                try { mediaPlayer?.pause() } catch (_: Exception) {}
            } else if (event == Lifecycle.Event.ON_RESUME) {
                try { mediaPlayer?.start() } catch (_: Exception) {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
            mediaPlayer = null
        }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        isSurfaceReady = true
                        val surface = Surface(surfaceTexture)
                        try {
                            val player = MediaPlayer.create(ctx, R.raw.video_showcase_loop)?.apply {
                                setSurface(surface)
                                setVolume(0f, 0f)
                                isLooping = true

                                val vw = videoWidth
                                val vh = videoHeight
                                if (vw > 0 && vh > 0) {
                                    onAspectRatioDiscovered(vw.toFloat() / vh.toFloat())
                                }
                                setOnVideoSizeChangedListener { _, w, h ->
                                    if (w > 0 && h > 0) {
                                        onAspectRatioDiscovered(w.toFloat() / h.toFloat())
                                    }
                                }
                                start()
                            }
                            mediaPlayer = player
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        isSurfaceReady = false
                        try {
                            mediaPlayer?.pause()
                            mediaPlayer?.setSurface(null)
                        } catch (_: Exception) {}
                        surface.release()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        },
        modifier = modifier
    )
}
