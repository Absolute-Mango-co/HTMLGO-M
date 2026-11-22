package com.absolutemango.htmlgom.core

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Performance & Thermal governor that dynamically determines if complex AGSL fragment shaders
 * (real-time directional blur, dynamic refraction lens, vibrancy) can be enabled safely
 * without causing GPU frame drops, low-memory killer events, or thermal throttling.
 *
 * Provides a developer toggle to turn off the governor and return to the standard
 * configuration of backdrops without any throttling or bypass. Idle inactivity never
 * stops or degrades backdrop rendering.
 */
object AgslPerformanceGovernor {
    const val PREF_GOVERNOR_ENABLED = "agsl_governor_enabled"
    private const val MIN_RAM_GB_FOR_AGSL = 6L
    private const val HEADROOM_POLL_INTERVAL_MS = 10_000L

    private val governorScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var headroomJob: Job? = null

    // Idle degradation is eliminated: backdrops keep rendering continuously without stopping
    private val _isIdleState = mutableStateOf(false)
    val isIdleState: State<Boolean> = _isIdleState

    private val _isGovernorEnabled = mutableStateOf(true)
    val isGovernorEnabled: State<Boolean> = _isGovernorEnabled

    private val _isThermalThrottled = mutableStateOf(false)
    val isThermalThrottled: State<Boolean> = _isThermalThrottled

    private var currentThermalStatus: Int = 0
    private var isInitialized = false
    private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    /**
     * Initializes governor settings, thermal headroom polling, and system thermal listeners.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val prefs = context.getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE)
        _isGovernorEnabled.value = prefs.getBoolean(PREF_GOVERNOR_ENABLED, true)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == PREF_GOVERNOR_ENABLED) {
                _isGovernorEnabled.value = sp.getBoolean(PREF_GOVERNOR_ENABLED, true)
            }
        }
        prefListener = listener
        prefs.registerOnSharedPreferenceChangeListener(listener)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return

        // Register OnThermalStatusChangedListener for Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                powerManager.addThermalStatusListener { status ->
                    currentThermalStatus = status
                    _isThermalThrottled.value = status >= PowerManager.THERMAL_STATUS_MODERATE
                }
            } catch (_: Throwable) {
                // Ignore security or OEM reflection failures
            }
        }

        // On API 30+ (Android 11+), poll thermal headroom every 10 seconds to detect thermal ramp
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            headroomJob?.cancel()
            headroomJob = governorScope.launch {
                while (isActive) {
                    try {
                        val headroom = powerManager.getThermalHeadroom(10)
                        if (!headroom.isNaN()) {
                            if (headroom > 0.85f) {
                                _isThermalThrottled.value = true
                            } else if (currentThermalStatus < PowerManager.THERMAL_STATUS_MODERATE) {
                                _isThermalThrottled.value = false
                            }
                        }
                    } catch (_: Throwable) {
                        // Fallback safely to listener
                    }
                    delay(HEADROOM_POLL_INTERVAL_MS)
                }
            }
        }
    }

    /**
     * Enables or disables the governor. When disabled, the app returns to the standard
     * configuration of the backdrop with full shaders and zero throttling.
     */
    fun setGovernorEnabled(context: Context, enabled: Boolean) {
        _isGovernorEnabled.value = enabled
        try {
            val prefs = context.getSharedPreferences("htmlgo_settings", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(PREF_GOVERNOR_ENABLED, enabled).apply()
        } catch (_: Exception) {}
    }

    /**
     * Notifies the governor of user interaction (touch, tap, scroll).
     * Idle shutoff is eliminated so backdrops remain at full visual fidelity continuously.
     */
    fun notifyUserInteraction() {
        _isIdleState.value = false
    }

    /**
     * Checks if complex AGSL shaders should be enabled based on device hardware capabilities.
     * When the governor is turned off, always returns true on Android 13+ (standard configuration).
     */
    fun shouldEnableComplexShaders(context: Context): Boolean {
        if (!_isGovernorEnabled.value) {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        if (am.isLowRamDevice) return false

        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalRamGb = memInfo.totalMem / (1024L * 1024L * 1024L)
        return totalRamGb >= MIN_RAM_GB_FOR_AGSL
    }

    /**
     * Helper to determine if AGSL shaders should be bypassed.
     * Returns false if governor is disabled, or if device is capable and cool.
     * Never bypasses on idle inactivity.
     */
    fun isBypassed(context: Context): Boolean {
        if (!_isGovernorEnabled.value) return false
        return !shouldEnableComplexShaders(context) || _isThermalThrottled.value
    }
}
