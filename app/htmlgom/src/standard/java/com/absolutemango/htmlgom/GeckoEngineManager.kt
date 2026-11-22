package com.absolutemango.htmlgom

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

object GeckoEngineManager {
    @Volatile
    private var runtime: GeckoRuntime? = null

    @Synchronized
    fun getOrCreateRuntime(context: Context): GeckoRuntime {
        runtime?.let { return it }
        val newRuntime = createStrippedRuntime(context.applicationContext)
        runtime = newRuntime
        return newRuntime
    }

    private fun createStrippedRuntime(context: Context): GeckoRuntime {
        val settings = GeckoRuntimeSettings.Builder()
            .consoleOutput(false)
            .aboutConfigEnabled(false)
            .build()

        return GeckoRuntime.create(context, settings)
    }

    fun createOptimizedSession(domStorageEnabled: Boolean = true): GeckoSession {
        val cleanUserAgent = "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        val sessionSettings = GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .allowJavascript(domStorageEnabled)
            .useTrackingProtection(false)
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .userAgentOverride(cleanUserAgent)
            .displayMode(GeckoSessionSettings.DISPLAY_MODE_STANDALONE)
            .build()

        return GeckoSession(sessionSettings)
    }
}
