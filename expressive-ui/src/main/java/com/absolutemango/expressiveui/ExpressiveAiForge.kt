package com.absolutemango.expressiveui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AtomicFile
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import android.widget.EditText
import android.text.TextWatcher
import android.text.Editable
import android.view.Gravity
import android.view.View
import android.util.TypedValue
import android.graphics.Typeface
import android.text.method.ScrollingMovementMethod
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb
import java.io.InputStream
import java.nio.charset.StandardCharsets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.runtime.DisposableEffect
import com.absolutemango.expressiveui.byok.CompactModelDropdown
import com.absolutemango.expressiveui.byok.KeyStorehouseSection
import com.absolutemango.expressiveui.byok.ModelConfig
import com.absolutemango.expressiveui.byok.ModelPickerBottomSheet
import com.absolutemango.expressiveui.byok.ModelScraperEngine
import com.absolutemango.expressiveui.byok.ProjectRunState
import com.absolutemango.expressiveui.byok.ProjectZipDetector
import com.absolutemango.expressiveui.byok.ModelSlideBar
import com.absolutemango.expressiveui.byok.MrForgerSystemPrompt
import com.absolutemango.expressiveui.byok.ProviderProfile
import com.absolutemango.expressiveui.byok.ProviderProfileRepository
import com.absolutemango.expressiveui.byok.ProviderType
import com.absolutemango.expressiveui.byok.RetryPhase
import com.absolutemango.expressiveui.byok.SecureKeyStorehouse
import com.absolutemango.expressiveui.byok.SpikeClassification
import com.absolutemango.expressiveui.byok.SpikeRetryState
import com.absolutemango.expressiveui.byok.SystemInstructionPreset
import com.absolutemango.expressiveui.byok.TrafficSpikeBanner
import com.absolutemango.expressiveui.byok.TrafficSpikeClassifier
import com.absolutemango.expressiveui.byok.UniversalChatDispatcher
import com.absolutemango.expressiveui.byok.UniversalChatEngine
import com.absolutemango.expressiveui.byok.getProviderBrand
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

// ============================================================================
// 1. Data Models
// ============================================================================

data class ForgeMessage(
    val role: String, // "user" or "model"
    val content: String,
    val hasHtml: Boolean = false,
    val htmlPayload: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val apiError: String? = null
)

data class AiForgeSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<ForgeMessage> = emptyList()
)

data class SessionSummary(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messageCount: Int
)

data class ForgeModelOption(
    val displayName: String,
    val modelId: String
)

val FORGE_MODELS = listOf(
    ForgeModelOption("Gemini 3.5 Flash", "gemini-3.5-flash-lite"),
    ForgeModelOption("Gemini 3.6", "gemini-3.6-flash"),
    ForgeModelOption("Gemini 3.8", "gemini-2.5-flash"),
    ForgeModelOption("Gemini 3.1 Pro", "gemini-3.1-pro")
)

const val DEFAULT_FORGE_MODEL_ID = "gemini-3.6-flash"
private const val TAG_TRAFFIC = "AiForgeTrafficDetector"

// ============================================================================
// 2. HTML Detection & Sanitization Sniffer
// ============================================================================

object HtmlDetector {
    private val CODE_FENCE_REGEX = Regex("```(?:html)?\\s*([\\s\\S]*?)(?:```|$)", RegexOption.IGNORE_CASE)

    fun detectHtml(text: String): Boolean {
        if (CODE_FENCE_REGEX.containsMatchIn(text)) return true
        val lower = text.lowercase()
        return lower.contains("<!doctype html") || lower.contains("<html")
    }

    fun extractCleanHtml(text: String): String? {
        val match = CODE_FENCE_REGEX.find(text)
        if (match != null) {
            val code = match.groupValues[1].trim()
            if (code.isNotEmpty()) return code
        }
        val lower = text.lowercase()
        val doctypeIdx = lower.indexOf("<!doctype html")
        val htmlIdx = lower.indexOf("<html")
        val start = when {
            doctypeIdx != -1 && htmlIdx != -1 -> minOf(doctypeIdx, htmlIdx)
            doctypeIdx != -1 -> doctypeIdx
            htmlIdx != -1 -> htmlIdx
            else -> -1
        }
        return if (start != -1) text.substring(start).trim() else null
    }
}

// ============================================================================
// 3. Atomic Session Storage Manager
// ============================================================================

