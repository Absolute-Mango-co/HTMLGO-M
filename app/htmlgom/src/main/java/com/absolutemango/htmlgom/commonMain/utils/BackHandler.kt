package com.absolutemango.htmlgom.commonMain.utils

import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler as ComposeBackHandler

@Composable
fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
) {
    ComposeBackHandler(enabled, onBack)
}

