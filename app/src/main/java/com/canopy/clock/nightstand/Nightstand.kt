package com.canopy.clock.nightstand

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Bundle
import android.service.dreams.DreamService
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import com.canopy.clock.alarm.Scheduler
import com.canopy.clock.data.Store
import com.canopy.clock.faces.FaceRenderer
import com.canopy.clock.faces.Faces
import com.canopy.clock.faces.Fonts
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.random.Random

/**
 * Vista de modo reloj: fondo negro, el reloj elegido a buen tamaño, brillo regulable,
 * filtro rojo opcional y un pequeño desplazamiento cada minuto para no marcar la pantalla.
 */
class NightClockView(context: Context) : View(context) {
    private val layer = Paint()
    private val dim = Paint()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shiftX = 0f
    private var shiftY = 0f
    private var lastMinute = -1
    private var nextAlarm: String? = null

    init {
        Store.init(context); Fonts.init(context)
        setBackgroundColor(Color.BLACK)
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    private fun refreshInfo() {
        val now = LocalDateTime.now()
        val n = Store.alarms().filter { it.enabled }.mapNotNull { Scheduler.next(context, it, now).first }.minOrNull()
        nextAlarm = n?.let { "Alarma " + it.format(DateTimeFormatter.ofPattern("HH:mm")) }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val now = LocalDateTime.now()
        val spec = Faces.byId(Store.nightFaceId)
        val opts = Store.faceOptions(spec.id)
        if (now.minute != lastMinute) {
            lastMinute = now.minute
            val d = resources.displayMetrics.density * 14
            shiftX = Random.nextFloat() * 2 * d - d
            shiftY = Random.nextFloat() * 2 * d - d
            refreshInfo()
        }
        val red = Store.nightRedShift
        layer.colorFilter = if (red) ColorMatrixColorFilter(
            ColorMatrix(floatArrayOf(
                0.3f, 0.59f, 0.11f, 0f, 0f,
                0f, 0.05f, 0f, 0f, 0f,
                0f, 0f, 0.02f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ))
        ) else null

        val w = width.toFloat(); val h = height.toFloat()
        val size = min(w, h) * if (w > h) 0.86f else 0.92f
        c.saveLayer(0f, 0f, w, h, layer)
        c.translate((w - size) / 2 + shiftX, (h - size) / 2 + shiftY - h * 0.03f)
        FaceRenderer.draw(c, size, size, spec, opts, now)
        c.restore()

        nextAlarm?.let {
            textPaint.color = if (red) Color.rgb(140, 20, 10) else Color.rgb(120, 120, 115)
            textPaint.textSize = 15 * resources.displayMetrics.scaledDensity
            textPaint.typeface = Fonts.semi
            textPaint.textAlign = Paint.Align.CENTER
            c.drawText(it, w / 2 + shiftX, h - 36 * resources.displayMetrics.density + shiftY / 2, textPaint)
        }

        // atenuación: capa negra encima, según el brillo elegido
        dim.color = Color.BLACK
        dim.alpha = ((1f - Store.nightDim) * 235).toInt().coerceIn(0, 235)
        c.drawRect(0f, 0f, w, h, dim)

        val smooth = (opts.sweep && opts.showSeconds) || spec.id in setOf("cubo", "radio", "televisor")
        if (smooth) postInvalidateOnAnimation()
        else postInvalidateDelayed(1000L - now.nano / 1_000_000 + 5)
    }
}

class NightstandActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val v = NightClockView(this)
        val gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean { finish(); return true }
            override fun onDown(e: MotionEvent) = true
        })
        v.setOnTouchListener { view, e -> gd.onTouchEvent(e); if (e.action == MotionEvent.ACTION_UP) view.performClick(); true }
        setContentView(v)
        window.insetsController?.let {
            it.hide(WindowInsets.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        android.widget.Toast.makeText(this, "Toca dos veces para salir", android.widget.Toast.LENGTH_SHORT).show()
    }
}

/** Salvapantallas del sistema: aparece solo al cargar si se elige en Ajustes > Pantalla > Salvapantallas. */
class ClockDream : DreamService() {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false
        setContentView(NightClockView(this))
    }
}
