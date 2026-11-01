package com.canopy.clock.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.canopy.clock.MainActivity
import com.canopy.clock.data.Alarm
import com.canopy.clock.data.RunningTimer
import com.canopy.clock.data.Store
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object Scheduler {
    const val ACTION_ALARM = "com.canopy.clock.ALARM"
    const val ACTION_SNOOZED = "com.canopy.clock.SNOOZED"
    const val ACTION_TIMER = "com.canopy.clock.TIMER"
    const val EXTRA_ID = "id"

    const val CH_ALARM = "alarm"
    const val CH_TIMER = "timer"
    const val CH_INFO = "info"

    fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_ALARM, "Alarmas sonando", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null); enableVibration(false)
        })
        nm.createNotificationChannel(NotificationChannel(CH_TIMER, "Temporizadores e intervalos", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
        })
        nm.createNotificationChannel(NotificationChannel(CH_INFO, "Avisos", NotificationManager.IMPORTANCE_DEFAULT))
    }

    private fun am(ctx: Context) = ctx.getSystemService(AlarmManager::class.java)

    fun canExact(ctx: Context) = android.os.Build.VERSION.SDK_INT < 31 || am(ctx).canScheduleExactAlarms()

    /** Próxima vez que debe sonar, teniendo en cuenta días y condiciones. Devuelve también los días saltados. */
    fun next(ctx: Context, a: Alarm, from: LocalDateTime = LocalDateTime.now()): Pair<LocalDateTime?, List<Pair<LocalDate, String>>> {
        val skipped = mutableListOf<Pair<LocalDate, String>>()
        var day = from.toLocalDate()
        for (i in 0 until 60) {
            val at = day.atTime(a.hour, a.minute)
            if (at.isAfter(from) && (a.days.isEmpty() || day.dayOfWeek.value in a.days)) {
                val reason = Conditions.skipReason(ctx, a, day)
                if (reason == null) return at to skipped
                skipped += day to reason
                if (a.days.isEmpty() && reason == "Omitida una vez") return null to skipped
            }
            day = day.plusDays(1)
        }
        return null to skipped
    }

    private fun alarmPi(ctx: Context, id: Int, action: String, flags: Int = PendingIntent.FLAG_UPDATE_CURRENT): PendingIntent? =
        PendingIntent.getBroadcast(
            ctx, if (action == ACTION_SNOOZED) id + 500_000 else id,
            Intent(ctx, AlarmReceiver::class.java).setAction(action).putExtra(EXTRA_ID, id),
            flags or PendingIntent.FLAG_IMMUTABLE
        )

    private fun showPi(ctx: Context) = PendingIntent.getActivity(
        ctx, 1, Intent(ctx, MainActivity::class.java).putExtra("tab", "alarms"),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun epoch(t: LocalDateTime) = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun scheduleAll(ctx: Context) {
        Store.init(ctx)
        for (a in Store.alarms()) schedule(ctx, a)
    }

    fun schedule(ctx: Context, a: Alarm) {
        val pi = alarmPi(ctx, a.id, ACTION_ALARM)!!
        am(ctx).cancel(pi)
        if (!a.enabled || !canExact(ctx)) return
        val at = next(ctx, a).first ?: return
        am(ctx).setAlarmClock(AlarmManager.AlarmClockInfo(epoch(at), showPi(ctx)), pi)
    }

    fun cancel(ctx: Context, id: Int) {
        alarmPi(ctx, id, ACTION_ALARM)?.let { am(ctx).cancel(it) }
        cancelSnooze(ctx, id)
    }

    fun snooze(ctx: Context, a: Alarm) {
        if (!canExact(ctx)) return
        val at = System.currentTimeMillis() + a.snoozeMin * 60_000L
        am(ctx).setAlarmClock(AlarmManager.AlarmClockInfo(at, showPi(ctx)), alarmPi(ctx, a.id, ACTION_SNOOZED)!!)
        Store.setSnooze(a.id, at)
    }

    fun cancelSnooze(ctx: Context, id: Int) {
        alarmPi(ctx, id, ACTION_SNOOZED)?.let { am(ctx).cancel(it) }
        Store.setSnooze(id, 0)
    }

    // ---------- temporizadores ----------
    private fun timerPi(ctx: Context, id: Int) = PendingIntent.getBroadcast(
        ctx, 900_000 + id, Intent(ctx, AlarmReceiver::class.java).setAction(ACTION_TIMER).putExtra(EXTRA_ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun scheduleTimer(ctx: Context, t: RunningTimer) {
        val pi = timerPi(ctx, t.id)
        am(ctx).cancel(pi)
        if (!t.running) return
        if (canExact(ctx)) am(ctx).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.endAt, pi)
        else am(ctx).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.endAt, pi)
    }

    fun cancelTimer(ctx: Context, id: Int) = am(ctx).cancel(timerPi(ctx, id))

    fun rescheduleTimers(ctx: Context) {
        Store.init(ctx)
        for (t in Store.timers()) scheduleTimer(ctx, t)
    }
}
