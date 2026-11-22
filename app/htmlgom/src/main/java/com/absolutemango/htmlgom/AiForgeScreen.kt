package com.absolutemango.htmlgom

import android.content.Context
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import com.absolutemango.htmlgom.commonMain.components.LiquidContainer
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import ru.noties.jlatexmath.JLatexMathDrawable

data class ForgeMessage(
    val role: String, // "user" or "model"
    val content: String,
)

@Deprecated(
    message = "Use ExpressiveAiForgeScreen from :expressive-ui instead",
    replaceWith = ReplaceWith(
        "ExpressiveAiForgeScreen(onBack = onBackClick, onRunGeneratedHtml = onRunGeneratedHtml, initialContext = initialContext)",
        "com.absolutemango.expressiveui.ExpressiveAiForgeScreen"
    )
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiForgeScreen(
    onBackClick: () -> Unit,
    onRunGeneratedHtml: (String) -> Unit,
    initialContext: String? = null,
    backdrop: Backdrop
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val prefs = remember { context.getSharedPreferences("ai_forge_prefs", Context.MODE_PRIVATE) }

    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var backupApiKey by remember { mutableStateOf(prefs.getString("backup_api_key", "") ?: "") }
    var selectedModel by remember { mutableStateOf(prefs.getString("model", "gemini-3.6-flash") ?: "gemini-3.6-flash") }
    var prompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(value = false) }
    var usingBackupKey by remember { mutableStateOf(false) }
    var configExpanded by remember { mutableStateOf(true) }
    var backupExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(initialContext) {
        if (!initialContext.isNullOrBlank()) {
            prompt = "Please modify this HTML code:\n\n```html\n$initialContext\n```\n\nInstructions: "
            configExpanded = false
        }
    }
    
    val recentPrompts = remember { 
        mutableStateListOf<String>().apply { 
            addAll(loadRecentPrompts(context))
        } 
    }
    var showRecentMenu by remember { mutableStateOf(false) }

    val messages = remember { 
        mutableStateListOf<ForgeMessage>().apply { 
            addAll(loadForgeHistory(context))
        }
    }
    var chatSession by remember { mutableStateOf<Chat?>(null) }

    val models = listOf(
        "gemini-3.6-flash" to "Flash 3.6",
        "gemini-3.5-flash-lite" to "Lite 3.5",
        "gemini-3.1-pro" to "Pro 3.1",
        "gemini-2.5-flash" to "Flash 2.5"
    )

    // Entrance animation states
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    // Detect the latest HTML in history for the top play button
    val latestHtml = remember(messages.size) {
        messages.asSequence()
            .filter { it.role == "model" }
            .mapNotNull { detectHtml(it.content) }
            .lastOrNull()
    }
    
    // Play button flash effect
    var flashCount by remember { mutableIntStateOf(0) }
    val isFlashing = (flashCount % 2) == 1
    
    val flashAlpha by animateFloatAsState(
        targetValue = if (isFlashing) 0.5f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "play_button_alpha"
    )
    
    val flashScale by animateFloatAsState(
        targetValue = if (isFlashing) 1.5f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "play_button_scale"
    )
    
    val flashColor by animateColorAsState(
        targetValue = if (isFlashing) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
        animationSpec = tween(300),
        label = "play_button_color"
    )
    
    val flashContainerColor by animateColorAsState(
        targetValue = if (isFlashing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        animationSpec = tween(300),
        label = "play_button_container"
    )

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty() || isGenerating) {
            listState.animateScrollToItem(if (isGenerating) messages.size else messages.size - 1)
        }
    }

    LaunchedEffect(latestHtml) {
        if (latestHtml != null) {
            repeat(10) {
                flashCount++
                delay(400L)
            }
            flashCount = 0
        }
    }

    fun initChat(forcedKey: String? = null) {
        val activeKey = forcedKey ?: (if (usingBackupKey) backupApiKey else apiKey)
        if (activeKey.isNotBlank()) {
            val model = GenerativeModel(modelName = selectedModel, apiKey = activeKey)
            val history = messages.map { content(role = it.role) { text(it.content) } }
            chatSession = model.startChat(history = history)
        }
    }

    LaunchedEffect(apiKey, selectedModel) {
        if (chatSession == null) initChat()
    }

    fun handleForge() {
        if (apiKey.isBlank() || prompt.isBlank() || isGenerating) return
        
        keyboardController?.hide()
        val userPrompt = prompt.trim()
        
        // Save to recent prompts
        if (recentPrompts.contains(userPrompt)) {
            recentPrompts.remove(userPrompt)
        }
        recentPrompts.add(0, userPrompt)
        if (recentPrompts.size > 15) recentPrompts.removeAt(recentPrompts.lastIndex)
        saveRecentPrompts(context, recentPrompts)
        
        prompt = ""
        messages.add(ForgeMessage("user", userPrompt))
        isGenerating = true
        
        scope.launch {
            try {
                val activeKey = if (usingBackupKey) backupApiKey else apiKey
                if (chatSession == null) {
                    val model = GenerativeModel(modelName = selectedModel, apiKey = activeKey)
                    chatSession = model.startChat()
                }
                
                var response = chatSession?.sendMessage(userPrompt)
                var responseText = response?.text ?: ""

                // Detect quota error in text or throw if empty
                if (responseText.isBlank()) throw Exception("Empty response from AI")
                
                if (detectQuotaError(responseText) && backupApiKey.isNotBlank() && !usingBackupKey) {
                    throw Exception("quota_exhausted_trigger_backup")
                }

                messages.add(ForgeMessage("model", responseText))
                saveForgeHistory(context, messages)
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: ""
                if ((errorMsg.contains("quota", true) || errorMsg.contains("429") || errorMsg == "quota_exhausted_trigger_backup") 
                    && backupApiKey.isNotBlank() && !usingBackupKey) {
                    
                    usingBackupKey = true
                    messages.add(ForgeMessage("model", "System: Primary key exhausted. Switching to Backup Forge..."))
                    
                    try {
                        val backupModel = GenerativeModel(modelName = selectedModel, apiKey = backupApiKey)
                        val history = messages.dropLast(1).map { content(role = it.role) { text(it.content) } }
                        val backupSession = backupModel.startChat(history = history)
                        chatSession = backupSession
                        
                        val retryResponse = backupSession.sendMessage(userPrompt)
                        messages.add(ForgeMessage("model", retryResponse.text ?: "Backup forge failed to output."))
                    } catch (e2: Exception) {
                        messages.add(ForgeMessage("model", "Forge Error (Critical): ${e2.localizedMessage}"))
                    }
                } else {
                    messages.add(ForgeMessage("model", "Forge Error: ${e.localizedMessage}"))
                }
                saveForgeHistory(context, messages)
            } finally {
                isGenerating = false
            }
        }
    }

    val bgColor = MaterialTheme.colorScheme.surface
    val accentColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(bgColor, bgColor, accentColor)))
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { 
                            Text(
                                "AI Forge", 
                                fontWeight = FontWeight.Black, 
                                letterSpacing = (-0.5).sp,
                                style = MaterialTheme.typography.headlineSmall
                            ) 
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        },
                        actions = {
                            if (latestHtml != null) {
                                Surface(
                                    onClick = { onRunGeneratedHtml(latestHtml) },
                                    shape = CircleShape,
                                    color = flashContainerColor,
                                    shadowElevation = 8.dp,
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .size(48.dp)
                                        .graphicsLayer { 
                                            alpha = flashAlpha
                                            scaleX = flashScale
                                            scaleY = flashScale
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.PlayArrow, 
                                            "Run Latest", 
                                            tint = flashColor,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { 
                                    messages.clear()
                                    chatSession = null
                                    saveForgeHistory(context, emptyList())
                                    initChat()
                                }
                            ) {
                                Icon(Icons.Default.DeleteSweep, "Clear Context", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // 1. API Configuration - Glass Card
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 40 }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { configExpanded = !configExpanded }
                            ) {
                                Text(
                                    text = if (usingBackupKey) "CONFIGURATION (BACKUP ACTIVE)" else "CONFIGURATION", 
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Black, 
                                    color = if (usingBackupKey) MaterialTheme.colorScheme.error.copy(0.7f) else MaterialTheme.colorScheme.primary.copy(0.6f), 
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (configExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { 
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, "https://aistudio.google.com/app/apikey".toUri())
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Info, "Get API Key", tint = MaterialTheme.colorScheme.primary.copy(0.5f), modifier = Modifier.size(16.dp))
                            }
                        }

                        // Primary Key
                        AnimatedVisibility(visible = configExpanded) {
                            LiquidContainer(
                                backdrop = backdrop,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(2.dp)) {
                                    OutlinedTextField(
                                        value = apiKey,
                                        onValueChange = { 
                                            apiKey = it
                                            prefs.edit { putString("api_key", it) }
                                            if (!usingBackupKey) chatSession = null
                                        },
                                        placeholder = { Text("Primary Gemini API Key", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), fontSize = 14.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedBorderColor = Color.Transparent,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                                        ),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                }
                            }
                        }

                        // Backup Key
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp).clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { backupExpanded = !backupExpanded }
                        ) {
                            Text("BACKUP CONFIGURATION", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary.copy(0.4f), letterSpacing = 1.2.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = if (backupExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(0.3f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        
                        AnimatedVisibility(visible = backupExpanded) {
                            LiquidContainer(
                                backdrop = backdrop,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(2.dp)) {
                                    OutlinedTextField(
                                        value = backupApiKey,
                                        onValueChange = { 
                                            backupApiKey = it
                                            prefs.edit { putString("backup_api_key", it) }
                                            if (usingBackupKey) chatSession = null
                                        },
                                        placeholder = { Text("Backup Gemini API Key (Optional)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), fontSize = 14.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedBorderColor = Color.Transparent,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                                        ),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Model Selection - Staggered
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600, 150)) + slideInVertically(tween(600, 150)) { 40 }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ENGINE MODEL", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary.copy(0.6f), letterSpacing = 1.2.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            models.forEach { (id, label) ->
                                val isSelected = selectedModel == id
                                ScaleInteraction {
                                    Surface(
                                        onClick = { 
                                            selectedModel = id
                                            prefs.edit { putString("model", id) }
                                            chatSession = null 
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(0.2f))
                                    ) {
                                        Text(
                                            text = label, 
                                            fontSize = 13.sp, 
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Prompt Forge
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600, 300)) + slideInVertically(tween(600, 300)) { 40 }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("FORGE INPUT", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary.copy(0.6f), letterSpacing = 1.2.sp)
                            
                            Box {
                                ScaleInteraction {
                                    Surface(
                                        onClick = { showRecentMenu = true },
                                        enabled = recentPrompts.isNotEmpty(),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.History, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary.copy(0.7f))
                                            Spacer(Modifier.width(6.dp))
                                            Text("RECENTS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary.copy(0.7f))
                                        }
                                    }
                                }
                                
                                DropdownMenu(
                                    expanded = showRecentMenu,
                                    onDismissRequest = { showRecentMenu = false },
                                    modifier = Modifier.widthIn(max = 300.dp).background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.2f), RoundedCornerShape(12.dp))
                                ) {
                                    recentPrompts.forEach { recent ->
                                        DropdownMenuItem(
                                            text = { 
                                                Text(recent, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) 
                                            },
                                            onClick = {
                                                prompt = recent
                                                showRecentMenu = false
                                            }
                                        )
                                    }
                                    if (recentPrompts.isNotEmpty()) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.2f))
                                        DropdownMenuItem(
                                            text = { Text("Clear All", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                recentPrompts.clear()
                                                saveRecentPrompts(context, emptyList())
                                                showRecentMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        LiquidContainer(
                            backdrop = backdrop,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(20.dp).heightIn(min = 120.dp)) {
                                if (prompt.isEmpty()) {
                                    Text("Describe your HTML5 vision...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), fontSize = 16.sp)
                                }
                                BasicTextField(
                                    value = prompt,
                                    onValueChange = { prompt = it },
                                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, lineHeight = 26.sp),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        ScaleInteraction {
                            Button(
                                onClick = { handleForge() },
                                enabled = prompt.isNotBlank() && apiKey.isNotBlank() && !isGenerating,
                                modifier = Modifier.fillMaxWidth().height(60.dp).shadow(12.dp, RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(22.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text("IGNITE PROJECT", fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.1f), thickness = 1.dp)

                // 4. Context History
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600, 450))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("REFINERY LOG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f), letterSpacing = 1.sp)
                        
                        val logContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f)
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(logContainerColor, RoundedCornerShape(28.dp))
                                .border(1.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.12f), RoundedCornerShape(28.dp))
                                .clip(RoundedCornerShape(28.dp))
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 60.dp)
                            ) {
                                items(messages) { msg ->
                                    ForgeBubble(msg, onRunGeneratedHtml)
                                }
                                if (isGenerating) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary.copy(0.5f))
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
}
}
}

@Composable
fun ScaleInteraction(content: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "scale_interaction"
    )
    
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .indication(interactionSource, null),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = {})) {
            content()
        }
    }
}

@Composable
fun ForgeBubble(
    message: ForgeMessage,
    onRunHtml: (String) -> Unit
) {
    val isUser = message.role == "user"
    val htmlCode = remember(message.content) { detectHtml(message.content) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.35f),
            border = BorderStroke(1.dp, if (isUser) MaterialTheme.colorScheme.primary.copy(0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(0.15f)),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                if (isUser) {
                    Text(
                        text = message.content,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                } else {
                    RichForgeText(
                        text = message.content,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!htmlCode.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    ScaleInteraction {
                        Button(
                            onClick = { onRunHtml(htmlCode) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("IGNITE PROJECT", fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RichForgeText(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { parseRichForgeText(text) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is RichBlock.Text -> {
                    Text(
                        text = block.annotatedString,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.9f),
                        fontSize = 15.sp,
                        lineHeight = 24.sp
                    )
                }
                is RichBlock.Latex -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(0.1f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LatexRender(latex = block.formula)
                        }
                    }
                }
            }
        }
    }
}

sealed class RichBlock {
    data class Text(val annotatedString: androidx.compose.ui.text.AnnotatedString) : RichBlock()
    data class Latex(val formula: String) : RichBlock()
}

fun parseRichForgeText(text: String): List<RichBlock> {
    val blocks = mutableListOf<RichBlock>()
    val latexRegex = Regex("""\$\$(.*?)\$\$|\\\[(.*?)\\]|\\\((.*?)\\\)|\$(.*?)\$""", RegexOption.DOT_MATCHES_ALL)
    
    var lastIndex = 0
    latexRegex.findAll(text).forEach { match ->
        if (match.range.first > lastIndex) {
            val plainText = text.substring(lastIndex, match.range.first)
            blocks.add(RichBlock.Text(formatForgeMarkdown(plainText)))
        }
        val formula = match.groupValues[1].ifBlank { 
            match.groupValues[2].ifBlank { 
                match.groupValues[3].ifBlank { 
                    match.groupValues[4] 
                } 
            } 
        }
        blocks.add(RichBlock.Latex(formula.trim()))
        lastIndex = match.range.last + 1
    }
    
    if (lastIndex < text.length) {
        blocks.add(RichBlock.Text(formatForgeMarkdown(text.substring(lastIndex))))
    }
    
    return if (blocks.isEmpty()) listOf(RichBlock.Text(formatForgeMarkdown(text))) else blocks
}

fun formatForgeMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        append(text)
        
        val boldRegex = Regex("""\*\*(.*?)\*\*""")
        val italicRegex = Regex("""\*(.*?)\*""")
        val codeRegex = Regex("""`(.*?)`""")
        
        boldRegex.findAll(text).forEach { match ->
            addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.Unspecified), match.range.first, match.range.last + 1)
        }
        italicRegex.findAll(text).forEach { match ->
            addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
        }
        codeRegex.findAll(text).forEach { match ->
            addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color.White.copy(0.05f), color = Color(0xFFCE9178)), match.range.first, match.range.last + 1)
        }
    }
}

