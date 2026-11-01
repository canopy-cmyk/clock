package com.canopy.clock.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.canopy.clock.R
import com.canopy.clock.data.Store
import com.canopy.clock.widget.WidgetUpdater
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Store.init(ctx)
        Scheduler.channels(ctx)
        val id = intent.getIntExtra(Scheduler.EXTRA_ID, -1)
        when (intent.action) {
            Scheduler.ACTION_ALARM -> {
                val a = Store.alarm(id) ?: return
                val today = LocalDate.now()
                val reason = Conditions.skipReason(ctx, a, today)
                // la omisión puntual ya se ha consumido
                if (a.skipDate != null && a.skipDate <= today.toString()) Store.saveAlarm(a.copy(skipDate = null))
                if (reason != null) {
                    notifySkipped(ctx, a.id, "%02d:%02d".format(a.hour, a.minute), reason)
                    if (a.days.isEmpty()) Store.saveAlarm(a.copy(enabled = false, skipDate = null))
                    Scheduler.scheduleAll(ctx)
                    return
                }
                if (a.days.isEmpty()) Store.saveAlarm(a.copy(enabled = false, skipDate = null))
                RingService.start(ctx, RingService.KIND_ALARM, a.id)
                Scheduler.scheduleAll(ctx)
            }
            Scheduler.ACTION_SNOOZED -> {
                Store.setSnooze(id, 0)
                RingService.start(ctx, RingService.KIND_ALARM, id)
            }
            Scheduler.ACTION_TIMER -> {
                val t = Store.timers().firstOrNull { it.id == id } ?: return
                Store.saveTimer(t.copy(running = false, remainingMs = 0, endAt = System.currentTimeMillis()))
                TimerNotifier.update(ctx)
                RingService.start(ctx, RingService.KIND_TIMER, id)
            }
        }
    }

    private fun notifySkipped(ctx: Context, id: Int, time: String, reason: String) {
        val n = NotificationCompat.Builder(ctx, Scheduler.CH_INFO)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("Alarma de las $time omitida")
            .setContentText(reason)
            .setAutoCancel(true)
            .build()
        runCatching { ctx.getSystemService(NotificationManager::class.java).notify(700_000 + id, n) }
    }
}

/** Reprograma todo tras reiniciar, cambiar la hora o actualizar la app. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Store.init(ctx)
        Scheduler.channels(ctx)
        Scheduler.scheduleAll(ctx)
        Scheduler.rescheduleTimers(ctx)
        TimerNotifier.update(ctx)
        WidgetUpdater.updateAll(ctx)
        WidgetUpdater.scheduleTick(ctx)
    }
}

/** Botones de las notificaciones (posponer, detener, pausar temporizador...). */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Store.init(ctx)
        val id = intent.getIntExtra(Scheduler.EXTRA_ID, -1)
        when (intent.action) {
            ACTION_DISMISS_SNOOZE -> Scheduler.cancelSnooze(ctx, id)
            ACTION_TIMER_PAUSE -> TimerControl.toggle(ctx, id)
            ACTION_TIMER_CANCEL -> TimerControl.remove(ctx, id)
            ACTION_TIMER_ADD -> TimerControl.addMinute(ctx, id)
            ACTION_INTERVAL_STOP -> ctx.stopService(Intent(ctx, IntervalService::class.java))
            ACTION_INTERVAL_PAUSE -> runCatching { IntervalService.toggle(ctx) }
        }
    }

    companion object {
        const val ACTION_DISMISS_SNOOZE = "com.canopy.clock.DISMISS_SNOOZE"
        const val ACTION_TIMER_PAUSE = "com.canopy.clock.TIMER_PAUSE"
        const val ACTION_TIMER_CANCEL = "com.canopy.clock.TIMER_CANCEL"
        const val ACTION_TIMER_ADD = "com.canopy.clock.TIMER_ADD"
        const val ACTION_INTERVAL_STOP = "com.canopy.clock.INTERVAL_STOP"
        const val ACTION_INTERVAL_PAUSE = "com.canopy.clock.INTERVAL_PAUSE"
    }
}
