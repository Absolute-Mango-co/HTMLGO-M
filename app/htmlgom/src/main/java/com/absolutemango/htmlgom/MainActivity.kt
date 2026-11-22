package com.absolutemango.htmlgom

import com.absolutemango.expressiveui.ExpressiveMainScreen
import com.absolutemango.expressiveui.ExpressiveAiForgeScreen
import com.absolutemango.expressiveui.components.ExpressiveWebProgressIndicator
import com.absolutemango.htmlgom.core.AgslPerformanceGovernor
import com.absolutemango.htmlgom.core.NativeThermalCore
import kotlinx.coroutines.withContext
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.absolutemango.htmlgom.commonMain.components.*
import com.kyant.backdrop.Backdrop
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.WindowManager
import android.annotation.SuppressLint
import androidx.core.content.edit
import java.util.Locale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.BackEventCompat
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import android.net.Uri
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.core.content.ContextCompat
import com.absolutemango.htmlgom.engine.EngineCapabilities
import com.absolutemango.htmlgom.engine.GeckoHtmlViewer
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.absolutemango.htmlgom.core.monet.DynamicSchemeFactory
import com.absolutemango.htmlgom.core.monet.M3SpecVersion
import com.absolutemango.htmlgom.core.monet.WallpaperColorExtractor
import com.absolutemango.htmlgom.core.monet.toColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.hypot
import kotlin.math.max
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import com.absolutemango.expressiveui.FileSharingHelper
import com.absolutemango.expressiveui.ZipArchiveSystem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.GlassOptics
import dev.chrisbanes.haze.glass.OpticalSizeValue
import androidx.compose.ui.graphics.RectangleShape
import org.intellij.lang.annotations.Language
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.highlight.Highlight
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.ui.graphics.asComposeRenderEffect

enum class DeviceDisplayLayoutState {
    PHONE_PORTRAIT,
    PHONE_LANDSCAPE,
    LARGE_DISPLAY
}

@Composable
fun rememberDeviceDisplayLayoutState(forceVerticalRail: Boolean = false): DeviceDisplayLayoutState {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val dm = context.resources.displayMetrics

    return remember(configuration.screenWidthDp, configuration.screenHeightDp, configuration.orientation, forceVerticalRail) {
        if (forceVerticalRail) {
            DeviceDisplayLayoutState.LARGE_DISPLAY
        } else {
            val xdpi = if (dm.xdpi > 10f) dm.xdpi else dm.densityDpi.toFloat()
            val ydpi = if (dm.ydpi > 10f) dm.ydpi else dm.densityDpi.toFloat()

            val widthInches = dm.widthPixels.toDouble() / xdpi
            val heightInches = dm.heightPixels.toDouble() / ydpi
            val physicalDiagonal = kotlin.math.sqrt(widthInches * widthInches + heightInches * heightInches)

            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val windowWidthDp = configuration.screenWidthDp

            when {
                physicalDiagonal > 7.5 && windowWidthDp >= 600 -> DeviceDisplayLayoutState.LARGE_DISPLAY
                isLandscape -> DeviceDisplayLayoutState.PHONE_LANDSCAPE
                else -> DeviceDisplayLayoutState.PHONE_PORTRAIT
            }
        }
    }
}



class CircularRevealShape(
    private val progress: Float,
    private val center: Offset
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val maxRadius = hypot(
            max(center.x, size.width - center.x).toDouble(),
            max(center.y, size.height - center.y).toDouble()
        ).toFloat().coerceAtLeast(1f)
        val currentRadius = maxRadius * progress.coerceIn(0f, 1f)
        val path = androidx.compose.ui.graphics.Path().apply {
            addOval(Rect(center, currentRadius))
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

fun buildM3Scheme(
    schemeName: String,
    isDark: Boolean,
    context: Context,
    extraBlack: Boolean = false,
    specVersion: M3SpecVersion = M3SpecVersion.SPEC_2025_EXPRESSIVE
): ColorScheme {
    if ((schemeName == "Dynamic Monet" || schemeName == "System Default") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val systemScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        return if (extraBlack && isDark) {
            systemScheme.copy(
                surface = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color(0xFF0A0A0A),
                surfaceContainer = Color(0xFF121212),
                surfaceContainerHigh = Color(0xFF1A1A1A),
                surfaceContainerHighest = Color(0xFF222222)
            )
        } else {
            systemScheme
        }
    }

    val seedColor = WallpaperColorExtractor.getPrimaryWallpaperSeed(context)
    val tokens = DynamicSchemeFactory.createScheme(schemeName, seedColor, isDark = isDark, specVersion = specVersion)
    return tokens.toColorScheme(isDark = isDark, isAmoled = extraBlack, specVersion = specVersion)
}

@Composable
fun CircularRevealThemeContainer(
    activeSchemeName: String,
    activeSpecVersion: M3SpecVersion,
    extraBlack: Boolean,
    content: @Composable (triggerReveal: (String, M3SpecVersion, Offset) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isDark = isSystemInDarkTheme() || extraBlack
    val coroutineScope = rememberCoroutineScope()

    var currentSchemeName by remember { mutableStateOf(activeSchemeName) }
    var currentSpecVersion by remember { mutableStateOf(activeSpecVersion) }

    LaunchedEffect(activeSchemeName) {
        currentSchemeName = activeSchemeName
    }
    LaunchedEffect(activeSpecVersion) {
        currentSpecVersion = activeSpecVersion
    }

    val activeColorScheme = remember(currentSchemeName, currentSpecVersion, isDark, extraBlack) {
        buildM3Scheme(currentSchemeName, isDark, context, extraBlack, currentSpecVersion)
    }

    var previousBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var revealCenter by remember { mutableStateOf(Offset.Zero) }
    val animProgress = remember { Animatable(1f) }

    val triggerReveal: (String, M3SpecVersion, Offset) -> Unit = { newScheme, newVersion, center ->
        try {
            if (view.width > 0 && view.height > 0) {
                val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                view.draw(canvas)
                previousBitmap = bmp
            }
        } catch (_: Exception) {
            previousBitmap = null
        }

        revealCenter = if (center != Offset.Zero) center else Offset(view.width / 2f, view.height / 2f)
        currentSchemeName = newScheme
        currentSpecVersion = newVersion

        coroutineScope.launch {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
                )
            )
            previousBitmap?.recycle()
            previousBitmap = null
        }
    }

    MaterialTheme(colorScheme = activeColorScheme) {
        SynchronizeWindowCanvas(canvasColor = activeColorScheme.surfaceContainerLowest)

        Box(modifier = Modifier.fillMaxSize()) {
            content(triggerReveal)

            val progress = animProgress.value
            val bmp = previousBitmap
            if (progress < 1f && bmp != null && !bmp.isRecycled) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                ) {
                    drawImage(bmp.asImageBitmap())

                    val maxRadius = hypot(
                        max(revealCenter.x, size.width - revealCenter.x).toDouble(),
                        max(revealCenter.y, size.height - revealCenter.y).toDouble()
                    ).toFloat().coerceAtLeast(1f)
                    val currentRadius = maxRadius * progress

                    drawCircle(
                        color = Color.Black,
                        radius = currentRadius,
                        center = revealCenter,
                        blendMode = BlendMode.Clear
                    )

                    drawCircle(
                        color = activeColorScheme.primary.copy(alpha = (1f - progress * 0.7f).coerceIn(0f, 1f)),
                        radius = currentRadius,
                        center = revealCenter,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            } else if (progress < 1f && bmp == null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxRadius = hypot(
                        max(revealCenter.x, size.width - revealCenter.x).toDouble(),
                        max(revealCenter.y, size.height - revealCenter.y).toDouble()
                    ).toFloat().coerceAtLeast(1f)
                    val currentRadius = maxRadius * progress
                    val alpha = (1f - progress).coerceIn(0f, 1f)
                    drawCircle(
                        color = activeColorScheme.primary.copy(alpha = alpha * 0.5f),
                        radius = currentRadius,
                        center = revealCenter
                    )
                }
            }
        }
    }
}

@Composable
fun SynchronizeWindowCanvas(canvasColor: Color) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(canvasColor) {
        val context = view.context
        val activity = generateSequence(context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>()
            .firstOrNull()
        val window = activity?.window
        if (window != null) {
            val argb = canvasColor.toArgb()
            window.setBackgroundDrawable(ColorDrawable(argb))
            window.decorView.setBackgroundColor(argb)
            val insetsController = WindowCompat.getInsetsController(window, view)
            val isLight = canvasColor.luminance() > 0.5f
            insetsController.isAppearanceLightStatusBars = isLight
            insetsController.isAppearanceLightNavigationBars = isLight
        }
        onDispose {}
    }
}

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
        buildM3Scheme(schemeName, effectiveDark, context, extraBlack, specVersion)
    }
    MaterialTheme(colorScheme = colorScheme) {
        SynchronizeWindowCanvas(canvasColor = colorScheme.surfaceContainerLowest)
        content()
    }
}

public val UniformCardShape = RoundedCornerShape(24.dp)

enum class ToolSubStage { NONE, COMPRESSOR, DECOMPRESSOR }
enum class ScreenState { MAIN, VIEWER, EDITOR, AI_EDIT, THEME, BROWSER, ABOUT, WEARABLE, AI_FORGE, ONBOARDING, FOLDER_SELECTION }

fun ScreenState.isOverlaySubscreen(): Boolean = this in setOf(
    ScreenState.ABOUT,
    ScreenState.WEARABLE,
    ScreenState.EDITOR,
    ScreenState.AI_FORGE,
    ScreenState.THEME,
    ScreenState.FOLDER_SELECTION,
    ScreenState.VIEWER,
    ScreenState.BROWSER
)

data class LastOpenedFileItem(
    val id: String,
    val name: String,
    val sizeText: String,
    val dateText: String,
    val uri: Uri
)

