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
            val wDp = max(o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0), o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)).coerceAtLeast(60)
            val hDp = max(o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0), 60).coerceAtLeast(60)
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
        if (!hasAny(ctx)) return
        val now = System.currentTimeMillis()
        val next = (now / 60_000 + 1) * 60_000 + 200
        // RTC (sin WAKEUP): solo llega con el móvil despierto, sin gastar batería con la pantalla apagada
        if (android.os.Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExact(AlarmManager.RTC, next, pi)
        else am.set(AlarmManager.RTC, next, pi)
    }
}
