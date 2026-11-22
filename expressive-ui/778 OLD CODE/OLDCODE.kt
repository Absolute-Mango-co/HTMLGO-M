package com.absolutemango.htmlgom

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.launch
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

@Immutable
data class LastOpenedFileItem(
    val id: String,
    val name: String,
    val sizeText: String,
    val dateText: String,
    val uri: Uri
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display High Refresh Rate (120Hz smooth scrolling)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            val maxMode = currentDisplay?.supportedModes?.maxByOrNull { it.refreshRate }
            if (maxMode != null) {
                val lp = window.attributes
                lp.preferredDisplayModeId = maxMode.modeId
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    lp.preferredRefreshRate = maxMode.refreshRate
                }
                window.attributes = lp
            }
        }

        setContent {
            val prefs = getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE)
            val extraBlack = remember { mutableStateOf(prefs.getBoolean("extra_black", false)) }

            HTMLGOMTheme(extraBlack = extraBlack.value) {
                MainAppScreen(
                    extraBlack = extraBlack.value,
                    onExtraBlackChange = {
                        extraBlack.value = it
                        prefs.edit().putBoolean("extra_black", it).apply()
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// MAIN APP SCREEN ARCHITECTURE
// ---------------------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainAppScreen(
    extraBlack: Boolean,
    onExtraBlackChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE) }

    var activeToolSubStage by remember { mutableStateOf(ToolSubStage.NONE) }
    var showAboutScreen by remember { mutableStateOf(false) }

    // Settings State
    var disableShaders by remember { mutableStateOf(prefs.getBoolean("disable_shaders", false)) }
    var domStorageEnabled by remember { mutableStateOf(prefs.getBoolean("dom_storage", true)) }
    var hwAccelerationEnabled by remember { mutableStateOf(prefs.getBoolean("hw_acceleration", true)) }

    // SAF Storage Access & State
    val savedFolderUriString = remember { prefs.getString("permitted_folder_uri", null) }
    var permittedFolderUri by remember { mutableStateOf(savedFolderUriString?.toUri()) }
    var selectedHtmlUri by remember { mutableStateOf<Uri?>(null) }

    var lastOpenedList by remember { mutableStateOf(loadLastOpenedFiles(context)) }

    // Tools File Selection State
    var selectedToolFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Pager State for Swipe Navigation: Page 0 = TOOLS (Left), Page 1 = HOME (Center), Page 2 = SETTINGS (Right)
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

    // Intercept hardware / gesture back navigation when About screen is open
    BackHandler(enabled = showAboutScreen) {
        showAboutScreen = false
    }

    // OVERLAY: ABOUT HTMLGOM SCREEN
    if (showAboutScreen) {
        AboutScreen(
            onBackClick = { showAboutScreen = false }
        )
        return
    }

    val canvasColor = MaterialTheme.colorScheme.surfaceContainerLowest

    // 1. ACTIVE HTML FULLSCREEN PLAYER VIEW
    val currentHtmlUri = selectedHtmlUri
    if (currentHtmlUri != null) {
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
            HtmlViewer(
                fileUri = currentHtmlUri,
                isDomEnabled = domStorageEnabled,
                isHwAccelEnabled = hwAccelerationEnabled,
                onWebViewCreated = { webViewInstance = it },
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    // 2. FULL-SCREEN ONBOARDING CHECK (IF NO PERMITTED DIRECTORY)
    if (permittedFolderUri == null) {
        FolderOnboardingScreen(
            onGrantAccess = { folderPickerLauncher.launch(null) }
        )
        return
    }

    // 3. MAIN APP UI SCAFFOLD WITH HORIZONTAL SWIPE PAGER
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = canvasColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = 1,
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
                                }
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
                                onRequestFolderPermission = { folderPickerLauncher.launch(null) }
                            )

                            2 -> SettingsScreen(
                                extraBlack = extraBlack,
                                onExtraBlackChange = onExtraBlackChange,
                                disableShaders = disableShaders,
                                onDisableShadersChange = {
                                    disableShaders = it
                                    updatePref("disable_shaders", it)
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
                                onOpenAbout = { showAboutScreen = true }
                            )
                        }
                    }

                    // Optional Atmospheric Shaders Overlays
                    if (!disableShaders) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .align(Alignment.TopCenter)
                                .background(Brush.verticalGradient(listOf(canvasColor, Color.Transparent)))
                        )
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

            // Docked Bottom Bar Navigation
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

