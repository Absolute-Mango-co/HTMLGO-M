package com.absolutemango.htmlgom

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileEditorScreen(
    onBackClick: () -> Unit,
    onRunFile: (File) -> Unit,
    initialFileUri: Uri? = null,
    backdrop: Backdrop = rememberLayerBackdrop()
) {
    val context = LocalContext.current
    val rootWorkspaceDir = remember {
        File(context.filesDir, "workspace").apply { if (!exists()) mkdirs() }
    }

    var currentDir by remember { mutableStateOf(rootWorkspaceDir) }
    var activeFile by remember { mutableStateOf<File?>(null) }
    var fileContent by remember { mutableStateOf("") }
    var isContentModified by remember { mutableStateOf(false) }

    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileNameInput by remember { mutableStateOf("") }
    var newFileExtInput by remember { mutableStateOf("html") }

    LaunchedEffect(initialFileUri) {
        if (initialFileUri != null) {
            try {
                if (initialFileUri.scheme == "file") {
                    val file = File(initialFileUri.path!!)
                    if (file.exists() && file.isFile) {
                        currentDir = file.parentFile ?: rootWorkspaceDir
                        fileContent = file.readText()
                        activeFile = file
                        isContentModified = false
                    }
                } else {
                    // For content:// URIs, create a temp file so activeFile works, but we'll also save back to the URI.
                    val tempFile = File(context.cacheDir, "editor_temp_${System.currentTimeMillis()}.html")
                    context.contentResolver.openInputStream(initialFileUri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    fileContent = tempFile.readText()
                    activeFile = tempFile
                    isContentModified = false
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot read file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val canInternalBack = activeFile != null || (currentDir.canonicalPath != rootWorkspaceDir.canonicalPath && currentDir.parentFile != null)

    BackHandler(enabled = canInternalBack) {
        when {
            activeFile != null -> {
                activeFile = null
                fileContent = ""
                isContentModified = false
            }
            currentDir.canonicalPath != rootWorkspaceDir.canonicalPath && currentDir.parentFile != null -> {
                currentDir = currentDir.parentFile!!
            }
        }
    }

    val dirFiles = remember(currentDir, activeFile, showNewFileDialog) {
        val list = currentDir.listFiles()?.toList() ?: emptyList()
        list.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        if (activeFile != null) {
            // --- CODE EDITOR VIEW ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Editor Toolbar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = {
                            activeFile = null
                            fileContent = ""
                            isContentModified = false
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = activeFile?.name ?: "Editor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isContentModified) {
                                Text(
                                    text = "Unsaved Changes",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Cancel Button
                            TextButton(onClick = {
                                activeFile = null
                                fileContent = ""
                                isContentModified = false
                            }) {
                                Text("Cancel", color = MaterialTheme.colorScheme.error)
                            }
                            
                            // Save Button
                            Button(onClick = {
                                try {
                                    activeFile?.writeText(fileContent)
                                    if (initialFileUri != null && initialFileUri.scheme == "content") {
                                        context.contentResolver.openOutputStream(initialFileUri, "wt")?.use { out ->
                                            out.write(fileContent.toByteArray())
                                        }
                                    }
                                    isContentModified = false
                                    Toast.makeText(context, "Saved ${activeFile?.name ?: "File"}!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Save Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Sora Code Editor Engine
                SoraEditor(
                    text = fileContent,
                    onTextChange = {
                        fileContent = it
                        isContentModified = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // --- FILE EXPLORER VIEW ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Explorer Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Column {
                                Text(
                                    text = "Workspace Editor",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Path: /${currentDir.name}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { showNewFileDialog = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "New File",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Directory Breadcrumb / Up
                if (currentDir.canonicalPath != rootWorkspaceDir.canonicalPath && currentDir.parentFile != null) {
                    ListItem(
                        headlineContent = { Text(".. (Parent Directory)", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                        leadingContent = { Icon(Icons.Outlined.Folder, null, tint = Color(0xFFFFD54F)) },
                        modifier = Modifier.clickable { currentDir = currentDir.parentFile!! }
                    )
                    HorizontalDivider()
                }

                if (dirFiles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Directory is empty\nTap + to create a new file",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(dirFiles, key = { it.absolutePath }) { file ->
                            val isDir = file.isDirectory
                            ListItem(
                                headlineContent = { Text(file.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                                supportingContent = {
                                    Text(
                                        if (isDir) "Directory" else "${file.length() / 1024} KB",
                                        fontSize = 11.sp
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = if (isDir) Icons.Outlined.Folder else Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = if (isDir) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingContent = {
                                    Row {
                                        if (!isDir && (file.extension.lowercase() in listOf("html", "htm", "zip"))) {
                                            IconButton(onClick = { onRunFile(file) }) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = "Run",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        IconButton(onClick = {
                                            file.deleteRecursively()
                                            Toast.makeText(context, "Deleted ${file.name}", Toast.LENGTH_SHORT).show()
                                            currentDir = File(currentDir.absolutePath)
                                        }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.clickable {
                                    if (isDir) {
                                        currentDir = file
                                    } else {
                                        try {
                                            fileContent = file.readText()
                                            activeFile = file
                                            isContentModified = false
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot read file: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }

        // --- NEW FILE DIALOG ---
        if (showNewFileDialog) {
            AlertDialog(
                onDismissRequest = { showNewFileDialog = false },
                icon = { Icon(Icons.Default.NoteAdd, null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Create New File", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newFileNameInput,
                            onValueChange = { newFileNameInput = it },
                            label = { Text("File Name") },
                            placeholder = { Text("e.g. index") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("html", "css", "js", "json", "txt").forEach { ext ->
                                FilterChip(
                                    selected = newFileExtInput == ext,
                                    onClick = { newFileExtInput = ext },
                                    label = { Text(".$ext", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val name = newFileNameInput.trim()
                        if (name.isNotEmpty()) {
                            val fullName = if (name.endsWith(".$newFileExtInput")) name else "$name.$newFileExtInput"
                            val createdFile = File(currentDir, fullName)
                            try {
                                val initialCode = when (newFileExtInput) {
                                    "html" -> "<!DOCTYPE html>\n<html>\n<head>\n  <title>$name</title>\n</head>\n<body>\n  <h1>Hello World</h1>\n</body>\n</html>"
                                    "css" -> "/* $fullName stylesheet */\nbody { background: #121212; color: #ffffff; }"
                                    "js" -> "// $fullName logic\nconsole.log('App initialized');"
                                    else -> ""
                                }
                                createdFile.writeText(initialCode)
                                showNewFileDialog = false
                                newFileNameInput = ""
                                fileContent = initialCode
                                activeFile = createdFile
                                isContentModified = false
                            } catch (e: Exception) {
                                Toast.makeText(context, "Creation Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewFileDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
