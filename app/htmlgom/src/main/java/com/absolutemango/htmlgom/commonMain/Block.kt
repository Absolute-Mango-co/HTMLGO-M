package com.absolutemango.htmlgom.commonMain

import androidx.compose.runtime.Composable

@Composable
fun Block(content: @Composable () -> Unit) {
    content()
}

