package com.absolutemango.htmlgom.commonMain

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun BackdropDemoScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(backdrop: LayerBackdrop) -> Unit
) {
    val backdrop = rememberLayerBackdrop()
    Box(modifier = modifier) {
        content(backdrop)
    }
}