object ForgeSessionStorage {
    suspend fun saveSessions(context: Context, sessions: List<AiForgeSession>) = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "forge_sessions")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "forge_sessions.json")
            val atomicFile = AtomicFile(file)
            val jsonArray = JSONArray()

            sessions.forEach { session ->
                val sObj = JSONObject().apply {
                    put("id", session.id)
                    put("title", session.title)
                    put("createdAt", session.createdAt)
                    put("updatedAt", session.updatedAt)
                    val mArray = JSONArray()
                    session.messages.forEach { msg ->
                        mArray.put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                            put("hasHtml", msg.hasHtml)
                            if (msg.htmlPayload != null) put("htmlPayload", msg.htmlPayload)
                            put("timestamp", msg.timestamp)
                            if (msg.apiError != null) put("apiError", msg.apiError)
                        })
                    }
                    put("messages", mArray)
                }
                jsonArray.put(sObj)
            }

            val bytes = jsonArray.toString(2).toByteArray(Charsets.UTF_8)
            val fos = atomicFile.startWrite()
            try {
                fos.write(bytes)
                atomicFile.finishWrite(fos)
            } catch (e: Exception) {
                atomicFile.failWrite(fos)
                throw e
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadSessions(context: Context): List<AiForgeSession> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "forge_sessions")
        val file = File(dir, "forge_sessions.json")
        if (!file.exists()) {
            return@withContext migrateLegacyIfAvailable(context)
        }
        try {
            val text = file.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(text)
            val list = mutableListOf<AiForgeSession>()
            for (i in 0 until jsonArray.length()) {
                val sObj = jsonArray.getJSONObject(i)
                val mArray = sObj.getJSONArray("messages")
                val messages = mutableListOf<ForgeMessage>()
                for (j in 0 until mArray.length()) {
                    val mObj = mArray.getJSONObject(j)
                    val content = mObj.getString("content")
                    val hasHtml = mObj.optBoolean("hasHtml", HtmlDetector.detectHtml(content))
                    val htmlPayload = if (mObj.has("htmlPayload")) {
                        mObj.getString("htmlPayload")
                    } else if (hasHtml) {
                        HtmlDetector.extractCleanHtml(content)
                    } else null

                    messages.add(
                        ForgeMessage(
                            role = mObj.getString("role"),
                            content = content,
                            hasHtml = hasHtml,
                            htmlPayload = htmlPayload,
                            timestamp = mObj.optLong("timestamp", System.currentTimeMillis()),
                            apiError = if (mObj.has("apiError")) mObj.getString("apiError") else null
                        )
                    )
                }
                list.add(
                    AiForgeSession(
                        id = sObj.getString("id"),
                        title = sObj.getString("title"),
                        createdAt = sObj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = sObj.optLong("updatedAt", System.currentTimeMillis()),
                        messages = messages
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun migrateLegacyIfAvailable(context: Context): List<AiForgeSession> {
        val legacyPrefs = context.getSharedPreferences("ai_forge_prefs", Context.MODE_PRIVATE)
        val legacyHistory = legacyPrefs.getString("history", null) ?: return emptyList()
        val list = mutableListOf<ForgeMessage>()
        try {
            val array = JSONArray(legacyHistory)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val content = obj.getString("content")
                val hasHtml = HtmlDetector.detectHtml(content)
                val payload = if (hasHtml) HtmlDetector.extractCleanHtml(content) else null
                list.add(ForgeMessage(role = obj.getString("role"), content = content, hasHtml = hasHtml, htmlPayload = payload))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
        if (list.isEmpty()) return emptyList()

        val firstPrompt = list.firstOrNull { it.role == "user" }?.content ?: "Previous Session"
        val title = firstPrompt.trim().lines().firstOrNull()?.take(30) ?: "Previous Session"
        val migratedSession = AiForgeSession(
            title = title,
            messages = list
        )
        val sessions = listOf(migratedSession)
        saveSessions(context, sessions)
        return sessions
    }
}

// ============================================================================
// 4. Main Screen Composable
// ============================================================================

data class ActiveEditTarget(
    val uri: Uri,
    val fileName: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveAiForgeScreen(
    onBack: () -> Unit,
    onRunGeneratedHtml: (String) -> Unit,
    initialContext: String? = null,
    onOpenInEditor: ((String) -> Unit)? = null,
    onRunUri: ((Uri) -> Unit)? = null,
    onOpenFileUri: ((Uri) -> Unit)? = null,
    initialEditUri: Uri? = null,
    initialEditFileName: String? = null,
    onClearEditTarget: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current

    // Preferences: Expressive AI Forge with migration from legacy "ai_forge_prefs"
    val prefs = remember { context.getSharedPreferences("expressive_ai_forge_prefs", Context.MODE_PRIVATE) }
    val legacyPrefs = remember { context.getSharedPreferences("ai_forge_prefs", Context.MODE_PRIVATE) }

    var apiKey by remember {
        val currentKey = prefs.getString("api_key", null)
        val legacyKey = legacyPrefs.getString("api_key", "") ?: ""
        val resolved = if (!currentKey.isNullOrBlank()) currentKey else legacyKey
        if (currentKey == null && resolved.isNotBlank()) {
            prefs.edit { putString("api_key", resolved) }
        }
        mutableStateOf(resolved)
    }

    var backupApiKey by remember {
        val currentBackup = prefs.getString("backup_api_key", null)
        val legacyBackup = legacyPrefs.getString("backup_api_key", "") ?: ""
        val resolved = if (!currentBackup.isNullOrBlank()) currentBackup else legacyBackup
        if (currentBackup == null && resolved.isNotBlank()) {
            prefs.edit { putString("backup_api_key", resolved) }
        }
        mutableStateOf(resolved)
    }

    var selectedModel by remember {
        val currentModel = prefs.getString("selected_model", null)
        val legacyModel = legacyPrefs.getString("model", DEFAULT_FORGE_MODEL_ID) ?: DEFAULT_FORGE_MODEL_ID
        val resolved = if (!currentModel.isNullOrBlank()) currentModel else legacyModel
        if (currentModel == null) {
            prefs.edit { putString("selected_model", resolved) }
        }
        mutableStateOf(resolved)
    }

    var usingBackupKey by remember {
        mutableStateOf(prefs.getBoolean("using_backup_key", false))
    }

    var isByokEnabled by remember {
        mutableStateOf(prefs.getBoolean("byok_enabled", false))
    }

    val keyStorehouse = remember { SecureKeyStorehouse(context) }
    val profileRepository = remember { ProviderProfileRepository(context, keyStorehouse) }
    val chatEngine = remember { UniversalChatEngine() }
    val chatDispatcher = remember { UniversalChatDispatcher(profileRepository, keyStorehouse, chatEngine) }
    val scraperEngine = remember { ModelScraperEngine() }

    val activeProfile by profileRepository.activeProfile.collectAsState()
    val allProfiles by profileRepository.profiles.collectAsState()
    val customSystemPrompt by profileRepository.customSystemPrompt.collectAsState()
    val modelConfig by profileRepository.modelConfig.collectAsState()
    val customApiJson by profileRepository.customApiJson.collectAsState()
    var showModelPicker by remember { mutableStateOf(false) }
    var showCustomPromptDialog by remember { mutableStateOf(false) }
    var showModelConfigDialog by remember { mutableStateOf(false) }
    var showCustomJsonDialog by remember { mutableStateOf(false) }

    LaunchedEffect(apiKey, backupApiKey, selectedModel, isByokEnabled) {
        if (!isByokEnabled) {
            profileRepository.setActiveProfile(ProviderProfileRepository.DEFAULT_GEMINI_ID)
            val gemini = allProfiles.find { it.id == ProviderProfileRepository.DEFAULT_GEMINI_ID }
            if (gemini != null && gemini.selectedModel != selectedModel) {
                profileRepository.saveProfile(gemini.copy(selectedModel = selectedModel, isActive = true))
            }
        }
        if (apiKey.isNotBlank()) {
            keyStorehouse.storeKeys(ProviderProfileRepository.DEFAULT_GEMINI_ID, apiKey, backupApiKey.ifBlank { null })
        }
    }

    // Active session and conversation messages
    var activeSessionId by remember {
        mutableStateOf(prefs.getString("active_session_id", UUID.randomUUID().toString()) ?: UUID.randomUUID().toString())
    }
    var activeSessionTitle by remember { mutableStateOf("New Chat") }
    val messages = remember { mutableStateListOf<ForgeMessage>() }
    val savedSessions = remember { mutableStateListOf<AiForgeSession>() }

    // UI state
    var prompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var retryState by remember { mutableStateOf<SpikeRetryState>(SpikeRetryState.Idle) }
    val instantRetryTrigger = remember { Channel<Unit>(Channel.CONFLATED) }
    var activeDispatchJob by remember { mutableStateOf<Job?>(null) }
    var cachedOriginalPrompt by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val hazeState = remember { HazeState() }

    var activeEditTarget by remember {
        mutableStateOf(
            if (initialEditUri != null && !initialEditFileName.isNullOrBlank()) {
                ActiveEditTarget(initialEditUri, initialEditFileName)
            } else null
        )
    }
    var hasInjectedForSession by remember { mutableStateOf(false) }

    fun extractFileContent(uri: Uri): String {
        return try {
            if (uri.scheme == "file") {
                File(uri.path ?: "").readText()
            } else {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activeDispatchJob?.cancel()
            activeDispatchJob = null
        }
    }

    // Load sessions and restore active session
    LaunchedEffect(Unit) {
        val loaded = ForgeSessionStorage.loadSessions(context)
        savedSessions.clear()
        savedSessions.addAll(loaded)

        if (initialEditUri == null || initialEditFileName.isNullOrBlank()) {
            val currentSession = loaded.find { it.id == activeSessionId } ?: loaded.firstOrNull()
            if (currentSession != null) {
                activeSessionId = currentSession.id
                activeSessionTitle = currentSession.title
                messages.clear()
                messages.addAll(currentSession.messages)
            } else {
                activeSessionTitle = "New Chat"
            }
        }
    }

    // Dedicated file edit session auto-initialization
    LaunchedEffect(initialEditUri, initialEditFileName) {
        if (initialEditUri != null && !initialEditFileName.isNullOrBlank()) {
            activeEditTarget = ActiveEditTarget(initialEditUri, initialEditFileName)
            hasInjectedForSession = false
            val newSessionId = UUID.randomUUID().toString()
            val sessionTitle = "Edit: $initialEditFileName"
            activeSessionId = newSessionId
            activeSessionTitle = sessionTitle
            messages.clear()
            prompt = ""

            val newSession = AiForgeSession(
                id = newSessionId,
                title = sessionTitle,
                messages = emptyList(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            savedSessions.add(0, newSession)
            ForgeSessionStorage.saveSessions(context, savedSessions.toList())
            prefs.edit { putString("active_session_id", newSessionId) }
        }
    }

    // Initial context prompt (e.g., from file editor or modify action)
    LaunchedEffect(initialContext) {
        if (!initialContext.isNullOrBlank()) {
            prompt = "Please modify this HTML code:\n\n```html\n$initialContext\n```\n\nInstructions: "
        }
    }

    // Latest runnable HTML across conversation
    val latestHtml = remember(messages.size, isGenerating) {
        messages.asSequence()
            .filter { it.role == "model" }
            .mapNotNull { it.htmlPayload ?: HtmlDetector.extractCleanHtml(it.content) }
            .lastOrNull()
    }

    // Latest project run state across conversation
    val projectRunState: ProjectRunState = remember(messages.size, isGenerating, activeSessionId) {
        val modelMessages = messages.filter { it.role == "model" }
        var detected: ProjectRunState = ProjectRunState.None
        for (i in modelMessages.indices.reversed()) {
            val msg = modelMessages[i]
            val state = ProjectZipDetector.detectProjectState(
                context = context,
                sessionId = activeSessionId,
                content = msg.content,
                htmlPayload = msg.htmlPayload
            )
            if (state is ProjectRunState.ZipProject) {
                detected = state
                break
            } else if (state is ProjectRunState.SingleHtml && detected !is ProjectRunState.ZipProject) {
                detected = state
                break
            } else if (state is ProjectRunState.InvalidZip && detected is ProjectRunState.None) {
                detected = state
            }
        }
        detected
    }

    val isZipProject = projectRunState is ProjectRunState.ZipProject
    val hasRunnableHtml = isZipProject || latestHtml != null

    // Auto-scroll when messages update or generating begins
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Helper: persist current session state to disk
    fun persistActiveSession() {
        val firstUserMsg = messages.firstOrNull { it.role == "user" }?.content
        val derivedTitle = if (!firstUserMsg.isNullOrBlank()) {
            firstUserMsg.trim().lines().firstOrNull()?.take(30) ?: "Untitled Session"
        } else {
            activeSessionTitle.ifBlank { "Untitled Session" }
        }
        activeSessionTitle = derivedTitle

        val existingIndex = savedSessions.indexOfFirst { it.id == activeSessionId }
        val updatedSession = AiForgeSession(
            id = activeSessionId,
            title = derivedTitle,
            updatedAt = System.currentTimeMillis(),
            messages = messages.toList()
        )

        if (existingIndex != -1) {
            savedSessions[existingIndex] = updatedSession
        } else {
            savedSessions.add(0, updatedSession)
        }

        scope.launch {
            ForgeSessionStorage.saveSessions(context, savedSessions.toList())
        }
    }

    // Action: create a new session
    fun createNewSession() {
        activeDispatchJob?.cancel()
        activeDispatchJob = null
        retryState = SpikeRetryState.Idle
        if (messages.isNotEmpty()) {
            persistActiveSession()
        }
        activeEditTarget = null
        hasInjectedForSession = false
        onClearEditTarget?.invoke()
        activeSessionId = UUID.randomUUID().toString()
        prefs.edit { putString("active_session_id", activeSessionId) }
        activeSessionTitle = "New Chat"
        messages.clear()
        prompt = ""
        isMenuOpen = false
    }

    // Action: switch to a saved session
    fun selectSession(session: AiForgeSession) {
        activeDispatchJob?.cancel()
        activeDispatchJob = null
        retryState = SpikeRetryState.Idle
        if (messages.isNotEmpty() && activeSessionId != session.id) {
            persistActiveSession()
        }
        activeEditTarget = null
        hasInjectedForSession = false
        onClearEditTarget?.invoke()
        activeSessionId = session.id
        prefs.edit { putString("active_session_id", activeSessionId) }
        activeSessionTitle = session.title
        messages.clear()
        messages.addAll(session.messages)
        prompt = ""
        isMenuOpen = false
    }

    // Action: delete a session
    fun deleteSession(session: AiForgeSession) {
        if (activeSessionId == session.id) {
            activeDispatchJob?.cancel()
            activeDispatchJob = null
            retryState = SpikeRetryState.Idle
        }
        savedSessions.removeAll { it.id == session.id }
        scope.launch {
            ForgeSessionStorage.saveSessions(context, savedSessions.toList())
        }
        if (activeSessionId == session.id) {
            if (savedSessions.isNotEmpty()) {
                selectSession(savedSessions.first())
            } else {
                createNewSession()
            }
        }
    }

    // Helper: Stop active retry sequence and restore prompt
    fun handleStopRetry() {
        Log.w(TAG_TRAFFIC, "Retry loop aborted by user via Stop button. Restored prompt length: ${cachedOriginalPrompt.length} chars.")
        activeDispatchJob?.cancel()
        activeDispatchJob = null
        retryState = SpikeRetryState.Idle
        isGenerating = false

        if (messages.isNotEmpty()) {
            val lastMsg = messages.last()
            if (lastMsg.role == "model" && lastMsg.content.isBlank()) {
                messages.removeAt(messages.lastIndex)
            }
        }

        if (cachedOriginalPrompt.isNotBlank()) {
            prompt = cachedOriginalPrompt
        }
        persistActiveSession()
    }

    // Multi-turn stream dispatcher with universal BYOK, dual-key failover and traffic spike auto-retry
    fun handleSend() {
        if (prompt.isBlank() || isGenerating) return

        keyboardController?.hide()
        val userPrompt = prompt.trim()
        cachedOriginalPrompt = userPrompt
        prompt = ""
        retryState = SpikeRetryState.Idle

        val target = activeEditTarget
        val finalUserPrompt = if (target != null && !hasInjectedForSession) {
            val fileContent = extractFileContent(target.uri)
            hasInjectedForSession = true
            if (fileContent.isNotBlank()) {
                val ext = target.fileName.substringAfterLast('.', "html").lowercase()
                """
                $userPrompt

                ---
                ### Attached File: ${target.fileName}
                ```$ext
                $fileContent
                ```
                Please apply the requested changes to this file.
                """.trimIndent()
            } else {
                userPrompt
            }
        } else {
            userPrompt
        }

        val userMessage = ForgeMessage(
            role = "user",
            content = finalUserPrompt,
            hasHtml = HtmlDetector.detectHtml(finalUserPrompt),
            htmlPayload = HtmlDetector.extractCleanHtml(finalUserPrompt)
        )
        messages.add(userMessage)
        isGenerating = true

        // Placeholder for streaming assistant response
        val assistantPlaceholder = ForgeMessage(
            role = "model",
            content = "",
            hasHtml = false,
            htmlPayload = null
        )
        messages.add(assistantPlaceholder)
        val assistantIndex = messages.lastIndex

        activeDispatchJob?.cancel()
        activeDispatchJob = scope.launch {
            val maxAttempts = 10
            var currentAttempt = 0
            val historyMessages = messages.dropLast(2)

            while (isActive) {
                try {
                    retryState = (retryState as? SpikeRetryState.Retrying)?.copy(phase = RetryPhase.SENDING) ?: retryState
                    instantRetryTrigger.tryReceive() // Drain any previous trigger signal

                    var accumulated = ""
                    val startTime = System.currentTimeMillis()
                    var firstChunkReceived = false

                    chatDispatcher.sendPrompt(finalUserPrompt, historyMessages).collect { delta ->
                        if (!firstChunkReceived) {
                            firstChunkReceived = true
                            if (retryState !is SpikeRetryState.Idle) {
                                val recoveryLatency = System.currentTimeMillis() - startTime
                                Log.i(TAG_TRAFFIC, "Upstream recovered on attempt ${currentAttempt + 1}. Total recovery latency: ${recoveryLatency}ms.")
                                // Success system: API responded without error codes - reset retry state immediately
                                retryState = SpikeRetryState.Idle
                            }
                        }
                        accumulated += delta
                        val hasHtml = HtmlDetector.detectHtml(accumulated)
                        val htmlCode = if (hasHtml) HtmlDetector.extractCleanHtml(accumulated) else null

                        messages[assistantIndex] = ForgeMessage(
                            role = "model",
                            content = accumulated,
                            hasHtml = hasHtml,
                            htmlPayload = htmlCode,
                            apiError = null // Normal valid stream response
                        )
                    }

                    if (accumulated.isBlank()) {
                        throw Exception("Model returned empty response.")
                    }

                    // Success System: Full API response completed without error codes
                    retryState = SpikeRetryState.Idle
                    messages[assistantIndex] = messages[assistantIndex].copy(apiError = null)
                    persistActiveSession()
                    break // Generation succeeded normally
                } catch (e: CancellationException) {
                    throw e // Respect cancellation immediately
                } catch (e: Exception) {
                    val classification = TrafficSpikeClassifier.classify(e)
                    when (classification) {
                        is SpikeClassification.TransientSpike -> {
                            currentAttempt++
                            if (currentAttempt < maxAttempts) {
                                // Default delay decreased to 3 seconds for fast, effective retries
                                val delaySeconds = classification.retryAfterMs?.let {
                                    (it / 1000L).coerceIn(1L, 30L).toInt()
                                } ?: 3

                                Log.i(
                                    TAG_TRAFFIC,
                                    "Congestion spike captured. Code=${classification.statusCode}, Reason='${classification.reason}'. Scheduling retry attempt $currentAttempt/$maxAttempts in ${delaySeconds}s."
                                )

                                retryState = SpikeRetryState.Retrying(
                                    attempt = currentAttempt,
                                    maxAttempts = maxAttempts,
                                    secondsRemaining = delaySeconds,
                                    phase = RetryPhase.COUNTDOWN,
                                    errorSnippet = classification.reason
                                )

                                // Countdown loop with instant retry support
                                for (sec in delaySeconds downTo 1) {
                                    retryState = (retryState as? SpikeRetryState.Retrying)?.copy(
                                        secondsRemaining = sec,
                                        phase = RetryPhase.COUNTDOWN
                                    ) ?: break

                                    // Wait 1 second or interrupt immediately if user tapped "Retry Now"
                                    val skipped = withTimeoutOrNull(1000L) {
                                        instantRetryTrigger.receive()
                                    }
                                    if (skipped != null) {
                                        Log.i(TAG_TRAFFIC, "Instant retry triggered by user.")
                                        break
                                    }
                                }

                                // Status progression: Hourglass turns into loading indicator, displaying "Sending..." then "Loading..."
                                retryState = (retryState as? SpikeRetryState.Retrying)?.copy(
                                    secondsRemaining = 0,
                                    phase = RetryPhase.SENDING
                                ) ?: retryState
                                delay(300L)
                                retryState = (retryState as? SpikeRetryState.Retrying)?.copy(
                                    phase = RetryPhase.LOADING
                                ) ?: retryState

                                Log.i(TAG_TRAFFIC, "Dispatching retry attempt ${currentAttempt + 1}/$maxAttempts.")
                            } else {
                                Log.e(TAG_TRAFFIC, "Retry ceiling reached ($maxAttempts/$maxAttempts). Surfacing terminal failure.")
                                retryState = SpikeRetryState.Exhausted(classification.reason)
                                val errMsg = e.localizedMessage ?: classification.reason
                                messages[assistantIndex] = ForgeMessage(
                                    role = "model",
                                    content = "AI Forge Traffic Error: Upstream service overloaded ($maxAttempts retry attempts exhausted).\n\nPlease switch models or try again in a few minutes.",
                                    hasHtml = false,
                                    htmlPayload = null,
                                    apiError = errMsg
                                )
                                persistActiveSession()
                                break
                            }
                        }
                        is SpikeClassification.TerminalError -> {
                            retryState = SpikeRetryState.Idle
                            val errMsg = e.localizedMessage ?: classification.error
                            val guidance = if (classification.isAuthOrBilling) {
                                "\n\nPlease check your API key, billing balance, or quota limits in Menu > BYOK Vault."
                            } else {
                                "\n\nPlease check your provider profile and configuration."
                            }
                            messages[assistantIndex] = ForgeMessage(
                                role = "model",
                                content = "AI Forge Error: $guidance",
                                hasHtml = false,
                                htmlPayload = null,
                                apiError = errMsg
                            )
                            persistActiveSession()
                            break
                        }
                        is SpikeClassification.Ignore -> {
                            retryState = SpikeRetryState.Idle
                            val errMsg = e.localizedMessage ?: "Failed to generate response"
                            messages[assistantIndex] = ForgeMessage(
                                role = "model",
                                content = "AI Forge Error: An unexpected error occurred while communicating with the provider.",
                                hasHtml = false,
                                htmlPayload = null,
                                apiError = errMsg
                            )
                            persistActiveSession()
                            break
                        }
                    }
                }
            }
            isGenerating = false
        }
    }

    // Outer Root Container (Edge-to-edge canvas with atmospheric top/bottom gradient fades)
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
            val effectiveBottomInset = maxOf(navBarBottom, imeBottom)

            // Top padding: statusBarTop + 84.dp (header gradient area) + 40.dp (CompactModelDropdown row) + dynamic banner height + 8.dp breathing space
            val bannerHeight by animateDpAsState(
                targetValue = if (retryState is SpikeRetryState.Retrying) 52.dp else 0.dp,
                label = "traffic_banner_height"
            )
            val listTopPadding = statusBarTop + 92.dp + 40.dp + bannerHeight

            // Bottom padding: prompt box (100.dp) + dock top padding (28.dp) + dock bottom padding (12.dp) + effectiveBottomInset + 16.dp
            val listBottomPadding = 156.dp + effectiveBottomInset

            // Area 1: Chat Stream Canvas (Full-bleed across canvas, recorded as Haze Source for frosted backdrop blur)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface)
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = listTopPadding,
                        bottom = listBottomPadding,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (messages.isEmpty() && !isGenerating) {
                        item {
                            EmptyForgeState(
                                onSuggestionClick = { suggestion ->
                                    prompt = suggestion
                                },
                                hazeState = hazeState
                            )
                        }
                    }

                    items(messages.size) { index ->
                        val msg = messages[index]
                        val isLast = index == messages.lastIndex
                        val isBubbleGenerating = isGenerating && isLast && msg.role == "model"

                        if (msg.role == "user") {
                            UserMessageBubble(
                                message = msg,
                                onEditPrompt = { textToEdit ->
                                    prompt = textToEdit
                                    scope.launch {
                                        keyboardController?.show()
                                    }
                                }
                            )
                        } else {
                            ModelResponseCard(
                                message = msg,
                                isGenerating = isBubbleGenerating,
                                retryState = if (isLast) retryState else SpikeRetryState.Idle,
                                providerType = activeProfile?.providerType ?: ProviderType.GEMINI,
                                onRunHtml = { code ->
                                    onRunGeneratedHtml(code)
                                },
                                onRunUri = onRunUri
                            )
                        }
                    }
                }
            }

            // Area 2: Centered Floating Scroll-To-Bottom Button (Floats right above the prompt box dock)
            val isScrolledUp by remember {
                derivedStateOf {
                    listState.canScrollForward
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isScrolledUp,
                enter = fadeIn(tween(150)) + scaleIn(tween(150)),
                exit = fadeOut(tween(150)) + scaleOut(tween(150)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = effectiveBottomInset + 152.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            scope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll to Bottom",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Bottom",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            val currentPrimary = if (isByokEnabled) {
                !keyStorehouse.getPrimaryKey(activeProfile?.id ?: "").isNullOrBlank() || (activeProfile?.providerType == ProviderType.GEMINI && apiKey.isNotBlank())
            } else {
                apiKey.isNotBlank()
            }
            val currentBackup = if (isByokEnabled) {
                !keyStorehouse.getBackupKey(activeProfile?.id ?: "").isNullOrBlank()
            } else {
                backupApiKey.isNotBlank()
            }
            val currentUsingBackup = if (isByokEnabled) {
                activeProfile?.usingBackupKey == true
            } else {
                usingBackupKey
            }

            // Area 3: Top Navigation Bar and Horizontal Model Slide Bar
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                AiForgeTopBar(
                    hasRunnableHtml = hasRunnableHtml,
                    isZipProject = isZipProject,
                    isGenerating = isGenerating,
                    hasPrimary = currentPrimary,
                    hasBackup = currentBackup,
                    usingBackupKey = currentUsingBackup,
                    onBack = {
                        activeDispatchJob?.cancel()
                        onBack()
                    },
                    onOpenFileEditor = {
                        if (projectRunState is ProjectRunState.ZipProject && projectRunState.entryPointFile.exists()) {
                            val entryUri = Uri.fromFile(projectRunState.entryPointFile)
                            if (onOpenFileUri != null) {
                                onOpenFileUri(entryUri)
                            } else {
                                val htmlToEdit = try { projectRunState.entryPointFile.readText(Charsets.UTF_8) } catch (_: Exception) { latestHtml }
                                if (!isGenerating && htmlToEdit != null) {
                                    onOpenInEditor?.invoke(htmlToEdit) ?: onRunGeneratedHtml(htmlToEdit)
                                }
                            }
                        } else {
                            val htmlToEdit = latestHtml
                            if (!isGenerating && htmlToEdit != null) {
                                onOpenInEditor?.invoke(htmlToEdit) ?: onRunGeneratedHtml(htmlToEdit)
                            }
                        }
                    },
                    onRunViewer = {
                        if (projectRunState is ProjectRunState.ZipProject) {
                            val zipFile = projectRunState.zipFile
                            val playbackUri: Uri? = if (zipFile != null && zipFile.exists()) {
                                ZipArchiveSystem.openZipForPlayback(context, Uri.fromFile(zipFile))
                            } else if (projectRunState.entryPointFile.exists()) {
                                Uri.fromFile(projectRunState.entryPointFile)
                            } else {
                                null
                            }

                            if (playbackUri != null) {
                                if (onRunUri != null) {
                                    onRunUri(playbackUri)
                                } else {
                                    val code = try { projectRunState.entryPointFile.readText(Charsets.UTF_8) } catch (_: Exception) { latestHtml ?: "" }
                                    if (code.isNotBlank()) onRunGeneratedHtml(code)
                                }
                            } else {
                                val codeToRun = latestHtml ?: ""
                                if (!isGenerating && codeToRun.isNotBlank()) {
                                    onRunGeneratedHtml(codeToRun)
                                }
                            }
                        } else {
                            val codeToRun = latestHtml ?: ""
                            if (!isGenerating && codeToRun.isNotBlank()) {
                                onRunGeneratedHtml(codeToRun)
                            }
                        }
                    },
                    onCopyHtml = {
                        val htmlToCopy = if (projectRunState is ProjectRunState.ZipProject) {
                            try { projectRunState.entryPointFile.readText(Charsets.UTF_8) } catch (_: Exception) { latestHtml }
                        } else {
                            latestHtml
                        }
                        if (htmlToCopy != null) {
                            clipboardManager.setText(AnnotatedString(htmlToCopy))
                            Toast.makeText(context, if (isZipProject) "Project index.html copied to clipboard" else "HTML code copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onShareHtml = {
                        val htmlToShare = if (projectRunState is ProjectRunState.ZipProject) {
                            try { projectRunState.entryPointFile.readText(Charsets.UTF_8) } catch (_: Exception) { latestHtml }
                        } else {
                            latestHtml
                        }
                        if (htmlToShare != null) {
                            FileSharingHelper.shareHtmlContent(context, htmlToShare, asZip = false)
                        }
                    },
                    onShareZip = {
                        if (projectRunState is ProjectRunState.ZipProject && projectRunState.zipFile != null && projectRunState.zipFile.exists()) {
                            FileSharingHelper.shareExistingFile(context, projectRunState.zipFile, "application/zip", "Share Project ZIP")
                        } else if (latestHtml != null) {
                            FileSharingHelper.shareHtmlContent(context, latestHtml, asZip = true)
                        }
                    },
                    onOpenMenu = { isMenuOpen = true },
                    hazeState = hazeState
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    CompactModelDropdown(
                        activeProfile = if (isByokEnabled) activeProfile else (allProfiles.find { it.id == ProviderProfileRepository.DEFAULT_GEMINI_ID } ?: activeProfile),
                        onOpenModelPicker = { showModelPicker = true },
                        onSelectModel = { modelId ->
                            if (isByokEnabled) {
                                if (activeProfile != null) {
                                    profileRepository.saveProfile(activeProfile!!.copy(selectedModel = modelId))
                                }
                            } else {
                                selectedModel = modelId
                                prefs.edit { putString("selected_model", modelId) }
                                val gemini = allProfiles.find { it.id == ProviderProfileRepository.DEFAULT_GEMINI_ID }
                                if (gemini != null) {
                                    profileRepository.saveProfile(gemini.copy(selectedModel = modelId, isActive = true))
                                }
                            }
                        },
                        onOpenSettings = { isMenuOpen = true }
                    )
                }

                TrafficSpikeBanner(
                    state = retryState as? SpikeRetryState.Retrying,
                    onStop = { handleStopRetry() },
                    onSwitchModel = { showModelPicker = true },
                    onRetryNow = { instantRetryTrigger.trySend(Unit) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Area 4: Keyboard-Responsive Prompt Dock with Upward Gradient Fade (Replaces solid back box)
            ExpressiveAiForgePromptBox(
                prompt = prompt,
                onPromptChange = { prompt = it },
                isGenerating = isGenerating,
                canSend = prompt.isNotBlank() && !isGenerating,
                onSend = { handleSend() },
                activeEditFileName = activeEditTarget?.fileName,
                onClearEditTarget = {
                    activeEditTarget = null
                    onClearEditTarget?.invoke()
                },
                hazeState = hazeState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Area 3: Slide-In Menu Overlay
            SlideInMenuOverlay(
                isOpen = isMenuOpen,
                onDismiss = { isMenuOpen = false },
                repository = profileRepository,
                keyStorehouse = keyStorehouse,
                scraperEngine = scraperEngine,
                apiKey = apiKey,
                onApiKeyChange = {
                    apiKey = it
                    prefs.edit { putString("api_key", it) }
                    keyStorehouse.storeKeys(ProviderProfileRepository.DEFAULT_GEMINI_ID, it, backupApiKey.ifBlank { null })
                },
                backupApiKey = backupApiKey,
                onBackupApiKeyChange = {
                    backupApiKey = it
                    prefs.edit { putString("backup_api_key", it) }
                    keyStorehouse.storeKeys(ProviderProfileRepository.DEFAULT_GEMINI_ID, apiKey, it.ifBlank { null })
                },
                selectedModel = selectedModel,
                onModelSelected = { modelId ->
                    selectedModel = modelId
                    prefs.edit { putString("selected_model", modelId) }
                    val gemini = allProfiles.find { it.id == ProviderProfileRepository.DEFAULT_GEMINI_ID }
                    if (gemini != null) {
                        profileRepository.saveProfile(gemini.copy(selectedModel = modelId, isActive = true))
                    }
                    if (!isByokEnabled) {
                        profileRepository.setActiveProfile(ProviderProfileRepository.DEFAULT_GEMINI_ID)
                    }
                },
                usingBackupKey = if (isByokEnabled) (activeProfile?.usingBackupKey == true) else usingBackupKey,
                onResetToPrimary = {
                    if (isByokEnabled && activeProfile != null) {
                        profileRepository.updateBackupKeyUsage(activeProfile!!.id, false, 0)
                    }
                    usingBackupKey = false
                    prefs.edit { putBoolean("using_backup_key", false) }
                    profileRepository.updateBackupKeyUsage(ProviderProfileRepository.DEFAULT_GEMINI_ID, false, 0)
                },
                isByokEnabled = isByokEnabled,
                onToggleByok = { enabled ->
                    isByokEnabled = enabled
                    prefs.edit { putBoolean("byok_enabled", enabled) }
                    if (!enabled) {
                        profileRepository.setActiveProfile(ProviderProfileRepository.DEFAULT_GEMINI_ID)
                    }
                },
                vaultProfiles = allProfiles,
                activeProfileId = activeProfile?.id,
                onSelectVaultModel = { profileId, modelId ->
                    if (!isByokEnabled) {
                        isByokEnabled = true
                        prefs.edit { putBoolean("byok_enabled", true) }
                    }
                    val target = allProfiles.find { it.id == profileId }
                    if (target != null) {
                        profileRepository.saveProfile(target.copy(selectedModel = modelId, isActive = true))
                    }
                    profileRepository.setActiveProfile(profileId)
                },
                sessions = savedSessions,
                activeSessionId = activeSessionId,
                onCreateNewSession = { createNewSession() },
                onSelectSession = { selectSession(it) },
                onDeleteSession = { deleteSession(it) },
                hasCustomSystemPrompt = customSystemPrompt.isNotBlank(),
                onOpenCustomPromptDialog = { showCustomPromptDialog = true },
                hasCustomModelConfig = modelConfig.hasCustomOverrides(),
                onOpenModelConfigDialog = { showModelConfigDialog = true },
                hasCustomApiJson = customApiJson.isNotBlank(),
                onOpenCustomJsonDialog = { showCustomJsonDialog = true }
            )

            if (showModelPicker) {
                ModelPickerBottomSheet(
                    activeProfile = if (isByokEnabled) activeProfile else (allProfiles.find { it.id == ProviderProfileRepository.DEFAULT_GEMINI_ID } ?: activeProfile),
                    allProfiles = if (isByokEnabled) allProfiles else allProfiles.filter { it.id == ProviderProfileRepository.DEFAULT_GEMINI_ID },
                    onDismiss = { showModelPicker = false },
                    onModelSelected = { profileId, modelId ->
                        if (profileId != ProviderProfileRepository.DEFAULT_GEMINI_ID && !isByokEnabled) {
                            isByokEnabled = true
                            prefs.edit { putBoolean("byok_enabled", true) }
                        }
                        profileRepository.setActiveProfile(profileId)
                        val target = allProfiles.find { it.id == profileId }
                        if (target != null) {
                            profileRepository.saveProfile(target.copy(selectedModel = modelId, isActive = true))
                        }
                        if (profileId == ProviderProfileRepository.DEFAULT_GEMINI_ID) {
                            selectedModel = modelId
                            prefs.edit { putString("selected_model", modelId) }
                        }
                        showModelPicker = false
                    }
                )
            }

            if (showCustomPromptDialog) {
                CustomSystemPromptDialog(
                    currentPrompt = customSystemPrompt,
                    onDismiss = { showCustomPromptDialog = false },
                    onSavePrompt = { newPrompt ->
                        profileRepository.setCustomSystemPrompt(newPrompt)
                        prefs.edit { putString("custom_system_prompt", newPrompt) }
                        showCustomPromptDialog = false
                    }
                )
            }

            if (showModelConfigDialog) {
                ModelConfigDialog(
                    currentConfig = modelConfig,
                    onDismiss = { showModelConfigDialog = false },
                    onSaveConfig = { newConfig ->
                        profileRepository.setModelConfig(newConfig)
                        showModelConfigDialog = false
                    }
                )
            }

            if (showCustomJsonDialog) {
                CustomApiJsonDialog(
                    currentJson = customApiJson,
                    onDismiss = { showCustomJsonDialog = false },
                    onSaveJson = { newJson ->
                        profileRepository.setCustomApiJson(newJson)
                        prefs.edit { putString("custom_api_json", newJson) }
                        showCustomJsonDialog = false
                    }
                )
            }
        }
    }
}

// ============================================================================
// 5. Top Navigation Bar & Segmented Split Pill
// ============================================================================

@Composable
private fun AiForgeTopBar(
    hasRunnableHtml: Boolean,
    isZipProject: Boolean = false,
    isGenerating: Boolean,
    hasPrimary: Boolean,
    hasBackup: Boolean,
    usingBackupKey: Boolean,
    onBack: () -> Unit,
    onOpenFileEditor: () -> Unit,
    onRunViewer: () -> Unit,
    onCopyHtml: () -> Unit,
    onShareHtml: () -> Unit = {},
    onShareZip: () -> Unit = {},
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val surface = MaterialTheme.colorScheme.surface
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBarHeight = statusBarTop + 84.dp

    val backBlurStyle = remember(surface, surfaceContainerHigh) {
        HazeBlurStyle {
            blurRadius(20.dp)
            backgroundColor(surface)
            colorEffects(listOf(HazeColorEffect.tint(surfaceContainerHigh.copy(alpha = 0.70f))))
        }
    }

    val titleBlurStyle = remember(surface, surfaceContainerHigh) {
        HazeBlurStyle {
            blurRadius(20.dp)
            backgroundColor(surface)
            colorEffects(listOf(HazeColorEffect.tint(surfaceContainerHigh.copy(alpha = 0.70f))))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(topBarHeight)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        surface,
                        surface.copy(alpha = 0.98f),
                        surface.copy(alpha = 0.88f),
                        surface.copy(alpha = 0.55f),
                        surface.copy(alpha = 0.20f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp)
    ) {
        // Left: Circular Back Button with frosted backdrop blur
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(44.dp)
                .clip(CircleShape)
                .then(
                    if (hazeState != null) {
                        Modifier.hazeBlur(
                            input = HazeInput.Sources(hazeState),
                            style = backBlurStyle
                        )
                    } else {
                        Modifier.background(surfaceContainerHigh.copy(alpha = 0.85f))
                    }
                )
                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Center: Title Capsule Pill (AI Forge canvas with frosted backdrop blur)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(21.dp))
                .then(
                    if (hazeState != null) {
                        Modifier.hazeBlur(
                            input = HazeInput.Sources(hazeState),
                            style = titleBlurStyle
                        )
                    } else {
                        Modifier.background(surfaceContainerHigh.copy(alpha = 0.85f))
                    }
                )
                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(21.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AI Forge",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Right Stack: Segmented Split Pill [ Play | Menu ] with Compacted Dual-Dot Status Indicator below
        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            SegmentedSplitPill(
                hasRunnableHtml = hasRunnableHtml,
                isZipProject = isZipProject,
                isGenerating = isGenerating,
                onOpenFileEditor = onOpenFileEditor,
                onRunViewer = onRunViewer,
                onCopyHtml = onCopyHtml,
                onShareHtml = onShareHtml,
                onShareZip = onShareZip,
                onMenu = onOpenMenu,
                hazeState = hazeState
            )

            // Compacted Dual-Dot Indicator directly underneath the split pill
            CompactedDualDotIndicator(
                hasPrimary = hasPrimary,
                hasBackup = hasBackup,
                usingBackup = usingBackupKey
            )
        }
    }
}

@Composable
private fun CompactedDualDotIndicator(
    hasPrimary: Boolean,
    hasBackup: Boolean,
    usingBackup: Boolean,
    modifier: Modifier = Modifier
) {
    val dot1Color = when {
        !hasPrimary -> Color(0xFF9CA3AF) // Gray
        usingBackup -> Color(0xFFEF4444) // Red (exhausted)
        else -> Color(0xFF22C55E) // Green (active)
    }

    val dot2Color = when {
        !hasBackup -> Color(0xFF9CA3AF) // Gray
        usingBackup -> Color(0xFF22C55E) // Green (active backup)
        else -> Color(0xFF9CA3AF) // Standby Gray
    }

    Row(
        modifier = modifier.padding(top = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dot1Color, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dot2Color, CircleShape)
        )
    }
}

@Composable
private fun SegmentedSplitPill(
    hasRunnableHtml: Boolean,
    isZipProject: Boolean = false,
    isGenerating: Boolean,
    onOpenFileEditor: () -> Unit,
    onRunViewer: () -> Unit,
    onCopyHtml: () -> Unit,
    onShareHtml: () -> Unit = {},
    onShareZip: () -> Unit = {},
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val surface = MaterialTheme.colorScheme.surface
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    var isPlayFlyoutExpanded by remember { mutableStateOf(false) }

    val activeAccentColor = if (isZipProject) Color(0xFFEF4444) else Color(0xFF4F46E5)

    // Pulsing outline stroke animation for Play segment when runnable HTML is detected
    val infiniteTransition = rememberInfiniteTransition(label = "split_pill_pulse")
    val pulsingStrokeWidth by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "play_outline_pulse"
    )

    val playBorderStroke = if (hasRunnableHtml) {
        BorderStroke(pulsingStrokeWidth.dp, activeAccentColor)
    } else {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    val playIconTint = if (hasRunnableHtml) {
        activeAccentColor
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    val playShape = RoundedCornerShape(topStart = 21.dp, bottomStart = 21.dp, topEnd = 0.dp, bottomEnd = 0.dp)
    val menuShape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 21.dp, bottomEnd = 21.dp)

    val playBlurStyle = remember(surface, surfaceContainerHigh, hasRunnableHtml, isZipProject) {
        HazeBlurStyle {
            blurRadius(20.dp)
            backgroundColor(surface)
            colorEffects(
                listOf(
                    HazeColorEffect.tint(
                        if (hasRunnableHtml) activeAccentColor.copy(alpha = 0.22f)
                        else surfaceContainerHigh.copy(alpha = 0.70f)
                    )
                )
            )
        }
    }

    val menuBlurStyle = remember(surface, surfaceContainerHigh) {
        HazeBlurStyle {
            blurRadius(20.dp)
            backgroundColor(surface)
            colorEffects(listOf(HazeColorEffect.tint(surfaceContainerHigh.copy(alpha = 0.70f))))
        }
    }

    Row(
        modifier = modifier.height(42.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Left Segment: Play Button with frosted backdrop blur (First option: File Editor)
        Box(modifier = Modifier.wrapContentSize()) {
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .width(48.dp)
                    .clip(playShape)
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeBlur(
                                input = HazeInput.Sources(hazeState),
                                style = playBlurStyle
                            )
                        } else {
                            Modifier.background(
                                if (hasRunnableHtml) activeAccentColor.copy(alpha = 0.18f)
                                else surfaceContainerHigh.copy(alpha = 0.65f)
                            )
                        }
                    )
                    .border(playBorderStroke, playShape)
                    .clickable(
                        enabled = hasRunnableHtml && !isGenerating,
                        onClick = { isPlayFlyoutExpanded = true }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isZipProject) Icons.Default.FolderZip else Icons.Default.PlayArrow,
                    contentDescription = if (isZipProject) "Run ZIP Project" else "Run Options",
                    modifier = Modifier.size(22.dp),
                    tint = playIconTint
                )
            }

            DropdownMenu(
                expanded = isPlayFlyoutExpanded,
                onDismissRequest = { isPlayFlyoutExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("File Editor") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        isPlayFlyoutExpanded = false
                        onOpenFileEditor()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isZipProject) "Play Project (.zip)" else "Run in Viewer") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isZipProject) Icons.Default.FolderZip else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = activeAccentColor
                        )
                    },
                    onClick = {
                        isPlayFlyoutExpanded = false
                        onRunViewer()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isZipProject) "Copy index.html" else "Copy HTML Code") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        isPlayFlyoutExpanded = false
                        onCopyHtml()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Share HTML") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        isPlayFlyoutExpanded = false
                        onShareHtml()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isZipProject) "Share Project (.zip)" else "Share as ZIP") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    },
                    onClick = {
                        isPlayFlyoutExpanded = false
                        onShareZip()
                    }
                )
            }
        }

        // Right Segment: Menu Button with frosted backdrop blur
        Box(
            modifier = Modifier
                .height(42.dp)
                .width(48.dp)
                .clip(menuShape)
                .then(
                    if (hazeState != null) {
                        Modifier.hazeBlur(
                            input = HazeInput.Sources(hazeState),
                            style = menuBlurStyle
                        )
                    } else {
                        Modifier.background(surfaceContainerHigh.copy(alpha = 0.85f))
                    }
                )
                .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant), menuShape)
                .clickable(onClick = onMenu),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open Settings & Recents",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ============================================================================
