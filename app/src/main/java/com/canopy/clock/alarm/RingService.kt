package com.canopy.clock.alarm

import android.app.Notification
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
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.canopy.clock.R
import com.canopy.clock.audio.TonePlayer
import com.canopy.clock.data.Store
import com.canopy.clock.data.ToneSettings
import kotlinx.coroutines.flow.MutableStateFlow

/** Servicio que hace sonar una alarma o un temporizador terminado. */
class RingService : Service() {

    data class Ringing(val kind: String, val id: Int, val title: String, val subtitle: String, val canSnooze: Boolean)

    private var player: TonePlayer? = null
    private var wake: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { finish(snooze = false) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Store.init(this)
        Scheduler.channels(this)
        when (intent?.action) {
            ACTION_DISMISS -> { finish(snooze = false); return START_NOT_STICKY }
            ACTION_SNOOZE -> { finish(snooze = true); return START_NOT_STICKY }
        }
        val kind = intent?.getStringExtra(EXTRA_KIND) ?: KIND_ALARM
        val id = intent?.getIntExtra(Scheduler.EXTRA_ID, -1) ?: -1

        // si ya sonaba otra cosa, se para antes de empezar la nueva
        stopSound()

        val ring: Ringing
        val tone: ToneSettings
        if (kind == KIND_TIMER) {
            val t = Store.timers().firstOrNull { it.id == id }
            ring = Ringing(kind, id, "Tiempo", t?.label?.ifBlank { null } ?: "Temporizador terminado", false)
            tone = Store.timerTone
        } else {
            val a = Store.alarm(id)
            ring = Ringing(kind, id, a?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "Alarma", a?.label?.ifBlank { null } ?: "Alarma", true)
            tone = a?.tone ?: ToneSettings()
        }
        state.value = ring

        ServiceCompat.startForeground(
            this, NOTIF_ID, notification(ring),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )

        wake = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "clock:ring").apply { acquire(11 * 60_000L) }

        player = TonePlayer(tone).also { it.start(maxSeconds = 600) }
        if (tone.vibrate) vibrate()

        // abre la pantalla de alarma si el móvil está desbloqueado y la app al frente
        runCatching {
            startActivity(Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION))
        }
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, 10 * 60_000L)
        return START_NOT_STICKY
    }

    private fun notification(r: Ringing): Notification {
        val full = PendingIntent.getActivity(
            this, 2, Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(this, Scheduler.CH_ALARM)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(r.title)
            .setContentText(r.subtitle)
            .setCategory(if (r.kind == KIND_TIMER) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(full, true)
            .setContentIntent(full)
            .setOngoing(true)
            .addAction(0, "Detener", selfPi(ACTION_DISMISS, 10))
        if (r.canSnooze) b.addAction(0, "Posponer", selfPi(ACTION_SNOOZE, 11))
        return b.build()
    }

    private fun selfPi(action: String, code: Int) = PendingIntent.getService(
        this, code, Intent(this, RingService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator
        else getSystemService(Vibrator::class.java)

    private fun vibrate() {
        runCatching { vibrator().vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 400, 500, 900), 0)) }
    }

    private fun stopSound() {
        player?.stop(); player = null
        runCatching { vibrator().cancel() }
    }

    private fun finish(snooze: Boolean) {
        val r = state.value
        stopSound()
        if (r != null) {
            if (r.kind == KIND_ALARM && snooze) Store.alarm(r.id)?.let { Scheduler.snooze(this, it) }
            if (r.kind == KIND_TIMER) { Store.deleteTimer(r.id); TimerNotifier.update(this) }
        }
        state.value = null
        handler.removeCallbacks(timeout)
        runCatching { wake?.release() }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopSound()
        state.value = null
        handler.removeCallbacks(timeout)
        runCatching { if (wake?.isHeld == true) wake?.release() }
        super.onDestroy()
    }

    companion object {
        const val KIND_ALARM = "alarm"
        const val KIND_TIMER = "timer"
        const val EXTRA_KIND = "kind"
        const val ACTION_DISMISS = "com.canopy.clock.RING_DISMISS"
        const val ACTION_SNOOZE = "com.canopy.clock.RING_SNOOZE"
        const val NOTIF_ID = 42

        val state = MutableStateFlow<Ringing?>(null)

        fun start(ctx: Context, kind: String, id: Int) {
            ContextCompat.startForegroundService(
                ctx, Intent(ctx, RingService::class.java).putExtra(EXTRA_KIND, kind).putExtra(Scheduler.EXTRA_ID, id)
            )
        }

        fun dismiss(ctx: Context) = ctx.startService(Intent(ctx, RingService::class.java).setAction(ACTION_DISMISS))
        fun snooze(ctx: Context) = ctx.startService(Intent(ctx, RingService::class.java).setAction(ACTION_SNOOZE))
    }
}
