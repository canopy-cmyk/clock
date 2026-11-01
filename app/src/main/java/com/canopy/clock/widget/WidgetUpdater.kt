package com.canopy.clock.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.widget.RemoteViews
import com.canopy.clock.MainActivity
import com.canopy.clock.R
import com.canopy.clock.data.Store
import com.canopy.clock.faces.FaceRenderer
import com.canopy.clock.faces.Faces
import com.canopy.clock.faces.Fonts
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.sqrt

/** Cada esfera tiene su propio widget: todos comparten esta lógica. */
abstract class FaceWidget(private val faceId: String) : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        WidgetUpdater.update(ctx, mgr, ids, faceId)
        WidgetUpdater.scheduleTick(ctx)
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle) {
        WidgetUpdater.update(ctx, mgr, intArrayOf(id), faceId)
    }

    override fun onEnabled(ctx: Context) = WidgetUpdater.scheduleTick(ctx)
}

/** Llega cada minuto para refrescar los widgets. No despierta el móvil si la pantalla está apagada. */
class WidgetTickReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        WidgetUpdater.updateAll(ctx)
        WidgetUpdater.scheduleTick(ctx)
    }
}

object WidgetUpdater {
    private const val MAX_PIXELS = 360_000f

    fun update(ctx: Context, mgr: AppWidgetManager, ids: IntArray, faceId: String) {
        Store.init(ctx)
        Fonts.init(ctx)
        val spec = Faces.byId(faceId)
        val opts = Store.faceOptions(faceId).copy(showSeconds = false, sweep = false)
        val now = LocalDateTime.now()
        val density = ctx.resources.displayMetrics.density
        val open = PendingIntent.getActivity(
            ctx, 5, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        for (id in ids) {
            val o = mgr.getAppWidgetOptions(id)
            // En vertical el ancho real es MIN_WIDTH y el alto MAX_HEIGHT; en horizontal, al revés.
            val portrait = ctx.resources.configuration.orientation != android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val minW = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110); val maxW = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW)
            val minH = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110); val maxH = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH)
            val wDp = (if (portrait) minW else maxW).coerceAtLeast(60)
            val hDp = (if (portrait) maxH else minH).coerceAtLeast(60)
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                val live = runCatching { liveViews(ctx, spec, Store.faceOptions(faceId).copy(sweep = false), wDp * density, hDp * density, open) }.getOrNull()
                if (live != null) { runCatching { mgr.updateAppWidget(id, live) }; continue }
            }
            var w = wDp * density
            var h = hDp * density
            val area = w * h
            if (area > MAX_PIXELS) { val k = sqrt(MAX_PIXELS / area); w *= k; h *= k }
            val bmp = Bitmap.createBitmap(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            FaceRenderer.draw(Canvas(bmp), bmp.width.toFloat(), bmp.height.toFloat(), spec, opts, now)
            val rv = RemoteViews(ctx.packageName, R.layout.widget_face)
            rv.setImageViewBitmap(R.id.widget_image, bmp)
            rv.setContentDescription(R.id.widget_image, "Reloj ${spec.name}")
            rv.setOnClickPendingIntent(R.id.widget_image, open)
            runCatching { mgr.updateAppWidget(id, rv) }
        }
    }

    /**
     * Widget en vivo (Android 12+): la carcasa es un bitmap fijo y la hora la mueve el sistema
     * con un AnalogClock y varios TextClock nativos colocados encima. No depende de que la app
     * siga viva, así que no se congela aunque el sistema cierre la app en segundo plano.
     */
    @androidx.annotation.RequiresApi(31)
    private fun liveViews(ctx: Context, spec: com.canopy.clock.faces.FaceSpec, opts: com.canopy.clock.faces.FaceOptions,
                          wPx: Float, hPx: Float, open: PendingIntent): RemoteViews {
        val area = wPx * hPx
        val k = if (area > MAX_PIXELS) sqrt(MAX_PIXELS / area) else 1f
        val bw = (wPx * k).toInt().coerceAtLeast(1); val bh = (hPx * k).toInt().coerceAtLeast(1)
        val bg = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val ovb = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val live = FaceRenderer.drawStatic(Canvas(bg), Canvas(ovb), bw.toFloat(), bh.toFloat(), spec, opts)
        val inv = 1f / k
        val PX = android.util.TypedValue.COMPLEX_UNIT_PX
        val rv = RemoteViews(ctx.packageName, R.layout.widget_live)
        rv.setViewLayoutWidth(R.id.frame, wPx, PX)
        rv.setViewLayoutHeight(R.id.frame, hPx, PX)
        rv.setImageViewBitmap(R.id.bg, bg)
        rv.setContentDescription(R.id.bg, "Reloj ${spec.name}")
        if (live.usesOverlay) { rv.setImageViewBitmap(R.id.ov, ovb); rv.setViewVisibility(R.id.ov, android.view.View.VISIBLE) }

        // ---------- textos ----------
        val pool = mutableMapOf(
            "heavy" to mutableListOf(R.id.t_heavy1, R.id.t_heavy2), "heavy3" to mutableListOf(R.id.t_heavy3),
            "semi" to mutableListOf(R.id.t_semi1), "regular" to mutableListOf(R.id.t_reg1),
            "seg" to mutableListOf(R.id.t_seg1), "segw" to mutableListOf(R.id.t_segw1, R.id.t_segw2),
            "dots" to mutableListOf(R.id.t_dots1), "tube" to mutableListOf(R.id.t_tube1, R.id.t_tube2),
        )
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        for (tr in live.texts) {
            val key = if (tr.font == "heavy" && tr.spacing < -0.035f) "heavy3" else tr.font
            val vid = pool[key]?.removeFirstOrNull() ?: continue
            paint.typeface = when (tr.font) {
                "heavy" -> Fonts.heavy; "semi" -> Fonts.semi; "regular" -> Fonts.regular
                "seg", "segw" -> Fonts.seg; "dots" -> Fonts.dots; else -> Fonts.tube
            }
            val size = tr.size * inv
            paint.textSize = size
            paint.letterSpacing = tr.spacing
            paint.fontFeatureSettings = if (tr.font == "heavy") "tnum" else null
            val fm = paint.fontMetricsInt
            val top = tr.baseline * inv + fm.ascent
            val pattern = if (opts.h24) tr.pattern24 else tr.pattern12
            rv.setCharSequence(vid, "setFormat24Hour", pattern)
            rv.setCharSequence(vid, "setFormat12Hour", pattern)
            rv.setTextColor(vid, tr.color)
            rv.setTextViewTextSize(vid, PX, size)
            if (tr.sample.isNotEmpty()) {
                val sample = if (pattern.startsWith("h") && !pattern.startsWith("hh")) tr.sample else tr.sample
                val tw = paint.measureText(sample) + 2f
                val left = if (tr.centered) (tr.left + tr.width / 2) * inv - tw / 2 else tr.left * inv - tr.spacing * size / 2f - 1f
                rv.setViewLayoutWidth(vid, tw, PX)
                rv.setViewLayoutMargin(vid, RemoteViews.MARGIN_LEFT, left, PX)
            } else {
                rv.setViewLayoutMargin(vid, RemoteViews.MARGIN_LEFT, tr.left * inv, PX)
            }
            rv.setViewLayoutHeight(vid, (fm.descent - fm.ascent + 2).toFloat(), PX)
            rv.setViewLayoutMargin(vid, RemoteViews.MARGIN_TOP, top, PX)
            rv.setViewVisibility(vid, android.view.View.VISIBLE)
        }

        // ---------- manecillas ----------
        val tiny = android.graphics.drawable.Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
        val groups = live.hands.groupBy { Math.round(it.cx / 3f) to Math.round(it.cy / 3f) }.values.take(2)
        for ((gi, hs) in groups.withIndex()) {
            val vid = if (gi == 0) R.id.clock1 else R.id.clock2
            val reach = hs.maxOf { max(it.len, it.tail) + it.wd } * inv
            val side = (2 * reach + 4).toInt().coerceAtLeast(4)
            val cx = hs[0].cx * inv; val cy = hs[0].cy * inv
            fun icon(type: Int): android.graphics.drawable.Icon {
                val h = hs.firstOrNull { it.type == type } ?: return tiny
                val b = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
                val pp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    style = android.graphics.Paint.Style.STROKE; color = h.color; strokeWidth = h.wd * inv; strokeCap = h.cap
                }
                val c0 = side / 2f
                Canvas(b).drawLine(c0, c0 + h.tail * inv, c0, c0 - h.len * inv, pp)
                return android.graphics.drawable.Icon.createWithBitmap(b)
            }
            rv.setIcon(vid, "setDial", tiny)
            rv.setIcon(vid, "setHourHand", icon(0))
            rv.setIcon(vid, "setMinuteHand", icon(1))
            rv.setIcon(vid, "setSecondHand", icon(2))
            rv.setViewLayoutWidth(vid, side.toFloat(), PX)
            rv.setViewLayoutHeight(vid, side.toFloat(), PX)
            rv.setViewLayoutMargin(vid, RemoteViews.MARGIN_LEFT, cx - side / 2f, PX)
            rv.setViewLayoutMargin(vid, RemoteViews.MARGIN_TOP, cy - side / 2f, PX)
            rv.setViewVisibility(vid, android.view.View.VISIBLE)
        }
        rv.setOnClickPendingIntent(R.id.root, open)
        return rv
    }

    fun updateAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        for ((faceId, cls) in WidgetRegistry.PROVIDERS) {
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, cls))
            if (ids.isNotEmpty()) update(ctx, mgr, ids, faceId)
        }
    }

    fun hasAny(ctx: Context): Boolean {
        val mgr = AppWidgetManager.getInstance(ctx)
        return WidgetRegistry.PROVIDERS.any { mgr.getAppWidgetIds(ComponentName(ctx, it.second)).isNotEmpty() }
    }

    fun scheduleTick(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx, 77, Intent(ctx, WidgetTickReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
        if (android.os.Build.VERSION.SDK_INT >= 31) return
        if (!hasAny(ctx)) return
        val now = System.currentTimeMillis()
        val next = (now / 60_000 + 1) * 60_000 + 200
        // RTC (sin WAKEUP): solo llega con el móvil despierto, sin gastar batería con la pantalla apagada
        if (android.os.Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExact(AlarmManager.RTC, next, pi)
        else am.set(AlarmManager.RTC, next, pi)
    }
}
