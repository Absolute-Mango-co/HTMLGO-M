package com.absolutemango.htmlgow

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.timeTextCurvedText
import java.io.File
import androidx.compose.foundation.background as composeBackground

private val CoalBackground = Color(0xFF212121)
private val LightBorderColor = Color(0xFF48484A)
private val HtmlHighlightColor = Color(0xFF81D4FA)

@Composable
fun FileManagerScreen(
    onBack: () -> Unit,
    onFileSelected: (File) -> Unit,
    isPickerMode: Boolean = false,
    onFilePickedForTransfer: (File) -> Unit = {}
) {
    val context = LocalContext.current
    val baseAmDir = remember {
        File(context.filesDir, "am").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    val sharedDir = remember {
        File(context.filesDir, "shared").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    val externalStorageDir = remember {
        Environment.getExternalStorageDirectory() ?: context.filesDir
    }
    val downloadsDir = remember {
        File(externalStorageDir, "Download").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    var currentDir by remember { mutableStateOf(baseAmDir) }
    var isFtpCardExpanded by remember { mutableStateOf(false) }

    // Intercept universal back button in file manager
    BackHandler(enabled = true) {
        if (currentDir.parentFile != null) {
            currentDir = currentDir.parentFile!!
        } else {
            onBack()
        }
    }

    val filesAndDirs = remember(currentDir) {
        val list = currentDir.listFiles()?.toList() ?: emptyList()
        list.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    val listState = rememberScalingLazyListState(
        initialCenterItemIndex = 0,
        initialCenterItemScrollOffset = 0
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
                // Header with Back button and folder path
                item {
                    ListHeader(modifier = Modifier.padding(bottom = 6.dp)) {
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
                            val dirPrefix = if (currentDir == baseAmDir) "am/" else if (currentDir == sharedDir) "shared/" else if (currentDir == downloadsDir) "Download/" else "${currentDir.name}/"
                            Text(
                                text = if (isPickerMode) "Send: $dirPrefix" else dirPrefix,
                                color = if (isPickerMode) HtmlHighlightColor else Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Storage Shortcuts Grid: Row 1 (3 items: am, shared, downloads) & Row 2 (2 items: storage, FTP glowing)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Row 1: 3 Shortcuts
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            CompactButton(
                                onClick = { currentDir = baseAmDir },
                                modifier = Modifier.border(1.dp, if (currentDir == baseAmDir) HtmlHighlightColor else LightBorderColor, CircleShape),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = CoalBackground,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "am",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            CompactButton(
                                onClick = { currentDir = sharedDir },
                                modifier = Modifier.border(1.dp, if (currentDir == sharedDir) HtmlHighlightColor else LightBorderColor, CircleShape),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = CoalBackground,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderShared,
                                    contentDescription = "shared",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            CompactButton(
                                onClick = { currentDir = downloadsDir },
                                modifier = Modifier.border(1.dp, if (currentDir == downloadsDir) HtmlHighlightColor else LightBorderColor, CircleShape),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = CoalBackground,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Downloads",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Row 2: 2 Shortcuts (Storage & FTP with glowing outline)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            CompactButton(
                                onClick = { currentDir = externalStorageDir },
                                modifier = Modifier.border(1.dp, if (currentDir == externalStorageDir) HtmlHighlightColor else LightBorderColor, CircleShape),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = CoalBackground,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SdCard,
                                    contentDescription = "Storage",
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Featured FTP Button with Glowing Sky Blue Outline!
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .composeBackground(HtmlHighlightColor.copy(alpha = 0.25f))
                                    .padding(2.dp)
                            ) {
                                CompactButton(
                                    onClick = { isFtpCardExpanded = !isFtpCardExpanded },
                                    modifier = Modifier.border(1.5.dp, HtmlHighlightColor, CircleShape),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = if (isFtpCardExpanded || FtpServerManager.isRunning) Color(0xFF1E3A5F) else CoalBackground,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "FTP Server",
                                        tint = HtmlHighlightColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // FTP Server Control Card (Expands when FTP shortcut button is tapped or server is active)
                if (isFtpCardExpanded || FtpServerManager.isRunning) {
                    item {
                        var isFtpRunning by remember { mutableStateOf(FtpServerManager.isRunning) }
                        var ftpUrl by remember { mutableStateOf(if (isFtpRunning) FtpServerManager.getFtpUrl(context) else "") }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .border(1.5.dp, HtmlHighlightColor, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(CoalBackground)
                                .padding(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = "FTP",
                                            tint = if (isFtpRunning) HtmlHighlightColor else Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "FTP Share",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    CompactButton(
                                        onClick = {
                                            if (isFtpRunning) {
                                                FtpServerManager.stopServer()
                                                isFtpRunning = false
                                                ftpUrl = ""
                                            } else {
                                                if (FtpServerManager.startServer(context)) {
                                                    isFtpRunning = true
                                                    ftpUrl = FtpServerManager.getFtpUrl(context)
                                                }
                                            }
                                        },
                                        modifier = Modifier.border(1.dp, if (isFtpRunning) HtmlHighlightColor else LightBorderColor, CircleShape),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = if (isFtpRunning) Color(0xFF0288D1) else Color.DarkGray,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text(if (isFtpRunning) "ON" else "OFF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (isFtpRunning) {
                                    val prefs = remember { context.getSharedPreferences("htmlgow_prefs", android.content.Context.MODE_PRIVATE) }
                                    val ftpAnon = remember(isFtpRunning) { prefs.getBoolean("ftp_anonymous", true) }
                                    Text(
                                        text = ftpUrl,
                                        fontSize = 11.sp,
                                        color = Color(0xFFFFD54F),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = if (ftpAnon) "Mode: Anonymous" else "Auth: htmlgo / 1234",
                                        fontSize = 9.sp,
                                        color = if (ftpAnon) Color.LightGray else HtmlHighlightColor,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Text(
                                        text = "Transfer via AnExplorer into /shared",
                                        fontSize = 9.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Up directory option if parent exists
                if (currentDir.parentFile != null) {
                    item {
                        Button(
                            onClick = {
                                currentDir = currentDir.parentFile!!
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .border(1.dp, LightBorderColor, RoundedCornerShape(22.dp)),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = CoalBackground,
                                contentColor = Color.White
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    tint = Color.Yellow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(".. (Parent Folder)", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (filesAndDirs.isEmpty()) {
                    item {
                        Text(
                            text = "Directory is empty.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                } else {
                    items(filesAndDirs.size) { index ->
                        val file = filesAndDirs[index]
                        val isDirectory = file.isDirectory
                        val ext = file.extension.lowercase()
                        val isHtml = ext in listOf("html", "htm")
                        val isZip = ext == "zip"
                        val isPlayable = isHtml || isZip

                        Button(
                            onClick = {
                                if (isDirectory) {
                                    currentDir = file
                                } else {
                                    if (isPickerMode) {
                                        onFilePickedForTransfer(file)
                                    } else {
                                        onFileSelected(file)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .border(
                                    1.dp,
                                    if (isPickerMode) HtmlHighlightColor else if (isPlayable) HtmlHighlightColor else LightBorderColor,
                                    RoundedCornerShape(24.dp)
                                ),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = CoalBackground,
                                contentColor = Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDirectory) Icons.Default.Folder else if (isPickerMode) Icons.AutoMirrored.Filled.Send else if (isZip) Icons.Default.Archive else Icons.Default.Description,
                                    contentDescription = if (isDirectory) "Folder" else if (isPickerMode) "Send File" else if (isZip) "ZIP Archive" else "File",
                                    tint = if (isDirectory) Color.Yellow else if (isPickerMode) HtmlHighlightColor else if (isPlayable) HtmlHighlightColor else Color.LightGray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = file.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isPlayable || isPickerMode) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
