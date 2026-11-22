package com.absolutemango.htmlgom.commonMain.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import kotlin.math.atan2

@Composable
fun rememberUISensor(): UISensor {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val uiSensor = remember { AndroidUISensor(sensorManager) }

    DisposableEffect(Unit) {
        uiSensor.start()
        onDispose {
            uiSensor.stop()
        }
    }

    return uiSensor
}

interface UISensor {

    val gravityAngle: Float

    val gravity: Offset

    fun start()

    fun stop()
}

internal class AndroidUISensor(private val sensorManager: SensorManager) : UISensor, SensorEventListener {
    override var gravityAngle by mutableStateOf(0f)
    override var gravity by mutableStateOf(Offset.Zero)

    override fun start() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    override fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        gravity = Offset(-x, y)
        gravityAngle = Math.toDegrees(atan2(x.toDouble(), y.toDouble())).toFloat()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

