package com.absolutemango.htmlgom

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.absolutemango.htmlgom.commonMain.components.LiquidButton
import com.absolutemango.htmlgom.commonMain.components.LiquidContainer
import com.absolutemango.htmlgom.engine.EngineCapabilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.absolutemango.htmlgom.R
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()
    var cardsHidden by remember { mutableStateOf(false) }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (cardsHidden) 0f else 1f,
        animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
        label = "aboutScrimAlpha"
    )

    // --- GYROSCOPE PARALLAX LOGIC ---
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val scope = rememberCoroutineScope()
    val rotationX = remember { Animatable(0f) }
    val rotationY = remember { Animatable(0f) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                // Reverse mapping: values[0] -> X, values[1] -> Y
                // Also negate X to move image *with* the tilt direction
                val targetX = -event.values[0] * 15f 
                val targetY = event.values[1] * 15f 
                
                scope.launch {
                    rotationX.animateTo(targetX.coerceIn(-45f, 45f), spring(stiffness = Spring.StiffnessLow))
                }
                scope.launch {
                    rotationY.animateTo(targetY.coerceIn(-45f, 45f), spring(stiffness = Spring.StiffnessLow))
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // Create a specific backdrop to capture the About Screen's own background image
    val aboutBackdrop = rememberLayerBackdrop()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // --- LAYER 1: RECORDING LAYER ---
        // We record the Image and Glow into 'aboutBackdrop'
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(aboutBackdrop)
        ) {
            // 1. Full-Screen Background Image (With Gyroscope Parallax)
            Image(
                painter = painterResource(id = R.drawable.bg_about_modern),
                contentDescription = "Background Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Apply Parallax Translation
                        translationX = rotationX.value
                        translationY = rotationY.value
                        // Scale up slightly to prevent seeing edges during tilt
                        scaleX = 1.15f
                        scaleY = 1.15f
                    }
            )

            // 2. Dark Scrim Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Keep scrim slightly offset too for consistency
                        translationX = rotationX.value * 0.2f
                        translationY = rotationY.value * 0.2f
                        alpha = scrimAlpha
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
        }

        // --- LAYER 2: CONSUMING LAYER ---
        // Main Scrollable Content Container (Consumes aboutBackdrop)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Component 1: Top Navigation Header
            FlyDownItem(delayMs = 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidButton(
                        onClick = onBackClick,
                        backdrop = aboutBackdrop,
                        modifier = Modifier.size(48.dp),
                        surfaceColor = Color.White.copy(alpha = 0.05f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Text(
                        text = "About HTMLGOM",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))

                    // Hide Button on Top Right
                    LiquidButton(
                        onClick = { cardsHidden = !cardsHidden },
                        backdrop = aboutBackdrop,
                        modifier = Modifier.size(48.dp),
                        surfaceColor = Color.White.copy(alpha = 0.05f)
                    ) {
                        Icon(
                            imageVector = if (cardsHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Toggle Visibility",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = !cardsHidden,
                enter = fadeIn() + expandVertically() + slideInVertically { it / 2 },
                exit = fadeOut() + shrinkVertically() + slideOutVertically { it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Component 2: Hero Banner Card (LiquidContainer)
                    FlyDownItem(delayMs = 80) {
                        LiquidContainer(
                            backdrop = aboutBackdrop,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            forceDarkMode = true
                        ) {
                            HeroBannerContent(backdrop = aboutBackdrop)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Component 3: App Overview Card (LiquidContainer)
                    FlyDownItem(delayMs = 160) {
                        LiquidContainer(
                            backdrop = aboutBackdrop,
                            modifier = Modifier.fillMaxWidth(),
                            forceDarkMode = true
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Overview",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "HTMLGOM is a high-performance, offline-first Web Application Runner & Workspace Engine built for playing HTML5 games, interactive demos, and web bundles directly on mobile.",
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Component 4: Specs Card (LiquidContainer)
                    FlyDownItem(delayMs = 240) {
                        LiquidContainer(
                            backdrop = aboutBackdrop,
                            modifier = Modifier.fillMaxWidth(),
                            forceDarkMode = true
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Engine Specs",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                SpecRow("Version", "v2.0R - EOL (Build 4)")
                                SpecRow("Renderer", EngineCapabilities.RENDERER_NAME)
                                SpecRow("Graphics", "AGLS-BackdropV5.6")
                                SpecRow("Package", "com.absolutemango.htmlgom")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Component 5: Video Showcase Card (LiquidContainer)
                    FlyDownItem(delayMs = 320) {
                        LiquidContainer(
                            backdrop = aboutBackdrop,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            forceDarkMode = true
                        ) {
                            MutedLoopingVideoPlayer(
                                isVisible = !cardsHidden,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(24.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBannerContent(backdrop: Backdrop) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_htmlgom_title_logo),
                    contentDescription = "App Icon",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(18.dp))
        Column {
            Text(
                text = "HTMLGO-M",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "v2.0R - EOL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        LiquidButton(
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Absolute-Mango-co"))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open GitHub link", Toast.LENGTH_SHORT).show()
                }
            },
            backdrop = backdrop,
            modifier = Modifier.size(48.dp),
            surfaceColor = Color.White.copy(alpha = 0.08f)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_github),
                contentDescription = "GitHub",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun FlyDownItem(
    delayMs: Int,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        isVisible = true
    }

    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else -120f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "flyDownOffsetY"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
        label = "flyDownAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = with(density) { offsetY.dp.toPx() }
                this.alpha = alpha
            }
    ) {
        content()
    }
}

@Composable
private fun MutedLoopingVideoPlayer(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isSurfaceReady by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, isVisible) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                try { mediaPlayer?.pause() } catch (_: Exception) {}
            } else if (event == Lifecycle.Event.ON_RESUME && isVisible) {
                try { mediaPlayer?.start() } catch (_: Exception) {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        if (!isVisible) {
            try { mediaPlayer?.pause() } catch (_: Exception) {}
        } else if (isSurfaceReady) {
            try { mediaPlayer?.start() } catch (_: Exception) {}
        }

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
                                if (isVisible) {
                                    start()
                                }
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