@Composable
fun LatexRender(latex: String) {
    val density = LocalDensity.current
    val color = MaterialTheme.colorScheme.primary.toArgb()
    
    val drawable = remember(latex) {
        runCatching {
            JLatexMathDrawable.builder(latex)
                .textSize(45f) 
                .color(color)
                .padding(8)
                .build()
        }.getOrNull()
    }
    
    if (drawable != null) {
        val rect = drawable.bounds
        Canvas(
            modifier = Modifier
                .height(with(density) { rect.height().toDp() })
                .width(with(density) { rect.width().toDp() })
        ) {
            drawIntoCanvas { canvas ->
                drawable.draw(canvas.nativeCanvas)
            }
        }
    } else {
        Text(text = latex, color = MaterialTheme.colorScheme.error.copy(0.7f), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
    }
}

fun detectQuotaError(text: String): Boolean {
    val low = text.lowercase()
    return low.contains("quota") || low.contains("limit exceeded") || low.contains("exhausted")
}

fun detectHtml(text: String): String? {
    val regex = "```(?:html)?\\s*([\\s\\S]*?)(?:```|$)".toRegex(RegexOption.IGNORE_CASE)
    val match = regex.find(text)
    return match?.groupValues?.get(1)?.trim() ?: if (text.contains("<!DOCTYPE html>", true) || text.contains("<html", true)) {
        val start = text.indexOf("<html", ignoreCase = true).coerceAtLeast(text.indexOf("<!DOCTYPE html>", ignoreCase = true))
        if (start != -1) text.substring(start).trim() else null
    } else null
}

fun saveForgeHistory(context: Context, messages: List<ForgeMessage>) {
    try {
        val array = JSONArray()
        messages.forEach {
            val obj = JSONObject()
            obj.put("role", it.role)
            obj.put("content", it.content)
            array.put(obj)
        }
        context.getSharedPreferences("ai_forge_prefs", Context.MODE_PRIVATE)
            .edit { putString("history", array.toString()) }
    } catch (e: Exception) { e.printStackTrace() }
}

fun loadForgeHistory(context: Context): List<ForgeMessage> {
    val list = mutableListOf<ForgeMessage>()
    try {
        val json = context.getSharedPreferences("ai_forge_prefs", Context.MODE_PRIVATE)
            .getString("history", null) ?: return list
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(ForgeMessage(obj.getString("role"), obj.getString("content")))
        }
    } catch (e: Exception) { e.printStackTrace() }
    return list
}

fun saveRecentPrompts(context: Context, prompts: List<String>) {
    try {
        val array = JSONArray()
        prompts.forEach { array.put(it) }
        context.getSharedPreferences("ai_forge_prefs", Context.MODE_PRIVATE)
            .edit { putString("recent_prompts", array.toString()) }
    } catch (e: Exception) { e.printStackTrace() }
}

fun loadRecentPrompts(context: Context): List<String> {
    val list = mutableListOf<String>()
    try {
        val json = context.getSharedPreferences("ai_forge_prefs", Context.MODE_PRIVATE)
            .getString("recent_prompts", null) ?: return list
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
    } catch (e: Exception) { e.printStackTrace() }
    return list
}
