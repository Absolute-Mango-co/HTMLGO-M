package com.absolutemango.htmlgom.engine

/**
 * Common action interface for web engine sessions (GeckoView / WebView)
 * to perform browser navigation actions without coupling caller to engine implementations.
 */
interface BrowserSessionActions {
    fun goBack()
    fun goForward()
    fun reload()
}