// ---------------------------------------------------------------------------
// HOME WORKSPACE PAGE (DYNAMIC TOP FADE ON COLLAPSE)
// ---------------------------------------------------------------------------
@Composable
fun HomeScreen(
    lastOpenedList: List<LastOpenedFileItem>,
    onFileClick: (LastOpenedFileItem) -> Unit,
    onDeleteFile: (String) -> Unit,
    onAddFileClick: () -> Unit,
    permittedFolderUri: Uri?,
    onRequestFolderPermission: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val listState = rememberLazyListState()
    val canvasColor = MaterialTheme.colorScheme.surfaceContainerLowest

    val scrollFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 180f).coerceIn(0f, 1f)
        }
    }

    val filteredList = remember(searchQuery, lastOpenedList) {
        if (searchQuery.isBlank()) emptyList()
        else lastOpenedList.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

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
                    .height(if (scrollFraction > 0.5f) 52.dp else (110 - (58 * scrollFraction)).dp),
                contentAlignment = Alignment.Center
            ) {
                if (scrollFraction < 0.9f) {
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "HtmlGO - M",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Card(
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clickable { isSearchActive = true }
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

                if (scrollFraction > 0.1f) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = ((scrollFraction - 0.1f) * 1.2f).coerceIn(0f, 1f)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "HtmlGO - M",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
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
                contentAlignment = if (scrollFraction > 0.5f) Alignment.Center else Alignment.CenterStart
            ) {
                Text(
                    text = "last opened",
                    fontSize = if (scrollFraction > 0.5f) 12.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer {
                        alpha = (1f - (0.35f * scrollFraction)).coerceIn(0.65f, 1f)
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                onDelete = { onDeleteFile(item.id) }
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

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddFileClick,
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 90.dp)
                .size(56.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add File", modifier = Modifier.size(28.dp))
        }

        // POP-OUT SEARCH OVERLAY
        AnimatedVisibility(
            visible = isSearchActive,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable {
                        isSearchActive = false
                        searchQuery = ""
                        focusManager.clearFocus()
                    }
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .align(Alignment.TopCenter)
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
                                            onFileClick(item)
                                        },
                                        onDelete = { onDeleteFile(item.id) }
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

// ---------------------------------------------------------------------------
// EXPRESSIVE CONTAINER FILE CARD
// ---------------------------------------------------------------------------
@Composable
fun ExpressiveFileCard(
    item: LastOpenedFileItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = UniformCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
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
            Text(
                text = item.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
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
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = item.sizeText,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = item.dateText,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
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
            dateText = String.format("%02d.%02d.26", (1..28).random(), (1..12).random()),
            uri = Uri.EMPTY
        )
    }
}

// ---------------------------------------------------------------------------
// TOOLS PAGE
// ---------------------------------------------------------------------------
@Composable
fun ToolsScreen(
    subStage: ToolSubStage,
    onSelectSubStage: (ToolSubStage) -> Unit,
    selectedFiles: List<Uri>,
    onPickFiles: () -> Unit,
    onExecuteAction: () -> Unit
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "tools",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                shape = UniformCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                border = BorderStroke(0.1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSubStage(ToolSubStage.COMPRESSOR) }
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Compress,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Compressor",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Compress HTML and assets into a clean ZIP",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                shape = UniformCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                border = BorderStroke(0.1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSubStage(ToolSubStage.DECOMPRESSOR) }
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Decompressor",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Decompress ZIP bundles into playable workspace folders",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (subStage != ToolSubStage.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { onSelectSubStage(ToolSubStage.NONE) },
                contentAlignment = Alignment.BottomCenter
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
                        .padding(bottom = 88.dp)
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                text = if (subStage == ToolSubStage.COMPRESSOR) "Compressor" else "Decompressor",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
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
}

// ---------------------------------------------------------------------------
// SETTINGS PAGE WITH DEVELOPER MODE & ABOUT
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
    permittedFolderUri: Uri?,
    onRequestFolderPermission: () -> Unit,
    onGenerateDummyFiles: () -> Unit,
    onClearDummyFiles: () -> Unit,
    onOpenAbout: () -> Unit = {}
) {
    var devModeEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 120.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "settings",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- APPEARANCE SECTION ---
        Text(
            text = "APPEARANCE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            shape = UniformCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSwitchRow(
                    title = "Developer Mode",
                    subtitle = "Unlock testing tools and dummy file generation",
                    checked = devModeEnabled,
                    onCheckedChange = { devModeEnabled = it }
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
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
// DOCKED BOTTOM NAVIGATION BAR
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
        tonalElevation = 0.dp,
        modifier = modifier
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
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = CircleShape,
        color = containerColor,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            if (isSelected) {
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// HTML VIEWER COMPOSABLE
// ---------------------------------------------------------------------------
@Composable
fun HtmlViewer(
    fileUri: Uri,
    isDomEnabled: Boolean,
    isHwAccelEnabled: Boolean,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = isDomEnabled
                    allowFileAccess = true
                    allowContentAccess = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_NO_CACHE
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
        modifier = modifier
    )
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

fun decompressZipToDir(context: Context, zipUri: Uri, targetDir: File): File? {
    return try {
        if (!targetDir.exists()) targetDir.mkdirs()
        val inputStream = context.contentResolver.openInputStream(zipUri) ?: return null
        val zis = ZipInputStream(BufferedInputStream(inputStream))
        var entry = zis.nextEntry
        val buffer = ByteArray(2048)

        while (entry != null) {
            val file = File(targetDir, entry.name)
            if (entry.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile?.mkdirs()
                val fos = FileOutputStream(file)
                var count: Int
                while (zis.read(buffer, 0, 2048).also { count = it } != -1) {
                    fos.write(buffer, 0, count)
                }
                fos.close()
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
        zis.close()
        findHtmlFile(targetDir)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun findHtmlFile(dir: File): File? {
    val files = dir.listFiles() ?: return null
    var fallbackHtml: File? = null
    for (file in files) {
        if (file.isDirectory) {
            val found = findHtmlFile(file)
            if (found != null) {
                if (found.name.equals("index.html", ignoreCase = true)) return found
                if (fallbackHtml == null) fallbackHtml = found
            }
        } else if (file.name.endsWith(".html", ignoreCase = true) || file.name.endsWith(".htm", ignoreCase = true)) {
            if (file.name.equals("index.html", ignoreCase = true)) return file
            if (fallbackHtml == null) fallbackHtml = file
        }
    }
    return fallbackHtml
}

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