// 6. Chat Canvas Messages & Diamond Spark Badge
// ============================================================================

@Composable
private fun UserMessageBubble(
    message: ForgeMessage,
    onEditPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box {
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Markdown(
                        content = message.content,
                        colors = markdownColor(
                            text = MaterialTheme.colorScheme.onPrimaryContainer,
                            codeBackground = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            inlineCodeBackground = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        ),
                        typography = markdownTypography(
                            paragraph = MaterialTheme.typography.bodyMedium
                        ),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(vertical = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { isMenuExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Copy Message") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        isMenuExpanded = false
                        clipboardManager.setText(AnnotatedString(message.content))
                        Toast.makeText(context, "Prompt copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Edit Prompt") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        isMenuExpanded = false
                        onEditPrompt(message.content)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModelResponseCard(
    message: ForgeMessage,
    isGenerating: Boolean,
    retryState: SpikeRetryState = SpikeRetryState.Idle,
    providerType: ProviderType = ProviderType.GEMINI,
    onRunHtml: (String) -> Unit,
    onRunUri: ((Uri) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val (badgeColor, badgeIcon) = getProviderBrand(providerType)
    val isZipMsg = remember(message.content) { ProjectZipDetector.hasProjectFlag(message.content) }
    val cleanHtml = remember(message.content, message.htmlPayload) {
        message.htmlPayload ?: HtmlDetector.extractCleanHtml(message.content)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Unclipped container hosting the response card and the overlapping Diamond Spark Badge
        Box(modifier = Modifier.widthIn(max = 340.dp)) {
            // Response Card Body
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp)) {
                    // Top header row: flyout options button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { isMenuExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copy Response") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    isMenuExpanded = false
                                    clipboardManager.setText(AnnotatedString(message.content))
                                    Toast.makeText(context, "Response copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                            if (isZipMsg) {
                                DropdownMenuItem(
                                    text = { Text("Run Project (.zip)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.FolderZip,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        isMenuExpanded = false
                                        val zipState = ProjectZipDetector.detectProjectState(context, "msg_${message.hashCode()}", message.content)
                                        if (zipState is ProjectRunState.ZipProject) {
                                            val playUri = if (zipState.zipFile != null && zipState.zipFile.exists()) {
                                                ZipArchiveSystem.openZipForPlayback(context, Uri.fromFile(zipState.zipFile))
                                            } else {
                                                Uri.fromFile(zipState.entryPointFile)
                                            }
                                            if (playUri != null && onRunUri != null) {
                                                onRunUri(playUri)
                                            } else if (cleanHtml != null) {
                                                onRunHtml(cleanHtml)
                                            }
                                        } else if (cleanHtml != null) {
                                            onRunHtml(cleanHtml)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share Project (.zip)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    },
                                    onClick = {
                                        isMenuExpanded = false
                                        val zipState = ProjectZipDetector.detectProjectState(context, "msg_${message.hashCode()}", message.content)
                                        if (zipState is ProjectRunState.ZipProject && zipState.zipFile != null && zipState.zipFile.exists()) {
                                            FileSharingHelper.shareExistingFile(context, zipState.zipFile, "application/zip", "Share Project ZIP")
                                        } else if (cleanHtml != null) {
                                            FileSharingHelper.shareHtmlContent(context, cleanHtml, asZip = true)
                                        }
                                    }
                                )
                            }
                            if (cleanHtml != null && !isZipMsg) {
                                DropdownMenuItem(
                                    text = { Text("Copy HTML Code") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        isMenuExpanded = false
                                        clipboardManager.setText(AnnotatedString(cleanHtml))
                                        Toast.makeText(context, "HTML code copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share HTML") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        isMenuExpanded = false
                                        FileSharingHelper.shareHtmlContent(context, cleanHtml, asZip = false)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share as ZIP") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    },
                                    onClick = {
                                        isMenuExpanded = false
                                        FileSharingHelper.shareHtmlContent(context, cleanHtml, asZip = true)
                                    }
                                )
                            }
                        }
                    }

                    if (isGenerating && message.content.isBlank()) {
                        if (retryState is SpikeRetryState.Retrying) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                when (retryState.phase) {
                                    RetryPhase.COUNTDOWN -> {
                                        Icon(
                                            imageVector = Icons.Rounded.HourglassTop,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = Color(0xFFF59E0B)
                                        )
                                        Text(
                                            text = "Server overloaded. Retrying in ${retryState.secondsRemaining}s (${retryState.attempt}/${retryState.maxAttempts})...",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = Color(0xFFF59E0B)
                                        )
                                    }
                                    RetryPhase.SENDING -> {
                                        ExpressiveLoadingIndicator(modifier = Modifier.size(20.dp))
                                        Text(
                                            text = "Sending (${retryState.attempt}/${retryState.maxAttempts})...",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    RetryPhase.LOADING -> {
                                        ExpressiveLoadingIndicator(modifier = Modifier.size(20.dp))
                                        Text(
                                            text = "Loading response (${retryState.attempt}/${retryState.maxAttempts})...",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else {
                            // Material 3 Expressive Loading Indicator before first stream chunk
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                ExpressiveLoadingIndicator(modifier = Modifier.size(24.dp))
                                Text(
                                    text = "Forging HTML response...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Render formatted text and collapsible code sections
                        FormattedAssistantResponse(
                            content = message.content,
                            onRunCode = onRunHtml
                        )
                    }

                    // Enclosed API Error Box below the text and within the response box
                    val displayError = message.apiError ?: (retryState as? SpikeRetryState.Retrying)?.errorSnippet?.takeIf { it.isNotBlank() }
                    if (!displayError.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "API Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "API Error",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = displayError,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Material 3 Expressive Indeterminate Wavy Indicator on Top Edge of API response box
                if (isGenerating) {
                    LinearWavyProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                        color = if (retryState is SpikeRetryState.Retrying) Color(0xFFF59E0B) else badgeColor,
                        trackColor = Color.Transparent
                    )
                }
            }
        }

            // Dynamic AI Provider Brand Badge
            Box(
                modifier = Modifier
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = providerType.displayName,
                    modifier = Modifier.size(16.5.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun FormattedAssistantResponse(
    content: String,
    onRunCode: (String) -> Unit
) {
    val sections = remember(content) { parseResponseSections(content) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sections.forEach { section ->
            when (section) {
                is ContentSection.Text -> {
                    Markdown(
                        content = section.text,
                        colors = markdownColor(
                            text = MaterialTheme.colorScheme.onSurface,
                            codeBackground = MaterialTheme.colorScheme.surfaceContainerLow,
                            inlineCodeBackground = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        typography = markdownTypography(
                            paragraph = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                        ),
                        components = markdownComponents(
                            codeBlock = highlightedCodeBlock,
                            codeFence = highlightedCodeFence
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is ContentSection.Code -> {
                    val isHtml = section.language?.contains("html", ignoreCase = true) == true ||
                            HtmlDetector.detectHtml(section.code)

                    if (isHtml) {
                        CollapsibleHtmlCodeBlock(
                            code = section.code,
                            onRunCode = onRunCode
                        )
                    } else {
                        // Syntax-highlighted code block for non-HTML code (CSS, JS, JSON, etc.)
                        val codeFence = "```${section.language ?: ""}\n${section.code}\n```"
                        Markdown(
                            content = codeFence,
                            colors = markdownColor(
                                text = MaterialTheme.colorScheme.onSurface,
                                codeBackground = MaterialTheme.colorScheme.surfaceContainerLowest
                            ),
                            components = markdownComponents(
                                codeBlock = highlightedCodeBlock,
                                codeFence = highlightedCodeFence
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Expand/Collapse Handle specifically for detected HTML code.
 * By default it is collapsed for minimal UI coverage, while leaving surrounding
 * text and summaries fully visible.
 */
@Composable
private fun CollapsibleHtmlCodeBlock(
    code: String,
    onRunCode: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val lineCount = remember(code) { code.lines().size }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Expand/Collapse Handle Bar (Always visible, minimal UI footprint)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "HTML Code",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "$lineCount lines",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Play in Viewer Action button on handle
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF4F46E5).copy(alpha = 0.15f),
                        contentColor = Color(0xFF4F46E5),
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onRunCode(code) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Play", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Chevron Indicator (Down = collapsed, Up = expanded)
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse HTML code" else "Expand HTML code",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Collapsible Code Content (Hidden by default)
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = code,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

private sealed class ContentSection {
    data class Text(val text: String) : ContentSection()
    data class Code(val language: String?, val code: String) : ContentSection()
}

private fun parseResponseSections(raw: String): List<ContentSection> {
    val result = mutableListOf<ContentSection>()
    val fenceRegex = Regex("```([a-zA-Z0-9_-]*)\\s*\\n?([\\s\\S]*?)(?:```|$)")
    var lastIndex = 0

    fenceRegex.findAll(raw).forEach { match ->
        val textBefore = raw.substring(lastIndex, match.range.first).trim()
        if (textBefore.isNotEmpty()) {
            splitRawHtmlIfNeeded(textBefore, result)
        }
        val lang = match.groupValues[1].ifBlank { null }
        val code = match.groupValues[2].trim()
        if (code.isNotEmpty()) {
            result.add(ContentSection.Code(lang, code))
        }
        lastIndex = match.range.last + 1
    }

    if (lastIndex < raw.length) {
        val remaining = raw.substring(lastIndex).trim()
        if (remaining.isNotEmpty()) {
            splitRawHtmlIfNeeded(remaining, result)
        }
    }

    if (result.isEmpty() && raw.isNotBlank()) {
        splitRawHtmlIfNeeded(raw, result)
    }
    return result
}

private fun splitRawHtmlIfNeeded(text: String, outList: MutableList<ContentSection>) {
    val lower = text.lowercase()
    val doctypeIdx = lower.indexOf("<!doctype html")
    val htmlIdx = lower.indexOf("<html")
    val start = when {
        doctypeIdx != -1 && htmlIdx != -1 -> minOf(doctypeIdx, htmlIdx)
        doctypeIdx != -1 -> doctypeIdx
        htmlIdx != -1 -> htmlIdx
        else -> -1
    }

    if (start != -1) {
        val before = text.substring(0, start).trim()
        if (before.isNotEmpty()) {
            outList.add(ContentSection.Text(before))
        }
        val htmlCode = text.substring(start).trim()
        if (htmlCode.isNotEmpty()) {
            outList.add(ContentSection.Code("html", htmlCode))
        }
    } else {
        outList.add(ContentSection.Text(text))
    }
}


// ============================================================================
// 8. Empty Canvas State
// ============================================================================

@Composable
private fun EmptyForgeState(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val surface = MaterialTheme.colorScheme.surface
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val chipBlurStyle = remember(surface, surfaceContainerHigh) {
        HazeBlurStyle {
            blurRadius(16.dp)
            backgroundColor(surface)
            colorEffects(listOf(HazeColorEffect.tint(surfaceContainerHigh.copy(alpha = 0.50f))))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = "Welcome to AI Forge",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Enter a prompt below or pick a blueprint suggestion to instantly generate and live-preview responsive web prototypes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        val suggestions = listOf(
            "Create a modern glassmorphic landing page",
            "Build an interactive 3D particle canvas",
            "Generate a responsive calculator with dark mode",
            "Design an animated retro arcade game"
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .then(
                            if (hazeState != null) {
                                Modifier.hazeBlur(
                                    input = HazeInput.Sources(hazeState),
                                    style = chipBlurStyle
                                )
                            } else {
                                Modifier.background(surfaceContainerHigh.copy(alpha = 0.5f))
                            }
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .clickable { onSuggestionClick(suggestion) }
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// 9. Area 3: Slide-In Menu Overlay (API Settings & Recents)
// ============================================================================

@Composable
private fun SlideInMenuOverlay(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    repository: ProviderProfileRepository,
    keyStorehouse: SecureKeyStorehouse,
    scraperEngine: ModelScraperEngine,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    backupApiKey: String,
    onBackupApiKeyChange: (String) -> Unit,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    usingBackupKey: Boolean,
    onResetToPrimary: () -> Unit,
    isByokEnabled: Boolean,
    onToggleByok: (Boolean) -> Unit,
    vaultProfiles: List<ProviderProfile>,
    activeProfileId: String?,
    onSelectVaultModel: (profileId: String, modelId: String) -> Unit,
    sessions: List<AiForgeSession>,
    activeSessionId: String,
    onCreateNewSession: () -> Unit,
    onSelectSession: (AiForgeSession) -> Unit,
    onDeleteSession: (AiForgeSession) -> Unit,
    hasCustomSystemPrompt: Boolean,
    onOpenCustomPromptDialog: () -> Unit,
    hasCustomModelConfig: Boolean,
    onOpenModelConfigDialog: () -> Unit,
    hasCustomApiJson: Boolean,
    onOpenCustomJsonDialog: () -> Unit
) {
    BackHandler(enabled = isOpen) {
        onDismiss()
    }

    // Edge drag gesture to dismiss
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        if (delta > 0) {
            dragOffsetX += delta
            if (dragOffsetX > 150f) {
                onDismiss()
                dragOffsetX = 0f
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Semi-transparent scrim revealing the underlying chat canvas on the left
        androidx.compose.animation.AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = fadeOut(animationSpec = tween(durationMillis = 250))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onDismiss)
            )
        }

        // Half-revealing side panel (82% width, rounded left edge, shadow) with synchronized slide in/out
        androidx.compose.animation.AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.85f)
                    .padding(top = statusBarTop + 4.dp, bottom = navBarBottom + 4.dp, end = 8.dp)
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Horizontal,
                        onDragStopped = { dragOffsetX = 0f }
                    ),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                shadowElevation = 16.dp
            ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Menu Header: (AI Forge) pill | MENU title (dead-center) | Close button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                Text(
                                    text = "AI Forge",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "MENU",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.Center)
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onDismiss),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Menu",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Section 1: API Settings Card
                        ApiSettingsCard(
                            apiKey = apiKey,
                            onApiKeyChange = onApiKeyChange,
                            backupApiKey = backupApiKey,
                            onBackupApiKeyChange = onBackupApiKeyChange,
                            selectedModel = selectedModel,
                            onModelSelected = onModelSelected,
                            usingBackupKey = usingBackupKey,
                            onResetToPrimary = onResetToPrimary,
                            isByokEnabled = isByokEnabled,
                            onToggleByok = onToggleByok,
                            vaultProfiles = vaultProfiles,
                            activeProfileId = activeProfileId,
                            onSelectVaultModel = onSelectVaultModel,
                            hasCustomSystemPrompt = hasCustomSystemPrompt,
                            onOpenCustomPromptDialog = onOpenCustomPromptDialog,
                            hasCustomModelConfig = hasCustomModelConfig,
                            onOpenModelConfigDialog = onOpenModelConfigDialog,
                            hasCustomApiJson = hasCustomApiJson,
                            onOpenCustomJsonDialog = onOpenCustomJsonDialog
                        )

                        // Section 1b: BYOK Key Storehouse Vault (revealed when BYOK is enabled)
                        AnimatedVisibility(
                            visible = isByokEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            KeyStorehouseSection(
                                repository = repository,
                                keyStorehouse = keyStorehouse,
                                scraperEngine = scraperEngine
                            )
                        }

                        // Section 2: Recents Card
                        RecentsCard(
                            sessions = sessions,
                            activeSessionId = activeSessionId,
                            onCreateNewSession = onCreateNewSession,
                            onSelectSession = onSelectSession,
                            onDeleteSession = onDeleteSession
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

// ============================================================================
// 10. Menu Sub-Cards: API Settings & Recents
// ============================================================================

@Composable
private fun ApiSettingsCard(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    backupApiKey: String,
    onBackupApiKeyChange: (String) -> Unit,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    usingBackupKey: Boolean,
    onResetToPrimary: () -> Unit,
    isByokEnabled: Boolean,
    onToggleByok: (Boolean) -> Unit,
    vaultProfiles: List<ProviderProfile>,
    activeProfileId: String?,
    onSelectVaultModel: (profileId: String, modelId: String) -> Unit,
    hasCustomSystemPrompt: Boolean,
    onOpenCustomPromptDialog: () -> Unit,
    hasCustomModelConfig: Boolean,
    onOpenModelConfigDialog: () -> Unit,
    hasCustomApiJson: Boolean,
    onOpenCustomJsonDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPrimarySecret by remember { mutableStateOf(false) }
    var showBackupSecret by remember { mutableStateOf(false) }

    var isGeminiExpanded by remember { mutableStateOf(true) }
    val expandedVaultStates = remember { mutableStateMapOf<String, Boolean>() }
    val nonDefaultVaults = remember(vaultProfiles) {
        vaultProfiles.filter { it.id != ProviderProfileRepository.DEFAULT_GEMINI_ID }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with AI Studio link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "API SETTINGS",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                // Info Icon / Link to Google AI Studio
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Get Key",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open Google AI Studio",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Primary Key Input
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Primary API Key") },
                singleLine = true,
                visualTransformation = if (showPrimarySecret) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPrimarySecret = !showPrimarySecret }) {
                        Icon(
                            imageVector = if (showPrimarySecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle visibility"
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            // Backup Key Input
            OutlinedTextField(
                value = backupApiKey,
                onValueChange = onBackupApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Backup API Key") },
                singleLine = true,
                visualTransformation = if (showBackupSecret) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showBackupSecret = !showBackupSecret }) {
                        Icon(
                            imageVector = if (showBackupSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle visibility"
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            // 3-State Dual-Dot Status Indicator & Reset Toggle
            DualDotStatusIndicator(
                hasPrimary = apiKey.isNotBlank(),
                hasBackup = backupApiKey.isNotBlank(),
                usingBackup = usingBackupKey,
                onResetToPrimary = onResetToPrimary
            )

            // Custom System Prompt Button
            Surface(
                onClick = onOpenCustomPromptDialog,
                shape = RoundedCornerShape(14.dp),
                color = if (hasCustomSystemPrompt) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                },
                border = BorderStroke(
                    1.dp,
                    if (hasCustomSystemPrompt) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (hasCustomSystemPrompt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Custom System Prompt",
                                    tint = if (hasCustomSystemPrompt) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Custom System Prompt",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasCustomSystemPrompt) "Custom prompt active (.md / text)" else "Default Mr. Forger prompt",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasCustomSystemPrompt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Custom System Prompt",
                        tint = if (hasCustomSystemPrompt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Model Config Button
            Surface(
                onClick = onOpenModelConfigDialog,
                shape = RoundedCornerShape(14.dp),
                color = if (hasCustomModelConfig) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                },
                border = BorderStroke(
                    1.dp,
                    if (hasCustomModelConfig) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (hasCustomModelConfig) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Model Config",
                                    tint = if (hasCustomModelConfig) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Model Config",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasCustomModelConfig) "Custom parameters active" else "Default provider parameters",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasCustomModelConfig) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Model Config",
                        tint = if (hasCustomModelConfig) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Custom API JSON Button
            Surface(
                onClick = onOpenCustomJsonDialog,
                shape = RoundedCornerShape(14.dp),
                color = if (hasCustomApiJson) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                },
                border = BorderStroke(
                    1.dp,
                    if (hasCustomApiJson) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (hasCustomApiJson) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Custom API JSON",
                                    tint = if (hasCustomApiJson) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Custom API JSON",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasCustomApiJson) "Custom JSON payload active" else "Default API payload",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasCustomApiJson) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Custom API JSON",
                        tint = if (hasCustomApiJson) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Model Selection Header
            Text(
                text = "MODEL SELECTION",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Category 1: Google Gemini (Collapsible Accordion)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isGeminiExpanded = !isGeminiExpanded },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Gemini",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (isGeminiExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isGeminiExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = isGeminiExpanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(FORGE_MODELS) { modelOption ->
                                    val isSelected = (!isByokEnabled || activeProfileId == ProviderProfileRepository.DEFAULT_GEMINI_ID) && selectedModel == modelOption.modelId
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onModelSelected(modelOption.modelId) },
                                        label = { Text(modelOption.displayName) },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Categories 2+: Vault Saves (Collapsible Accordion for each added vault)
            nonDefaultVaults.forEach { vault ->
                val isExpanded = expandedVaultStates[vault.id] ?: false
                val (brandColor, brandIcon) = getProviderBrand(vault.providerType)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, brandColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedVaultStates[vault.id] = !isExpanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = brandIcon,
                                contentDescription = null,
                                tint = brandColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = vault.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${vault.models.size} models • ${vault.providerType.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                if (vault.models.isEmpty()) {
                                    Text(
                                        text = "No models configured yet. Use Vault below to fetch or add models.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        items(vault.models) { modelId ->
                                            val isSelected = isByokEnabled && activeProfileId == vault.id && vault.selectedModel == modelId
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { onSelectVaultModel(vault.id, modelId) },
                                                label = { Text(modelId.split("/").last()) },
                                                leadingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                } else null,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = brandColor.copy(alpha = 0.2f),
                                                    selectedLabelColor = brandColor
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Divider & BYOK Master Toggle Switch
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bring Your Own Key (BYOK)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Unlock multi-provider vault & custom endpoints",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = isByokEnabled,
                    onCheckedChange = onToggleByok
                )
            }
        }
    }
}

@Composable
private fun DualDotStatusIndicator(
    hasPrimary: Boolean,
    hasBackup: Boolean,
    usingBackup: Boolean,
    onResetToPrimary: () -> Unit
) {
    // Dot 1 (Primary): Green if active, Red if failover active, Gray if not set
    val dot1Color = when {
        !hasPrimary -> Color(0xFF9CA3AF)
        usingBackup -> Color(0xFFEF4444) // Primary exhausted/bypassed
        else -> Color(0xFF22C55E) // Primary active
    }

    // Dot 2 (Backup): Green if active, Gray if standby/not set
    val dot2Color = when {
        !hasBackup -> Color(0xFF9CA3AF)
        usingBackup -> Color(0xFF22C55E) // Backup active
        else -> Color(0xFF9CA3AF) // Backup on standby
    }

    val statusText = when {
        !hasPrimary && !hasBackup -> "No API Keys Set"
        usingBackup -> "Backup Key Active (Failover)"
        else -> "Primary Key Active"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dual Dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(dot1Color, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(dot2Color, CircleShape)
                    )
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (usingBackup) {
                TextButton(
                    onClick = onResetToPrimary,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Reset", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun RecentsCard(
    sessions: List<AiForgeSession>,
    activeSessionId: String,
    onCreateNewSession: () -> Unit,
    onSelectSession: (AiForgeSession) -> Unit,
    onDeleteSession: (AiForgeSession) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: RECENTS title & "+ Create New Session" button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENTS",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                FilledTonalButton(
                    onClick = onCreateNewSession,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Create New Session", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (sessions.isEmpty()) {
                Text(
                    text = "No saved sessions yet. Your conversations are saved here automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sessions.forEach { session ->
                        val isActive = session.id == activeSessionId

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSession(session) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${dateFormat.format(Date(session.updatedAt))} • ${session.messages.size} msgs",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteSession(session) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Session",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
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

// ============================================================================
// 11. Custom System Prompt Dialog (.md Upload & Text Box Editor)
// ============================================================================

private fun readInputStreamTextUniversal(inputStream: InputStream): String {
    val bytes = inputStream.readBytes()
    if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
        return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
    }
    if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
        return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE)
    }
    if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
        return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
    }
    val raw = String(bytes, StandardCharsets.UTF_8)
    return if (raw.startsWith("\uFEFF")) raw.substring(1) else raw
}

@Composable
private fun CustomSystemPromptDialog(
    currentPrompt: String,
    onDismiss: () -> Unit,
    onSavePrompt: (String) -> Unit
) {
    val context = LocalContext.current
    var promptText by remember { mutableStateOf(currentPrompt.removePrefix("\uFEFF")) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                    readInputStreamTextUniversal(stream)
                } ?: ""
                val cleaned = text.removePrefix("\uFEFF").trim()
                if (cleaned.isNotBlank()) {
                    promptText = cleaned
                    val fileName = ZipArchiveSystem.getFileName(context, uri)
                    Toast.makeText(context, "Loaded $fileName (${cleaned.length} chars)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Selected file is empty", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("AiForge", "Error loading prompt file", e)
                Toast.makeText(context, "Failed to read file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header: Icon + Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Custom System Prompt",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Upload a .md file or edit instructions below",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Action Bar: Upload .md File, Template, Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            try {
                                filePickerLauncher.launch(arrayOf("*/*", "text/*", "text/plain", "text/markdown"))
                            } catch (e: Exception) {
                                try {
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Upload .md File",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            val clip = try {
                                clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
                            } catch (_: Exception) { "" }
                            val cleaned = clip.removePrefix("\uFEFF").trim()
                            if (cleaned.isNotBlank()) {
                                promptText = cleaned
                                Toast.makeText(context, "Pasted ${cleaned.length} chars from clipboard", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Paste", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            promptText = MrForgerSystemPrompt.getPrompt(SystemInstructionPreset.ZIP_STRICT)
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Template", style = MaterialTheme.typography.labelMedium)
                    }

                    if (promptText.isNotBlank()) {
                        IconButton(
                            onClick = { promptText = "" },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Prompt",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Text Box for typing or reviewing markdown
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = { Text("System Prompt") },
                    placeholder = {
                        Text("Enter your custom markdown system prompt here...\n\nYou can specify custom persona rules, code styling guidelines, framework preferences, or specific instructions for AI Forge.")
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(Modifier.height(8.dp))

                // Info Footer: Character Count & Unsaved status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (promptText.isBlank()) "Default system prompt will be used" else "${promptText.length} characters",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (currentPrompt.isNotBlank() && promptText != currentPrompt) {
                        Text(
                            text = "Modified",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Bottom Action Buttons: Reset to Default, Cancel, Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentPrompt.isNotBlank()) {
                        TextButton(
                            onClick = {
                                onSavePrompt("")
                                Toast.makeText(context, "Reset to default system prompt", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Reset Default", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val trimmed = promptText.trim()
                            onSavePrompt(trimmed)
                            if (trimmed.isNotBlank()) {
                                Toast.makeText(context, "Custom system prompt saved", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Using default system prompt", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// ============================================================================
// 12. Model Config Dialog (Hyperparameters, Samplers, Gemini & Local/OpenRouter)
// ============================================================================

@Composable
private fun ModelConfigDialog(
    currentConfig: ModelConfig,
    onDismiss: () -> Unit,
    onSaveConfig: (ModelConfig) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Universal / OpenAI", "Gemini Native", "Extra Samplers")

    // Category 1: Universal / OpenAI
    var tempOverride by remember { mutableStateOf(currentConfig.temperature != null) }
    var temperature by remember { mutableFloatStateOf(currentConfig.temperature ?: 1.0f) }

    var topPOverride by remember { mutableStateOf(currentConfig.topP != null) }
    var topP by remember { mutableFloatStateOf(currentConfig.topP ?: 1.0f) }

    var topK by remember { mutableStateOf(currentConfig.topK?.toString() ?: "") }
    var maxCompletionTokens by remember { mutableStateOf(currentConfig.maxCompletionTokens?.toString() ?: "") }
    var maxTokens by remember { mutableStateOf(currentConfig.maxTokens?.toString() ?: "") }
    var reasoningEffort by remember { mutableStateOf(currentConfig.reasoningEffort ?: "") }
    var stop by remember { mutableStateOf(currentConfig.stop ?: "") }
    var stream by remember { mutableStateOf(currentConfig.stream) }

    var freqPenaltyOverride by remember { mutableStateOf(currentConfig.frequencyPenalty != null) }
    var frequencyPenalty by remember { mutableFloatStateOf(currentConfig.frequencyPenalty ?: 0.0f) }

    var presPenaltyOverride by remember { mutableStateOf(currentConfig.presencePenalty != null) }
    var presencePenalty by remember { mutableFloatStateOf(currentConfig.presencePenalty ?: 0.0f) }

    var seed by remember { mutableStateOf(currentConfig.seed?.toString() ?: "") }
    var responseFormat by remember { mutableStateOf(currentConfig.responseFormat ?: "") }
    var logprobs by remember { mutableStateOf(currentConfig.logprobs ?: false) }
    var topLogprobs by remember { mutableStateOf(currentConfig.topLogprobs?.toString() ?: "") }
    var logitBias by remember { mutableStateOf(currentConfig.logitBias ?: "") }
    var n by remember { mutableStateOf(currentConfig.n?.toString() ?: "") }

    // Category 2: Gemini Native (generationConfig)
    var geminiTempOverride by remember { mutableStateOf(currentConfig.geminiTemperature != null) }
    var geminiTemperature by remember { mutableFloatStateOf(currentConfig.geminiTemperature ?: 1.0f) }

    var geminiTopPOverride by remember { mutableStateOf(currentConfig.geminiTopP != null) }
    var geminiTopP by remember { mutableFloatStateOf(currentConfig.geminiTopP ?: 0.95f) }

    var geminiTopK by remember { mutableStateOf(currentConfig.geminiTopK?.toString() ?: "") }
    var geminiMaxOutputTokens by remember { mutableStateOf(currentConfig.geminiMaxOutputTokens?.toString() ?: "") }
    var geminiStopSequences by remember { mutableStateOf(currentConfig.geminiStopSequences ?: "") }

    var geminiFreqPenaltyOverride by remember { mutableStateOf(currentConfig.geminiFrequencyPenalty != null) }
    var geminiFrequencyPenalty by remember { mutableFloatStateOf(currentConfig.geminiFrequencyPenalty ?: 0.0f) }

    var geminiPresPenaltyOverride by remember { mutableStateOf(currentConfig.geminiPresencePenalty != null) }
    var geminiPresencePenalty by remember { mutableFloatStateOf(currentConfig.geminiPresencePenalty ?: 0.0f) }

    var geminiSeed by remember { mutableStateOf(currentConfig.geminiSeed?.toString() ?: "") }
    var geminiCandidateCount by remember { mutableStateOf(currentConfig.geminiCandidateCount?.toString() ?: "") }
    var geminiResponseMimeType by remember { mutableStateOf(currentConfig.geminiResponseMimeType ?: "") }
    var geminiResponseSchema by remember { mutableStateOf(currentConfig.geminiResponseSchema ?: "") }
    var geminiThinkingLevel by remember { mutableStateOf(currentConfig.geminiThinkingLevel ?: "") }
    var geminiThinkingBudget by remember { mutableStateOf(currentConfig.geminiThinkingBudget?.toString() ?: "") }

    // Category 3: Local / OpenRouter Extra Samplers
    var minPOverride by remember { mutableStateOf(currentConfig.minP != null) }
    var minP by remember { mutableFloatStateOf(currentConfig.minP ?: 0.05f) }

    var repPenaltyOverride by remember { mutableStateOf(currentConfig.repetitionPenalty != null) }
    var repetitionPenalty by remember { mutableFloatStateOf(currentConfig.repetitionPenalty ?: 1.10f) }

    var typicalPOverride by remember { mutableStateOf(currentConfig.typicalP != null) }
    var typicalP by remember { mutableFloatStateOf(currentConfig.typicalP ?: 1.0f) }

    var tfsZOverride by remember { mutableStateOf(currentConfig.tfsZ != null) }
    var tfsZ by remember { mutableFloatStateOf(currentConfig.tfsZ ?: 1.0f) }

    var mirostat by remember { mutableStateOf(currentConfig.mirostat?.toString() ?: "") }
    var mirostatTau by remember { mutableStateOf(currentConfig.mirostatTau?.toString() ?: "") }
    var mirostatEta by remember { mutableStateOf(currentConfig.mirostatEta?.toString() ?: "") }
    var repeatLastN by remember { mutableStateOf(currentConfig.repeatLastN?.toString() ?: "") }
    var numCtx by remember { mutableStateOf(currentConfig.numCtx?.toString() ?: "") }

    val hasAnyOverrides = tempOverride || topPOverride || topK.isNotBlank() ||
            maxCompletionTokens.isNotBlank() || maxTokens.isNotBlank() || reasoningEffort.isNotBlank() ||
            stop.isNotBlank() || !stream || freqPenaltyOverride || presPenaltyOverride || seed.isNotBlank() ||
            responseFormat.isNotBlank() || logprobs || topLogprobs.isNotBlank() || logitBias.isNotBlank() ||
            n.isNotBlank() || geminiTempOverride || geminiTopPOverride || geminiTopK.isNotBlank() ||
            geminiMaxOutputTokens.isNotBlank() || geminiStopSequences.isNotBlank() || geminiFreqPenaltyOverride ||
            geminiPresPenaltyOverride || geminiSeed.isNotBlank() || geminiCandidateCount.isNotBlank() ||
            geminiResponseMimeType.isNotBlank() || geminiResponseSchema.isNotBlank() || geminiThinkingLevel.isNotBlank() ||
            geminiThinkingBudget.isNotBlank() || minPOverride || repPenaltyOverride || typicalPOverride ||
            tfsZOverride || mirostat.isNotBlank() || mirostatTau.isNotBlank() || mirostatEta.isNotBlank() ||
            repeatLastN.isNotBlank() || numCtx.isNotBlank()

    fun resetAll() {
        tempOverride = false; temperature = 1.0f
        topPOverride = false; topP = 1.0f
        topK = ""
        maxCompletionTokens = ""
        maxTokens = ""
        reasoningEffort = ""
        stop = ""
        stream = true
        freqPenaltyOverride = false; frequencyPenalty = 0.0f
        presPenaltyOverride = false; presencePenalty = 0.0f
        seed = ""
        responseFormat = ""
        logprobs = false
        topLogprobs = ""
        logitBias = ""
        n = ""

        geminiTempOverride = false; geminiTemperature = 1.0f
        geminiTopPOverride = false; geminiTopP = 0.95f
        geminiTopK = ""
        geminiMaxOutputTokens = ""
        geminiStopSequences = ""
        geminiFreqPenaltyOverride = false; geminiFrequencyPenalty = 0.0f
        geminiPresPenaltyOverride = false; geminiPresencePenalty = 0.0f
        geminiSeed = ""
        geminiCandidateCount = ""
        geminiResponseMimeType = ""
        geminiResponseSchema = ""
        geminiThinkingLevel = ""
        geminiThinkingBudget = ""

        minPOverride = false; minP = 0.05f
        repPenaltyOverride = false; repetitionPenalty = 1.10f
        typicalPOverride = false; typicalP = 1.0f
        tfsZOverride = false; tfsZ = 1.0f
        mirostat = ""
        mirostatTau = ""
        mirostatEta = ""
        repeatLastN = ""
        numCtx = ""
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header: Icon + Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Model Config",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Fine-tune hyperparameters and samplers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Traditional Tabs Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Scrollable Content Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Tab 1: Universal / OpenAI
                            ModelConfigSliderItem(
                                label = "temperature",
                                description = "Randomness / creativity (0.0 = deterministic, 2.0 = creative)",
                                isOverridden = tempOverride,
                                value = temperature,
                                valueRange = 0.0f..2.0f,
                                onToggleOverride = { tempOverride = it },
                                onValueChange = { temperature = it }
                            )

                            ModelConfigSliderItem(
                                label = "top_p",
                                description = "Nucleus sampling probability mass (0.0 to 1.0)",
                                isOverridden = topPOverride,
                                value = topP,
                                valueRange = 0.0f..1.0f,
                                onToggleOverride = { topPOverride = it },
                                onValueChange = { topP = it }
                            )

                            ModelConfigTextFieldItem(
                                label = "top_k",
                                description = "Filter top-K tokens (OpenRouter / open-weight providers only)",
                                value = topK,
                                onValueChange = { topK = it },
                                placeholder = "e.g. 40",
                                keyboardType = KeyboardType.Number
                            )

                            ModelConfigTextFieldItem(
                                label = "max_completion_tokens",
                                description = "Max tokens for completion (o1, o3, GPT-4o)",
                                value = maxCompletionTokens,
                                onValueChange = { maxCompletionTokens = it },
                                placeholder = "e.g. 4096",
                                keyboardType = KeyboardType.Number
                            )

                            ModelConfigTextFieldItem(
                                label = "max_tokens",
                                description = "Max completion tokens (legacy models)",
                                value = maxTokens,
                                onValueChange = { maxTokens = it },
                                placeholder = "e.g. 2048",
                                keyboardType = KeyboardType.Number
                            )

                            ModelConfigTextFieldItem(
                                label = "reasoning_effort",
                                description = "Constrains effort for reasoning models (e.g. low, medium, high)",
                                value = reasoningEffort,
                                onValueChange = { reasoningEffort = it },
                                placeholder = "low, medium, high"
                            )

                            ModelConfigTextFieldItem(
                                label = "stop",
                                description = "Up to 4 sequences where generation will stop",
                                value = stop,
                                onValueChange = { stop = it },
                                placeholder = "Sequence or comma-separated list"
                            )

                            ModelConfigSwitchItem(
                                label = "stream",
                                description = "Enable Server-Sent Events (SSE) token streaming",
                                checked = stream,
                                onCheckedChange = { stream = it }
                            )

                            ModelConfigSliderItem(
                                label = "frequency_penalty",
                                description = "Penalize new tokens based on frequency (-2.0 to 2.0)",
                                isOverridden = freqPenaltyOverride,
                                value = frequencyPenalty,
                                valueRange = -2.0f..2.0f,
                                onToggleOverride = { freqPenaltyOverride = it },
                                onValueChange = { frequencyPenalty = it }
                            )

                            ModelConfigSliderItem(
                                label = "presence_penalty",
                                description = "Penalize new tokens based on presence (-2.0 to 2.0)",
                                isOverridden = presPenaltyOverride,
                                value = presencePenalty,
                                valueRange = -2.0f..2.0f,
                                onToggleOverride = { presPenaltyOverride = it },
                                onValueChange = { presencePenalty = it }
                            )

                            ModelConfigTextFieldItem(
                                label = "seed",
                                description = "Deterministic sampling seed (integer)",
                                value = seed,
                                onValueChange = { seed = it },
                                placeholder = "e.g. 42",
                                keyboardType = KeyboardType.Number
                            )

                            ModelConfigTextFieldItem(
                                label = "response_format",
                                description = "Enforce format (e.g. json_object or JSON schema)",
                                value = responseFormat,
                                onValueChange = { responseFormat = it },
                                placeholder = "json_object"
                            )

                            ModelConfigSwitchItem(
                                label = "logprobs",
                                description = "Return log probabilities on the most likely tokens",
                                checked = logprobs,
                                onCheckedChange = { logprobs = it }
                            )

                            ModelConfigTextFieldItem(
                                label = "top_logprobs",
                                description = "Number of most likely tokens to return at each position (0-20)",
                                value = topLogprobs,
                                onValueChange = { topLogprobs = it },
                                placeholder = "0 to 20",
                                keyboardType = KeyboardType.Number
                            )

                            ModelConfigTextFieldItem(
                                label = "logit_bias",
                                description = "Modify likelihood of specified tokens (JSON map string)",
                                value = logitBias,
                                onValueChange = { logitBias = it },
                                placeholder = "{\"50256\": -100}"
                            )

                            ModelConfigTextFieldItem(
                                label = "n",
                                description = "Number of chat completion choices to generate",
                                value = n,
                                onValueChange = { n = it },
                                placeholder = "e.g. 1",
                                keyboardType = KeyboardType.Number
                            )
                        }

                        1 -> {
                            // Tab 2: Gemini Native (generationConfig)
                            ModelConfigSliderItem(
                                label = "temperature",
                                description = "Gemini generation temperature (0.0 to 2.0)",
                                isOverridden = geminiTempOverride,
                                value = geminiTemperature,
                                valueRange = 0.0f..2.0f,
                                onToggleOverride = { geminiTempOverride = it },
                                onValueChange = { geminiTemperature = it }
                            )

                            ModelConfigSliderItem(
                                label = "topP",
                                description = "Gemini nucleus sampling topP (0.0 to 1.0)",
                                isOverridden = geminiTopPOverride,
                                value = geminiTopP,
                                valueRange = 0.0f..1.0f,
                                onToggleOverride = { geminiTopPOverride = it },
                                onValueChange = { geminiTopP = it }
                            )

                            ModelConfigTextFieldItem(
                                label = "topK",
                                description = "Gemini top-K sampling candidate pool",
                                value = geminiTopK,
                                onValueChange = { geminiTopK = it },
                                placeholder = "e.g. 40",
                                keyboardType = KeyboardType.Number
                            )

                            ModelConfigTextFieldItem(
                                label = "maxOutputTokens",
                                description = "Max response tokens for Gemini (e.g. 8192)",
                                value = geminiMaxOutputTokens,
                                onValueChange = { geminiMaxOutputTokens = it },
                                placeholder = "e.g. 8192",
                                keyboardType = KeyboardType.Number
                            )

                            ModelConfigTextFieldItem(
                                label = "stopSequences",
                                description = "Comma-separated sequences to halt Gemini generation",
                                value = geminiStopSequences,
                                onValueChange = { geminiStopSequences = it },
                                placeholder = "Comma-separated or [\"stop1\"]"
                            )

                            ModelConfigSliderItem(
                                label = "frequencyPenalty",
                                description = "Gemini frequency penalty (-2.0 to 2.0)",
                                isOverridden = geminiFreqPenaltyOverride,
                                value = geminiFrequencyPenalty,
                                valueRange = -2.0f..2.0f,
                                onToggleOverride = { geminiFreqPenaltyOverride = it },
                                onValueChange = { geminiFrequencyPenalty = it }
                            )

                            ModelConfigSliderItem(
                                label = "presencePenalty",
                                description = "Gemini presence penalty (-2.0 to 2.0)",
                                isOverridden = geminiPresPenaltyOverride,
                                value = geminiPresencePenalty,
                                valueRange = -2.0f..2.0f,
                                onToggleOverride = { geminiPresPenaltyOverride = it },
                                onValueChange = { geminiPresencePenalty = it }
                            )

                            ModelConfigTextFieldItem(
                                label = "seed",
                                description = "Gemini deterministic sampling seed",
                                value = geminiSeed,
                                onValueChange = { geminiSeed = it },
                                placeholder = "e.g. 12345",
                                keyboardType = KeyboardType.Number
                            )

                            ModelConfigTextFieldItem(
                                label = "candidateCount",
                                description = "Number of generated responses to return",
                                value = geminiCandidateCount,
                                onValueChange = { geminiCandidateCount = it },
                                placeholder = "e.g. 1",
                                keyboardType = KeyboardType.Number
                            )

                            ModelConfigTextFieldItem(
                                label = "responseMimeType",
                                description = "MIME type (application/json or text/plain)",
                                value = geminiResponseMimeType,
                                onValueChange = { geminiResponseMimeType = it },
                                placeholder = "application/json"
                            )

                            ModelConfigTextFieldItem(
                                label = "responseSchema",
                                description = "Structured JSON schema for Gemini response format",
                                value = geminiResponseSchema,
                                onValueChange = { geminiResponseSchema = it },
                                placeholder = "JSON schema specification"
                            )

                            ModelConfigTextFieldItem(
                                label = "thinkingConfig (thinkingLevel)",
                                description = "Gemini thinking depth level (e.g. LOW, MEDIUM, HIGH)",
                                value = geminiThinkingLevel,
                                onValueChange = { geminiThinkingLevel = it },
                                placeholder = "LOW, MEDIUM, HIGH"
                            )

                            ModelConfigTextFieldItem(
                                label = "thinkingConfig (thinkingBudget)",
                                description = "Budget tokens for thinking (-1 for dynamic/off)",
                                value = geminiThinkingBudget,
                                onValueChange = { geminiThinkingBudget = it },
                                placeholder = "e.g. 1024 or -1",
                                keyboardType = KeyboardType.Number
                            )
                        }

                        2 -> {
                            // Tab 3: Local / OpenRouter Extra Samplers
                            ModelConfigSliderItem(
                                label = "min_p",
                                description = "Minimum token probability relative to top token (0.0 to 1.0)",
                                isOverridden = minPOverride,
                                value = minP,
                                valueRange = 0.0f..1.0f,
                                onToggleOverride = { minPOverride = it },
                                onValueChange = { minP = it }
                            )

                            ModelConfigSliderItem(
                                label = "repetition_penalty",
                                description = "Penalize repeated tokens (1.0 = none, 1.1 = moderate)",
                                isOverridden = repPenaltyOverride,
                                value = repetitionPenalty,
                                valueRange = 0.0f..2.0f,
                                onToggleOverride = { repPenaltyOverride = it },
                                onValueChange = { repetitionPenalty = it }
                            )

                            ModelConfigSliderItem(
                                label = "typical_p",
                                description = "Locally typical sampling threshold (0.0 to 1.0)",
                                isOverridden = typicalPOverride,
                                value = typicalP,
                                valueRange = 0.0f..1.0f,
                                onToggleOverride = { typicalPOverride = it },
                                onValueChange = { typicalP = it }
                            )

                            ModelConfigSliderItem(
                                label = "tfs_z",
                                description = "Tail Free Sampling parameter z (0.0 to 1.0)",
                                isOverridden = tfsZOverride,
                                value = tfsZ,
                                valueRange = 0.0f..1.0f,
                                onToggleOverride = { tfsZOverride = it },
                                onValueChange = { tfsZ = it }
                            )

                            ModelConfigTextFieldItem(
                                label = "mirostat",
                                description = "Mirostat sampling algorithm (0 = off, 1 = v1, 2 = v2)",
                                value = mirostat,
                                onValueChange = { mirostat = it },
                                placeholder = "0, 1, or 2",
                                keyboardType = KeyboardType.Number
                            )

                            ModelConfigTextFieldItem(
                                label = "mirostat_tau",
                                description = "Mirostat target perplexity / entropy (tau)",
                                value = mirostatTau,
                                onValueChange = { mirostatTau = it },
                                placeholder = "e.g. 5.0",
                                keyboardType = KeyboardType.Decimal
                            )

                            ModelConfigTextFieldItem(
                                label = "mirostat_eta",
                                description = "Mirostat learning rate (eta)",
                                value = mirostatEta,
                                onValueChange = { mirostatEta = it },
                                placeholder = "e.g. 0.1",
                                keyboardType = KeyboardType.Decimal
                            )

                            ModelConfigTextFieldItem(
                                label = "repeat_last_n",
                                description = "Number of past tokens to check for repetition penalty",
                                value = repeatLastN,
                                onValueChange = { repeatLastN = it },
                                placeholder = "e.g. 64",
                                keyboardType = KeyboardType.Number
                            )

                            ModelConfigTextFieldItem(
                                label = "num_ctx (Ollama)",
                                description = "Context window buffer size for Ollama models",
                                value = numCtx,
                                onValueChange = { numCtx = it },
                                placeholder = "e.g. 8192, 32768",
                                keyboardType = KeyboardType.Number
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Bottom Action Buttons: Reset Defaults, Cancel, Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasAnyOverrides) {
                        TextButton(
                            onClick = {
                                resetAll()
                                Toast.makeText(context, "Reset all settings to provider defaults", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Reset Defaults", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val newConfig = ModelConfig(
                                temperature = if (tempOverride) temperature else null,
                                topP = if (topPOverride) topP else null,
                                topK = topK.trim().toIntOrNull(),
                                maxCompletionTokens = maxCompletionTokens.trim().toIntOrNull(),
                                maxTokens = maxTokens.trim().toIntOrNull(),
                                reasoningEffort = reasoningEffort.trim().ifBlank { null },
                                stop = stop.trim().ifBlank { null },
                                stream = stream,
                                frequencyPenalty = if (freqPenaltyOverride) frequencyPenalty else null,
                                presencePenalty = if (presPenaltyOverride) presencePenalty else null,
                                seed = seed.trim().toLongOrNull(),
                                responseFormat = responseFormat.trim().ifBlank { null },
                                logprobs = if (logprobs) true else null,
                                topLogprobs = topLogprobs.trim().toIntOrNull(),
                                logitBias = logitBias.trim().ifBlank { null },
                                n = n.trim().toIntOrNull(),

                                geminiTemperature = if (geminiTempOverride) geminiTemperature else null,
                                geminiTopP = if (geminiTopPOverride) geminiTopP else null,
                                geminiTopK = geminiTopK.trim().toIntOrNull(),
                                geminiMaxOutputTokens = geminiMaxOutputTokens.trim().toIntOrNull(),
                                geminiStopSequences = geminiStopSequences.trim().ifBlank { null },
                                geminiFrequencyPenalty = if (geminiFreqPenaltyOverride) geminiFrequencyPenalty else null,
                                geminiPresencePenalty = if (geminiPresPenaltyOverride) geminiPresencePenalty else null,
                                geminiSeed = geminiSeed.trim().toLongOrNull(),
                                geminiCandidateCount = geminiCandidateCount.trim().toIntOrNull(),
                                geminiResponseMimeType = geminiResponseMimeType.trim().ifBlank { null },
                                geminiResponseSchema = geminiResponseSchema.trim().ifBlank { null },
                                geminiThinkingLevel = geminiThinkingLevel.trim().ifBlank { null },
                                geminiThinkingBudget = geminiThinkingBudget.trim().toIntOrNull(),

                                minP = if (minPOverride) minP else null,
                                repetitionPenalty = if (repPenaltyOverride) repetitionPenalty else null,
                                typicalP = if (typicalPOverride) typicalP else null,
                                tfsZ = if (tfsZOverride) tfsZ else null,
                                mirostat = mirostat.trim().toIntOrNull(),
                                mirostatTau = mirostatTau.trim().toFloatOrNull(),
                                mirostatEta = mirostatEta.trim().toFloatOrNull(),
                                repeatLastN = repeatLastN.trim().toIntOrNull(),
                                numCtx = numCtx.trim().toIntOrNull()
                            )
                            onSaveConfig(newConfig)
                            Toast.makeText(context, "Model configuration saved", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Config")
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelConfigSliderItem(
    label: String,
    description: String,
    isOverridden: Boolean,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    formatPattern: String = "%.2f",
    onToggleOverride: (Boolean) -> Unit,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
            .border(
                1.dp,
                if (isOverridden) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isOverridden) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = if (isOverridden) String.format(Locale.US, formatPattern, value) else "Default",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isOverridden) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                Switch(
                    checked = isOverridden,
                    onCheckedChange = onToggleOverride
                )
            }
        }

        if (isOverridden) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ModelConfigTextFieldItem(
    label: String,
    description: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = if (placeholder.isNotBlank()) { { Text(placeholder) } } else null,
            trailingIcon = if (value.isNotBlank()) {
                {
                    IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun ModelConfigSwitchItem(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
            .border(
                1.dp,
                if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// ============================================================================
// 13. Custom API JSON Dialog (.json Upload & JSON Editor)
// ============================================================================

@Composable
private fun CustomApiJsonDialog(
    currentJson: String,
    onDismiss: () -> Unit,
    onSaveJson: (String) -> Unit
) {
    val context = LocalContext.current
    val cleanInitial = remember(currentJson) { currentJson.removePrefix("\uFEFF").trim() }
    var jsonText by remember { mutableStateOf(cleanInitial) }
    val editTextRef = remember { mutableStateOf<EditText?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                    readInputStreamTextUniversal(stream)
                } ?: ""
                val cleaned = text.removePrefix("\uFEFF").trim()
                if (cleaned.isNotBlank()) {
                    jsonText = cleaned
                    editTextRef.value?.setText(cleaned)
                    editTextRef.value?.setSelection(cleaned.length)
                    val fileName = ZipArchiveSystem.getFileName(context, uri)
                    Toast.makeText(context, "Loaded $fileName (${cleaned.length} chars)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Selected file is empty", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("AiForge", "Error loading JSON file", e)
                Toast.makeText(context, "Failed to read file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Debounced, background-thread JSON validation
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var isValidJson by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(jsonText) {
        val trimmed = jsonText.trim().removePrefix("\uFEFF").trim()
        if (trimmed.isBlank()) {
            validationMessage = null
            isValidJson = null
        } else {
            delay(250) // Debounce so typing/scrolling remains 120 FPS
            val result = withContext(Dispatchers.Default) {
                try {
                    if (trimmed.startsWith("{")) {
                        JSONObject(trimmed)
                        Result.success("Valid JSON Object")
                    } else if (trimmed.startsWith("[")) {
                        JSONArray(trimmed)
                        Result.success("Valid JSON Array")
                    } else {
                        Result.failure(IllegalArgumentException("Must start with { or [ and end with } or ]"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            if (result.isSuccess) {
                validationMessage = result.getOrNull()
                isValidJson = true
            } else {
                validationMessage = result.exceptionOrNull()?.message ?: "Invalid JSON syntax"
                isValidJson = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header: Icon + Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Custom API JSON",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Inject custom fields into LLM request payloads",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Action Bar: Upload .json File, Paste, Format / Sample, Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            try {
                                filePickerLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            } catch (e: Exception) {
                                try {
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Upload .json",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            val clip = try {
                                clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
                            } catch (_: Exception) { "" }
                            val cleaned = clip.removePrefix("\uFEFF").trim()
                            if (cleaned.isNotBlank()) {
                                jsonText = cleaned
                                editTextRef.value?.setText(cleaned)
                                editTextRef.value?.setSelection(cleaned.length)
                                Toast.makeText(context, "Pasted ${cleaned.length} chars from clipboard", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Paste", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            val trimmed = jsonText.trim().removePrefix("\uFEFF").trim()
                            if (trimmed.isNotBlank()) {
                                try {
                                    val formatted = if (trimmed.startsWith("{")) {
                                        JSONObject(trimmed).toString(2)
                                    } else if (trimmed.startsWith("[")) {
                                        JSONArray(trimmed).toString(2)
                                    } else {
                                        throw IllegalArgumentException("Must start with { or [")
                                    }
                                    jsonText = formatted
                                    editTextRef.value?.setText(formatted)
                                    editTextRef.value?.setSelection(formatted.length)
                                    Toast.makeText(context, "Formatted JSON", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Invalid JSON: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val sample = "{\n  \"transforms\": [\"middle-out\"],\n  \"route\": \"fallback\"\n}"
                                jsonText = sample
                                editTextRef.value?.setText(sample)
                                editTextRef.value?.setSelection(sample.length)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (jsonText.isBlank()) "Sample" else "Format", style = MaterialTheme.typography.labelMedium)
                    }

                    if (jsonText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                jsonText = ""
                                editTextRef.value?.setText("")
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear JSON",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // High-performance native AndroidView EditText (prevents Compose IME buffer chunk corruption with 10k+ chars)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
                        .border(
                            1.dp,
                            if (isValidJson == false) MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
                    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

                    AndroidView(
                        factory = { ctx ->
                            EditText(ctx).apply {
                                editTextRef.value = this
                                typeface = Typeface.MONOSPACE
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                                setTextColor(textColor)
                                setHintTextColor(hintColor)
                                hint = "{\n  \"safetySettings\": [...],\n  \"route\": \"fallback\"\n}"
                                gravity = Gravity.TOP or Gravity.START
                                background = null
                                setPadding(28, 28, 28, 28)
                                isVerticalScrollBarEnabled = true
                                scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
                                movementMethod = ScrollingMovementMethod.getInstance()
                                setText(jsonText)
                                setSelection(jsonText.length)

                                addTextChangedListener(object : TextWatcher {
                                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                    override fun afterTextChanged(s: Editable?) {
                                        val newStr = s?.toString() ?: ""
                                        if (newStr != jsonText) {
                                            jsonText = newStr
                                        }
                                    }
                                })
                            }
                        },
                        update = { editText ->
                            editText.setTextColor(textColor)
                            editText.setHintTextColor(hintColor)
                            if (editText.text.toString() != jsonText) {
                                editText.setText(jsonText)
                                editText.setSelection(editText.text.length)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Info Footer: Character Count & Debounced Validation status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        jsonText.isBlank() -> {
                            Text(
                                text = "Default API request payload will be used",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        isValidJson == true -> {
                            Text(
                                text = "✓ ${validationMessage ?: "Valid JSON"} (${jsonText.length} chars)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        isValidJson == false -> {
                            Text(
                                text = "⚠ Syntax error: ${validationMessage ?: "Invalid JSON"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        else -> {
                            Text(
                                text = "Validating syntax (${jsonText.length} chars)...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (cleanInitial.isNotBlank() && jsonText != cleanInitial) {
                        Text(
                            text = "Modified",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Bottom Action Buttons: Reset to Default, Cancel, Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (cleanInitial.isNotBlank()) {
                        TextButton(
                            onClick = {
                                onSaveJson("")
                                Toast.makeText(context, "Cleared custom API JSON", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Reset Default", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val trimmed = jsonText.trim().removePrefix("\uFEFF").trim()
                            if (trimmed.isNotBlank()) {
                                try {
                                    if (trimmed.startsWith("{")) {
                                        JSONObject(trimmed)
                                    } else if (trimmed.startsWith("[")) {
                                        JSONArray(trimmed)
                                    } else {
                                        throw IllegalArgumentException("JSON must start with { or [")
                                    }
                                    onSaveJson(trimmed)
                                    Toast.makeText(context, "Custom API JSON saved (${trimmed.length} chars)", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot save invalid JSON: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                onSaveJson("")
                                Toast.makeText(context, "Using default API payload", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
