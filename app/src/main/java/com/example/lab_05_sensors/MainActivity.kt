package com.example.lab_05_sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var avatar: ImageView
    private lateinit var root: View

    // Low-pass filter variables
    private var filteredAx = 0f
    private var filteredAy = 0f
    private val alpha = 0.15f // smoothing factor (0..1)

    // Motion tuning
    private val sensitivity = 18f // movement pixels per g
    private val maxTilt = 1.8f    // clamp g-force

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.main)
        avatar = findViewById(R.id.avatar)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // no-op
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        // Device coordinates: x (left-right), y (up-down). We invert to feel natural.
        val ax = (-event.values[0]).coerceIn(-maxTilt, maxTilt)
        val ay = (event.values[1]).coerceIn(-maxTilt, maxTilt)

        // Low-pass filter for smoothness
        filteredAx = filteredAx + alpha * (ax - filteredAx)
        filteredAy = filteredAy + alpha * (ay - filteredAy)

        // Compute desired translation
        val targetTx = filteredAx * sensitivity
        val targetTy = filteredAy * sensitivity

        // Boundaries so the view stays on screen
        val parent = root
        if (parent.width == 0 || parent.height == 0) return

        val halfW = avatar.width / 2f
        val halfH = avatar.height / 2f
        val maxX = (parent.width / 2f) - halfW
        val maxY = (parent.height / 2f) - halfH

        val clampedX = targetTx.coerceIn(-maxX, maxX)
        val clampedY = targetTy.coerceIn(-maxY, maxY)

        // Animate with a subtle springy feel
        avatar.animate()
            .translationX(clampedX)
            .translationY(clampedY)
            .setInterpolator(OvershootInterpolator(0.6f))
            .setDuration(120)
            .start()
    }
}