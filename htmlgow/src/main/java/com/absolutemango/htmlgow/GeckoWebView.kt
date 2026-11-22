package com.absolutemango.htmlgow

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoView
import java.io.File

private const val PREFS_NAME = "htmlgow_prefs"
private const val KEY_HARDWARE_ACCEL = "hardware_acceleration"
private const val KEY_DOM_STORAGE = "dom_storage"

@Composable
fun GeckoWebViewScreen(
    filePath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val hardwareAccel = remember { prefs.getBoolean(KEY_HARDWARE_ACCEL, true) }
    val domStorage = remember { prefs.getBoolean(KEY_DOM_STORAGE, true) }

    val runtime = remember { GeckoEngineManager.getOrCreateRuntime(context) }
    val session = remember(domStorage) { GeckoEngineManager.createOptimizedSession(domStorageEnabled = domStorage) }

    var geckoViewInstance by remember { mutableStateOf<GeckoView?>(null) }
    val focusRequester = remember { FocusRequester() }

    // Physical System Back Button handler
    BackHandler(enabled = true) {
        onBack()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    DisposableEffect(filePath, session) {
        session.open(runtime)
        session.compositorController.setClearColor(android.graphics.Color.BLACK)

        val uriToLoad = when {
            filePath.startsWith("file://") || filePath.startsWith("http://") || filePath.startsWith("https://") -> filePath
            filePath.startsWith("/") -> Uri.fromFile(File(filePath)).toString()
            else -> Uri.fromFile(File(context.filesDir, filePath)).toString()
        }

        session.loadUri(uriToLoad)

        onDispose {
            session.close()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent { event ->
                val pixels = event.verticalScrollPixels
                if (pixels != 0f) {
                    dispatchMouseWheelScroll(geckoViewInstance, pixels)
                }
                true
            }
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                GeckoView(ctx).apply {
                    coverUntilFirstPaint(android.graphics.Color.BLACK)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setSession(session)
                    session.compositorController.setClearColor(android.graphics.Color.BLACK)
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()

                    setOnTouchListener { v, _ ->
                        // Prevent parent containers & system gesture detectors from intercepting touch gestures
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        false
                    }

                    setOnGenericMotionListener { _, event ->
                        if (event.action == MotionEvent.ACTION_SCROLL && event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
                            val delta = event.getAxisValue(MotionEvent.AXIS_SCROLL)
                            if (delta != 0f) {
                                dispatchMouseWheelScroll(this, -delta * 25f)
                                return@setOnGenericMotionListener true
                            }
                        }
                        false
                    }

                    if (!hardwareAccel) {
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    }

                    geckoViewInstance = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun dispatchMouseWheelScroll(geckoView: GeckoView?, rawAmount: Float) {
    if (geckoView == null || rawAmount == 0f) return

    val width = geckoView.width.toFloat().coerceAtLeast(100f)
    val height = geckoView.height.toFloat().coerceAtLeast(100f)
    val now = SystemClock.uptimeMillis()

    // Calibrated mouse-wheel scroll factor (smooth, controlled notch movement)
    val axisValue = (-rawAmount * 0.12f).coerceIn(-3f, 3f)

    val pointerProperties = arrayOf(
        MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        }
    )
    val pointerCoords = arrayOf(
        MotionEvent.PointerCoords().apply {
            x = width / 2f
            y = height / 2f
            setAxisValue(MotionEvent.AXIS_VSCROLL, axisValue)
            setAxisValue(MotionEvent.AXIS_SCROLL, axisValue)
        }
    )

    val scrollEvent = MotionEvent.obtain(
        now,
        now,
        MotionEvent.ACTION_SCROLL,
        1,
        pointerProperties,
        pointerCoords,
        0,
        0,
        1f,
        1f,
        0,
        0,
        InputDevice.SOURCE_MOUSE or InputDevice.SOURCE_CLASS_POINTER,
        0
    )

    try {
        geckoView.dispatchGenericMotionEvent(scrollEvent)
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        scrollEvent.recycle()
    }
}
