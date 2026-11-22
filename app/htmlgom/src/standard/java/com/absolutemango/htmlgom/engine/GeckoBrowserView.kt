package com.absolutemango.htmlgom.engine

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.absolutemango.htmlgom.GeckoEngineManager
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

@Composable
fun GeckoBrowserView(
    currentUrl: String,
    activeTabId: String,
    domStorageEnabled: Boolean,
    onProgressChange: (progress: Float, isLoading: Boolean) -> Unit,
    onCanGoBackChange: (Boolean) -> Unit,
    onCanGoForwardChange: (Boolean) -> Unit,
    onSessionActionsCreated: (BrowserSessionActions?) -> Unit,
    onTitleChange: ((String) -> Unit)? = null,
    onUrlChange: ((String) -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var geckoFilePrompt by remember { mutableStateOf<GeckoSession.PromptDelegate.FilePrompt?>(null) }
    var geckoResultCallback by remember { mutableStateOf<GeckoResult<GeckoSession.PromptDelegate.PromptResponse>?>(null) }

    var pendingGeckoMediaCallback by remember { mutableStateOf<GeckoSession.PermissionDelegate.MediaCallback?>(null) }
    var pendingGeckoVideoSource by remember { mutableStateOf<GeckoSession.PermissionDelegate.MediaSource?>(null) }
    var pendingGeckoAudioSource by remember { mutableStateOf<GeckoSession.PermissionDelegate.MediaSource?>(null) }
    var pendingGeckoAndroidCallback by remember { mutableStateOf<GeckoSession.PermissionDelegate.Callback?>(null) }
    var pendingGeckoContentResult by remember { mutableStateOf<GeckoResult<Int>?>(null) }

    val webPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val geckoMediaCb = pendingGeckoMediaCallback
        if (geckoMediaCb != null) {
            val micOk = pendingGeckoAudioSource == null ||
                permissionsMap[Manifest.permission.RECORD_AUDIO] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            val camOk = pendingGeckoVideoSource == null ||
                permissionsMap[Manifest.permission.CAMERA] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

            if (micOk && camOk) {
                geckoMediaCb.grant(pendingGeckoVideoSource, pendingGeckoAudioSource)
            } else {
                geckoMediaCb.reject()
            }
            pendingGeckoMediaCallback = null
            pendingGeckoVideoSource = null
            pendingGeckoAudioSource = null
        }

        val geckoAndroidCb = pendingGeckoAndroidCallback
        if (geckoAndroidCb != null) {
            val allGranted = permissionsMap.values.all { it }
            if (allGranted) geckoAndroidCb.grant() else geckoAndroidCb.reject()
            pendingGeckoAndroidCallback = null
        }

        val geckoContentRes = pendingGeckoContentResult
        if (geckoContentRes != null) {
            val fine = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarse = permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (fine || coarse) {
                geckoContentRes.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
            } else {
                geckoContentRes.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
            }
            pendingGeckoContentResult = null
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

        val prompt = geckoFilePrompt
        val geckoResult = geckoResultCallback
        geckoFilePrompt = null
        geckoResultCallback = null
        if (prompt != null && geckoResult != null) {
            if (uris != null && uris.isNotEmpty()) {
                val response = if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE) {
                    prompt.confirm(context, uris)
                } else {
                    prompt.confirm(context, uris[0])
                }
                geckoResult.complete(response)
            } else {
                geckoResult.complete(prompt.dismiss())
            }
        }
    }

    val runtime = remember {
        try {
            GeckoEngineManager.getOrCreateRuntime(context)
        } catch (t: Throwable) {
            onError?.invoke(t)
            null
        }
    }
    val session = remember(activeTabId) {
        try {
            GeckoEngineManager.createOptimizedSession(domStorageEnabled = domStorageEnabled)
        } catch (t: Throwable) {
            onError?.invoke(t)
            null
        }
    }

    var lastLoadedUri by remember(activeTabId) { mutableStateOf<String?>(null) }

    DisposableEffect(session, runtime) {
        val currentSession = session
        val currentRuntime = runtime
        if (currentSession != null && currentRuntime != null) {
            try {
                if (!currentSession.isOpen) {
                    currentSession.open(currentRuntime)
                }
            } catch (t: Throwable) {
                onError?.invoke(t)
            }

            val actions = object : BrowserSessionActions {
                override fun goBack() {
                    try { currentSession.goBack() } catch (_: Exception) {}
                }
                override fun goForward() {
                    try { currentSession.goForward() } catch (_: Exception) {}
                }
                override fun reload() {
                    try { currentSession.reload() } catch (_: Exception) {}
                }
            }
            onSessionActionsCreated(actions)

            currentSession.navigationDelegate = object : GeckoSession.NavigationDelegate {
                override fun onCanGoBack(s: GeckoSession, canGoBack: Boolean) {
                    onCanGoBackChange(canGoBack)
                }
                override fun onCanGoForward(s: GeckoSession, canGoForward: Boolean) {
                    onCanGoForwardChange(canGoForward)
                }
                override fun onLocationChange(
                    s: GeckoSession,
                    url: String?,
                    perms: List<GeckoSession.PermissionDelegate.ContentPermission>
                ) {
                    if (!url.isNullOrEmpty() && url != "about:blank") {
                        lastLoadedUri = url
                        onUrlChange?.invoke(url)
                    }
                }
            }

            currentSession.contentDelegate = object : GeckoSession.ContentDelegate {
                override fun onTitleChange(s: GeckoSession, title: String?) {
                    if (!title.isNullOrEmpty() && title != "about:blank") {
                        onTitleChange?.invoke(title)
                    }
                }
            }

            currentSession.progressDelegate = object : GeckoSession.ProgressDelegate {
                override fun onPageStart(session: GeckoSession, url: String) {
                    if (url != "about:blank") {
                        onProgressChange(0.05f, true)
                    }
                }
                override fun onPageStop(session: GeckoSession, success: Boolean) {
                    onProgressChange(if (success) 1.0f else 0.0f, false)
                }
                override fun onProgressChange(session: GeckoSession, progress: Int) {
                    if (progress >= 100) {
                        onProgressChange(1.0f, false)
                    } else {
                        onProgressChange((progress / 100f).coerceIn(0.05f, 0.99f), true)
                    }
                }
            }

            currentSession.promptDelegate = object : GeckoSession.PromptDelegate {
                override fun onFilePrompt(
                    session: GeckoSession,
                    prompt: GeckoSession.PromptDelegate.FilePrompt
                ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                    val geckoResult = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                    geckoFilePrompt = prompt
                    geckoResultCallback = geckoResult

                    val mimeTypes = prompt.mimeTypes?.filter { !it.isNullOrBlank() } ?: emptyList()
                    val isMultiple = prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE

                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = if (mimeTypes.size == 1) mimeTypes[0] else "*/*"
                        if (mimeTypes.size > 1) {
                            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
                        }
                        if (isMultiple) {
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        }
                    }

                    try {
                        filePickerLauncher.launch(intent)
                    } catch (_: Exception) {
                        geckoResult.complete(prompt.dismiss())
                        geckoFilePrompt = null
                        geckoResultCallback = null
                    }
                    return geckoResult
                }
            }

            currentSession.permissionDelegate = object : GeckoSession.PermissionDelegate {
                override fun onMediaPermissionRequest(
                    session: GeckoSession,
                    uri: String,
                    video: Array<GeckoSession.PermissionDelegate.MediaSource>?,
                    audio: Array<GeckoSession.PermissionDelegate.MediaSource>?,
                    callback: GeckoSession.PermissionDelegate.MediaCallback
                ) {
                    val needed = mutableListOf<String>()
                    if (!audio.isNullOrEmpty()) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            needed.add(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                    if (!video.isNullOrEmpty()) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            needed.add(Manifest.permission.CAMERA)
                        }
                    }

                    if (needed.isEmpty()) {
                        callback.grant(video?.firstOrNull(), audio?.firstOrNull())
                    } else {
                        pendingGeckoMediaCallback = callback
                        pendingGeckoVideoSource = video?.firstOrNull()
                        pendingGeckoAudioSource = audio?.firstOrNull()
                        webPermissionLauncher.launch(needed.toTypedArray())
                    }
                }

                override fun onAndroidPermissionsRequest(
                    session: GeckoSession,
                    permissions: Array<out String>?,
                    callback: GeckoSession.PermissionDelegate.Callback
                ) {
                    val ungranted = permissions?.filter {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                    } ?: emptyList()
                    if (ungranted.isEmpty()) {
                        callback.grant()
                    } else {
                        pendingGeckoAndroidCallback = callback
                        webPermissionLauncher.launch(ungranted.toTypedArray())
                    }
                }

                override fun onContentPermissionRequest(
                    session: GeckoSession,
                    perm: GeckoSession.PermissionDelegate.ContentPermission
                ): GeckoResult<Int> {
                    if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION) {
                        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (fine || coarse) {
                            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                        }
                        val result = GeckoResult<Int>()
                        pendingGeckoContentResult = result
                        webPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        return result
                    }
                    return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                }
            }
        }

        onDispose {
            onSessionActionsCreated(null)
            session?.let { s ->
                s.navigationDelegate = null
                s.contentDelegate = null
                s.progressDelegate = null
                s.promptDelegate = null
                s.permissionDelegate = null
                try {
                    if (s.isOpen) {
                        s.close()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(currentUrl, session) {
        val currentSession = session
        if (currentSession != null && currentUrl.isNotEmpty() && currentUrl != "about:blank" && currentUrl != lastLoadedUri) {
            lastLoadedUri = currentUrl
            try {
                if (runtime != null && !currentSession.isOpen) {
                    currentSession.open(runtime)
                }
                currentSession.loadUri(currentUrl)
            } catch (t: Throwable) {
                onError?.invoke(t)
            }
        }
    }

    if (session != null) {
        AndroidView(
            factory = { ctx ->
                GeckoView(ctx).apply {
                    setViewBackend(GeckoView.BACKEND_TEXTURE_VIEW)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    try {
                        setSession(session)
                        coverUntilFirstPaint(android.graphics.Color.BLACK)
                        session.compositorController?.setClearColor(android.graphics.Color.BLACK)
                    } catch (t: Throwable) {
                        onError?.invoke(t)
                    }
                }
            },
            onRelease = { geckoView ->
                try {
                    geckoView.releaseSession()
                } catch (_: Exception) {}
            },
            modifier = modifier.fillMaxSize()
        )
    }
}
