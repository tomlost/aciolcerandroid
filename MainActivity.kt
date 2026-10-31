package com.example.dijitalacolcer

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.min

class MainActivity : Activity(), SensorEventListener {
    private lateinit var sm: SensorManager
    private var rotationSensor: Sensor? = null
    private lateinit var pitchText: TextView
    private lateinit var rollText: TextView
    private lateinit var statusText: TextView
    private lateinit var level: LevelView
    private var pitch = 0f
    private var roll = 0f
    private var pitchZero = 0f
    private var rollZero = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(22, 20, 22, 16)
            setBackgroundColor(Color.rgb(16, 24, 32))
        }
        root.addView(TextView(this).apply {
            text = "DİJİTAL AÇI ÖLÇER"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, 60))

        statusText = TextView(this).apply {
            text = if (rotationSensor == null) "Bu telefonda dönüş sensörü bulunamadı." else "Telefonu ölçmek istediğin yüzeye yerleştir."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        root.addView(statusText, LinearLayout.LayoutParams(-1, 42))

        pitchText = angleText("EĞİM / ÖN-ARKA")
        rollText = angleText("EĞİM / SAĞ-SOL")
        root.addView(pitchText, LinearLayout.LayoutParams(-1, 82))
        root.addView(rollText, LinearLayout.LayoutParams(-1, 82))

        level = LevelView(this)
        root.addView(level, LinearLayout.LayoutParams(270, 270).apply {
            gravity = Gravity.CENTER
            topMargin = 8
            bottomMargin = 12
        })

        root.addView(Button(this).apply {
            text = "BU KONUMU 0° KABUL ET"
            textSize = 16f
            setOnClickListener {
                pitchZero = pitch
                rollZero = roll
                updateReadout()
                statusText.text = "Sıfır noktası ayarlandı."
            }
        }, LinearLayout.LayoutParams(-1, 58))

        root.addView(TextView(this).apply {
            text = "Telefonu yüzeye düz yatır. Ön-arka ve sağ-sol eğimleri ayrı ayrı gösterilir. Sıfırlama, mevcut konumu referans alır."
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(8, 12, 8, 0)
        }, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
    }

    private fun angleText(label: String) = TextView(this).apply {
        text = "$label\n0.00°"
        textSize = 23f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
    }

    override fun onResume() {
        super.onResume()
        rotationSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onPause() {
        super.onPause()
        sm.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        val matrix = FloatArray(9)
        val orientation = FloatArray(3)
        SensorManager.getRotationMatrixFromVector(matrix, event.values)
        SensorManager.getOrientation(matrix, orientation)
        pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
        roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
        updateReadout()
    }

    private fun updateReadout() {
        val p = wrap180(pitch - pitchZero)
        val r = wrap180(roll - rollZero)
        pitchText.text = "EĞİM / ÖN-ARKA\n${fmt(p)}"
        rollText.text = "EĞİM / SAĞ-SOL\n${fmt(r)}"
        level.setAngles(p, r)
    }

    private fun fmt(v: Float) = String.format(java.util.Locale.US, "%.2f°", v)
    private fun wrap180(v0: Float): Float {
        var v = v0
        while (v > 180f) v -= 360f
        while (v < -180f) v += 360f
        return v
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        statusText.text = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "Sensör doğruluğu: yüksek"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Sensör doğruluğu: orta"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Sensör doğruluğu: düşük"
            else -> "Sensör kalibrasyonu kontrol edilmeli."
        }
    }

    private class LevelView(context: Context) : View(context) {
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY; strokeWidth = 3f; style = Paint.Style.STROKE
        }
        private val yellow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(244, 180, 0); style = Paint.Style.FILL
        }
        private var p = 0f
        private var r = 0f
        fun setAngles(pitch: Float, roll: Float) { p = pitch; r = roll; invalidate() }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val rad = min(width, height) * 0.40f
            canvas.drawCircle(cx, cy, rad, ring)
            canvas.drawCircle(cx, cy, rad * 0.66f, ring)
            canvas.drawLine(cx - rad, cy, cx + rad, cy, ring)
            canvas.drawLine(cx, cy - rad, cx, cy + rad, ring)
            val dx = (r / 30f).coerceIn(-1f, 1f) * rad * 0.66f
            val dy = (p / 30f).coerceIn(-1f, 1f) * rad * 0.66f
            canvas.drawCircle(cx + dx, cy + dy, 12f, yellow)
        }
    }
}
