package com.absolutemango.htmlgom

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SendToMobile
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.absolutemango.htmlgom.commonMain.components.LiquidButton
import com.absolutemango.htmlgom.commonMain.components.LiquidContainer
import com.google.android.gms.wearable.Wearable
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun WearableConnectionScreen(
    onBackClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var cardsHidden by remember { mutableStateOf(false) }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (cardsHidden) 0f else 1f,
        animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
        label = "wearableScrimAlpha"
    )
    val scope = rememberCoroutineScope()

    var connectedWatchName by remember { mutableStateOf("Checking...") }
    var connectedWatchId by remember { mutableStateOf<String?>(null) }

    fun checkConnection() {
        connectedWatchName = "Checking..."
        scope.launch(Dispatchers.IO) {
            try {
                try {
                    Wearable.getCapabilityClient(context).addLocalCapability("htmlgom_phone_app").await()
                } catch (ignored: Exception) {}

                val capabilityInfo = Wearable.getCapabilityClient(context)
                    .getCapability("htmlgow_wear_app", com.google.android.gms.wearable.CapabilityClient.FILTER_REACHABLE)
                    .await()
                val watchNode = capabilityInfo.nodes.firstOrNull()
                    ?: Wearable.getNodeClient(context).connectedNodes.await().firstOrNull()

                withContext(Dispatchers.Main) {
                    if (watchNode != null) {
                        connectedWatchName = watchNode.displayName
                        connectedWatchId = watchNode.id
                    } else {
                        connectedWatchName = "Not Connected"
                        connectedWatchId = null
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    connectedWatchName = "Error checking status"
                    connectedWatchId = null
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        checkConnection()
    }

    val sendFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && connectedWatchId != null) {
            Toast.makeText(context, "Sending file to watch...", Toast.LENGTH_SHORT).show()
            scope.launch(Dispatchers.IO) {
                try {
                    val fileName = getFileName(context, uri).replace("/", "_")
                    val tempFile = java.io.File(context.cacheDir, "transfer_${System.currentTimeMillis()}_$fileName")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val channelClient = Wearable.getChannelClient(context)
                    val channelPath = "/file_transfer/$fileName"
                    val channel = channelClient.openChannel(connectedWatchId!!, channelPath).await()
                    channelClient.sendFile(channel, Uri.fromFile(tempFile)).await()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "File sent successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to send file: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // --- GYROSCOPE PARALLAX LOGIC ---
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val rotationX = remember { Animatable(0f) }
    val rotationY = remember { Animatable(0f) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
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

    val customBackdrop = rememberLayerBackdrop()

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().layerBackdrop(customBackdrop)) {
            Image(
                painter = painterResource(id = R.drawable.bg_about_modern),
                contentDescription = "Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = rotationX.value
                        translationY = rotationY.value
                        scaleX = 1.15f
                        scaleY = 1.15f
                    }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = rotationX.value * 0.2f
                        translationY = rotationY.value * 0.2f
                        alpha = scrimAlpha
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.60f),
                                Color.Black.copy(alpha = 0.40f),
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )

            WearableAmbientGlowBackground(rotationX.value, rotationY.value)
        }

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

            FlyDownItem(delayMs = 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidButton(
                        onClick = onBackClick,
                        backdrop = customBackdrop,
                        modifier = Modifier.size(48.dp),
                        surfaceColor = Color.White.copy(alpha = 0.05f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Wearable System", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.weight(1f))
                    LiquidButton(
                        onClick = { cardsHidden = !cardsHidden },
                        backdrop = customBackdrop,
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
                    FlyDownItem(delayMs = 80) {
                        LiquidContainer(
                            backdrop = customBackdrop,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            forceDarkMode = true
                        ) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Watch, "Watch Icon", tint = Color.White, modifier = Modifier.size(36.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(18.dp))
                                Column {
                                    Text("HTMLGO-W", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    Text("Status: $connectedWatchName", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (connectedWatchId != null) Color(0xFF00FF88) else Color.White.copy(alpha = 0.6f), letterSpacing = 1.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    FlyDownItem(delayMs = 160) {
                        LiquidContainer(
                            backdrop = customBackdrop,
                            modifier = Modifier.fillMaxWidth(),
                            forceDarkMode = true
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Remote Tools", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                LiquidButton(
                                    onClick = { checkConnection() },
                                    backdrop = customBackdrop,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    surfaceColor = Color.White.copy(alpha = 0.1f)
                                ) {
                                    Text("Check Connection Status", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                LiquidButton(
                                    onClick = { sendFileLauncher.launch(arrayOf("*/*")) },
                                    backdrop = customBackdrop,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    isInteractive = connectedWatchId != null,
                                    surfaceColor = Color.White.copy(alpha = 0.1f)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SendToMobile, null, tint = Color.White)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Send File to Watch", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlyDownItem(delayMs: Int, content: @Composable () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        isVisible = true
    }

    val offsetY by animateFloatAsState(targetValue = if (isVisible) 0f else -120f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow), label = "")
    val alpha by animateFloatAsState(targetValue = if (isVisible) 1f else 0f, animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing), label = "")

    Box(modifier = Modifier.fillMaxWidth().graphicsLayer { translationY = with(density) { offsetY.dp.toPx() }; this.alpha = alpha }) {
        content()
    }
}

@Composable
private fun WearableAmbientGlowBackground(offsetX: Float = 0f, offsetY: Float = 0f) {
    val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")
    val alphaPulse by infiniteTransition.animateFloat(initialValue = 0.12f, targetValue = 0.32f, animationSpec = infiniteRepeatable(animation = tween(durationMillis = 3500, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "")
    val scalePulse by infiniteTransition.animateFloat(initialValue = 0.92f, targetValue = 1.08f, animationSpec = infiniteRepeatable(animation = tween(durationMillis = 4200, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "")
    val primaryColor = Color(0xFF81D4FA) // Blueish glow for wearable
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = alphaPulse
                scaleX = scalePulse
                scaleY = scalePulse
                translationX = offsetX * 0.5f
                translationY = offsetY * 0.5f
            }
            .background(brush = Brush.radialGradient(colors = listOf(primaryColor.copy(alpha = 0.3f), primaryColor.copy(alpha = 0.1f), Color.Transparent)))
    )
}