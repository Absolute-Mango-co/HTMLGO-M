package com.absolutemango.expressiveui

import com.absolutemango.expressiveui.components.ExpressiveWebProgressIndicator

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.app.Activity
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.abs

// ---------------------------------------------------------------------------
// MONET SYSTEM THEME ENGINE
// ---------------------------------------------------------------------------
@Composable
fun HTMLGOMTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    extraBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    if (darkTheme && extraBlack) {
        colorScheme = colorScheme.copy(
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0A0A0A),
            surfaceContainer = Color(0xFF121212),
            surfaceContainerHigh = Color(0xFF1A1A1A),
            surfaceContainerHighest = Color(0xFF222222)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// Universal Tokens
val UniformCardShape = RoundedCornerShape(22.dp)

// ---------------------------------------------------------------------------
// DATA MODELS & ENUMS
// ---------------------------------------------------------------------------
enum class NavigationTab {
    TOOLS, HOME, SETTINGS
}

enum class ToolSubStage {
    NONE, COMPRESSOR, DECOMPRESSOR
}

enum class SubScreenState {
    NONE, VIEWER, FILE_EDITOR, AI_FORGE, AI_EDIT, EMERGENCY_BROWSER, WEARABLE, THEME, ABOUT
}

fun SubScreenState.isOverlaySubscreen(): Boolean = this in setOf(
    SubScreenState.ABOUT,
    SubScreenState.WEARABLE,
    SubScreenState.FILE_EDITOR,
    SubScreenState.AI_FORGE,
    SubScreenState.THEME,
    SubScreenState.VIEWER,
    SubScreenState.EMERGENCY_BROWSER
)

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

@Immutable
data class LastOpenedFileItem(
    val id: String,
    val name: String,
    val sizeText: String,
    val dateText: String,
    val uri: Uri
)

// ---------------------------------------------------------------------------
// MAIN APP SCREEN ARCHITECTURE (EXPRESSIVE NON-BACKDROP ENGINE)
// ---------------------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveMainScreen(
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    extraBlack: Boolean = false,
    onExtraBlackChange: (Boolean) -> Unit = {},
    initialHtmlUri: Uri? = null,
    disableShaders: Boolean = true,
    onDisableShadersChange: (Boolean) -> Unit = {},
    fileEditorContent: @Composable (uri: Uri?, onBack: () -> Unit, onRun: (File) -> Unit) -> Unit = { _, _, _ -> },
    aiForgeContent: @Composable (onBack: () -> Unit, onRun: (String) -> Unit, initialEditUri: Uri?, initialEditFileName: String?, onClearEditTarget: () -> Unit) -> Unit = { onBack, onRun, editUri, editName, onClear ->
        ExpressiveAiForgeScreen(
            onBack = onBack,
            onRunGeneratedHtml = onRun,
            initialEditUri = editUri,
            initialEditFileName = editName,
            onClearEditTarget = onClear
        )
    },
    aiEditContent: @Composable (id: String, name: String, sizeText: String, dateText: String, uri: Uri, onBack: () -> Unit, onRun: (String) -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
    emergencyBrowserContent: @Composable (onBack: () -> Unit) -> Unit = { _ -> },
    aboutContent: @Composable (onBack: () -> Unit) -> Unit = { _ -> },
    themePageContent: @Composable (activeScheme: String, onSchemeSelected: (String) -> Unit, onBack: () -> Unit) -> Unit = { _, _, _ -> },
    wearableConnectionContent: @Composable (onBack: () -> Unit) -> Unit = { _ -> },
    htmlViewerContent: @Composable (uri: Uri, isDom: Boolean, isHw: Boolean, useGecko: Boolean, onWebViewCreated: (WebView?) -> Unit) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE) }
    var forceVerticalRail by remember { mutableStateOf(prefs.getBoolean("force_vertical_rail", false)) }
    val displayState = rememberDeviceDisplayLayoutState(forceVerticalRail = forceVerticalRail)

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
    var activeToolSubStage by remember { mutableStateOf(ToolSubStage.NONE) }
    var showAboutScreen by remember { mutableStateOf(false) }
    var showAiScreen by remember { mutableStateOf(false) }
    var showEmergencyBrowser by remember { mutableStateOf(false) }
    var showFileEditor by remember { mutableStateOf(false) }
    var fileEditorTargetUri by remember { mutableStateOf<Uri?>(null) }
    var aiForgeEditUri by remember { mutableStateOf<Uri?>(null) }
    var aiForgeEditName by remember { mutableStateOf<String?>(null) }
    var showThemePage by remember { mutableStateOf(false) }
    var showWearableConnection by remember { mutableStateOf(false) }
    var activeSchemeName by remember { mutableStateOf(prefs.getString("m3_palette_scheme", "Dynamic Monet") ?: "Dynamic Monet") }

    // Settings State
    var domStorageEnabled by remember { mutableStateOf(prefs.getBoolean("dom_storage", true)) }
    var hwAccelerationEnabled by remember { mutableStateOf(prefs.getBoolean("hw_acceleration", true)) }
    var useGeckoEngine by remember { mutableStateOf(prefs.getBoolean("use_gecko_engine", true)) }

    // SAF Storage Access & State
    val savedFolderUriString = remember { prefs.getString("permitted_folder_uri", null) }
    var permittedFolderUri by remember { mutableStateOf(savedFolderUriString?.toUri()) }
    var selectedHtmlUri by remember { mutableStateOf<Uri?>(null) }

    var isHistoryLoaded by remember { mutableStateOf(false) }
    var lastOpenedList by remember { mutableStateOf<List<LastOpenedFileItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        val diskItems = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            loadLastOpenedFiles(context)
        }
        lastOpenedList = (lastOpenedList + diskItems).distinctBy { it.id }
        isHistoryLoaded = true
    }

    // Tools File Selection State
    var selectedToolFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Pager State for Swipe Navigation: Page 0 = TOOLS, Page 1 = HOME, Page 2 = SETTINGS
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })

    // Active Navigation Tab synced directly with Swipe Page
    val currentTab = when (pagerState.currentPage) {
        0 -> NavigationTab.TOOLS
        2 -> NavigationTab.SETTINGS
        else -> NavigationTab.HOME
    }

    // Reset sub-stage tool state when swiping between tabs
    LaunchedEffect(pagerState.currentPage) {
        activeToolSubStage = ToolSubStage.NONE
    }

    fun updatePref(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    fun updateAndPersistLastOpened(newList: List<LastOpenedFileItem>) {
        lastOpenedList = newList
        saveLastOpenedFiles(context, newList)
    }

    fun saveHtmlFromPaste(htmlContent: String): Uri? {
        Toast.makeText(context, "Creating project...", Toast.LENGTH_SHORT).show()
        val rootUri = permittedFolderUri ?: return null.also { Toast.makeText(context, "Grant folder permission first!", Toast.LENGTH_SHORT).show() }
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return null.also { Toast.makeText(context, "Cannot write to workspace!", Toast.LENGTH_SHORT).show() }
            val timestamp = SimpleDateFormat("ddMMyy_HHmm", Locale.US).format(Date())
            val folderName = "pasted_$timestamp"
            val fileName = "index_$timestamp.html"
            val newFolder = rootDoc.createDirectory(folderName) ?: return null.also { Toast.makeText(context, "Failed to create folder", Toast.LENGTH_SHORT).show() }
            val newFile = newFolder.createFile("text/html", fileName) ?: return null.also { Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show() }
            context.contentResolver.openOutputStream(newFile.uri)?.use { it.write(htmlContent.toByteArray()) }
            val newItem = LastOpenedFileItem(id = System.currentTimeMillis().toString(), name = fileName, sizeText = "Pasted", dateText = SimpleDateFormat("dd.MM.yy", Locale.US).format(Date()), uri = newFile.uri)
            updateAndPersistLastOpened(listOf(newItem) + lastOpenedList)
            Toast.makeText(context, "Saved to $folderName", Toast.LENGTH_SHORT).show()
            return newFile.uri
        } catch (e: Exception) {
            Toast.makeText(context, "Error saving HTML: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        }
    }

    // Handle initialHtmlUri from external startup/intents
    LaunchedEffect(initialHtmlUri) {
        if (initialHtmlUri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(initialHtmlUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}

            val fileName = getFileName(context, initialHtmlUri)
            val newItem = LastOpenedFileItem(
                id = System.currentTimeMillis().toString(),
                name = fileName,
                sizeText = "External",
                dateText = SimpleDateFormat("dd.MM.yy", Locale.US).format(Date()),
                uri = initialHtmlUri
            )
            val currentList = if (isHistoryLoaded) lastOpenedList else loadLastOpenedFiles(context)
            updateAndPersistLastOpened(listOf(newItem) + currentList.filterNot { it.name == fileName })

            if (fileName.endsWith(".zip", ignoreCase = true)) {
                val tempDir = File(context.cacheDir, "extracted_${System.currentTimeMillis()}")
                val htmlFile = decompressZipToDir(context, initialHtmlUri, tempDir)
                if (htmlFile != null && htmlFile.isFile) {
                    selectedHtmlUri = Uri.fromFile(htmlFile)
                } else {
                    Toast.makeText(context, "ZIP Extracted successfully!", Toast.LENGTH_SHORT).show()
                }
            } else {
                selectedHtmlUri = initialHtmlUri
            }
        }
    }

    // SAF Launchers
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            permittedFolderUri = uri
            prefs.edit { putString("permitted_folder_uri", uri.toString()) }
            Toast.makeText(context, "Directory permission granted!", Toast.LENGTH_SHORT).show()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val fileName = getFileName(context, uri)
            val newItem = LastOpenedFileItem(
                id = System.currentTimeMillis().toString(),
                name = fileName,
                sizeText = "Local",
                dateText = SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date()),
                uri = uri
            )

            val updatedList = listOf(newItem) + lastOpenedList.filterNot { it.name == fileName }
            updateAndPersistLastOpened(updatedList)

            if (fileName.endsWith(".zip", ignoreCase = true)) {
                val tempDir = File(context.cacheDir, "extracted_${System.currentTimeMillis()}")
                val htmlFile = decompressZipToDir(context, uri, tempDir)
                if (htmlFile != null && htmlFile.isFile) {
                    selectedHtmlUri = Uri.fromFile(htmlFile)
                } else {
                    Toast.makeText(context, "ZIP Extracted successfully!", Toast.LENGTH_SHORT).show()
                }
            } else {
                selectedHtmlUri = uri
            }
        }
    }

    val compressPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) selectedToolFiles = uris
    }

    val decompressPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) selectedToolFiles = listOf(uri)
    }

    val compressSaveToLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null && selectedToolFiles.isNotEmpty()) {
            val resultMsg = compressUrisToZipSAF(context, selectedToolFiles, uri)
            Toast.makeText(context, resultMsg, Toast.LENGTH_LONG).show()
            activeToolSubStage = ToolSubStage.NONE
            selectedToolFiles = emptyList()
        }
    }

    val decompressSaveToLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null && selectedToolFiles.isNotEmpty()) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            val resultMsg = decompressZipToFolderSAF(context, selectedToolFiles.first(), uri)
            Toast.makeText(context, resultMsg, Toast.LENGTH_LONG).show()
            activeToolSubStage = ToolSubStage.NONE
            selectedToolFiles = emptyList()
        }
    }

    // Determine current active subscreen state
    val currentScreenState = when {
        selectedHtmlUri != null -> SubScreenState.VIEWER
        fileEditorTargetUri != null || showFileEditor -> SubScreenState.FILE_EDITOR
        showAiScreen -> SubScreenState.AI_FORGE
        showEmergencyBrowser -> SubScreenState.EMERGENCY_BROWSER
        showWearableConnection -> SubScreenState.WEARABLE
        showThemePage -> SubScreenState.THEME
        showAboutScreen -> SubScreenState.ABOUT
        else -> SubScreenState.NONE
    }

    // 0. FULL-SCREEN ONBOARDING CHECK (IF NO PERMITTED DIRECTORY)
    if (permittedFolderUri == null) {
        FolderOnboardingScreen(
            onGrantAccess = { folderPickerLauncher.launch(null) }
        )
        return
    }

    val canvasColor = MaterialTheme.colorScheme.surfaceContainerLowest

    // Hybrid UI Re-enable Confirmation State
    var showHybridPrompt by remember { mutableStateOf(false) }

    // ---------------------------------------------------------------------------
    // TWO-LAYER SCAFFOLD WITH ENCAPSULATED SHARED TRANSITION SCOPE
    // ---------------------------------------------------------------------------
    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // LAYER 0: BASE WORKSPACE LAYER (zIndex = 0f, permanently composed)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
                    .then(
                        if (currentScreenState != SubScreenState.NONE) {
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = canvasColor
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (displayState == DeviceDisplayLayoutState.LARGE_DISPLAY) Modifier.padding(end = 88.dp) else Modifier)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                HorizontalPager(
                                    state = pagerState,
                                    beyondViewportPageCount = 1,
                                    userScrollEnabled = !isSearchActive,
                                    modifier = Modifier.fillMaxSize()
                                ) { page: Int ->
                                    when (page) {
                                        0 -> ToolsScreen(
                                            subStage = activeToolSubStage,
                                            onSelectSubStage = {
                                                activeToolSubStage = it
                                                selectedToolFiles = emptyList()
                                            },
                                            selectedFiles = selectedToolFiles,
                                            onPickFiles = {
                                                if (activeToolSubStage == ToolSubStage.COMPRESSOR) {
                                                    compressPickerLauncher.launch(arrayOf("*/*"))
                                                } else {
                                                    decompressPickerLauncher.launch(
                                                        arrayOf("application/zip", "application/x-zip-compressed", "*/*")
                                                    )
                                                }
                                            },
                                            onExecuteAction = {
                                                if (activeToolSubStage == ToolSubStage.COMPRESSOR) {
                                                    compressSaveToLauncher.launch("compressed_${System.currentTimeMillis()}.zip")
                                                } else {
                                                    decompressSaveToLauncher.launch(null)
                                                }
                                            },
                                            sharedTransitionScope = this@SharedTransitionLayout
                                        )

                                        1 -> HomeScreen(
                                            lastOpenedList = lastOpenedList,
                                            onFileClick = { item ->
                                                if (item.name.endsWith(".zip", ignoreCase = true)) {
                                                    val tempDir = File(context.cacheDir, "extracted_${item.id}")
                                                    val htmlFile = decompressZipToDir(context, item.uri, tempDir)
                                                    if (htmlFile != null && htmlFile.isFile) {
                                                        selectedHtmlUri = Uri.fromFile(htmlFile)
                                                    }
                                                } else {
                                                    selectedHtmlUri = item.uri
                                                }
                                            },
                                            onDeleteFile = { id ->
                                                val updatedList = lastOpenedList.filterNot { it.id == id }
                                                updateAndPersistLastOpened(updatedList)
                                            },
                                            onAddFileClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                                            permittedFolderUri = permittedFolderUri,
                                            onRequestFolderPermission = { folderPickerLauncher.launch(null) },
                                            onEditWithAi = { item ->
                                                aiForgeEditUri = item.uri
                                                aiForgeEditName = item.name
                                                showAiScreen = true
                                            },
                                            onOpenFileEditor = { item -> fileEditorTargetUri = item.uri },
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            isSearchActive = isSearchActive,
                                            onSearchActiveChange = { isSearchActive = it },
                                            displayState = displayState
                                        )

                                        2 -> SettingsScreen(
                                            extraBlack = extraBlack,
                                            onExtraBlackChange = onExtraBlackChange,
                                            disableShaders = disableShaders,
                                            onDisableShadersChange = { requestedDisable ->
                                                if (!requestedDisable && disableShaders) {
                                                    // User wants to re-enable shaders -> prompt confirmation
                                                    showHybridPrompt = true
                                                } else {
                                                    onDisableShadersChange(requestedDisable)
                                                }
                                            },
                                            domStorage = domStorageEnabled,
                                            onDomStorageChange = {
                                                domStorageEnabled = it
                                                updatePref("dom_storage", it)
                                            },
                                            hwAcceleration = hwAccelerationEnabled,
                                            onHwAccelChange = {
                                                hwAccelerationEnabled = it
                                                updatePref("hw_acceleration", it)
                                            },
                                            useGeckoEngine = useGeckoEngine,
                                            onGeckoEngineChange = {
                                                useGeckoEngine = it
                                                updatePref("use_gecko_engine", it)
                                            },
                                            permittedFolderUri = permittedFolderUri,
                                            onRequestFolderPermission = { folderPickerLauncher.launch(null) },
                                            onGenerateDummyFiles = {
                                                val dummyList = generateDummyFiles()
                                                val updatedList = dummyList + lastOpenedList
                                                updateAndPersistLastOpened(updatedList)
                                                Toast.makeText(context, "Added 35 dummy files!", Toast.LENGTH_SHORT).show()
                                            },
                                            onClearDummyFiles = {
                                                updateAndPersistLastOpened(emptyList())
                                                Toast.makeText(context, "Cleared workspace files!", Toast.LENGTH_SHORT).show()
                                            },
                                            onOpenAbout = { showAboutScreen = true },
                                            onOpenAiForge = { showAiScreen = true },
                                            onOpenEmergencyBrowser = { showEmergencyBrowser = true },
                                            onOpenFileEditor = { showFileEditor = true },
                                            onOpenThemePage = { showThemePage = true },
                                            onOpenWearableConnection = { showWearableConnection = true },
                                            activeSchemeName = activeSchemeName,
                                            forceVerticalRail = forceVerticalRail,
                                            onForceVerticalRailChange = {
                                                forceVerticalRail = it
                                                updatePref("force_vertical_rail", it)
                                            }
                                        )
                                    }
                                }

                                // Optional Atmospheric Shaders Overlays (Clean static gradients, zero AGSL)
                                if (!disableShaders) {
                                    if (pagerState.currentPage != 2) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(24.dp)
                                                .align(Alignment.TopCenter)
                                                .background(Brush.verticalGradient(listOf(canvasColor, Color.Transparent)))
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .align(Alignment.BottomCenter)
                                            .background(Brush.verticalGradient(listOf(Color.Transparent, canvasColor)))
                                    )
                                }
                            }
                        }

                        // 3-State Adaptive Navigation Docking
                        when (displayState) {
                            DeviceDisplayLayoutState.LARGE_DISPLAY -> {
                                M3EVerticalNavRail(
                                    currentTab = currentTab,
                                    onTabSelected = { selectedTab ->
                                        val targetPage = when (selectedTab) {
                                            NavigationTab.TOOLS -> 0
                                            NavigationTab.HOME -> 1
                                            NavigationTab.SETTINGS -> 2
                                        }
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(targetPage)
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 20.dp, bottom = 88.dp)
                                )
                            }
                            DeviceDisplayLayoutState.PHONE_LANDSCAPE -> {
                                M3EBottomBar(
                                    currentTab = currentTab,
                                    onTabSelected = { selectedTab ->
                                        val targetPage = when (selectedTab) {
                                            NavigationTab.TOOLS -> 0
                                            NavigationTab.HOME -> 1
                                            NavigationTab.SETTINGS -> 2
                                        }
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(targetPage)
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(bottom = 20.dp, end = 16.dp)
                                )
                            }
                            DeviceDisplayLayoutState.PHONE_PORTRAIT -> {
                                M3EBottomBar(
                                    currentTab = currentTab,
                                    onTabSelected = { selectedTab ->
                                        val targetPage = when (selectedTab) {
                                            NavigationTab.TOOLS -> 0
                                            NavigationTab.HOME -> 1
                                            NavigationTab.SETTINGS -> 2
                                        }
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(targetPage)
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // LAYER 1: OVERLAY SUBSCREEN DECK (zIndex = 1f, horizontal slide transition)
            Box(modifier = Modifier.fillMaxSize().zIndex(1f)) {
                AnimatedContent(
                    targetState = currentScreenState,
                    transitionSpec = {
                        if (initialState == SubScreenState.NONE && targetState.isOverlaySubscreen()) {
                            (slideInHorizontally(
                                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                                initialOffsetX = { it }
                            ) + fadeIn(
                                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                            )) togetherWith fadeOut(
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                            )
                        } else if (initialState.isOverlaySubscreen() && targetState == SubScreenState.NONE) {
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
                    label = "SubScreenDeckTransition"
                ) { targetScreen ->
                    when (targetScreen) {
                        SubScreenState.NONE -> {
                            Spacer(modifier = Modifier.fillMaxSize())
                        }

                        SubScreenState.VIEWER -> {
                            val currentUri = selectedHtmlUri
                            if (currentUri != null) {
                                BackHandler {
                                    if (webViewInstance?.canGoBack() == true) {
                                        webViewInstance?.goBack()
                                    } else {
                                        selectedHtmlUri = null
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black)
                                        .statusBarsPadding()
                                ) {
                                    htmlViewerContent(
                                        currentUri,
                                        domStorageEnabled,
                                        hwAccelerationEnabled,
                                        useGeckoEngine
                                    ) { webViewInstance = it }
                                }
                            }
                        }

                        SubScreenState.FILE_EDITOR -> {
                            BackHandler {
                                fileEditorTargetUri = null
                                showFileEditor = false
                            }
                            fileEditorContent(
                                fileEditorTargetUri,
                                {
                                    fileEditorTargetUri = null
                                    showFileEditor = false
                                },
                                { runFile ->
                                    fileEditorTargetUri = null
                                    showFileEditor = false
                                    selectedHtmlUri = Uri.fromFile(runFile)
                                }
                            )
                        }

                        SubScreenState.AI_EDIT -> {
                            LaunchedEffect(Unit) {
                                showAiScreen = true
                            }
                        }

                        SubScreenState.AI_FORGE -> {
                            BackHandler {
                                showAiScreen = false
                                aiForgeEditUri = null
                                aiForgeEditName = null
                            }
                            aiForgeContent(
                                {
                                    showAiScreen = false
                                    aiForgeEditUri = null
                                    aiForgeEditName = null
                                },
                                { generatedHtml ->
                                    showAiScreen = false
                                    aiForgeEditUri = null
                                    aiForgeEditName = null
                                    val savedUri = saveHtmlFromPaste(generatedHtml)
                                    if (savedUri != null) selectedHtmlUri = savedUri
                                },
                                aiForgeEditUri,
                                aiForgeEditName,
                                {
                                    aiForgeEditUri = null
                                    aiForgeEditName = null
                                }
                            )
                        }

                        SubScreenState.EMERGENCY_BROWSER -> {
                            BackHandler { showEmergencyBrowser = false }
                            emergencyBrowserContent { showEmergencyBrowser = false }
                        }

                        SubScreenState.WEARABLE -> {
                            BackHandler { showWearableConnection = false }
                            wearableConnectionContent { showWearableConnection = false }
                        }

                        SubScreenState.THEME -> {
                            BackHandler { showThemePage = false }
                            themePageContent(
                                activeSchemeName,
                                { newScheme ->
                                    activeSchemeName = newScheme
                                    prefs.edit { putString("m3_palette_scheme", newScheme) }
                                },
                                { showThemePage = false }
                            )
                        }

                        SubScreenState.ABOUT -> {
                            BackHandler { showAboutScreen = false }
                            aboutContent { showAboutScreen = false }
                        }
                    }
                }
            }

            // LAYER 2: HOISTED FULL-SCREEN MODAL SEARCH OVERLAY (zIndex = 2f)
            androidx.compose.animation.AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(200)),
                modifier = Modifier.zIndex(2f)
            ) {
                val searchVisibilityScope = this
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
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = searchCardModifier
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "search_transform_pill"),
                                animatedVisibilityScope = searchVisibilityScope,
                                boundsTransform = { _, _ -> spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow) }
                            )
                            .clickable(enabled = false) {}
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "Search files...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 15.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        if (searchQuery.isNotEmpty()) {
                                            searchQuery = ""
                                        } else {
                                            isSearchActive = false
                                            focusManager.clearFocus()
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
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
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (searchQuery.isNotBlank() && filteredList.isEmpty()) {
                                Card(
                                    shape = UniformCardShape,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.SearchOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(38.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "no results",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else if (filteredList.isNotEmpty()) {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.heightIn(max = 320.dp)
                                ) {
                                    items(filteredList, key = { it.id }) { item ->
                                        ExpressiveFileCard(
                                            item = item,
                                            onClick = {
                                                isSearchActive = false
                                                if (item.name.endsWith(".zip", ignoreCase = true)) {
                                                    val tempDir = File(context.cacheDir, "extracted_${item.id}")
                                                    val htmlFile = decompressZipToDir(context, item.uri, tempDir)
                                                    if (htmlFile != null && htmlFile.isFile) {
                                                        selectedHtmlUri = Uri.fromFile(htmlFile)
                                                    }
                                                } else {
                                                    selectedHtmlUri = item.uri
                                                }
                                            },
                                            onDelete = {
                                                val updatedList = lastOpenedList.filterNot { it.id == item.id }
                                                updateAndPersistLastOpened(updatedList)
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
                                Text(
                                    text = "Type to search your files...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet confirmation when re-enabling Hybrid UI shaders
    if (showHybridPrompt) {
        ModalBottomSheet(
            onDismissRequest = { showHybridPrompt = false },
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
                    text = "Switch back to Hybrid UI?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Do you want to return to the hybrid UI with backdrop shaders and blurs?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showHybridPrompt = false },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            showHybridPrompt = false
                            onDisableShadersChange(false)
                        },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Switch")
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// HOME WORKSPACE PAGE (DYNAMIC TOP FADE ON COLLAPSE & SHARED SEARCH)
// ---------------------------------------------------------------------------
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    lastOpenedList: List<LastOpenedFileItem>,
    onFileClick: (LastOpenedFileItem) -> Unit,
    onDeleteFile: (String) -> Unit,
    onAddFileClick: () -> Unit,
    permittedFolderUri: Uri?,
    onRequestFolderPermission: () -> Unit,
    onEditWithAi: (LastOpenedFileItem) -> Unit = {},
    onOpenFileEditor: (LastOpenedFileItem) -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {},
    displayState: DeviceDisplayLayoutState = DeviceDisplayLayoutState.PHONE_PORTRAIT
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE) }
    val easterEggController = rememberExpressiveEasterEggController(prefs)
    val listState = rememberLazyListState()
    val canvasColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val isLightMode = canvasColor.luminance() > 0.5f

    val scrollFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 180f).coerceIn(0f, 1f)
        }
    }

    val isLargeDisplay = displayState == DeviceDisplayLayoutState.LARGE_DISPLAY
    val isCollapsedHeader = (scrollFraction > 0.5f) || isLargeDisplay

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // DYNAMIC HEADER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCollapsedHeader) 52.dp else (110 - (58 * scrollFraction)).dp),
                contentAlignment = Alignment.Center
            ) {
                if (!isCollapsedHeader) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = (1f - (scrollFraction * 1.25f)).coerceIn(0f, 1f)
                                scaleX = 1f - (0.08f * scrollFraction)
                                scaleY = 1f - (0.08f * scrollFraction)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                            Icon(
                                painter = painterResource(R.drawable.ic_htmlgom_title_logo),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(46.dp, 30.dp)
                            )
                            Text(
                                text = "HtmlGO - M",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isSearchActive,
                            enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.95f),
                            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.95f)
                        ) {
                            val searchScope = this
                            val searchCardModifier = if (sharedTransitionScope != null) {
                                with(sharedTransitionScope) {
                                    Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .sharedBounds(
                                            sharedContentState = rememberSharedContentState(key = "search_transform_pill"),
                                            animatedVisibilityScope = searchScope,
                                            boundsTransform = { _, _ -> spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow) }
                                        )
                                        .clickable { onSearchActiveChange(true) }
                                }
                            } else {
                                Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clickable { onSearchActiveChange(true) }
                            }

                            Card(
                                shape = CircleShape,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = searchCardModifier
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Search files...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = if (isLargeDisplay) 1f else ((scrollFraction - 0.1f) * 1.2f).coerceIn(0f, 1f)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
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
                            Icon(
                                painter = painterResource(R.drawable.ic_htmlgom_title_logo),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp, 22.dp)
                            )
                            Text(
                                text = "HtmlGO - M",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isSearchActive,
                            enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f),
                            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f)
                        ) {
                            val searchScope = this
                            val searchBtnModifier = if (sharedTransitionScope != null) {
                                with(sharedTransitionScope) {
                                    Modifier
                                        .size(47.5.dp)
                                        .sharedBounds(
                                            sharedContentState = rememberSharedContentState(key = "search_transform_pill"),
                                            animatedVisibilityScope = searchScope,
                                            boundsTransform = { _, _ -> spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow) }
                                        )
                                }
                            } else {
                                Modifier.size(47.5.dp)
                            }

                            IconButton(
                                onClick = { onSearchActiveChange(true) },
                                modifier = searchBtnModifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.5.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Storage Access Banner Card
            if (permittedFolderUri == null) {
                Card(
                    shape = UniformCardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRequestFolderPermission() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Grant Storage Folder Access",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Required for relative asset & workspace output loading",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // LAST OPENED LABEL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = if (isCollapsedHeader) Alignment.Center else Alignment.CenterStart
            ) {
                Text(
                    text = "last opened",
                    fontSize = if (isCollapsedHeader) 12.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer {
                        alpha = if (isLargeDisplay) 1f else (1f - (0.35f * scrollFraction)).coerceIn(0.65f, 1f)
                    }
                )
            }

            // WORKSPACE FILE LIST
            Box(modifier = Modifier.weight(1f)) {
                if (lastOpenedList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = UniformCardShape,
                            colors = CardDefaults.cardColors(containerColor = if (isLightMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1.00f) else MaterialTheme.colorScheme.surfaceContainerHigh),
                            border = if (isLightMode) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.size(220.dp, 160.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FolderOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "no files here yet\ntap + to pick a file",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                        modifier = Modifier.fillMaxSize()
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

                    // Top Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                alpha = scrollFraction
                            }
                            .background(Brush.verticalGradient(listOf(canvasColor, Color.Transparent)))
                    )

                    // Bottom Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .align(Alignment.BottomCenter)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, canvasColor)))
                    )
                }
            }
        }

        val fabModifier = when (displayState) {
            DeviceDisplayLayoutState.LARGE_DISPLAY -> Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp)
            DeviceDisplayLayoutState.PHONE_LANDSCAPE -> Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 108.dp)
            DeviceDisplayLayoutState.PHONE_PORTRAIT -> Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 112.dp)
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddFileClick,
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = fabModifier
                .size(56.dp)
                .then(if (isLightMode) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)) else Modifier)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add File", modifier = Modifier.size(28.dp))
        }

        // Overlay Deck: Toast & Achievement Banner
        ExpressiveToastHost(
            controller = easterEggController,
            displayState = displayState
        )
        ExpressiveMinecraftAchievementBanner(
            controller = easterEggController
        )
        ExpressiveEasterEggAudioPlayer(
            isClimaxPlaying = easterEggController.isClimaxPlaying.value
        )
    }
}

