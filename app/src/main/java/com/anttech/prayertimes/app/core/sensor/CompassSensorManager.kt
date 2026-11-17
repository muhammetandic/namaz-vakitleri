package com.anttech.prayertimes.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface CompassSensorManager {
    fun getAzimuthFlow(): Flow<Float>
}

class DefaultCompassSensorManager(context: Context) : CompassSensorManager {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    override fun getAzimuthFlow(): Flow<Float> = callbackFlow {
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)

                    // Azimuth angle in radians -> convert to degrees (0 - 360)
                    var azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    azimuthDegrees = (azimuthDegrees + 360f) % 360f

                    trySend(azimuthDegrees)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Kalibrasyon durumu değiştiğinde ele alınabilir
            }
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(
                listener,
                rotationSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
