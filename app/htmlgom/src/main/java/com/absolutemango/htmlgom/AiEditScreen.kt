package com.absolutemango.htmlgom

import androidx.compose.runtime.Composable
import com.kyant.backdrop.Backdrop

/**
 * Legacy standalone AI Edit screen.
 * Replaced by the unified ExpressiveAiForgeScreen with just-in-time code extraction and active file context binding.
 */
@Deprecated("Use ExpressiveAiForgeScreen with initialEditUri and initialEditFileName")
@Composable
fun AiEditScreen(
    fileItem: LastOpenedFileItem,
    onBackClick: () -> Unit,
    onRunGeneratedHtml: (String) -> Unit,
    backdrop: Backdrop
) {
    // Deprecated no-op stub: all invocations are routed directly to ExpressiveAiForgeScreen
}
