package com.canopy.clock.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.canopy.clock.MainActivity
import com.canopy.clock.R
import com.canopy.clock.audio.TonePlayer
import com.canopy.clock.audio.ToneSynth
import com.canopy.clock.data.IntervalProgram
import com.canopy.clock.data.RunningTimer
import com.canopy.clock.data.Store
import kotlinx.coroutines.flow.MutableStateFlow

object TimerControl {
    fun start(ctx: Context, seconds: Int, label: String) {
        val ms = seconds * 1000L
        val t = RunningTimer(Store.nextId(), label, ms, System.currentTimeMillis() + ms, ms, true)
        Store.saveTimer(t)
        Scheduler.scheduleTimer(ctx, t)
        TimerNotifier.update(ctx)
    }

    fun toggle(ctx: Context, id: Int) {
        val t = Store.timers().firstOrNull { it.id == id } ?: return
        val now = System.currentTimeMillis()
        val n = if (t.running) t.copy(running = false, remainingMs = t.left(now))
        else t.copy(running = true, endAt = now + t.remainingMs)
        Store.saveTimer(n); Scheduler.scheduleTimer(ctx, n); TimerNotifier.update(ctx)
    }

    fun addMinute(ctx: Context, id: Int) {
        val t = Store.timers().firstOrNull { it.id == id } ?: return
        val n = if (t.running) t.copy(endAt = t.endAt + 60_000, totalMs = t.totalMs + 60_000)
        else t.copy(remainingMs = t.remainingMs + 60_000, totalMs = t.totalMs + 60_000)
        Store.saveTimer(n); Scheduler.scheduleTimer(ctx, n); TimerNotifier.update(ctx)
    }

    fun reset(ctx: Context, id: Int) {
        val t = Store.timers().firstOrNull { it.id == id } ?: return
        val n = t.copy(running = false, remainingMs = t.totalMs)
        Store.saveTimer(n); Scheduler.scheduleTimer(ctx, n); TimerNotifier.update(ctx)
    }

    fun remove(ctx: Context, id: Int) {
        Scheduler.cancelTimer(ctx, id)
        Store.deleteTimer(id)
        TimerNotifier.update(ctx)
    }
}

