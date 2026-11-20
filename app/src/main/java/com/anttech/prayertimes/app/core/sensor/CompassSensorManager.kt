package com.anttech.prayertimes.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

interface CompassSensorManager {
    fun getAzimuthFlow(): Flow<Float>
    fun isSensorAvailable(): Boolean
}

class DefaultCompassSensorManager(context: Context) : CompassSensorManager {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Öncelik 1: Donanımsal Rotation Vector veya Geomagnetic Rotation Vector
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

    // Öncelik 2: Ayrık İvmeölçer + Manyetik Alan Sensörü
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    override fun isSensorAvailable(): Boolean {
        return rotationSensor != null || (accelerometer != null && magnetometer != null)
    }

    override fun getAzimuthFlow(): Flow<Float> {
        if (!isSensorAvailable()) return emptyFlow()

        return callbackFlow {
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)

            val lastAccelerometer = FloatArray(3)
            val lastMagnetometer = FloatArray(3)
            var hasAccelerometer = false
            var hasMagnetometer = false

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR,
                        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> {
                            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                            SensorManager.getOrientation(rotationMatrix, orientationAngles)

                            var azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                            azimuthDegrees = (azimuthDegrees + 360f) % 360f
                            trySend(azimuthDegrees)
                        }
                        Sensor.TYPE_ACCELEROMETER -> {
                            System.arraycopy(event.values, 0, lastAccelerometer, 0, 3)
                            hasAccelerometer = true
                            if (hasMagnetometer) {
                                computeFromAccAndMag()
                            }
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            System.arraycopy(event.values, 0, lastMagnetometer, 0, 3)
                            hasMagnetometer = true
                            if (hasAccelerometer) {
                                computeFromAccAndMag()
                            }
                        }
                    }
                }

                private fun computeFromAccAndMag() {
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)) {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        var azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                        azimuthDegrees = (azimuthDegrees + 360f) % 360f
                        trySend(azimuthDegrees)
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            if (rotationSensor != null) {
                sensorManager.registerListener(
                    listener,
                    rotationSensor,
                    SensorManager.SENSOR_DELAY_UI
                )
            } else if (accelerometer != null && magnetometer != null) {
                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
                sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
            }

            awaitClose {
                sensorManager.unregisterListener(listener)
            }
        }
    }
}
