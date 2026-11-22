package com.absolutemango.htmlgom

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.absolutemango.htmlgom.engine.BrowserSessionActions
import com.absolutemango.htmlgom.engine.EngineCapabilities
import com.absolutemango.htmlgom.engine.GeckoBrowserView
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.OpticalSizeValue
import kotlinx.coroutines.delay
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

data class WebTabItem(
    val id: String,
    var title: MutableState<String>,
    var url: MutableState<String>,
    var webView: WebView? = null
)

private fun normalizeWebUrl(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file://")) {
        return trimmed
    }
    if (trimmed.contains(".") && !trimmed.contains(" ")) {
        return "https://$trimmed"
    }
    return "https://www.google.com/search?q=${Uri.encode(trimmed)}"
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyBrowserScreen(
    onBackClick: () -> Unit,
    backdrop: Backdrop = rememberLayerBackdrop(),
    useGeckoEngine: Boolean = true
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val hazeState = remember { HazeState() }

    val initialEngine = if (EngineCapabilities.IS_GECKO_AVAILABLE && useGeckoEngine) "Mozilla GeckoView" else "System WebView"
    var selectedEngine by remember { mutableStateOf(initialEngine) }
    var isAddressBarCollapsed by remember { mutableStateOf(false) }
    var isDockedToEdge by remember { mutableStateOf(false) }

    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val glassStyle = remember(surfaceContainerHigh) {
        GlassStyle.regular.then {
            shape(CircleShape)
            tint(surfaceContainerHigh.copy(alpha = 0.25f))
            edgeShadow(Color.Transparent)
            optics {
                this.copy(
                    refractionStrength = (this.refractionStrength * 1.10f).coerceIn(0f, 1f),
                    blurRadius = OpticalSizeValue.Fixed(1.dp)
                )
            }
        }
    }

    val collapsedGlassStyle = remember(surfaceContainerHigh, isDockedToEdge) {
        GlassStyle.regular.then {
            shape(if (isDockedToEdge) RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 24.dp, bottomEnd = 24.dp) else CircleShape)
            tint(surfaceContainerHigh.copy(alpha = 0.25f))
            edgeShadow(Color.Transparent)
            optics {
                this.copy(
                    refractionStrength = (this.refractionStrength * 1.10f).coerceIn(0f, 1f),
                    blurRadius = OpticalSizeValue.Fixed(1.dp)
                )
            }
        }
    }

    val tabs = remember {
        mutableStateListOf(
            WebTabItem(
                id = "tab_${System.currentTimeMillis()}",
                title = mutableStateOf("New Tab"),
                url = mutableStateOf("")
            )
        )
    }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    val activeTab = remember(activeTabId, tabs.size) {
        tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()
    }

    LaunchedEffect(isAddressBarCollapsed, activeTab?.url?.value) {
        if (isAddressBarCollapsed && activeTab?.url?.value?.isNotEmpty() == true) {
            isDockedToEdge = false
            delay(3000L)
            isDockedToEdge = true
        } else {
            isDockedToEdge = false
        }
    }

    val collapsedStartPadding by animateDpAsState(
        targetValue = if (isDockedToEdge) 0.dp else 16.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "CollapsedPillEdgeDock"
    )

    val collapsedShape = if (isDockedToEdge) {
        RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 24.dp, bottomEnd = 24.dp)
    } else {
        CircleShape
    }

    var urlInputField by remember { mutableStateOf(activeTab?.url?.value ?: "") }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showTabsManagerSheet by remember { mutableStateOf(false) }

    var webViewFileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

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

    var pendingWebPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingGeoOrigin by remember { mutableStateOf<String?>(null) }
    var pendingGeoCallback by remember { mutableStateOf<GeolocationPermissions.Callback?>(null) }

    val webPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        pendingWebPermissionRequest?.let { webReq ->
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

    LaunchedEffect(activeTab?.url?.value) {
        urlInputField = activeTab?.url?.value ?: ""
        if (urlInputField.isNotEmpty()) {
            isAddressBarCollapsed = true
        }
    }

    var activeGeckoActions by remember(activeTabId) { mutableStateOf<BrowserSessionActions?>(null) }
    var canGoBackGecko by remember(activeTabId) { mutableStateOf(false) }
    var canGoForwardGecko by remember(activeTabId) { mutableStateOf(false) }

    val canGoBack = if (selectedEngine == "Mozilla GeckoView") canGoBackGecko else (activeTab?.webView?.canGoBack() == true)
    val canGoForward = if (selectedEngine == "Mozilla GeckoView") canGoForwardGecko else (activeTab?.webView?.canGoForward() == true)

    val canInternalBack = showTabsManagerSheet || showMenuSheet || canGoBack

    BackHandler(enabled = canInternalBack) {
        when {
            showTabsManagerSheet -> showTabsManagerSheet = false
            showMenuSheet -> showMenuSheet = false
            canGoBack -> {
                if (selectedEngine == "Mozilla GeckoView") {
                    activeGeckoActions?.goBack()
                } else {
                    activeTab?.webView?.goBack()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        // --- LAYER 1: FULLSCREEN WEBVIEW OR DEFAULT START PAGE ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .hazeSource(hazeState)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            )
            if (activeTab != null) {
                val currentUrl = activeTab.url.value
                var lastLoadedUrl by remember(activeTabId, selectedEngine) { mutableStateOf("") }
                var webProgress by remember(activeTabId, selectedEngine) { mutableFloatStateOf(0f) }
                var isWebLoading by remember(activeTabId, selectedEngine) { mutableStateOf(false) }

                if (currentUrl.isEmpty()) {
                    // DEFAULT STARTING PAGE
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Box(modifier = Modifier.padding(24.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = "Globe",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }

                        Text(
                            text = "Emergency Browser",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Please type an address to get a result.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(24.dp))

                        // Quick Shortcut Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val target = "https://google.com"
                                    activeTab.url.value = target
                                    activeTab.title.value = "Google"
                                    urlInputField = target
                                    isAddressBarCollapsed = true
                                },
                                label = { Text("Google", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val target = "https://youtube.com"
                                    activeTab.url.value = target
                                    activeTab.title.value = "YouTube"
                                    urlInputField = target
                                    isAddressBarCollapsed = true
                                },
                                label = { Text("YouTube", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val target = "https://wikipedia.org"
                                    activeTab.url.value = target
                                    activeTab.title.value = "Wikipedia"
                                    urlInputField = target
                                    isAddressBarCollapsed = true
                                },
                                label = { Text("Wikipedia", fontSize = 12.sp) }
                            )
                        }
                    }
                } else {
                    // DYNAMIC WEB ENGINE: GECKOVIEW VS SYSTEM WEBVIEW
                    if (selectedEngine == "Mozilla GeckoView") {
                        GeckoBrowserView(
                            currentUrl = currentUrl,
                            activeTabId = activeTabId,
                            domStorageEnabled = true,
                            onProgressChange = { p, loading ->
                                webProgress = p
                                isWebLoading = loading
                            },
                            onCanGoBackChange = { canGoBackGecko = it },
                            onCanGoForwardChange = { canGoForwardGecko = it },
                            onSessionActionsCreated = { activeGeckoActions = it },
                            onTitleChange = { title ->
                                if (title.isNotEmpty() && title != "about:blank") {
                                    activeTab.title.value = title
                                }
                            },
                            onUrlChange = { url ->
                                if (url.isNotEmpty() && url != "about:blank") {
                                    activeTab.url.value = url
                                    urlInputField = url
                                }
                            },
                            onError = { error ->
                                Toast.makeText(
                                    context,
                                    "GeckoView engine error: ${error.localizedMessage ?: "Engine failure"}. Switched to System WebView.",
                                    Toast.LENGTH_LONG
                                ).show()
                                selectedEngine = "System WebView"
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // SYSTEM WEBVIEW
                        LaunchedEffect(currentUrl, activeTabId) {
                            if (currentUrl.isNotEmpty() && currentUrl != lastLoadedUrl) {
                                lastLoadedUrl = currentUrl
                                activeTab.webView?.loadUrl(currentUrl)
                            }
                        }

                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
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
                                            if (!url.isNullOrEmpty() && url != lastLoadedUrl) {
                                                lastLoadedUrl = url
                                                activeTab.url.value = url
                                                urlInputField = url
                                            }
                                            view?.title?.let {
                                                if (it.isNotEmpty()) activeTab.title.value = it
                                            }
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
                                            webProgress = newProgress / 100f
                                            if (newProgress >= 100) {
                                                isWebLoading = false
                                            } else if (newProgress > 0) {
                                                isWebLoading = true
                                            }
                                        }

                                        override fun onReceivedTitle(view: WebView?, title: String?) {
                                            super.onReceivedTitle(view, title)
                                            title?.let {
                                                if (it.isNotEmpty()) activeTab.title.value = it
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
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        allowFileAccess = true
                                        allowContentAccess = true
                                        allowFileAccessFromFileURLs = true
                                        allowUniversalAccessFromFileURLs = true
                                        mediaPlaybackRequiresUserGesture = false
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        cacheMode = WebSettings.LOAD_DEFAULT

                                        val defaultUa = userAgentString
                                        userAgentString = defaultUa
                                            .replace("; wv", "")
                                            .replace(";wv", "")
                                            .replace("Version/4.0 ", "")
                                    }
                                    activeTab.webView = this
                                    if (currentUrl.isNotEmpty()) {
                                        lastLoadedUrl = currentUrl
                                        loadUrl(currentUrl)
                                    }
                                }
                            },
                            update = { wv ->
                                activeTab.webView = wv
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // --- LAYER 2: COLLAPSED SIDE PILL (Sits at Bottom-Left, docks to edge if untouched) ---
        AnimatedVisibility(
            visible = isAddressBarCollapsed && activeTab?.url?.value?.isNotEmpty() == true,
            enter = fadeIn(tween(300)) + slideInHorizontally(initialOffsetX = { -it }),
            exit = fadeOut(tween(200)) + slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = collapsedStartPadding, bottom = 16.dp)
        ) {
            Surface(
                shape = collapsedShape,
                color = surfaceContainerHigh.copy(alpha = 0.20f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .hazeGlass(
                        input = HazeInput.Sources(hazeState),
                        style = collapsedGlassStyle
                    )
                    .clickable {
                        isAddressBarCollapsed = false
                        isDockedToEdge = false
                    }
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = if (isDockedToEdge) 12.dp else 14.dp,
                        end = if (isDockedToEdge) 10.dp else 14.dp,
                        top = 10.dp,
                        bottom = 10.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Language,
                        contentDescription = "Expand Address Bar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    AnimatedVisibility(visible = !isDockedToEdge) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = activeTab?.title?.value?.ifEmpty { activeTab.url.value } ?: "Web Page",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 160.dp)
                            )
                            Icon(
                                Icons.Default.UnfoldMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (isDockedToEdge) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Expand Address Bar",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // --- LAYER 3: FULL FLOATING HAZE LIQUID ADDRESS BAR & HAMBURGER BAR ---
        AnimatedVisibility(
            visible = !isAddressBarCollapsed || activeTab?.url?.value?.isEmpty() == true,
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. HAMBURGER LIQUID GLASS PILL
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .hazeGlass(
                            input = HazeInput.Sources(hazeState),
                            style = glassStyle
                        ),
                    shape = CircleShape,
                    color = surfaceContainerHigh.copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    shadowElevation = 0.dp
                ) {
                    IconButton(
                        onClick = { showMenuSheet = true },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 2. ADDRESS BAR LIQUID GLASS PILL
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .hazeGlass(
                            input = HazeInput.Sources(hazeState),
                            style = glassStyle
                        ),
                    shape = CircleShape,
                    color = surfaceContainerHigh.copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )

                        OutlinedTextField(
                            value = urlInputField,
                            onValueChange = { urlInputField = it },
                            placeholder = { Text("Search or type URL...", fontSize = 13.sp) },
                            singleLine = true,
                            shape = CircleShape,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )

                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                val targetUrl = normalizeWebUrl(urlInputField)
                                if (targetUrl.isNotEmpty() && activeTab != null) {
                                    activeTab.url.value = targetUrl
                                    activeTab.title.value = targetUrl
                                    isAddressBarCollapsed = true
                                    isDockedToEdge = false
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Go",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // --- LAYER 4: HAMBURGER MENU BOTTOM SHEET ---
        if (showMenuSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMenuSheet = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Browser Actions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Back Action
                    ListItem(
                        headlineContent = { Text("Go Back") },
                        leadingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go Back",
                                tint = if (canGoBack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        },
                        modifier = Modifier.clickable(enabled = canGoBack) {
                            showMenuSheet = false
                            if (selectedEngine == "Mozilla GeckoView") activeGeckoActions?.goBack() else activeTab?.webView?.goBack()
                        }
                    )

                    // Forward Action
                    ListItem(
                        headlineContent = { Text("Go Forward") },
                        leadingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Go Forward",
                                tint = if (canGoForward) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        },
                        modifier = Modifier.clickable(enabled = canGoForward) {
                            showMenuSheet = false
                            if (selectedEngine == "Mozilla GeckoView") activeGeckoActions?.goForward() else activeTab?.webView?.goForward()
                        }
                    )

                    // Reload Action
                    ListItem(
                        headlineContent = { Text("Reload Page") },
                        leadingContent = { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            showMenuSheet = false
                            if (selectedEngine == "Mozilla GeckoView") activeGeckoActions?.reload() else activeTab?.webView?.reload()
                        }
                    )

                    // Tabs Manager
                    ListItem(
                        headlineContent = { Text("Tabs (${tabs.size})") },
                        leadingContent = { Icon(Icons.Default.Tab, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            showMenuSheet = false
                            showTabsManagerSheet = true
                        }
                    )

                    // Home Page Reset
                    ListItem(
                        headlineContent = { Text("Start Page") },
                        leadingContent = { Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            showMenuSheet = false
                            activeTab?.url?.value = ""
                            urlInputField = ""
                            isAddressBarCollapsed = false
                        }
                    )

                    // Engine Switcher (when GeckoView is available)
                    if (EngineCapabilities.IS_GECKO_AVAILABLE) {
                        ListItem(
                            headlineContent = { Text("Web Engine") },
                            supportingContent = { Text(selectedEngine) },
                            leadingContent = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = {
                                Switch(
                                    checked = selectedEngine == "Mozilla GeckoView",
                                    onCheckedChange = { isGecko ->
                                        selectedEngine = if (isGecko) "Mozilla GeckoView" else "System WebView"
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                selectedEngine = if (selectedEngine == "Mozilla GeckoView") "System WebView" else "Mozilla GeckoView"
                            }
                        )
                    }

                    // Copy Link
                    ListItem(
                        headlineContent = { Text("Copy Link") },
                        leadingContent = { Icon(Icons.Default.ContentCopy, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            showMenuSheet = false
                            val currentUrl = activeTab?.url?.value ?: ""
                            if (currentUrl.isNotEmpty()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = android.content.ClipData.newPlainText("URL", currentUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No URL to copy", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // Close Emergency Browser
                    ListItem(
                        headlineContent = { Text("Close Emergency Browser", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            showMenuSheet = false
                            onBackClick()
                        }
                    )
                }
            }
        }

        // --- LAYER 5: TABS MANAGER BOTTOM SHEET ---
        if (showTabsManagerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTabsManagerSheet = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tabs Manager (${tabs.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                val newTab = WebTabItem(
                                    id = "tab_${System.currentTimeMillis()}",
                                    title = mutableStateOf("New Tab"),
                                    url = mutableStateOf("")
                                )
                                tabs.add(newTab)
                                activeTabId = newTab.id
                                showTabsManagerSheet = false
                                isAddressBarCollapsed = false
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Tab", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                    ) {
                        items(tabs, key = { it.id }) { tab ->
                            val isSelected = tab.id == activeTabId
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.50f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeTabId = tab.id
                                        showTabsManagerSheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tab.title.value.ifEmpty { "New Tab" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = tab.url.value.ifEmpty { "Start Page" },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (tabs.size > 1) {
                                        IconButton(
                                            onClick = {
                                                tabs.remove(tab)
                                                if (isSelected) {
                                                    activeTabId = tabs.first().id
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Close Tab", modifier = Modifier.size(18.dp))
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
