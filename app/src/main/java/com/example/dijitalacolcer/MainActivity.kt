
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
import android.widget.*
import kotlin.math.min

class MainActivity : Activity(), SensorEventListener {

    private lateinit var sm: SensorManager
    private var rotationSensor: Sensor? = null

    private lateinit var pitchValue: TextView
    private lateinit var rollValue: TextView
    private lateinit var statusText: TextView
    private lateinit var level: LevelView

    private var pitch = 0f
    private var roll = 0f
    private var pitchZero = 0f
    private var rollZero = 0f

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(16, 24, 32)
        window.navigationBarColor = Color.rgb(16, 24, 32)

        sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(15), dp(20), dp(15))
            setBackgroundColor(Color.rgb(16, 24, 32))
        }

        root.addView(TextView(this).apply {
            text = "DİJİTAL AÇI ÖLÇER"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, dp(55)))

        statusText = TextView(this).apply {
            text = if (rotationSensor == null)
                "Sensör bulunamadı"
            else
                "Canlı açı ölçümü"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        root.addView(statusText, LinearLayout.LayoutParams(-1, dp(35)))

        // Ön-arka açı
        root.addView(TextView(this).apply {
            text = "ÖN – ARKA AÇISI"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(244, 180, 0))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, dp(35)))

        pitchValue = numberText()
        root.addView(pitchValue, LinearLayout.LayoutParams(-1, dp(75)))

        // Sağ-sol açı
        root.addView(TextView(this).apply {
            text = "SAĞ – SOL AÇISI"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(244, 180, 0))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, dp(35)))

        rollValue = numberText()
        root.addView(rollValue, LinearLayout.LayoutParams(-1, dp(75)))

        level = LevelView(this)
        root.addView(level, LinearLayout.LayoutParams(dp(180), dp(180)).apply {
            gravity = Gravity.CENTER
            topMargin = dp(8)
            bottomMargin = dp(12)
        })

        root.addView(Button(this).apply {
            text = "SIFIRLA  (0°)"
            textSize = 17f
            setOnClickListener {
                pitchZero = pitch
                rollZero = roll
                updateReadout()
                statusText.text = "Sıfır noktası ayarlandı"
            }
        }, LinearLayout.LayoutParams(-1, dp(55)))

        root.addView(TextView(this).apply {
            text = "Telefonu ölçmek istediğin yüzeye yerleştir."
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, dp(45)))

        setContentView(root)
    }

    private fun numberText() = TextView(this).apply {
        text = "0,00°"
        textSize = 42f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        includeFontPadding = false
    }

    override fun onResume() {
        super.onResume()
        rotationSensor?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
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

        pitchValue.text = formatAngle(p)
        rollValue.text = formatAngle(r)

        level.setAngles(p, r)
    }

    private fun formatAngle(v: Float): String =
        String.format(java.util.Locale("tr", "TR"), "%.2f°", v)

    private fun wrap180(value: Float): Float {
        var v = value
        while (v > 180f) v -= 360f
        while (v < -180f) v += 360f
        return v
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        statusText.text = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "Sensör doğruluğu: yüksek"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Sensör doğruluğu: orta"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Sensör doğruluğu: düşük"
            else -> "Canlı açı ölçümü"
        }
    }

    private class LevelView(context: Context) : View(context) {

        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }

        private val yellow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(244, 180, 0)
            style = Paint.Style.FILL
        }

        private var p = 0f
        private var r = 0f

        fun setAngles(pitch: Float, roll: Float) {
            p = pitch
            r = roll
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val rad = min(width, height) * 0.40f

            canvas.drawCircle(cx, cy, rad, ring)
            canvas.drawCircle(cx, cy, rad * 0.66f, ring)
            canvas.drawLine(cx-rad, cy, cx+rad, cy, ring)
            canvas.drawLine(cx, cy-rad, cx, cy+rad, ring)

            val dx = (r / 30f).coerceIn(-1f, 1f) * rad * 0.66f
            val dy = (p / 30f).coerceIn(-1f, 1f) * rad * 0.66f

            canvas.drawCircle(cx + dx, cy + dy, 10f, yellow)
        }
    }
}
