package com.absolutemango.htmlgom.engine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
    // No-op in Lite edition as GeckoView is stripped from runtime
}
