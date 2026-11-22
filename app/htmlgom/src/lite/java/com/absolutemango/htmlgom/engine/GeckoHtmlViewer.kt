package com.absolutemango.htmlgom.engine

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun GeckoHtmlViewer(
    fileUri: Uri,
    isDomEnabled: Boolean,
    isHwAccelEnabled: Boolean,
    onProgressChange: (progress: Float, isLoading: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // No-op in Lite edition as GeckoView is stripped from runtime
}