/** Notificación con cuenta atrás de cada temporizador activo. */
object TimerNotifier {
    private fun pi(ctx: Context, action: String, id: Int, code: Int) = PendingIntent.getBroadcast(
        ctx, code, Intent(ctx, ActionReceiver::class.java).setAction(action).putExtra(Scheduler.EXTRA_ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun update(ctx: Context) {
        Scheduler.channels(ctx)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val now = System.currentTimeMillis()
        val timers = Store.timers()
        val active = timers.map { 800_000 + it.id }.toSet()
        runCatching { nm.activeNotifications.filter { it.id in 800_000..899_999 && it.id !in active }.forEach { nm.cancel(it.id) } }
        val open = PendingIntent.getActivity(
            ctx, 3, Intent(ctx, MainActivity::class.java).putExtra("tab", "timer"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        for (t in timers) {
            if (t.left(now) <= 0 && !t.running) continue
            val b = NotificationCompat.Builder(ctx, Scheduler.CH_TIMER)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(t.label.ifBlank { "Temporizador" })
                .setOnlyAlertOnce(true)
                .setOngoing(t.running)
                .setContentIntent(open)
                .addAction(0, if (t.running) "Pausar" else "Seguir", pi(ctx, ActionReceiver.ACTION_TIMER_PAUSE, t.id, 810_000 + t.id))
                .addAction(0, "+1 min", pi(ctx, ActionReceiver.ACTION_TIMER_ADD, t.id, 820_000 + t.id))
                .addAction(0, "Quitar", pi(ctx, ActionReceiver.ACTION_TIMER_CANCEL, t.id, 830_000 + t.id))
            if (t.running) {
                b.setUsesChronometer(true).setChronometerCountDown(true).setWhen(t.endAt).setShowWhen(true)
            } else {
                b.setContentText("En pausa · " + fmt(t.remainingMs))
            }
            runCatching { nm.notify(800_000 + t.id, b.build()) }
        }
    }

    fun fmt(ms: Long): String {
        val s = (ms + 999) / 1000
        val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }
}

/** Ejecuta un programa de intervalos en segundo plano, con avisos sonoros entre fases. */
class IntervalService : Service() {

    data class Phase(val name: String, val seconds: Int, val color: Int, val round: Int)

    data class RunState(
        val program: IntervalProgram,
        val phases: List<Phase>,
        val index: Int,
        val phaseEnd: Long,
        val pausedLeft: Long,
        val paused: Boolean,
        val finished: Boolean = false,
    ) {
        fun left(now: Long) = if (paused) pausedLeft else (phaseEnd - now).coerceAtLeast(0)
        val current get() = phases.getOrNull(index)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var wake: PowerManager.WakeLock? = null
    private var lastBeepSec = -1L

    private val tick = object : Runnable {
        override fun run() {
            val s = state.value ?: return
            if (!s.paused && !s.finished) {
                val now = System.currentTimeMillis()
                val left = s.left(now)
                val secLeft = (left + 999) / 1000
                if (secLeft in 1..3 && secLeft != lastBeepSec) {
                    lastBeepSec = secLeft
                    TonePlayer.once(ToneSynth.click(), 1f)
                }
                if (left <= 0) advance(now)
            }
            handler.postDelayed(this, 200)
        }
    }

    private fun advance(now: Long) {
        val s = state.value ?: return
        val next = s.index + 1
        lastBeepSec = -1
        if (next >= s.phases.size) {
            state.value = s.copy(finished = true, index = s.phases.size - 1)
            TonePlayer.once(ToneSynth.cue(s.program.tone, 3))
            notifyState()
            handler.postDelayed({ stopSelf() }, 4000)
            return
        }
        val p = s.phases[next]
        state.value = s.copy(index = next, phaseEnd = now + p.seconds * 1000L)
        TonePlayer.once(ToneSynth.cue(s.program.tone, if (next == 0) 1 else 2))
        notifyState()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Store.init(this)
        Scheduler.channels(this)
        when (intent?.action) {
            ACTION_TOGGLE -> { toggle(); return START_NOT_STICKY }
            ACTION_SKIP -> { advance(System.currentTimeMillis()); return START_NOT_STICKY }
        }
        val id = intent?.getIntExtra(Scheduler.EXTRA_ID, -1) ?: -1
        val prog = Store.programs().firstOrNull { it.id == id } ?: run { stopSelf(); return START_NOT_STICKY }
        val phases = build(prog)
        if (phases.isEmpty()) { stopSelf(); return START_NOT_STICKY }
        val now = System.currentTimeMillis()
        state.value = RunState(prog, phases, 0, now + phases[0].seconds * 1000L, 0, false)
        ServiceCompat.startForeground(
            this, NOTIF, buildNotif(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        wake = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "clock:interval")
            .apply { acquire(prog.totalSec * 1000L + 120_000L) }
        TonePlayer.once(ToneSynth.cue(prog.tone, 1))
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_NOT_STICKY
    }

    private fun toggle() {
        val s = state.value ?: return
        val now = System.currentTimeMillis()
        state.value = if (s.paused) s.copy(paused = false, phaseEnd = now + s.pausedLeft)
        else s.copy(paused = true, pausedLeft = s.left(now))
        notifyState()
    }

    private fun notifyState() {
        runCatching { getSystemService(NotificationManager::class.java).notify(NOTIF, buildNotif()) }
    }

    private fun buildNotif(): android.app.Notification {
        val s = state.value
        val open = PendingIntent.getActivity(
            this, 4, Intent(this, MainActivity::class.java).putExtra("tab", "intervals"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        fun act(a: String, c: Int) = PendingIntent.getBroadcast(
            this, c, Intent(this, ActionReceiver::class.java).setAction(a), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(this, Scheduler.CH_TIMER)
            .setSmallIcon(R.drawable.ic_stat)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(open)
            .setContentTitle(s?.current?.name ?: "Intervalos")
            .setContentText(s?.let { "${it.program.name} · ronda ${it.current?.round ?: 0}/${it.program.rounds} · ${it.index + 1}/${it.phases.size}" } ?: "")
            .addAction(0, if (s?.paused == true) "Seguir" else "Pausar", act(ActionReceiver.ACTION_INTERVAL_PAUSE, 840_001))
            .addAction(0, "Terminar", act(ActionReceiver.ACTION_INTERVAL_STOP, 840_002))
        if (s != null && !s.paused && !s.finished) b.setUsesChronometer(true).setChronometerCountDown(true).setWhen(s.phaseEnd).setShowWhen(true)
        return b.build()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        state.value = null
        runCatching { if (wake?.isHeld == true) wake?.release() }
        super.onDestroy()
    }

    companion object {
        const val NOTIF = 43
        const val ACTION_TOGGLE = "com.canopy.clock.INTERVAL_TOGGLE"
        const val ACTION_SKIP = "com.canopy.clock.INTERVAL_SKIP"
        val state = MutableStateFlow<RunState?>(null)

        fun build(p: IntervalProgram): List<Phase> {
            val out = mutableListOf<Phase>()
            if (p.warmup > 0) out += Phase("Calentamiento", p.warmup, 0xFF8C8A84.toInt(), 0)
            for (r in 1..p.rounds) for (s in p.steps) if (s.seconds > 0) out += Phase(s.name, s.seconds, s.color, r)
            if (p.cooldown > 0) out += Phase("Vuelta a la calma", p.cooldown, 0xFF8C8A84.toInt(), p.rounds)
            return out
        }

        fun start(ctx: Context, id: Int) = androidx.core.content.ContextCompat.startForegroundService(
            ctx, Intent(ctx, IntervalService::class.java).putExtra(Scheduler.EXTRA_ID, id)
        )
        fun toggle(ctx: Context) = ctx.startService(Intent(ctx, IntervalService::class.java).setAction(ACTION_TOGGLE))
        fun skip(ctx: Context) = ctx.startService(Intent(ctx, IntervalService::class.java).setAction(ACTION_SKIP))
        fun stop(ctx: Context) = ctx.stopService(Intent(ctx, IntervalService::class.java))
    }
}
