package com.absolutemango.htmlgow

import android.content.Context
import android.os.Bundle
import android.net.Uri
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.SwipeToDismissValue
import androidx.wear.compose.material.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.app.RemoteInput
import android.content.Intent
import androidx.wear.input.RemoteInputIntentHelper
import androidx.compose.foundation.background as composeBackground
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.background
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.foundation.sizeIn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.SwitchButtonDefaults
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.timeTextCurvedText
import androidx.wear.compose.material3.timeTextSeparator
import androidx.wear.compose.material3.ButtonDefaults as M3ButtonDefaults
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import kotlinx.coroutines.delay

import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.collectAsState

private const val PREFS_NAME = "htmlgow_prefs"
private const val KEY_RECENTS = "recent_files"
private const val KEY_HARDWARE_ACCEL = "hardware_acceleration"
private const val KEY_DOM_STORAGE = "dom_storage"
private const val KEY_DEV_OPTION = "developer_option"
private const val KEY_FTP_ANONYMOUS = "ftp_anonymous"

private val CoalBackground = Color(0xFF212121)
private val LightBorderColor = Color(0xFF48484A)

private fun loadRecentFiles(prefs: android.content.SharedPreferences): List<String> {
    val jsonString = prefs.getString(KEY_RECENTS, null) ?: return emptyList()
    val list = mutableListOf<String>()
    try {
        val array = org.json.JSONArray(jsonString)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

private fun saveRecentFiles(prefs: android.content.SharedPreferences, files: List<String>) {
    val array = org.json.JSONArray()
    for (file in files) {
        array.put(file)
    }
    prefs.edit().putString(KEY_RECENTS, array.toString()).apply()
}

private fun createSampleZipFile(targetZipFile: java.io.File) {
    if (targetZipFile.exists()) return

    val htmlContent = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <title>Mango Offline ZIP</title>
            <link rel="stylesheet" href="style.css">
        </head>
        <body>
            <div class="card">
                <div class="title">Mango Offline ZIP</div>
                <div class="clock" id="clock">00:00:00</div>
                <img src="mango.svg" class="mango-img" alt="Mango SVG">
                <div class="counter" id="countVal">0</div>
                <button class="btn" onclick="addCount()">+ Tap Mango</button>
            </div>
            <script src="app.js"></script>
        </body>
        </html>
    """.trimIndent()

    val cssContent = """
        * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
        body {
            background-color: #000000;
            color: #ffffff;
            font-family: -apple-system, Roboto, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            width: 100vw;
            height: 100vh;
            overflow: hidden;
        }
        .card {
            width: 92vw;
            height: 92vw;
            max-width: 320px;
            max-height: 320px;
            background: radial-gradient(circle, #1c1c1e 0%, #000000 100%);
            border-radius: 50%;
            border: 2px solid #81D4FA;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 12px;
            text-align: center;
        }
        .title { font-size: 11px; font-weight: 800; color: #81D4FA; text-transform: uppercase; margin-bottom: 2px; }
        .clock { font-size: 10px; color: #8e8e93; margin-bottom: 4px; }
        .mango-img { width: 56px; height: 56px; margin: 2px 0; }
        .counter { font-size: 22px; font-weight: 800; color: #FFD54F; margin: 2px 0; }
        .btn { background: #2c2c2e; border: 1px solid #81D4FA; border-radius: 20px; color: #fff; padding: 4px 12px; font-size: 11px; font-weight: bold; cursor: pointer; }
        .btn:active { background: #0288D1; }
    """.trimIndent()

    val jsContent = """
        let count = 0;
        function addCount() {
            count++;
            document.getElementById('countVal').innerText = count;
        }
        function updateClock() {
            const now = new Date();
            document.getElementById('clock').innerText = now.toLocaleTimeString();
        }
        setInterval(updateClock, 1000);
        updateClock();
    """.trimIndent()

    val svgContent = """
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="80" height="80">
          <defs>
            <linearGradient id="mangoGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#FFEB3B"/>
              <stop offset="60%" stop-color="#FF9800"/>
              <stop offset="100%" stop-color="#F44336"/>
            </linearGradient>
            <linearGradient id="leafGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#81C784"/>
              <stop offset="100%" stop-color="#2E7D32"/>
            </linearGradient>
          </defs>
          <path d="M50 20 C75 20, 85 45, 75 75 C68 92, 40 92, 30 75 C20 55, 30 20, 50 20 Z" fill="url(#mangoGrad)" stroke="#E65100" stroke-width="2"/>
          <path d="M50 20 C48 12, 45 8, 42 5" fill="none" stroke="#5D4037" stroke-width="3" stroke-linecap="round"/>
          <path d="M48 12 C35 5, 25 15, 35 22 C42 22, 47 18, 48 12 Z" fill="url(#leafGrad)" stroke="#1B5E20" stroke-width="1"/>
        </svg>
    """.trimIndent()

    try {
        java.util.zip.ZipOutputStream(targetZipFile.outputStream()).use { zos ->
            fun addEntry(name: String, content: String) {
                zos.putNextEntry(java.util.zip.ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            addEntry("index.html", htmlContent)
            addEntry("style.css", cssContent)
            addEntry("app.js", jsContent)
            addEntry("mango.svg", svgContent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun ensureSampleFileExists(context: Context) {
    val amDir = java.io.File(context.filesDir, "am").apply {
        if (!exists()) mkdirs()
    }
    java.io.File(context.filesDir, "shared").apply {
        if (!exists()) mkdirs()
    }
    java.io.File(context.filesDir, "archives").apply {
        if (!exists()) mkdirs()
    }
    
    val sampleFiles = listOf("sample_watch.html", "sample_storage_hw.html", "oogway_wisdom.html")
    val createdFiles = mutableListOf<java.io.File>()

    for (fileName in sampleFiles) {
        val targetFile = java.io.File(amDir, fileName)
        createdFiles.add(targetFile)
        if (!targetFile.exists()) {
            try {
                context.assets.open(fileName).use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val sampleZip = java.io.File(amDir, "sample_app.zip")
    createSampleZipFile(sampleZip)
    createdFiles.add(0, sampleZip)

    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val recents = loadRecentFiles(prefs)
    if (recents.isEmpty()) {
        saveRecentFiles(prefs, createdFiles.map { it.absolutePath })
    }
}

private fun resolvePlayableFilePath(context: Context, rawPath: String): String {
    val file = java.io.File(rawPath)
    if (file.exists() && file.isFile && file.extension.lowercase(Locale.US) == "zip") {
        val archivesDir = java.io.File(context.filesDir, "archives").apply {
            if (!exists()) mkdirs()
        }
        val targetDir = java.io.File(archivesDir, file.nameWithoutExtension)
        var entryPoint = ZipExtractor.findHtmlEntryPoint(targetDir)
        if (entryPoint == null) {
            entryPoint = ZipExtractor.unzipAndFindEntryPoint(file, targetDir)
        }
        if (entryPoint != null) {
            return entryPoint.absolutePath
        }
    }
    return rawPath
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HtmlGoWearApp()
        }
    }
}

@Composable
fun HtmlGoWearApp(isScrolledToBottom: Boolean = false) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ensureSampleFileExists(context)
    }

    var activeViewerFile by remember { mutableStateOf<String?>(null) }
    var showingFileManager by remember { mutableStateOf(false) }

    MaterialTheme {
        AppScaffold {
            when {
                activeViewerFile != null -> {
                    val resolvedPath = remember(activeViewerFile) {
                        resolvePlayableFilePath(context, activeViewerFile!!)
                    }
                    GeckoWebViewScreen(
                        filePath = resolvedPath,
                        onBack = { activeViewerFile = null }
                    )
                }
                showingFileManager -> {
                    FileManagerScreen(
                        onBack = { showingFileManager = false },
                        onFileSelected = { file ->
                            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val current = loadRecentFiles(prefs).toMutableList()
                            current.remove(file.absolutePath)
                            current.add(0, file.absolutePath)
                            saveRecentFiles(prefs, current)
                            activeViewerFile = file.absolutePath
                            showingFileManager = false
                        }
                    )
                }
                else -> {
                    val pagerState = rememberPagerState(pageCount = { 2 })
                    var recentsVersion by remember { mutableStateOf(0) }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> RecentsScreen(
                                isScrolledToBottom = isScrolledToBottom,
                                recentsVersion = recentsVersion,
                                onOpenFileManager = { showingFileManager = true },
                                onOpenFile = { filePath -> activeViewerFile = filePath }
                            )
                            1 -> MenuScreen(
                                onRecentsChanged = { recentsVersion++ },
                                onOpenFile = { filePath -> activeViewerFile = filePath }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentsScreen(
    isScrolledToBottom: Boolean = false,
    recentsVersion: Int = 0,
    onOpenFileManager: () -> Unit = {},
    onOpenFile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    
    val recentFiles = remember(recentsVersion) {
        mutableStateListOf<String>().apply {
            addAll(loadRecentFiles(prefs))
        }
    }
    
    var fileToDelete by remember { mutableStateOf<String?>(null) }
    
    val listState = rememberScalingLazyListState(
        initialCenterItemIndex = 0,
        initialCenterItemScrollOffset = 0
    )
    var isScrollIndicatorActive by remember { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isScrollIndicatorActive = true
        } else {
            delay(2500L)
            isScrollIndicatorActive = false
        }
    }
    
    // Force scroll to the bottom for the preview
    LaunchedEffect(isScrolledToBottom) {
        if (isScrolledToBottom) {
            listState.scrollToItem(recentFiles.size.coerceAtLeast(1))
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        ScreenScaffold(
            scrollState = listState,
            timeText = {
                TimeText(
                    maxSweepAngle = 100f
                ) { time ->
                    timeTextCurvedText(
                        time = "HTMLGO-W",
                        style = CurvedTextStyle(
                            fontWeight = FontWeight.Black
                        )
                    )
                    timeTextSeparator()
                    timeTextCurvedText(
                        time = time,
                        style = CurvedTextStyle(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            edgeButtonSpacing = 8.dp,
            edgeButton = {
                EdgeButton(
                    onClick = onOpenFileManager,
                    border = BorderStroke(1.dp, LightBorderColor),
                    colors = M3ButtonDefaults.buttonColors(
                        containerColor = CoalBackground,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            }
        ) { contentPadding ->
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding
            ) {
                item {
                    ListHeader(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(
                            text = "Last Opened",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                if (recentFiles.isEmpty()) {
                    item {
                        Text(
                            text = "Sorry, nothing has been played yet.\nStart by picking a file.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 5.dp, vertical = 12.dp)
                        )
                    }
                } else {
                    items(recentFiles.size) { index ->
                        val filePath = recentFiles[index]
                        val displayName = java.io.File(filePath).name
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .border(1.dp, LightBorderColor, RoundedCornerShape(26.dp))
                                .clip(RoundedCornerShape(26.dp))
                                .composeBackground(CoalBackground)
                                .combinedClickable(
                                    onClick = { onOpenFile(filePath) },
                                    onLongClick = {
                                        fileToDelete = filePath
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        
        // Curved pill handle on the right edge - hidden during scroll & retention delay to avoid scrollbar overlap!
        if (!isScrollIndicatorActive) {
            CurvedLayout(
                anchor = 0f, // 0 degrees is 3 o'clock (right edge)
                modifier = Modifier.fillMaxSize()
            ) {
                curvedRow(
                    modifier = CurvedModifier
                        .sizeIn(minSweepDegrees = 14f, minThickness = 4.dp)
                        .background(Color.Gray, cap = StrokeCap.Round)
                ) {
                    // Curved handle
                }
            }
        }

        // Long-Press Delete / Upload Options Overlay
        if (fileToDelete != null) {
            val file = java.io.File(fileToDelete!!)
            BackHandler(enabled = true) {
                fileToDelete = null
            }
            val dismissState = rememberSwipeToDismissBoxState()
            LaunchedEffect(dismissState.currentValue) {
                if (dismissState.currentValue == SwipeToDismissValue.Dismissed) {
                    fileToDelete = null
                    dismissState.snapTo(SwipeToDismissValue.Default)
                }
            }

            SwipeToDismissBox(
                state = dismissState,
                modifier = Modifier.fillMaxSize()
            ) { isBackground ->
                if (!isBackground) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .composeBackground(Color.Black.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = file.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Delete button
                                Button(
                                    onClick = {
                                        recentFiles.remove(fileToDelete)
                                        saveRecentFiles(prefs, recentFiles)
                                        fileToDelete = null
                                    },
                                    modifier = Modifier.size(46.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFFF44336),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // 2. Upload / Send to Phone button
                                Button(
                                    onClick = {
                                        val targetFile = file
                                        fileToDelete = null
                                        WearTransferManager.sendFileToPhone(context, targetFile)
                                    },
                                    modifier = Modifier.size(46.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF0288D1),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Upload,
                                        contentDescription = "Send to Phone",
                                        modifier = Modifier.size(22.dp)
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

@Composable
fun MenuScreen(
    onRecentsChanged: () -> Unit = {},
    onOpenFile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showingSettings by remember { mutableStateOf(false) }
    var showingAiPrompt by remember { mutableStateOf(false) }
    var showingPhoneConnection by remember { mutableStateOf(false) }
    var showingFileManagerPicker by remember { mutableStateOf(false) }

    if (showingFileManagerPicker) {
        FileManagerScreen(
            onBack = { showingFileManagerPicker = false },
            onFileSelected = { /* No-op in picker mode */ },
            isPickerMode = true,
            onFilePickedForTransfer = { file ->
                WearTransferManager.sendFileToPhone(context, file)
                showingFileManagerPicker = false
            }
        )
    } else if (showingAiPrompt) {
        AiPromptScreen(
            onBack = { showingAiPrompt = false },
            onOpenFile = { path ->
                showingAiPrompt = false
                onOpenFile(path)
            }
        )
    } else if (showingPhoneConnection) {
        PhoneConnectionScreen(
            onBackClick = { showingPhoneConnection = false },
            onSendFileClick = { showingFileManagerPicker = true }
        )
    } else if (showingSettings) {
        SettingsScreen(
            onBack = { showingSettings = false },
            onRecentsChanged = onRecentsChanged
        )
    } else {
        val listState = rememberScalingLazyListState(
            initialCenterItemIndex = 0,
            initialCenterItemScrollOffset = 0
        )
        
        Box(modifier = Modifier.fillMaxSize()) {
            ScreenScaffold(
                scrollState = listState,
                timeText = {
                    TimeText { time ->
                        timeTextCurvedText(
                            time = time,
                            style = CurvedTextStyle(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            ) { contentPadding ->
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = contentPadding
                ) {
                    item {
                        ListHeader(modifier = Modifier.padding(bottom = 8.dp)) {
                            Text(
                                text = "Menu",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    item {
                        Button(
                            onClick = { showingPhoneConnection = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, LightBorderColor, CircleShape),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = CoalBackground,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Phone Connection")
                        }
                    }
                    item {
                        Button(
                            onClick = { showingAiPrompt = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, LightBorderColor, CircleShape),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = CoalBackground,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Use AI (Phone API)")
                        }
                    }
                    item {
                        Button(
                            onClick = { showingSettings = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, LightBorderColor, CircleShape),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = CoalBackground,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Settings")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRecentsChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var hardwareAccel by remember {
        mutableStateOf(prefs.getBoolean(KEY_HARDWARE_ACCEL, true))
    }
    var domStorage by remember {
        mutableStateOf(prefs.getBoolean(KEY_DOM_STORAGE, true))
    }
    var devOption by remember {
        mutableStateOf(prefs.getBoolean(KEY_DEV_OPTION, false))
    }
    var ftpAnonymous by remember {
        mutableStateOf(prefs.getBoolean(KEY_FTP_ANONYMOUS, true))
    }

    val listState = rememberScalingLazyListState(
        initialCenterItemIndex = 0,
        initialCenterItemScrollOffset = 0
    )
    val customSwitchColors = SwitchButtonDefaults.switchButtonColors(
        checkedContainerColor = CoalBackground,
        checkedContentColor = Color.White,
        checkedThumbColor = Color(0xFF81D4FA), // Sky blue thumb when ON
        checkedTrackColor = Color(0xFF0288D1), // Sky blue track when ON
        checkedTrackBorderColor = LightBorderColor, // Lighter gray outline
        uncheckedContainerColor = CoalBackground,
        uncheckedContentColor = Color.White,
        uncheckedThumbColor = Color.Gray, // Default gray thumb when OFF
        uncheckedTrackColor = CoalBackground, // Default dark track when OFF
        uncheckedTrackBorderColor = LightBorderColor // Lighter gray outline
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ScreenScaffold(
            scrollState = listState,
            timeText = {
                TimeText { time ->
                    timeTextCurvedText(
                        time = time,
                        style = CurvedTextStyle(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        ) { contentPadding ->
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding
            ) {
                item {
                    ListHeader(modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(28.dp)
                                    .border(1.dp, LightBorderColor, CircleShape),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = CoalBackground,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Back"
                                )
                            }
                            Text(
                                text = "Settings",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Option 1: Hardware acceleration
                item {
                    SwitchButton(
                        checked = hardwareAccel,
                        onCheckedChange = {
                            hardwareAccel = it
                            prefs.edit().putBoolean(KEY_HARDWARE_ACCEL, it).apply()
                        },
                        label = { Text("Hardware acceleration", fontSize = 12.sp) },
                        colors = customSwitchColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LightBorderColor, CircleShape)
                    )
                }

                // Option 2: DOM storage
                item {
                    SwitchButton(
                        checked = domStorage,
                        onCheckedChange = {
                            domStorage = it
                            prefs.edit().putBoolean(KEY_DOM_STORAGE, it).apply()
                        },
                        label = { Text("DOM storage", fontSize = 12.sp) },
                        colors = customSwitchColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LightBorderColor, CircleShape)
                    )
                }

                // Option 3: Developer option
                item {
                    SwitchButton(
                        checked = devOption,
                        onCheckedChange = {
                            devOption = it
                            prefs.edit().putBoolean(KEY_DEV_OPTION, it).apply()
                        },
                        label = { Text("Developer option", fontSize = 12.sp) },
                        colors = customSwitchColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LightBorderColor, CircleShape)
                    )
                }

                // Option 4: FTP Anonymous Mode
                item {
                    SwitchButton(
                        checked = ftpAnonymous,
                        onCheckedChange = {
                            ftpAnonymous = it
                            prefs.edit().putBoolean(KEY_FTP_ANONYMOUS, it).apply()
                        },
                        label = { Text("FTP Anonymous Mode", fontSize = 12.sp) },
                        secondaryLabel = {
                            Text(
                                if (ftpAnonymous) "Anonymous Access Allowed" else "Login: htmlgo / 1234",
                                fontSize = 10.sp,
                                color = if (ftpAnonymous) Color.LightGray else Color(0xFFFFD54F)
                            )
                        },
                        colors = customSwitchColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LightBorderColor, CircleShape)
                    )
                }

                // Conditional Developer Section
                if (devOption) {
                    item {
                        ListHeader(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                            Text(
                                text = "Developer Actions",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                val current = loadRecentFiles(prefs).toMutableList()
                                for (i in 1..25) {
                                    current.add("Dummy File ${current.size + 1}.html")
                                }
                                saveRecentFiles(prefs, current)
                                onRecentsChanged()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, LightBorderColor, CircleShape),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = CoalBackground,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Add 25 dummy files", fontSize = 11.sp)
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                saveRecentFiles(prefs, emptyList())
                                onRecentsChanged()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, LightBorderColor, CircleShape),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Red,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Remove everything in recents", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@WearPreviewDevices
@Composable
fun TopPreview() {
    HtmlGoWearApp(isScrolledToBottom = false)
}

@WearPreviewDevices
@Composable
fun BottomPreview() {
    HtmlGoWearApp(isScrolledToBottom = true)
}