// ---------------------------------------------------------------------------
// EXPRESSIVE CONTAINER FILE CARD (SPRING PHYSICS & DIRECTIONAL SWIPE DELETION)
// ---------------------------------------------------------------------------
@Composable
fun ExpressiveFileCard(
    item: LastOpenedFileItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEditWithAi: () -> Unit = {},
    onOpenFileEditor: () -> Unit = {},
    isScrollInProgress: Boolean = false
) {
    val context = LocalContext.current
    var isDeleting by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
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

    Card(
        shape = UniformCardShape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (isDeleting) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.4f)) else (if (isLightMode) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)),
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
                    onLongPress = { showContextMenu = true },
                    onTap = { if (!isDeleting) onClick() }
                )
            }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.name.endsWith(".zip", ignoreCase = true)) Icons.Default.FolderZip else Icons.Default.Html,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Last access: ${item.dateText}",
                        fontSize = 11.sp,
                        fontWeight = if (isLightMode) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isLightMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        contentDescription = "Delete",
                        tint = if (isSystemInDarkTheme()) Color(0xFFFFB4AB) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isLightMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = item.sizeText,
                            color = if (isLightMode) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = if (isLightMode) 3.5.dp else 3.dp)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (isLightMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = item.dateText,
                            color = if (isLightMode) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = if (isLightMode) 3.5.dp else 3.dp)
                        )
                    }
                }
            }

            // WINDOWS-STYLE RIGHT-CLICK / LONG-PRESS CONTEXT MENU
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
                            FileSharingHelper.shareFileFromUri(context, item.uri, item.name, asZip = false)
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Share HTML", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            showContextMenu = false
                            FileSharingHelper.shareFileFromUri(context, item.uri, item.name, asZip = false)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share as ZIP", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.tertiary) },
                        onClick = {
                            showContextMenu = false
                            FileSharingHelper.shareFileFromUri(context, item.uri, item.name, asZip = true)
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
                        Toast.makeText(context, "Sending to Watch...", Toast.LENGTH_SHORT).show()
                        sendFileToWatch(context, item.uri) { success ->
                            if (success) {
                                Toast.makeText(context, "Sent successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Watch not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// DEVELOPER MODE DUMMY FILE GENERATOR
// ---------------------------------------------------------------------------
fun generateDummyFiles(): List<LastOpenedFileItem> {
    val sampleNames = listOf(
        "index.html", "game_v2.zip", "portfolio_template.html", "threejs_demo.zip",
        "canvas_animation.html", "dashboard_v1.zip", "phaser_game.zip", "webgl_shader.html",
        "platformer_engine.zip", "snake_game.html", "tetris_clone.html", "rpg_maker.zip",
        "flappy_bird.html", "pong_v2.html", "mario_clone.zip", "calculator.html",
        "todo_app.html", "weather_widget.zip", "pwa_shell.html", "audio_visualizer.zip",
        "3d_cube_demo.html", "chart_dashboard.zip", "markdown_editor.html", "chat_ui.zip",
        "retro_arcade.zip", "synth_wave.html", "space_invaders.zip", "cookie_clicker.html",
        "memory_game.zip", "tic_tac_toe.html", "minesweeper.zip", "pixel_art_editor.html",
        "quiz_app.zip", "breakout_game.html", "sudoku_solver.zip"
    )

    return sampleNames.mapIndexed { index, name ->
        LastOpenedFileItem(
            id = "dummy_${System.currentTimeMillis()}_$index",
            name = name,
            sizeText = if (name.endsWith(".zip")) "${(12..85).random() / 10.0} MB" else "${(15..450).random()} KB",
            dateText = String.format(Locale.US, "%02d.%02d.26", (1..28).random(), (1..12).random()),
            uri = Uri.EMPTY
        )
    }
}

// ---------------------------------------------------------------------------
// TOOLS PAGE (WITH SHARED ELEMENT EXPANSION & BACK HANDLER)
// ---------------------------------------------------------------------------
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ToolsScreen(
    subStage: ToolSubStage,
    onSelectSubStage: (ToolSubStage) -> Unit,
    selectedFiles: List<Uri>,
    onPickFiles: () -> Unit,
    onExecuteAction: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null
) {
    val context = LocalContext.current

    // Scoped back navigation containment: revert stage to NONE rather than exit app
    BackHandler(enabled = subStage != ToolSubStage.NONE) {
        onSelectSubStage(ToolSubStage.NONE)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val isLightMode = MaterialTheme.colorScheme.surfaceContainerLowest.luminance() > 0.5f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = CircleShape,
                color = if (isLightMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1.00f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Text(
                    text = "tools",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = subStage,
                transitionSpec = {
                    fadeIn(tween(250)) togetherWith fadeOut(tween(200))
                },
                label = "ToolsScreenMorph"
            ) { stage ->
                if (stage == ToolSubStage.NONE) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val compressCardModifier = if (sharedTransitionScope != null) {
                            with(sharedTransitionScope) {
                                Modifier
                                    .fillMaxWidth()
                                    .sharedBounds(
                                        sharedContentState = rememberSharedContentState(key = "tool_compress_card"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = { _, _ -> spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow) }
                                    )
                                    .clickable { onSelectSubStage(ToolSubStage.COMPRESSOR) }
                            }
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSubStage(ToolSubStage.COMPRESSOR) }
                        }

                        Card(
                            shape = UniformCardShape,
                            colors = CardDefaults.cardColors(containerColor = if (isLightMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1.00f) else MaterialTheme.colorScheme.surfaceContainerHigh),
                            border = if (isLightMode) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = compressCardModifier
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderZip,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Compress Project to ZIP",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Package project directory into archive",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        val decompressCardModifier = if (sharedTransitionScope != null) {
                            with(sharedTransitionScope) {
                                Modifier
                                    .fillMaxWidth()
                                    .sharedBounds(
                                        sharedContentState = rememberSharedContentState(key = "tool_decompress_card"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = { _, _ -> spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow) }
                                    )
                                    .clickable { onSelectSubStage(ToolSubStage.DECOMPRESSOR) }
                            }
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSubStage(ToolSubStage.DECOMPRESSOR) }
                        }

                        Card(
                            shape = UniformCardShape,
                            colors = CardDefaults.cardColors(containerColor = if (isLightMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1.00f) else MaterialTheme.colorScheme.surfaceContainerHigh),
                            border = if (isLightMode) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = decompressCardModifier
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Unarchive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Decompress ZIP Archive",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Extract project archive into folder",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val currentKey = if (stage == ToolSubStage.COMPRESSOR) "tool_compress_card" else "tool_decompress_card"
                    val expandedModifier = if (sharedTransitionScope != null) {
                        with(sharedTransitionScope) {
                            Modifier
                                .fillMaxWidth()
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = currentKey),
                                    animatedVisibilityScope = this@AnimatedContent,
                                    boundsTransform = { _, _ -> spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow) }
                                )
                        }
                    } else {
                        Modifier.fillMaxWidth()
                    }

                    Card(
                        shape = UniformCardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = expandedModifier
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { onSelectSubStage(ToolSubStage.NONE) }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (stage == ToolSubStage.COMPRESSOR) "Compress to ZIP" else "Decompress ZIP",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            ) {
                                if (selectedFiles.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.FolderOpen,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Text(
                                                text = "No files selected",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(selectedFiles) { uri ->
                                            val fileName = remember(uri) { getFileName(context, uri) }
                                            val isZip = fileName.endsWith(".zip", ignoreCase = true)
                                            val isHtml = fileName.endsWith(".html", ignoreCase = true) || fileName.endsWith(".htm", ignoreCase = true)

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = when {
                                                        isZip -> Icons.Default.FolderZip
                                                        isHtml -> Icons.Default.Html
                                                        else -> Icons.Default.Description
                                                    },
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = fileName,
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

                            Text(
                                text = if (selectedFiles.isNotEmpty()) "${selectedFiles.size} file(s) selected" else "pick files",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = onPickFiles,
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Select Files", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = onExecuteAction,
                                    enabled = selectedFiles.isNotEmpty(),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = if (stage == ToolSubStage.COMPRESSOR) "Compress" else "Decompress",
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
    }
}

// ---------------------------------------------------------------------------
// SETTINGS PAGE WITH SYSTEM SUITE, WEARABLE SYNC, DEVELOPER MODE & ABOUT
// ---------------------------------------------------------------------------
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
    onOpenAbout: () -> Unit = {},
    onOpenAiForge: () -> Unit = {},
    onOpenEmergencyBrowser: () -> Unit = {},
    onOpenFileEditor: () -> Unit = {},
    onOpenThemePage: () -> Unit = {},
    onOpenWearableConnection: () -> Unit = {},
    activeSchemeName: String = "Dynamic Monet",
    forceVerticalRail: Boolean = false,
    onForceVerticalRailChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE) }
    var isEasterEggEverUnlocked by remember { mutableStateOf(prefs.getBoolean(PREF_EASTER_EGG_EVER_UNLOCKED, false)) }
    var devModeEnabled by remember { mutableStateOf(prefs.getBoolean("developer_mode", false) || forceVerticalRail) }
    var agslGovernorEnabled by remember { mutableStateOf(prefs.getBoolean("agsl_governor_enabled", true)) }
    val scrollState = rememberScrollState()
    val canvasColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val isLightMode = canvasColor.luminance() > 0.5f
    val settingsCardBorder = if (isLightMode) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    val isScrolled by remember { derivedStateOf { scrollState.value > 0 } }
    val isActivelyScrolling by remember { derivedStateOf { scrollState.isScrollInProgress } }

    val targetBackdropHeight = when {
        isActivelyScrolling -> 110.dp
        isScrolled -> 80.dp
        else -> 0.dp
    }

    val animatedBackdropHeight by animateDpAsState(
        targetValue = targetBackdropHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ExpressiveSettingsTopBackdropHeight"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 64.dp, bottom = 120.dp)
        ) {
            // --- SYSTEM SUITE SECTION ---
        Text(
            text = "SYSTEM SUITE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            shape = UniformCardShape,
            colors = CardDefaults.cardColors(containerColor = if (isLightMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1.00f) else MaterialTheme.colorScheme.surfaceContainerHigh),
            border = settingsCardBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenEmergencyBrowser() }
                        .padding(16.dp),
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenFileEditor() }
                        .padding(16.dp),
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAiForge() }
                        .padding(16.dp),
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

        Spacer(modifier = Modifier.height(20.dp))

        // --- WEARABLE SYNC SECTION ---
        Text(
            text = "WEARABLE SYNC",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            shape = UniformCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = settingsCardBorder,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenWearableConnection() }
        ) {
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

        Spacer(modifier = Modifier.height(20.dp))

        // --- APPEARANCE & THEMES SECTION ---
        Text(
            text = "APPEARANCE & THEMES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            shape = UniformCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = settingsCardBorder,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenThemePage() }
        ) {
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

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            shape = UniformCardShape,
            colors = CardDefaults.cardColors(containerColor = if (isLightMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1.00f) else MaterialTheme.colorScheme.surfaceContainerHigh),
            border = settingsCardBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSwitchRow(
                    title = "Extra Black Mode",
                    subtitle = "Pure dark theme for AMOLED screens",
                    checked = extraBlack,
                    onCheckedChange = onExtraBlackChange
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsSwitchRow(
                    title = "Disable Overlay Shaders",
                    subtitle = "Turn off top/bottom ambient gradient effects",
                    checked = disableShaders,
                    onCheckedChange = onDisableShadersChange
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- WEBVIEW ENGINE SECTION ---
        Text(
            text = "WEBVIEW ENGINE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            shape = UniformCardShape,
            colors = CardDefaults.cardColors(containerColor = if (isLightMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1.00f) else MaterialTheme.colorScheme.surfaceContainerHigh),
            border = settingsCardBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSwitchRow(
                    title = "DOM Storage API",
                    subtitle = "Enable localStorage and sessionStorage",
                    checked = domStorage,
                    onCheckedChange = onDomStorageChange
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsSwitchRow(
                    title = "Hardware Acceleration",
                    subtitle = "Enable GPU rendering for WebGL & Canvas",
                    checked = hwAcceleration,
                    onCheckedChange = onHwAccelChange
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsSwitchRow(
                    title = "GeckoView Engine",
                    subtitle = "High-performance Mozilla Firefox browser core",
                    checked = useGeckoEngine,
                    onCheckedChange = onGeckoEngineChange
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- DEVELOPER OPTIONS SECTION ---
        Text(
            text = "DEVELOPER OPTIONS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            shape = UniformCardShape,
            colors = CardDefaults.cardColors(containerColor = if (isLightMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1.00f) else MaterialTheme.colorScheme.surfaceContainerHigh),
            border = settingsCardBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSwitchRow(
                    title = "Developer Mode",
                    subtitle = "Unlock testing tools and dummy file generation",
                    checked = devModeEnabled,
                    onCheckedChange = {
                        devModeEnabled = it
                        prefs.edit().putBoolean("developer_mode", it).apply()
                    }
                )

                if (devModeEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onGenerateDummyFiles,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("+35 Dummy Files", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onClearDummyFiles,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("Clear All Files", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            resetEasterEggState(prefs)
                            isEasterEggEverUnlocked = false
                            Toast.makeText(context, "Easter egg state reset", Toast.LENGTH_SHORT).show()
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("Reset Easter Egg State", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    SettingsSwitchRow(
                        title = "Force Vertical Rail",
                        subtitle = "Enable vertical railing navigation bar regardless of display size",
                        checked = forceVerticalRail,
                        onCheckedChange = onForceVerticalRailChange
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    SettingsSwitchRow(
                        title = "AGSL Performance Governor",
                        subtitle = "Dynamically optimize shaders based on thermals. Turn off to return to standard configuration of backdrop",
                        checked = agslGovernorEnabled,
                        onCheckedChange = {
                            agslGovernorEnabled = it
                            prefs.edit().putBoolean("agsl_governor_enabled", it).apply()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- ABOUT SECTION ---
        Text(
            text = "ABOUT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            shape = UniformCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = settingsCardBorder,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenAbout() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "About HTMLGOM",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Version, specs & application details",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ExpressiveEasterEggCommemorationCard(isUnlocked = isEasterEggEverUnlocked)
    }

    // --- LAYER 1: DYNAMIC TOP BACKDROP (Zero AGSL Static Gradient) ---
    if (animatedBackdropHeight > 0.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedBackdropHeight)
                .align(Alignment.TopCenter)
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

    // --- LAYER 2: PINNED SETTINGS CAPSULE PILL ---
    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.TopCenter)
            .align(Alignment.TopCenter)
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

// ---------------------------------------------------------------------------
// SETTINGS ROW COMPOSABLE
// ---------------------------------------------------------------------------
@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// ---------------------------------------------------------------------------
// ADAPTIVE VERTICAL NAVIGATION RAIL (FOR LARGE DISPLAYS / TABLETS)
// ---------------------------------------------------------------------------
@Composable
fun M3EVerticalNavRail(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier = modifier
            .width(64.dp)
            .height(296.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                Triple(NavigationTab.TOOLS, Icons.Default.Build, "Tools"),
                Triple(NavigationTab.HOME, Icons.Default.Home, "Home"),
                Triple(NavigationTab.SETTINGS, Icons.Default.Settings, "Settings")
            )
            tabs.forEach { (tab, icon, title) ->
                val isSelected = currentTab == tab
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                    label = "railTabBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                    label = "railTabContent"
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = bgColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .bouncyClickable { onTabSelected(tab) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// DOCKED BOTTOM NAVIGATION BAR (WITH OUTLINE BORDER & SLIDING SPRING PILL)
// ---------------------------------------------------------------------------
@Composable
fun M3EBottomBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        modifier = modifier.wrapContentWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Build,
                label = "Tools",
                isSelected = currentTab == NavigationTab.TOOLS,
                onClick = { onTabSelected(NavigationTab.TOOLS) }
            )
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = currentTab == NavigationTab.HOME,
                onClick = { onTabSelected(NavigationTab.HOME) }
            )
            BottomNavItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = currentTab == NavigationTab.SETTINGS,
                onClick = { onTabSelected(NavigationTab.SETTINGS) }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tabBgColor"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tabContentColor"
    )

    Surface(
        shape = CircleShape,
        color = animatedBgColor,
        modifier = Modifier.bouncyClickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = animatedContentColor,
                modifier = Modifier.size(20.dp)
            )
            if (isSelected) {
                Text(
                    text = label,
                    color = animatedContentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// HTML VIEWER COMPOSABLE (STANDALONE WEBVIEW ENGINE FALLBACK)
// ---------------------------------------------------------------------------
@Composable
fun HtmlViewer(
    fileUri: Uri,
    isDomEnabled: Boolean,
    isHwAccelEnabled: Boolean,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var webViewFileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var pendingWebPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingGeoOrigin by remember { mutableStateOf<String?>(null) }
    var pendingGeoCallback by remember { mutableStateOf<GeolocationPermissions.Callback?>(null) }

    var webProgress by remember { mutableFloatStateOf(0f) }
    var isWebLoading by remember { mutableStateOf(false) }

    val webPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
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
                            val resources = request.resources ?: emptyArray()
                            val needed = mutableListOf<String>()

                            resources.forEach { res ->
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
                        cacheMode = WebSettings.LOAD_NO_CACHE

                        val defaultUa = userAgentString
                        userAgentString = defaultUa
                            .replace("; wv", "")
                            .replace(";wv", "")
                            .replace("Version/4.0 ", "")
                    }
                    if (isHwAccelEnabled) {
                        setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                    } else {
                        setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
                    }
                    onWebViewCreated(this)
                }
            },
            update = { webView ->
                webView.loadUrl(fileUri.toString())
            },
            modifier = Modifier.fillMaxSize()
        )
        ExpressiveWebProgressIndicator(
            progress = webProgress,
            isLoading = isWebLoading,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

// ---------------------------------------------------------------------------
// FULL-SCREEN FOLDER PERMISSION ONBOARDING SCREEN
// ---------------------------------------------------------------------------
@Composable
fun FolderOnboardingScreen(
    onGrantAccess: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderSpecial,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Select Workspace Folder",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "HTMLGOM requires permission to access a dedicated folder directory. This allows the app to load, decompress, and store your local HTML web projects and games securely.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = onGrantAccess,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Grant Folder Access",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// HELPER FUNCTIONS & UTILITIES
// ---------------------------------------------------------------------------
fun saveLastOpenedFiles(context: Context, list: List<LastOpenedFileItem>) {
    try {
        val jsonArray = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("sizeText", item.sizeText)
            obj.put("dateText", item.dateText)
            obj.put("uri", item.uri.toString())
            jsonArray.put(obj)
        }
        context.getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("last_opened_list_json", jsonArray.toString())
            .apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadLastOpenedFiles(context: Context): List<LastOpenedFileItem> {
    val list = mutableListOf<LastOpenedFileItem>()
    try {
        val jsonString = context.getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE)
            .getString("last_opened_list_json", null) ?: return list
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                LastOpenedFileItem(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    sizeText = obj.getString("sizeText"),
                    dateText = obj.getString("dateText"),
                    uri = Uri.parse(obj.getString("uri"))
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

fun compressUrisToZipSAF(context: Context, uris: List<Uri>, zipFileUri: Uri): String {
    return try {
        val outputStream = context.contentResolver.openOutputStream(zipFileUri)
            ?: return "Failed to open output stream"
        val zos = ZipOutputStream(BufferedOutputStream(outputStream))
        val buffer = ByteArray(2048)

        uris.forEach { uri ->
            val fileName = getFileName(context, uri)
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val entry = ZipEntry(fileName)
                zos.putNextEntry(entry)
                var count: Int
                val bis = BufferedInputStream(inputStream, 2048)
                while (bis.read(buffer, 0, 2048).also { count = it } != -1) {
                    zos.write(buffer, 0, count)
                }
                bis.close()
                zos.closeEntry()
            }
        }
        zos.close()
        "Compression complete!"
    } catch (e: Exception) {
        e.printStackTrace()
        "Error compressing: ${e.message}"
    }
}

fun decompressZipToFolderSAF(context: Context, zipUri: Uri, targetFolderUri: Uri): String {
    return try {
        val inputStream = context.contentResolver.openInputStream(zipUri)
            ?: return "Failed to open ZIP"
        val zis = ZipInputStream(BufferedInputStream(inputStream))
        var entry = zis.nextEntry

        val targetDoc = DocumentFile.fromTreeUri(context, targetFolderUri)
            ?: return "Invalid target directory"

        val buffer = ByteArray(2048)
        while (entry != null) {
            if (!entry.isDirectory) {
                val entryName = entry.name.substringAfterLast("/")
                val newFile = targetDoc.createFile("application/octet-stream", entryName)
                if (newFile != null) {
                    val outStream = context.contentResolver.openOutputStream(newFile.uri)
                    if (outStream != null) {
                        var count: Int
                        while (zis.read(buffer, 0, 2048).also { count = it } != -1) {
                            outStream.write(buffer, 0, count)
                        }
                        outStream.close()
                    }
                }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
        zis.close()
        "Decompression successful!"
    } catch (e: Exception) {
        e.printStackTrace()
        "Error decompressing: ${e.message}"
    }
}

fun decompressZipToDir(context: Context, zipUri: Uri, targetDir: File): File? =
    ZipArchiveSystem.decompressZipToDir(context, zipUri, targetDir)

fun findHtmlFile(dir: File): File? =
    ZipArchiveSystem.findHtmlFile(dir)

fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = it.getString(index)
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "unknown_file"
}

fun sendFileToWatch(
    context: Context,
    fileUri: Uri,
    onResult: (Boolean) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val nodeClient = Wearable.getNodeClient(context)
            val nodes = nodeClient.connectedNodes.await()
            val watchNode = nodes.firstOrNull()
            if (watchNode == null) {
                withContext(Dispatchers.Main) { onResult(false) }
                return@launch
            }

            val fileName = getFileName(context, fileUri).replace("/", "_")
            val tempFile = File(context.cacheDir, "transfer_${System.currentTimeMillis()}_$fileName")
            context.contentResolver.openInputStream(fileUri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val channelClient = Wearable.getChannelClient(context)
            val channelPath = "/file_transfer/$fileName"
            val channel = channelClient.openChannel(watchNode.id, channelPath).await()
            channelClient.sendFile(channel, Uri.fromFile(tempFile)).await()

            withContext(Dispatchers.Main) {
                onResult(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onResult(false)
            }
        }
    }
}