class MainActivity : ComponentActivity() {
    private val incomingUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AgslPerformanceGovernor.initialize(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val prefs = getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("extra_black", false)) {
            window.setBackgroundDrawable(ColorDrawable(AndroidColor.BLACK))
            window.decorView.setBackgroundColor(AndroidColor.BLACK)
        }
        handleIntent(intent)
        window.decorView.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val currentDisplay = display
                val maxMode = currentDisplay?.supportedModes?.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    val lp = window.attributes
                    lp.preferredDisplayModeId = maxMode.modeId
                    lp.preferredRefreshRate = maxMode.refreshRate
                    window.attributes = lp
                }
            }
        }
        setContent {
            val prefs = getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE)
            val extraBlack = remember { mutableStateOf(prefs.getBoolean("extra_black", false)) }
            val savedScheme = prefs.getString("m3_palette_scheme", "Dynamic Monet") ?: "Dynamic Monet"
            val activeScheme = remember { mutableStateOf(savedScheme) }
            val savedSpecName = prefs.getString("m3_theme_spec_version", M3SpecVersion.SPEC_2025_EXPRESSIVE.name)
            val activeSpecVersion = remember {
                mutableStateOf(
                    if (savedSpecName == M3SpecVersion.SPEC_2023.name) M3SpecVersion.SPEC_2023 else M3SpecVersion.SPEC_2025_EXPRESSIVE
                )
            }

            CircularRevealThemeContainer(
                activeSchemeName = activeScheme.value,
                activeSpecVersion = activeSpecVersion.value,
                extraBlack = extraBlack.value
            ) { triggerReveal ->
                MainAppScreen(
                    extraBlack = extraBlack.value,
                    onExtraBlackChange = {
                        extraBlack.value = it
                        prefs.edit { putBoolean("extra_black", it) }
                    },
                    activeSchemeName = activeScheme.value,
                    activeSpecVersion = activeSpecVersion.value,
                    onThemeChangeRequested = { newScheme, offset ->
                        activeScheme.value = newScheme
                        prefs.edit().putString("m3_palette_scheme", newScheme).apply()
                        triggerReveal(newScheme, activeSpecVersion.value, offset)
                    },
                    onSpecVersionChangeRequested = { newVersion, offset ->
                        activeSpecVersion.value = newVersion
                        prefs.edit().putString("m3_theme_spec_version", newVersion.name).apply()
                        triggerReveal(activeScheme.value, newVersion, offset)
                    },
                    initialHtmlUri = incomingUri.value
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            incomingUri.value = intent.data
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PredictiveBackWrapper(
    onBack: () -> Unit,
    onGestureCommit: () -> Unit = {},
    onGestureCancel: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val translationFraction = remember { Animatable(0f) }
    PredictiveBackHandler { progress ->
        try {
            progress.collect { backEvent ->
                // Interactive peek during touch drag: max 35% of width
                translationFraction.snapTo(backEvent.progress * 0.35f)
            }
            // Gesture committed: notify parent container to suppress duplicate transition
            onGestureCommit()
            // Animate smoothly from current peek position to 100% offscreen
            translationFraction.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
            )
            onBack()
        } catch (e: CancellationException) {
            // Gesture aborted: notify parent and spring back smoothly to rest position
            onGestureCancel()
            translationFraction.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = translationFraction.value * size.width
            }
    ) {
        content()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MainAppScreen(
    extraBlack: Boolean,
    onExtraBlackChange: (Boolean) -> Unit,
    activeSchemeName: String = "Dynamic Monet",
    activeSpecVersion: M3SpecVersion = M3SpecVersion.SPEC_2025_EXPRESSIVE,
    onThemeChangeRequested: (String, Offset) -> Unit = { _, _ -> },
    onSpecVersionChangeRequested: (M3SpecVersion, Offset) -> Unit = { _, _ -> },
    initialHtmlUri: Uri? = null
) {
    val mContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { mContext.getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE) }
    var disableShaders by remember { mutableStateOf(prefs.getBoolean("disable_shaders", false)) }
    var isEngineSwitching by remember { mutableStateOf(false) }

    fun handleEngineToggle(newValue: Boolean) {
        coroutineScope.launch {
            isEngineSwitching = true
            delay(1000L)
            disableShaders = newValue
            prefs.edit { putBoolean("disable_shaders", newValue) }
            isEngineSwitching = false
        }
    }

    if (isEngineSwitching) {
        Material3ExpressiveLoadingScreen(
            message = "Switching Rendering Engine...",
            subtitle = if (disableShaders) "Enabling Backdrop Shaders & Blur" else "Applying Solid Gradient Mode"
        )
    } else {
        SharedTransitionLayout {
            if (disableShaders) {
                ExpressiveMainScreen(
                    extraBlack = extraBlack,
                    onExtraBlackChange = onExtraBlackChange,
                    initialHtmlUri = initialHtmlUri,
                    disableShaders = true,
                    onDisableShadersChange = { handleEngineToggle(it) },
                    fileEditorContent = { uri, onBack, onRun ->
                        FileEditorScreen(
                            initialFileUri = uri,
                            onBackClick = onBack,
                            onRunFile = onRun,
                            backdrop = rememberLayerBackdrop()
                        )
                    },
                    aiForgeContent = { onBack, onRun, editUri, editName, onClear ->
                        ExpressiveAiForgeScreen(
                            onBack = onBack,
                            onRunGeneratedHtml = onRun,
                            initialEditUri = editUri,
                            initialEditFileName = editName,
                            onClearEditTarget = onClear
                        )
                    },
                    aiEditContent = { _, _, _, _, _, _, _ -> },
                    emergencyBrowserContent = { onBack ->
                        EmergencyBrowserScreen(onBackClick = onBack, backdrop = rememberLayerBackdrop())
                    },
                    aboutContent = { onBack ->
                        AboutScreen(onBackClick = onBack, backdrop = rememberLayerBackdrop())
                    },
                    themePageContent = { activeScheme, onSchemeSelected, onBack ->
                        ThemePageScreen(
                            activeSchemeName = activeSchemeName,
                            activeSpecVersion = activeSpecVersion,
                            onSpecVersionChanged = { newVersion ->
                                onSpecVersionChangeRequested(newVersion, Offset.Zero)
                            },
                            onSchemeSelected = { newScheme, offset ->
                                onThemeChangeRequested(newScheme, offset)
                                onSchemeSelected(newScheme)
                            },
                            onBackClick = onBack,
                            backdrop = rememberLayerBackdrop()
                        )
                    },
                    wearableConnectionContent = { onBack ->
                        WearableConnectionScreen(onBackClick = onBack, backdrop = rememberLayerBackdrop())
                    },
                    htmlViewerContent = { uri, isDom, isHw, useGecko, onWebViewCreated ->
                        HtmlViewer(
                            fileUri = uri,
                            isDomEnabled = isDom,
                            isHwAccelEnabled = isHw,
                            useGeckoEngine = useGecko,
                            onWebViewCreated = onWebViewCreated
                        )
                    }
                )
            } else {
                MainAppScreenWithBackdrop(
                    extraBlack = extraBlack,
                    onExtraBlackChange = onExtraBlackChange,
                    activeSchemeName = activeSchemeName,
                    activeSpecVersion = activeSpecVersion,
                    onThemeChangeRequested = onThemeChangeRequested,
                    onSpecVersionChangeRequested = onSpecVersionChangeRequested,
                    initialHtmlUri = initialHtmlUri,
                    disableShaders = false,
                    onDisableShadersChange = { handleEngineToggle(it) }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.MainAppScreenWithBackdrop(
    extraBlack: Boolean,
    onExtraBlackChange: (Boolean) -> Unit,
    activeSchemeName: String = "Dynamic Monet",
    activeSpecVersion: M3SpecVersion = M3SpecVersion.SPEC_2025_EXPRESSIVE,
    onThemeChangeRequested: (String, Offset) -> Unit = { _, _ -> },
    onSpecVersionChangeRequested: (M3SpecVersion, Offset) -> Unit = { _, _ -> },
    initialHtmlUri: Uri? = null,
    disableShaders: Boolean,
    onDisableShadersChange: (Boolean) -> Unit
) {
    val mContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { mContext.getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE) }
    val rootBackdrop = rememberLayerBackdrop()
    val uiBackdrop = rememberLayerBackdrop()
    var activeToolSubStage by remember { mutableStateOf(ToolSubStage.NONE) }
    var showAboutScreen by remember { mutableStateOf(false) }
    var showAiScreen by remember { mutableStateOf(false) }
    var showEmergencyBrowser by remember { mutableStateOf(false) }
    var showFileEditor by remember { mutableStateOf(false) }
    var showThemePage by remember { mutableStateOf(false) }
    var showWearableConnection by remember { mutableStateOf(false) }
    var hasCompletedOnboarding by remember { mutableStateOf(prefs.getBoolean("onboarding_completed", false)) }
    var showOnboardingScreen by remember { mutableStateOf(false) }
    var domStorageEnabled by remember { mutableStateOf(prefs.getBoolean("dom_storage", true)) }
    var hwAccelerationEnabled by remember { mutableStateOf(prefs.getBoolean("hw_acceleration", true)) }
    var useGeckoEngine by remember { mutableStateOf(prefs.getBoolean("use_gecko_engine", true)) }
    var forceVerticalRail by remember { mutableStateOf(prefs.getBoolean("force_vertical_rail", false)) }
    val savedFolderUriString = remember { prefs.getString("permitted_folder_uri", null) }
    var permittedFolderUri by remember { mutableStateOf(savedFolderUriString?.toUri()) }
    var selectedHtmlUri by remember { mutableStateOf(initialHtmlUri) }
    var fileEditorTargetUri by remember { mutableStateOf<Uri?>(null) }
    var aiForgeEditUri by remember { mutableStateOf<Uri?>(null) }
    var aiForgeEditName by remember { mutableStateOf<String?>(null) }
    var isHistoryLoaded by remember { mutableStateOf(false) }
    var lastOpenedList by remember { mutableStateOf<List<LastOpenedFileItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        val diskItems = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            loadLastOpenedFiles(mContext)
        }
        lastOpenedList = (lastOpenedList + diskItems).distinctBy { it.id }
        isHistoryLoaded = true
    }

    var selectedToolFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isGestureExitActive by remember { mutableStateOf(false) }
    val handleGestureCommit = { isGestureExitActive = true }
    val handleGestureCancel = { isGestureExitActive = false }

    fun updateAndPersistLastOpened(newList: List<LastOpenedFileItem>) {
        lastOpenedList = newList
        saveLastOpenedFiles(mContext, newList)
    }

    fun routeUriThroughThermalCore(uri: Uri, name: String, onResult: (Uri) -> Unit) {
        if (name.endsWith(".zip", ignoreCase = true)) {
            val tempDir = File(mContext.cacheDir, "extracted_${System.currentTimeMillis()}")
            if (NativeThermalCore.isLoaded) {
                val pfd = try { mContext.contentResolver.openFileDescriptor(uri, "r") } catch (_: Exception) { null }
                if (pfd != null) {
                    coroutineScope.launch {
                        val count = NativeThermalCore.extractZipInteractive(pfd, tempDir)
                        try { pfd.close() } catch (_: Exception) {}
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (count > 0) {
                                val htmlFile = ZipArchiveSystem.findHtmlFile(tempDir)
                                if (htmlFile != null && htmlFile.isFile) {
                                    onResult(Uri.fromFile(htmlFile))
                                } else {
                                    Toast.makeText(mContext, "ZIP Extracted ($count files)", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val fallbackHtml = decompressZipToDir(mContext, uri, tempDir)
                                if (fallbackHtml != null && fallbackHtml.isFile) {
                                    onResult(Uri.fromFile(fallbackHtml))
                                } else {
                                    Toast.makeText(mContext, "ZIP Extracted successfully!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    return
                }
            }
            val htmlFile = decompressZipToDir(mContext, uri, tempDir)
            if (htmlFile != null && htmlFile.isFile) {
                onResult(Uri.fromFile(htmlFile))
            } else {
                Toast.makeText(mContext, "ZIP Extracted successfully!", Toast.LENGTH_SHORT).show()
            }
        } else if (name.endsWith(".mhtml", ignoreCase = true) || name.endsWith(".mht", ignoreCase = true)) {
            val tempDir = File(mContext.cacheDir, "mhtml_${System.currentTimeMillis()}")
            val pfd = try { mContext.contentResolver.openFileDescriptor(uri, "r") } catch (_: Exception) { null }
            if (pfd != null) {
                coroutineScope.launch(NativeThermalCore.ThermalWorkerDispatcher) {
                    val count = NativeThermalCore.parseMhtml(pfd, pfd.statSize, tempDir)
                    try { pfd.close() } catch (_: Exception) {}
                    val htmlFile = File(tempDir, "index.html")
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (count > 0 && htmlFile.exists()) {
                            onResult(Uri.fromFile(htmlFile))
                        } else {
                            onResult(uri)
                        }
                    }
                }
            } else {
                onResult(uri)
            }
        } else {
            onResult(uri)
        }
    }

    // Handle changes to initialHtmlUri (e.g. from onNewIntent or startup)
    LaunchedEffect(initialHtmlUri) {
        if (initialHtmlUri != null) {
            // Also add to last opened if it's a new external file
            try {
                mContext.contentResolver.takePersistableUriPermission(initialHtmlUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            
            val fileName = getFileName(mContext, initialHtmlUri)
            val newItem = LastOpenedFileItem(
                id = System.currentTimeMillis().toString(), 
                name = fileName, 
                sizeText = "External", 
                dateText = SimpleDateFormat("dd.MM.yy", Locale.US).format(Date()), 
                uri = initialHtmlUri
            )
            val currentList = if (isHistoryLoaded) lastOpenedList else loadLastOpenedFiles(mContext)
            updateAndPersistLastOpened(listOf(newItem) + currentList.filterNot { it.name == fileName })

            routeUriThroughThermalCore(initialHtmlUri, fileName) { targetUri ->
                selectedHtmlUri = targetUri
            }
        }
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })

    LaunchedEffect(pagerState.currentPage) { activeToolSubStage = ToolSubStage.NONE }

    fun updatePref(key: String, value: Boolean) { prefs.edit { putBoolean(key, value) } }

    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            mContext.contentResolver.takePersistableUriPermission(uri, takeFlags)
            permittedFolderUri = uri
            prefs.edit { putString("permitted_folder_uri", uri.toString()) }
            Toast.makeText(mContext, "Directory permission granted!", Toast.LENGTH_SHORT).show()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { mContext.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            val fileName = getFileName(mContext, uri)
            val newItem = LastOpenedFileItem(id = System.currentTimeMillis().toString(), name = fileName, sizeText = "Local", dateText = SimpleDateFormat("dd.MM.yy", Locale.US).format(Date()), uri = uri)
            updateAndPersistLastOpened(listOf(newItem) + lastOpenedList.filterNot { it.name == fileName })
            Toast.makeText(mContext, "File added: $fileName", Toast.LENGTH_SHORT).show()
            routeUriThroughThermalCore(uri, fileName) { targetUri ->
                selectedHtmlUri = targetUri
            }
        }
    }

    val compressPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> if (uris.isNotEmpty()) selectedToolFiles = uris }
    val decompressPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) selectedToolFiles = listOf(uri) }
    val compressSaveToLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && selectedToolFiles.isNotEmpty()) {
            val resultMsg = compressUrisToZipSAF(mContext, selectedToolFiles, uri)
            Toast.makeText(mContext, resultMsg, Toast.LENGTH_LONG).show()
            activeToolSubStage = ToolSubStage.NONE
            selectedToolFiles = emptyList()
        }
    }
    val decompressSaveToLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null && selectedToolFiles.isNotEmpty()) {
            mContext.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val resultMsg = decompressZipToFolderSAF(mContext, selectedToolFiles.first(), uri)
            Toast.makeText(mContext, resultMsg, Toast.LENGTH_LONG).show()
            activeToolSubStage = ToolSubStage.NONE
            selectedToolFiles = emptyList()
        }
    }

    fun saveHtmlFromPaste(htmlContent: String): Uri? {
        Toast.makeText(mContext, "Creating project...", Toast.LENGTH_SHORT).show()
        val rootUri = permittedFolderUri ?: return null.also { Toast.makeText(mContext, "Grant folder permission first!", Toast.LENGTH_SHORT).show() }
        try {
            val rootDoc = DocumentFile.fromTreeUri(mContext, rootUri) ?: return null.also { Toast.makeText(mContext, "Cannot write to workspace!", Toast.LENGTH_SHORT).show() }
            val timestamp = SimpleDateFormat("ddMMyy_HHmm", Locale.US).format(Date())
            val folderName = "pasted_$timestamp"
            val fileName = "index_$timestamp.html"
            val newFolder = rootDoc.createDirectory(folderName) ?: return null.also { Toast.makeText(mContext, "Failed to create folder", Toast.LENGTH_SHORT).show() }
            val newFile = newFolder.createFile("text/html", fileName) ?: return null.also { Toast.makeText(mContext, "Failed to create file", Toast.LENGTH_SHORT).show() }
            mContext.contentResolver.openOutputStream(newFile.uri)?.use { it.write(htmlContent.toByteArray()) }

            // If an unpacked multi-file project exists, copy sibling assets into project folder
            try {
                val projectsDir = File(mContext.cacheDir, "ai_forge_projects")
                if (projectsDir.exists()) {
                    val latestSessionDir = projectsDir.listFiles()?.filter { it.isDirectory }?.maxByOrNull { it.lastModified() }
                    val unpackedDir = latestSessionDir?.let { File(it, "unpacked") }
                    if (unpackedDir != null && unpackedDir.exists()) {
                        unpackedDir.walkTopDown().filter { it.isFile && it.name != "index.html" }.forEach { asset ->
                            val mime = when {
                                asset.name.endsWith(".css", true) -> "text/css"
                                asset.name.endsWith(".js", true) -> "application/javascript"
                                asset.name.endsWith(".svg", true) -> "image/svg+xml"
                                asset.name.endsWith(".png", true) -> "image/png"
                                asset.name.endsWith(".json", true) -> "application/json"
                                else -> "application/octet-stream"
                            }
                            val assetDoc = newFolder.createFile(mime, asset.name)
                            if (assetDoc != null) {
                                mContext.contentResolver.openOutputStream(assetDoc.uri)?.use { out ->
                                    asset.inputStream().use { it.copyTo(out) }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            val newItem = LastOpenedFileItem(id = System.currentTimeMillis().toString(), name = fileName, sizeText = "Pasted", dateText = SimpleDateFormat("dd.MM.yy", Locale.US).format(Date()), uri = newFile.uri)
            updateAndPersistLastOpened(listOf(newItem) + lastOpenedList)
            Toast.makeText(mContext, "Saved to $folderName", Toast.LENGTH_SHORT).show()
            return newFile.uri
        } catch (e: Exception) { 
            Toast.makeText(mContext, "Error saving HTML: ${e.message}", Toast.LENGTH_LONG).show() 
            return null
        }
    }

    val canvasColor = MaterialTheme.colorScheme.surfaceContainerLowest

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        AgslPerformanceGovernor.notifyUserInteraction()
                    }
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize().layerBackdrop(rootBackdrop)) {
            Surface(modifier = Modifier.fillMaxSize(), color = canvasColor) {}
        }
        val currentHtmlUri = selectedHtmlUri
        val currentScreenState = when {
            showOnboardingScreen || !hasCompletedOnboarding -> ScreenState.ONBOARDING
            currentHtmlUri != null -> ScreenState.VIEWER
            fileEditorTargetUri != null || showFileEditor -> ScreenState.EDITOR
            showThemePage -> ScreenState.THEME
            showEmergencyBrowser -> ScreenState.BROWSER
            showAboutScreen -> ScreenState.ABOUT
            showWearableConnection -> ScreenState.WEARABLE
            showAiScreen -> ScreenState.AI_FORGE
            permittedFolderUri == null -> ScreenState.FOLDER_SELECTION
            else -> ScreenState.MAIN
        }

        LaunchedEffect(currentScreenState) {
            if (currentScreenState == ScreenState.MAIN) {
                isGestureExitActive = false
            }
        }

        @Composable
        fun MainScreenLayer(isInteractive: Boolean) {
            var isSearchActive by remember { mutableStateOf(false) }
            var searchQuery by remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current

            BackHandler(enabled = isSearchActive) {
                isSearchActive = false
                searchQuery = ""
                focusManager.clearFocus()
            }

            LaunchedEffect(isSearchActive) {
                if (isSearchActive) {
                    delay(200L)
                    focusRequester.requestFocus()
                    keyboardController?.show()
                } else {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            }

            val displayState = rememberDeviceDisplayLayoutState(forceVerticalRail = forceVerticalRail)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (!isInteractive) {
                            Modifier.pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        } else Modifier
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(uiBackdrop)
                        .then(
                            if (displayState == DeviceDisplayLayoutState.LARGE_DISPLAY) {
                                Modifier.padding(end = 88.dp)
                            } else Modifier
                        )
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        HorizontalPager(
                            state = pagerState,
                            beyondViewportPageCount = 1,
                            userScrollEnabled = !isSearchActive,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (page) {
                                1 -> HomeScreen(
                                    lastOpenedList = lastOpenedList, 
                                    onFileClick = { item ->
                                        Toast.makeText(mContext, "Opening ${item.name}...", Toast.LENGTH_SHORT).show()
                                        routeUriThroughThermalCore(item.uri, item.name) { targetUri ->
                                            selectedHtmlUri = targetUri
                                        }
                                    }, 
                                    onDeleteFile = { id -> 
                                        updateAndPersistLastOpened(lastOpenedList.filterNot { it.id == id }) 
                                        Toast.makeText(mContext, "File removed", Toast.LENGTH_SHORT).show()
                                    }, 
                                    onAddFileClick = { filePickerLauncher.launch(arrayOf("*/*")) }, 
                                    onPasteHtml = { saveHtmlFromPaste(it) }, 
                                    onDirectPlay = { text ->
                                        Toast.makeText(mContext, "Launching from clipboard...", Toast.LENGTH_SHORT).show()
                                        val uri = saveHtmlFromPaste(text)
                                        if (uri != null) selectedHtmlUri = uri
                                    },
                                    onEditWithAi = { item ->
                                        aiForgeEditUri = item.uri
                                        aiForgeEditName = item.name
                                        showAiScreen = true
                                    },
                                    onOpenFileEditor = { item ->
                                        fileEditorTargetUri = item.uri
                                    },
                                    backdrop = rootBackdrop,
                                    isSearchActive = isSearchActive,
                                    onSearchActiveChange = { isSearchActive = it },
                                    disableShaders = disableShaders,
                                    displayState = displayState
                                )
                                0 -> ToolsScreen(activeToolSubStage, onSelectSubStage = { activeToolSubStage = it; selectedToolFiles = emptyList() }, selectedToolFiles, onPickFiles = { if (activeToolSubStage == ToolSubStage.COMPRESSOR) compressPickerLauncher.launch(arrayOf("*/*")) else decompressPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*")) }, onExecuteAction = { if (activeToolSubStage == ToolSubStage.COMPRESSOR) compressSaveToLauncher.launch("compressed_${System.currentTimeMillis()}.zip") else decompressSaveToLauncher.launch(null) }, rootBackdrop)
                                2 -> SettingsScreen(
                                    extraBlack = extraBlack,
                                    onExtraBlackChange = onExtraBlackChange,
                                    disableShaders = disableShaders,
                                    onDisableShadersChange = { onDisableShadersChange(it) },
                                    domStorage = domStorageEnabled,
                                    onDomStorageChange = { domStorageEnabled = it; updatePref("dom_storage", it) },
                                    hwAcceleration = hwAccelerationEnabled,
                                    onHwAccelChange = { hwAccelerationEnabled = it; updatePref("hw_acceleration", it) },
                                    useGeckoEngine = useGeckoEngine,
                                    onGeckoEngineChange = { useGeckoEngine = it; updatePref("use_gecko_engine", it) },
                                    permittedFolderUri = permittedFolderUri,
                                    onRequestFolderPermission = { folderPickerLauncher.launch(null) },
                                    onGenerateDummyFiles = {
                                        updateAndPersistLastOpened(generateDummyFiles() + lastOpenedList)
                                        Toast.makeText(mContext, "Generated 35 test files", Toast.LENGTH_SHORT).show()
                                    },
                                    onClearDummyFiles = {
                                        updateAndPersistLastOpened(emptyList())
                                        Toast.makeText(mContext, "Workspace cleared", Toast.LENGTH_SHORT).show()
                                    },
                                    onOpenAbout = { showAboutScreen = true },
                                    onOpenAiForge = { showAiScreen = true },
                                    onOpenEmergencyBrowser = { showEmergencyBrowser = true },
                                    onOpenFileEditor = { showFileEditor = true },
                                    onOpenThemePage = { showThemePage = true },
                                    onOpenWearableConnection = { showWearableConnection = true },
                                    onTriggerOnboarding = { showOnboardingScreen = true },
                                    activeSchemeName = activeSchemeName,
                                    backdrop = rootBackdrop,
                                    forceVerticalRail = forceVerticalRail,
                                    onForceVerticalRailChange = {
                                        forceVerticalRail = it
                                        updatePref("force_vertical_rail", it)
                                    }
                                )
                            }
                        }
                    }
                }
                if (displayState == DeviceDisplayLayoutState.LARGE_DISPLAY) {
                    VerticalLiquidBottomTabs(
                        selectedTabIndex = { pagerState.currentPage },
                        onTabSelected = { targetIndex -> coroutineScope.launch { pagerState.animateScrollToPage(targetIndex) } },
                        backdrop = rememberCombinedBackdrop(rootBackdrop, uiBackdrop),
                        tabsCount = 3,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(end = 20.dp, bottom = 88.dp)
                    ) {
                        VerticalLiquidBottomTab(onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } }) {
                            Icon(painter = painterResource(R.drawable.ic_nav_tools), "Tools", tint = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                            Text("Tools", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        VerticalLiquidBottomTab(onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }) {
                            Icon(painter = painterResource(R.drawable.ic_nav_home), "Home", tint = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                            Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        VerticalLiquidBottomTab(onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } }) {
                            Icon(painter = painterResource(R.drawable.ic_nav_settings), "Settings", tint = if (pagerState.currentPage == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                            Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (pagerState.currentPage == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    val navBarModifier = if (displayState == DeviceDisplayLayoutState.PHONE_LANDSCAPE) {
                        Modifier
                            .align(Alignment.BottomEnd)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .width(296.dp)
                            .padding(bottom = 12.dp, end = 16.dp)
                    } else {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .width(296.dp)
                            .padding(bottom = 12.dp)
                    }
                    LiquidBottomTabs(
                        selectedTabIndex = { pagerState.currentPage }, 
                        onTabSelected = { targetIndex -> coroutineScope.launch { pagerState.animateScrollToPage(targetIndex) } }, 
                        backdrop = rememberCombinedBackdrop(rootBackdrop, uiBackdrop), 
                        tabsCount = 3, 
                        modifier = navBarModifier
                    ) {
                        LiquidBottomTab(onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } }) {
                            Icon(painter = painterResource(R.drawable.ic_nav_tools), "Tools", tint = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                            Text("Tools", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        LiquidBottomTab(onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }) {
                            Icon(painter = painterResource(R.drawable.ic_nav_home), "Home", tint = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                            Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        LiquidBottomTab(onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } }) {
                            Icon(painter = painterResource(R.drawable.ic_nav_settings), "Settings", tint = if (pagerState.currentPage == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                            Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (pagerState.currentPage == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // HOISTED MODAL SEARCH OVERLAY (Full-Screen Scrim, Z-index 2)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSearchActive,
                    enter = fadeIn(tween(250)),
                    exit = fadeOut(tween(200))
                ) {
                    val searchVisibilityScope = this
                    val isDark = isSystemInDarkTheme()
                    val searchCardColor = if (isDark) Color(0xFF121212).copy(0.4f) else Color(0xFFFAFAFA).copy(0.6f)
                    val filteredList = remember(searchQuery, lastOpenedList) {
                        if (searchQuery.isBlank()) emptyList()
                        else lastOpenedList.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable {
                                isSearchActive = false
                                searchQuery = ""
                                focusManager.clearFocus()
                            }
                    ) {
                        val searchCardModifier = if (displayState == DeviceDisplayLayoutState.LARGE_DISPLAY) {
                            Modifier
                                .statusBarsPadding()
                                .padding(top = 16.dp, start = 16.dp, end = 24.dp)
                                .widthIn(max = 400.dp)
                                .fillMaxWidth()
                                .align(Alignment.TopEnd)
                                .padding(vertical = 20.dp)
                        } else {
                            Modifier
                                .statusBarsPadding()
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                                .align(Alignment.TopCenter)
                        }
                        Box(
                            modifier = searchCardModifier
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "search_transform_pill"),
                                    animatedVisibilityScope = searchVisibilityScope,
                                    boundsTransform = { _, _ -> spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow) }
                                )
                                .drawBackdrop(
                                    backdrop = rememberCombinedBackdrop(rootBackdrop, uiBackdrop),
                                    shape = { RoundedCornerShape(28.dp) },
                                    effects = {
                                        vibrancy()
                                        blur(3f.dp.toPx())
                                        lens(
                                            refractionHeight = 24.dp.toPx(),
                                            refractionAmount = 48.dp.toPx(),
                                            depthEffect = true
                                        )
                                    },
                                    highlight = { Highlight.Ambient.copy(alpha = 0.2f) },
                                    onDrawSurface = {
                                        drawRect(searchCardColor)
                                    }
                                )
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(28.dp))
                                .clip(RoundedCornerShape(28.dp))
                                .clickable(enabled = false) {}
                        ) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search files...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f), fontSize = 15.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            if (searchQuery.isNotEmpty()) searchQuery = ""
                                            else {
                                                isSearchActive = false
                                                focusManager.clearFocus()
                                            }
                                        }) { Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurface) }
                                    },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .focusRequester(focusRequester)
                                        .background(MaterialTheme.colorScheme.surfaceContainer.copy(0.3f), CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), CircleShape)
                                )

                                Spacer(Modifier.height(16.dp))
                                if (searchQuery.isNotBlank() && filteredList.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .background(MaterialTheme.colorScheme.surfaceContainer.copy(0.2f), UniformCardShape)
                                            .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)), UniformCardShape)
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Icon(Icons.Outlined.SearchOff, null, tint = MaterialTheme.colorScheme.primary.copy(0.6f), modifier = Modifier.size(38.dp))
                                            Spacer(Modifier.height(8.dp))
                                            Text("no results", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else if (filteredList.isNotEmpty()) {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 320.dp)) {
                                        items(filteredList, key = { it.id }) { item ->
                                            ExpressiveFileCard(
                                                item = item,
                                                onClick = {
                                                    isSearchActive = false
                                                    Toast.makeText(mContext, "Opening ${item.name}...", Toast.LENGTH_SHORT).show()
                                                    routeUriThroughThermalCore(item.uri, item.name) { targetUri ->
                                                        selectedHtmlUri = targetUri
                                                    }
                                                },
                                                onDelete = {
                                                    updateAndPersistLastOpened(lastOpenedList.filterNot { it.id == item.id })
                                                    Toast.makeText(mContext, "File removed", Toast.LENGTH_SHORT).show()
                                                },
                                                onEditWithAi = {
                                                    isSearchActive = false
                                                    aiForgeEditUri = item.uri
                                                    aiForgeEditName = item.name
                                                    showAiScreen = true
                                                },
                                                onOpenFileEditor = {
                                                    isSearchActive = false
                                                    fileEditorTargetUri = item.uri
                                                },
                                                isScrollInProgress = false
                                            )
                                        }
                                    }
                                } else {
                                    Text("Type to search your files...", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.8f), modifier = Modifier.padding(vertical = 12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Layer 1: Permanent Base Layer (always composed, zIndex = 0f)
        Box(modifier = Modifier.fillMaxSize().zIndex(0f)) {
            MainScreenLayer(isInteractive = (currentScreenState == ScreenState.MAIN))
        }

        // Layer 2: Overlay Deck (zIndex = 1f guarantees subscreens draw strictly above Layer 1)
        Box(modifier = Modifier.fillMaxSize().zIndex(1f)) {
            AnimatedContent(
            targetState = currentScreenState,
            transitionSpec = {
                if (targetState == ScreenState.MAIN && isGestureExitActive) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else if (initialState == ScreenState.ONBOARDING || targetState == ScreenState.ONBOARDING) {
                    fadeIn(tween(250, easing = FastOutSlowInEasing)) togetherWith fadeOut(tween(200, easing = FastOutSlowInEasing))
                } else if (initialState == ScreenState.MAIN && targetState.isOverlaySubscreen()) {
                    (slideInHorizontally(
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                        initialOffsetX = { it }
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                    )) togetherWith fadeOut(
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    )
                } else if (initialState.isOverlaySubscreen() && targetState == ScreenState.MAIN) {
                    fadeIn(
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    ) togetherWith (slideOutHorizontally(
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                        targetOffsetX = { it }
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    ))
                } else {
                    fadeIn(
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    ) togetherWith fadeOut(
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                    )
                }
            },
            label = "SubScreen Transition"
        ) { targetScreen ->
            when (targetScreen) {
                ScreenState.VIEWER -> {
                    val canWebViewGoBack = webViewInstance?.canGoBack() == true
                    BackHandler(enabled = canWebViewGoBack) {
                        webViewInstance?.goBack()
                    }
                    PredictiveBackWrapper(
                        onBack = { 
                            webViewInstance = null
                            selectedHtmlUri = null 
                        },
                        onGestureCommit = handleGestureCommit,
                        onGestureCancel = handleGestureCancel
                    ) {
                        val activeUri = remember(targetScreen) { currentHtmlUri }
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                            if (activeUri != null) {
                                HtmlViewer(fileUri = activeUri, isDomEnabled = domStorageEnabled, isHwAccelEnabled = hwAccelerationEnabled, useGeckoEngine = useGeckoEngine, onWebViewCreated = { webViewInstance = it }, modifier = Modifier.fillMaxSize().statusBarsPadding())
                            }
                        }
                    }
                }
                ScreenState.EDITOR -> {
                    PredictiveBackWrapper(
                        onBack = {
                            showFileEditor = false
                            fileEditorTargetUri = null
                        },
                        onGestureCommit = handleGestureCommit,
                        onGestureCancel = handleGestureCancel
                    ) {
                        FileEditorScreen(
                            onBackClick = {
                                showFileEditor = false
                                fileEditorTargetUri = null
                            },
                            onRunFile = { file ->
                                selectedHtmlUri = Uri.fromFile(file)
                                showFileEditor = false
                                fileEditorTargetUri = null
                            },
                            initialFileUri = fileEditorTargetUri,
                            backdrop = rootBackdrop
                        )
                    }
                }
                ScreenState.AI_EDIT -> {
                    LaunchedEffect(Unit) {
                        showAiScreen = true
                    }
                }
                ScreenState.THEME -> {
                    PredictiveBackWrapper(
                        onBack = { showThemePage = false },
                        onGestureCommit = handleGestureCommit,
                        onGestureCancel = handleGestureCancel
                    ) {
                        ThemePageScreen(
                            onBackClick = { showThemePage = false },
                            activeSchemeName = activeSchemeName,
                            activeSpecVersion = activeSpecVersion,
                            onSpecVersionChanged = { newVersion ->
                                onSpecVersionChangeRequested(newVersion, Offset.Zero)
                            },
                            onSchemeSelected = { newScheme, offset ->
                                onThemeChangeRequested(newScheme, offset)
                            },
                            backdrop = rootBackdrop
                        )
                    }
                }
                ScreenState.BROWSER -> {
                    PredictiveBackWrapper(
                        onBack = { showEmergencyBrowser = false },
                        onGestureCommit = handleGestureCommit,
                        onGestureCancel = handleGestureCancel
                    ) {
                        EmergencyBrowserScreen(onBackClick = { showEmergencyBrowser = false }, backdrop = rootBackdrop)
                    }
                }
                ScreenState.ABOUT -> {
                    PredictiveBackWrapper(
                        onBack = { showAboutScreen = false },
                        onGestureCommit = handleGestureCommit,
                        onGestureCancel = handleGestureCancel
                    ) {
                        AboutScreen(onBackClick = { showAboutScreen = false }, backdrop = rootBackdrop)
                    }
                }
                ScreenState.WEARABLE -> {
                    PredictiveBackWrapper(
                        onBack = { showWearableConnection = false },
                        onGestureCommit = handleGestureCommit,
                        onGestureCancel = handleGestureCancel
                    ) {
                        WearableConnectionScreen(onBackClick = { showWearableConnection = false }, backdrop = rootBackdrop)
                    }
                }
                ScreenState.AI_FORGE -> {
                    PredictiveBackWrapper(
                        onBack = {
                            showAiScreen = false
                        },
                        onGestureCommit = handleGestureCommit,
                        onGestureCancel = handleGestureCancel
                    ) {
                        ExpressiveAiForgeScreen(
                            onBack = {
                                showAiScreen = false
                                aiForgeEditUri = null
                                aiForgeEditName = null
                            },
                            onRunGeneratedHtml = { html: String ->
                                val uri = saveHtmlFromPaste(html)
                                if (uri != null) selectedHtmlUri = uri
                            },
                            initialEditUri = aiForgeEditUri,
                            initialEditFileName = aiForgeEditName,
                            onClearEditTarget = {
                                aiForgeEditUri = null
                                aiForgeEditName = null
                            },
                            onOpenInEditor = { html: String ->
                                val uri = saveHtmlFromPaste(html)
                                if (uri != null) {
                                    fileEditorTargetUri = uri
                                    showFileEditor = true
                                    showAiScreen = false
                                }
                            },
                            onRunUri = { uri ->
                                val fileName = getFileName(mContext, uri)
                                val newItem = LastOpenedFileItem(
                                    id = System.currentTimeMillis().toString(),
                                    name = if (fileName.endsWith(".html", true)) "Project: $fileName" else fileName,
                                    sizeText = "AI Forge",
                                    dateText = SimpleDateFormat("dd.MM.yy", Locale.US).format(Date()),
                                    uri = uri
                                )
                                updateAndPersistLastOpened(listOf(newItem) + lastOpenedList.filterNot { it.name == newItem.name })
                                selectedHtmlUri = uri
                                showAiScreen = false
                            },
                            onOpenFileUri = { uri ->
                                fileEditorTargetUri = uri
                                showFileEditor = true
                                showAiScreen = false
                            }
                        )
                    }
                }
                ScreenState.ONBOARDING -> {
                    PredictiveBackWrapper(onBack = {
                        if (showOnboardingScreen) {
                            showOnboardingScreen = false
                        }
                    }) {
                        OnboardingScreen(
                            onContinue = {
                                prefs.edit { putBoolean("onboarding_completed", true) }
                                hasCompletedOnboarding = true
                                showOnboardingScreen = false
                            },
                            backdrop = rootBackdrop
                        )
                    }
                }
                ScreenState.FOLDER_SELECTION -> {
                    PredictiveBackWrapper(onBack = {}) {
                        FolderOnboardingScreen(onGrantAccess = { folderPickerLauncher.launch(null) }, backdrop = rootBackdrop)
                    }
                }
                ScreenState.MAIN -> {
                    Spacer(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.HomeScreen(
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    lastOpenedList: List<LastOpenedFileItem>, 
    onFileClick: (LastOpenedFileItem) -> Unit, 
    onDeleteFile: (String) -> Unit, 
    onAddFileClick: () -> Unit, 
    onPasteHtml: (String) -> Unit, 
    onDirectPlay: (String) -> Unit,
    onEditWithAi: (LastOpenedFileItem) -> Unit = {},
    onOpenFileEditor: (LastOpenedFileItem) -> Unit = {},
    backdrop: Backdrop,
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {},
    disableShaders: Boolean = false,
    displayState: DeviceDisplayLayoutState = DeviceDisplayLayoutState.PHONE_PORTRAIT
) {
    val mContext = LocalContext.current
    val prefs = remember(mContext) { mContext.getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE) }
    val easterEggController = rememberEasterEggController(prefs)
    val hazeState = remember { HazeState() }
    val homeContentBackdrop = rememberLayerBackdrop()
    val isShaderSupported = !disableShaders && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pastedHtml by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }

    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val listState = rememberLazyListState()
    val canvasColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val isLightMode = canvasColor.luminance() > 0.5f
    val scrollFraction by remember { derivedStateOf { if (listState.firstVisibleItemIndex > 0) 1f else (listState.firstVisibleItemScrollOffset / 180f).coerceIn(0f, 1f) } }
    val showBottomBlur by remember { derivedStateOf { listState.canScrollForward } }
    val isLargeDisplay = displayState == DeviceDisplayLayoutState.LARGE_DISPLAY
    val isCollapsedHeader = (scrollFraction > 0.5f) || isLargeDisplay
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerContentHeight = if (isCollapsedHeader) 52.dp else (110 - (58 * scrollFraction)).dp
    val headerFullHeight = headerContentHeight + statusBarHeight + 20.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // --- LAYER 1: SCROLLABLE CONTENT (recorded for Haze and Progressive Blur) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isShaderSupported) Modifier.layerBackdrop(homeContentBackdrop) else Modifier)
                .hazeSource(hazeState)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(canvasColor)
            )
            if (lastOpenedList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(shape = UniformCardShape, colors = CardDefaults.cardColors(if (isLightMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1.00f) else MaterialTheme.colorScheme.surfaceContainerHigh), border = if (isLightMode) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.size(220.dp, 160.dp)) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Outlined.FolderOff, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                            Spacer(Modifier.height(12.dp)); Text("no files here yet\ntap + to pick a file", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, lineHeight = 16.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 188.dp + statusBarHeight, bottom = 120.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    items(lastOpenedList, key = { it.id }) { item ->
                        ExpressiveFileCard(
                            item = item,
                            onClick = { onFileClick(item) },
                            onDelete = { onDeleteFile(item.id) },
                            onEditWithAi = { onEditWithAi(item) },
                            onOpenFileEditor = { onOpenFileEditor(item) },
                            isScrollInProgress = listState.isScrollInProgress
                        )
                    }
                }
            }
        }

        // --- LAYER 2: PROGRESSIVE BLUR (Transition between header and list) ---
        if (!isFabMenuExpanded) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (scrollFraction > 0.01f) {
                    if (isShaderSupported) {
                        ProgressiveBlur(
                            backdrop = homeContentBackdrop,
                            isTop = true,
                            tint = canvasColor,
                            tintIntensity = 0.75f,
                            blurRadius = 10f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = headerFullHeight)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = headerFullHeight)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            canvasColor,
                                            canvasColor.copy(alpha = 0.65f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }
                
                if (showBottomBlur) {
                    if (isShaderSupported) {
                        ProgressiveBlur(
                            backdrop = homeContentBackdrop,
                            isTop = false,
                            tint = canvasColor,
                            tintIntensity = 0.75f,
                            blurRadius = 10f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .align(Alignment.BottomCenter)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            canvasColor.copy(alpha = 0.65f),
                                            canvasColor
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }

        // --- LAYER 3: GLASS HEADER (Full Top Overlay) ---
        val headerBlurStyle = remember(canvasColor) {
            HazeBlurStyle {
                blurRadius(24.dp)
                backgroundColor(canvasColor)
                colorEffects(listOf(HazeColorEffect.tint(canvasColor.copy(alpha = 0.45f))))
            }
        }
        val headerShape = remember {
            RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = 28.dp,
                bottomEnd = 28.dp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(headerShape)
                .hazeBlur(
                    input = HazeInput.Sources(hazeState),
                    style = headerBlurStyle
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerContentHeight),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isCollapsedHeader) {
                        Column(modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = (1f - (scrollFraction * 1.25f)).coerceIn(0f, 1f); scaleX = 1f - (0.08f * scrollFraction); scaleY = 1f - (0.08f * scrollFraction) }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    easterEggController.onTitleTap()
                                }
                            ) {
                                Icon(painter = painterResource(R.drawable.ic_htmlgom_title_logo), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(46.dp, 30.dp))
                                Text("HtmlGO - M", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(Modifier.height(14.dp))
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !isSearchActive,
                                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.95f),
                                exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.95f)
                            ) {
                                Card(
                                    shape = CircleShape, 
                                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh), 
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), 
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .sharedBounds(
                                            sharedContentState = rememberSharedContentState(key = "search_transform_pill"),
                                            animatedVisibilityScope = this,
                                            boundsTransform = { _, _ -> spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow) }
                                        )
                                        .clickable { onSearchActiveChange(true) }
                                ) {
                                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp)); Text("Search files...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = if (isLargeDisplay) 1f else ((scrollFraction - 0.1f) * 1.2f).coerceIn(0f, 1f) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    easterEggController.onTitleTap()
                                }
                            ) {
                                Icon(painter = painterResource(R.drawable.ic_htmlgom_title_logo), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp, 22.dp))
                                Text("HtmlGO - M", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !isSearchActive,
                                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f),
                                exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f)
                            ) {
                                val searchCompactSurfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                IconButton(
                                    onClick = { onSearchActiveChange(true) }, 
                                    modifier = Modifier
                                        .size(47.5.dp)
                                        .sharedBounds(
                                            sharedContentState = rememberSharedContentState(key = "search_transform_pill"),
                                            animatedVisibilityScope = this,
                                            boundsTransform = { _, _ -> spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow) },
                                            clipInOverlayDuringTransition = OverlayClip(remember { CircleShape })
                                        )
                                        .then(
                                            if (isShaderSupported) {
                                                Modifier.drawBackdrop(
                                                    backdrop = homeContentBackdrop,
                                                    shape = { CircleShape },
                                                    effects = {
                                                        vibrancy()
                                                        blur(2f.dp.toPx())
                                                        lens(
                                                            refractionHeight = 12.dp.toPx(),
                                                            refractionAmount = 24.dp.toPx(),
                                                            depthEffect = true
                                                        )
                                                    },
                                                    highlight = { Highlight.Ambient.copy(alpha = 0.15f) },
                                                    onDrawSurface = {
                                                        drawRect(searchCompactSurfaceColor.copy(alpha = 0.40f))
                                                    }
                                                )
                                            } else {
                                                Modifier.background(searchCompactSurfaceColor, CircleShape)
                                            }
                                        )
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        .clip(CircleShape)
                                ) {
                                    Icon(Icons.Default.Search, "Search", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.5.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = if (isCollapsedHeader) Alignment.Center else Alignment.CenterStart) {
                    Text("last opened", fontSize = if (isCollapsedHeader) 12.sp else 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.graphicsLayer { alpha = if (isLargeDisplay) 1f else (1f - (0.35f * scrollFraction)).coerceIn(0.65f, 1f) })
                }
            }
        }

        // --- LAYER 3.5: SCRIM (Dim background when FAB menu is expanded) ---
        androidx.compose.animation.AnimatedVisibility(
            visible = isFabMenuExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .pointerInput(Unit) {
                        detectTapGestures { isFabMenuExpanded = false }
                    }
            )
        }

        // --- LAYER 4: FLOATING ACTION BUTTON ---
        val fabModifier = when (displayState) {
            DeviceDisplayLayoutState.LARGE_DISPLAY -> Modifier.align(Alignment.BottomEnd).windowInsetsPadding(WindowInsets.navigationBars).padding(end = 20.dp, bottom = 20.dp)
            DeviceDisplayLayoutState.PHONE_LANDSCAPE -> Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 108.dp)
            DeviceDisplayLayoutState.PHONE_PORTRAIT -> Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 112.dp)
        }
        val fabAlignment = if (isLargeDisplay) Alignment.CenterEnd else Alignment.BottomEnd

        Box(modifier = fabModifier, contentAlignment = fabAlignment) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isFabMenuExpanded,
                enter = if (isLargeDisplay) {
                    fadeIn(tween(250)) + expandHorizontally(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), Alignment.End) + scaleIn(initialScale = 0.5f, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
                } else {
                    fadeIn(tween(250)) + expandVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), Alignment.Bottom) + scaleIn(initialScale = 0.5f, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
                },
                exit = if (isLargeDisplay) {
                    fadeOut(tween(200)) + shrinkHorizontally(shrinkTowards = Alignment.End) + scaleOut(targetScale = 0.5f)
                } else {
                    fadeOut(tween(200)) + shrinkVertically(shrinkTowards = Alignment.Bottom) + scaleOut(targetScale = 0.5f)
                }
            ) {
                val speedDialItems: @Composable () -> Unit = {
                    // Option 1: Direct Play (Fastest, bypassing UI limits)
                    var directPlayVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(isFabMenuExpanded) {
                        if (isFabMenuExpanded) {
                            delay(120)
                            directPlayVisible = true
                        } else {
                            directPlayVisible = false
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = directPlayVisible,
                        enter = fadeIn(tween(300)) + (if (isLargeDisplay) slideInHorizontally { it / 2 } else slideInVertically { it / 2 }),
                        exit = fadeOut(tween(150))
                    ) {
                        SmallFloatingActionButton(
                            onClick = { 
                                isFabMenuExpanded = false
                                val clipboard = mContext.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString()
                                    if (!text.isNullOrBlank()) {
                                        onDirectPlay(text)
                                    } else {
                                        Toast.makeText(mContext, "Clipboard is empty!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(mContext, "No text found in clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) { Icon(Icons.Default.ContentPaste, "Direct Play Clipboard") }
                    }

                    // Option 2: Code Paste Box
                    var pasteVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(isFabMenuExpanded) {
                        if (isFabMenuExpanded) {
                            delay(60)
                            pasteVisible = true
                        } else {
                            pasteVisible = false
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = pasteVisible,
                        enter = fadeIn(tween(300)) + (if (isLargeDisplay) slideInHorizontally { it / 2 } else slideInVertically { it / 2 }),
                        exit = fadeOut(tween(150))
                    ) {
                        SmallFloatingActionButton(
                            onClick = { isFabMenuExpanded = false; showPasteDialog = true },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) { Icon(Icons.Default.Code, "Edit & Paste") }
                    }

                    // Option 3: File Picker
                    var pickerVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(isFabMenuExpanded) {
                        if (isFabMenuExpanded) {
                            delay(0)
                            pickerVisible = true
                        } else {
                            pickerVisible = false
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = pickerVisible,
                        enter = fadeIn(tween(300)) + (if (isLargeDisplay) slideInHorizontally { it / 2 } else slideInVertically { it / 2 }),
                        exit = fadeOut(tween(150))
                    ) {
                        SmallFloatingActionButton(
                            onClick = { isFabMenuExpanded = false; onAddFileClick() },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) { Icon(Icons.Default.FolderOpen, "Pick File") }
                    }
                }

                if (isLargeDisplay) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 70.dp)
                    ) {
                        speedDialItems()
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 70.dp)
                    ) {
                        speedDialItems()
                    }
                }
            }
            val fabMonetTint = MaterialTheme.colorScheme.primaryContainer
            val fabGlassStyle = remember(fabMonetTint) {
                GlassStyle.regular.then {
                    shape(RoundedCornerShape(18.dp))
                    tint(fabMonetTint.copy(alpha = 0.50f))
                    edgeShadow(Color.Transparent)
                    optics {
                        this.copy(
                            refractionStrength = (this.refractionStrength * 1.10f).coerceIn(0f, 1f),
                            blurRadius = OpticalSizeValue.Fixed(1.dp)
                        )
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent, 
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer, 
                tonalElevation = 0.dp, 
                shadowElevation = 0.dp, 
                border = if (isLightMode) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                modifier = Modifier
                    .size(56.dp)
                    .hazeGlass(
                        input = HazeInput.Sources(hazeState),
                        style = fabGlassStyle,
                        interactionSource = interactionSource
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .indication(interactionSource, LocalIndication.current)
                    .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        val press = PressInteraction.Press(down.position)
                        coroutineScope.launch { interactionSource.emit(press) }
                        val up = withTimeoutOrNull(680) { waitForUpOrCancellation() }
                        if (up == null) {
                            isFabMenuExpanded = !isFabMenuExpanded
                            coroutineScope.launch { interactionSource.emit(PressInteraction.Release(press)) }
                            while (true) { if (awaitPointerEvent().changes.all { !it.pressed }) break }
                        } else {
                            coroutineScope.launch { interactionSource.emit(PressInteraction.Release(press)) }
                            if (!up.isConsumed) { if (isFabMenuExpanded) isFabMenuExpanded = false else onAddFileClick() }
                        }
                    }
                }
            }) { Box(contentAlignment = Alignment.Center) { Icon(if (isFabMenuExpanded) Icons.Default.Close else Icons.Default.Add, "Add File", modifier = Modifier.size(28.dp)) } }
        }

        // --- LAYER 5: DIALOGS & OVERLAYS ---
        LiquidGlassToastHost(
            controller = easterEggController,
            hazeState = hazeState,
            displayState = displayState
        )
        MinecraftAchievementBanner(
            controller = easterEggController
        )
        EasterEggAudioPlayer(
            isClimaxPlaying = easterEggController.isClimaxPlaying.value
        )

        if (showPasteDialog) {
            AlertDialog(
                onDismissRequest = { showPasteDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("HTML Studio Editor", fontWeight = FontWeight.Bold)
                    }
                }, 
                text = { 
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(560.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E1E))
                            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
                    ) {
                        SoraEditor(
                            text = pastedHtml.text,
                            onTextChange = { pastedHtml = androidx.compose.ui.text.input.TextFieldValue(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }, 
                confirmButton = { 
                    Button(
                        onClick = { 
                            if (pastedHtml.text.isNotBlank()) { 
                                onPasteHtml(pastedHtml.text)
                                pastedHtml = androidx.compose.ui.text.input.TextFieldValue("")
                                showPasteDialog = false 
                            } 
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { 
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Run Engine") 
                    } 
                }, 
                dismissButton = { 
                    TextButton(onClick = { 
                        pastedHtml = androidx.compose.ui.text.input.TextFieldValue("")
                        showPasteDialog = false 
                    }) { Text("Discard") } 
                }
            )
        }
    }
}

@Composable
fun ExpressiveFileCard(
    item: LastOpenedFileItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEditWithAi: () -> Unit = {},
    onOpenFileEditor: () -> Unit = {},
    isScrollInProgress: Boolean = false
) {
    val mContext = LocalContext.current
    var isDeleting by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    val springScale by animateFloatAsState(
        targetValue = when {
            isDeleting -> 0.75f
            isPressed -> 0.94f
            else -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "springScale"
    )

    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isDeleting) 800f else dragOffsetX,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "animatedOffsetX"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isDeleting) 0f else 1f,
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "alpha"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isDeleting) -15f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "rotation"
    )

    val isLightMode = MaterialTheme.colorScheme.surfaceContainerLowest.luminance() > 0.5f

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isDeleting -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
            isLightMode -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1.00f)
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        },
        animationSpec = tween(durationMillis = 300),
        label = "bgColor"
    )

    val cardBorder = when {
        isDeleting -> BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        isLightMode -> null
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
    }

    Card(
        shape = UniformCardShape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.scaleX = springScale
                this.scaleY = springScale
                this.alpha = alpha
                this.translationX = animatedOffsetX
                this.rotationZ = rotation
                this.cameraDistance = 12f * density
            }
            .pointerInput(isScrollInProgress) {
                if (!isScrollInProgress) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            dragOffsetX = 0f
                        },
                        onDragCancel = { dragOffsetX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            dragOffsetX = (dragOffsetX + dragAmount).coerceIn(-140f, 140f)
                            change.consume()
                        }
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onLongPress = {
                        showContextMenu = true
                    },
                    onTap = {
                        if (!isDeleting) onClick()
                    }
                )
            }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDeleting) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (item.name.endsWith(".zip", true)) Icons.Default.FolderZip else Icons.Default.Html,
                            contentDescription = null,
                            tint = if (isDeleting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isDeleting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Last access: ${item.dateText}", fontSize = 11.sp, fontWeight = if (isLightMode) FontWeight.SemiBold else FontWeight.Normal, color = if (isDeleting) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else if (isLightMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { 
                    scope.launch { 
                        isDeleting = true
                        delay(400)
                        onDelete() 
                    } 
                }) { 
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline, 
                        "Delete", 
                        tint = if (isDeleting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), 
                        modifier = Modifier.size(20.dp)
                    ) 
                }
                Spacer(Modifier.width(4.dp))
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = CircleShape, 
                        color = if (isDeleting) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) 
                        else if (isLightMode) MaterialTheme.colorScheme.secondary 
                        else MaterialTheme.colorScheme.secondaryContainer
                    ) { 
                        Text(
                            item.sizeText, 
                            color = if (isDeleting) MaterialTheme.colorScheme.error 
                            else if (isLightMode) MaterialTheme.colorScheme.onSecondary 
                            else MaterialTheme.colorScheme.onSecondaryContainer, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold, 
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = if (isLightMode) 3.5.dp else 3.dp)
                        ) 
                    }
                    Surface(
                        shape = CircleShape, 
                        color = if (isDeleting) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) 
                        else if (isLightMode) MaterialTheme.colorScheme.tertiary 
                        else MaterialTheme.colorScheme.tertiaryContainer
                    ) { 
                        Text(
                            item.dateText, 
                            color = if (isDeleting) MaterialTheme.colorScheme.error 
                            else if (isLightMode) MaterialTheme.colorScheme.onTertiary 
                            else MaterialTheme.colorScheme.onTertiaryContainer, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold, 
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = if (isLightMode) 3.5.dp else 3.dp)
                        ) 
                    }
                }
            }

            // TAP AND HOLD WINDOWS-STYLE RIGHT-CLICK CONTEXT ACTION BOX
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                DropdownMenuItem(
                    text = { Text("Edit with AI", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showContextMenu = false
                        onEditWithAi()
                    }
                )
                DropdownMenuItem(
                    text = { Text("File Editor", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Default.EditNote, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showContextMenu = false
                        onOpenFileEditor()
                    }
                )
                if (item.name.endsWith(".zip", ignoreCase = true)) {
                    DropdownMenuItem(
                        text = { Text("Share ZIP File", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            showContextMenu = false
                            FileSharingHelper.shareFileFromUri(mContext, item.uri, item.name, asZip = false)
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Share HTML", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            showContextMenu = false
                            FileSharingHelper.shareFileFromUri(mContext, item.uri, item.name, asZip = false)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share as ZIP", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.tertiary) },
                        onClick = {
                            showContextMenu = false
                            FileSharingHelper.shareFileFromUri(mContext, item.uri, item.name, asZip = true)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showContextMenu = false
                        scope.launch {
                            isDeleting = true
                            delay(400)
                            onDelete()
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Send to Watch", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Default.Watch, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showContextMenu = false
                        Toast.makeText(mContext, "Sending to Watch...", Toast.LENGTH_SHORT).show()
                        com.absolutemango.expressiveui.sendFileToWatch(mContext, item.uri) { success ->
                            if (success) {
                                Toast.makeText(mContext, "Sent successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(mContext, "Watch not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

fun generateDummyFiles(): List<LastOpenedFileItem> {
    val sampleNames = listOf("index.html", "game_v2.zip", "portfolio_template.html", "threejs_demo.zip", "canvas_animation.html", "dashboard_v1.zip", "phaser_game.zip", "webgl_shader.html", "platformer_engine.zip", "snake_game.html", "tetris_clone.html", "rpg_maker.zip", "flappy_bird.html", "pong_v2.html", "mario_clone.zip", "calculator.html", "todo_app.html", "weather_widget.zip", "pwa_shell.html", "audio_visualizer.zip", "3d_cube_demo.html", "chart_dashboard.zip", "markdown_editor.html", "chat_ui.zip", "retro_arcade.zip", "synth_wave.html", "space_invaders.zip", "cookie_clicker.html", "memory_game.zip", "tic_tac_toe.html", "minesweeper.zip", "pixel_art_editor.html", "quiz_app.zip", "breakout_game.html", "sudoku_solver.zip")
    return sampleNames.mapIndexed { index, name -> LastOpenedFileItem(id = "dummy_${System.currentTimeMillis()}_$index", name = name, sizeText = if (name.endsWith(".zip")) "${(12..85).random() / 10.0} MB" else "${(15..450).random()} KB", dateText = String.format(Locale.US, "%02d.%02d.26", (1..28).random(), (1..12).random()), uri = Uri.EMPTY) }
}

@Composable
fun ToolsScreen(subStage: ToolSubStage, onSelectSubStage: (ToolSubStage) -> Unit, selectedFiles: List<Uri>, onPickFiles: () -> Unit, onExecuteAction: () -> Unit, backdrop: Backdrop) {
    val context = LocalContext.current
    BackHandler(enabled = subStage != ToolSubStage.NONE) {
        onSelectSubStage(ToolSubStage.NONE)
    }
    Box(Modifier.fillMaxSize()) {
        val isLightMode = MaterialTheme.colorScheme.surfaceContainerLowest.luminance() > 0.5f
        val toolCardColor = if (isLightMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1.00f) else MaterialTheme.colorScheme.primaryContainer.copy(0.25f)
        val toolCardBorder = if (isLightMode) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.2f))

        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(16.dp))
                Surface(shape = CircleShape, color = if (isLightMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1.00f) else MaterialTheme.colorScheme.primaryContainer.copy(0.3f), border = if (isLightMode) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.5f))) { Text("tools", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) }
                Spacer(Modifier.height(28.dp))
                Card(shape = UniformCardShape, colors = CardDefaults.cardColors(toolCardColor), border = toolCardBorder, modifier = Modifier.fillMaxWidth().clickable { onSelectSubStage(ToolSubStage.COMPRESSOR) }) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Compress, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(16.dp))
                        Column { Text("Compressor", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold); Text("Compress HTML and assets into a clean ZIP", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Card(shape = UniformCardShape, colors = CardDefaults.cardColors(toolCardColor), border = toolCardBorder, modifier = Modifier.fillMaxWidth().clickable { onSelectSubStage(ToolSubStage.DECOMPRESSOR) }) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FolderZip, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(16.dp))
                        Column { Text("Decompressor", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold); Text("Decompress ZIP bundles into playable workspace folders", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        // Layer 1: Ambient Dimming Scrim (strictly alpha fadeIn / fadeOut, stationary full-screen)
        androidx.compose.animation.AnimatedVisibility(
            visible = subStage != ToolSubStage.NONE,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(250))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .pointerInput(Unit) {
                        detectTapGestures { onSelectSubStage(ToolSubStage.NONE) }
                    }
            )
        }

        // Layer 2: Modal Card (spring slide-in and scale-in anchored to BottomCenter)
        androidx.compose.animation.AnimatedVisibility(
            visible = subStage != ToolSubStage.NONE,
            enter = fadeIn(tween(300)) + slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            ) + scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = fadeOut(tween(250)) + slideOutVertically(targetOffsetY = { it / 3 }) + scaleOut(targetScale = 0.85f),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                    shape = UniformCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = BorderStroke(0.1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 124.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { /* Block dismiss when clicking inside */ }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        var headerVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(subStage) { 
                            if (subStage != ToolSubStage.NONE) {
                                delay(100)
                                headerVisible = true
                            } else {
                                headerVisible = false
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = headerVisible,
                            enter = fadeIn() + slideInVertically { -it / 2 }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (subStage == ToolSubStage.COMPRESSOR) Icons.Default.Compress else Icons.Default.FolderZip,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Text(
                                    text = if (subStage == ToolSubStage.COMPRESSOR) "Compress File" else "Decompress File",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        Text(
                            text = if (subStage == ToolSubStage.COMPRESSOR) 
                                "Select multiple files or a directory to create a packaged HTML5 zip bundle." 
                                else "Pick a .zip file and target directory to extract its contents for previewing.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // The "Middle Box" for picked files
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            if (selectedFiles.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "No files selected",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(selectedFiles) { uri ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.FilePresent, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = getFileName(context, uri),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LiquidButton(
                                onClick = onPickFiles,
                                backdrop = backdrop,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (subStage == ToolSubStage.COMPRESSOR) Icons.Default.AddBox else Icons.Default.FileOpen, 
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (selectedFiles.isEmpty()) "Pick Files" else "Change Files",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            LiquidButton(
                                onClick = onExecuteAction,
                                backdrop = backdrop,
                                isInteractive = selectedFiles.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = if (subStage == ToolSubStage.COMPRESSOR) "Compress" else "Decompress",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    extraBlack: Boolean,
    onExtraBlackChange: (Boolean) -> Unit,
    disableShaders: Boolean,
    onDisableShadersChange: (Boolean) -> Unit,
    domStorage: Boolean,
    onDomStorageChange: (Boolean) -> Unit,
    hwAcceleration: Boolean,
    onHwAccelChange: (Boolean) -> Unit,
    useGeckoEngine: Boolean = true,
    onGeckoEngineChange: (Boolean) -> Unit = {},
    permittedFolderUri: Uri?,
    onRequestFolderPermission: () -> Unit,
    onGenerateDummyFiles: () -> Unit,
    onClearDummyFiles: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAiForge: () -> Unit,
    onOpenEmergencyBrowser: () -> Unit = {},
    onOpenFileEditor: () -> Unit = {},
    onOpenThemePage: () -> Unit = {},
    onOpenWearableConnection: () -> Unit = {},
    onTriggerOnboarding: () -> Unit = {},
    activeSchemeName: String = "Dynamic Monet",
    backdrop: Backdrop,
    forceVerticalRail: Boolean = false,
    onForceVerticalRailChange: (Boolean) -> Unit = {}
) {
    val settingsContentBackdrop = rememberLayerBackdrop()
    val mContext = LocalContext.current
    val prefs = remember(mContext) { mContext.getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE) }
    var isEasterEggEverUnlocked by remember { mutableStateOf(prefs.getBoolean(PREF_EASTER_EGG_EVER_UNLOCKED, false)) }
    val isShaderSupported = !disableShaders && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val canvasColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val scrollState = rememberScrollState()
    var devModeEnabled by remember { mutableStateOf(prefs.getBoolean("developer_mode", false) || forceVerticalRail) }
    var agslGovernorEnabled by remember { mutableStateOf(prefs.getBoolean(AgslPerformanceGovernor.PREF_GOVERNOR_ENABLED, true)) }
    var showShaderPrompt by remember { mutableStateOf(false) }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isScrolled by remember { derivedStateOf { scrollState.value > 0 } }
    val targetBackdropHeight = if (isScrolled) statusBarHeight + 170.dp else 0.dp
    val animatedBackdropHeight by animateDpAsState(
        targetValue = targetBackdropHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SettingsTopBackdropHeight"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // --- LAYER 0: SCROLLABLE CONTENT (Full-bleed coordinate space for Backdrop sampling) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isShaderSupported) Modifier.layerBackdrop(settingsContentBackdrop) else Modifier)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(canvasColor)
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(statusBarHeight + 64.dp))

                val isLightMode = MaterialTheme.colorScheme.surfaceContainerLowest.luminance() > 0.5f
                val settingsCardBorder = if (isLightMode) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

                Text("SYSTEM SUITE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                Card(
                    shape = UniformCardShape,
                    colors = CardDefaults.cardColors(if (isLightMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1.00f) else MaterialTheme.colorScheme.primaryContainer.copy(0.25f)),
                    border = settingsCardBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // 1. Emergency Browser
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenEmergencyBrowser() }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Language, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Emergency Browser", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Bottom address bar & liquid tabs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // 2. File Editor
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenFileEditor() }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.EditNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Workspace File Editor", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Edit HTML, CSS, JS and JSON files", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // 3. AI Forge
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenAiForge() }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text("AI Forge Studio", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Generate web projects with AI", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                Text("WEARABLE SYNC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                Card(shape = UniformCardShape, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh), border = settingsCardBorder, modifier = Modifier.fillMaxWidth().clickable { onOpenWearableConnection() }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Watch, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Wearable System", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Manage HTMLGO-W Connection", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("APPEARANCE & THEMES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                Card(shape = UniformCardShape, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh), border = settingsCardBorder, modifier = Modifier.fillMaxWidth().clickable { onOpenThemePage() }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Theme Palette ($activeSchemeName)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("9 Expressive Material 3 color schemes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Card(shape = UniformCardShape, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh), border = settingsCardBorder, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        SettingsSwitchRow("Extra Black Mode", "Pure dark theme for AMOLED screens", extraBlack, onExtraBlackChange, backdrop)
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsSwitchRow("Disable Overlay Shaders", "Turn off top/bottom ambient gradient effects", disableShaders, { showShaderPrompt = true }, backdrop)
                    }
                }

                if (showShaderPrompt) {
                    ModalBottomSheet(
                        onDismissRequest = { showShaderPrompt = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Change App Engine Version?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Do you want to change to a lighter version of this app?",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showShaderPrompt = false },
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text("No")
                                }

                                Button(
                                    onClick = {
                                        showShaderPrompt = false
                                        onDisableShadersChange(!disableShaders)
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text("Yes")
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("WEBVIEW ENGINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                Card(shape = UniformCardShape, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh), border = settingsCardBorder, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        if (EngineCapabilities.IS_GECKO_AVAILABLE) {
                            SettingsSwitchRow("Mozilla GeckoView Engine", "Enable Mozilla GeckoView for HTML Loader & Browser", useGeckoEngine, onGeckoEngineChange, backdrop)
                            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        SettingsSwitchRow("DOM Storage API", "Enable localStorage and sessionStorage", domStorage, onDomStorageChange, backdrop)
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsSwitchRow("Hardware Acceleration", "Enable GPU rendering for WebGL & Canvas", hwAcceleration, onHwAccelChange, backdrop)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("DEVELOPER OPTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                Card(shape = UniformCardShape, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh), border = settingsCardBorder, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        SettingsSwitchRow("Developer Mode", "Unlock testing tools and dummy file generation", devModeEnabled, {
                            devModeEnabled = it
                            prefs.edit { putBoolean("developer_mode", it) }
                        }, backdrop)
                        if (devModeEnabled) {
                            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LiquidButton(onClick = onGenerateDummyFiles, backdrop = backdrop, modifier = Modifier.weight(1f).height(44.dp)) { Text("+35 Dummy Files", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                LiquidButton(onClick = onClearDummyFiles, backdrop = backdrop, modifier = Modifier.weight(1f).height(44.dp)) { Text("Clear All Files", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            }
                            Spacer(Modifier.height(10.dp))
                            LiquidButton(
                                onClick = {
                                    resetEasterEggState(prefs)
                                    isEasterEggEverUnlocked = false
                                    Toast.makeText(mContext, "Easter egg state reset", Toast.LENGTH_SHORT).show()
                                },
                                backdrop = backdrop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text("Reset Easter Egg State", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(10.dp))
                            LiquidButton(
                                onClick = onTriggerOnboarding,
                                backdrop = backdrop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text("Trigger Onboarding", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            SettingsSwitchRow(
                                title = "Force Vertical Rail",
                                subtitle = "Enable vertical railing navigation bar regardless of display size",
                                checked = forceVerticalRail,
                                onCheckedChange = onForceVerticalRailChange,
                                backdrop = backdrop
                            )
                            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            SettingsSwitchRow(
                                title = "AGSL Performance Governor",
                                subtitle = "Dynamically optimize shaders based on thermals. Turn off to return to standard configuration of backdrop",
                                checked = agslGovernorEnabled,
                                onCheckedChange = {
                                    agslGovernorEnabled = it
                                    AgslPerformanceGovernor.setGovernorEnabled(mContext, it)
                                },
                                backdrop = backdrop
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Card(shape = UniformCardShape, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh), border = settingsCardBorder, modifier = Modifier.fillMaxWidth().clickable { onOpenAbout() }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) { Text("About HTMLGOM", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Text("Version, specs & application details", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                EasterEggCommemorationCard(isUnlocked = isEasterEggEverUnlocked)
                Spacer(Modifier.height(120.dp))
            }
        }
        
        // --- LAYER 1: DYNAMIC TOP BACKDROP ---
        SettingsTopBackdrop(
            animatedHeight = animatedBackdropHeight,
            disableShaders = disableShaders,
            canvasColor = canvasColor,
            backdrop = settingsContentBackdrop,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // --- LAYER 1: BOTTOM OVERLAY ---
        val showBottomBlur by remember { derivedStateOf { scrollState.canScrollForward } }
        if (showBottomBlur) {
            if (isShaderSupported) {
                ProgressiveBlur(
                    backdrop = settingsContentBackdrop,
                    isTop = false,
                    tint = canvasColor,
                    tintIntensity = 0.75f,
                    blurRadius = 10f,
                    modifier = Modifier.fillMaxWidth().height(100.dp).align(Alignment.BottomCenter)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    canvasColor.copy(alpha = 0.65f),
                                    canvasColor
                                )
                            )
                        )
                )
            }
        }

        // --- LAYER 2: PINNED SETTINGS CAPSULE PILL ---
        Box(
            modifier = Modifier
                .wrapContentSize(Alignment.TopCenter)
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 2.dp
            ) {
                Text(
                    text = "settings",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsTopBackdrop(
    animatedHeight: Dp,
    disableShaders: Boolean,
    canvasColor: Color,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    if (animatedHeight <= 0.dp) return

    val isShaderSupported = !disableShaders && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    if (backdrop != null && isShaderSupported) {
        ProgressiveBlur(
            backdrop = backdrop,
            isTop = true,
            tint = canvasColor,
            tintIntensity = 0.75f,
            blurRadius = 25f,
            modifier = modifier
                .fillMaxWidth()
                .height(animatedHeight)
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .background(
                    Brush.verticalGradient(
                        0.00f to canvasColor,
                        0.45f to canvasColor,
                        0.75f to canvasColor.copy(alpha = 0.60f),
                        1.00f to Color.Transparent
                    )
                )
        )
    }
}

@Composable
fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, backdrop: Backdrop) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        LiquidToggle(selected = { checked }, onSelect = onCheckedChange, backdrop = backdrop)
    }
}

@Composable
fun HtmlViewer(
    fileUri: Uri,
    isDomEnabled: Boolean,
    isHwAccelEnabled: Boolean,
    modifier: Modifier = Modifier,
    useGeckoEngine: Boolean = true,
    onWebViewCreated: (WebView?) -> Unit = {}
) {
    val context = LocalContext.current
    val effectiveUseGecko = EngineCapabilities.IS_GECKO_AVAILABLE && useGeckoEngine
    val uriString = fileUri.toString()

    var webViewFileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var webProgress by remember { mutableFloatStateOf(0f) }
    var isWebLoading by remember { mutableStateOf(false) }

    var pendingWebPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingGeoOrigin by remember { mutableStateOf<String?>(null) }
    var pendingGeoCallback by remember { mutableStateOf<GeolocationPermissions.Callback?>(null) }

    val webPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        // Process pending WebView WebRTC PermissionRequest
        val webReq = pendingWebPermissionRequest
        if (webReq != null) {
            val grantedResources = mutableListOf<String>()
            webReq.resources?.forEach { res ->
                when (res) {
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                        val isGranted = permissionsMap[Manifest.permission.RECORD_AUDIO] == true ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (isGranted) grantedResources.add(res)
                    }
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                        val isGranted = permissionsMap[Manifest.permission.CAMERA] == true ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        if (isGranted) grantedResources.add(res)
                    }
                    PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> {
                        grantedResources.add(res)
                    }
                    else -> grantedResources.add(res)
                }
            }
            if (grantedResources.isNotEmpty()) {
                webReq.grant(grantedResources.toTypedArray())
            } else {
                webReq.deny()
            }
            pendingWebPermissionRequest = null
        }

        // Process pending WebView Geolocation
        val geoCallback = pendingGeoCallback
        val geoOrigin = pendingGeoOrigin
        if (geoCallback != null && geoOrigin != null) {
            val fine = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarse = permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            geoCallback.invoke(geoOrigin, fine || coarse, false)
            pendingGeoCallback = null
            pendingGeoOrigin = null
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris: Array<Uri>? = if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            when {
                data?.clipData != null -> {
                    val clip = data.clipData!!
                    Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
                }
                data?.data != null -> arrayOf(data.data!!)
                else -> null
            }
        } else {
            null
        }

        webViewFileChooserCallback?.onReceiveValue(uris)
        webViewFileChooserCallback = null
    }

    Box(modifier = modifier) {
        if (effectiveUseGecko) {
            GeckoHtmlViewer(
                fileUri = fileUri,
                isDomEnabled = isDomEnabled,
                isHwAccelEnabled = isHwAccelEnabled,
                onProgressChange = { p, loading ->
                    webProgress = p
                    isWebLoading = loading
                },
                modifier = Modifier.fillMaxSize()
            )
            LaunchedEffect(Unit) {
                onWebViewCreated(null)
            }
        } else {
            var lastLoadedUriString by remember { mutableStateOf("") }
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isWebLoading = true
                                webProgress = 0.05f
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isWebLoading = false
                                webProgress = 1.0f
                            }
                            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    isWebLoading = false
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                if (newProgress >= 100) {
                                    webProgress = 1.0f
                                    isWebLoading = false
                                } else if (isWebLoading) {
                                    webProgress = (newProgress / 100f).coerceIn(0.05f, 0.99f)
                                }
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                webViewFileChooserCallback?.onReceiveValue(null)
                                webViewFileChooserCallback = filePathCallback

                                val acceptTypes = fileChooserParams?.acceptTypes?.filter { !it.isNullOrBlank() } ?: emptyList()
                                val allowMultiple = fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE

                                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = if (acceptTypes.size == 1) acceptTypes[0] else "*/*"
                                    if (acceptTypes.size > 1) {
                                        putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes.toTypedArray())
                                    }
                                }
                                if (allowMultiple) {
                                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                }

                                return try {
                                    filePickerLauncher.launch(intent)
                                    true
                                } catch (_: Exception) {
                                    webViewFileChooserCallback?.onReceiveValue(null)
                                    webViewFileChooserCallback = null
                                    false
                                }
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                if (request == null) return
                                val resources = request.resources ?: return
                                val needed = mutableListOf<String>()

                                for (res in resources) {
                                    when (res) {
                                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                                needed.add(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                                needed.add(Manifest.permission.CAMERA)
                                            }
                                        }
                                    }
                                }

                                if (needed.isEmpty()) {
                                    request.grant(resources)
                                } else {
                                    pendingWebPermissionRequest = request
                                    webPermissionLauncher.launch(needed.toTypedArray())
                                }
                            }

                            override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                                if (pendingWebPermissionRequest == request) {
                                    pendingWebPermissionRequest = null
                                }
                            }

                            override fun onGeolocationPermissionsShowPrompt(
                                origin: String?,
                                callback: GeolocationPermissions.Callback?
                            ) {
                                if (origin == null || callback == null) return
                                val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                                if (fine || coarse) {
                                    callback.invoke(origin, true, false)
                                } else {
                                    pendingGeoOrigin = origin
                                    pendingGeoCallback = callback
                                    webPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }

                            override fun onGeolocationPermissionsHidePrompt() {
                                pendingGeoCallback = null
                                pendingGeoOrigin = null
                            }
                        }
                        settings.apply {
                            @SuppressLint("SetJavaScriptEnabled")
                            javaScriptEnabled = true
                            domStorageEnabled = isDomEnabled
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            allowFileAccessFromFileURLs = true
                            allowUniversalAccessFromFileURLs = true
                            mediaPlaybackRequiresUserGesture = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            cacheMode = WebSettings.LOAD_DEFAULT

                            val defaultUa = userAgentString
                            userAgentString = defaultUa
                                .replace("; wv", "")
                                .replace(";wv", "")
                                .replace("Version/4.0 ", "")
                        }
                        if (isHwAccelEnabled) setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                        else setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
                        onWebViewCreated(this)
                        lastLoadedUriString = uriString
                        loadUrl(uriString)
                    }
                },
                update = { wv ->
                    if (uriString.isNotEmpty() && uriString != lastLoadedUriString) {
                        lastLoadedUriString = uriString
                        wv.loadUrl(uriString)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        ExpressiveWebProgressIndicator(
            progress = webProgress,
            isLoading = isWebLoading,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
fun FolderOnboardingScreen(onGrantAccess: () -> Unit, backdrop: Backdrop) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(110.dp).clip(RoundedCornerShape(28.dp)).border(2.dp, MaterialTheme.colorScheme.primary.copy(0.4f), RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(0.35f)), Alignment.Center) { Icon(Icons.Outlined.FolderSpecial, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp)) }
                Spacer(Modifier.height(32.dp)); Text("Select Workspace Folder", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(12.dp)); Text("HTMLGOM requires permission to access a dedicated folder directory. This allows the app to load, decompress, and store your local HTML web projects and games securely.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 12.dp))
                Spacer(Modifier.height(40.dp)); LiquidButton(onClick = onGrantAccess, backdrop = backdrop, modifier = Modifier.fillMaxWidth().height(52.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Text("Grant Folder Access", fontSize = 15.sp, fontWeight = FontWeight.Bold) } }
            }
        }
    }
}

fun saveLastOpenedFiles(context: Context, list: List<LastOpenedFileItem>) {
    try {
        val jsonArray = JSONArray()
        list.forEach { item -> val obj = JSONObject(); obj.put("id", item.id); obj.put("name", item.name); obj.put("sizeText", item.sizeText); obj.put("dateText", item.dateText); obj.put("uri", item.uri.toString()); jsonArray.put(obj) }
        context.getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE).edit().putString("last_opened_list_json", jsonArray.toString()).apply()
    } catch (_: Exception) {}
}

fun loadLastOpenedFiles(context: Context): List<LastOpenedFileItem> {
    val list = mutableListOf<LastOpenedFileItem>()
    try {
        val jsonString = context.getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE).getString("last_opened_list_json", null) ?: return list
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) { val obj = jsonArray.getJSONObject(i); list.add(LastOpenedFileItem(id = obj.getString("id"), name = obj.getString("name"), sizeText = obj.getString("sizeText"), dateText = obj.getString("dateText"), uri = Uri.parse(obj.getString("uri")))) }
    } catch (_: Exception) {}
    return list
}

fun compressUrisToZipSAF(context: Context, uris: List<Uri>, zipFileUri: Uri): String {
    return try {
        context.contentResolver.openOutputStream(zipFileUri)?.use { outputStream ->
            val zos = ZipOutputStream(BufferedOutputStream(outputStream))
            uris.forEach { uri ->
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bis = BufferedInputStream(inputStream)
                    zos.putNextEntry(ZipEntry(getFileName(context, uri)))
                    bis.copyTo(zos)
                    zos.closeEntry()
                }
            }
            zos.close()
        }
        "Project compressed successfully!"
    } catch (e: Exception) { "Error compressing: ${e.message}" }
}

fun decompressZipToFolderSAF(context: Context, zipUri: Uri, targetFolderUri: Uri): String {
    return try {
        val rootDoc = DocumentFile.fromTreeUri(context, targetFolderUri) ?: return "Failed to access target folder"
        context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
            val zis = ZipInputStream(BufferedInputStream(inputStream))
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val segments = entry.name.split("/")
                    var currentDir = rootDoc
                    for (i in 0 until segments.size - 1) {
                        currentDir = currentDir.findFile(segments[i]) ?: currentDir.createDirectory(segments[i]) ?: currentDir
                    }
                    val fileName = segments.last()
                    val file = currentDir.createFile("application/octet-stream", fileName)
                    file?.let { context.contentResolver.openOutputStream(it.uri)?.use { out -> zis.copyTo(out) } }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()
        }
        "Project decompressed to workspace!"
    } catch (e: Exception) { "Error decompressing: ${e.message}" }
}

fun decompressZipToDir(context: Context, zipUri: Uri, targetDir: File): File? =
    ZipArchiveSystem.decompressZipToDir(context, zipUri, targetDir)

fun findHtmlFile(dir: File): File? =
    ZipArchiveSystem.findHtmlFile(dir)

fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { if (it.moveToFirst()) { val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (index != -1) result = it.getString(index) } }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) result = result?.substring(cut + 1)
    }
    return result ?: "unknown_file"
}



@Composable
fun ProgressiveBlur(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isTop: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.surface,
    tintIntensity: Float = 0.8f,
    blurRadius: Float = 10f
) {
    Box(
        modifier
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    blur(blurRadius)
                    runtimeShaderEffect(
                        "AlphaMask",
                        """
                        uniform shader content;
                        uniform float2 size;
                        layout(color) uniform half4 tint;
                        uniform float tintIntensity;
                        uniform float isTop;
                        
                        half4 main(float2 coord) {
                            if (size.y < 0.001) return half4(0.0);
                            float alpha;
                            if (isTop > 0.5) {
                                alpha = 1.0 - smoothstep(size.y * 0.35, size.y, coord.y);
                            } else {
                                alpha = smoothstep(0.0, size.y * 0.65, coord.y);
                            }
                            return mix(content.eval(coord) * alpha, tint * alpha, half(tintIntensity));
                        }
                        """,
                        "content"
                    ) {
                        setFloatUniform("size", size.width, size.height)
                        setColorUniform("tint", tint)
                        setFloatUniform("tintIntensity", tintIntensity)
                        setFloatUniform("isTop", if (isTop) 1f else 0f)
                    }
                }
            )
    )
}
