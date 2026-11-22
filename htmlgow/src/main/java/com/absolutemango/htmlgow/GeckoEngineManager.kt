package com.absolutemango.htmlgow

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

object GeckoEngineManager {
    @Volatile
    private var runtime: GeckoRuntime? = null

    fun getOrCreateRuntime(context: Context): GeckoRuntime {
        return runtime ?: synchronized(this) {
            runtime ?: createLightweightRuntime(context.applicationContext).also {
                runtime = it
            }
        }
    }

    private fun createLightweightRuntime(context: Context): GeckoRuntime {
        val settings = GeckoRuntimeSettings.Builder()
            .consoleOutput(false)
            .arguments(arrayOf("--disable-telemetry", "--disable-crash-reporter", "--disable-breakpad"))
            .build()

        return GeckoRuntime.create(context, settings)
    }

    fun createOptimizedSession(domStorageEnabled: Boolean = true): GeckoSession {
        val sessionSettings = GeckoSessionSettings.Builder()
            .usePrivateMode(!domStorageEnabled)
            .allowJavascript(domStorageEnabled)
            .useTrackingProtection(false)
            .displayMode(GeckoSessionSettings.DISPLAY_MODE_STANDALONE)
            .build()

        return GeckoSession(sessionSettings)
    }
}
